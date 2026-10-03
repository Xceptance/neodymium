/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.replay;

import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.executor.selenide.PageAnalyzer;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.LocatorCascadeResolver;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.steps.AgentToolLoopStep;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Replay engine executing recorded {@link ToolCall}s directly via {@link ToolRegistry}
 * without invoking LLMs. Supports offline sub-millisecond similarity healing and Tier 2
 * perceptual visual dHash matching when selectors shift between application versions.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookToolReplayer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaybookToolReplayer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PlaybookToolReplayer()
    {
    }

    /**
     * Replays all recorded tool calls of a playbook step using the provided registry and context.
     *
     * @param step the playbook step to replay
     * @param registry tool registry containing registered tools
     * @param context tool context for execution
     * @return tool result summarizing replay outcome
     * @throws Exception if execution fails or unrecoverable defect occurs
     */
    public static ToolResult replayStep(
            final PlaybookStep step,
            final ToolRegistry registry,
            final ToolContext context) throws Exception
    {
        return replayStep(step, registry, context, null);
    }

    /**
     * Replays all recorded tool calls of a playbook step using the provided registry, context, and session data.
     *
     * @param step the playbook step to replay
     * @param registry tool registry containing registered tools
     * @param context tool context for execution
     * @param sessionData session data for variable resolution
     * @return tool result summarizing replay outcome
     * @throws Exception if execution fails or unrecoverable defect occurs
     */
    public static ToolResult replayStep(
            final PlaybookStep step,
            final ToolRegistry registry,
            final ToolContext context,
            final SessionData sessionData) throws Exception
    {
        return replayStep(step, registry, context, sessionData, null);
    }

    /**
     * Replays all recorded tool calls of a playbook step using the provided registry, context, session data, and target executor.
     *
     * @param step the playbook step to replay
     * @param registry tool registry containing registered tools
     * @param context tool context for execution
     * @param sessionData session data for variable resolution
     * @param targetExecutor target executor for fallback action dispatch
     * @return tool result summarizing replay outcome
     * @throws Exception if execution fails or unrecoverable defect occurs
     */
    public static ToolResult replayStep(
            final PlaybookStep step,
            final ToolRegistry registry,
            final ToolContext context,
            final SessionData sessionData,
            final TargetExecutor targetExecutor) throws Exception
    {
        if (step == null)
        {
            throw new IllegalArgumentException("PlaybookStep must not be null.");
        }

        final String schemaVersion = step.getSchemaVersion();
        if (schemaVersion == null || !PlaybookStep.CURRENT_SCHEMA_VERSION.equals(schemaVersion.trim()))
        {
            throw new IncompatiblePlaybookSchemaException(schemaVersion, PlaybookStep.CURRENT_SCHEMA_VERSION);
        }

        final ToolRegistry effectiveRegistry = registry != null ? registry : createDefaultRegistry();
        final ToolContext effectiveContext = context != null ? context : new SimpleToolContext(effectiveRegistry);
        final TargetExecutor effectiveExecutor = targetExecutor != null
            ? targetExecutor
            : effectiveContext.getVariable("neodymium.targetExecutor", TargetExecutor.class).orElse(null);

        final List<ToolCall> toolCalls = step.getToolCalls();
        if (toolCalls.isEmpty())
        {
            LOGGER.debug("No tool calls recorded for step: {}", step.getInstruction());
            step.setStatus(PlaybookStepStatus.SUCCESS);
            return ToolResult.success(UUID.randomUUID().toString(), "No tool calls recorded for step");
        }

        LOGGER.info("▶ Replaying {} tool calls for step: \"{}\"", toolCalls.size(), step.getInstruction());

        final ExecutionMode executionMode = effectiveContext.getVariable("neodymium.executionMode", ExecutionMode.class)
                .orElseGet(() -> {
                    final AiSession session = effectiveContext.getVariable("neodymium.session", AiSession.class).orElse(null);
                    if (session != null && session.getExecutionMode() != null)
                    {
                        return session.getExecutionMode();
                    }
                    return AiConfiguration.getInstance().getExecutionMode();
                });

        boolean anyHealed = false;

        for (int i = 0; i < toolCalls.size(); i++)
        {
            final ToolCall rawCall = toolCalls.get(i);
            final ToolCall variableResolvedCall = resolveVariables(rawCall, sessionData);

            // Attempt locator self-healing if candidates or DomFeatureVector are available,
            // strictly bypassing in REPLAY_STRICT or when healing is unsupported
            ToolCall healedCall = null;
            if (executionMode != ExecutionMode.REPLAY_STRICT && (executionMode == null || executionMode.supportsHealing()))
            {
                try
                {
                    healedCall = attemptHealing(variableResolvedCall, step, i, effectiveContext);
                }
                catch (final AssertionError | Exception e)
                {
                    LOGGER.debug("Self-healing evaluation skipped due to exception: {}", e.getMessage());
                }
            }
            final ToolCall intermediateCall;
            if (healedCall != null)
            {
                intermediateCall = healedCall;
                anyHealed = true;
            }
            else
            {
                intermediateCall = variableResolvedCall;
            }

            // Check if tool is complete_step completion marker
            if ("complete_step".equals(intermediateCall.toolName()))
            {
                LOGGER.debug("Skipping completion marker tool 'complete_step' during replay");
                continue;
            }

            final ToolCall finalCall = intermediateCall;

            final AiSession session = effectiveContext.getVariable("neodymium.session", AiSession.class).orElse(null);

            // Check if tool is registered
            final Optional<AiTool> toolOpt = effectiveRegistry.getTool(finalCall.toolName());
            final ToolResult result;
            if (toolOpt.isPresent())
            {
                try
                {
                    result = toolOpt.get().execute(finalCall, effectiveContext);
                    if (session != null && session.getEventBus() != null)
                    {
                        final Action mapped = AgentToolLoopStep.mapToolCallToAction(finalCall);
                        session.getEventBus().dispatch(new ActionExecutedEvent(mapped, true));
                    }
                }
                catch (final AssertionError e)
                {
                    if (session != null && session.getEventBus() != null)
                    {
                        final Action mapped = AgentToolLoopStep.mapToolCallToAction(finalCall);
                        session.getEventBus().dispatch(new ActionExecutedEvent(mapped, false));
                    }
                    step.setStatus(PlaybookStepStatus.FAILED);
                    step.setFailed(true);
                    step.setFailureReason(e.getMessage());
                    throw e;
                }
                catch (final Exception e)
                {
                    if (session != null && session.getEventBus() != null)
                    {
                        final Action mapped = AgentToolLoopStep.mapToolCallToAction(finalCall);
                        session.getEventBus().dispatch(new ActionExecutedEvent(mapped, false));
                    }
                    step.setStatus(PlaybookStepStatus.FAILED);
                    step.setFailed(true);
                    step.setFailureReason(e.getMessage());
                    throw e;
                }
            }
            else if (effectiveExecutor != null)
            {
                final Action mapped = AgentToolLoopStep.mapToolCallToAction(finalCall);
                try
                {
                    effectiveExecutor.execute(mapped);
                    result = ToolResult.success(finalCall.callId(), "Action executed via TargetExecutor");
                    if (session != null && session.getEventBus() != null)
                    {
                        session.getEventBus().dispatch(new ActionExecutedEvent(mapped, true));
                    }
                }
                catch (final AssertionError e)
                {
                    if (session != null && session.getEventBus() != null)
                    {
                        session.getEventBus().dispatch(new ActionExecutedEvent(mapped, false));
                    }
                    step.setStatus(PlaybookStepStatus.FAILED);
                    step.setFailed(true);
                    step.setFailureReason(e.getMessage());
                    throw e;
                }
                catch (final Exception e)
                {
                    if (session != null && session.getEventBus() != null)
                    {
                        session.getEventBus().dispatch(new ActionExecutedEvent(mapped, false));
                    }
                    step.setStatus(PlaybookStepStatus.FAILED);
                    step.setFailed(true);
                    step.setFailureReason(e.getMessage());
                    throw e;
                }
            }
            else
            {
                throw new IllegalArgumentException("Unknown tool during replay: " + finalCall.toolName());
            }

            if (result != null && result.status() == ToolResult.Status.ERROR)
            {
                if (result.content() != null && result.content().startsWith("AssertionError"))
                {
                    step.setStatus(PlaybookStepStatus.FAILED);
                    step.setFailed(true);
                    step.setFailureReason(result.content());
                    throw new AssertionError(result.content());
                }
                throw new RuntimeException("Tool execution error in '" + finalCall.toolName() + "': " + result.content());
            }
        }

        if (anyHealed)
        {
            step.setStatus(PlaybookStepStatus.HEALED);
            step.setSchemaVersion(PlaybookStep.CURRENT_SCHEMA_VERSION);
            LOGGER.info("✨ Step successfully replayed with self-healing: \"{}\"", step.getInstruction());
        }
        else
        {
            step.setStatus(PlaybookStepStatus.SUCCESS);
            LOGGER.info("✅ Step successfully replayed: \"{}\"", step.getInstruction());
        }

        return ToolResult.success(UUID.randomUUID().toString(), "Successfully replayed " + toolCalls.size() + " tool calls");
    }

    /**
     * Replays a step within an {@link ExecutionContext}.
     *
     * @param step playbook step
     * @param registry tool registry
     * @param executionContext pipeline execution context
     * @return tool result
     * @throws Exception on error
     */
    public static ToolResult replayStep(
            final PlaybookStep step,
            final ToolRegistry registry,
            final ExecutionContext executionContext) throws Exception
    {
        final ToolRegistry effectiveRegistry = registry != null ? registry : createDefaultRegistry();
        final ToolContext toolContext = new SimpleToolContext(effectiveRegistry);
        if (executionContext != null)
        {
            final Integer replays = (Integer) executionContext.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_REPLAYS, 0);
            executionContext.getTransientData().put(ExecutionContext.KEY_TOTAL_REPLAYS, replays + 1);
            return replayStep(step, effectiveRegistry, toolContext, executionContext.getSessionData());
        }
        return replayStep(step, effectiveRegistry, toolContext, null);
    }

    private static ToolCall resolveVariables(final ToolCall call, final SessionData sessionData)
    {
        if (sessionData == null || call.arguments() == null || !call.arguments().isObject())
        {
            return call;
        }

        final ObjectNode args = call.arguments().deepCopy();
        final List<String> textKeys = new ArrayList<>();
        final Iterator<String> fieldNames = args.fieldNames();
        while (fieldNames.hasNext())
        {
            final String fieldName = fieldNames.next();
            if (args.get(fieldName).isTextual())
            {
                textKeys.add(fieldName);
            }
        }
        final boolean isStoreTool = call.toolName() != null && "store".equalsIgnoreCase(call.toolName().trim());
        final String storeTargetVar;
        if (isStoreTool)
        {
            if (args.hasNonNull("variableName"))
            {
                storeTargetVar = args.path("variableName").asText().trim();
            }
            else if (args.hasNonNull("variable"))
            {
                storeTargetVar = args.path("variable").asText().trim();
            }
            else if (args.hasNonNull("name"))
            {
                storeTargetVar = args.path("name").asText().trim();
            }
            else if (args.hasNonNull("key"))
            {
                storeTargetVar = args.path("key").asText().trim();
            }
            else
            {
                storeTargetVar = null;
            }
        }
        else
        {
            storeTargetVar = null;
        }

        boolean changed = false;
        for (final String key : textKeys)
        {
            if (isStoreTool && ("variableName".equals(key) || "variable".equals(key) || "name".equals(key) || "key".equals(key)))
            {
                continue;
            }

            final String original = args.get(key).asText();

            if (isStoreTool && storeTargetVar != null && "value".equals(key) && original.equals("${" + storeTargetVar + "}"))
            {
                if (sessionData.get(storeTargetVar) == null)
                {
                    LOGGER.warn("Skipping unresolvable self-referential variable placeholder '{}' in store tool value", original);
                    continue;
                }
            }

            final String resolved = sessionData.resolveVariables(original);
            if (!resolved.equals(original))
            {
                args.put(key, resolved);
                changed = true;
            }
        }
        return changed ? new ToolCall(call.callId(), call.toolName(), args) : call;
    }

    private static ToolCall attemptHealing(
            final ToolCall call,
            final PlaybookStep step,
            final int callIndex,
            final ToolContext context)
    {
        final JsonNode args = call.arguments();
        if (args == null || (!args.hasNonNull("target") && !args.hasNonNull("selector")))
        {
            return null;
        }

        final ExecutionMode executionMode = context != null
                ? context.getVariable("neodymium.executionMode", ExecutionMode.class).orElse(null)
                : null;
        if (executionMode == ExecutionMode.REPLAY_STRICT || (executionMode != null && !executionMode.supportsHealing()))
        {
            return null;
        }

        // Only interactive / actionable tools targeting an element can be healed.
        // Read-only inspection / navigation tools must never trigger locator healing.
        final String toolName = call.toolName() != null ? call.toolName().trim().toLowerCase() : "";
        if ("query_dom".equals(toolName) || "inspect_element".equals(toolName) || "execute_script".equals(toolName)
                || "take_screenshot".equals(toolName) || "screenshot".equals(toolName) || "navigate".equals(toolName)
                || "browser_navigate".equals(toolName) || "get_page_source".equals(toolName))
        {
            return null;
        }

        final String currentTarget = args.hasNonNull("target") ? args.path("target").asText().trim() : args.path("selector").asText().trim();
        if (currentTarget.isBlank() || currentTarget.startsWith("coord:"))
        {
            return null;
        }

        // Check if DomFeatureVector is recorded
        DomFeatureVector recordedVector = null;
        if (callIndex < step.getActions().size())
        {
            final Action matchingAction = step.getActions().get(callIndex);
            if (matchingAction != null)
            {
                recordedVector = matchingAction.getDomFeatureVector();
            }
        }

        if (recordedVector == null && args.has("domFeatureVector"))
        {
            try
            {
                recordedVector = MAPPER.treeToValue(args.path("domFeatureVector"), DomFeatureVector.class);
            }
            catch (final Exception ignored)
            {
            }
        }

        if (recordedVector == null)
        {
            return null;
        }

        // If the current target is already directly present and visible on the live page,
        // no healing is needed. Never overwrite or corrupt an active, working locator.
        try
        {
            if (WebDriverRunner.hasWebDriverStarted() && SelenideElementFinder.isDirectlyPresent(currentTarget))
            {
                return null;
            }
        }
        catch (final AssertionError | Exception ignored)
        {
        }

        // Optional steps (e.g. dismissible modals, cookie banners) must not perform heavy full-DOM healing scans
        if (step != null && step.isOptional())
        {
            return null;
        }

        // Check for live candidate vectors provided via context variable or dynamically extracted from active page
        List<DomFeatureVector> liveCandidates = null;
        final Object candidateObj = context != null ? context.getVariable("liveCandidates", Object.class).orElse(null) : null;
        if (candidateObj instanceof final List<?> candidateList && !candidateList.isEmpty()
                && candidateList.get(0) instanceof DomFeatureVector)
        {
            @SuppressWarnings("unchecked")
            final List<DomFeatureVector> cast = (List<DomFeatureVector>) candidateList;
            liveCandidates = cast;
        }
        else if (WebDriverRunner.hasWebDriverStarted())
        {
            // For standard CSS or ID locators, do a brief poll (up to 300ms) in case an async element (e.g. dropdown) is currently rendering
            if (!currentTarget.contains("data-ai=") && !currentTarget.contains("#xc"))
            {
                final long quickPollStart = System.currentTimeMillis();
                while (System.currentTimeMillis() - quickPollStart < 300)
                {
                    try
                    {
                        Thread.sleep(50);
                    }
                    catch (final InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (SelenideElementFinder.isDirectlyPresent(currentTarget))
                    {
                        return null;
                    }
                }
            }

            try
            {
                liveCandidates = new PageAnalyzer().extractFeatureVectors(WebDriverRunner.getWebDriver());
            }
            catch (final Exception ignored)
            {
            }
        }

        if (liveCandidates != null && !liveCandidates.isEmpty())
        {
            // Sub-millisecond similarity healing on CPU
            final DomFeatureVector best = LocatorCascadeResolver.findBestMatch(recordedVector, liveCandidates, 0.70);
            if (best != null)
            {
                final String healedSelector = resolveSelectorForCandidate(best);
                if (healedSelector != null && !currentTarget.equals(healedSelector))
                {
                    LOGGER.info("🧬 Healed locator '{}' -> '{}' using DomFeatureVector similarity", currentTarget, healedSelector);
                    final ObjectNode updatedArgs = args.deepCopy();
                    if (args.hasNonNull("selector"))
                    {
                        updatedArgs.put("selector", healedSelector);
                    }
                    if (args.hasNonNull("target"))
                    {
                        updatedArgs.put("target", healedSelector);
                    }
                    return new ToolCall(call.callId(), call.toolName(), updatedArgs);
                }
            }
        }

        return null;
    }

    /**
     * Derives a selector for a healed candidate element, in decreasing order of stability.
     * <p>
     * The result is written back into the replayed tool call, so it has to match the candidate: every
     * attribute value is escaped, and no selector is invented from data the element does not carry.
     * In particular the computed accessible name is <em>not</em> turned into an {@code aria-label}
     * attribute selector (it usually comes from the element text or a label, so such a selector never
     * matches), and raw viewport coordinates are never produced because they are not an element identity.
     *
     * @param candidate the best matching live element
     * @return a CSS-style selector for the candidate, or {@code null} if none can be derived
     */
    static String resolveSelectorForCandidate(final DomFeatureVector candidate)
    {
        if (candidate.getAttributes().containsKey("id") && !candidate.getAttributes().get("id").isBlank())
        {
            final String idVal = candidate.getAttributes().get("id").trim();
            if (isStandardCssIdentifier(idVal))
            {
                return "#" + idVal;
            }
            final String tag = candidate.getTag() != null && !candidate.getTag().isBlank() ? candidate.getTag() : "";
            return tag + "[id=\"" + escapeAttributeValue(idVal) + "\"]";
        }
        if (candidate.getAttributes().containsKey("data-testid") && !candidate.getAttributes().get("data-testid").isBlank())
        {
            return candidate.getTag() + "[data-testid=\"" + escapeAttributeValue(candidate.getAttributes().get("data-testid")) + "\"]";
        }
        if (candidate.getAttributes().containsKey("name") && !candidate.getAttributes().get("name").isBlank())
        {
            return candidate.getTag() + "[name=\"" + escapeAttributeValue(candidate.getAttributes().get("name")) + "\"]";
        }

        final List<String> validClasses = candidate.getClasses() != null
                ? candidate.getClasses().stream()
                        .filter(PlaybookToolReplayer::isStandardCssIdentifier)
                        .toList()
                : List.of();

        if (candidate.getText() != null && !candidate.getText().isBlank())
        {
            final String cleanText = escapeAttributeValue(candidate.getText().trim());
            final String base = !validClasses.isEmpty()
                    ? candidate.getTag() + "." + String.join(".", validClasses)
                    : candidate.getTag();
            return base + ":has-text(\"" + cleanText + "\")";
        }
        if (candidate.getAttributes().containsKey("aria-label") && !candidate.getAttributes().get("aria-label").isBlank())
        {
            return candidate.getTag() + "[aria-label=\"" + escapeAttributeValue(candidate.getAttributes().get("aria-label")) + "\"]";
        }
        if (!validClasses.isEmpty())
        {
            return candidate.getTag() + "." + String.join(".", validClasses);
        }
        return candidate.getTag().isBlank() ? null : candidate.getTag();
    }

    /**
     * Checks whether the given string is a standard, unescaped CSS identifier (letters, digits,
     * underscores, and hyphens, not starting with a digit or a hyphen followed by a digit).
     *
     * @param identifier candidate identifier string
     * @return {@code true} if safe to use directly without CSS escaping
     */
    private static boolean isStandardCssIdentifier(final String identifier)
    {
        return identifier != null && identifier.matches("^-?[a-zA-Z_][a-zA-Z0-9_-]*$");
    }

    /**
     * Escapes a value for use inside a double-quoted CSS attribute selector string.
     * Backslashes and double quotes are escaped; line breaks, which cannot appear unescaped in a CSS
     * string, are replaced by a space.
     *
     * @param value the raw attribute value
     * @return the value safe to embed between double quotes
     */
    private static String escapeAttributeValue(final String value)
    {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
    }

    private static ToolRegistry createDefaultRegistry()
    {
        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);
        return registry;
    }
}
