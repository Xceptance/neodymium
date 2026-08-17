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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionListener;
import org.neodymium.ai.event.InteractiveConsoleListener;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.executor.selenide.PageAnalyzer;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.executor.selenide.plugins.ClickAction;
import org.neodymium.ai.executor.selenide.plugins.ClickAction.CoordinateTarget;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.DivergenceException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.pipeline.ToLevelEscalationException;
import org.neodymium.ai.pipeline.UnexpectedSuccessException;
import org.neodymium.ai.pipeline.structural.SequenceStep;
import org.neodymium.ai.pipeline.structural.TryCatchStep;
import org.neodymium.ai.playbook.PlaybookParser;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.prompt.ActionExtractionPrompt;
import org.neodymium.ai.prompt.ActionSanitizer;
import org.neodymium.ai.prompt.AiPrompt;
import org.neodymium.ai.prompt.DefaultActionSanitizer;
import org.neodymium.ai.prompt.PesapPrompt;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.util.LocatorImprover;
import org.neodymium.ai.util.ScreenshotHasher;
import org.neodymium.ai.util.VisualStabilityDetector;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.ElementNotFound;

/**
 * Concrete pipeline step executing actions parsed from LLM responses, sanitizing/parameterizing
 * their inputs on the fly, storing execution history, and dispatching executed events.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecuteActionsStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteActionsStep.class);
    private static final Pattern TIMEOUT_PATTERN = Pattern.compile("(?i)\\(\\s*timeout\\s*:\\s*(\\d+)(ms|s)?\\s*\\)");

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

        final ExecutionMode execMode = (ExecutionMode) context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
        final PlaybookStep currentStep = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        final boolean isNoReplay = currentStep != null && currentStep.isNoReplay();
        final boolean isReplayingStep = execMode != null && execMode.isReplay() && !isNoReplay
            && (currentStep == null || execMode == ExecutionMode.REPLAY_STRICT || (currentStep.getActions() != null && (!currentStep.getActions().isEmpty() || currentStep.getScreenshotHash() != null)));

        if (!isReplayingStep && currentStep != null)
        {
            currentStep.getActions().clear();
        }

        // Check if InteractiveConsoleListener is attached to the session
        InteractiveConsoleListener interactiveListener = null;
        if (session.getEventBus() != null)
        {
            for (final ExecutionListener listener : session.getEventBus().getListeners())
            {
                if (listener instanceof InteractiveConsoleListener icl && icl.isInteractive())
                {
                    interactiveListener = icl;
                    break;
                }
            }
        }

        if (interactiveListener != null)
        {
            final String userAction = interactiveListener.pauseBeforeActionExecution(context, currentStep);
            if ("SKIP".equalsIgnoreCase(userAction))
            {
                if (currentStep != null)
                {
                    currentStep.setStatus(PlaybookStepStatus.SKIPPED);
                }
                return;
            }
            else if ("ABORT".equalsIgnoreCase(userAction) || "STOP".equalsIgnoreCase(userAction))
            {
                throw new ConclusiveFailureException("Interactive test execution aborted by user");
            }

            final boolean stepWasEdited = Boolean.TRUE.equals(context.getTransientData().remove("KEY_STEP_EDITED"))
                || "EDIT".equalsIgnoreCase(userAction)
                || "UPDATE_STEP".equalsIgnoreCase(userAction)
                || "SAVE_STEP".equalsIgnoreCase(userAction);

            if (stepWasEdited && currentStep != null)
            {
                LOGGER.info("[ExecuteActionsStep] Step instruction was edited during pause. Re-triggering LLM reasoning for: \"{}\"", currentStep.getInstruction());
                context.getTransientData().remove(ExecutionContext.KEY_LAST_LLM_RESULT);
                context.pushStep(mapPlaybookStepToPipelineStep(currentStep, session, context));
                return;
            }
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
                if (resolvedAction.getDomFeatureVector() != null)
                {
                    for (final String line : resolvedAction.getDomFeatureVector().toFormattedLines("      📐 Vector:      ", "                      "))
                    {
                        LOGGER.trace(line);
                    }
                }

                // Execute SUT action via targeted SUT driver
                // Mask any raw sensitive inputs dynamically matching SessionData variable keys
                Action sanitized = this.actionSanitizer.sanitize(action, context.getSessionData());
                ExecutionMode mode = (ExecutionMode) context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
                if (mode == null && session != null)
                {
                    mode = session.getExecutionMode();
                }
                final PlaybookStep step = (PlaybookStep) context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
                final boolean isNoReplay = step != null && step.isNoReplay();
                final boolean isReplayingStep = mode != null && mode.isReplay() && !isNoReplay && (step == null || mode == ExecutionMode.REPLAY_STRICT || (step.getActions() != null && (!step.getActions().isEmpty() || step.getScreenshotHash() != null)));

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
                    final Matcher m = TIMEOUT_PATTERN.matcher(rawInstruction);
                    if (m.find())
                    {
                        final long parsedVal = Long.parseLong(m.group(1));
                        final String unit = m.group(2);
                        customTimeoutMs = "s".equalsIgnoreCase(unit) ? parsedVal * 1000L : parsedVal;
                    }
                }

                final long origTimeout = Configuration.timeout;
                if (customTimeoutMs != null)
                {
                    Configuration.timeout = customTimeoutMs;
                }

                if (isReplayingStep && AiConfiguration.getInstance().isUseRecordedDelays())
                {
                    final Long recDelay = sanitized.getDelayMs();
                    if (recDelay != null && recDelay > 0)
                    {
                        final double scale = AiConfiguration.getInstance().getReplayDelayScale();
                        final long sleepTime = Math.max(0L, (long) (recDelay * scale));
                        if (sleepTime > 0)
                        {
                            try
                            {
                                Thread.sleep(sleepTime);
                            }
                            catch (final InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }

                final long actionStartTime = System.currentTimeMillis();
                if (!isReplayingStep)
                {
                    final Long lastActionEndTime = (Long) context.getTransientData().get("KEY_LAST_ACTION_END_TIME");
                    if (lastActionEndTime != null && sanitized.getDelayMs() == null)
                    {
                        sanitized.setDelayMs(Math.max(0L, actionStartTime - lastActionEndTime));
                    }
                }

                try
                {
                    if (executor instanceof SelenideTargetExecutor ste)
                    {
                        ste.setExecutionContext(context);
                    }

                    if (step != null && executor != null && !isReplayingStep)
                    {
                        step.setTargetFramework(executor.getFrameworkName());
                    }

                    Action actionToExecute = resolvedAction;
                    if (step != null && step.getDomFeatureVector() != null && actionToExecute.getDomFeatureVector() == null)
                    {
                        actionToExecute = actionToExecute.withDomFeatureVector(step.getDomFeatureVector());
                    }
                    if (!isReplayingStep
                        && executor != null
                        && WebDriverRunner.hasWebDriverStarted()
                        && resolvedAction.getTarget() != null
                        && !resolvedAction.getTarget().isBlank())
                    {
                        try
                        {
                            final WebDriver driver = WebDriverRunner.getWebDriver();
                            final SelenideElement found = SelenideElementFinder.findElement(resolvedAction);
                            if (found != null && found.toWebElement() != null)
                            {
                                final DomFeatureVector vector = new PageAnalyzer(driver).extractFeatureVector(found.toWebElement());
                                if (vector != null)
                                {
                                    actionToExecute = actionToExecute.withDomFeatureVector(vector);
                                    sanitized.setDomFeatureVector(vector);
                                    if (LOGGER.isTraceEnabled())
                                    {
                                        LOGGER.trace("   📐 Captured DomFeatureVector for target '{}':", resolvedAction.getTarget());
                                        for (final String line : vector.toFormattedLines("        │ ", "        │ "))
                                        {
                                            LOGGER.trace(line);
                                        }
                                    }
                                    if (step != null && step.getDomFeatureVector() == null)
                                    {
                                        step.setDomFeatureVector(vector);
                                    }
                                }

                                if (executor.supportsLocatorImprovement()
                                    && AiConfiguration.getInstance().isLocatorImproverEnabled())
                                {
                                    final String improvedLocator = LocatorImprover.improveLocator(driver, found.toWebElement(), resolvedAction.getTarget());
                                    if (improvedLocator != null && !improvedLocator.equals(resolvedAction.getTarget()))
                                    {
                                        actionToExecute = actionToExecute.withTarget(improvedLocator);
                                        final Action upgraded = sanitized.withTarget(improvedLocator);
                                        if (vector != null)
                                        {
                                            upgraded.setDomFeatureVector(vector);
                                        }
                                        if (step != null && step.getActions() != null && !step.getActions().isEmpty())
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
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }

                    final CoordinateTarget coordinateTarget = ClickAction.parseCoordinateTarget(resolvedAction.getTarget());
                    if (coordinateTarget != null && !isReplayingStep && step != null && executor != null)
                    {
                        try
                        {
                            final SutState preState = executor.captureState(ContextLevel.VISUAL_LEAN, false);
                            if (preState != null && preState.getAttachments() != null)
                            {
                                for (final SutAttachment attachment : preState.getAttachments())
                                {
                                    if (attachment.mediaType().startsWith("image/") && attachment.base64Data() != null)
                                    {
                                        final String tileHash = ScreenshotHasher.computeTileSsimMatrix(attachment.base64Data(), coordinateTarget.x(), coordinateTarget.y(), 32);
                                        if (tileHash != null)
                                        {
                                            step.setScreenshotHash(tileHash);
                                            sanitized.setStepScreenshotHash(tileHash);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        catch (final Exception e)
                        {
                            LOGGER.debug("Failed to capture pre-action coordinate tile visual baseline: {}", e.getMessage());
                        }
                    }

                    context.getTransientData().put("currentAction", actionToExecute);
                    executor.execute(actionToExecute);

                    final long actionDuration = System.currentTimeMillis() - actionStartTime;
                    if (!isReplayingStep)
                    {
                        sanitized.setDurationMs(actionDuration);
                        context.getTransientData().put("KEY_LAST_ACTION_END_TIME", System.currentTimeMillis());
                    }
                }
                finally
                {
                    Configuration.timeout = origTimeout;
                    context.getTransientData().remove("currentAction");
                }
                
                final long settleMs = AiConfiguration.getInstance()
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
                        final ContextLevel cl = (step != null && step.isVisualStep())
                            ? ContextLevel.VISUAL
                            : ContextLevel.VISUAL_LEAN;
                        final boolean isFullPageReq = Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT"));
                        final SutState postActionState = executor.captureState(cl, isFullPageReq);
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
                    final ContextLevel currentLevel =
                        context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL) instanceof ContextLevel cl
                            ? cl
                            : ContextLevel.LEAN;

                    final LlmCapability capability = currentLevel.includesScreenshot() ? LlmCapability.VISION : LlmCapability.TEXT_ONLY;
                    final CallLlmStep<List<Action>> continuationLlmStep = new CallLlmStep<>(activePrompt, capability);

                    context.pushStep(new VerifyOutcomeStep());
                    context.pushStep(this);
                    if (AiConfiguration.getInstance().isJudgeEnabled())
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
            step.setStatus(PlaybookStepStatus.RUNNING);
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

            @SuppressWarnings("unchecked")
            final Set<PlaybookStep> alreadySplitSteps = (Set<PlaybookStep>) contextState.getTransientData()
                .computeIfAbsent("pesap.alreadySplitSteps", k -> new HashSet<>());

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

            final String lower = resolvedInstruction.toLowerCase();
            final boolean hasVisualFull = PlaybookStep.VISUAL_FULL_PATTERN.matcher(resolvedInstruction).find();
            final boolean hasLayout = PlaybookStep.LAYOUT_PATTERN.matcher(resolvedInstruction).find();
            final boolean hasVisual = PlaybookStep.VISUAL_PATTERN.matcher(resolvedInstruction).find();
            final boolean hasHint = PlaybookStep.HINT_PATTERN.matcher(resolvedInstruction).find();

            final boolean isFullPageTag = hasVisualFull || hasLayout;
            contextState.getTransientData().put("KEY_IS_FULL_PAGE_SCREENSHOT", isFullPageTag);

            if (hasVisualFull)
            {
                initialLevel = ContextLevel.VISUAL;
            }
            else if (hasVisual)
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
                int stepIndex = 0;
                if (step.getParent() != null && flatSteps != null)
                {
                    stepIndex = flatSteps.indexOf(step.getParent());
                }
                else if (flatSteps != null)
                {
                    stepIndex = flatSteps.indexOf(step);
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
            else if (flatSteps != null && flatSteps.contains(step))
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
            contextState.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, initialLevel);
            step.setContextLevel(initialLevel.name());

            final PesapPreStep pesapPreStep = new PesapPreStep(step, session);
            final boolean isSplit = pesapPreStep.executePreStep(contextState);
            if (isSplit)
            {
                return;
            }

            final ContextLevel effectiveLevel = (ContextLevel) contextState.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL);
            stats.addContextLevel(effectiveLevel != null ? effectiveLevel.name() : initialLevel.name());

            final int maxRetriesAtMaxLevel = AiConfiguration.getInstance().getInt("neodymium.ai.maxRetriesAtMaxLevel", 1);
            final int ladderDistance = ContextLevel.VISUAL_RICH.ordinal() - (effectiveLevel != null ? effectiveLevel.ordinal() : initialLevel.ordinal()) + 1;
            final int totalStepBudget = Math.max(1, ladderDistance) + Math.max(0, maxRetriesAtMaxLevel);

            contextState.getTransientData().put("KEY_STEP_TOTAL_BUDGET", totalStepBudget);
            contextState.getTransientData().put("KEY_STEP_ATTEMPTS_USED", 1);

            @SuppressWarnings("unchecked")
            final AiPrompt<List<Action>> candidatePrompt = (AiPrompt<List<Action>>) contextState.getTransientData()
                .get(ExecutionContext.KEY_ACTIVE_PROMPT);
            final AiPrompt<List<Action>> activePrompt = candidatePrompt != null ? candidatePrompt : new ActionExtractionPrompt();
            if (candidatePrompt == null)
            {
                contextState.getTransientData().put(ExecutionContext.KEY_ACTIVE_PROMPT, activePrompt);
            }

            final ExecutionMode mode = (ExecutionMode) contextState.getTransientData()
                .computeIfAbsent(ExecutionContext.KEY_EXECUTION_MODE, k -> AiConfiguration.getInstance().getExecutionMode());

            final VisualBaselineGateStep visualBaselineGateStep = new VisualBaselineGateStep(step, session);
            final boolean isBypassed = visualBaselineGateStep.executeGate(contextState);
            if (isBypassed)
            {
                return;
            }

            // Assemble try block sequence: [CaptureStateStep] -> [CallLlmStep | Replay Actions] -> ExecuteActionsStep -> VerifyOutcomeStep
            final ExecuteActionsStep executeStep = new ExecuteActionsStep();
            final VerifyOutcomeStep verifyStep = new VerifyOutcomeStep();

            final List<PipelineStep> standardFlow = new ArrayList<>();

            final boolean isReplay = mode.isReplay() && !stepNoReplay && (mode == ExecutionMode.REPLAY_STRICT || (step.getActions() != null && (!step.getActions().isEmpty() || step.getScreenshotHash() != null)));

            if (isReplay)
            {
                // Replay mode: Stamp live DOM with data-ai attributes before executing step actions
                standardFlow.add(c -> {
                    final boolean isRecorded = step.getActions() != null;
                    final boolean isVisualOnly = step.getScreenshotHash() != null && !step.getScreenshotHash().isEmpty();
                    final boolean isComposite = step.getSubSteps() != null && !step.getSubSteps().isEmpty();
                    if (mode == ExecutionMode.REPLAY_STRICT && !isRecorded && !isVisualOnly && !isComposite)
                    {
                        final String resolvedStrictStep = c.getSessionData() != null
                            ? c.getSessionData().resolveVariables(step.getInstruction())
                            : step.getInstruction();
                        throw new ConclusiveFailureException(
                            "No recorded actions found for step '" + resolvedStrictStep + "' in REPLAY_STRICT mode. Companion JSON recording file is missing or step was not recorded.");
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
                    c.getTransientData().put(ExecutionContext.KEY_LAST_LLM_RESULT, step.getActions() != null ? step.getActions() : List.of());
                    final Integer replays = (Integer) c.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_REPLAYS, 0);
                    c.getTransientData().put(ExecutionContext.KEY_TOTAL_REPLAYS, replays + 1);
                });
            }
            else
            {
                // Live mode: Query LLM for actions
                final boolean verificationEnabled = AiConfiguration.getInstance().isSemanticVerificationEnabled();
                final ContextLevel captureLevel;
                if (verificationEnabled && (initialLevel == ContextLevel.MINIMAL || initialLevel == ContextLevel.LEAN))
                {
                    captureLevel = ContextLevel.VISUAL_LEAN;
                }
                else
                {
                    captureLevel = initialLevel;
                }

                standardFlow.add(c -> {
                    final TargetExecutor executor = (TargetExecutor) c.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                    try
                    {
                        final boolean isFullPageReq = Boolean.TRUE.equals(c.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT"));
                        final SutState state = executor.captureState(captureLevel, isFullPageReq);
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
                    catch (final IOException e)
                    {
                        throw new ConclusiveFailureException("Failed to capture SUT state before execution", e);
                    }
                });

                final LlmCapability capability = (initialLevel != null && initialLevel.includesScreenshot()) ? LlmCapability.VISION : LlmCapability.TEXT_ONLY;
                final CallLlmStep<List<Action>> llmStep = new CallLlmStep<>(activePrompt, capability);
                standardFlow.add(llmStep);
                if (AiConfiguration.getInstance().isJudgeEnabled())
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
                    ContextLevel activeLevel =
                        c.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL) instanceof ContextLevel cl
                            ? cl
                            : null;

                    final boolean isFirstHealingAttempt = (activeLevel == null);
                    if (activeLevel == null && step.getContextLevel() != null)
                    {
                        try
                        {
                            activeLevel = ContextLevel.valueOf(step.getContextLevel().toUpperCase().trim());
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                    if (activeLevel == null)
                    {
                        activeLevel = ContextLevel.MINIMAL;
                    }

                    final ContextLevel escalatedLevel = isFirstHealingAttempt ? activeLevel : activeLevel.escalate();

                    if (escalatedLevel == null)
                    {
                        final Object lastErrObj = c.getTransientData().get(ExecutionContext.KEY_LAST_EXECUTION_ERROR);
                        final String lastErr = lastErrObj != null ? String.valueOf(lastErrObj) : "Maximum context escalation level reached (" + activeLevel + ")";
                        throw new ConclusiveFailureException("Action execution failed after maximum context escalation (" + activeLevel + "): " + lastErr);
                    }

                    if (activeLevel == escalatedLevel || escalatedLevel == ContextLevel.VISUAL_RICH)
                    {
                        final Integer attemptsUsed = (Integer) c.getTransientData().getOrDefault("KEY_STEP_ATTEMPTS_USED", 0);
                        final Integer totalBudget = (Integer) c.getTransientData().getOrDefault("KEY_STEP_TOTAL_BUDGET", 8);
                        if (attemptsUsed >= totalBudget && activeLevel == ContextLevel.VISUAL_RICH)
                        {
                            LOGGER.error(
                                "🛑 Circuit Breaker Tripped: Exceeded step execution budget ({}/{}) at highest context level ({}) for step. Aborting retry loop.",
                                attemptsUsed, totalBudget, activeLevel);
                            throw new ConclusiveFailureException(
                                "Maximum step execution budget (" + totalBudget + " attempts) exceeded for step. Aborting pipeline.");
                        }
                        c.getTransientData().put("KEY_STEP_ATTEMPTS_USED", attemptsUsed + 1);
                    }

                    final TargetExecutor currentExecutor = (TargetExecutor) c.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
                    if (!WebDriverRunner.hasWebDriverStarted() && currentExecutor == null)
                    {
                        throw new ConclusiveFailureException("Browser/WebDriver has not started yet. Ensure the playbook starts with a NAVIGATE step or browser is initialized in setup.");
                    }

                    c.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, escalatedLevel);
                    step.setContextLevel(escalatedLevel.name());
                    LOGGER.warn("⚠️ Context escalated on action execution failure to: {}", escalatedLevel);

                    final Object statsObj = c.getTransientData().get("KEY_CURRENT_STEP_STATS");
                    if (statsObj instanceof StepStats stepStats)
                    {
                        stepStats.addContextLevel(escalatedLevel.name());
                    }

                    final LlmCapability capability = escalatedLevel.includesScreenshot() ? LlmCapability.VISION : LlmCapability.TEXT_ONLY;
                    final PrepareRetryStep prepareStep = new PrepareRetryStep();
                    final CallLlmStep<List<Action>> escalationLlmStep = new CallLlmStep<>(activePrompt, capability);
                    final List<PipelineStep> healFlow = new ArrayList<>();
                    healFlow.add(prepareStep);
                    healFlow.add(new CaptureStateStep());
                    healFlow.add(escalationLlmStep);
                    healFlow.add(executeStep);
                    healFlow.add(verifyStep);

                    final TryCatchStep healTryCatch = new TryCatchStep(new SequenceStep(healFlow), handlers);
                    c.pushStep(healTryCatch);
                });

                handlers.put(ToLevelEscalationException.class, c -> {
                    final Object errObj = c.getTransientData().get(ExecutionContext.KEY_LAST_EXECUTION_ERROR);
                    final ToLevelEscalationException e = errObj instanceof ToLevelEscalationException tle ? tle : null;
                    final String targetLevelStr = e != null ? e.getTargetLevel() : "VISUAL_RICH";
                    ContextLevel targetLevel = ContextLevel.MINIMAL;
                    try
                    {
                        targetLevel = ContextLevel.valueOf(targetLevelStr.toUpperCase());
                        final Object curLevelObj = c.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL);
                        final ContextLevel currentLevel = curLevelObj instanceof ContextLevel cl ? cl : ContextLevel.MINIMAL;

                        final Integer attemptsUsed = (Integer) c.getTransientData().getOrDefault("KEY_STEP_ATTEMPTS_USED", 0);
                        final Integer totalBudget = (Integer) c.getTransientData().getOrDefault("KEY_STEP_TOTAL_BUDGET", 8);

                        if (attemptsUsed >= totalBudget && (currentLevel == ContextLevel.VISUAL_RICH || targetLevel == currentLevel))
                        {
                            LOGGER.error(
                                "🛑 Circuit Breaker Tripped: Exceeded step execution budget ({}/{}) at context level ({}) for step. Aborting retry loop.",
                                attemptsUsed, totalBudget, targetLevel);
                            throw new ConclusiveFailureException(
                                "Maximum step execution budget (" + totalBudget + " attempts) exceeded for step. Aborting pipeline.");
                        }
                        c.getTransientData().put("KEY_STEP_ATTEMPTS_USED", attemptsUsed + 1);

                        c.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, targetLevel);
                        step.setContextLevel(targetLevel.name());
                        LOGGER.warn("⚠️ Context escalated to: {}", targetLevel);

                        final Object statsObj = c.getTransientData().get("KEY_CURRENT_STEP_STATS");
                        if (statsObj instanceof StepStats stepStats)
                        {
                            stepStats.addContextLevel(targetLevel.name());
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

                    final List<PipelineStep> escFlow = new ArrayList<>();
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
                    step.setStatus(PlaybookStepStatus.HEALED);
                }
                else
                {
                    step.setStatus(PlaybookStepStatus.SUCCESS);
                }
                step.setFailed(false);
                step.setFailureReason(null);
                final Object statsObj = c.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof StepStats stepStats)
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
                    final String resolvedBugInstruction = c.getSessionData() != null
                        ? c.getSessionData().resolveVariables(step.getInstruction())
                        : step.getInstruction();
                    final String msg = String.format("Expected bug%s but step succeeded: %s:%d (%s)",
                        bugStr, step.getSourceFile(), step.getLineNumber(), resolvedBugInstruction);
                    LOGGER.error("   ❌ {}", msg);

                    if (!step.isContinueOnError())
                    {
                        throw new UnexpectedSuccessException(msg);
                    }
                    else
                    {
                        @SuppressWarnings("unchecked")
                        final List<String> warnings = (List<String>) c.getTransientData()
                            .computeIfAbsent("verificationWarnings", k -> new ArrayList<String>());
                        warnings.add(msg);
                    }
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
        return stats;
    }

    private Action resolveActionVariables(final Action rawAction, final SessionData data)
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
        final List<LocatorCandidate> resolvedCandidates = new ArrayList<>();
        if (rawAction.getCandidateLocators() != null)
        {
            for (final LocatorCandidate candidate : rawAction.getCandidateLocators())
            {
                if (candidate != null)
                {
                    final String rawLoc = candidate.getLocator();
                    final String resolvedCandidateTarget = rawLoc != null ? data.resolveVariables(rawLoc) : "";
                    resolvedCandidates.add(new LocatorCandidate(resolvedCandidateTarget, candidate.getStrategy(), candidate.getScore(), candidate.getReasoning()));
                }
            }
        }
        resolvedAction.setCandidateLocators(resolvedCandidates);
        resolvedAction.setDomFeatureVector(rawAction.getDomFeatureVector());
        resolvedAction.setDurationMs(rawAction.getDurationMs());
        resolvedAction.setDelayMs(rawAction.getDelayMs());
        resolvedAction.setHasElse(rawAction.getHasElse());

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
        prepared = prepared.replaceAll("(?i)\\s*\\(\\s*visual(?:\\s*:\\s*full)?\\s*\\)\\s*", " ");
        return prepared.replaceAll("\\s+", " ").trim();
    }
}
