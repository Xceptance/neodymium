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

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
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
import org.neodymium.ai.executor.selenide.plugins.ClickAction;
import org.neodymium.ai.executor.selenide.plugins.ClickAction.CoordinateTarget;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.LocatorCascadeResolver;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
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
import org.neodymium.ai.tool.browser.ReanchoringBridge;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

        final AiSession session = effectiveContext.getVariable("neodymium.session", AiSession.class).orElse(null);
        final ExecutionMode executionMode = effectiveContext.getVariable("neodymium.executionMode", ExecutionMode.class)
                .orElseGet(() -> {
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
            final boolean isToolHealed = healedCall != null;

            if (executionMode == ExecutionMode.REPLAY_STRICT)
            {
                verifyStrictReplayGuards(finalCall, step, i);
            }

            // Check if tool is registered
            final Optional<AiTool> toolOpt = effectiveRegistry.getTool(finalCall.toolName());
            final ToolResult result;
            if (toolOpt.isPresent())
            {
                try
                {
                    result = toolOpt.get().execute(finalCall, effectiveContext);
                    dispatchActionEvent(session, variableResolvedCall, finalCall, isToolHealed, true);
                }
                catch (final AssertionError e)
                {
                    dispatchActionEvent(session, variableResolvedCall, finalCall, isToolHealed, false);
                    step.setStatus(PlaybookStepStatus.FAILED);
                    step.setFailed(true);
                    step.setFailureReason(e.getMessage());
                    if (PlaybookStep.isAssertionToolName(finalCall.toolName()) || (step != null && step.isAssertionStep()))
                    {
                        throw new ConclusiveFailureException("Assertion failed during replay: " + e.getMessage(), e);
                    }
                    throw e;
                }
                catch (final Exception e)
                {
                    dispatchActionEvent(session, variableResolvedCall, finalCall, isToolHealed, false);
                    step.setStatus(PlaybookStepStatus.FAILED);
                    step.setFailed(true);
                    step.setFailureReason(e.getMessage());
                    if (PlaybookStep.isAssertionToolName(finalCall.toolName()) || (step != null && step.isAssertionStep()))
                    {
                        throw new ConclusiveFailureException("Assertion failed during replay: " + e.getMessage(), e);
                    }
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
                    dispatchActionEvent(session, variableResolvedCall, finalCall, isToolHealed, true);
                }
                catch (final AssertionError e)
                {
                    dispatchActionEvent(session, variableResolvedCall, finalCall, isToolHealed, false);
                    step.setStatus(PlaybookStepStatus.FAILED);
                    step.setFailed(true);
                    step.setFailureReason(e.getMessage());
                    if (PlaybookStep.isAssertionToolName(finalCall.toolName()) || (step != null && step.isAssertionStep()))
                    {
                        throw new ConclusiveFailureException("Assertion failed during replay: " + e.getMessage(), e);
                    }
                    throw e;
                }
                catch (final Exception e)
                {
                    dispatchActionEvent(session, variableResolvedCall, finalCall, isToolHealed, false);
                    step.setStatus(PlaybookStepStatus.FAILED);
                    step.setFailed(true);
                    step.setFailureReason(e.getMessage());
                    if (PlaybookStep.isAssertionToolName(finalCall.toolName()) || (step != null && step.isAssertionStep()))
                    {
                        throw new ConclusiveFailureException("Assertion failed during replay: " + e.getMessage(), e);
                    }
                    throw e;
                }
            }
            else
            {
                throw new IllegalArgumentException("Unknown tool during replay: " + finalCall.toolName());
            }

            if (result != null && result.status() == ToolResult.Status.ERROR)
            {
                step.setStatus(PlaybookStepStatus.FAILED);
                step.setFailed(true);
                step.setFailureReason(result.content());
                if (PlaybookStep.isAssertionToolName(finalCall.toolName()) || (step != null && step.isAssertionStep()))
                {
                    throw new ConclusiveFailureException("Assertion failed during replay: " + result.content());
                }
                if (result.content() != null && result.content().startsWith("AssertionError"))
                {
                    throw new AssertionError(result.content());
                }
                throw new RuntimeException("Tool execution error in '" + finalCall.toolName() + "': " + result.content());
            }
        }

        final boolean isTransientHealed = session != null && session.getExecutionContext() != null
                && Boolean.TRUE.equals(session.getExecutionContext().getTransientData().get(ExecutionContext.KEY_IS_HEALED_STEP));
        if (anyHealed || step.getStatus() == PlaybookStepStatus.HEALED || isTransientHealed)
        {
            step.setStatus(PlaybookStepStatus.HEALED);
            step.setSchemaVersion(PlaybookStep.CURRENT_SCHEMA_VERSION);
            if (session != null && session.getExecutionContext() != null && session.getExecutionContext().getTransientData() != null)
            {
                session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_IS_HEALED_STEP, Boolean.TRUE);
            }
            LOGGER.info("✨ Step successfully replayed with self-healing: \"{}\"", step.getInstruction());
        }
        else
        {
            step.setStatus(PlaybookStepStatus.SUCCESS);
            LOGGER.info("✅ Step successfully replayed: \"{}\"", step.getInstruction());
        }

        return ToolResult.success(UUID.randomUUID().toString(), "Successfully replayed " + toolCalls.size() + " tool calls");
    }

    private static void dispatchActionEvent(
        final AiSession session,
        final ToolCall canonicalCall,
        final ToolCall finalCall,
        final boolean isHealed,
        final boolean success
    )
    {
        if (session != null && session.getEventBus() != null)
        {
            final Action resolvedAction = AgentToolLoopStep.mapToolCallToAction(finalCall);
            if (isHealed && canonicalCall != null)
            {
                final Action canonicalAction = AgentToolLoopStep.mapToolCallToAction(canonicalCall);
                session.getEventBus().dispatch(new ActionExecutedEvent(canonicalAction, resolvedAction, success, null, true));
            }
            else
            {
                session.getEventBus().dispatch(new ActionExecutedEvent(resolvedAction, success));
            }
        }
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
        final String toolName = call.toolName() != null ? call.toolName().trim().toLowerCase(Locale.ROOT) : "";
        if ("query_dom".equals(toolName) || "inspect_element".equals(toolName) || "execute_script".equals(toolName)
                || "take_screenshot".equals(toolName) || "screenshot".equals(toolName) || "navigate".equals(toolName)
                || "browser_navigate".equals(toolName) || "get_page_source".equals(toolName)
                || "assert_url".equals(toolName) || "browser_assert_url".equals(toolName)
                || "assert_title".equals(toolName) || "browser_assert_title".equals(toolName)
                || "assert_count".equals(toolName) || "browser_assert_count".equals(toolName))
        {
            return null;
        }

        final String currentTarget = args.hasNonNull("target") ? args.path("target").asText().trim() : args.path("selector").asText().trim();
        final boolean isCoordinateTarget = currentTarget.startsWith("coord:")
                || (args.hasNonNull("x") && args.hasNonNull("y") && (!args.hasNonNull("selector") || args.path("selector").asText().isBlank()));
        if (currentTarget.isBlank() && !isCoordinateTarget)
        {
            return null;
        }

        // Check if DomFeatureVector is recorded
        final DomFeatureVector recordedVector = extractRecordedVector(args, step, callIndex);
        if (recordedVector == null)
        {
            return null;
        }

        if (isCoordinateTarget)
        {
            if (WebDriverRunner.hasWebDriverStarted())
            {
                final int[] resolvedCoords = resolveCoordinatePoint(args, currentTarget);
                final int x = resolvedCoords[0];
                final int y = resolvedCoords[1];
                final WebDriver driver = WebDriverRunner.getWebDriver();
                final ReanchoringBridge.ReanchoredElement liveEl = ReanchoringBridge.resolveElementAtPoint(driver, x, y);
                if (matchesRecordedIdentity(liveEl, recordedVector))
                {
                    return null;
                }
            }
            else if (context == null || context.getVariable("liveCandidates", Object.class).isEmpty())
            {
                return null;
            }
        }
        else
        {
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
                if (PlaybookStep.isAssertionToolName(toolName) && !candidateMatchesAssertionExpectation(best, toolName, args))
                {
                    LOGGER.info("🚫 Rejected locator healing for assertion tool '{}': candidate element did not match assertion criteria", toolName);
                    return null;
                }
                final String healedSelector = resolveSelectorForCandidate(best);
                final ObjectNode updatedArgs = args.deepCopy();
                if (healedSelector != null && !currentTarget.equals(healedSelector) && (!isCoordinateTarget || !isBareGenericTag(healedSelector)))
                {
                    LOGGER.info("🧬 Healed locator '{}' -> '{}' using DomFeatureVector similarity", currentTarget, healedSelector);
                    if (args.hasNonNull("selector") || isCoordinateTarget)
                    {
                        updatedArgs.put("selector", healedSelector);
                    }
                    if (args.hasNonNull("target") || isCoordinateTarget)
                    {
                        updatedArgs.put("target", healedSelector);
                    }
                    if (isCoordinateTarget)
                    {
                        updatedArgs.remove("x");
                        updatedArgs.remove("y");
                    }
                    return new ToolCall(call.callId(), call.toolName(), updatedArgs);
                }
                else if (isCoordinateTarget)
                {
                    final int newX = best.getX() + (best.getWidth() / 2);
                    final int newY = best.getY() + (best.getHeight() / 2);
                    final String newTarget = "coord: " + newX + "," + newY;
                    LOGGER.info("🧬 Relocated coordinate target '{}' -> '{}' using DomFeatureVector similarity", currentTarget, newTarget);
                    updatedArgs.put("target", newTarget);
                    updatedArgs.put("x", newX);
                    updatedArgs.put("y", newY);
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
    private static boolean isBareGenericTag(final String selector)
    {
        if (selector == null)
        {
            return true;
        }
        final String s = selector.trim().toLowerCase();
        return "div".equals(s) || "span".equals(s) || "section".equals(s) || "p".equals(s)
                || "li".equals(s) || "ul".equals(s) || "ol".equals(s) || "body".equals(s);
    }

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

    private static void verifyStrictReplayGuards(final ToolCall call, final PlaybookStep step, final int callIndex)
    {
        final String toolName = call.toolName() != null ? call.toolName().trim().toLowerCase() : "";
        if (!"click".equals(toolName) && !"browser_click".equals(toolName))
        {
            return;
        }

        final JsonNode args = call.arguments();
        if (args == null)
        {
            return;
        }

        final String target = args.hasNonNull("target") ? args.path("target").asText().trim() : "";
        final boolean isCoord = target.startsWith("coord:")
                || (args.hasNonNull("x") && args.hasNonNull("y") && (!args.hasNonNull("selector") || args.path("selector").asText().isBlank()));
        if (!isCoord)
        {
            return;
        }

        final DomFeatureVector recordedVector = extractRecordedVector(args, step, callIndex);
        if (recordedVector != null && WebDriverRunner.hasWebDriverStarted())
        {
            final int[] resolvedCoords = resolveCoordinatePoint(args, target);
            final int x = resolvedCoords[0];
            final int y = resolvedCoords[1];
            final WebDriver driver = WebDriverRunner.getWebDriver();
            final ReanchoringBridge.ReanchoredElement liveEl = ReanchoringBridge.resolveElementAtPoint(driver, x, y);
            if (!matchesRecordedIdentity(liveEl, recordedVector))
            {
                final String actualIdentity = liveEl != null
                        ? liveEl.tagName() + (liveEl.text().isBlank() ? "" : " ['" + liveEl.text() + "']")
                        : "none";
                final String expectedIdentity = recordedVector.getTag()
                        + (recordedVector.getText() == null || recordedVector.getText().isBlank() ? "" : " ['" + recordedVector.getText() + "']");
                throw new AssertionError("Coordinate click target drifted in REPLAY_STRICT at (" + x + ", " + y + "): expected "
                        + expectedIdentity + ", but found " + actualIdentity + " on the live page");
            }
        }
    }

    static DomFeatureVector extractRecordedVector(final JsonNode args, final PlaybookStep step, final int callIndex)
    {
        DomFeatureVector recordedVector = null;
        if (step != null && callIndex >= 0 && callIndex < step.getActions().size())
        {
            final Action matchingAction = step.getActions().get(callIndex);
            if (matchingAction != null)
            {
                recordedVector = matchingAction.getDomFeatureVector();
            }
        }
        if (recordedVector == null && args != null && args.has("domFeatureVector"))
        {
            try
            {
                recordedVector = MAPPER.treeToValue(args.path("domFeatureVector"), DomFeatureVector.class);
            }
            catch (final Exception ignored)
            {
            }
        }
        return recordedVector;
    }

    static int[] resolveCoordinatePoint(final JsonNode args, final String target)
    {
        final CoordinateTarget coordTarget = ClickAction.parseCoordinateTarget(target);
        if (coordTarget != null && coordTarget.anchorSelector() != null && !coordTarget.anchorSelector().isBlank())
        {
            try
            {
                final SelenideElement anchorEl = SelenideElementFinder.findElement(coordTarget.anchorSelector());
                if (anchorEl != null && anchorEl.exists())
                {
                    SelenideElementFinder.scrollIntoViewIfNeeded(anchorEl);
                    final Object rectObj = Selenide.executeJavaScript(
                        "var r = arguments[0].getBoundingClientRect(); return [Math.round(r.left), Math.round(r.top)];",
                        anchorEl);
                    if (rectObj instanceof List<?> list && list.size() >= 2)
                    {
                        final int cx = ((Number) list.get(0)).intValue() + coordTarget.x();
                        final int cy = ((Number) list.get(1)).intValue() + coordTarget.y();
                        return new int[]{cx, cy};
                    }
                }
            }
            catch (final Exception | AssertionError ignored)
            {
            }
            return new int[]{coordTarget.x(), coordTarget.y()};
        }
        final int x = extractCoordinate(args, "x", target, 0);
        final int y = extractCoordinate(args, "y", target, 1);
        return new int[]{x, y};
    }

    static int extractCoordinate(final JsonNode args, final String prop, final String target, final int index)
    {
        final CoordinateTarget coordTarget = ClickAction.parseCoordinateTarget(target);
        if (coordTarget != null)
        {
            return index == 0 ? coordTarget.x() : coordTarget.y();
        }
        if (args != null && args.hasNonNull(prop))
        {
            return args.path(prop).asInt();
        }
        if (target != null && target.startsWith("coord:"))
        {
            final String[] parts = target.substring(6).split(",");
            if (parts.length > index)
            {
                try
                {
                    return Integer.parseInt(parts[index].trim());
                }
                catch (final NumberFormatException ignored)
                {
                }
            }
        }
        return 0;
    }

    static boolean matchesRecordedIdentity(final ReanchoringBridge.ReanchoredElement liveEl, final DomFeatureVector recordedVector)
    {
        if (liveEl == null || recordedVector == null)
        {
            return false;
        }
        final String liveTag = liveEl.tagName();
        final String recordedTag = recordedVector.getTag();
        if (liveTag == null || recordedTag == null || !liveTag.equalsIgnoreCase(recordedTag))
        {
            return false;
        }
        final String recordedText = recordedVector.getText();
        if (recordedText != null && !recordedText.isBlank())
        {
            final String liveText = liveEl.text();
            if (liveText == null || liveText.isBlank())
            {
                return false;
            }
            final String normRecorded = recordedText.trim().toLowerCase();
            final String normLive = liveText.trim().toLowerCase();
            return normLive.contains(normRecorded) || normRecorded.contains(normLive);
        }
        return true;
    }

    private static boolean candidateMatchesAssertionExpectation(
            final DomFeatureVector candidate,
            final String toolName,
            final JsonNode args)
    {
        if (candidate == null || toolName == null || args == null)
        {
            return false;
        }

        final String cleanTool = stripNamespacePrefix(toolName).toLowerCase(Locale.ROOT);

        if ("assert_text".equals(cleanTool) || "browser_assert_text".equals(cleanTool))
        {
            final String rawExpectedText = args.hasNonNull("expectedText")
                    ? args.path("expectedText").asText()
                    : args.path("text").asText("");
            final boolean exact = args.path("exact").asBoolean(false);
            final boolean regex = args.path("regex").asBoolean(false);
            final boolean negated = args.path("negated").asBoolean(false)
                    || args.path("not").asBoolean(false)
                    || args.path("invert").asBoolean(false);

            final List<String> textCandidates = new ArrayList<>();
            if (candidate.getText() != null && !candidate.getText().isBlank())
            {
                textCandidates.add(candidate.getText());
            }
            if (candidate.getAccessibleName() != null && !candidate.getAccessibleName().isBlank())
            {
                textCandidates.add(candidate.getAccessibleName());
            }
            final String valAttr = candidate.getAttributes().get("value");
            if (valAttr != null && !valAttr.isBlank())
            {
                textCandidates.add(valAttr);
            }
            final String placeholder = candidate.getAttributes().get("placeholder");
            if (placeholder != null && !placeholder.isBlank())
            {
                textCandidates.add(placeholder);
            }
            final String title = candidate.getAttributes().get("title");
            if (title != null && !title.isBlank())
            {
                textCandidates.add(title);
            }

            boolean matched = false;
            if (regex)
            {
                Pattern pattern;
                try
                {
                    pattern = Pattern.compile(rawExpectedText, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                }
                catch (final PatternSyntaxException e)
                {
                    pattern = Pattern.compile(Pattern.quote(rawExpectedText), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                }
                for (final String text : textCandidates)
                {
                    if (pattern.matcher(text).find() || pattern.matcher(text.replaceAll("\\s+", " ")).find())
                    {
                        matched = true;
                        break;
                    }
                }
            }
            else
            {
                final String normExpected = rawExpectedText.replaceAll("\\s+", " ").trim();
                for (final String text : textCandidates)
                {
                    if (exact)
                    {
                        if (text.trim().equalsIgnoreCase(rawExpectedText.trim())
                                || text.replaceAll("\\s+", " ").trim().equalsIgnoreCase(normExpected))
                        {
                            matched = true;
                            break;
                        }
                    }
                    else
                    {
                        if (text.toLowerCase(Locale.ROOT).contains(rawExpectedText.toLowerCase(Locale.ROOT))
                                || text.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT).contains(normExpected.toLowerCase(Locale.ROOT)))
                        {
                            matched = true;
                            break;
                        }
                    }
                }
            }

            return negated ? !matched : matched;
        }

        if ("assert_attribute".equals(cleanTool) || "browser_assert_attribute".equals(cleanTool))
        {
            final String attrName = args.hasNonNull("attribute")
                    ? args.path("attribute").asText()
                    : args.path("attributeName").asText("");
            if (attrName.isBlank())
            {
                return false;
            }

            final boolean hasExpectedVal = args.hasNonNull("expectedValue") || args.hasNonNull("value");
            final String expectedVal = args.hasNonNull("expectedValue")
                    ? args.path("expectedValue").asText()
                    : args.path("value").asText("");
            final boolean exact = args.path("exact").asBoolean(false);
            final boolean regex = args.path("regex").asBoolean(false);
            final boolean negated = args.path("negated").asBoolean(false)
                    || args.path("not").asBoolean(false);

            String actualVal = null;
            for (final Map.Entry<String, String> entry : candidate.getAttributes().entrySet())
            {
                if (entry.getKey().equalsIgnoreCase(attrName))
                {
                    actualVal = entry.getValue();
                    break;
                }
            }

            if (actualVal == null)
            {
                return negated;
            }

            if (!hasExpectedVal)
            {
                return !negated;
            }

            boolean matched = false;
            if (regex)
            {
                Pattern pattern;
                try
                {
                    pattern = Pattern.compile(expectedVal, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                }
                catch (final PatternSyntaxException e)
                {
                    pattern = Pattern.compile(Pattern.quote(expectedVal), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                }
                matched = pattern.matcher(actualVal).find();
            }
            else if (exact)
            {
                matched = actualVal.equalsIgnoreCase(expectedVal);
            }
            else
            {
                matched = actualVal.toLowerCase(Locale.ROOT).contains(expectedVal.toLowerCase(Locale.ROOT));
            }

            return negated ? !matched : matched;
        }

        if ("assert_element_state".equals(cleanTool) || "browser_assert_element_state".equals(cleanTool))
        {
            final List<String> states = new ArrayList<>();
            if (args.has("states") && args.path("states").isArray())
            {
                for (final JsonNode s : args.path("states"))
                {
                    if (s.isTextual() && !s.asText().isBlank())
                    {
                        states.add(s.asText().trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
            if (states.isEmpty())
            {
                final String rawState = args.hasNonNull("state")
                        ? args.path("state").asText()
                        : (args.hasNonNull("expectedState")
                                ? args.path("expectedState").asText()
                                : args.path("value").asText(""));
                for (final String part : rawState.split("[,&]|\\band\\b"))
                {
                    final String trimmed = part.trim().toLowerCase(Locale.ROOT);
                    if (!trimmed.isBlank())
                    {
                        states.add(trimmed);
                    }
                }
            }

            final boolean negated = args.path("negated").asBoolean(false)
                    || args.path("not").asBoolean(false);

            for (final String state : states)
            {
                final boolean satisfies = switch (state)
                {
                    case "visible", "displayed" -> candidate.getWidth() > 0 && candidate.getHeight() > 0;
                    case "hidden", "invisible" -> candidate.getWidth() <= 0 || candidate.getHeight() <= 0;
                    case "disabled" -> candidate.getAttributes().containsKey("disabled")
                            || "true".equalsIgnoreCase(candidate.getAttributes().get("aria-disabled"));
                    case "enabled" -> !candidate.getAttributes().containsKey("disabled")
                            && !"true".equalsIgnoreCase(candidate.getAttributes().get("aria-disabled"));
                    case "checked" -> candidate.getAttributes().containsKey("checked")
                            || "true".equalsIgnoreCase(candidate.getAttributes().get("aria-checked"));
                    case "unchecked" -> !candidate.getAttributes().containsKey("checked")
                            && !"true".equalsIgnoreCase(candidate.getAttributes().get("aria-checked"));
                    case "readonly" -> candidate.getAttributes().containsKey("readonly")
                            || "true".equalsIgnoreCase(candidate.getAttributes().get("aria-readonly"));
                    case "editable" -> !candidate.getAttributes().containsKey("readonly")
                            && !candidate.getAttributes().containsKey("disabled");
                    case "exists" -> true;
                    case "absent" -> false;
                    default -> true;
                };

                final boolean stateMatched = negated ? !satisfies : satisfies;
                if (!stateMatched)
                {
                    return false;
                }
            }
            return true;
        }

        // For any other assertion tools, do not allow healing without exact validation
        return false;
    }

    private static String stripNamespacePrefix(final String toolName)
    {
        if (toolName == null)
        {
            return "";
        }
        final int colonIdx = toolName.indexOf(':');
        return colonIdx >= 0 ? toolName.substring(colonIdx + 1) : toolName;
    }
}
