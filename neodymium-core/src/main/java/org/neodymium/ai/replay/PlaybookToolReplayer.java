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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.LocatorCascadeResolver;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
        if (step == null)
        {
            throw new IllegalArgumentException("PlaybookStep must not be null.");
        }

        final ToolRegistry effectiveRegistry = registry != null ? registry : createDefaultRegistry();
        final ToolContext effectiveContext = context != null ? context : new SimpleToolContext(effectiveRegistry);

        final List<ToolCall> toolCalls = step.getToolCalls();
        if (toolCalls.isEmpty())
        {
            LOGGER.debug("No tool calls recorded for step: {}", step.getInstruction());
            step.setStatus(PlaybookStepStatus.SUCCESS);
            return ToolResult.success(UUID.randomUUID().toString(), "No tool calls recorded for step");
        }

        LOGGER.info("▶ Replaying {} tool calls for step: \"{}\"", toolCalls.size(), step.getInstruction());

        boolean anyHealed = false;

        for (int i = 0; i < toolCalls.size(); i++)
        {
            final ToolCall call = toolCalls.get(i);
            ToolCall effectiveCall = call;

            // Check if tool is registered
            final AiTool tool = effectiveRegistry.getTool(call.toolName())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown tool during replay: " + call.toolName()));

            // Attempt locator self-healing if candidates or DomFeatureVector are available
            final ToolCall healedCall = attemptHealing(call, step, i, effectiveContext);
            if (healedCall != null)
            {
                effectiveCall = healedCall;
                anyHealed = true;
            }

            // Execute the tool without LLM invocation
            final ToolResult result;
            try
            {
                result = tool.execute(effectiveCall, effectiveContext);
            }
            catch (final AssertionError e)
            {
                step.setStatus(PlaybookStepStatus.FAILED);
                step.setFailed(true);
                step.setFailureReason(e.getMessage());
                throw e;
            }
            catch (final Exception e)
            {
                step.setStatus(PlaybookStepStatus.FAILED);
                step.setFailed(true);
                step.setFailureReason(e.getMessage());
                throw e;
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
                throw new RuntimeException("Tool execution error in '" + effectiveCall.toolName() + "': " + result.content());
            }
        }

        if (anyHealed)
        {
            step.setStatus(PlaybookStepStatus.HEALED);
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
        final Integer replays = (Integer) executionContext.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_REPLAYS, 0);
        executionContext.getTransientData().put(ExecutionContext.KEY_TOTAL_REPLAYS, replays + 1);

        return replayStep(step, effectiveRegistry, toolContext);
    }

    private static ToolCall attemptHealing(
            final ToolCall call,
            final PlaybookStep step,
            final int callIndex,
            final ToolContext context)
    {
        final JsonNode args = call.arguments();
        if (args == null || !args.hasNonNull("target"))
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

        // Check for live candidate vectors provided via context variable
        final Object candidateObj = context.getVariable("liveCandidates", Object.class).orElse(null);
        if (candidateObj instanceof final List<?> candidateList && !candidateList.isEmpty()
                && candidateList.get(0) instanceof DomFeatureVector)
        {
            @SuppressWarnings("unchecked")
            final List<DomFeatureVector> liveCandidates = (List<DomFeatureVector>) candidateList;

            // Sub-millisecond similarity healing on CPU
            final DomFeatureVector best = LocatorCascadeResolver.findBestMatch(recordedVector, liveCandidates, 0.70);
            if (best != null)
            {
                final String currentTarget = args.path("target").asText();
                final String healedSelector = resolveSelectorForCandidate(best);
                if (!currentTarget.equals(healedSelector))
                {
                    LOGGER.info("🧬 Healed locator '{}' -> '{}' using DomFeatureVector similarity", currentTarget, healedSelector);
                    final ObjectNode updatedArgs = args.deepCopy();
                    updatedArgs.put("target", healedSelector);
                    return new ToolCall(call.callId(), call.toolName(), updatedArgs);
                }
            }
        }

        return null;
    }

    private static String resolveSelectorForCandidate(final DomFeatureVector candidate)
    {
        if (candidate.getAttributes().containsKey("id"))
        {
            return "#" + candidate.getAttributes().get("id");
        }
        if (!candidate.getClasses().isEmpty())
        {
            return candidate.getTag() + "." + String.join(".", candidate.getClasses());
        }
        return candidate.getTag();
    }

    private static ToolRegistry createDefaultRegistry()
    {
        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);
        return registry;
    }
}
