/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package org.neodymium.ai.event;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.event.diagnostic.DiagnosticErrorEvent;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.PlaybookPipelineStep;
import org.neodymium.ai.report.DiskReportFormat;
import org.neodymium.ai.report.PreliminaryReportListener;
import org.neodymium.ai.report.TestExecutionReport;
import org.neodymium.ai.session.AiSession;
import org.neodymium.util.Neodymium;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.ai.console.InteractiveStateBuilder;

/**
 * Event listener that consumes {@link ExecutionEvent} dispatches from the {@link ExecutionEventBus},
 * converts them into JSON state snapshots pushed to the {@link InteractiveConsoleEngine}, and handles
 * interactive step-by-step pauses and user command handshakes.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class InteractiveConsoleListener implements ExecutionListener
{
    private static final Logger LOG = LoggerFactory.getLogger(InteractiveConsoleListener.class);

    private final InteractiveConsoleEngine consoleEngine;
    private final AiSession session;
    private final boolean interactive;
    private final PreliminaryReportListener reportListener;
    private volatile boolean autoRun;

    /**
     * Constructs an InteractiveConsoleListener.
     *
     * @param consoleEngine the interactive console engine backing HTTP/SSE dispatches
     * @param session the active AI session
     * @param interactive true if step-by-step pausing is enabled; false for passive SSE streaming
     */
    public InteractiveConsoleListener(
        final InteractiveConsoleEngine consoleEngine,
        final AiSession session,
        final boolean interactive
    )
    {
        this.consoleEngine = consoleEngine;
        this.session = session;
        this.interactive = interactive;
        this.autoRun = false;
        final AiConfiguration config = AiConfiguration.getInstance();
        this.reportListener = new PreliminaryReportListener(
            Paths.get(config.getDiskReportDirectory()),
            DiskReportFormat.parseFormats(config.getDiskReportFormat()),
            config.isDiskReportEnabled()
        );
    }

    /**
     * Returns the underlying PreliminaryReportListener instance.
     *
     * @return the report listener
     */
    public PreliminaryReportListener getReportListener()
    {
        return this.reportListener;
    }

    /**
     * Returns the active TestExecutionReport aggregated by this listener.
     *
     * @return test execution report instance
     */
    public TestExecutionReport getReport()
    {
        return this.reportListener != null ? this.reportListener.getReport() : null;
    }

    /**
     * Returns whether interactive step pausing is active.
     *
     * @return true if interactive step pausing is enabled
     */
    public boolean isInteractive()
    {
        return this.interactive;
    }

    /**
     * Returns whether continuous auto-run mode is currently set.
     *
     * @return true if running continuously without pausing
     */
    public boolean isAutoRun()
    {
        return this.autoRun;
    }

    /**
     * Sets auto-run state.
     *
     * @param autoRun true to skip step pauses
     */
    public void setAutoRun(final boolean autoRun)
    {
        this.autoRun = autoRun;
    }

    @Override
    public void onEvent(final ExecutionEvent event)
    {
        if (event == null)
        {
            return;
        }

        final ExecutionContext context = this.session != null ? this.session.getExecutionContext() : ExecutionContext.getActiveContext();

        if (this.reportListener != null)
        {
            this.reportListener.onEvent(event);
            if (context != null)
            {
                context.getTransientData().put("testExecutionReport", this.reportListener.getReport());
            }
        }

        if (this.consoleEngine == null)
        {
            return;
        }

        if (event instanceof StepStartedEvent stepStarted)
        {
            final int stepIndex = stepStarted.getStepIndex();
            final PlaybookStep currentStep = stepStarted.getStep();
            if (currentStep != null)
            {
                currentStep.setStatus(PlaybookStepStatus.RUNNING);
                if (currentStep.getParent() != null)
                {
                    final PlaybookStep parent = currentStep.getParent();
                    final int subIdx = parent.getSubSteps().indexOf(currentStep) + 1;
                    LOG.info("[InteractiveConsoleListener] Executing sub-step #{}/{} (\"{}\") of parent \"{}\"",
                        subIdx, parent.getSubSteps().size(), currentStep.getInstruction(), parent.getInstruction());
                }
                else
                {
                    LOG.info("[InteractiveConsoleListener] Executing step #{} (\"{}\")", stepIndex + 1, currentStep.getInstruction());
                }
            }

            if (context != null)
            {
                try
                {
                    if (WebDriverRunner.hasWebDriverStarted())
                    {
                        final String base64 = Selenide.screenshot(OutputType.BASE64);
                        if (base64 != null && !base64.isEmpty())
                        {
                            context.getTransientData().put("currentScreenshot", "data:image/png;base64," + base64);
                        }
                    }
                }
                catch (final Throwable ignored)
                {
                }
            }

            final String stateJson = InteractiveStateBuilder.buildStateJson(
                this.session, context, this.consoleEngine.getRunId(), stepIndex, "running", null, getReport());

            this.consoleEngine.pushState(stateJson);
        }
        else if (event instanceof StepFinishedEvent stepFinished)
        {
            if (stepFinished.getStep() != null)
            {
                final PlaybookStep finishedStep = stepFinished.getStep();
                if (finishedStep.getParent() != null)
                {
                    final PlaybookStep parent = finishedStep.getParent();
                    final int subIdx = parent.getSubSteps().indexOf(finishedStep) + 1;
                    LOG.info("[InteractiveConsoleListener] Finished sub-step #{}/{} (\"{}\") with status {}",
                        subIdx, parent.getSubSteps().size(), finishedStep.getInstruction(), stepFinished.getStatus());
                }
            }
            final int stepIndex = resolveStepIndex(context, stepFinished.getStep());
            final String stateJson = InteractiveStateBuilder.buildStateJson(
                this.session, context, this.consoleEngine.getRunId(), stepIndex, "running", null, getReport());

            this.consoleEngine.pushState(stateJson);
        }
        else if (event instanceof ActionExecutedEvent)
        {
            final Object stepObj = context != null ? context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP) : null;
            final PlaybookStep currentStep = stepObj instanceof PlaybookStep ps ? ps : null;
            final int stepIndex = resolveStepIndex(context, currentStep);
            final String stateJson = InteractiveStateBuilder.buildStateJson(
                this.session, context, this.consoleEngine.getRunId(), stepIndex, "running", null, getReport());

            this.consoleEngine.pushState(stateJson);
        }
        else if (event instanceof SessionFinishedEvent sessionFinished)
        {
            final TestExecutionReport rep = getReport();
            final boolean hasHealed = (rep != null && rep.getMetrics() != null && rep.getMetrics().getHealedSteps() > 0)
                || (this.session != null && this.session.getExecutionContext() != null
                    && Boolean.TRUE.equals(this.session.getExecutionContext().getTransientData().get(ExecutionContext.KEY_IS_HEALED_STEP)));
            final String overallStatus = !sessionFinished.isSuccess() ? "failed" : (hasHealed ? "healed" : "passed");

            if (this.interactive)
            {
                final String pauseId = "pause-final-" + UUID.randomUUID().toString();
                this.consoleEngine.registerPauseId(pauseId);

                final String stateJson = InteractiveStateBuilder.buildStateJson(
                    this.session, context, this.consoleEngine.getRunId(), 0, overallStatus, pauseId, getReport());

                this.consoleEngine.pushState(stateJson);

                LOG.info("[InteractiveConsoleListener] Session finished with status '{}'. Awaiting user final action (pauseId={})", overallStatus, pauseId);
                final long pauseStart = System.currentTimeMillis();
                try
                {
                    final JsonObject userAction = this.consoleEngine.waitForAction(pauseId);
                    handleUserAction(userAction, null, context);
                }
                catch (final InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    final long pauseDurationMs = System.currentTimeMillis() - pauseStart;
                    if (context != null)
                    {
                        context.recordInteractivePause(pauseDurationMs);
                    }
                }
            }
            else
            {
                final String stateJson = InteractiveStateBuilder.buildStateJson(
                    this.session, context, this.consoleEngine.getRunId(), 0, overallStatus, null, getReport());

                this.consoleEngine.pushState(stateJson);
            }

            if (AiConfiguration.getInstance().isDiskReportEnabled() && this.reportListener != null)
            {
                this.reportListener.flushReport();
            }
        }
        else if (event instanceof DiagnosticErrorEvent errorEvent)
        {
            LOG.warn("[InteractiveConsoleListener] Execution diagnostic error reported: {}", errorEvent.getMessage());
        }
    }

    /**
     * Pauses the interactive console on a step failure, allowing the user to
     * trigger AI Healing, retry/re-execute, skip, or finish/accept the test.
     *
     * @param context active execution context
     * @param step playbook step that failed
     * @param error failure cause
     * @return the user action string ("HEAL", "RUN", "SKIP", "FINISH", "ABORT", etc.)
     */
    public String pauseOnStepFailure(final ExecutionContext context, final PlaybookStep step, final Throwable error)
    {
        this.autoRun = false;
        if (!this.interactive || this.consoleEngine == null)
        {
            return "ABORT";
        }

        final int stepIndex = resolveStepIndex(context, step);

        while (true)
        {
            final String pauseId = UUID.randomUUID().toString();
            this.consoleEngine.registerPauseId(pauseId);

            final String stateJson = InteractiveStateBuilder.buildStateJson(
                this.session, context, this.consoleEngine.getRunId(), stepIndex, "paused", pauseId, getReport());

            this.consoleEngine.pushState(stateJson);

            if (step != null && step.getParent() != null)
            {
                final PlaybookStep parent = step.getParent();
                final String parentInst = parent.getInstruction();
                final String incFile = step.getSourceFile();
                final int subIdx = parent.getSubSteps().indexOf(step) + 1;
                final String parentContext = (incFile != null && !incFile.isBlank())
                    ? String.format("parent include step \"%s\" (includeFile=%s)", parentInst, incFile)
                    : String.format("parent step \"%s\"", parentInst);
                LOG.warn("[InteractiveConsoleListener] Sub-step failure encountered at sub-step #{}/{} (\"{}\") inside {}. Sub-steps cannot be skipped individually. Skip the parent to skip this step.",
                    subIdx, parent.getSubSteps().size(), step.getInstruction(), parentContext);
            }
            else
            {
                LOG.info("[InteractiveConsoleListener] Step failure encountered at step {}. Pausing for user recovery action (pauseId={})", stepIndex, pauseId);
            }
            final long pauseStart = System.currentTimeMillis();
            final JsonObject userAction;
            try
            {
                userAction = this.consoleEngine.waitForAction(pauseId);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return "ABORT";
            }
            finally
            {
                final long pauseDurationMs = System.currentTimeMillis() - pauseStart;
                if (context != null)
                {
                    context.recordInteractivePause(pauseDurationMs);
                }
            }

            final String action = userAction != null && userAction.has("action") ? userAction.get("action").getAsString().toUpperCase() : "ABORT";
            final boolean shouldKeepPausing = handleUserAction(userAction, step, context);
            if (!shouldKeepPausing)
            {
                if ("HEAL".equalsIgnoreCase(action) || "RETRY".equalsIgnoreCase(action) || "RUN".equalsIgnoreCase(action))
                {
                    if (step != null)
                    {
                        step.setStatus(PlaybookStepStatus.RUNNING);
                    }
                    pushCurrentState(context, step);
                }
                return action;
            }
        }
    }

    /**
     * Pauses test execution for user review and approval of planned actions and AI reasoning.
     *
     * @param context the current execution context
     * @param step the current playbook step
     * @return the action string requested by the user ("RUN", "SKIP", "ABORT", etc.)
     */
    public String pauseBeforeActionExecution(final ExecutionContext context, final PlaybookStep step)
    {
        if (!this.interactive || this.autoRun || this.consoleEngine == null)
        {
            return "RUN";
        }

        boolean isInitialPause = true;

        while (true)
        {
            final int stepIndex = resolveStepIndex(context, step);

            if (context != null && isInitialPause)
            {
                try
                {
                    if (WebDriverRunner.hasWebDriverStarted())
                    {
                        final String base64 = Selenide.screenshot(OutputType.BASE64);
                        if (base64 != null && !base64.isEmpty())
                        {
                            context.getTransientData().put("currentScreenshot", "data:image/png;base64," + base64);
                        }
                    }
                }
                catch (final Throwable ignored)
                {
                }
            }
            isInitialPause = false;

            final String pauseId = UUID.randomUUID().toString();
            this.consoleEngine.registerPauseId(pauseId);

            final String stateJson = InteractiveStateBuilder.buildStateJson(
                this.session, context, this.consoleEngine.getRunId(), stepIndex, "paused", pauseId, getReport());

            this.consoleEngine.pushState(stateJson);

            if (step != null && step.getParent() != null)
            {
                final PlaybookStep parent = step.getParent();
                final String parentInst = parent.getInstruction();
                final String incFile = step.getSourceFile();
                final int subIdx = parent.getSubSteps().indexOf(step) + 1;
                final String parentContext = (incFile != null && !incFile.isBlank())
                    ? String.format("parent include \"%s\" (includeFile=%s)", parentInst, incFile)
                    : String.format("parent step \"%s\"", parentInst);
                LOG.info("[InteractiveConsoleListener] Pausing for user action review at sub-step #{}/{} (\"{}\") of {} (pauseId={})",
                    subIdx, parent.getSubSteps().size(), step.getInstruction(), parentContext, pauseId);
            }
            else
            {
                LOG.info("[InteractiveConsoleListener] Pausing for user action review at step {} (pauseId={})", stepIndex, pauseId);
            }
            final long pauseStart = System.currentTimeMillis();
            final JsonObject userAction;
            try
            {
                userAction = this.consoleEngine.waitForAction(pauseId);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return "ABORT";
            }
            finally
            {
                final long pauseDurationMs = System.currentTimeMillis() - pauseStart;
                if (context != null)
                {
                    context.recordInteractivePause(pauseDurationMs);
                }
            }

            final String action = userAction != null && userAction.has("action") ? userAction.get("action").getAsString().toUpperCase() : "RUN";
            final boolean shouldKeepPausing = handleUserAction(userAction, step, context);
            if (!shouldKeepPausing)
            {
                if (Boolean.TRUE.equals(context != null ? context.getTransientData().get("KEY_STEP_RESCHEDULED") : null))
                {
                    return "RESCHEDULE";
                }
                if ("RUN".equalsIgnoreCase(action) || "STEP".equalsIgnoreCase(action) || "AUTO".equalsIgnoreCase(action)
                    || "RUN_ALL".equalsIgnoreCase(action) || "CONTINUE".equalsIgnoreCase(action))
                {
                    if (step != null)
                    {
                        step.setStatus(PlaybookStepStatus.RUNNING);
                    }
                    pushCurrentState(context, step);
                }
                return action;
            }
        }
    }

    /**
     * Pushes an immediate RUNNING state snapshot for the given step to all connected SSE console clients.
     *
     * @param context active execution context
     * @param step playbook step currently being processed
     */
    public void pushCurrentState(final ExecutionContext context, final PlaybookStep step)
    {
        if (this.consoleEngine == null)
        {
            return;
        }

        final int stepIndex = resolveStepIndex(context, step);

        final String stateJson = InteractiveStateBuilder.buildStateJson(
            this.session, context, this.consoleEngine.getRunId(), stepIndex, "running", null, getReport());

        this.consoleEngine.pushState(stateJson);
    }

    private void applyDataBindings(final JsonObject newBindings, final PlaybookStep currentStep, final ExecutionContext context, final boolean isEditAction)
    {
        if (newBindings == null || context == null)
        {
            return;
        }

        final SessionData sessionData = context.getSessionData();
        for (final Map.Entry<String, JsonElement> entry : newBindings.entrySet())
        {
            final String key = entry.getKey();
            final JsonElement el = entry.getValue();
            if (el != null && !el.isJsonNull())
            {
                final String val = el.getAsString();
                if (sessionData != null)
                {
                    sessionData.set(key, val);
                }

                try
                {
                    if (Neodymium.getData() != null)
                    {
                        Neodymium.getData().put(key, val);
                    }
                }
                catch (final Throwable ignored)
                {
                }

                try
                {
                    if (System.getProperty(key) != null)
                    {
                        System.setProperty(key, val);
                    }
                }
                catch (final Throwable ignored)
                {
                }
            }
        }

        if (sessionData != null && currentStep != null)
        {
            final int stepIndex = resolveStepIndex(context, currentStep);
            sessionData.captureSnapshot(stepIndex);
        }

        if (isEditAction)
        {
            context.getTransientData().put("KEY_BINDINGS_EDITED", Boolean.TRUE);
        }
        LOG.info("[InteractiveConsoleListener] Updated {} data bindings from user action (isEditAction={})", newBindings.size(), isEditAction);
    }

    private boolean handleUserAction(final JsonObject userAction, final PlaybookStep currentStep, final ExecutionContext context)
    {
        if (userAction == null)
        {
            return false;
        }

        final String action = userAction.has("action") ? userAction.get("action").getAsString() : "";
        final boolean isEditAction = "EDIT".equalsIgnoreCase(action) || "UPDATE_STEP".equalsIgnoreCase(action) || "SAVE_STEP".equalsIgnoreCase(action);

        if (userAction.has("bindings") && userAction.get("bindings").isJsonObject())
        {
            final JsonObject newBindings = userAction.getAsJsonObject("bindings");
            applyDataBindings(newBindings, currentStep, context, isEditAction);
        }

        LOG.info("[InteractiveConsoleListener] User action received: {}", action);

        switch (action.toUpperCase())
        {
            case "ADD":
                return handleAddStep(userAction, currentStep, context);

            case "REORDER":
                return handleReorderStep(userAction, currentStep, context);

            case "AUTO":
            case "RUN_ALL":
            case "CONTINUE":
                this.autoRun = true;
                LOG.info("[InteractiveConsoleListener] Resuming auto-run execution mode");
                return false;

            case "SKIP":
                if (currentStep != null)
                {
                    currentStep.setStatus(PlaybookStepStatus.SKIPPED);
                    if (currentStep.getParent() != null)
                    {
                        final PlaybookStep parent = currentStep.getParent();
                        final String parentInst = parent.getInstruction();
                        final String incFile = currentStep.getSourceFile();
                        final int subIdx = parent.getSubSteps().indexOf(currentStep) + 1;
                        final String parentContext = (incFile != null && !incFile.isBlank())
                            ? String.format("parent include \"%s\" (includeFile=%s)", parentInst, incFile)
                            : String.format("parent step \"%s\"", parentInst);
                        LOG.warn("[InteractiveConsoleListener] Step marked as SKIPPED per user request on sub-step #{}/{} (\"{}\") inside {}. Note: Sub-steps cannot be skipped individually; skip the parent to skip this block.",
                            subIdx, parent.getSubSteps().size(), currentStep.getInstruction(), parentContext);
                    }
                    else if (currentStep.hasSubSteps())
                    {
                        LOG.info("[InteractiveConsoleListener] Parent include step marked as SKIPPED per user request: \"{}\" containing {} sub-steps",
                            currentStep.getInstruction(), currentStep.getSubSteps().size());
                        for (final PlaybookStep sub : currentStep.getSubSteps())
                        {
                            sub.setStatus(PlaybookStepStatus.SKIPPED);
                        }
                    }
                    else
                    {
                        LOG.info("[InteractiveConsoleListener] Step marked as SKIPPED per user request: \"{}\"", currentStep.getInstruction());
                    }
                    pushCurrentState(context, currentStep);
                }
                return false;

            case "EDIT":
            case "UPDATE_STEP":
            case "SAVE_STEP":
                int targetStepIdx = -1;
                if (userAction.has("blockIndex") && !userAction.get("blockIndex").isJsonNull())
                {
                    try
                    {
                        targetStepIdx = userAction.get("blockIndex").getAsInt();
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                else if (userAction.has("stepIndex") && !userAction.get("stepIndex").isJsonNull())
                {
                    try
                    {
                        targetStepIdx = userAction.get("stepIndex").getAsInt();
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                else if (userAction.has("index") && !userAction.get("index").isJsonNull())
                {
                    try
                    {
                        targetStepIdx = userAction.get("index").getAsInt();
                    }
                    catch (final Exception ignored)
                    {
                    }
                }

                final boolean hasExplicitCurrent = userAction.has("isCurrentStep") && !userAction.get("isCurrentStep").isJsonNull();
                final boolean isExplicitCurrent = hasExplicitCurrent && userAction.get("isCurrentStep").getAsBoolean();
                final boolean isExplicitNonCurrent = hasExplicitCurrent && !userAction.get("isCurrentStep").getAsBoolean();

                final int currentIdx = resolveStepIndex(context, currentStep);
                final boolean isCurrentStepEdit = isExplicitCurrent
                    || (!isExplicitNonCurrent && (targetStepIdx == -1 || targetStepIdx == currentIdx));

                if (isCurrentStepEdit && currentStep != null)
                {
                    String newInst = null;
                    if (userAction.has("newInstruction"))
                    {
                        newInst = userAction.get("newInstruction").getAsString();
                    }
                    else if (userAction.has("instruction"))
                    {
                        newInst = userAction.get("instruction").getAsString();
                    }
                    if (newInst != null && !newInst.isBlank())
                    {
                        currentStep.setInstruction(newInst);
                        currentStep.setRawInstruction(newInst);
                        currentStep.setStatus(PlaybookStepStatus.RUNNING);
                        currentStep.setFailed(false);
                        currentStep.setFailureReason(null);
                        if (currentStep.getActions() != null)
                        {
                            try
                            {
                                currentStep.getActions().clear();
                            }
                            catch (final UnsupportedOperationException e)
                            {
                                currentStep.setActions(new ArrayList<>());
                            }
                        }
                        if (currentStep.getToolCalls() != null)
                        {
                            try
                            {
                                currentStep.getToolCalls().clear();
                            }
                            catch (final UnsupportedOperationException e)
                            {
                                currentStep.setToolCalls(new ArrayList<>());
                            }
                        }
                        currentStep.setReasoning(null);
                        if (context != null)
                        {
                            context.getTransientData().put("KEY_STEP_EDITED", true);
                        }
                        pushCurrentState(context, currentStep);
                        LOG.info("[InteractiveConsoleListener] Step instruction updated to: \"{}\"", newInst);
                    }
                    else
                    {
                        currentStep.setStatus(PlaybookStepStatus.RUNNING);
                        currentStep.setFailed(false);
                        currentStep.setFailureReason(null);
                        if (currentStep.getActions() != null)
                        {
                            try
                            {
                                currentStep.getActions().clear();
                            }
                            catch (final UnsupportedOperationException e)
                            {
                                currentStep.setActions(new ArrayList<>());
                            }
                        }
                        if (currentStep.getToolCalls() != null)
                        {
                            try
                            {
                                currentStep.getToolCalls().clear();
                            }
                            catch (final UnsupportedOperationException e)
                            {
                                currentStep.setToolCalls(new ArrayList<>());
                            }
                        }
                        currentStep.setReasoning(null);
                        if (context != null)
                        {
                            context.getTransientData().put("KEY_STEP_EDITED", true);
                        }
                        pushCurrentState(context, currentStep);
                        LOG.info("[InteractiveConsoleListener] Step re-queued with updated parameters/bindings: \"{}\"", currentStep.getInstruction());
                    }
                    return false;
                }
                else if (context != null && targetStepIdx != -1)
                {
                    final String blockName = userAction.has("block") && !userAction.get("block").isJsonNull()
                        ? userAction.get("block").getAsString()
                        : null;
                    PlaybookStep targetStep = findStepByIndex(context, targetStepIdx, blockName);
                    if (targetStep == null && userAction.has("flatIndex") && !userAction.get("flatIndex").isJsonNull())
                    {
                        try
                        {
                            targetStep = findStepByIndex(context, userAction.get("flatIndex").getAsInt(), null);
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                    if (targetStep == null && targetStepIdx > 0)
                    {
                        targetStep = findStepByIndex(context, targetStepIdx - 1, blockName);
                    }
                    if (targetStep != null)
                    {
                        String newInst = null;
                        if (userAction.has("newInstruction"))
                        {
                            newInst = userAction.get("newInstruction").getAsString();
                        }
                        else if (userAction.has("instruction"))
                        {
                            newInst = userAction.get("instruction").getAsString();
                        }
                        if (newInst != null && !newInst.isBlank())
                        {
                            targetStep.setInstruction(newInst);
                            targetStep.setRawInstruction(newInst);
                            updateFlatSteps(context);
                            final String targetBlock = resolveStepBlock(context, targetStep);
                            final String currentBlock = resolveActiveBlock(context, currentStep);
                            if (targetBlock.equalsIgnoreCase(currentBlock))
                            {
                                rescheduleRemainingSteps(context, currentStep);
                            }
                            LOG.info("[InteractiveConsoleListener] Updated pending step #{} instruction to: \"{}\"", targetStepIdx, newInst);
                        }
                    }
                    return true;
                }
                return false;

            case "HEAL":
                this.autoRun = false;
                LOG.info("[InteractiveConsoleListener] AI Healing requested by user for step");
                return false;

            case "SUGGEST_FIX":
                this.autoRun = false;
                LOG.info("[InteractiveConsoleListener] AI Fix Suggestion requested by user for step");
                handleSuggestFix(userAction, currentStep, context);
                return true;

            case "FINISH":
            case "ACCEPT_FINISH":
                this.autoRun = false;
                LOG.info("[InteractiveConsoleListener] User accepted current state and requested to finish test");
                return false;

            case "ABORT":
            case "STOP":
                throw new RuntimeException(new ConclusiveFailureException("Interactive test execution aborted by user via Aura Manager"));

            case "SAVE_EXIT":
            case "DISCARD":
            case "CLOSE":
                this.autoRun = false;
                LOG.info("[InteractiveConsoleListener] Session final action completed ({})", action);
                return false;

            case "RUN":
            case "NEXT":
            case "STEP":
            case "EXECUTE":
            default:
                this.autoRun = false;
                LOG.info("[InteractiveConsoleListener] Executing single step; will pause on next step");
                return false;
        }
    }

    private boolean handleAddStep(final JsonObject userAction, final PlaybookStep currentStep, final ExecutionContext context)
    {
        if (userAction == null || context == null)
        {
            return false;
        }

        final String instruction = userAction.has("instruction") ? userAction.get("instruction").getAsString() : "";
        if (instruction == null || instruction.isBlank())
        {
            return false;
        }

        final String blockName = userAction.has("block") ? userAction.get("block").getAsString().toLowerCase(Locale.ROOT) : "steps";
        final String listKey = switch (blockName)
        {
            case "before" -> "playbook.beforeSteps";
            case "after" -> "playbook.afterSteps";
            default -> "playbook.steps";
        };

        @SuppressWarnings("unchecked")
        List<PlaybookStep> targetList = (List<PlaybookStep>) context.getTransientData().get(listKey);
        if (targetList == null)
        {
            final Object pbObj = context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
            if (pbObj instanceof Playbook pb)
            {
                targetList = switch (blockName)
                {
                    case "before" -> new ArrayList<>(pb.getBeforeSteps());
                    case "after" -> new ArrayList<>(pb.getAfterSteps());
                    default -> new ArrayList<>(pb.getSteps());
                };
            }
            else
            {
                targetList = new ArrayList<>();
            }
            context.getTransientData().put(listKey, targetList);
        }
        else if (!(targetList instanceof ArrayList))
        {
            targetList = new ArrayList<>(targetList);
            context.getTransientData().put(listKey, targetList);
        }

        final PlaybookStep newStep = new PlaybookStep();
        newStep.setInstruction(instruction);
        newStep.setRawInstruction(instruction);
        newStep.setBlock(blockName);
        newStep.setStatus(PlaybookStepStatus.PENDING);
        newStep.setFailed(false);
        newStep.setSourceFile(context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_IDENTIFIER) != null
            ? String.valueOf(context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_IDENTIFIER))
            : "interactive");

        int targetIndex = -1;
        if (userAction.has("index") && !userAction.get("index").isJsonNull())
        {
            try
            {
                targetIndex = userAction.get("index").getAsInt();
            }
            catch (final Exception ignored)
            {
            }
        }

        final String currentBlock = resolveActiveBlock(context, currentStep);
        final int currentIdx = resolveStepIndex(context, currentStep);
        final boolean isInsertBeforeOrAtCurrent = blockName.equalsIgnoreCase(currentBlock) && targetIndex >= 0 && targetIndex <= currentIdx;

        if (targetIndex >= 0 && targetIndex <= targetList.size())
        {
            targetList.add(targetIndex, newStep);
        }
        else
        {
            targetList.add(newStep);
        }

        @SuppressWarnings("unchecked")
        final List<PlaybookStep> beforeStepsList = (List<PlaybookStep>) context.getTransientData().get("playbook.beforeSteps");
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> mainStepsList = (List<PlaybookStep>) context.getTransientData().get("playbook.steps");
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> afterStepsList = (List<PlaybookStep>) context.getTransientData().get("playbook.afterSteps");

        final Object playbookObj = context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
        if (playbookObj instanceof Playbook playbook)
        {
            final List<PlaybookStep> effBefore = beforeStepsList != null ? beforeStepsList : playbook.getBeforeSteps();
            final List<PlaybookStep> effMain = mainStepsList != null ? mainStepsList : playbook.getSteps();
            final List<PlaybookStep> effAfter = afterStepsList != null ? afterStepsList : playbook.getAfterSteps();
            context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK,
                new Playbook(effBefore, effMain, effAfter, playbook.getDataSets(), playbook.getPromptAddons(), playbook.getDescription()));
        }

        if (mainStepsList != null && context.getTransientData().containsKey("playbook.mainSteps"))
        {
            context.getTransientData().put("playbook.mainSteps", mainStepsList);
        }
        if (beforeStepsList != null && context.getTransientData().containsKey("playbook.beforeStepsToRun"))
        {
            context.getTransientData().put("playbook.beforeStepsToRun", beforeStepsList);
        }
        if (afterStepsList != null && context.getTransientData().containsKey("playbook.afterStepsToRun"))
        {
            context.getTransientData().put("playbook.afterStepsToRun", afterStepsList);
        }

        if (isInsertBeforeOrAtCurrent && currentStep != null)
        {
            if (currentStep.getActions() != null)
            {
                try
                {
                    currentStep.getActions().clear();
                }
                catch (final UnsupportedOperationException e)
                {
                    currentStep.setActions(new ArrayList<>());
                }
            }
            if (currentStep.getToolCalls() != null)
            {
                try
                {
                    currentStep.getToolCalls().clear();
                }
                catch (final UnsupportedOperationException e)
                {
                    currentStep.setToolCalls(new ArrayList<>());
                }
            }
            currentStep.setReasoning(null);
            currentStep.setStatus(PlaybookStepStatus.PENDING);
            context.getTransientData().put("KEY_STEP_RESCHEDULED", Boolean.TRUE);
        }

        updateFlatSteps(context);
        if (blockName.equalsIgnoreCase(currentBlock))
        {
            rescheduleRemainingSteps(context, currentStep);
        }

        LOG.info("[InteractiveConsoleListener] Added new step to '{}' at index {}: \"{}\"", blockName, targetIndex, instruction);
        return !isInsertBeforeOrAtCurrent;
    }

    private boolean handleReorderStep(final JsonObject userAction, final PlaybookStep currentStep, final ExecutionContext context)
    {
        if (userAction == null || context == null)
        {
            return false;
        }

        final String fromBlock = userAction.has("fromBlock")
            ? userAction.get("fromBlock").getAsString().toLowerCase()
            : (userAction.has("block") ? userAction.get("block").getAsString().toLowerCase() : "steps");

        final String toBlock = userAction.has("toBlock")
            ? userAction.get("toBlock").getAsString().toLowerCase()
            : fromBlock;

        int fromIndex = -1;
        if (userAction.has("fromIndex") && !userAction.get("fromIndex").isJsonNull())
        {
            try
            {
                fromIndex = userAction.get("fromIndex").getAsInt();
            }
            catch (final Exception ignored)
            {
            }
        }
        else if (userAction.has("sourceIndex") && !userAction.get("sourceIndex").isJsonNull())
        {
            try
            {
                fromIndex = userAction.get("sourceIndex").getAsInt();
            }
            catch (final Exception ignored)
            {
            }
        }

        int toIndex = -1;
        if (userAction.has("toIndex") && !userAction.get("toIndex").isJsonNull())
        {
            try
            {
                toIndex = userAction.get("toIndex").getAsInt();
            }
            catch (final Exception ignored)
            {
            }
        }
        else if (userAction.has("targetIndex") && !userAction.get("targetIndex").isJsonNull())
        {
            try
            {
                toIndex = userAction.get("targetIndex").getAsInt();
            }
            catch (final Exception ignored)
            {
            }
        }

        if (fromIndex == -1 || toIndex == -1)
        {
            return true;
        }

        final String fromKey = switch (fromBlock)
        {
            case "before" -> "playbook.beforeSteps";
            case "after" -> "playbook.afterSteps";
            default -> "playbook.steps";
        };
        final String toKey = switch (toBlock)
        {
            case "before" -> "playbook.beforeSteps";
            case "after" -> "playbook.afterSteps";
            default -> "playbook.steps";
        };

        @SuppressWarnings("unchecked")
        List<PlaybookStep> fromList = (List<PlaybookStep>) context.getTransientData().get(fromKey);
        if (fromList == null)
        {
            final Object pbObj = context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
            if (pbObj instanceof Playbook pb && "steps".equals(fromBlock))
            {
                fromList = new ArrayList<>(pb.getSteps());
                context.getTransientData().put(fromKey, fromList);
            }
        }
        @SuppressWarnings("unchecked")
        List<PlaybookStep> toList = (List<PlaybookStep>) context.getTransientData().get(toKey);
        if (toList == null)
        {
            if (fromBlock.equals(toBlock))
            {
                toList = fromList;
            }
            else
            {
                final Object pbObj = context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
                if (pbObj instanceof Playbook pb && "steps".equals(toBlock))
                {
                    toList = new ArrayList<>(pb.getSteps());
                }
                else
                {
                    toList = new ArrayList<>();
                }
                context.getTransientData().put(toKey, toList);
            }
        }

        if (fromList == null || fromIndex < 0 || fromIndex >= fromList.size())
        {
            return true;
        }
        if (!(fromList instanceof ArrayList))
        {
            fromList = new ArrayList<>(fromList);
            context.getTransientData().put(fromKey, fromList);
        }
        if (fromBlock.equals(toBlock))
        {
            toList = fromList;
        }
        else if (toList == null)
        {
            toList = new ArrayList<>();
            context.getTransientData().put(toKey, toList);
        }
        else if (!(toList instanceof ArrayList))
        {
            toList = new ArrayList<>(toList);
            context.getTransientData().put(toKey, toList);
        }

        final String currentBlock = resolveActiveBlock(context, currentStep);
        final int currentIdx = resolveStepIndex(context, currentStep);
        final PlaybookStep removed = fromList.remove(fromIndex);
        removed.setBlock(toBlock);
        if (removed.hasSubSteps())
        {
            for (final PlaybookStep sub : removed.getSubSteps())
            {
                sub.setBlock(toBlock);
            }
        }
        int adjustedToIndex = toIndex;
        if (adjustedToIndex < 0)
        {
            adjustedToIndex = 0;
        }
        if (adjustedToIndex > toList.size())
        {
            adjustedToIndex = toList.size();
        }
        toList.add(adjustedToIndex, removed);

        @SuppressWarnings("unchecked")
        final List<PlaybookStep> beforeStepsList = (List<PlaybookStep>) context.getTransientData().get("playbook.beforeSteps");
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> mainStepsList = (List<PlaybookStep>) context.getTransientData().get("playbook.steps");
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> afterStepsList = (List<PlaybookStep>) context.getTransientData().get("playbook.afterSteps");

        final Object playbookObj = context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
        if (playbookObj instanceof Playbook playbook)
        {
            final List<PlaybookStep> effBefore = beforeStepsList != null ? beforeStepsList : playbook.getBeforeSteps();
            final List<PlaybookStep> effMain = mainStepsList != null ? mainStepsList : playbook.getSteps();
            final List<PlaybookStep> effAfter = afterStepsList != null ? afterStepsList : playbook.getAfterSteps();
            context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK,
                new Playbook(effBefore, effMain, effAfter, playbook.getDataSets(), playbook.getPromptAddons(), playbook.getDescription()));
        }

        if (mainStepsList != null && context.getTransientData().containsKey("playbook.mainSteps"))
        {
            context.getTransientData().put("playbook.mainSteps", mainStepsList);
        }
        if (beforeStepsList != null && context.getTransientData().containsKey("playbook.beforeStepsToRun"))
        {
            context.getTransientData().put("playbook.beforeStepsToRun", beforeStepsList);
        }
        if (afterStepsList != null && context.getTransientData().containsKey("playbook.afterStepsToRun"))
        {
            context.getTransientData().put("playbook.afterStepsToRun", afterStepsList);
        }

        final boolean isFromCurrent = currentBlock.equalsIgnoreCase(fromBlock) && fromIndex == currentIdx;
        final boolean isToBeforeOrAtCurrent = currentBlock.equalsIgnoreCase(toBlock) && adjustedToIndex <= currentIdx;
        final boolean isTargetBefore = "before".equalsIgnoreCase(toBlock) && !"before".equalsIgnoreCase(currentBlock);
        final boolean isRescheduleNeeded = isFromCurrent || isToBeforeOrAtCurrent || isTargetBefore;

        if (isRescheduleNeeded && currentStep != null)
        {
            if (currentStep.getActions() != null)
            {
                try
                {
                    currentStep.getActions().clear();
                }
                catch (final UnsupportedOperationException e)
                {
                    currentStep.setActions(new ArrayList<>());
                }
            }
            if (currentStep.getToolCalls() != null)
            {
                try
                {
                    currentStep.getToolCalls().clear();
                }
                catch (final UnsupportedOperationException e)
                {
                    currentStep.setToolCalls(new ArrayList<>());
                }
            }
            currentStep.setReasoning(null);
            currentStep.setStatus(PlaybookStepStatus.PENDING);
            context.getTransientData().put("KEY_STEP_RESCHEDULED", Boolean.TRUE);
        }

        updateFlatSteps(context);
        if (fromBlock.equalsIgnoreCase(currentBlock) || toBlock.equalsIgnoreCase(currentBlock))
        {
            rescheduleRemainingSteps(context, currentStep);
        }

        LOG.info("[InteractiveConsoleListener] Reordered step from '{}:{}' to '{}:{}' (rescheduled={})",
            fromBlock, fromIndex, toBlock, adjustedToIndex, isRescheduleNeeded);
        return !isRescheduleNeeded;
    }

    private void updateFlatSteps(final ExecutionContext context)
    {
        if (context == null)
        {
            return;
        }
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> beforeSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.beforeSteps");
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> playbookSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.steps");
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> afterSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.afterSteps");

        @SuppressWarnings("unchecked")
        List<PlaybookStep> flatSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.flatSteps");
        if (flatSteps == null)
        {
            flatSteps = new ArrayList<>();
            context.getTransientData().put("playbook.flatSteps", flatSteps);
        }
        flatSteps.clear();
        if (beforeSteps != null)
        {
            flattenSteps(beforeSteps, flatSteps);
        }
        if (playbookSteps != null)
        {
            flattenSteps(playbookSteps, flatSteps);
        }
        if (afterSteps != null)
        {
            flattenSteps(afterSteps, flatSteps);
        }
    }

    private static void flattenSteps(final List<PlaybookStep> source, final List<PlaybookStep> target)
    {
        if (source == null || target == null)
        {
            return;
        }
        for (final PlaybookStep s : source)
        {
            target.add(s);
            if (s.getSubSteps() != null && !s.getSubSteps().isEmpty())
            {
                flattenSteps(s.getSubSteps(), target);
            }
        }
    }

    private void rescheduleRemainingSteps(final ExecutionContext context, final PlaybookStep activeStep)
    {
        if (context == null)
        {
            return;
        }

        final PlaybookStep currentStep = activeStep != null
            ? activeStep
            : (context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP) instanceof PlaybookStep ps ? ps : null);

        final String activeBlock = resolveActiveBlock(context, currentStep);
        final String listKey = switch (activeBlock)
        {
            case "before" -> "playbook.beforeSteps";
            case "after" -> "playbook.afterSteps";
            default -> "playbook.steps";
        };

        @SuppressWarnings("unchecked")
        List<PlaybookStep> blockSteps = (List<PlaybookStep>) context.getTransientData().get(listKey);
        if (blockSteps == null)
        {
            final Object pbObj = context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
            if (pbObj instanceof Playbook pb)
            {
                blockSteps = switch (activeBlock)
                {
                    case "before" -> pb.getBeforeSteps();
                    case "after" -> pb.getAfterSteps();
                    default -> pb.getSteps();
                };
            }
        }
        if (blockSteps == null)
        {
            return;
        }

        final PlaybookStep rootCurrent = (currentStep != null && currentStep.getRootStep() != null)
            ? currentStep.getRootStep()
            : currentStep;

        final List<PlaybookStep> remaining = new ArrayList<>();
        final boolean isRescheduled = Boolean.TRUE.equals(context.getTransientData().get("KEY_STEP_RESCHEDULED"));

        if (isRescheduled)
        {
            int firstPendingIdx = -1;
            for (int i = 0; i < blockSteps.size(); i++)
            {
                if (blockSteps.get(i).getStatus() == PlaybookStepStatus.PENDING)
                {
                    firstPendingIdx = i;
                    break;
                }
            }
            if (firstPendingIdx >= 0)
            {
                for (int i = firstPendingIdx; i < blockSteps.size(); i++)
                {
                    remaining.add(blockSteps.get(i));
                }
            }
        }
        else if (rootCurrent != null)
        {
            final int currentIdx = blockSteps.indexOf(rootCurrent);
            if (currentIdx >= 0)
            {
                for (int i = currentIdx + 1; i < blockSteps.size(); i++)
                {
                    remaining.add(blockSteps.get(i));
                }
            }
            else
            {
                for (final PlaybookStep s : blockSteps)
                {
                    if (s.getStatus() == PlaybookStepStatus.PENDING)
                    {
                        remaining.add(s);
                    }
                }
            }
        }
        else
        {
            for (final PlaybookStep s : blockSteps)
            {
                if (s.getStatus() == PlaybookStepStatus.PENDING)
                {
                    remaining.add(s);
                }
            }
        }

        context.updateRemainingPlaybookSteps(remaining, this.session);
    }

    private PlaybookStep findStepByIndex(final ExecutionContext context, final int targetIndex)
    {
        return findStepByIndex(context, targetIndex, null);
    }

    private PlaybookStep findStepByIndex(final ExecutionContext context, final int targetIndex, final String blockName)
    {
        if (context == null || targetIndex < 0)
        {
            return null;
        }
        if (blockName != null && !blockName.isBlank())
        {
            final String listKey = switch (blockName.toLowerCase())
            {
                case "before" -> "playbook.beforeSteps";
                case "after" -> "playbook.afterSteps";
                default -> "playbook.steps";
            };
            @SuppressWarnings("unchecked")
            final List<PlaybookStep> blockSteps = (List<PlaybookStep>) context.getTransientData().get(listKey);
            if (blockSteps != null && targetIndex < blockSteps.size())
            {
                return blockSteps.get(targetIndex);
            }
        }
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> flatSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.flatSteps");
        if (flatSteps != null && targetIndex < flatSteps.size())
        {
            return flatSteps.get(targetIndex);
        }
        @SuppressWarnings("unchecked")
        List<PlaybookStep> playbookSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.steps");
        if (playbookSteps == null)
        {
            final Object pbObj = context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
            if (pbObj instanceof Playbook pb)
            {
                playbookSteps = pb.getSteps();
            }
        }
        if (playbookSteps != null && targetIndex < playbookSteps.size())
        {
            return playbookSteps.get(targetIndex);
        }
        return null;
    }

    private void handleSuggestFix(final JsonObject userAction, final PlaybookStep currentStep, final ExecutionContext context)
    {
        final String instruction = currentStep != null ? currentStep.getInstruction() : "";
        String suggestion = instruction;
        try
        {
            if (this.session != null && instruction != null && !instruction.isBlank())
            {
                final LlmProvider provider = this.session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
                if (provider != null)
                {
                    final AiConfiguration config = AiConfiguration.getInstance();
                    final double temp = config.getTemperature("action");
                    final int timeoutSeconds = config.getTimeoutSeconds("action");

                    final LlmRequest request = new LlmRequest(
                        "You are an expert test automation assistant. Suggest a clear, atomic instruction for a web test step.",
                        "Please suggest a clear, atomic phrasing for this test step: \"" + instruction + "\"",
                        Collections.emptyList(),
                        null,
                        temp,
                        timeoutSeconds
                    );

                    final LlmResponse response = provider.chat(request);
                    if (response != null && response.content() != null && !response.content().isBlank())
                    {
                        suggestion = response.content().trim();
                    }
                }
            }
        }
        catch (final Throwable t)
        {
            LOG.warn("[InteractiveConsoleListener] Failed to generate suggestion for step: {}", t.getMessage());
        }

        final JsonObject fixEvent = new JsonObject();
        fixEvent.addProperty("originalInstruction", instruction);
        fixEvent.addProperty("suggestedInstruction", suggestion);
        if (this.consoleEngine != null)
        {
            this.consoleEngine.broadcastSseEvent("fixSuggestion", new Gson().toJson(fixEvent));
        }
    }

    private int resolveStepIndex(final ExecutionContext context, final PlaybookStep step)
    {
        if (context == null || step == null)
        {
            return 0;
        }
        final PlaybookStep root = step.getRootStep() != null ? step.getRootStep() : step;
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> beforeSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.beforeSteps");
        if (beforeSteps != null && beforeSteps.contains(root))
        {
            return beforeSteps.indexOf(root);
        }
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> playbookSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.steps");
        if (playbookSteps != null && playbookSteps.contains(root))
        {
            return playbookSteps.indexOf(root);
        }
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> flatSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.flatSteps");
        if (flatSteps != null && flatSteps.contains(root))
        {
            return flatSteps.indexOf(root);
        }
        if (flatSteps != null && flatSteps.contains(step))
        {
            return flatSteps.indexOf(step);
        }
        @SuppressWarnings("unchecked")
        final List<PlaybookStep> afterSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.afterSteps");
        if (afterSteps != null && afterSteps.contains(root))
        {
            return afterSteps.indexOf(root);
        }
        return 0;
    }

    private String resolveStepBlock(final ExecutionContext context, final PlaybookStep step)
    {
        if (step != null)
        {
            final PlaybookStep root = step.getRootStep() != null ? step.getRootStep() : step;
            if (context != null)
            {
                @SuppressWarnings("unchecked")
                final List<PlaybookStep> beforeSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.beforeSteps");
                if (beforeSteps != null && (beforeSteps.contains(root) || beforeSteps.contains(step)))
                {
                    return "before";
                }
                @SuppressWarnings("unchecked")
                final List<PlaybookStep> afterSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.afterSteps");
                if (afterSteps != null && (afterSteps.contains(root) || afterSteps.contains(step)))
                {
                    return "after";
                }
                @SuppressWarnings("unchecked")
                final List<PlaybookStep> playbookSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.steps");
                if (playbookSteps != null && (playbookSteps.contains(root) || playbookSteps.contains(step)))
                {
                    return "steps";
                }
            }
            if (root.getBlock() != null && !root.getBlock().isBlank())
            {
                return root.getBlock().toLowerCase(Locale.ROOT);
            }
            if (step.getBlock() != null && !step.getBlock().isBlank())
            {
                return step.getBlock().toLowerCase(Locale.ROOT);
            }
        }
        return "steps";
    }

    private String resolveActiveBlock(final ExecutionContext context, final PlaybookStep activeStep)
    {
        if (activeStep != null)
        {
            return resolveStepBlock(context, activeStep);
        }
        if (context != null)
        {
            final Object curObj = context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
            if (curObj instanceof PlaybookStep ps)
            {
                return resolveStepBlock(context, ps);
            }
            if (context.hasSteps())
            {
                final PipelineStep top = context.peekStep();
                if (top instanceof PlaybookPipelineStep pps && pps.getPlaybookStep() != null)
                {
                    return resolveStepBlock(context, pps.getPlaybookStep());
                }
            }
            @SuppressWarnings("unchecked")
            final List<PlaybookStep> beforeSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.beforeSteps");
            if (beforeSteps != null && beforeSteps.stream().anyMatch(s -> s.getStatus() == PlaybookStepStatus.PENDING || s.getStatus() == PlaybookStepStatus.RUNNING))
            {
                return "before";
            }
            @SuppressWarnings("unchecked")
            final List<PlaybookStep> steps = (List<PlaybookStep>) context.getTransientData().get("playbook.steps");
            if (steps != null && steps.stream().anyMatch(s -> s.getStatus() == PlaybookStepStatus.PENDING || s.getStatus() == PlaybookStepStatus.RUNNING))
            {
                return "steps";
            }
            @SuppressWarnings("unchecked")
            final List<PlaybookStep> afterSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.afterSteps");
            if (afterSteps != null && afterSteps.stream().anyMatch(s -> s.getStatus() == PlaybookStepStatus.PENDING || s.getStatus() == PlaybookStepStatus.RUNNING))
            {
                return "after";
            }
        }
        return "steps";
    }
}
