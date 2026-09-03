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
package org.neodymium.ai.pipeline.steps;

import java.util.ArrayList;
import java.util.List;

import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.event.diagnostic.DiagnosticErrorEvent;
import org.neodymium.ai.event.llm.LlmRequestSentEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.prompt.VisualRcaPrompt;
import org.neodymium.ai.session.AiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline step executing Visual Root Cause Analysis (RCA) on test failure or breakpoint.
 * Captures page screenshots/DOM and queries LLM to diagnose visual/functional failure reasons.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class VisualRcaStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VisualRcaStep.class);

    /** Specific error message or exception context triggering the RCA analysis */
    private final String errorMessage;

    /**
     * Constructs a VisualRcaStep with explicit error message context.
     *
     * @param errorMessage the error message or exception summary
     */
    public VisualRcaStep(final String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    /**
     * Constructs a VisualRcaStep without explicit error message.
     */
    public VisualRcaStep()
    {
        this(null);
    }

    /**
     * Executes this Visual RCA step within the active thread-local execution context.
     *
     * @param context the execution context containing session and SUT state
     * @throws PipelineException if unhandled execution errors occur
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final ExecutionContext previousContext = ExecutionContext.getActiveContext();
        try
        {
            ExecutionContext.setActiveContext(context);
            executeInternal(context);
        }
        finally
        {
            ExecutionContext.setActiveContext(previousContext);
        }
    }

    /**
     * Internal implementation performing visual root cause analysis, logging responses,
     * tracking token consumption, and dispatching diagnostic events.
     *
     * @param context the active execution context
     * @throws PipelineException if unhandled pipeline errors occur
     */
    private void executeInternal(final ExecutionContext context) throws PipelineException
    {
        if (!AiConfiguration.getInstance().isVisualRcaEnabled())
        {
            LOGGER.debug("Visual RCA is disabled via configuration (neodymium.ai.visualRca.enabled=false). Skipping Visual RCA analysis.");
            return;
        }

        // 1. Retrieve the active session instance
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        if (session == null)
        {
            LOGGER.debug("No active session found in context. Skipping Visual RCA analysis.");
            return;
        }

        // 2. Extract current playbook step and current SUT state from transient context
        final PlaybookStep currentStep = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        final SutState lastState = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);

        final String rawInstruction = currentStep != null ? currentStep.getInstruction() : "Unknown step";
        final String instruction = context.getSessionData() != null ? context.getSessionData().resolveVariables(rawInstruction) : rawInstruction;
        final String err = this.errorMessage != null ? this.errorMessage : "Execution error occurred";

        // 3. Construct VisualRcaPrompt template with step instruction and error context
        final VisualRcaPrompt rcaPrompt = new VisualRcaPrompt(instruction, err);
        final String system = rcaPrompt.compileSystemMessage(context);
        final String user = rcaPrompt.compileUserMessage(context);

        LOGGER.debug("Compiling prompt: VisualRcaPrompt");
        LOGGER.trace("System Prompt:\n{}", system);
        LOGGER.trace("User Prompt:\n{}", user);

        // 4. Extract image attachments (screenshots) for multimodal vision analysis
        final List<SutAttachment> imageAttachments = new ArrayList<>();
        if (lastState != null && lastState.getAttachments() != null)
        {
            for (final SutAttachment attachment : lastState.getAttachments())
            {
                if (attachment.mediaType() != null && attachment.mediaType().startsWith("image/"))
                {
                    imageAttachments.add(attachment);
                }
            }
        }

        // 4b. Dynamic Fallback: If no screenshot attachments were in lastState, attempt fresh capture via TargetExecutor
        if (imageAttachments.isEmpty())
        {
            final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
            if (executor != null)
            {
                try
                {
                    final SutState freshState = executor.captureState(ContextLevel.VISUAL_RICH);
                    if (freshState != null && freshState.getAttachments() != null)
                    {
                        for (final SutAttachment attachment : freshState.getAttachments())
                        {
                            if (attachment.mediaType() != null && attachment.mediaType().startsWith("image/"))
                            {
                                imageAttachments.add(attachment);
                            }
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.debug("Fallback state capture during Visual RCA failed or unavailable: {}", e.getMessage());
                }
            }
        }

        // 5. Select VISION capability if images present, otherwise fall back to TEXT_ONLY
        final LlmCapability requiredCap = imageAttachments.isEmpty() ? LlmCapability.TEXT_ONLY : LlmCapability.VISION;
        final LlmProvider provider = session.getLlmRegistry().getProvider(requiredCap);
        if (provider == null)
        {
            LOGGER.debug("No LLM provider registered for capability: {}. Skipping Visual RCA.", requiredCap);
            return;
        }

        // 6. Build LLM chat request for diagnosis
        final LlmRequest request = new LlmRequest(
            system,
            user,
            imageAttachments,
            rcaPrompt.getResponseSchema(),
            0.0,
            60
        );

        try
        {
            // 7. Dispatch diagnosis call and parse natural language RCA diagnosis summary
            LOGGER.debug("Executing Visual RCA via LLM provider '{}'", provider.getClass().getSimpleName());
            session.getEventBus().dispatch(new LlmRequestSentEvent(request, "VISUAL_RCA"));
            final long startTime = System.currentTimeMillis();

            final LlmResponse response = provider.chat(request);
            final long durationMs = System.currentTimeMillis() - startTime;
            session.getEventBus().dispatch(new LlmResponseReceivedEvent(request, response, durationMs, "VISUAL_RCA"));

            LOGGER.debug("Visual RCA LLM response received in {} ms (length: {} chars)",
                durationMs, response != null && response.content() != null ? response.content().length() : 0);
            if (LOGGER.isDebugEnabled() && response != null && response.content() != null)
            {
                LOGGER.debug("   🔍 [Visual RCA Response]:\n{}", response.content());
            }
            if (LOGGER.isTraceEnabled() && response != null)
            {
                LOGGER.trace("Raw response content:\n{}", response.content());
            }

            // 8. Track total LLM calls and RCA call count metrics
            final Integer calls = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_LLM_CALLS, 0);
            context.getTransientData().put(ExecutionContext.KEY_TOTAL_LLM_CALLS, calls + 1);
            final Integer rcaCalls = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_RCA_CALL_COUNT, 0);
            context.getTransientData().put(ExecutionContext.KEY_RCA_CALL_COUNT, rcaCalls + 1);

            // 9. Accumulate token consumption and update StepStats
            final TokenUsage newUsage = response != null ? response.tokenUsage() : null;
            if (newUsage != null)
            {
                LOGGER.debug("   📊 Visual RCA Tokens: {} in ({} cached) → {} out (total: {})",
                    newUsage.inputTokenCount(), newUsage.cachedTokenCount(), newUsage.outputTokenCount(), newUsage.totalTokenCount());

                final TokenUsage existing = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_RCA_TOKEN_USAGE);
                if (existing == null)
                {
                    context.getTransientData().put(ExecutionContext.KEY_RCA_TOKEN_USAGE, newUsage);
                }
                else
                {
                    context.getTransientData().put(ExecutionContext.KEY_RCA_TOKEN_USAGE, new TokenUsage(
                        existing.inputTokenCount() + newUsage.inputTokenCount(),
                        existing.outputTokenCount() + newUsage.outputTokenCount(),
                        existing.totalTokenCount() + newUsage.totalTokenCount(),
                        existing.cachedTokenCount() + newUsage.cachedTokenCount()
                    ));
                }

                final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof StepStats stats)
                {
                    stats.addRcaCall((int) newUsage.inputTokenCount(), (int) newUsage.outputTokenCount(), (int) newUsage.cachedTokenCount());
                }
            }
            else
            {
                final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof StepStats stats)
                {
                    stats.addRcaCall(0, 0, 0);
                }
            }

            final String diagnosis = rcaPrompt.parseResponse(response != null ? response.content() : null, context);

            // 10. Enrich PlaybookStep failure reason if not already populated
            if (currentStep != null && currentStep.getFailureReason() == null)
            {
                currentStep.setFailureReason(diagnosis);
            }

            // 11. Log diagnosis warning and dispatch DiagnosticErrorEvent to event bus
            LOGGER.warn("🚨 [Visual RCA Diagnosis]: {}", diagnosis);
            context.getTransientData().put(ExecutionContext.KEY_VISUAL_RCA_SUMMARY, diagnosis);
            context.getTransientData().put(ExecutionContext.KEY_VISUAL_RCA_EXPLANATION, diagnosis);
            session.getEventBus().dispatch(new DiagnosticErrorEvent("Visual RCA Diagnosis: " + diagnosis, null));
        }
        catch (final Exception e)
        {
            // 12. Graceful fallback on analysis error so RCA never masks underlying test failure
            LOGGER.warn("Failed to perform Visual RCA analysis: {}", e.getMessage(), e);
            context.getTransientData().put(ExecutionContext.KEY_VISUAL_RCA_SUMMARY, "RCA analysis unavailable: " + e.getMessage());
            context.getTransientData().put(ExecutionContext.KEY_VISUAL_RCA_EXPLANATION, "RCA analysis unavailable: " + e.getMessage());
        }
    }
}
