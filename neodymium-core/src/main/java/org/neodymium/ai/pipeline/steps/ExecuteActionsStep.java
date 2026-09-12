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

import com.codeborne.selenide.Configuration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.structural.StateCapturedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.ExpectedBugNotReproducedException;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.pipeline.structural.SequenceStep;
import org.neodymium.ai.pipeline.structural.TryCatchStep;
import org.neodymium.ai.replay.PlaybookToolReplayer;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory utility providing playbook step mapping to unified tooling execution pipelines.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecuteActionsStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteActionsStep.class);

    /**
     * Private constructor to prevent instantiation of utility factory class.
     */
    private ExecuteActionsStep()
    {
    }

    /**
     * Maps a parsed {@link PlaybookStep} to an executable {@link PipelineStep} tree wired with
     * unified tooling execution, JIT PESAP pre-step analysis, baseline gating, outcome verification,
     * and self-healing.
     *
     * @param step the parsed playbook step
     * @param session the active AI session
     * @param context the execution context
     * @return the mapped pipeline step
     */
    public static PipelineStep mapPlaybookStepToPipelineStep(
        final PlaybookStep step,
        final AiSession session,
        final ExecutionContext context
    )
    {
        if (step == null)
        {
            return c ->
            {
            };
        }

        // If the step has sub-steps (composite), schedule the sub-steps in sequence
        if (step.getSubSteps() != null && !step.getSubSteps().isEmpty())
        {
            return contextState ->
            {
                contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);
                step.setStatus(PlaybookStepStatus.RUNNING);
                final String rawInstruction = step.getInstruction();
                final String resolvedInstruction = contextState.getSessionData() != null
                    ? contextState.getSessionData().resolveVariables(rawInstruction)
                    : rawInstruction;
                contextState.getTransientData().put("KEY_CURRENT_STEP_RAW_INSTRUCTION", resolvedInstruction);
                contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, rawInstruction);

                if (session != null && session.getEventBus() != null)
                {
                    int stepIndex = -1;
                    @SuppressWarnings("unchecked")
                    final List<PlaybookStep> flatSteps = (List<PlaybookStep>) contextState.getTransientData().get("playbook.flatSteps");
                    if (flatSteps != null)
                    {
                        for (int i = 0; i < flatSteps.size(); i++)
                        {
                            final PlaybookStep fs = flatSteps.get(i);
                            if (fs == step || (fs.getInstruction() != null && fs.getInstruction().equals(step.getInstruction())))
                            {
                                stepIndex = i;
                                break;
                            }
                        }
                    }
                    session.getEventBus().dispatch(new StepStartedEvent(step, Math.max(0, stepIndex)));
                }

                final PipelineStep finishParent = c ->
                {
                    PlaybookStepStatus finalStatus = PlaybookStepStatus.SUCCESS;
                    for (final PlaybookStep sub : step.getSubSteps())
                    {
                        if (sub.getStatus() == PlaybookStepStatus.FAILED)
                        {
                            finalStatus = PlaybookStepStatus.FAILED;
                            break;
                        }
                        else if (sub.getStatus() == PlaybookStepStatus.HEALED)
                        {
                            finalStatus = PlaybookStepStatus.HEALED;
                        }
                    }
                    step.setStatus(finalStatus);
                    if (session != null && session.getEventBus() != null)
                    {
                        session.getEventBus().dispatch(new StepFinishedEvent(step, finalStatus));
                    }
                };

                contextState.pushStep(finishParent);
                for (int i = step.getSubSteps().size() - 1; i >= 0; i--)
                {
                    contextState.pushStep(mapPlaybookStepToPipelineStep(step.getSubSteps().get(i), session, contextState));
                }
            };
        }

        // For leaf steps, return a pipeline step wrapper setting the active instruction and pushing execution loop
        return contextState ->
        {
            final PlaybookStepStatus initialStepStatus = step.getStatus();
            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);
            step.setStatus(PlaybookStepStatus.RUNNING);
            final String rawInstruction = step.getInstruction();
            final String resolvedInstruction = contextState.getSessionData() != null
                ? contextState.getSessionData().resolveVariables(rawInstruction)
                : rawInstruction;
            final String preparedInstruction = prepareInstruction(resolvedInstruction);
            contextState.getTransientData().put("KEY_CURRENT_STEP_RAW_INSTRUCTION", resolvedInstruction);
            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, preparedInstruction);
            contextState.getTransientData().remove("KEY_IN_CONTINUATION_LOOP");
            contextState.getTransientData().remove(ExecutionContext.KEY_INTERNAL_MILESTONES);

            final boolean stepNoReplay = step.isNoReplay();
            contextState.getTransientData().put("KEY_CURRENT_STEP_NO_REPLAY", stepNoReplay);

            final Long stepTimeoutMs = step.getTimeoutMs();
            if (stepTimeoutMs != null && stepTimeoutMs > 0)
            {
                contextState.getTransientData().put("KEY_ORIG_SELENIDE_TIMEOUT", Configuration.timeout);
                Configuration.timeout = stepTimeoutMs;
            }

            @SuppressWarnings("unchecked")
            List<StepStats> allStats = (List<StepStats>) contextState.getTransientData().get("execution.stepStatsList");
            if (allStats == null)
            {
                allStats = new ArrayList<>();
                contextState.getTransientData().put("execution.stepStatsList", allStats);
            }

            @SuppressWarnings("unchecked")
            final Map<PlaybookStep, StepStats> stepStatsMap =
                (Map<PlaybookStep, StepStats>) contextState.getTransientData()
                    .computeIfAbsent("execution.stepStatsMap", k -> new HashMap<>());

            final ExecutionMode executionMode = (ExecutionMode) contextState.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
            final boolean isReplayStats = executionMode != null && executionMode.isReplay() && !stepNoReplay;
            final long stepStartTime = System.currentTimeMillis();
            step.setStartTimeMs(stepStartTime);
            contextState.getTransientData().put("KEY_STEP_START_TIME", stepStartTime);

            if (executionMode != null && !executionMode.isReplay())
            {
                final Long lastStepEndTime = (Long) contextState.getTransientData().get("KEY_LAST_STEP_END_TIME");
                if (lastStepEndTime != null && step.getDelayMs() == null)
                {
                    step.setDelayMs(Math.max(0L, stepStartTime - lastStepEndTime));
                }
            }

            final StepStats stats = getOrCreateStatsForStep(step, stepStartTime, isReplayStats, stepStatsMap, allStats, contextState);
            contextState.getTransientData().put("KEY_CURRENT_STEP_STATS", stats);

            ContextLevel initialLevel = ContextLevel.MINIMAL;
            if (step.getContextLevel() != null && !step.getContextLevel().isBlank())
            {
                try
                {
                    initialLevel = ContextLevel.valueOf(step.getContextLevel().toUpperCase().trim());
                }
                catch (final Exception ignored)
                {
                }
            }

            final boolean hasVisualFull = PlaybookStep.VISUAL_FULL_PATTERN.matcher(resolvedInstruction).find();
            final boolean hasLayout = PlaybookStep.LAYOUT_PATTERN.matcher(resolvedInstruction).find();
            final boolean hasVisual = PlaybookStep.VISUAL_PATTERN.matcher(resolvedInstruction).find();
            final boolean hasHint = PlaybookStep.HINT_PATTERN.matcher(resolvedInstruction).find();

            final boolean isFullPageTag = hasVisualFull || hasLayout;
            contextState.getTransientData().put("KEY_IS_FULL_PAGE_SCREENSHOT", isFullPageTag);

            if (hasVisualFull || hasVisual)
            {
                initialLevel = ContextLevel.VISUAL;
            }
            else if (hasLayout)
            {
                initialLevel = ContextLevel.VISUAL_RICH;
            }
            else if (hasHint)
            {
                initialLevel = ContextLevel.HINT;
            }

            @SuppressWarnings("unchecked")
            final List<PlaybookStep> flatSteps = (List<PlaybookStep>) contextState.getTransientData().get("playbook.flatSteps");

            if (session != null && session.getEventBus() != null)
            {
                int stepIndex = -1;
                final PlaybookStep targetForIndex = step.getParent() != null ? step.getParent() : step;
                if (flatSteps != null)
                {
                    for (int i = 0; i < flatSteps.size(); i++)
                    {
                        final PlaybookStep fs = flatSteps.get(i);
                        if (fs == targetForIndex)
                        {
                            stepIndex = i;
                            break;
                        }
                        if (fs.getInstruction() != null && fs.getInstruction().equals(targetForIndex.getInstruction()))
                        {
                            if (fs.getLineNumber() == targetForIndex.getLineNumber() || fs.getLineNumber() == -1 || targetForIndex.getLineNumber() == -1)
                            {
                                stepIndex = i;
                                break;
                            }
                        }
                    }
                }
                session.getEventBus().dispatch(new StepStartedEvent(step, Math.max(0, stepIndex)));
            }

            if (step.getStatus() == PlaybookStepStatus.SKIPPED)
            {
                LOGGER.info("   ⏭️ Skipping step execution per user request: \"{}\"", resolvedInstruction);
                if (session != null && session.getEventBus() != null)
                {
                    session.getEventBus().dispatch(new StepFinishedEvent(step, PlaybookStepStatus.SKIPPED));
                }
                return;
            }

            LOGGER.debug("================================================================================");
            if (step.getParent() != null && flatSteps != null)
            {
                final int parentIdx = flatSteps.indexOf(step.getParent()) + 1;
                final int subIdx = step.getParent().getSubSteps().indexOf(step) + 1;
                LOGGER.debug("▶ [Step {}.{}] Instruction: \"{}\"", parentIdx, subIdx, resolvedInstruction);
            }
            else if (flatSteps != null)
            {
                final int idx = flatSteps.indexOf(step) + 1;
                LOGGER.debug("▶ [Step {}] Instruction: \"{}\"", idx, resolvedInstruction);
            }
            else
            {
                LOGGER.debug("▶ Instruction: \"{}\"", resolvedInstruction);
            }
            LOGGER.debug("       Location:    {}:{}", step.getSourceFile(), step.getLineNumber());

            final List<String> flags = new ArrayList<>();
            if (step.isOptional()) flags.add("optional");
            if (step.isBug()) flags.add("bug");
            if (step.isNoHealing()) flags.add("noHealing");
            if (step.isNoReplay()) flags.add("noReplay");
            if (step.isContinueOnError()) flags.add("continueOnError");
            if (!flags.isEmpty())
            {
                LOGGER.debug("       Flags:       {}", flags);
            }
            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, initialLevel);
            step.setContextLevel(initialLevel.name());

            final ExecutionMode mode = (ExecutionMode) contextState.getTransientData()
                .computeIfAbsent(ExecutionContext.KEY_EXECUTION_MODE, k -> AiConfiguration.getInstance().getExecutionMode());

            final boolean hasRecordedContent = (step.getToolCalls() != null && !step.getToolCalls().isEmpty())
                || (step.getScreenshotHash() != null && !step.getScreenshotHash().isEmpty())
                || (initialStepStatus != null && initialStepStatus != PlaybookStepStatus.PENDING);
            final boolean isReplay = mode.isReplay() && !stepNoReplay && (mode == ExecutionMode.REPLAY_STRICT || hasRecordedContent);

            final PesapPreStep pesapPreStep = new PesapPreStep(step, session);
            pesapPreStep.executePreStep(contextState);

            if (!contextState.getTransientData().containsKey(ExecutionContext.KEY_PESAP_INTENT))
            {
                step.setSemanticIntent(null);
            }

            final ContextLevel effectiveLevel = (ContextLevel) contextState.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL);
            stats.addContextLevel(effectiveLevel != null ? effectiveLevel.name() : initialLevel.name());

            final VisualBaselineGateStep visualBaselineGateStep = new VisualBaselineGateStep(step, session);
            final boolean isBypassed = visualBaselineGateStep.executeGate(contextState);
            if (isBypassed)
            {
                step.setDurationMs(System.currentTimeMillis() - stepStartTime);
                contextState.getTransientData().put("KEY_LAST_STEP_END_TIME", System.currentTimeMillis());

                step.setStatus(PlaybookStepStatus.SUCCESS);
                if (session != null && session.getEventBus() != null)
                {
                    session.getEventBus().dispatch(new StepFinishedEvent(step, PlaybookStepStatus.SUCCESS));
                }
                return;
            }

            final VerifyOutcomeStep verifyStep = new VerifyOutcomeStep();
            final List<PipelineStep> standardFlow = new ArrayList<>();

            // Pre-step visual state capture for outcome verification
            standardFlow.add(c ->
            {
                final SutState incomingState = (SutState) c.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
                if (incomingState != null && incomingState.getAttachments() != null && !incomingState.getAttachments().isEmpty())
                {
                    c.getTransientData().put(ExecutionContext.KEY_PRE_ACTION_STATE, incomingState);
                }
                else if (AiConfiguration.getInstance().isSemanticVerificationEnabled())
                {
                    final TargetExecutor executor = (TargetExecutor) c.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                    if (executor != null)
                    {
                        try
                        {
                            final boolean isFullPageReq = Boolean.TRUE.equals(c.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT"))
                                    || (step != null && step.isFullPageVisualStep());
                            final ContextLevel cl = isFullPageReq ? ContextLevel.VISUAL_LEAN : ContextLevel.VISUAL;
                            final SutState initialVisual = executor.captureState(cl, isFullPageReq);
                            if (initialVisual != null && initialVisual.getAttachments() != null && !initialVisual.getAttachments().isEmpty())
                            {
                                c.getTransientData().put(ExecutionContext.KEY_PRE_ACTION_STATE, initialVisual);
                            }
                        }
                        catch (final Exception e)
                        {
                            LOGGER.debug("Failed to capture pre-step visual state: {}", e.getMessage());
                        }
                    }
                }
            });

            if (isReplay)
            {
                standardFlow.add(c ->
                {
                    final boolean isRecorded = step.getToolCalls() != null && !step.getToolCalls().isEmpty();
                    final boolean isVisualOnly = step.getScreenshotHash() != null && !step.getScreenshotHash().isEmpty();
                    final boolean isComposite = step.getSubSteps() != null && !step.getSubSteps().isEmpty();
                    final boolean isRecordedCompletedStep = initialStepStatus != null && initialStepStatus != PlaybookStepStatus.PENDING;
                    if (mode == ExecutionMode.REPLAY_STRICT && !isRecorded && !isVisualOnly && !isComposite && !isRecordedCompletedStep)
                    {
                        final String resolvedStrictStep = c.getSessionData() != null
                            ? c.getSessionData().resolveVariables(step.getInstruction())
                            : step.getInstruction();
                        throw new ConclusiveFailureException(
                            "No recorded tool calls found for step '" + resolvedStrictStep + "' in REPLAY_STRICT mode. Companion JSON recording file is missing or step was not recorded.");
                    }

                    if (step.getStatus() == PlaybookStepStatus.FAILED || step.isFailed())
                    {
                        final String reason = step.getFailureReason() != null && !step.getFailureReason().trim().isEmpty()
                            ? step.getFailureReason()
                            : "Recorded step execution failed.";
                        throw new ConclusiveFailureException(reason);
                    }

                    final TargetExecutor executor = (TargetExecutor) c.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                    if (executor != null)
                    {
                        try
                        {
                            final SutState state = executor.captureState(ContextLevel.STANDARD);
                            c.getTransientData().put(ExecutionContext.KEY_LAST_STATE, state);
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }

                    ToolRegistry reg = (ToolRegistry) c.getTransientData().get("KEY_TOOL_REGISTRY");
                    if (reg == null)
                    {
                        reg = new ToolRegistry();
                        BrowserToolProvider.registerBrowserTools(reg);
                        c.getTransientData().put("KEY_TOOL_REGISTRY", reg);
                    }

                    try
                    {
                        final SimpleToolContext toolContext = new SimpleToolContext(reg);
                        if (executor != null)
                        {
                            toolContext.setVariable("neodymium.targetExecutor", executor);
                        }
                        PlaybookToolReplayer.replayStep(step, reg, toolContext, c.getSessionData(), executor);
                    }
                    catch (final Throwable t)
                    {
                        if (mode.supportsHealing() && !step.isNoHealing())
                        {
                            throw new HealingRequiredException("Replay step execution failed against SUT: " + t.getMessage(), t);
                        }
                        if (t instanceof RuntimeException re)
                        {
                            throw re;
                        }
                        if (t instanceof Error err)
                        {
                            throw err;
                        }
                        throw new RuntimeException(t);
                    }
                });
            }
            else
            {
                // Live mode: AgentToolLoopStep performs Think -> ToolCall -> Observe -> Finish
                standardFlow.add(new AgentToolLoopStep());
            }

            // Post-step visual state capture for outcome verification & report
            standardFlow.add(c ->
            {
                final TargetExecutor executor = (TargetExecutor) c.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                if (executor != null && session != null && session.getEventBus() != null)
                {
                    try
                    {
                        final boolean hasMutatingAction = step != null && step.getActions() != null && step.getActions().stream()
                            .anyMatch(a -> a != null && a.getType() != null
                                && !a.getType().toUpperCase().startsWith("ASSERT")
                                && !"NONE".equalsIgnoreCase(a.getType())
                                && !"VERIFY".equalsIgnoreCase(a.getType()));

                        final long settleMs = AiConfiguration.getInstance().getVisualPostActionSettleMs();
                        if (settleMs > 0 && hasMutatingAction)
                        {
                            try
                            {
                                Thread.sleep(settleMs);
                            }
                            catch (final InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                            }
                        }

                        final boolean isFullPageReq = Boolean.TRUE.equals(c.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT"))
                                || (step != null && step.isFullPageVisualStep());
                        final ContextLevel cl = isFullPageReq ? ContextLevel.VISUAL_LEAN : ContextLevel.VISUAL;
                        final SutState postStepState = executor.captureState(cl, isFullPageReq);
                        if (postStepState != null)
                        {
                            c.getTransientData().put(ExecutionContext.KEY_POST_ACTION_STATE, postStepState);
                            session.getEventBus().dispatch(new StateCapturedEvent(postStepState));
                        }
                    }
                    catch (final Exception e)
                    {
                        LOGGER.debug("Failed to capture post-step visual state: {}", e.getMessage());
                    }
                }
            });

            if (AiConfiguration.getInstance().isSemanticVerificationEnabled())
            {
                standardFlow.add(verifyStep);
            }

            // Register exception handlers based on execution mode
            final Map<Class<? extends PipelineException>, PipelineStep> handlers = new HashMap<>();

            if (!step.isNoHealing() && (mode.isLive() || mode.supportsHealing()))
            {
                handlers.put(HealingRequiredException.class, c ->
                {
                    c.getTransientData().put(ExecutionContext.KEY_IS_HEALED_STEP, true);
                    LOGGER.warn("⚠️ Replay step requires online healing — launching AgentToolLoopStep for: \"{}\"", step.getInstruction());
                    if (AiConfiguration.getInstance().isSemanticVerificationEnabled())
                    {
                        c.pushStep(verifyStep);
                    }
                    c.pushStep(new AgentToolLoopStep());
                });
            }

            final TryCatchStep tryCatch = new TryCatchStep(new SequenceStep(standardFlow), handlers);

            // Push end-hook step first, so it runs AFTER tryCatch executes
            contextState.pushStep(c ->
            {
                if (step.getSubSteps() != null && !step.getSubSteps().isEmpty())
                {
                    return;
                }
                final Boolean isHealed = (Boolean) c.getTransientData().get(ExecutionContext.KEY_IS_HEALED_STEP);
                if (Boolean.TRUE.equals(isHealed))
                {
                    step.setStatus(PlaybookStepStatus.HEALED);
                }
                else
                {
                    step.setStatus(PlaybookStepStatus.SUCCESS);
                }
                step.setFailed(false);
                step.setFailureReason(null);
                if (step.getStartTimeMs() != null)
                {
                    step.setDurationMs(System.currentTimeMillis() - step.getStartTimeMs());
                }
                final Object statsObj = c.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof StepStats stepStats)
                {
                    stepStats.setDurationMs(System.currentTimeMillis() - stepStats.getStartTime());
                }

                if (step.isBug())
                {
                    final String bugComment = step.getBugDetails();
                    final String bugStr = bugComment != null ? " (" + bugComment + ")" : "";
                    final String resolvedBugInstruction = c.getSessionData() != null
                        ? c.getSessionData().resolveVariables(step.getInstruction())
                        : step.getInstruction();
                    final String msg = String.format("Expected bug%s but step succeeded: %s:%d (%s)",
                        bugStr, step.getSourceFile(), step.getLineNumber(), resolvedBugInstruction);
                    LOGGER.error("   ❌ {}", msg);

                    if (!step.isContinueOnError())
                    {
                        throw new ExpectedBugNotReproducedException(msg);
                    }
                    else
                    {
                        @SuppressWarnings("unchecked")
                        final List<String> warnings = (List<String>) c.getTransientData()
                            .computeIfAbsent("verificationWarnings", k -> new ArrayList<String>());
                        warnings.add(msg);
                    }
                }

                final Long origTimeout = (Long) c.getTransientData().remove("KEY_ORIG_SELENIDE_TIMEOUT");
                if (origTimeout != null)
                {
                    Configuration.timeout = origTimeout;
                }

                c.getTransientData().remove(ExecutionContext.KEY_POST_ACTION_STATE);
                c.getTransientData().remove(ExecutionContext.KEY_PRE_ACTION_STATE);
                c.getTransientData().remove("KEY_IS_FULL_PAGE_SCREENSHOT");

                if (session != null && session.getEventBus() != null)
                {
                    session.getEventBus().dispatch(new StepFinishedEvent(step, step.getStatus()));
                }
            });

            contextState.pushStep(tryCatch);
        };
    }

    private static StepStats getOrCreateStatsForStep(
        final PlaybookStep step,
        final long startTime,
        final boolean replayed,
        final Map<PlaybookStep, StepStats> stepStatsMap,
        final List<StepStats> allStats,
        final ExecutionContext contextState
    )
    {
        StepStats stats = stepStatsMap.get(step);
        if (stats == null)
        {
            final String raw = step.getInstruction();
            final String resolved = (contextState != null && contextState.getSessionData() != null)
                ? contextState.getSessionData().resolveVariables(raw)
                : raw;
            stats = new StepStats(resolved, startTime);
            stats.setReplayed(replayed);
            if (step.getSemanticIntent() != null)
            {
                stats.setSemanticIntent(step.getSemanticIntent().name());
            }
            stepStatsMap.put(step, stats);

            final PlaybookStep parentStep = step.getParent();
            if (parentStep != null)
            {
                final StepStats parentStats = getOrCreateStatsForStep(parentStep, startTime, replayed, stepStatsMap, allStats, contextState);
                parentStats.getSubStats().add(stats);
            }
            else
            {
                allStats.add(stats);
            }
        }
        else if (stats.getSemanticIntent() == null && step.getSemanticIntent() != null)
        {
            stats.setSemanticIntent(step.getSemanticIntent().name());
        }
        return stats;
    }

    /**
     * Prepares the instruction by stripping all explicit control tags case-insensitively.
     *
     * @param instruction the raw instruction prompt
     * @return the cleaned instruction string
     */
    public static String prepareInstruction(final String instruction)
    {
        if (instruction == null)
        {
            return null;
        }
        String prepared = instruction;
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*no-replay\\s*\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*bug(?:\\s*:\\s*[^)]+)?\\s*\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*continue-on-error\\s*\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*no-healing\\s*\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*(optional|soft)\\s*\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*timeout\\s*:\\s*\\d+(?:ms|s)?\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*visual(?:\\s*:\\s*full)?\\s*\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*layout\\s*\\)\\s*", " ");
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*hint(?:\\s*:\\s*[^)]+)?\\s*\\)\\s*", " ");
        return prepared.replaceAll("\\s+", " ").trim();
    }
}
