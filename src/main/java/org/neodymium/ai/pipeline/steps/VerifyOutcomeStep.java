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

import java.util.List;

import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.prompt.VerificationPrompt;
import org.neodymium.ai.prompt.VerificationResult;
import org.neodymium.ai.session.AiSession;

/**
 * Pipeline step executed after ExecuteActionsStep. Evaluates step execution
 * outcome by performing a dual-state semantic validation comparison if enabled.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerifyOutcomeStep implements PipelineStep
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(VerifyOutcomeStep.class);

    /**
     * Constructs a default VerifyOutcomeStep.
     */
    public VerifyOutcomeStep()
    {
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        // 1. Check if semantic verification is enabled in configuration
        final AiConfiguration config = new AiConfiguration();
        if (!config.isSemanticVerificationEnabled())
        {
            LOGGER.debug("Semantic outcome verification is disabled in configuration. Skipping step.");
            return;
        }

        // 2. Retrieve required context instances from transient execution data map
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);

        // Fail conclusively if essential session runtime structures are missing
        if (session == null)
        {
            throw new ConclusiveFailureException("No active AiSession registered in ExecutionContext transient data");
        }
        if (executor == null)
        {
            throw new ConclusiveFailureException("No active TargetExecutor registered in ExecutionContext transient data");
        }

        // 3. Evaluate replay mode and skip verification if current step was replayed without active modification
        final org.neodymium.ai.config.ExecutionMode mode = (org.neodymium.ai.config.ExecutionMode) context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
        final PlaybookStep step = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        final boolean stepWasReplayed = mode != null && mode.isReplay() && (step == null || !step.isNoReplay());

        if (mode == null || stepWasReplayed)
        {
            LOGGER.debug("Step was replayed from baseline. Bypassing semantic outcome verification.");
            return;
        }

        LOGGER.debug("================================================================================");
        LOGGER.debug("🔍 [Optional AI Outcome Verification]");
        LOGGER.debug("       Instruction: \"{}\"", step != null ? step.getInstruction() : "Unknown");
        LOGGER.debug("================================================================================");

        try
        {
            // 4. Capture the post-execution SUT state (always VISUAL level to capture post-action screenshots)
            final org.neodymium.ai.executor.selenide.ContextLevel verificationLevel = org.neodymium.ai.executor.selenide.ContextLevel.VISUAL;
            LOGGER.debug("📸 [Capture] Capturing SUT state (level: {}) AFTER executing actions", verificationLevel);
            final SutState finalState = executor.captureState(verificationLevel);
            context.getTransientData().put("finalState", finalState);

            // 5. Build system and user prompt messages using VerificationPrompt template
            final VerificationPrompt prompt = new VerificationPrompt();
            LOGGER.debug("Compiling prompt: VerificationPrompt");
            final String system = prompt.compileSystemMessage(context);
            final String user = prompt.compileUserMessage(context);

            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("System Prompt:\n{}", system);
                LOGGER.trace("User Prompt:\n{}", user);
            }

            // 6. Gather visual image attachments from initial and final SUT states for multimodal LLM comparison
            final List<SutAttachment> attachments = new java.util.ArrayList<>();
            final SutState initialState = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
            if (initialState != null && initialState.getAttachments() != null)
            {
                for (final SutAttachment att : initialState.getAttachments())
                {
                    if (att.mediaType().startsWith("image/"))
                    {
                        attachments.add(att);
                    }
                }
            }
            if (finalState != null && finalState.getAttachments() != null)
            {
                for (final SutAttachment att : finalState.getAttachments())
                {
                    if (att.mediaType().startsWith("image/"))
                    {
                        attachments.add(att);
                    }
                }
            }

            // 7. Dispatch verification LLM request to configured provider capability
            final LlmRequest request = new LlmRequest(
                system,
                user,
                attachments,
                prompt.getResponseSchema(),
                config.getTemperature("verification"),
                config.getTimeoutSeconds("verification")
            );

            final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.VERIFICATION);
            LOGGER.debug("Calling LLM provider '{}' via capability: VERIFICATION", provider.getClass().getSimpleName());
            final long startTime = System.currentTimeMillis();
            final LlmResponse response = provider.chat(request);
            final long durationMs = System.currentTimeMillis() - startTime;
            LOGGER.debug("LLM response received. Length: {} chars (duration: {} ms)", response.content() != null ? response.content().length() : 0, durationMs);
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Raw response content:\n{}", CallLlmStep.formatJsonForLogging(response.content()));
            }

            // 8. Track and accumulate token usage metrics specifically for verification calls
            final TokenUsage newUsage = response.tokenUsage();
            if (newUsage != null)
            {
                final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof org.neodymium.ai.pipeline.StepStats stats)
                {
                    stats.addVerificationCall(newUsage.inputTokenCount(), newUsage.outputTokenCount(), newUsage.cachedTokenCount());
                }

                LOGGER.debug("   📊 Verification Tokens: {} in ({} cached) → {} out (total: {})",
                    newUsage.inputTokenCount(), newUsage.cachedTokenCount(), newUsage.outputTokenCount(), newUsage.totalTokenCount());

                final TokenUsage existing = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE);
                if (existing == null)
                {
                    context.getTransientData().put(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE, newUsage);
                }
                else
                {
                    context.getTransientData().put(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE, new TokenUsage(
                        existing.inputTokenCount() + newUsage.inputTokenCount(),
                        existing.outputTokenCount() + newUsage.outputTokenCount(),
                        existing.totalTokenCount() + newUsage.totalTokenCount(),
                        existing.cachedTokenCount() + newUsage.cachedTokenCount()
                    ));
                }
            }

            // 9. Parse multi-rubric verification result and log soft warnings on failure
            try
            {
                final VerificationResult result = prompt.parseResponse(response.content(), context);
                LOGGER.debug("Successfully parsed verification result: passed={}, actionReasoning={}, visualReasoning={}", result.passed(), result.actionReasoning(), result.visualReasoning());
                if (!result.passed())
                {
                    @SuppressWarnings("unchecked")
                    final List<String> warnings = (List<String>) context.getTransientData().computeIfAbsent("verificationWarnings", k -> new java.util.ArrayList<String>());
                    final String stepStr = step != null ? String.format("%s:%d (%s)", step.getSourceFile(), step.getLineNumber(), step.getInstruction()) : "Unknown Step";
                    final String combinedReason = String.format("Action Check: %s | Visual Check: %s", result.actionReasoning(), result.visualReasoning());
                    warnings.add(String.format("Step: %s. Reason: %s", stepStr, combinedReason));
                    LOGGER.warn("   ⚠️ Semantic outcome verification FAILED for step: {}", stepStr);
                    LOGGER.warn("   ⚠️ Reason: {}", combinedReason);
                }
            }
            catch (final Exception e)
            {
                // Soft-handle parsing errors without failing the whole test pipeline
                @SuppressWarnings("unchecked")
                final List<String> warnings = (List<String>) context.getTransientData().computeIfAbsent("verificationWarnings", k -> new java.util.ArrayList<String>());
                final String stepStr = step != null ? String.format("%s:%d (%s)", step.getSourceFile(), step.getLineNumber(), step.getInstruction()) : "Unknown Step";
                warnings.add(String.format("Step: %s. Parse error: %s", stepStr, e.getMessage()));
                LOGGER.warn("   ⚠️ Semantic outcome verification response parsing FAILED for step: {}", stepStr, e);
            }

            // 10. Update last state to post-verification finalState for subsequent steps
            context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, finalState);

            // 11. Calculate difference hash (dHash) for visual baselines when image attachment is available
            if (finalState != null && finalState.getAttachments() != null && step != null && step.isVisualStep())
            {
                String dHash = null;
                for (final SutAttachment attachment : finalState.getAttachments())
                {
                    if (attachment.mediaType().startsWith("image/") && attachment.base64Data() != null)
                    {
                        dHash = org.neodymium.ai.util.ScreenshotHasher.computeHash(attachment.base64Data());
                        break;
                    }
                }

                if (dHash != null)
                {
                    LOGGER.debug("   📸 Computed dHash: {} for instruction: \"{}\"", dHash, step.getInstruction());
                    step.setScreenshotHash(dHash);

                    // 12. Create synthetic NONE action if no explicit DOM actions were generated to hold visual baseline
                    if (step.getActions().isEmpty())
                    {
                        final Action noneAction = new Action("NONE", "", "Visual baseline check");
                        noneAction.setStepInstruction(step.getInstruction());
                        noneAction.setStepLine(step.getLineNumber());
                        noneAction.setStepFile(step.getSourceFile());
                        noneAction.setStepScreenshotHash(dHash);
                        step.getActions().add(noneAction);
                        LOGGER.debug("   📝 Dispatching dummy NONE action for visual baseline check");
                        session.getEventBus().dispatch(new org.neodymium.ai.event.structural.ActionExecutedEvent(noneAction, true));
                    }
                    else
                    {
                        // Attach hash to the last action in the playbook step
                        final Action lastAction = step.getActions().get(step.getActions().size() - 1);
                        LOGGER.debug("   📝 Setting screenshot hash on last action: {} ({})", lastAction.getType(), lastAction.getTarget());
                        lastAction.setStepScreenshotHash(dHash);
                    }
                }
                else
                {
                    LOGGER.warn("   ⚠️ No image attachment found or base64 data was empty to compute dHash for instruction: \"{}\"", step.getInstruction());
                }
            }
        }
        catch (final Exception e)
        {
            // Catch unexpected runtime errors during verification and record as verification warnings
            @SuppressWarnings("unchecked")
            final List<String> warnings = (List<String>) context.getTransientData().computeIfAbsent("verificationWarnings", k -> new java.util.ArrayList<String>());
            final String stepStr = step != null ? String.format("%s:%d (%s)", step.getSourceFile(), step.getLineNumber(), step.getInstruction()) : "Unknown Step";
            warnings.add(String.format("Step: %s. Execution error: %s", stepStr, e.getMessage()));
            LOGGER.warn("   ⚠️ Semantic outcome verification execution FAILED for step: {}", stepStr, e);
        }
        finally
        {
            // Always clean transient state to prevent context leakage across pipeline steps
            context.getTransientData().remove("finalState");
        }
    }
}
