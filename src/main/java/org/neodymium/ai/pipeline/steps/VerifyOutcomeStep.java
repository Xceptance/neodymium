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
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.StateCapturedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.executor.selenide.plugins.ClickAction;
import org.neodymium.ai.executor.selenide.plugins.ClickAction.CoordinateTarget;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.prompt.VerificationIssue;
import org.neodymium.ai.prompt.VerificationPrompt;
import org.neodymium.ai.prompt.VerificationResult;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.util.ScreenshotHasher;
import org.neodymium.ai.util.VisualStabilityDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pipeline step executed after ExecuteActionsStep. Evaluates step execution
 * outcome by performing a dual-state semantic validation comparison if enabled.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerifyOutcomeStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyOutcomeStep.class);

    /**
     * Constructs a default VerifyOutcomeStep.
     */
    public VerifyOutcomeStep()
    {
    }

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

    private void executeInternal(final ExecutionContext context) throws PipelineException
    {
        final AiConfiguration config = AiConfiguration.getInstance();
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
        final ExecutionMode mode = (ExecutionMode) context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
        final PlaybookStep step = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);

        final ContextLevel activeLevel =
            context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL) instanceof ContextLevel cl
                ? cl
                : ContextLevel.MINIMAL;
        final boolean isVisualExecution = (step != null && step.isVisualStep()) || (activeLevel != null && activeLevel.includesScreenshot());

        // 1. Calculate and record visual baseline hash (SSIM matrix) during live/recording execution for visual steps / escalated visual context
        if (executor != null && mode != null && !mode.isReplay() && step != null && isVisualExecution)
        {
            try
            {
                SutState capturedState = (SutState) context.getTransientData().remove("KEY_POST_ACTION_STATE");
                if (capturedState == null || capturedState.getAttachments() == null || capturedState.getAttachments().isEmpty())
                {
                    final SutState lastState = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
                    if (lastState != null && lastState.getAttachments() != null && !lastState.getAttachments().isEmpty())
                    {
                        capturedState = lastState;
                    }
                    else
                    {
                        final ContextLevel level = (activeLevel != null && activeLevel.includesScreenshot()) 
                            ? activeLevel 
                            : ((step != null && step.isVisualStep()) 
                                ? ContextLevel.VISUAL 
                                : ContextLevel.VISUAL_LEAN);
                        final boolean isFullPageReq = Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT"));
                        capturedState = executor.captureState(level, isFullPageReq);
                    }
                }
                if (capturedState != null && capturedState.getAttachments() != null)
                {
                    if (session != null && session.getEventBus() != null)
                    {
                        session.getEventBus().dispatch(new StateCapturedEvent(capturedState));
                    }

                    CoordinateTarget coordinateTarget = null;
                    if (step.getActions() != null)
                    {
                        for (final Action act : step.getActions())
                        {
                            if (act != null && act.getTarget() != null)
                            {
                                final CoordinateTarget parsed = ClickAction.parseCoordinateTarget(act.getTarget());
                                if (parsed != null)
                                {
                                    coordinateTarget = parsed;
                                    break;
                                }
                            }
                        }
                    }

                    for (final SutAttachment attachment : capturedState.getAttachments())
                    {
                        if (attachment.mediaType().startsWith("image/") && attachment.base64Data() != null)
                        {
                            final String ssimMatrix;
                            if (coordinateTarget != null)
                            {
                                if (step.getScreenshotHash() == null || step.getScreenshotHash().isBlank())
                                {
                                    ssimMatrix = ScreenshotHasher.computeTileSsimMatrix(attachment.base64Data(), coordinateTarget.x(), coordinateTarget.y(), 32);
                                }
                                else
                                {
                                    ssimMatrix = step.getScreenshotHash();
                                }
                            }
                            else
                            {
                                ssimMatrix = ScreenshotHasher.computeSsimMatrix(attachment.base64Data());
                            }
                            if (ssimMatrix != null)
                            {
                                step.setScreenshotHash(ssimMatrix);
                                final String resolvedInstr = context.getSessionData() != null
                                    ? context.getSessionData().resolveVariables(step.getInstruction())
                                    : step.getInstruction();
                                LOGGER.debug("📸 [Visual Hashing] Computed SSIM matrix for instruction: \"{}\"", resolvedInstr);

                                if (step.getActions().isEmpty())
                                {
                                    final Action noneAction = new Action("NONE", "", "Visual baseline check");
                                    noneAction.setStepInstruction(step.getInstruction());
                                    noneAction.setStepLine(step.getLineNumber());
                                    noneAction.setStepFile(step.getSourceFile());
                                    noneAction.setStepScreenshotHash(ssimMatrix);
                                    step.getActions().add(noneAction);
                                    if (session != null)
                                    {
                                        session.getEventBus().dispatch(new ActionExecutedEvent(noneAction, true));
                                    }
                                }
                                else
                                {
                                    final Action lastAction = step.getActions().get(step.getActions().size() - 1);
                                    lastAction.setStepScreenshotHash(ssimMatrix);
                                }
                            }
                            break;
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                final String resolvedInstr = (step != null && context.getSessionData() != null)
                    ? context.getSessionData().resolveVariables(step.getInstruction())
                    : (step != null ? step.getInstruction() : "Unknown");
                LOGGER.warn("⚠️ Failed to capture visual baseline hash for instruction: \"{}\": {}", resolvedInstr, e.getMessage());
            }
        }

        // 2. Check if optional semantic LLM verification is enabled
        final Object override = context.getTransientData().get("semanticVerification.enabled");
        final boolean isEnabled = override instanceof Boolean b ? b : config.isSemanticVerificationEnabled();
        if (!isEnabled)
        {
            if (step != null)
            {
                final Long stepStartTime = (Long) context.getTransientData().get("KEY_STEP_START_TIME");
                if (stepStartTime != null)
                {
                    step.setDurationMs(System.currentTimeMillis() - stepStartTime);
                }
                context.getTransientData().put("KEY_LAST_STEP_END_TIME", System.currentTimeMillis());

                step.setStatus(PlaybookStepStatus.SUCCESS);
                if (session != null && session.getEventBus() != null)
                {
                    session.getEventBus().dispatch(new StepFinishedEvent(step, PlaybookStepStatus.SUCCESS));
                }
            }
            LOGGER.debug("Semantic outcome verification is disabled in configuration. Skipping step.");
            return;
        }

        // Fail conclusively if essential session runtime structures are missing
        if (session == null)
        {
            throw new ConclusiveFailureException("No active AiSession registered in ExecutionContext transient data");
        }
        if (executor == null)
        {
            throw new ConclusiveFailureException("No active TargetExecutor registered in ExecutionContext transient data");
        }

        // 3. Skip outcome verification during deterministic replay unless healing is actively engaged
        if (mode != null && mode.isReplay() && !mode.supportsHealing())
        {
            if (step != null)
            {
                final Long stepStartTime = (Long) context.getTransientData().get("KEY_STEP_START_TIME");
                if (stepStartTime != null)
                {
                    step.setDurationMs(System.currentTimeMillis() - stepStartTime);
                }
                context.getTransientData().put("KEY_LAST_STEP_END_TIME", System.currentTimeMillis());

                step.setStatus(PlaybookStepStatus.SUCCESS);
                if (session != null && session.getEventBus() != null)
                {
                    session.getEventBus().dispatch(new StepFinishedEvent(step, PlaybookStepStatus.SUCCESS));
                }
            }
            LOGGER.debug("Step was replayed from baseline. Bypassing semantic outcome verification.");
            return;
        }

        LOGGER.debug("================================================================================");
        LOGGER.debug("🔍 [Optional AI Outcome Verification]");
        final String resolvedHeaderInstr = (step != null && context.getSessionData() != null)
            ? context.getSessionData().resolveVariables(step.getInstruction())
            : (step != null ? step.getInstruction() : "Unknown");
        LOGGER.debug("       Instruction: \"{}\"", resolvedHeaderInstr);
        LOGGER.debug("================================================================================");

        try
        {
            // 4. Capture the post-execution SUT state (with temporal visual stability settling for visual assertions)
            final boolean isFullPageReq = Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT"));
            final SutState finalState;
            if (step != null && (step.isVisualStep() || isFullPageReq))
            {
                LOGGER.debug("📸 [Capture] Capturing settled SUT state with temporal stability detection (fullPage: {}) AFTER executing actions", isFullPageReq);
                finalState = VisualStabilityDetector.captureSettledState(executor, isFullPageReq);
            }
            else
            {
                final ContextLevel verificationLevel = ContextLevel.VISUAL;
                LOGGER.debug("📸 [Capture] Capturing SUT state (level: {}, fullPage: {}) AFTER executing actions", verificationLevel, isFullPageReq);
                finalState = executor.captureState(verificationLevel, isFullPageReq);
            }
            context.getTransientData().put("finalState", finalState);
            if (session != null && session.getEventBus() != null && finalState != null)
            {
                session.getEventBus().dispatch(new StateCapturedEvent(finalState));
            }

            // 5. Build system and user prompt messages using VerificationPrompt template
            final VerificationPrompt prompt = new VerificationPrompt();
            LOGGER.debug("Compiling prompt: VerificationPrompt");
            final String system = prompt.compileSystemMessage(context);
            final String user = prompt.compileUserMessage(context);

            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("┌─ [Verification System Prompt] ───────────────────────────────────────────");
                LOGGER.trace("{}", system);
                LOGGER.trace("├─ [Verification User Prompt] ─────────────────────────────────────────────");
                LOGGER.trace("{}", user);
                LOGGER.trace("└──────────────────────────────────────────────────────────────────────────");
            }

            // 6. Gather visual image attachments from initial and final SUT states for multimodal LLM comparison
            final List<SutAttachment> attachments = new ArrayList<>();
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
                LOGGER.trace("┌─ [Verification Raw Response] ─────────────────────────────────────────────");
                LOGGER.trace("{}", CallLlmStep.formatJsonForLogging(response.content()));
                LOGGER.trace("└──────────────────────────────────────────────────────────────────────────");
            }

            // 8. Track and accumulate token usage metrics specifically for verification calls
            final TokenUsage newUsage = response.tokenUsage();
            if (newUsage != null)
            {
                final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof StepStats stats)
                {
                    stats.addVerificationCall(newUsage.inputTokenCount(), newUsage.outputTokenCount(), newUsage.cachedTokenCount());
                }

                LOGGER.debug("   📊 Verification Tokens: {} in ({} cached) → {} out (total: {})",
                    newUsage.inputTokenCount(), newUsage.cachedTokenCount(), newUsage.outputTokenCount(), newUsage.totalTokenCount());

                context.getTransientData().compute("verificationCallCount", (k, v) -> v == null ? 1 : ((Integer) v) + 1);

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
                logVerificationResult(result);
                if (!result.passed())
                {
                    @SuppressWarnings("unchecked")
                    final List<Object> warnings = (List<Object>) context.getTransientData().computeIfAbsent("verificationWarnings", k -> new ArrayList<Object>());
                    
                    final String stepLoc = step != null ? String.format("%s:%d", step.getSourceFile(), step.getLineNumber()) : "Unknown Location";
                    final String rawInstr = step != null ? step.getInstruction() : "";
                    final String instruction = (context.getSessionData() != null)
                        ? context.getSessionData().resolveVariables(rawInstr)
                        : rawInstr;
                    final String summary = result.getOverallVerdict() != null ? result.getOverallVerdict().summary() : "";

                    String intentScore = "FAIL";
                    String intentAnalysis = "";
                    String visualScore = "FAIL";
                    String visualAnalysis = "";
                    String errorScore = "PASS";
                    String errorAnalysis = "";

                    if (result.getRubrics() != null)
                    {
                        final VerificationResult.Rubrics rubrics = result.getRubrics();
                        if (rubrics.intentMatch() != null)
                        {
                            intentScore = rubrics.intentMatch().score();
                            intentAnalysis = rubrics.intentMatch().analysis();
                        }
                        if (rubrics.visualDelta() != null)
                        {
                            visualScore = rubrics.visualDelta().score();
                            visualAnalysis = rubrics.visualDelta().analysis();
                        }
                        if (rubrics.absenceOfErrors() != null)
                        {
                            errorScore = rubrics.absenceOfErrors().score();
                            errorAnalysis = rubrics.absenceOfErrors().analysis();
                        }
                    }

                    final VerificationIssue issue = new VerificationIssue(
                        stepLoc, instruction, summary,
                        intentScore, intentAnalysis,
                        visualScore, visualAnalysis,
                        errorScore, errorAnalysis,
                        null
                    );
                    warnings.add(issue);
                    LOGGER.warn("   ⚠️ Semantic outcome verification FAILED for step {}: \"{}\"", stepLoc, instruction);
                }
            }
            catch (final Exception e)
            {
                // Soft-handle parsing errors without failing the whole test pipeline
                @SuppressWarnings("unchecked")
                final List<Object> warnings = (List<Object>) context.getTransientData().computeIfAbsent("verificationWarnings", k -> new ArrayList<Object>());
                final String rawInstr = step != null ? step.getInstruction() : "";
                final String resolvedInstr = (context.getSessionData() != null)
                    ? context.getSessionData().resolveVariables(rawInstr)
                    : rawInstr;
                final String stepStr = step != null ? String.format("%s:%d (%s)", step.getSourceFile(), step.getLineNumber(), resolvedInstr) : "Unknown Step";
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
                        dHash = ScreenshotHasher.computeSsimMatrix(attachment.base64Data());
                        break;
                    }
                }

                if (dHash != null)
                {
                    final String resolvedInstr = context.getSessionData() != null
                        ? context.getSessionData().resolveVariables(step.getInstruction())
                        : step.getInstruction();
                    LOGGER.debug("   📸 Computed SSIM matrix for instruction: \"{}\"", resolvedInstr);
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
                        session.getEventBus().dispatch(new ActionExecutedEvent(noneAction, true));
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
                    final String resolvedInstr = context.getSessionData() != null
                        ? context.getSessionData().resolveVariables(step.getInstruction())
                        : step.getInstruction();
                    LOGGER.warn("   ⚠️ No image attachment found or base64 data was empty to compute dHash for instruction: \"{}\"", resolvedInstr);
                }
            }
            if (step != null)
            {
                final Long stepStartTime = (Long) context.getTransientData().get("KEY_STEP_START_TIME");
                if (stepStartTime != null)
                {
                    step.setDurationMs(System.currentTimeMillis() - stepStartTime);
                }
                context.getTransientData().put("KEY_LAST_STEP_END_TIME", System.currentTimeMillis());

                step.setStatus(PlaybookStepStatus.SUCCESS);
                if (session != null && session.getEventBus() != null)
                {
                    session.getEventBus().dispatch(new StepFinishedEvent(step, PlaybookStepStatus.SUCCESS));
                }
            }
        }
        catch (final Exception e)
        {
            // Catch unexpected runtime errors during verification and record as verification warnings
            @SuppressWarnings("unchecked")
            final List<String> warnings = (List<String>) context.getTransientData().computeIfAbsent("verificationWarnings", k -> new ArrayList<String>());
            final String rawInstr = step != null ? step.getInstruction() : "";
            final String resolvedInstr = (context.getSessionData() != null)
                ? context.getSessionData().resolveVariables(rawInstr)
                : rawInstr;
            final String stepStr = step != null ? String.format("%s:%d (%s)", step.getSourceFile(), step.getLineNumber(), resolvedInstr) : "Unknown Step";
            warnings.add(String.format("Step: %s. Execution error: %s", stepStr, e.getMessage()));
            LOGGER.warn("   ⚠️ Semantic outcome verification execution FAILED for step: {}", stepStr, e);
            if (step != null)
            {
                final Long stepStartTime = (Long) context.getTransientData().get("KEY_STEP_START_TIME");
                if (stepStartTime != null)
                {
                    step.setDurationMs(System.currentTimeMillis() - stepStartTime);
                }
                context.getTransientData().put("KEY_LAST_STEP_END_TIME", System.currentTimeMillis());

                step.setStatus(PlaybookStepStatus.FAILED);
                if (session != null && session.getEventBus() != null)
                {
                    session.getEventBus().dispatch(new StepFinishedEvent(step, PlaybookStepStatus.FAILED));
                }
            }
        }
        finally
        {
            // Always clean transient state to prevent context leakage across pipeline steps
            context.getTransientData().remove("finalState");
        }
    }

    /**
     * Formats and logs the verification result in a structured, human-readable box layout.
     *
     * @param result the verification result to log
     */
    private void logVerificationResult(final VerificationResult result)
    {
        if (result == null)
        {
            return;
        }

        final boolean passed = result.passed();
        final String statusStr = passed ? "PASS" : "FAIL";
        final String icon = passed ? "🔍" : "⚠️";

        LOGGER.debug("   ┌─ {} Verification Verdict: {} (passed={}) ─────────────────────────", icon, statusStr, passed);

        if (result.getOverallVerdict() != null && result.getOverallVerdict().summary() != null)
        {
            LOGGER.debug("   │ Summary:        {}", result.getOverallVerdict().summary());
        }

        if (result.getRubrics() != null)
        {
            final VerificationResult.Rubrics rubrics = result.getRubrics();
            if (rubrics.intentMatch() != null)
            {
                LOGGER.debug("   │ Intent Check:   [{}] {}", rubrics.intentMatch().score(), rubrics.intentMatch().analysis());
            }
            if (rubrics.visualDelta() != null)
            {
                LOGGER.debug("   │ Visual Check:   [{}] {}", rubrics.visualDelta().score(), rubrics.visualDelta().analysis());
            }
            if (rubrics.absenceOfErrors() != null)
            {
                LOGGER.debug("   │ Error Check:    [{}] {}", rubrics.absenceOfErrors().score(), rubrics.absenceOfErrors().analysis());
            }
        }
        else
        {
            if (result.actionReasoning() != null && !result.actionReasoning().trim().isEmpty())
            {
                LOGGER.debug("   │ Action Reasoning: {}", result.actionReasoning());
            }
            if (result.visualReasoning() != null && !result.visualReasoning().trim().isEmpty())
            {
                LOGGER.debug("   │ Visual Reasoning: {}", result.visualReasoning());
            }
        }

        LOGGER.debug("   └────────────────────────────────────────────────────────");
    }
}
