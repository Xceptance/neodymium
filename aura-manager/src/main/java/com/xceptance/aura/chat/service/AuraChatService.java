/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.aura.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.aura.chat.dto.ChatMessageDto;
import com.xceptance.aura.chat.dto.ChatRequest;
import com.xceptance.aura.chat.dto.ChatResponse;
import com.xceptance.aura.chat.dto.PendingConfirmationDto;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Orchestrates Aura Dashboard chat conversations using Spring AI {@link ChatClient}
 * with native multi-turn tool calling bound to {@link AuraMcpToolCallbackProvider}.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
@Service
public class AuraChatService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraChatService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_TRACE_LENGTH = 2000;
    private static final int CONFIRMATION_TTL_SECONDS = 55;
    private static final String DEFAULT_SESSION_KEY = "_default";
    private static final String TOOL_DELETE_TEST = "delete_test";
    private static final String TOOL_CANCEL_RUN = "cancel_run";
    private static final Map<String, String> DESTRUCTIVE_TOOL_TARGET_PARAMS = Map.of(
            TOOL_DELETE_TEST, "testPath",
            TOOL_CANCEL_RUN, "runId");
    private static final String STATUS_CONFIRMATION_REQUIRED = "CONFIRMATION_REQUIRED";

    private static final String SYSTEM_PROMPT = """
            You are the Neodymium Aura AI Assistant embedded in the Aura Manager web dashboard.
            You have direct access to Aura's MCP tools for listing tests, inspecting test content, \
            creating/updating or deleting YAML playbooks, listing browser profiles, dispatching test \
            or suite runs, checking run status, inspecting failures, cancelling active runs, and \
            analyzing dashboards, batches, and historical run results.

            Guidelines:
            - Use the provided tools autonomously whenever the user asks about tests, browser profiles, \
            run status, failures, dashboard metrics, batches, or wants to run/create/edit tests.
            - You may invoke multiple tools in sequence to gather all needed facts before answering.
            - Any '[Active editor file: ...]' or '[Active view context: ...]' prefix is passive UI context \
            showing what the user is currently viewing in Aura Manager:
              * When the user asks about "this dashboard", "this page", "current metrics", or "these results" \
            and a dashboard pageId is present in view context, call analyze_dashboard_page(pageId) first; \
            then drill into specific runs via get_status(runId) or get_run_failures(runId) when anomalies or failures appear.
              * When a runId is present in view context and the user asks about "this run" or its failures, \
            call get_status(runId) and get_run_failures(runId).
              * For overview/batch/history questions or when no pageId/runId is given, use list_runs, list_batches, \
            or list_custom_dashboards to discover relevant runs and dashboards.
              * Only inspect or modify the active editor file if the user refers to the currently open/active file.
            - When creating or updating YAML playbooks via create_test:
              * Keep 'steps' as a multiline YAML string ('|') with plain natural-language step sentences.
              * Do not include root metadata fields like 'name' or 'description'.
              * Do not invent HTML IDs, CSS selectors, or XPaths unless explicitly provided.
            - Destructive tools (delete_test, cancel_run) require explicit UI button confirmation:
              * Never invoke delete_test or cancel_run speculatively if the target is ambiguous or unverified; \
            clarify with the user first.
              * When a tool returns CONFIRMATION_REQUIRED, explain the impact clearly and instruct the user \
            to click the Confirm or Cancel button below the message (chat replies like "yes" cannot confirm destructive actions).
            - Present clear, concise, markdown-formatted answers grounded strictly in tool outputs.""";

    private final AuraChatModelFactory chatModelFactory;
    private final AuraMcpToolCallbackProvider toolCallbackProvider;
    private final Clock clock;
    private final ConcurrentHashMap<String, Map<ConfirmationKey, PendingConfirmation>> pendingConfirmationsBySession =
            new ConcurrentHashMap<>();
    private final AtomicLong confirmationMutationSeq = new AtomicLong(0L);
    private final AtomicLong turnIdSeq = new AtomicLong(0L);
    private final Object turnWatermarkLock = new Object();
    private final Map<Long, Long> inFlightTurnStartSeqs = new HashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<ConfirmationKey, Long>> resolvedSeqBySession =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> sessionClearedSeqBySession =
            new ConcurrentHashMap<>();

    /**
     * Constructs the chat service.
     *
     * @param chatModelFactory factory resolving the active Spring AI {@link ChatModel}
     * @param toolCallbackProvider provider exposing {@code AuraMcpToolService} tools to {@link ChatClient}
     */
    @Autowired
    public AuraChatService(final AuraChatModelFactory chatModelFactory,
                           final AuraMcpToolCallbackProvider toolCallbackProvider)
    {
        this(chatModelFactory, toolCallbackProvider, Clock.systemUTC());
    }

    AuraChatService(final AuraChatModelFactory chatModelFactory,
                    final AuraMcpToolCallbackProvider toolCallbackProvider,
                    final Clock clock)
    {
        this.chatModelFactory = chatModelFactory;
        this.toolCallbackProvider = toolCallbackProvider;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    /**
     * Executes a chat turn using Spring AI {@link ChatClient} and returns the synthesized answer
     * along with tool execution traces and any UI-refresh actions.
     *
     * @param req incoming chat request containing prompt and prior conversation history
     * @return structured chat response
     */
    public ChatResponse runChatWorkflow(final ChatRequest req)
    {
        if (req == null || req.prompt == null || req.prompt.isBlank())
        {
            return new ChatResponse("Please enter a prompt.", "", null, null);
        }

        final List<String> thinkingEntries = Collections.synchronizedList(new ArrayList<>());
        final Set<String> uiActions = Collections.synchronizedSet(new LinkedHashSet<>());

        final ChatModel chatModel;
        try
        {
            chatModel = this.chatModelFactory.createChatModel();
        }
        catch (final Exception e)
        {
            LOGGER.warn("Chat model initialization failed: {}", e.getMessage());
            return new ChatResponse(e.getMessage(), "", "error", null);
        }

        final String sessionKey = normalizeSessionKey(req.sessionId);
        final long turnId = this.turnIdSeq.incrementAndGet();
        final long turnStartSeq = registerInFlightTurn(turnId);
        final Map<ConfirmationKey, PendingConfirmation> issuedInCurrentTurn = new ConcurrentHashMap<>();

        try
        {
            final List<Message> historyMessages = buildHistoryMessages(req.history);

            final ToolCallback[] baseDelegates = this.toolCallbackProvider.getToolCallbacks();
            final ToolCallback[] barrierCallbacks = wrapWithConfirmationBarrier(
                    baseDelegates,
                    issuedInCurrentTurn,
                    thinkingEntries,
                    confirmKey -> isResolvedSince(sessionKey, confirmKey, turnStartSeq));
            final ToolCallback[] observedCallbacks = wrapWithTraceObserver(
                    barrierCallbacks, thinkingEntries, uiActions);

            final ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultSystem(SYSTEM_PROMPT)
                    .build();

            final String basePrompt = buildContextualUserPrompt(req);
            final String userPrompt = mergeTrailingUserPrompt(historyMessages, basePrompt);

            final ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                    .toolCallbacks(List.of(observedCallbacks));
            if (!historyMessages.isEmpty())
            {
                requestSpec.messages(historyMessages);
            }

            final String assistantReply = requestSpec.user(userPrompt).call().content();
            final String safeReply = (assistantReply != null && !assistantReply.isBlank())
                    ? assistantReply
                    : "Done.";

            mergeNewlyIssuedConfirmations(sessionKey, issuedInCurrentTurn, turnStartSeq);

            final List<PendingConfirmationDto> pendingDtos = getPendingConfirmations(sessionKey);
            final List<String> actionList = snapshotUiActions(uiActions);
            final String primaryAction = actionList.isEmpty() ? null : actionList.get(0);

            return new ChatResponse(
                    safeReply,
                    joinTrace(thinkingEntries),
                    primaryAction,
                    actionList.isEmpty() ? null : actionList,
                    pendingDtos.isEmpty() ? null : pendingDtos);
        }
        catch (final Exception e)
        {
            LOGGER.error("Spring AI ChatClient execution failed", e);
            final List<PendingConfirmationDto> survivingPending = getPendingConfirmations(sessionKey);
            final List<String> actionList = snapshotUiActions(uiActions);
            thinkingEntries.add("[Error] " + e.getMessage());
            return new ChatResponse("Error processing request: " + e.getMessage(),
                    joinTrace(thinkingEntries),
                    "error",
                    actionList.isEmpty() ? null : actionList,
                    survivingPending.isEmpty() ? null : survivingPending);
        }
        finally
        {
            releaseInFlightTurnAndPrune(turnId);
        }
    }

    /**
     * Returns UI-safe summaries of unexpired pending destructive confirmations for the given session.
     *
     * @param sessionId chat session identifier
     * @return immutable list of unexpired pending confirmation summaries
     */
    public List<PendingConfirmationDto> getPendingConfirmations(final String sessionId)
    {
        final String sessionKey = normalizeSessionKey(sessionId);
        final List<PendingConfirmationDto> result = new ArrayList<>();
        final Instant now = this.clock.instant();

        this.pendingConfirmationsBySession.computeIfPresent(sessionKey, (key, sessionMap) -> {
            sessionMap.entrySet().removeIf(entry -> entry.getValue() == null || !entry.getValue().expiresAt().isAfter(now));
            if (sessionMap.isEmpty())
            {
                return null;
            }
            for (final PendingConfirmation pending : sessionMap.values())
            {
                result.add(new PendingConfirmationDto(
                        pending.actionId(),
                        pending.toolName(),
                        pending.target(),
                        pending.expiresAt().toEpochMilli()));
            }
            return sessionMap;
        });

        if (result.isEmpty())
        {
            return List.of();
        }
        result.sort(Comparator.comparing((PendingConfirmationDto dto) -> dto.toolName)
                .thenComparing(dto -> dto.target)
                .thenComparing(dto -> dto.actionId));
        return List.copyOf(result);
    }

    /**
     * Executes a single pending destructive confirmation explicitly triggered via a UI Confirm action,
     * injecting the server-held confirmation token into the underlying MCP tool without exposing it.
     *
     * @param sessionId chat session identifier
     * @param actionId pending confirmation action identifier
     * @return structured response reflecting the tool execution outcome and UI actions
     */
    public ChatResponse executePendingConfirmation(final String sessionId, final String actionId)
    {
        final String sessionKey = normalizeSessionKey(sessionId);
        if (actionId == null || actionId.isBlank())
        {
            return errorConfirmationResponse(sessionKey, "Invalid confirmation action identifier.", "");
        }

        final PendingConfirmation pending = removeByActionId(sessionKey, actionId.trim());
        if (pending == null)
        {
            return errorConfirmationResponse(
                    sessionKey,
                    "Confirmation request was not found or has already been handled. Please issue the request again if needed.",
                    "");
        }

        if (!pending.expiresAt().isAfter(this.clock.instant()))
        {
            return errorConfirmationResponse(
                    sessionKey,
                    "Confirmation expired for `" + pending.toolName() + "` on `" + pending.target()
                            + "`. Please ask again to issue a new confirmation.",
                    "");
        }

        final ToolCallback delegate = findToolCallback(pending.toolName());
        if (delegate == null)
        {
            return errorConfirmationResponse(sessionKey, "Tool `" + pending.toolName() + "` is unavailable.", "");
        }

        final String targetParam = resolveDestructiveTargetParam(pending.toolName());
        final List<String> thinkingEntries = Collections.synchronizedList(new ArrayList<>());
        final Set<String> uiActions = Collections.synchronizedSet(new LinkedHashSet<>());

        try
        {
            final String displayArgs = OBJECT_MAPPER.writeValueAsString(Map.of(targetParam, pending.target()));
            final String executionArgs = OBJECT_MAPPER.writeValueAsString(Map.of(
                    targetParam, pending.target(),
                    "confirmationToken", pending.token()));

            final String rawOutput = delegate.call(executionArgs);
            appendToolTrace(thinkingEntries, pending.toolName(), displayArgs, rawOutput);
            detectUiAction(pending.toolName(), rawOutput, uiActions);

            final List<String> actionList = new ArrayList<>(uiActions);
            final boolean isToolError = isToolErrorStatus(rawOutput);
            final String primaryAction = isToolError
                    ? "error"
                    : (actionList.isEmpty() ? null : actionList.get(0));
            final List<String> effectiveActions = (!isToolError && !actionList.isEmpty()) ? actionList : null;
            final String reply = formatDirectConfirmationOutcome(pending, rawOutput, isToolError);
            final List<PendingConfirmationDto> remaining = getPendingConfirmations(sessionKey);

            return new ChatResponse(
                    reply,
                    joinTrace(thinkingEntries),
                    primaryAction,
                    effectiveActions,
                    remaining.isEmpty() ? null : remaining);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed executing confirmed action {} on {}", pending.toolName(), pending.target(), e);
            thinkingEntries.add("[Error] " + e.getMessage());
            return errorConfirmationResponse(
                    sessionKey,
                    "Failed to execute `" + pending.toolName() + "`: " + e.getMessage(),
                    joinTrace(thinkingEntries));
        }
    }

    private ChatResponse errorConfirmationResponse(final String sessionKey, final String message, final String thinking)
    {
        final List<PendingConfirmationDto> remaining = getPendingConfirmations(sessionKey);
        return new ChatResponse(
                message,
                thinking != null ? thinking : "",
                "error",
                null,
                remaining.isEmpty() ? null : remaining);
    }

    /**
     * Cancels a single pending destructive confirmation explicitly rejected via the UI Cancel button.
     *
     * @param sessionId chat session identifier
     * @param actionId pending confirmation action identifier
     * @return confirmation cancellation response
     */
    public ChatResponse cancelPendingConfirmation(final String sessionId, final String actionId)
    {
        if (actionId == null || actionId.isBlank())
        {
            return new ChatResponse("Pending confirmation cleared.", "");
        }

        final String sessionKey = normalizeSessionKey(sessionId);
        final PendingConfirmation removed = removeByActionId(sessionKey, actionId.trim());
        if (removed == null)
        {
            return errorConfirmationResponse(
                    sessionKey,
                    "Pending confirmation was already cleared or expired.",
                    "");
        }

        final List<PendingConfirmationDto> remaining = getPendingConfirmations(sessionKey);
        return new ChatResponse(
                "Cancelled pending `" + removed.toolName() + "` action on `" + removed.target()
                        + "`. No changes were made.",
                "",
                null,
                null,
                remaining.isEmpty() ? null : remaining);
    }

    /**
     * Evicts all pending destructive confirmations associated with a deleted or reset chat session.
     *
     * @param sessionId chat session identifier
     */
    public void clearPendingConfirmations(final String sessionId)
    {
        if (sessionId == null || sessionId.isBlank())
        {
            return;
        }
        final String sessionKey = sessionId.trim();
        final long seq = this.confirmationMutationSeq.incrementAndGet();
        this.sessionClearedSeqBySession.put(sessionKey, seq);
        this.pendingConfirmationsBySession.remove(sessionKey);
        this.resolvedSeqBySession.remove(sessionKey);
        pruneIdleResolutionMarkers();
    }

    private void pruneIdleResolutionMarkers()
    {
        final long watermark;
        synchronized (this.turnWatermarkLock)
        {
            if (!this.inFlightTurnStartSeqs.isEmpty())
            {
                return;
            }
            watermark = this.confirmationMutationSeq.get();
        }
        pruneObsoleteResolutionMarkers(watermark);
    }

    private long registerInFlightTurn(final long turnId)
    {
        synchronized (this.turnWatermarkLock)
        {
            final long startSeq = this.confirmationMutationSeq.get();
            this.inFlightTurnStartSeqs.put(turnId, startSeq);
            return startSeq;
        }
    }

    private void releaseInFlightTurnAndPrune(final long turnId)
    {
        final long watermark;
        synchronized (this.turnWatermarkLock)
        {
            this.inFlightTurnStartSeqs.remove(turnId);
            watermark = computeSafePruneWatermarkLocked();
        }
        pruneObsoleteResolutionMarkers(watermark);
    }

    private long computeSafePruneWatermarkLocked()
    {
        if (this.inFlightTurnStartSeqs.isEmpty())
        {
            return this.confirmationMutationSeq.get();
        }
        long minStartSeq = Long.MAX_VALUE;
        for (final Long seq : this.inFlightTurnStartSeqs.values())
        {
            if (seq != null && seq < minStartSeq)
            {
                minStartSeq = seq;
            }
        }
        return minStartSeq == Long.MAX_VALUE ? this.confirmationMutationSeq.get() : minStartSeq;
    }

    private void pruneObsoleteResolutionMarkers(final long watermark)
    {
        this.sessionClearedSeqBySession.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() <= watermark);
        for (final String key : List.copyOf(this.resolvedSeqBySession.keySet()))
        {
            this.resolvedSeqBySession.computeIfPresent(key, (ignored, keyMap) -> {
                keyMap.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() <= watermark);
                return keyMap.isEmpty() ? null : keyMap;
            });
        }
    }

    int getResolutionMarkerCount()
    {
        int count = this.sessionClearedSeqBySession.size();
        for (final ConcurrentHashMap<ConfirmationKey, Long> keyMap : this.resolvedSeqBySession.values())
        {
            count += keyMap.size();
        }
        return count;
    }

    private boolean isResolvedSince(final String sessionKey, final ConfirmationKey confirmKey, final long turnStartSeq)
    {
        final Long clearedSeq = this.sessionClearedSeqBySession.get(sessionKey);
        if (clearedSeq != null && clearedSeq > turnStartSeq)
        {
            return true;
        }
        if (confirmKey == null)
        {
            return false;
        }
        final ConcurrentHashMap<ConfirmationKey, Long> sessionResolved = this.resolvedSeqBySession.get(sessionKey);
        if (sessionResolved == null)
        {
            return false;
        }
        final Long resolvedSeq = sessionResolved.get(confirmKey);
        return resolvedSeq != null && resolvedSeq > turnStartSeq;
    }

    private static String normalizeSessionKey(final String sessionId)
    {
        return (sessionId != null && !sessionId.isBlank()) ? sessionId.trim() : DEFAULT_SESSION_KEY;
    }

    private static String resolveDestructiveTargetParam(final String toolName)
    {
        return DESTRUCTIVE_TOOL_TARGET_PARAMS.getOrDefault(toolName, "runId");
    }

    private void mergeNewlyIssuedConfirmations(
            final String sessionKey,
            final Map<ConfirmationKey, PendingConfirmation> newlyIssued,
            final long turnStartSeq)
    {
        if (newlyIssued == null || newlyIssued.isEmpty())
        {
            return;
        }
        final Instant now = this.clock.instant();
        this.pendingConfirmationsBySession.compute(sessionKey, (key, existing) -> {
            final Map<ConfirmationKey, PendingConfirmation> merged = new ConcurrentHashMap<>();
            if (existing != null)
            {
                existing.forEach((confirmKey, pending) -> {
                    if (pending != null && pending.expiresAt().isAfter(now))
                    {
                        merged.put(confirmKey, pending);
                    }
                });
            }
            newlyIssued.forEach((confirmKey, pending) -> {
                if (pending != null
                        && pending.expiresAt().isAfter(now)
                        && !isResolvedSince(sessionKey, confirmKey, turnStartSeq))
                {
                    merged.put(confirmKey, pending);
                }
            });
            return merged.isEmpty() ? null : merged;
        });
    }

    private PendingConfirmation removeByActionId(final String sessionKey, final String actionId)
    {
        final PendingConfirmation[] removedHolder = new PendingConfirmation[1];
        final Instant now = this.clock.instant();
        this.pendingConfirmationsBySession.computeIfPresent(sessionKey, (key, sessionMap) -> {
            for (final Map.Entry<ConfirmationKey, PendingConfirmation> entry : sessionMap.entrySet())
            {
                final PendingConfirmation value = entry.getValue();
                if (value != null && actionId.equals(value.actionId()))
                {
                    removedHolder[0] = sessionMap.remove(entry.getKey());
                    if (removedHolder[0] != null && removedHolder[0].expiresAt().isAfter(now))
                    {
                        final long seq = this.confirmationMutationSeq.incrementAndGet();
                        this.resolvedSeqBySession
                                .computeIfAbsent(sessionKey, ignored -> new ConcurrentHashMap<>())
                                .put(entry.getKey(), seq);
                    }
                    break;
                }
            }
            return sessionMap.isEmpty() ? null : sessionMap;
        });
        if (removedHolder[0] != null)
        {
            pruneIdleResolutionMarkers();
        }
        return removedHolder[0];
    }

    private ToolCallback findToolCallback(final String toolName)
    {
        for (final ToolCallback callback : this.toolCallbackProvider.getToolCallbacks())
        {
            if (toolName.equals(callback.getToolDefinition().name()))
            {
                return callback;
            }
        }
        return null;
    }

    private static boolean isToolErrorStatus(final String rawOutput)
    {
        if (rawOutput == null || rawOutput.isBlank())
        {
            return false;
        }
        try
        {
            final JsonNode root = OBJECT_MAPPER.readTree(rawOutput);
            final String status = root.path("status").asText("").trim();
            return !status.isEmpty()
                    && !"SUCCESS".equalsIgnoreCase(status)
                    && !"CANCELLED".equalsIgnoreCase(status);
        }
        catch (final Exception e)
        {
            LOGGER.debug("Unable to inspect confirmation tool status as JSON: {}", e.getMessage());
            return false;
        }
    }

    private static String formatDirectConfirmationOutcome(
            final PendingConfirmation pending,
            final String rawOutput,
            final boolean isToolError)
    {
        String detail = "";
        if (rawOutput != null && !rawOutput.isBlank())
        {
            try
            {
                final JsonNode root = OBJECT_MAPPER.readTree(rawOutput);
                final String msg = root.path("message").asText("").trim();
                final String status = root.path("status").asText("").trim();
                if (!msg.isEmpty())
                {
                    detail = " " + msg;
                }
                else if (!status.isEmpty())
                {
                    detail = " Status: " + status + ".";
                }
            }
            catch (final Exception e)
            {
                LOGGER.debug("Unable to parse confirmation tool output as JSON: {}", e.getMessage());
            }
        }
        final String prefix = isToolError
                ? "Could not execute `" + pending.toolName() + "` on `" + pending.target() + "`."
                : "Confirmed `" + pending.toolName() + "` for `" + pending.target() + "`.";
        return prefix + detail;
    }

    private ToolCallback[] wrapWithConfirmationBarrier(
            final ToolCallback[] delegates,
            final Map<ConfirmationKey, PendingConfirmation> issuedInCurrentTurn,
            final List<String> thinkingEntries,
            final Predicate<ConfirmationKey> concurrentlyResolvedCheck)
    {
        final ToolCallback[] wrapped = new ToolCallback[delegates.length];
        for (int i = 0; i < delegates.length; i++)
        {
            final ToolCallback delegate = delegates[i];
            final String name = delegate.getToolDefinition().name();
            if (DESTRUCTIVE_TOOL_TARGET_PARAMS.containsKey(name))
            {
                wrapped[i] = new ConfirmationBarrierToolCallback(
                        delegate,
                        issuedInCurrentTurn,
                        thinkingEntries,
                        concurrentlyResolvedCheck,
                        this.clock);
            }
            else
            {
                wrapped[i] = delegate;
            }
        }
        return wrapped;
    }

    private static ToolCallback[] wrapWithTraceObserver(
            final ToolCallback[] delegates,
            final List<String> thinkingEntries,
            final Set<String> uiActions)
    {
        final ToolCallback[] wrapped = new ToolCallback[delegates.length];
        for (int i = 0; i < delegates.length; i++)
        {
            wrapped[i] = new TraceObservingToolCallback(delegates[i], thinkingEntries, uiActions);
        }
        return wrapped;
    }

    private static List<String> snapshotUiActions(final Set<String> uiActions)
    {
        synchronized (uiActions)
        {
            return new ArrayList<>(uiActions);
        }
    }

    private static String mergeTrailingUserPrompt(final List<Message> historyMessages, final String activePrompt)
    {
        if (historyMessages != null && !historyMessages.isEmpty()
                && historyMessages.get(historyMessages.size() - 1) instanceof UserMessage trailingUser)
        {
            historyMessages.remove(historyMessages.size() - 1);
            return trailingUser.getText() + "\n\n" + activePrompt;
        }
        return activePrompt;
    }

    static List<Message> buildHistoryMessages(final List<ChatMessageDto> history)
    {
        final List<Message> messages = new ArrayList<>();
        if (history == null || history.isEmpty())
        {
            return messages;
        }
        for (final ChatMessageDto msg : history)
        {
            if (msg == null || msg.error || msg.content == null || msg.content.isBlank())
            {
                continue;
            }
            if ("user".equalsIgnoreCase(msg.role))
            {
                if (!messages.isEmpty() && messages.get(messages.size() - 1) instanceof UserMessage previousUser)
                {
                    messages.set(messages.size() - 1, new UserMessage(previousUser.getText() + "\n\n" + msg.content));
                }
                else
                {
                    messages.add(new UserMessage(msg.content));
                }
            }
            else if ("ai".equalsIgnoreCase(msg.role) || "assistant".equalsIgnoreCase(msg.role))
            {
                if (!messages.isEmpty() && messages.get(messages.size() - 1) instanceof AssistantMessage previousAi)
                {
                    messages.set(messages.size() - 1, new AssistantMessage(previousAi.getText() + "\n\n" + msg.content));
                }
                else
                {
                    messages.add(new AssistantMessage(msg.content));
                }
            }
        }
        final List<Message> bounded = messages.size() <= MAX_HISTORY_MESSAGES
                ? messages
                : new ArrayList<>(messages.subList(messages.size() - MAX_HISTORY_MESSAGES, messages.size()));
        while (!bounded.isEmpty() && bounded.get(0) instanceof AssistantMessage)
        {
            bounded.remove(0);
        }
        return bounded;
    }

    static void detectUiAction(final String toolName, final String toolOutput, final Set<String> uiActions)
    {
        if (toolName == null || toolOutput == null || toolOutput.isBlank())
        {
            return;
        }
        try
        {
            final JsonNode root = OBJECT_MAPPER.readTree(toolOutput);
            final String status = root.path("status").asText("").trim();
            if (TOOL_DELETE_TEST.equals(toolName) || "create_test".equals(toolName))
            {
                if ("SUCCESS".equalsIgnoreCase(status))
                {
                    uiActions.add("refresh_files");
                }
            }
            else if ("run_test".equals(toolName) || "run_suite".equals(toolName))
            {
                if ("RUNNING".equalsIgnoreCase(status) || "ENQUEUED".equalsIgnoreCase(status))
                {
                    uiActions.add("refresh_runs");
                }
            }
            else if (TOOL_CANCEL_RUN.equals(toolName))
            {
                if ("CANCELLED".equalsIgnoreCase(status))
                {
                    uiActions.add("refresh_runs");
                }
            }
        }
        catch (final Exception e)
        {
            LOGGER.debug("Skipping UI action detection for non-JSON tool output of {}: {}", toolName, e.getMessage());
        }
    }

    private static void appendToolTrace(final List<String> thinkingEntries, final String toolName,
                                        final String toolInput, final String toolOutput)
    {
        final String redactedInput = redactConfirmationToken(toolInput != null ? toolInput : "{}");
        final String redactedOutput = redactConfirmationToken(toolOutput);
        thinkingEntries.add("[Tool Call] " + toolName
                + "\n  Args: " + indentBlock(formatJsonForTrace(redactedInput))
                + "\n  Result: " + indentBlock(formatJsonForTrace(redactedOutput))
                + "\n\n");
    }

    private static String joinTrace(final List<String> thinkingEntries)
    {
        final List<String> snapshot;
        synchronized (thinkingEntries)
        {
            snapshot = new ArrayList<>(thinkingEntries);
        }
        return String.join("", snapshot).trim();
    }

    private static String redactConfirmationToken(final String jsonOrText)
    {
        if (jsonOrText == null || !jsonOrText.contains("confirmationToken"))
        {
            return jsonOrText;
        }
        try
        {
            final JsonNode root = OBJECT_MAPPER.readTree(jsonOrText);
            if (stripConfirmationTokensRecursively(root))
            {
                return OBJECT_MAPPER.writeValueAsString(root);
            }
        }
        catch (final Exception e)
        {
            LOGGER.debug("Could not parse trace payload as JSON during token redaction: {}", e.getMessage());
        }
        return jsonOrText;
    }

    private static boolean stripConfirmationTokensRecursively(final JsonNode node)
    {
        boolean removed = false;
        if (node instanceof ObjectNode obj)
        {
            if (obj.has("confirmationToken"))
            {
                obj.remove("confirmationToken");
                removed = true;
            }
            for (final JsonNode child : obj)
            {
                if (stripConfirmationTokensRecursively(child))
                {
                    removed = true;
                }
            }
        }
        else if (node instanceof ArrayNode array)
        {
            for (final JsonNode item : array)
            {
                if (stripConfirmationTokensRecursively(item))
                {
                    removed = true;
                }
            }
        }
        return removed;
    }

    private static String formatJsonForTrace(final String text)
    {
        if (text == null)
        {
            return "";
        }
        final String trimmed = text.trim();
        if (trimmed.isEmpty())
        {
            return "";
        }
        try
        {
            final JsonNode root = OBJECT_MAPPER.readTree(trimmed);
            if (root != null && (root.isObject() || root.isArray()))
            {
                final String pretty = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                return truncateForTrace(pretty);
            }
        }
        catch (final Exception e)
        {
            LOGGER.debug("Formatting non-JSON trace payload as plain text: {}", e.getMessage());
        }
        return truncateForTrace(trimmed);
    }

    private static String indentBlock(final String formatted)
    {
        return formatted.replace("\r\n", "\n").replace("\n", "\n  ");
    }

    private static String buildContextualUserPrompt(final ChatRequest req)
    {
        final StringBuilder prefix = new StringBuilder();
        if (req.viewContext != null && !req.viewContext.isBlank())
        {
            prefix.append("[Active view context: ").append(req.viewContext.trim()).append("]\n");
        }
        if (req.activeFile != null && !req.activeFile.isBlank())
        {
            prefix.append("[Active editor file: ").append(req.activeFile.trim()).append("]\n");
        }
        return prefix.isEmpty() ? req.prompt : prefix + req.prompt;
    }

    private static String truncateForTrace(final String text)
    {
        if (text == null)
        {
            return "";
        }
        if (text.length() <= MAX_TRACE_LENGTH)
        {
            return text;
        }
        final int newlineCutoff = text.lastIndexOf('\n', MAX_TRACE_LENGTH);
        final int cutoff = newlineCutoff > MAX_TRACE_LENGTH / 2 ? newlineCutoff : MAX_TRACE_LENGTH;
        return text.substring(0, cutoff) + "\n... (truncated)";
    }

    private record ConfirmationKey(String toolName, String target)
    {
    }

    private record PendingConfirmation(String actionId, String toolName, String target, String token, Instant expiresAt)
    {
    }

    private static final class TraceObservingToolCallback implements ToolCallback
    {
        private final ToolCallback delegate;
        private final List<String> thinkingEntries;
        private final Set<String> uiActions;

        private TraceObservingToolCallback(final ToolCallback delegate, final List<String> thinkingEntries,
                                           final Set<String> uiActions)
        {
            this.delegate = delegate;
            this.thinkingEntries = thinkingEntries;
            this.uiActions = uiActions;
        }

        @Override
        public ToolDefinition getToolDefinition()
        {
            return this.delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata()
        {
            return this.delegate.getToolMetadata();
        }

        @Override
        public String call(final String toolInput)
        {
            return invokeObserved(toolInput, null);
        }

        @Override
        public String call(final String toolInput, final ToolContext toolContext)
        {
            return invokeObserved(toolInput, toolContext);
        }

        private String invokeObserved(final String toolInput, final ToolContext toolContext)
        {
            final String toolName = this.delegate.getToolDefinition().name();
            try
            {
                final String output = toolContext != null
                        ? this.delegate.call(toolInput, toolContext)
                        : this.delegate.call(toolInput);
                appendToolTrace(this.thinkingEntries, toolName, toolInput, output);
                detectUiAction(toolName, output, this.uiActions);
                return output;
            }
            catch (final RuntimeException e)
            {
                final String msg = e.getMessage() != null ? e.getMessage() : "Tool execution failed";
                appendToolTrace(this.thinkingEntries, toolName, toolInput, "[ERROR] " + e.getClass().getSimpleName() + ": " + msg);
                throw e;
            }
        }
    }

    private static final class ConfirmationBarrierToolCallback implements ToolCallback
    {
        private final ToolCallback delegate;
        private final Map<ConfirmationKey, PendingConfirmation> issuedInCurrentTurn;
        private final List<String> thinkingEntries;
        private final Predicate<ConfirmationKey> concurrentlyResolvedCheck;
        private final Clock clock;

        private ConfirmationBarrierToolCallback(
                final ToolCallback delegate,
                final Map<ConfirmationKey, PendingConfirmation> issuedInCurrentTurn,
                final List<String> thinkingEntries,
                final Predicate<ConfirmationKey> concurrentlyResolvedCheck,
                final Clock clock)
        {
            this.delegate = delegate;
            this.issuedInCurrentTurn = issuedInCurrentTurn;
            this.thinkingEntries = thinkingEntries;
            this.concurrentlyResolvedCheck = concurrentlyResolvedCheck;
            this.clock = clock;
        }

        @Override
        public ToolDefinition getToolDefinition()
        {
            return this.delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata()
        {
            return this.delegate.getToolMetadata();
        }

        @Override
        public String call(final String toolInput)
        {
            return executeWithBarrier(toolInput, null);
        }

        @Override
        public String call(final String toolInput, final ToolContext toolContext)
        {
            return executeWithBarrier(toolInput, toolContext);
        }

        private String executeWithBarrier(final String toolInput, final ToolContext toolContext)
        {
            final String toolName = this.delegate.getToolDefinition().name();
            final String targetParam = resolveDestructiveTargetParam(toolName);
            final String target = extractJsonField(toolInput, targetParam);
            final ConfirmationKey key = target != null ? new ConfirmationKey(toolName, target) : null;

            if (key != null && this.issuedInCurrentTurn.containsKey(key))
            {
                return blockSameTurnConfirmation(toolName, target);
            }
            if (key != null && this.concurrentlyResolvedCheck.test(key))
            {
                return blockConcurrentlyResolvedConfirmation(toolName, target);
            }

            final String effectiveInput = stripConfirmationToken(toolInput);
            final String rawOutput = toolContext != null
                    ? this.delegate.call(effectiveInput, toolContext)
                    : this.delegate.call(effectiveInput);

            return interceptConfirmationIssuance(toolName, target, rawOutput);
        }

        private String blockConcurrentlyResolvedConfirmation(final String toolName, final String target)
        {
            this.thinkingEntries.add("[Barrier] Blocked stale confirmation for "
                    + toolName + " (" + target + ") already handled via UI\n\n");
            try
            {
                return OBJECT_MAPPER.writeValueAsString(Map.of(
                        "status", "CONFIRMATION_ALREADY_HANDLED",
                        "message", "Pending confirmation for " + toolName
                                + " was already handled in the UI while this request was running. Do not re-issue it unless the user sends a new request."));
            }
            catch (final Exception e)
            {
                LOGGER.debug("Failed serializing CONFIRMATION_ALREADY_HANDLED payload: {}", e.getMessage());
                return "{\"status\":\"CONFIRMATION_ALREADY_HANDLED\"}";
            }
        }

        private String blockSameTurnConfirmation(final String toolName, final String target)
        {
            this.thinkingEntries.add("[Barrier] Blocked same-turn confirmation retry for "
                    + toolName + " (" + target + ")\n\n");
            try
            {
                return OBJECT_MAPPER.writeValueAsString(Map.of(
                        "status", "CONFIRMATION_PENDING_USER",
                        "message", "Confirmation was already requested for " + toolName
                                + ". Instruct the user to click the Confirm or Cancel button in the UI rather than retrying via chat."));
            }
            catch (final Exception e)
            {
                LOGGER.debug("Failed serializing CONFIRMATION_PENDING_USER payload: {}", e.getMessage());
                return "{\"status\":\"CONFIRMATION_PENDING_USER\"}";
            }
        }

        private String interceptConfirmationIssuance(final String toolName, final String target, final String rawOutput)
        {
            if (rawOutput == null || rawOutput.isBlank())
            {
                return rawOutput;
            }
            try
            {
                final JsonNode root = OBJECT_MAPPER.readTree(rawOutput);
                if (root instanceof ObjectNode obj
                        && STATUS_CONFIRMATION_REQUIRED.equalsIgnoreCase(obj.path("status").asText("")))
                {
                    final String issuedToken = obj.path("confirmationToken").asText(null);
                    if (issuedToken != null && !issuedToken.isBlank() && target != null && !target.isBlank())
                    {
                        final String actionId = UUID.randomUUID().toString();
                        this.issuedInCurrentTurn.put(
                                new ConfirmationKey(toolName, target),
                                new PendingConfirmation(
                                        actionId,
                                        toolName,
                                        target,
                                        issuedToken,
                                        this.clock.instant().plusSeconds(CONFIRMATION_TTL_SECONDS)));
                    }
                    obj.remove("confirmationToken");
                    obj.put("message", "Confirmation required for " + toolName + " on '" + target
                            + "'. Ask the user to click the Confirm or Cancel button in the UI (chat messages cannot confirm this action).");
                    return OBJECT_MAPPER.writeValueAsString(obj);
                }
            }
            catch (final Exception e)
            {
                LOGGER.debug("Returning raw tool output when inspecting confirmation payload: {}", e.getMessage());
            }
            return rawOutput;
        }

        private static String extractJsonField(final String json, final String fieldName)
        {
            if (json == null || json.isBlank())
            {
                return null;
            }
            try
            {
                final JsonNode node = OBJECT_MAPPER.readTree(json);
                final JsonNode field = node.get(fieldName);
                if (field != null && field.isTextual())
                {
                    final String rawVal = field.asText();
                    return rawVal.trim().isEmpty() ? null : rawVal;
                }
            }
            catch (final Exception e)
            {
                LOGGER.debug("Ignoring malformed JSON argument while reading field {}: {}", fieldName, e.getMessage());
            }
            return null;
        }

        private static String stripConfirmationToken(final String json)
        {
            if (json == null || json.isBlank() || !json.contains("confirmationToken"))
            {
                return json;
            }
            try
            {
                final JsonNode node = OBJECT_MAPPER.readTree(json);
                if (node instanceof ObjectNode obj && obj.has("confirmationToken"))
                {
                    obj.remove("confirmationToken");
                    return OBJECT_MAPPER.writeValueAsString(obj);
                }
            }
            catch (final Exception e)
            {
                LOGGER.debug("Failed stripping unverified confirmationToken from tool JSON payload: {}", e.getMessage());
            }
            return json;
        }
    }
}
