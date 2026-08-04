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
package org.neodymium.ai.runner;

import java.util.Collections;
import java.util.List;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.event.diagnostic.DiagnosticErrorEvent;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.structural.TryCatchStep;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.prompt.VisualRcaPrompt;
import org.neodymium.ai.session.AiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * State machine loop that pops and executes scheduled steps from the LIFO stack
 * in the active session context. Coordinates execution hooks and Exception handling.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class StateMachineRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachineRunner.class);
    /**
     * The active playbook execution session.
     */
    private final AiSession session;

    /**
     * Constructs a StateMachineRunner.
     *
     * @param session the execution session
     */
    public StateMachineRunner(final AiSession session)
    {
        this.session = session;
    }

    /**
     * Wires the pre hooks, executes the scheduled steps on the context stack,
     * handles Try/Catch exceptions routing, and triggers post hooks.
     *
     * @throws PipelineException if a step fails and is not caught by any try-catch block
     */
    public void run() throws PipelineException
    {
        final long startTime = System.currentTimeMillis();
        this.session.runPreHooks();
        final ExecutionContext context = this.session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_SESSION, this.session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, this.session.getTargetExecutor());

        final org.neodymium.ai.config.ExecutionMode mode = (org.neodymium.ai.config.ExecutionMode) context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
        final String datasetLabel = (String) context.getTransientData().get(ExecutionContext.KEY_ACTIVE_DATASET_LABEL);
        final String testName = org.neodymium.util.Neodymium.getTestName();
        final String loadedPlaybook = (String) context.getTransientData().get("playbook.resolvedPath");

        LOGGER.debug("╔════════════════════════════════════════════════════════════════════════════════════");
        LOGGER.debug("║ 🚀 STARTING TEST CASE: {}", testName != null ? testName : "Unknown Test");
        LOGGER.debug("║ 📂 Active Dataset:    {}", datasetLabel != null ? datasetLabel : "default");
        LOGGER.debug("║ ⚙️  Execution Mode:    {}", mode != null ? mode : "LLM_ONLY");
        LOGGER.debug("║ 📜 Loaded Playbook:   {}", loadedPlaybook != null ? loadedPlaybook : "None");
        LOGGER.debug("╚════════════════════════════════════════════════════════════════════════════════════");

        if (LOGGER.isTraceEnabled())
        {
            final org.neodymium.ai.config.AiConfiguration config = org.neodymium.ai.config.AiConfiguration.getInstance();
            LOGGER.trace("   ┌─ [Configured LLM Capabilities & Providers] ──────────────────────────────");
            for (final LlmCapability cap : LlmCapability.values())
            {
                final String roleKey = cap.name().toLowerCase();
                String provName = "Unknown";
                try
                {
                    final LlmProvider prov = this.session.getLlmRegistry().getProvider(cap);
                    if (prov != null)
                    {
                        provName = prov.getClass().getSimpleName();
                    }
                }
                catch (final Exception ignored)
                {
                    provName = config.getProvider(roleKey);
                }

                final String modelName = config.getModel(roleKey);
                final String apiKey = config.getApiKey(roleKey);
                final String keyHint = maskApiKeyHint(apiKey);

                LOGGER.trace("   │ {} Provider: {} Model: {} Key: {}",
                    String.format("%-16s", cap.name() + ":"),
                    String.format("%-22s", provName),
                    String.format("%-24s", modelName != null ? modelName : "default"),
                    keyHint);
            }
            LOGGER.trace("   └──────────────────────────────────────────────────────────────────────────");
        }

        boolean success = false;
        Throwable failureCause = null;

        final ExecutionContext previousContext = ExecutionContext.getActiveContext();
        try
        {
            ExecutionContext.setActiveContext(context);
            while (context.hasSteps())
            {
                final PipelineStep step = context.popStep();
                try
                {
                    step.execute(context);
                }
                catch (final Throwable t)
                {
                    if (t instanceof VirtualMachineError || t instanceof ThreadDeath || t instanceof LinkageError)
                    {
                        throw (Error) t;
                    }

                    final PipelineException e;
                    if (t instanceof PipelineException)
                    {
                        e = (PipelineException) t;
                    }
                    else
                    {
                        e = new org.neodymium.ai.pipeline.ConclusiveFailureException(t.getMessage() != null ? t.getMessage() : t.toString(), t);
                    }

                    // Check if an active TryCatch scope can handle this exception
                    final PipelineStep activeScope = context.peekTryCatch();
                    if (activeScope instanceof TryCatchStep tryCatch)
                    {
                        final PipelineStep handler = tryCatch.getHandlerFor(e);
                        if (handler != null)
                        {
                            // Pop TryCatch from the exception scope stack
                            context.popTryCatch();
                            // Discard try block pending steps up to boundary marker
                            context.discardStepsUpToTryCatch(tryCatch);
                            // Store the exception in the context for the handler (e.g. LLM Escalation prompt)
                            context.getTransientData().put(ExecutionContext.KEY_LAST_EXECUTION_ERROR, e);
                            // Schedule exception handler step
                            context.pushStep(handler);
                            continue;
                        }
                    }

                    // Check if the current PlaybookStep is marked with a bug
                    final org.neodymium.ai.model.PlaybookStep playbookStep = (org.neodymium.ai.model.PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
                    if (playbookStep != null && playbookStep.isBug() && !(e instanceof org.neodymium.ai.pipeline.UnexpectedSuccessException))
                    {
                        if (activeScope instanceof TryCatchStep tryCatch)
                        {
                            // Pop TryCatch from the exception scope stack
                            context.popTryCatch();
                            // Discard the remaining pending steps of this step block
                            context.discardStepsUpToTryCatch(tryCatch);
                        }

                        // Mark step status on playbook step
                        playbookStep.setStatus(org.neodymium.ai.model.PlaybookStepStatus.SUCCESS);

                        final String bugComment = playbookStep.getBugDetails();
                        final String bugStr = bugComment != null ? " (" + bugComment + ")" : "";
                        LOGGER.info("   🐞 Expected bug hit{} on step: {}:{} ({}) - Error: {}",
                            bugStr,
                            playbookStep.getSourceFile(),
                            playbookStep.getLineNumber(),
                            playbookStep.getInstruction(),
                            e.getMessage());

                        if (playbookStep.isContinueOnError())
                        {
                            continue;
                        }
                        else
                        {
                            context.clearSteps();
                            continue;
                        }
                    }

                    // Check if the current PlaybookStep is optional
                    if (playbookStep != null && playbookStep.isOptional())
                    {
                        if (activeScope instanceof TryCatchStep tryCatch)
                        {
                            // Pop TryCatch from the exception scope stack
                            context.popTryCatch();
                            // Discard the remaining pending steps of this step block
                            context.discardStepsUpToTryCatch(tryCatch);
                        }

                        // Add failure to warnings/reporting list
                        @SuppressWarnings("unchecked")
                        final List<String> warnings = (List<String>) context.getTransientData()
                            .computeIfAbsent("verificationWarnings", k -> new java.util.ArrayList<String>());

                        final String stepStr = String.format("%s:%d (%s)",
                            playbookStep.getSourceFile(),
                            playbookStep.getLineNumber(),
                            playbookStep.getInstruction());

                        warnings.add(String.format("Step: %s. Optional step execution failed: %s", stepStr, e.getMessage()));
                        LOGGER.warn("   ⚠️ Optional step execution FAILED: {}", stepStr);
                        LOGGER.warn("   ⚠️ Reason: {}", e.getMessage());

                        continue;
                    }

                    // Bubbling up out of loop
                    if (t instanceof RuntimeException)
                    {
                        throw (RuntimeException) t;
                    }
                    if (t instanceof Error)
                    {
                        throw (Error) t;
                    }
                    throw e;
                }
            }
            success = true;
        }
        catch (final PipelineException e)
        {
            failureCause = e;
            runVisualRca(context, e);
            throw e;
        }
        catch (final Throwable t)
        {
            failureCause = t;
            throw t;
        }
        finally
        {
            final long durationMs = System.currentTimeMillis() - startTime;
            @SuppressWarnings("unchecked")
            final List<String> warningsList = (List<String>) context.getTransientData().get(ExecutionContext.KEY_EXECUTION_WARNINGS);
            try
            {
                this.session.getEventBus().dispatch(new SessionFinishedEvent(durationMs, success, warningsList != null ? warningsList : Collections.emptyList()));
                logFinalStatsSummary(context, durationMs, success, failureCause);
                this.session.runPostHooks(success);
            }
            finally
            {
                ExecutionContext.setActiveContext(previousContext);
            }
        }
    }

    /**
     * Logs both the cumulative AI execution statistics and the step-by-step trace statistics.
     */
    private void logFinalStatsSummary(final ExecutionContext context, final long durationMs, final boolean success, final Throwable failureCause)
    {
        @SuppressWarnings("unchecked")
        final List<org.neodymium.ai.pipeline.StepStats> stepStatsList = 
            (List<org.neodymium.ai.pipeline.StepStats>) context.getTransientData().get("execution.stepStatsList");
        
        if (stepStatsList != null && !stepStatsList.isEmpty())
        {
            if (!success && failureCause != null)
            {
                final org.neodymium.ai.pipeline.StepStats lastStats = stepStatsList.get(stepStatsList.size() - 1);
                if (lastStats.getDurationMs() == 0)
                {
                    lastStats.setDurationMs(System.currentTimeMillis() - lastStats.getStartTime());
                    Throwable root = failureCause;
                    while (root.getCause() != null && root != root.getCause())
                    {
                        root = root.getCause();
                    }
                    lastStats.setFailureReason(root.getMessage() != null ? root.getMessage() : root.toString());
                    
                    @SuppressWarnings("unchecked")
                    final List<org.neodymium.ai.action.Action> stepActions = 
                        (List<org.neodymium.ai.action.Action>) context.getTransientData().get(ExecutionContext.KEY_CURRENT_STEP_ACTIONS);
                    if (stepActions != null)
                    {
                        lastStats.getActions().addAll(stepActions);
                    }
                }
            }

            if (!success && failureCause != null)
            {
                Throwable root = failureCause;
                while (root.getCause() != null && root != root.getCause())
                {
                    root = root.getCause();
                }
                LOGGER.error("   ❌ Step Execution Failed: {}", root.getMessage() != null ? root.getMessage() : root.toString());
            }

            LOGGER.debug("======== 📊 AI Step Execution Statistics ========");
            for (final org.neodymium.ai.pipeline.StepStats stats : stepStatsList)
            {
                calculateDuration(stats);
            }
            for (int i = 0; i < stepStatsList.size(); i++)
            {
                logStats(stepStatsList.get(i), "  ", String.valueOf(i + 1));
            }
            LOGGER.debug("=================================================");
        }

        final Integer llmCalls = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_LLM_CALLS, 0);
        final Integer replays = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_REPLAYS, 0);
        
        final org.neodymium.ai.client.TokenUsage standardUsage = (org.neodymium.ai.client.TokenUsage) context.getTransientData().get(ExecutionContext.KEY_STANDARD_TOKEN_USAGE);
        final org.neodymium.ai.client.TokenUsage verificationUsage = (org.neodymium.ai.client.TokenUsage) context.getTransientData().get(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE);

        final long standardIn = standardUsage != null ? standardUsage.inputTokenCount() : 0;
        final long standardOut = standardUsage != null ? standardUsage.outputTokenCount() : 0;
        final long standardCached = standardUsage != null ? standardUsage.cachedTokenCount() : 0;
        final long verificationIn = verificationUsage != null ? verificationUsage.inputTokenCount() : 0;
        final long verificationOut = verificationUsage != null ? verificationUsage.outputTokenCount() : 0;
        final long verificationCached = verificationUsage != null ? verificationUsage.cachedTokenCount() : 0;
        
        final long totalIn = standardIn + verificationIn;
        final long totalOut = standardOut + verificationOut;
        final long totalCached = standardCached + verificationCached;
        final long totalTokens = totalIn + totalOut;

        LOGGER.debug("╔════════════════════════════════════════════════════════════════════════════════════");
        LOGGER.debug("║ 🏁 TEST CASE COMPLETED: {}", success ? "SUCCESS" : "FAILED");
        if (!success && failureCause != null)
        {
            Throwable root = failureCause;
            while (root.getCause() != null && root != root.getCause())
            {
                root = root.getCause();
            }
            LOGGER.debug("║ ❌ Failure Reason:      {}", root.getMessage() != null ? root.getMessage() : root.toString());
        }
        final Integer verifCallsObj = (Integer) context.getTransientData().get("verificationCallCount");
        final int verificationCalls = verifCallsObj != null ? verifCallsObj : (verificationUsage != null ? 1 : 0);
        LOGGER.debug("║ ⏱️ Duration:            {} ms", String.format("%,d", durationMs));
        LOGGER.debug("║ 🤖 LLM Calls:           {} (Standard: {}, Verification: {})", llmCalls + verificationCalls, llmCalls, verificationCalls);
        LOGGER.debug("║ 🎟️ Replays:             {}", replays);
        LOGGER.debug("║ 🪙 Tokens:              {} (Input: {}, Cached: {}, Output: {})",
            String.format("%,d", totalTokens),
            String.format("%,d", totalIn),
            String.format("%,d", totalCached),
            String.format("%,d", totalOut));
        LOGGER.debug("╚════════════════════════════════════════════════════════════════════════════════════");
        @SuppressWarnings("unchecked")
        final List<Object> warnings = (List<Object>) context.getTransientData().get("verificationWarnings");
        if (warnings != null && !warnings.isEmpty())
        {
            LOGGER.warn("════════════════════════════════════════════════════════════════════════════════");
            LOGGER.warn("⚠️ Semantic Verification Warnings/Failures ({} issue(s) detected):", warnings.size());
            LOGGER.warn("════════════════════════════════════════════════════════════════════════════════");

            for (int i = 0; i < warnings.size(); i++)
            {
                final Object warnObj = warnings.get(i);
                final int issueNum = i + 1;

                if (warnObj instanceof org.neodymium.ai.prompt.VerificationIssue issue)
                {
                    LOGGER.warn("  📌 [Issue {}/{}] Step {}", issueNum, warnings.size(), issue.stepLocation());
                    if (issue.instruction() != null && !issue.instruction().isBlank())
                    {
                        LOGGER.warn("     Instruction: \"{}\"", issue.instruction());
                    }
                    if (issue.summary() != null && !issue.summary().isBlank())
                    {
                        LOGGER.warn("     Summary:     {}", issue.summary());
                    }
                    if (issue.intentAnalysis() != null && !issue.intentAnalysis().isBlank())
                    {
                        LOGGER.warn("     • Intent Match:   [{}] {}", issue.intentScore(), issue.intentAnalysis());
                    }
                    if (issue.visualAnalysis() != null && !issue.visualAnalysis().isBlank())
                    {
                        LOGGER.warn("     • Visual Delta:   [{}] {}", issue.visualScore(), issue.visualAnalysis());
                    }
                    if (issue.errorAnalysis() != null && !issue.errorAnalysis().isBlank())
                    {
                        LOGGER.warn("     • Error Check:    [{}] {}", issue.errorScore(), issue.errorAnalysis());
                    }
                }
                else if (warnObj != null)
                {
                    final String warnStr = warnObj.toString();
                    if (warnStr.contains(" | "))
                    {
                        final String[] parts = warnStr.split(" \\| ");
                        LOGGER.warn("  📌 [Issue {}/{}] {}", issueNum, warnings.size(), parts[0]);
                        for (int p = 1; p < parts.length; p++)
                        {
                            LOGGER.warn("     • {}", parts[p]);
                        }
                    }
                    else
                    {
                        LOGGER.warn("  📌 [Issue {}/{}] {}", issueNum, warnings.size(), warnStr);
                    }
                }

                if (i < warnings.size() - 1)
                {
                    LOGGER.warn("");
                }
            }
            LOGGER.warn("════════════════════════════════════════════════════════════════════════════════");
        }
    }

    /**
     * Captures SUT state and schedules Vision-based LLM query to analyze and document visual root causes.
     */
    private void runVisualRca(final ExecutionContext context, final Throwable exception)
    {
        try
        {
            final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
            if (executor == null)
            {
                return;
            }

            final SutState state = executor.captureState();
            if (state == null)
            {
                return;
            }

            final String failedInstruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
            final String errorMessage = exception != null ? exception.getMessage() : "Unknown execution error";

            final VisualRcaPrompt rcaPrompt = new VisualRcaPrompt(failedInstruction, errorMessage);
            final String system = rcaPrompt.compileSystemMessage(context);
            final String user = rcaPrompt.compileUserMessage(context);

            final LlmRequest request = new LlmRequest(
                system,
                user,
                state.getAttachments() != null ? state.getAttachments() : Collections.emptyList(),
                ResponseSchema.TEXT,
                0.0,
                60
            );

            final LlmProvider provider = this.session.getLlmRegistry().getProvider(LlmCapability.VISION);
            if (provider == null)
            {
                return;
            }
            if (provider instanceof org.neodymium.ai.client.MockLlmProvider mlp && !mlp.hasQueuedResponses())
            {
                return;
            }

            LOGGER.debug("Calling LLM provider '{}' via capability: VISION (Visual RCA)", provider.getClass().getSimpleName());
            final long startTime = System.currentTimeMillis();
            final ExecutionContext previousContext = ExecutionContext.getActiveContext();
            final LlmResponse response;
            try
            {
                ExecutionContext.setActiveContext(context);
                response = provider.chat(request);
            }
            finally
            {
                ExecutionContext.setActiveContext(previousContext);
            }
            final long durationMs = System.currentTimeMillis() - startTime;
            LOGGER.debug("LLM response received. Length: {} chars (duration: {} ms)", response.content() != null ? response.content().length() : 0, durationMs);
            final String rcaExplanation = rcaPrompt.parseResponse(response.content(), context);

            context.getTransientData().put(ExecutionContext.KEY_VISUAL_RCA_EXPLANATION, rcaExplanation);
            this.session.getEventBus().dispatch(new DiagnosticErrorEvent("Visual RCA analysis: " + rcaExplanation, exception));
        }
        catch (final Exception e)
        {
            LOGGER.debug("   ⚠️ Visual RCA skipped or failed: {}", e.getMessage());
        }
    }

    private long calculateDuration(final org.neodymium.ai.pipeline.StepStats stats)
    {
        if (stats.getDurationMs() > 0)
        {
            return stats.getDurationMs();
        }
        if (stats.getSubStats() != null && !stats.getSubStats().isEmpty())
        {
            long sum = 0;
            for (final org.neodymium.ai.pipeline.StepStats child : stats.getSubStats())
            {
                sum += calculateDuration(child);
            }
            stats.setDurationMs(sum);
            return sum;
        }
        return 0;
    }

    private void logStats(final org.neodymium.ai.pipeline.StepStats stats, final String prefix, final String stepNum)
    {
        LOGGER.debug("{}Step {}: {}", prefix, stepNum, stats.getInstruction());
        final String indent = prefix + "    ";
        LOGGER.debug("{}Mode:           {}", indent, stats.isReplayed() ? "REPLAY" : "LLM");
        LOGGER.debug("{}Duration:       {} ms", indent, stats.getDurationMs());
        LOGGER.debug("{}Escalations:    {}", indent, Math.max(0, stats.getContextLevels().size() - 1));
        if (!stats.getContextLevels().isEmpty())
        {
            LOGGER.debug("{}Context Levels: {}", indent, String.join(" -> ", stats.getContextLevels()));
        }
        
        final List<org.neodymium.ai.action.Action> actions = stats.getActions();
        if (actions != null && !actions.isEmpty())
        {
            final String actionTypes = actions.stream()
                .map(act -> act.getType())
                .collect(java.util.stream.Collectors.joining(", "));
            LOGGER.debug("{}Actions:        {} ({})", indent, actions.size(), actionTypes);
        }
        else
        {
            LOGGER.debug("{}Actions:        0", indent);
        }

        if (stats.getStandardCalls() > 0)
        {
            LOGGER.debug("{}Standard Calls: {} (Tokens: {} in ({} cached) → {} out)",
                indent,
                stats.getStandardCalls(),
                stats.getStandardInputTokens(),
                stats.getStandardCachedTokens(),
                stats.getStandardOutputTokens());
        }
        if (stats.getVerificationCalls() > 0)
        {
            LOGGER.debug("{}Verification Calls: {} (Tokens: {} in ({} cached) → {} out)",
                indent,
                stats.getVerificationCalls(),
                stats.getVerificationInputTokens(),
                stats.getVerificationCachedTokens(),
                stats.getVerificationOutputTokens());
        }
        if (stats.getFailureReason() != null)
        {
            LOGGER.debug("{}Failure:        {}", indent, stats.getFailureReason());
        }

        if (stats.getSubStats() != null && !stats.getSubStats().isEmpty())
        {
            for (int j = 0; j < stats.getSubStats().size(); j++)
            {
                logStats(stats.getSubStats().get(j), prefix + "  ", stepNum + "." + (j + 1));
            }
        }
    }

    /**
     * Masks an API key for TRACE log hints (e.g. AQ....4E).
     *
     * @param apiKey the raw API key to format
     * @return the masked API key hint
     */
    private static String maskApiKeyHint(final String apiKey)
    {
        if (apiKey == null || apiKey.isBlank())
        {
            return "None";
        }
        final String trimmed = apiKey.trim();
        if (trimmed.length() <= 4)
        {
            return "****";
        }
        return trimmed.substring(0, 2) + "...." + trimmed.substring(trimmed.length() - 2);
    }
}
