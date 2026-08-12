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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.DivergenceException;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import com.codeborne.selenide.ex.ElementNotFound;
import org.openqa.selenium.NoSuchElementException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.structural.SequenceStep;
import org.neodymium.ai.pipeline.structural.TryCatchStep;
import org.neodymium.ai.playbook.PlaybookParser;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.prompt.ActionSanitizer;
import org.neodymium.ai.prompt.AiPrompt;
import org.neodymium.ai.prompt.DefaultActionSanitizer;
import org.neodymium.ai.prompt.PesapPrompt;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.neodymium.ai.session.AiSession;

/**
 * Concrete pipeline step executing actions parsed from LLM responses, sanitizing/parameterizing
 * their inputs on the fly, storing execution history, and dispatching executed events.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecuteActionsStep implements PipelineStep
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class);
    private static final java.util.regex.Pattern TIMEOUT_PATTERN = java.util.regex.Pattern.compile("(?i)\\(\\s*timeout\\s*:\\s*(\\d+)(ms|s)?\\s*\\)");

    /**
     * The sanitizer used for variable parameterization of recorded actions.
     */
    private final ActionSanitizer actionSanitizer = new DefaultActionSanitizer();

    /**
     * Constructs an ExecuteActionsStep.
     */
    public ExecuteActionsStep()
    {
    }

    /**
     * Retrieves the LLM actions result, executes each action, sanitizes them,
     * appends them to the session recording log, and dispatches status events.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if an action fails (triggers HealingRequiredException)
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        // Retrieve the active session from context transient storage
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        // Retrieve SUT target executor driving browser/REST operations
        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);

        if (session == null)
        {
            throw new ConclusiveFailureException("No active AiSession registered in ExecutionContext transient data");
        }
        if (executor == null)
        {
            throw new ConclusiveFailureException("No active TargetExecutor registered in ExecutionContext transient data");
        }

        // Retrieve the list of actions returned by the prior CallLlmStep execution
        @SuppressWarnings("unchecked")
        final List<Action> actions = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_LAST_LLM_RESULT);
        if (actions == null)
        {
            return;
        }

        // Initialize or fetch the concurrent recording collection tracking all executed playbooks actions
        @SuppressWarnings("unchecked")
        final List<Action> recordedActions = (List<Action>) context.getTransientData()
            .computeIfAbsent(ExecutionContext.KEY_RECORDING, k -> new CopyOnWriteArrayList<>());

        // Execute each parsed action sequentially
        for (final Action action : actions)
        {
            if (action != null)
            {
                executeSingleAction(action, executor, recordedActions, session, context);
            }
        }
    }

    /**
     * Helper executing a single action, applying sanitization, logging, and throwing healing exceptions on failure.
     */
    @SuppressWarnings("deprecation")
    private void executeSingleAction(
        final Action action,
        final TargetExecutor executor,
        final List<Action> recordedActions,
        final AiSession session,
        final ExecutionContext context
    ) throws PipelineException
    {
        final ExecutionContext previousContext = ExecutionContext.getActiveContext();
        ExecutionContext.setActiveContext(context);
        try
        {
            if (action == null)
            {
                return;
            }

            // Intercept control / assertion actions that require no SUT execution
            if ("NONE".equalsIgnoreCase(action.getType()) || "VERIFY".equalsIgnoreCase(action.getType()))
            {
                return;
            }

            // Intercept INCLUDE control actions to perform dynamic runtime inclusion expansion
            if (action.getType().equalsIgnoreCase("INCLUDE"))
            {
                executeIncludeAction(action, session, context, recordedActions);
                return;
            }

            final Action resolvedAction = resolveActionVariables(action, context.getSessionData());

            try
            {
                LOGGER.debug("   ▶️ [Action] Type:        {}", resolvedAction.getType());
                if (resolvedAction.getDescription() != null && !resolvedAction.getDescription().trim().isEmpty())
                {
                    LOGGER.debug("      🤖 Description: {}", resolvedAction.getDescription());
                }
                if (resolvedAction.getTarget() != null && !resolvedAction.getTarget().trim().isEmpty())
                {
                    LOGGER.debug("      🎯 Target:      {}", resolvedAction.getTarget());
                }
                final String val = resolvedAction.getValue();
                if (val != null && !val.trim().isEmpty())
                {
                    LOGGER.debug("      💵 Value:       {}", val);
                }

                // Execute SUT action via targeted SUT driver
                // Mask any raw sensitive inputs dynamically matching SessionData variable keys
                Action sanitized = this.actionSanitizer.sanitize(action, context.getSessionData());
                org.neodymium.ai.config.ExecutionMode mode = (org.neodymium.ai.config.ExecutionMode) context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
                if (mode == null && session != null)
                {
                    mode = session.getExecutionMode();
                }
                final PlaybookStep step = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
                final boolean isNoReplay = step != null && step.isNoReplay();
                final boolean isReplayingStep = mode != null && mode.isReplay() && !isNoReplay && (step == null || mode == org.neodymium.ai.config.ExecutionMode.REPLAY_STRICT || (step.getActions() != null && (!step.getActions().isEmpty() || step.getScreenshotHash() != null)));

                if (step != null)
                {
                     sanitized.setStepInstruction(step.getInstruction());
                     sanitized.setStepLine(step.getLineNumber());
                     sanitized.setStepFile(step.getSourceFile());
                     if (!isReplayingStep)
                     {
                         if (isNoReplay && Boolean.TRUE.equals(context.getTransientData().get("KEY_CURRENT_STEP_FIRST_ACTION")))
                         {
                             step.getActions().clear();
                             context.getTransientData().put("KEY_CURRENT_STEP_FIRST_ACTION", false);
                         }
                         step.getActions().add(sanitized);
                     }
                }
                
                // Log to local recording and dispatch verification updates to active event listeners
                recordedActions.add(sanitized);
                @SuppressWarnings("unchecked")
                final List<Action> stepActions = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_CURRENT_STEP_ACTIONS);
                if (stepActions != null)
                {
                    stepActions.add(sanitized);
                }

                final String rawInstruction = (String) context.getTransientData().get("KEY_CURRENT_STEP_RAW_INSTRUCTION");
                Long customTimeoutMs = null;
                if (rawInstruction != null)
                {
                    final java.util.regex.Matcher m = TIMEOUT_PATTERN.matcher(rawInstruction);
                    if (m.find())
                    {
                        final long parsedVal = Long.parseLong(m.group(1));
                        final String unit = m.group(2);
                        customTimeoutMs = "s".equalsIgnoreCase(unit) ? parsedVal * 1000L : parsedVal;
                    }
                }

                final long origTimeout = com.codeborne.selenide.Configuration.timeout;
                if (customTimeoutMs != null)
                {
                    com.codeborne.selenide.Configuration.timeout = customTimeoutMs;
                }

                try
                {
                    if (executor instanceof org.neodymium.ai.executor.selenide.SelenideTargetExecutor ste)
                    {
                        ste.setExecutionContext(context);
                    }
                    context.getTransientData().put("currentAction", resolvedAction);
                    executor.execute(resolvedAction);

                    if (!isReplayingStep
                        && executor != null
                        && executor.supportsLocatorImprovement()
                        && org.neodymium.ai.config.AiConfiguration.getInstance().isLocatorImproverEnabled()
                        && com.codeborne.selenide.WebDriverRunner.hasWebDriverStarted()
                        && resolvedAction.getTarget() != null
                        && !resolvedAction.getTarget().isBlank())
                    {
                        try
                        {
                            final org.openqa.selenium.WebDriver driver = com.codeborne.selenide.WebDriverRunner.getWebDriver();
                            final com.codeborne.selenide.SelenideElement found = org.neodymium.ai.executor.selenide.SelenideElementFinder.findElement(resolvedAction.getTarget());
                            if (found != null && found.toWebElement() != null)
                            {
                                final String improvedLocator = org.neodymium.ai.util.LocatorImprover.improveLocator(driver, found.toWebElement(), resolvedAction.getTarget());
                                if (improvedLocator != null && !improvedLocator.equals(resolvedAction.getTarget()))
                                {
                                    final Action upgraded = sanitized.withTarget(improvedLocator);
                                    if (step.getActions() != null && !step.getActions().isEmpty())
                                    {
                                        final int idx = step.getActions().indexOf(sanitized);
                                        if (idx != -1)
                                        {
                                            step.getActions().set(idx, upgraded);
                                        }
                                    }
                                    if (recordedActions != null && !recordedActions.isEmpty())
                                    {
                                        final int idx = recordedActions.indexOf(sanitized);
                                        if (idx != -1)
                                        {
                                            recordedActions.set(idx, upgraded);
                                        }
                                    }
                                    if (stepActions != null && !stepActions.isEmpty())
                                    {
                                        final int idx = stepActions.indexOf(sanitized);
                                        if (idx != -1)
                                        {
                                            stepActions.set(idx, upgraded);
                                        }
                                    }
                                    sanitized = upgraded;
                                }
                            }
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                }
                finally
                {
                    com.codeborne.selenide.Configuration.timeout = origTimeout;
                    context.getTransientData().remove("currentAction");
                }
                
                final long settleMs = org.neodymium.ai.config.AiConfiguration.getInstance()
                    .getLong("neodymium.ai.visual.postActionSettleMs", 1000L);
                if (settleMs > 0)
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

                if (executor != null && !isReplayingStep)
                {
                    try
                    {
                        final org.neodymium.ai.executor.selenide.ContextLevel cl = (step != null && step.isVisualStep())
                            ? org.neodymium.ai.executor.selenide.ContextLevel.VISUAL
                            : org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_LEAN;
                        final SutState postActionState = executor.captureState(cl);
                        if (postActionState != null)
                        {
                            context.getTransientData().put("KEY_POST_ACTION_STATE", postActionState);
                        }
                    }
                    catch (final Exception e)
                    {
                        LOGGER.debug("Failed to capture post-action state for visual baseline: {}", e.getMessage());
                    }
                }

                session.getEventBus().dispatch(new ActionExecutedEvent(sanitized, true));
                context.getTransientData().remove(ExecutionContext.KEY_LAST_EXECUTION_ERROR);

                if (Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_CONTINUATION_STEP")))
                {
                    context.getTransientData().put("KEY_IS_CONTINUATION_STEP", false);
                    LOGGER.info("   🔄 Prelude action executed for instruction — initiating continuation LLM step with updated DOM context.");

                    @SuppressWarnings("unchecked")
                    final AiPrompt<List<Action>> activePrompt = (AiPrompt<List<Action>>) context.getTransientData().get(ExecutionContext.KEY_ACTIVE_PROMPT);
                    final org.neodymium.ai.executor.selenide.ContextLevel currentLevel =
                        context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL) instanceof org.neodymium.ai.executor.selenide.ContextLevel cl
                            ? cl
                            : org.neodymium.ai.executor.selenide.ContextLevel.LEAN;

                    final LlmCapability capability = currentLevel.includesScreenshot() ? LlmCapability.VISION : LlmCapability.TEXT_ONLY;
                    final CallLlmStep<List<Action>> continuationLlmStep = new CallLlmStep<>(activePrompt, capability);

                    context.pushStep(new VerifyOutcomeStep());
                    context.pushStep(this);
                    if (org.neodymium.ai.config.AiConfiguration.getInstance().isJudgeEnabled())
                    {
                        context.pushStep(new QualityJudgeStep());
                    }
                    context.pushStep(continuationLlmStep);
                    context.pushStep(new CaptureStateStep());
                }
            }
            catch (final Throwable t)
            {
                if (t instanceof VirtualMachineError || t instanceof ThreadDeath || t instanceof LinkageError)
                {
                    throw (Error) t;
                }

                // Dispatch failed event status and log error immediately
                final String failureMsg = t.getMessage() != null ? t.getMessage() : t.toString();
                LOGGER.error("   ❌ Action execution failed on SUT: {}", failureMsg);
                session.getEventBus().dispatch(new ActionExecutedEvent(action, false));
                context.getTransientData().put(
                    ExecutionContext.KEY_LAST_EXECUTION_ERROR,
                    "Action " + action.getType() + " on locator '" + action.getTarget() + "' failed: " + failureMsg
                );
                if (t instanceof PipelineException pe)
                {
                    throw pe;
                }

                final boolean isElementNotFound = t instanceof ElementNotFound
                    || t instanceof NoSuchElementException
                    || (failureMsg != null && (failureMsg.contains("Element not found") || failureMsg.contains("ElementNotFound")));

                boolean isAssertionFailure = !isElementNotFound && (t instanceof AssertionError);
                Throwable cause = t.getCause();
                while (!isAssertionFailure && !isElementNotFound && cause != null && cause != t)
                {
                    if (cause instanceof AssertionError)
                    {
                        isAssertionFailure = true;
                        break;
                    }
                    cause = cause.getCause();
                }

                if ("ASSERT".equalsIgnoreCase(action.getType()))
                {
                    if (isAssertionFailure)
                    {
                        final Throwable finalCause = t;
                        throw new ConclusiveFailureException("Assertion failed: " + failureMsg, finalCause);
                    }
                    else
                    {
                        throw new HealingRequiredException("Action execution failed against SUT: " + action.getDescription() + " (" + failureMsg + ")", t);
                    }
                }

                throw new HealingRequiredException("Action execution failed against SUT: " + action.getDescription() + " (" + failureMsg + ")", t);
            }
        }
        finally
        {
            ExecutionContext.setActiveContext(previousContext);
        }
    }

    /**
     * Executes INCLUDE control action dynamically by resolving, parsing, and pushing steps on stack.
     */
    private void executeIncludeAction(
        final Action action,
        final AiSession session,
        final ExecutionContext context,
        final List<Action> recordedActions
    ) throws PipelineException
    {
        // Extract include path target from SUT action definition
        final String pathTemp = action.getTarget();
        final String path = (pathTemp == null || pathTemp.trim().isEmpty()) ? action.getValue() : pathTemp;
        if (path == null || path.trim().isEmpty())
        {
            throw new ConclusiveFailureException("INCLUDE action target path is null or empty");
        }

        // Retrieve registered PlaybookResourceManager from the context state
        final PlaybookResourceManager manager = (PlaybookResourceManager) context.getTransientData().get(ExecutionContext.KEY_RESOURCE_MANAGER);
        if (manager == null)
        {
            throw new ConclusiveFailureException("No PlaybookResourceManager registered in ExecutionContext transient data");
        }

        // Retrieve or instantiate the default YamlPlaybookParser
        PlaybookParser parser = (PlaybookParser) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK_PARSER);
        if (parser == null)
        {
            parser = new YamlPlaybookParser();
        }

        // Fetch thread-safe stack listing active includes to detect cycle inclusions
        @SuppressWarnings("unchecked")
        final List<String> runtimeStack = (List<String>) context.getTransientData()
            .computeIfAbsent(ExecutionContext.KEY_RUNTIME_INCLUDE_STACK, k -> new ArrayList<>());

        if (runtimeStack.contains(path))
        {
            throw new ConclusiveFailureException("Circular dynamic inclusion detected: " + String.join(" -> ", runtimeStack) + " -> " + path);
        }

        // Resolve absolute or relative path context based on active parent directory
        final String currentParent = (String) context.getTransientData().getOrDefault(ExecutionContext.KEY_CURRENT_PLAYBOOK_IDENTIFIER, "");
        final String resolvedIdentifier = manager.resolveInclude(currentParent, path);

        // Add to callstack before parsing to cover circular validations
        runtimeStack.add(path);
        try
        {
            // Parse included YAML playbook target
            final Playbook playbook = parser.parse(resolvedIdentifier, manager);
            final List<PlaybookStep> playbookSteps = playbook.getSteps();

            final PlaybookStep includeStep = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
            // Push included sub-steps onto LIFO stack in reverse order to ensure sequential execution
            for (int i = playbookSteps.size() - 1; i >= 0; i--)
            {
                final PlaybookStep step = playbookSteps.get(i);
                if (includeStep != null)
                {
                    step.setParent(includeStep);
                }
                final PipelineStep stepPipeline = mapPlaybookStepToPipelineStep(step, session, context);
                context.pushStep(stepPipeline);
            }
            
            // Parameterize and log the INCLUDE step record in history
            final Action sanitized = this.actionSanitizer.sanitize(action, context.getSessionData());
            recordedActions.add(sanitized);
            @SuppressWarnings("unchecked")
            final List<Action> stepActions = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_CURRENT_STEP_ACTIONS);
            if (stepActions != null)
            {
                stepActions.add(sanitized);
            }
            session.getEventBus().dispatch(new ActionExecutedEvent(sanitized, true));
        }
        catch (final IOException e)
        {
            throw new ConclusiveFailureException("Failed to read or parse included playbook: " + path, e);
        }
        finally
        {
            // Clean stack isolation frame
            runtimeStack.remove(runtimeStack.size() - 1);
        }
    }

    /**
     * Maps parsed PlaybookStep to concrete PipelineStep execution tree.
     */
    public static PipelineStep mapPlaybookStepToPipelineStep(
        final PlaybookStep step,
        final AiSession session,
        final ExecutionContext context
    )
    {
        // For composite steps, map and execute all children sequentially using SequenceStep
        if (step.isComposite())
        {
            final List<PipelineStep> subPipelineSteps = new ArrayList<>();
            for (final PlaybookStep subStep : step.getSubSteps())
            {
                subPipelineSteps.add(mapPlaybookStepToPipelineStep(subStep, session, context));
            }
            return new SequenceStep(subPipelineSteps);
        }

        // For leaf steps, return a pipeline step wrapper setting the active instruction and pushing execution loop
        return contextState -> {
            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);
            step.setStatus(org.neodymium.ai.model.PlaybookStepStatus.RUNNING);
            final String rawInstruction = step.getInstruction();
            final String resolvedInstruction = contextState.getSessionData().resolveVariables(rawInstruction);
            final String preparedInstruction = prepareInstruction(resolvedInstruction);
            contextState.getTransientData().put("KEY_CURRENT_STEP_RAW_INSTRUCTION", resolvedInstruction);
            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, preparedInstruction);
            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_STEP_ACTIONS, new CopyOnWriteArrayList<Action>());

            final boolean stepNoReplay = step.isNoReplay();
            contextState.getTransientData().put("KEY_CURRENT_STEP_NO_REPLAY", stepNoReplay);
            if (stepNoReplay)
            {
                contextState.getTransientData().put("KEY_CURRENT_STEP_FIRST_ACTION", true);
            }

            @SuppressWarnings("unchecked")
            List<org.neodymium.ai.pipeline.StepStats> allStats = (List<org.neodymium.ai.pipeline.StepStats>) contextState.getTransientData().get("execution.stepStatsList");
            if (allStats == null)
            {
                allStats = new java.util.ArrayList<>();
                contextState.getTransientData().put("execution.stepStatsList", allStats);
            }

            @SuppressWarnings("unchecked")
            final Map<PlaybookStep, org.neodymium.ai.pipeline.StepStats> stepStatsMap =
                (Map<PlaybookStep, org.neodymium.ai.pipeline.StepStats>) contextState.getTransientData()
                    .computeIfAbsent("execution.stepStatsMap", k -> new java.util.HashMap<>());

            final org.neodymium.ai.config.ExecutionMode executionMode = (org.neodymium.ai.config.ExecutionMode) contextState.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
            final boolean isReplayStats = executionMode != null && executionMode.isReplay() && !stepNoReplay;

            final org.neodymium.ai.pipeline.StepStats stats = getOrCreateStatsForStep(step, System.currentTimeMillis(), isReplayStats, stepStatsMap, allStats);
            contextState.getTransientData().put("KEY_CURRENT_STEP_STATS", stats);

            @SuppressWarnings("unchecked")
            final Set<PlaybookStep> alreadySplitSteps = (Set<PlaybookStep>) contextState.getTransientData()
                .computeIfAbsent("pesap.alreadySplitSteps", k -> new HashSet<>());

            org.neodymium.ai.executor.selenide.ContextLevel initialLevel = org.neodymium.ai.executor.selenide.ContextLevel.MINIMAL;
            final String lower = resolvedInstruction.toLowerCase();
            if (lower.contains("(visual)"))
            {
                initialLevel = org.neodymium.ai.executor.selenide.ContextLevel.VISUAL;
            }
            else if (lower.contains("(layout)"))
            {
                initialLevel = org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_RICH;
            }
            else if (lower.contains("(hint:"))
            {
                initialLevel = org.neodymium.ai.executor.selenide.ContextLevel.HINT;
            }

            @SuppressWarnings("unchecked")
            final List<PlaybookStep> flatSteps = (List<PlaybookStep>) contextState.getTransientData().get("playbook.flatSteps");

            if (session != null && session.getEventBus() != null)
            {
                final int stepIndex = flatSteps != null ? flatSteps.indexOf(step) : 0;
                session.getEventBus().dispatch(new org.neodymium.ai.event.structural.StepStartedEvent(step, stepIndex));
            }

            if (step.getStatus() == org.neodymium.ai.model.PlaybookStepStatus.SKIPPED)
            {
                LOGGER.info("   ⏭️ Skipping step execution per user request: \"{}\"", resolvedInstruction);
                if (session != null && session.getEventBus() != null)
                {
                    session.getEventBus().dispatch(new org.neodymium.ai.event.structural.StepFinishedEvent(step, org.neodymium.ai.model.PlaybookStepStatus.SKIPPED));
                }
                return;
            }

            LOGGER.debug("================================================================================");
            if (flatSteps != null && flatSteps.contains(step))
            {
                final int stepIndex = flatSteps.indexOf(step) + 1;
                LOGGER.debug("▶ [Step {}/{}] Instruction: \"{}\"", stepIndex, flatSteps.size(), resolvedInstruction);
            }
            else
            {
                LOGGER.debug("▶ [Step] Instruction: \"{}\"", resolvedInstruction);
            }

            if (step.getSourceFile() != null && !step.getSourceFile().isEmpty())
            {
                LOGGER.debug("       Location:    {}:{}", step.getSourceFile(), step.getLineNumber());
            }
            if (executionMode != null)
            {
                LOGGER.debug("       Mode:        {}", executionMode);
            }

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
            LOGGER.debug("================================================================================");

            final boolean isReplayMode = executionMode != null && executionMode.isReplay();
            final org.neodymium.ai.config.AiConfiguration config = org.neodymium.ai.config.AiConfiguration.getInstance();
            if (!isReplayMode && config.getBoolean("neodymium.ai.pesap.enabled", true) && !alreadySplitSteps.contains(step))
            {
                alreadySplitSteps.add(step);
                try
                {
                    String previousInstruction = null;
                    final List<String> nextInstructions = new ArrayList<>();
                    if (flatSteps != null)
                    {
                        final int idx = flatSteps.indexOf(step);
                        if (idx != -1)
                        {
                            if (idx > 0)
                            {
                                previousInstruction = contextState.getSessionData().resolveVariables(flatSteps.get(idx - 1).getInstruction());
                            }
                            for (int i = idx + 1; i < flatSteps.size() && nextInstructions.size() < 2; i++)
                            {
                                nextInstructions.add(contextState.getSessionData().resolveVariables(flatSteps.get(i).getInstruction()));
                            }
                        }
                    }

                    final PesapPrompt pesapPrompt = new PesapPrompt(resolvedInstruction, previousInstruction, nextInstructions);
                    final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.PESAP);
                    final double temp = config.getTemperature("action");
                    final int timeoutSeconds = config.getTimeoutSeconds("action");

                    final LlmRequest request = new LlmRequest(
                        pesapPrompt.compileSystemMessage(contextState),
                        pesapPrompt.compileUserMessage(contextState),
                        Collections.emptyList(),
                        pesapPrompt.getResponseSchema(),
                        temp,
                        timeoutSeconds
                    );

                    LOGGER.debug("💬 [Pre-Step PESAP] Running analysis using provider '{}'", provider.getClass().getSimpleName());
                    if (LOGGER.isTraceEnabled())
                    {
                        LOGGER.trace("System Prompt:\n{}", request.systemMessage());
                        LOGGER.trace("User Prompt:\n{}", request.userMessage());
                    }

                    final long startTime = System.currentTimeMillis();
                    final LlmResponse response = provider.chat(request);
                    final long durationMs = System.currentTimeMillis() - startTime;

                    LOGGER.debug("LLM response received. Length: {} chars (duration: {} ms)", response.content() != null ? response.content().length() : 0, durationMs);
                    if (LOGGER.isTraceEnabled())
                    {
                        LOGGER.trace("Raw response content:\n{}", CallLlmStep.formatJsonForLogging(response.content()));
                    }

                    contextState.getTransientData().compute("pesapCallCount", (k, v) -> v == null ? 1 : ((Integer) v) + 1);

                    final org.neodymium.ai.client.TokenUsage newUsage = response.tokenUsage();
                    if (newUsage != null)
                    {
                        stats.addPesapCall(newUsage.inputTokenCount(), newUsage.outputTokenCount(), newUsage.cachedTokenCount());

                        LOGGER.debug("   📊 [Pre-Step PESAP] Tokens: {} in ({} cached) → {} out (total: {})",
                            newUsage.inputTokenCount(), newUsage.cachedTokenCount(), newUsage.outputTokenCount(), newUsage.totalTokenCount());

                        final org.neodymium.ai.client.TokenUsage existing = (org.neodymium.ai.client.TokenUsage) contextState.getTransientData().get(ExecutionContext.KEY_PESAP_TOKEN_USAGE);
                        if (existing == null)
                        {
                            contextState.getTransientData().put(ExecutionContext.KEY_PESAP_TOKEN_USAGE, newUsage);
                        }
                        else
                        {
                            contextState.getTransientData().put(ExecutionContext.KEY_PESAP_TOKEN_USAGE, new org.neodymium.ai.client.TokenUsage(
                                existing.inputTokenCount() + newUsage.inputTokenCount(),
                                existing.outputTokenCount() + newUsage.outputTokenCount(),
                                existing.totalTokenCount() + newUsage.totalTokenCount(),
                                existing.cachedTokenCount() + newUsage.cachedTokenCount()
                            ));
                        }
                    }

                    final PesapPrompt.PesapResult pesapResult = pesapPrompt.parseResponse(response.content(), contextState);

                    if (pesapResult.splitSteps() != null && pesapResult.splitSteps().size() > 1)
                    {
                        LOGGER.info("✂️ Upfront JIT step split detected: \"{}\" split into {}", resolvedInstruction, pesapResult.splitSteps());
                        for (final String part : pesapResult.splitSteps())
                        {
                            final PlaybookStep subStep = new PlaybookStep(part);
                            subStep.setSourceFile(step.getSourceFile());
                            subStep.setLineNumber(step.getLineNumber());
                            subStep.setParent(step);
                            
                            // Explicitly copy control flags from the parent step
                            subStep.setBug(step.isBug());
                            subStep.setBugDetails(step.getBugDetails());
                            subStep.setContinueOnError(step.isContinueOnError());
                            subStep.setNoHealing(step.isNoHealing());
                            subStep.setOptional(step.isOptional());
                            
                            step.getSubSteps().add(subStep);
                        }

                        final List<PipelineStep> subPipelineSteps = new ArrayList<>();
                        for (final PlaybookStep subStep : step.getSubSteps())
                        {
                            subPipelineSteps.add(mapPlaybookStepToPipelineStep(subStep, session, context));
                        }
                        for (int i = subPipelineSteps.size() - 1; i >= 0; i--)
                        {
                            contextState.pushStep(subPipelineSteps.get(i));
                        }
                        return;
                    }

                    if (pesapResult.contextLevel() != null)
                    {
                        try
                        {
                            initialLevel = org.neodymium.ai.executor.selenide.ContextLevel.valueOf(pesapResult.contextLevel().toUpperCase().trim());
                        }
                        catch (final Exception e)
                        {
                            // Keep default
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.warn("⚠️ Pre-Step PESAP failed for step '{}' — falling back to defaults: {}", resolvedInstruction, e.getMessage());
                }
            }

            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, initialLevel);
            step.setContextLevel(initialLevel.name());
            stats.getContextLevels().add(initialLevel.name());

            final int maxRetriesAtMaxLevel = org.neodymium.ai.config.AiConfiguration.getInstance().getInt("neodymium.ai.maxRetriesAtMaxLevel", 1);
            final int ladderDistance = org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_RICH.ordinal() - initialLevel.ordinal() + 1;
            final int totalStepBudget = Math.max(1, ladderDistance) + Math.max(0, maxRetriesAtMaxLevel);

            contextState.getTransientData().put("KEY_STEP_TOTAL_BUDGET", totalStepBudget);
            contextState.getTransientData().put("KEY_STEP_ATTEMPTS_USED", 1);

            @SuppressWarnings("unchecked")
            final AiPrompt<List<Action>> activePrompt = (AiPrompt<List<Action>>) contextState.getTransientData()
                .get(ExecutionContext.KEY_ACTIVE_PROMPT);

            if (activePrompt == null)
            {
                throw new ConclusiveFailureException("No active prompt template registered in ExecutionContext transient data");
            }

            final org.neodymium.ai.config.ExecutionMode mode = (org.neodymium.ai.config.ExecutionMode) contextState.getTransientData()
                .computeIfAbsent(ExecutionContext.KEY_EXECUTION_MODE, k -> org.neodymium.ai.config.AiConfiguration.getInstance().getExecutionMode());

            // Check if we are in replay mode and have a recorded dHash for this step
            if (mode.isReplay() && !stepNoReplay && step.getScreenshotHash() != null && !step.getScreenshotHash().isEmpty())
            {
                final TargetExecutor executor = (TargetExecutor) contextState.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                if (executor != null)
                {
                    try
                    {
                        final SutState currentState = executor.captureState(org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_LEAN);
                        contextState.getTransientData().put(ExecutionContext.KEY_LAST_STATE, currentState);

                        String currentSsimMatrix = null;
                        if (currentState != null && currentState.getAttachments() != null)
                        {
                            for (final SutAttachment attachment : currentState.getAttachments())
                            {
                                if (attachment.mediaType().startsWith("image/") && attachment.base64Data() != null)
                                {
                                    currentSsimMatrix = org.neodymium.ai.util.ScreenshotHasher.computeSsimMatrix(attachment.base64Data());
                                    break;
                                }
                            }
                        }

                        if (step.getScreenshotHash() != null)
                        {
                            boolean isVisualMatch = false;
                            final String recordedHash = step.getScreenshotHash();
                            if (currentSsimMatrix != null)
                            {
                                final double ssimScore = org.neodymium.ai.util.ScreenshotHasher.calculateSsim(recordedHash, currentSsimMatrix);
                                final double minScore = org.neodymium.ai.config.AiConfiguration.getInstance().getDouble("neodymium.ai.ssim.minScore", 0.99);
                                LOGGER.debug("   🖼️ [Visual SSIM Check] Instruction: \"{}\" | SSIM Score: {} | Required Min Score: {}",
                                    resolvedInstruction, String.format("%.4f", ssimScore), minScore);

                                if (ssimScore >= minScore)
                                {
                                    isVisualMatch = true;
                                    LOGGER.info("   ✅ Visual SSIM match (score: {} >= {}) for instruction: \"{}\". Bypassing LLM call/actions.",
                                        String.format("%.4f", ssimScore), minScore, resolvedInstruction);
                                }
                                else
                                {
                                    LOGGER.debug("   ⚠️ Visual SSIM score below threshold ({} < {}) for instruction: \"{}\"",
                                        String.format("%.4f", ssimScore), minScore, resolvedInstruction);
                                }
                            }

                            final boolean hasActualActions = step.getActions() != null && !step.getActions().isEmpty()
                                && step.getActions().stream().anyMatch(a -> !"NONE".equalsIgnoreCase(a.getType()));

                            if (isVisualMatch && !hasActualActions)
                            {
                                return;
                            }
                            else
                            {
                                if (hasActualActions)
                                {
                                    if (isVisualMatch)
                                    {
                                        org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).info(
                                            "   Visual match for interactive instruction: \"{}\". Executing actions anyway to guarantee state.",
                                            resolvedInstruction);
                                    }
                                    else
                                    {
                                        org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).debug(
                                            "   Visual mismatch for interactive instruction: \"{}\". Proceeding to execute actions.",
                                            resolvedInstruction);
                                    }
                                }
                                else
                                {
                                    final String msg = String.format("Visual mismatch for instruction: \"%s\".", resolvedInstruction);
                                    org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).warn("   ❌ " + msg);

                                    if (mode.supportsHealing())
                                    {
                                        throw new HealingRequiredException(msg);
                                    }
                                    else
                                    {
                                        throw new DivergenceException(msg);
                                    }
                                }
                            }
                        }
                    }
                    catch (final PipelineException e)
                    {
                        throw e;
                    }
                    catch (final Exception e)
                    {
                        org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).warn("   ⚠️ Failed to capture state or compare visual dHash: {}", e.getMessage());
                    }
                }
            }

            // Assemble try block sequence: [CaptureStateStep] -> [CallLlmStep | Replay Actions] -> ExecuteActionsStep -> VerifyOutcomeStep
            final ExecuteActionsStep executeStep = new ExecuteActionsStep();
            final VerifyOutcomeStep verifyStep = new VerifyOutcomeStep();

            final List<PipelineStep> standardFlow = new ArrayList<>();

            final boolean isReplay = mode.isReplay() && !stepNoReplay && (mode == org.neodymium.ai.config.ExecutionMode.REPLAY_STRICT || (step.getActions() != null && (!step.getActions().isEmpty() || step.getScreenshotHash() != null)));

            if (isReplay)
            {
                // Replay mode: Stamp live DOM with data-ai attributes before executing step actions
                standardFlow.add(c -> {
                    if (mode == org.neodymium.ai.config.ExecutionMode.REPLAY_STRICT && step.getActions() == null)
                    {
                        throw new org.neodymium.ai.pipeline.ConclusiveFailureException(
                            "No recorded actions found for step '" + step.getInstruction() + "' in REPLAY_STRICT mode. Companion JSON recording file is missing or step was not recorded.");
                    }

                    if (step.getStatus() == org.neodymium.ai.model.PlaybookStepStatus.FAILED || step.isFailed())
                    {
                        final String reason = step.getFailureReason() != null && !step.getFailureReason().trim().isEmpty()
                            ? step.getFailureReason()
                            : "Recorded step execution failed.";
                        throw new org.neodymium.ai.pipeline.ConclusiveFailureException(reason);
                    }

                    final TargetExecutor executor = (TargetExecutor) c.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                    if (executor != null)
                    {
                        try
                        {
                            final SutState state = executor.captureState(org.neodymium.ai.executor.selenide.ContextLevel.STANDARD);
                            c.getTransientData().put(ExecutionContext.KEY_LAST_STATE, state);
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                    c.getTransientData().put(ExecutionContext.KEY_LAST_LLM_RESULT, step.getActions() != null ? step.getActions() : List.of());
                    final Integer replays = (Integer) c.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_REPLAYS, 0);
                    c.getTransientData().put(ExecutionContext.KEY_TOTAL_REPLAYS, replays + 1);
                });
            }
            else
            {
                // Live mode: Query LLM for actions
                final boolean verificationEnabled = org.neodymium.ai.config.AiConfiguration.getInstance().isSemanticVerificationEnabled();
                final org.neodymium.ai.executor.selenide.ContextLevel captureLevel;
                if (verificationEnabled && (initialLevel == org.neodymium.ai.executor.selenide.ContextLevel.MINIMAL || initialLevel == org.neodymium.ai.executor.selenide.ContextLevel.LEAN))
                {
                    captureLevel = org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_LEAN;
                }
                else
                {
                    captureLevel = initialLevel;
                }

                standardFlow.add(c -> {
                    final TargetExecutor executor = (TargetExecutor) c.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                    try
                    {
                        final SutState state = executor.captureState(captureLevel);
                        c.getTransientData().put(ExecutionContext.KEY_LAST_STATE, state);
                        if (state != null && state.getTextContent() != null)
                        {
                            final PlaybookStep currentStep = (PlaybookStep) c.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
                            if (currentStep != null)
                            {
                                currentStep.setBaselineState(new DefaultActionSanitizer().sanitizeText(state.getTextContent(), c.getSessionData()));
                            }
                        }
                    }
                    catch (final java.io.IOException e)
                    {
                        throw new ConclusiveFailureException("Failed to capture SUT state before execution", e);
                    }
                });

                final LlmCapability capability = (initialLevel != null && initialLevel.includesScreenshot()) ? LlmCapability.VISION : LlmCapability.TEXT_ONLY;
                final CallLlmStep<List<Action>> llmStep = new CallLlmStep<>(activePrompt, capability);
                standardFlow.add(llmStep);
                if (org.neodymium.ai.config.AiConfiguration.getInstance().isJudgeEnabled())
                {
                    standardFlow.add(new QualityJudgeStep());
                }
            }

            standardFlow.add(executeStep);
            standardFlow.add(verifyStep);

            final SequenceStep tryBlock = new SequenceStep(standardFlow);

            // Register exception handlers based on execution mode
            final Map<Class<? extends PipelineException>, PipelineStep> handlers = new HashMap<>();

            if (!step.isNoHealing() && (mode.isLive() || mode.supportsHealing()))
            {
                // Healing Escalation: PrepareRetryStep -> CallLlmStep -> ExecuteActionsStep -> VerifyOutcomeStep
                handlers.put(HealingRequiredException.class, c -> {
                    org.neodymium.ai.executor.selenide.ContextLevel activeLevel =
                        c.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL) instanceof org.neodymium.ai.executor.selenide.ContextLevel cl
                            ? cl
                            : null;

                    final boolean isFirstHealingAttempt = (activeLevel == null);
                    if (activeLevel == null && step.getContextLevel() != null)
                    {
                        try
                        {
                            activeLevel = org.neodymium.ai.executor.selenide.ContextLevel.valueOf(step.getContextLevel().toUpperCase().trim());
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                    if (activeLevel == null)
                    {
                        activeLevel = org.neodymium.ai.executor.selenide.ContextLevel.MINIMAL;
                    }

                    final org.neodymium.ai.executor.selenide.ContextLevel escalatedLevel = isFirstHealingAttempt ? activeLevel : activeLevel.escalate();

                    if (escalatedLevel == null)
                    {
                        final Object lastErrObj = c.getTransientData().get(ExecutionContext.KEY_LAST_EXECUTION_ERROR);
                        final String lastErr = lastErrObj != null ? String.valueOf(lastErrObj) : "Maximum context escalation level reached (" + activeLevel + ")";
                        throw new ConclusiveFailureException("Action execution failed after maximum context escalation (" + activeLevel + "): " + lastErr);
                    }

                    if (activeLevel == escalatedLevel || escalatedLevel == org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_RICH)
                    {
                        final Integer attemptsUsed = (Integer) c.getTransientData().getOrDefault("KEY_STEP_ATTEMPTS_USED", 0);
                        final Integer totalBudget = (Integer) c.getTransientData().getOrDefault("KEY_STEP_TOTAL_BUDGET", 8);
                        if (attemptsUsed >= totalBudget && activeLevel == org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_RICH)
                        {
                            org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).error(
                                "🛑 Circuit Breaker Tripped: Exceeded step execution budget ({}/{}) at highest context level ({}) for step. Aborting retry loop.",
                                attemptsUsed, totalBudget, activeLevel);
                            throw new ConclusiveFailureException(
                                "Maximum step execution budget (" + totalBudget + " attempts) exceeded for step. Aborting pipeline.");
                        }
                        c.getTransientData().put("KEY_STEP_ATTEMPTS_USED", attemptsUsed + 1);
                    }

                    final TargetExecutor currentExecutor = (TargetExecutor) c.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                    if (!com.codeborne.selenide.WebDriverRunner.hasWebDriverStarted() && currentExecutor == null)
                    {
                        throw new ConclusiveFailureException("Browser/WebDriver has not started yet. Ensure the playbook starts with a NAVIGATE step or browser is initialized in setup.");
                    }

                    c.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, escalatedLevel);
                    org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).warn("⚠️ Context escalated on action execution failure to: {}", escalatedLevel);

                    final Object statsObj = c.getTransientData().get("KEY_CURRENT_STEP_STATS");
                    if (statsObj instanceof org.neodymium.ai.pipeline.StepStats stepStats)
                    {
                        stepStats.getContextLevels().add(escalatedLevel.name());
                    }

                    final LlmCapability capability = escalatedLevel.includesScreenshot() ? LlmCapability.VISION : LlmCapability.TEXT_ONLY;
                    final PrepareRetryStep prepareStep = new PrepareRetryStep();
                    final CallLlmStep<List<Action>> escalationLlmStep = new CallLlmStep<>(activePrompt, capability);
                    final List<PipelineStep> healFlow = new java.util.ArrayList<>();
                    healFlow.add(prepareStep);
                    if (escalatedLevel.includesScreenshot())
                    {
                        healFlow.add(new CaptureStateStep());
                    }
                    healFlow.add(escalationLlmStep);
                    healFlow.add(executeStep);
                    healFlow.add(verifyStep);

                    final TryCatchStep healTryCatch = new TryCatchStep(new SequenceStep(healFlow), handlers);
                    c.pushStep(healTryCatch);
                });

                handlers.put(org.neodymium.ai.pipeline.ToLevelEscalationException.class, c -> {
                    final Object errObj = c.getTransientData().get(ExecutionContext.KEY_LAST_EXECUTION_ERROR);
                    final org.neodymium.ai.pipeline.ToLevelEscalationException e = errObj instanceof org.neodymium.ai.pipeline.ToLevelEscalationException tle ? tle : null;
                    final String targetLevelStr = e != null ? e.getTargetLevel() : "VISUAL_RICH";
                    org.neodymium.ai.executor.selenide.ContextLevel targetLevel = org.neodymium.ai.executor.selenide.ContextLevel.MINIMAL;
                    try
                    {
                        targetLevel = org.neodymium.ai.executor.selenide.ContextLevel.valueOf(targetLevelStr.toUpperCase());
                        final Object curLevelObj = c.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL);
                        final org.neodymium.ai.executor.selenide.ContextLevel currentLevel = curLevelObj instanceof org.neodymium.ai.executor.selenide.ContextLevel cl ? cl : org.neodymium.ai.executor.selenide.ContextLevel.MINIMAL;

                        final Integer attemptsUsed = (Integer) c.getTransientData().getOrDefault("KEY_STEP_ATTEMPTS_USED", 0);
                        final Integer totalBudget = (Integer) c.getTransientData().getOrDefault("KEY_STEP_TOTAL_BUDGET", 8);

                        if (attemptsUsed >= totalBudget && (currentLevel == org.neodymium.ai.executor.selenide.ContextLevel.VISUAL_RICH || targetLevel == currentLevel))
                        {
                            org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).error(
                                "🛑 Circuit Breaker Tripped: Exceeded step execution budget ({}/{}) at context level ({}) for step. Aborting retry loop.",
                                attemptsUsed, totalBudget, targetLevel);
                            throw new ConclusiveFailureException(
                                "Maximum step execution budget (" + totalBudget + " attempts) exceeded for step. Aborting pipeline.");
                        }
                        c.getTransientData().put("KEY_STEP_ATTEMPTS_USED", attemptsUsed + 1);

                        c.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, targetLevel);
                        org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).warn("⚠️ Context escalated to: {}", targetLevel);

                        final Object statsObj = c.getTransientData().get("KEY_CURRENT_STEP_STATS");
                        if (statsObj instanceof org.neodymium.ai.pipeline.StepStats stepStats)
                        {
                            stepStats.getContextLevels().add(targetLevel.name());
                        }
                    }
                    catch (final ConclusiveFailureException cfe)
                    {
                        throw cfe;
                    }
                    catch (final Exception ex)
                    {
                        // Fallback or ignore invalid level
                    }
                    
                    final LlmCapability escalationCapability = (targetLevel != null && targetLevel.includesScreenshot()) ? LlmCapability.VISION : LlmCapability.TEXT_ONLY;
                    final CallLlmStep<List<Action>> escalationLlmStep = new CallLlmStep<>(activePrompt, escalationCapability);

                    final List<PipelineStep> escFlow = new java.util.ArrayList<>();
                    escFlow.add(new CaptureStateStep());
                    escFlow.add(escalationLlmStep);
                    escFlow.add(executeStep);
                    escFlow.add(verifyStep);

                    final TryCatchStep escTryCatch = new TryCatchStep(new SequenceStep(escFlow), handlers);
                    c.pushStep(escTryCatch);
                });
            }
            else if (mode.supportsHealing() && isReplay && !step.isOptional() && !step.isNoHealing())
            {
                // Replay Healing: PrepareRetryStep -> CaptureStateStep -> SemanticDivergenceAnalysisStep -> CallLlmStep -> ExecuteActionsStep -> VerifyOutcomeStep
                handlers.put(HealingRequiredException.class, c -> {
                    c.getTransientData().put(ExecutionContext.KEY_IS_HEALED_STEP, true);
                    final PrepareRetryStep prepareStep = new PrepareRetryStep();
                    final SemanticDivergenceAnalysisStep diffStep = new SemanticDivergenceAnalysisStep();
                    final CallLlmStep<List<Action>> healLlmStep = new CallLlmStep<>(activePrompt, LlmCapability.TEXT_ONLY);
                    
                    c.pushStep(verifyStep);
                    c.pushStep(executeStep);
                    c.pushStep(healLlmStep);
                    c.pushStep(diffStep);
                    c.pushStep(new CaptureStateStep());
                    c.pushStep(prepareStep);
                });
            }
            // If REPLAY_STRICT, no HealingRequiredException handler is registered; exception escapes to trigger Visual RCA

            final TryCatchStep tryCatch = new TryCatchStep(tryBlock, handlers);

            // Push end-hook step first, so it runs AFTER tryCatch executes
            contextState.pushStep(c -> {
                if (step.getSubSteps() != null && !step.getSubSteps().isEmpty())
                {
                    return;
                }
                final Boolean isHealed = (Boolean) c.getTransientData().get(ExecutionContext.KEY_IS_HEALED_STEP);
                if (Boolean.TRUE.equals(isHealed))
                {
                    step.setStatus(org.neodymium.ai.model.PlaybookStepStatus.HEALED);
                }
                else
                {
                    step.setStatus(org.neodymium.ai.model.PlaybookStepStatus.SUCCESS);
                }
                step.setFailed(false);
                step.setFailureReason(null);
                final Object statsObj = c.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof org.neodymium.ai.pipeline.StepStats stepStats)
                {
                    stepStats.setDurationMs(System.currentTimeMillis() - stepStats.getStartTime());
                    @SuppressWarnings("unchecked")
                    final List<Action> stepActions = (List<Action>) c.getTransientData().get(ExecutionContext.KEY_CURRENT_STEP_ACTIONS);
                    if (stepActions != null)
                    {
                        stepStats.getActions().addAll(stepActions);
                    }
                }

                if (step.isBug())
                {
                    final String bugComment = step.getBugDetails();
                    final String bugStr = bugComment != null ? " (" + bugComment + ")" : "";
                    final String msg = String.format("Expected bug%s but step succeeded: %s:%d (%s)",
                        bugStr, step.getSourceFile(), step.getLineNumber(), step.getInstruction());
                    org.slf4j.LoggerFactory.getLogger(ExecuteActionsStep.class).error("   ❌ {}", msg);

                    if (!step.isContinueOnError())
                    {
                        throw new org.neodymium.ai.pipeline.UnexpectedSuccessException(msg);
                    }
                    else
                    {
                        @SuppressWarnings("unchecked")
                        final List<String> warnings = (List<String>) c.getTransientData()
                            .computeIfAbsent("verificationWarnings", k -> new java.util.ArrayList<String>());
                        warnings.add(msg);
                    }
                }
            });

            contextState.pushStep(tryCatch);
        };
    }

    private static org.neodymium.ai.pipeline.StepStats getOrCreateStatsForStep(
        final PlaybookStep step,
        final long startTime,
        final boolean replayed,
        final Map<PlaybookStep, org.neodymium.ai.pipeline.StepStats> stepStatsMap,
        final List<org.neodymium.ai.pipeline.StepStats> allStats
    )
    {
        org.neodymium.ai.pipeline.StepStats stats = stepStatsMap.get(step);
        if (stats == null)
        {
            stats = new org.neodymium.ai.pipeline.StepStats(step.getInstruction(), startTime);
            stats.setReplayed(replayed);
            stepStatsMap.put(step, stats);

            final PlaybookStep parentStep = step.getParent();
            if (parentStep != null)
            {
                final org.neodymium.ai.pipeline.StepStats parentStats = getOrCreateStatsForStep(parentStep, startTime, replayed, stepStatsMap, allStats);
                parentStats.getSubStats().add(stats);
            }
            else
            {
                allStats.add(stats);
            }
        }
        return stats;
    }

    private Action resolveActionVariables(final Action rawAction, final org.neodymium.ai.model.SessionData data)
    {
        if (rawAction == null || data == null)
        {
            return rawAction;
        }

        // 1. Resolve values list
        final List<String> resolvedValues = new ArrayList<>();
        for (final String val : rawAction.getValues())
        {
            resolvedValues.add(val != null ? data.resolveVariables(val) : null);
        }

        // 2. Resolve target selector/URL
        final String resolvedTarget = rawAction.getTarget() != null ? data.resolveVariables(rawAction.getTarget()) : null;

        // 3. Resolve description
        final String resolvedDesc = rawAction.getDescription() != null ? data.resolveVariables(rawAction.getDescription()) : null;

        // Construct the new resolved action
        final Action resolvedAction = new Action(
            rawAction.getType(),
            resolvedTarget,
            resolvedValues,
            resolvedDesc,
            rawAction.getReasoning()
        );

        resolvedAction.setIsRegex(rawAction.isRegex());
        resolvedAction.setStepInstruction(rawAction.getStepInstruction());
        resolvedAction.setStepLine(rawAction.getStepLine());
        resolvedAction.setStepFile(rawAction.getStepFile());
        resolvedAction.setStepScreenshotHash(rawAction.getStepScreenshotHash());
        resolvedAction.setAdjust(rawAction.getAdjust());
        resolvedAction.setSelfCritique(rawAction.getSelfCritique());
        resolvedAction.setCandidateLocators(new ArrayList<>(rawAction.getCandidateLocators()));

        // Copy dynamic parameters map
        resolvedAction.getParameters().putAll(rawAction.getParameters());

        // Recursively resolve nested branch/conditional action lists
        if (rawAction.getCondition() != null)
        {
            final List<Action> resolvedCondition = new ArrayList<>();
            for (final Action condAct : rawAction.getCondition())
            {
                resolvedCondition.add(resolveActionVariables(condAct, data));
            }
            resolvedAction.setCondition(resolvedCondition);
        }
        
        if (rawAction.getThen() != null)
        {
            final List<Action> resolvedThen = new ArrayList<>();
            for (final Action thenAct : rawAction.getThen())
            {
                resolvedThen.add(resolveActionVariables(thenAct, data));
            }
            resolvedAction.setThen(resolvedThen);
        }
        
        if (rawAction.getElseActions() != null)
        {
            final List<Action> resolvedElse = new ArrayList<>();
            for (final Action elseAct : rawAction.getElseActions())
            {
                resolvedElse.add(resolveActionVariables(elseAct, data));
            }
            resolvedAction.setElseActions(resolvedElse);
        }

        return resolvedAction;
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
        return prepared.replaceAll("\\s+", " ").trim();
    }
}
