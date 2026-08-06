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

import org.neodymium.ai.event.diagnostic.DiagnosticErrorEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if (event == null || this.consoleEngine == null)
        {
            return;
        }

        final ExecutionContext context = this.session != null ? this.session.getExecutionContext() : ExecutionContext.getActiveContext();

        if (event instanceof StepStartedEvent stepStarted)
        {
            final int stepIndex = stepStarted.getStepIndex();
            final PlaybookStep currentStep = stepStarted.getStep();
            if (currentStep != null)
            {
                currentStep.setStatus(PlaybookStepStatus.RUNNING);
            }

            if (context != null)
            {
                try
                {
                    if (com.codeborne.selenide.WebDriverRunner.hasWebDriverStarted())
                    {
                        final String base64 = com.codeborne.selenide.Selenide.screenshot(org.openqa.selenium.OutputType.BASE64);
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
                this.session, context, this.consoleEngine.getRunId(), stepIndex, "paused", this.consoleEngine.getCurrentPauseId());

            this.consoleEngine.pushState(stateJson);

            if (this.interactive && !this.autoRun)
            {
                LOG.info("[InteractiveConsoleListener] Pausing for user action at step {}", stepIndex);
                try
                {
                    final JsonObject userAction = this.consoleEngine.waitForAction();
                    handleUserAction(userAction, currentStep, context);
                }
                catch (final InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interactive execution interrupted", e);
                }
            }
        }
        else if (event instanceof StepFinishedEvent stepFinished)
        {
            int stepIndex = 0;
            if (context != null && stepFinished.getStep() != null)
            {
                @SuppressWarnings("unchecked")
                final java.util.List<org.neodymium.ai.model.PlaybookStep> flatSteps =
                    (java.util.List<org.neodymium.ai.model.PlaybookStep>) context.getTransientData().get("playbook.flatSteps");
                if (flatSteps != null)
                {
                    stepIndex = flatSteps.indexOf(stepFinished.getStep());
                }
            }
            final String stateJson = InteractiveStateBuilder.buildStateJson(
                this.session, context, this.consoleEngine.getRunId(), stepIndex, "running");

            this.consoleEngine.pushState(stateJson);
        }
        else if (event instanceof SessionFinishedEvent sessionFinished)
        {
            final String overallStatus = sessionFinished.isSuccess() ? "passed" : "failed";
            final String stateJson = InteractiveStateBuilder.buildStateJson(
                this.session, context, this.consoleEngine.getRunId(), 0, overallStatus);

            this.consoleEngine.pushState(stateJson);
        }
        else if (event instanceof DiagnosticErrorEvent errorEvent)
        {
            LOG.warn("[InteractiveConsoleListener] Execution diagnostic error reported: {}", errorEvent.getMessage());
        }
    }

    private void handleUserAction(final JsonObject userAction, final PlaybookStep currentStep, final ExecutionContext context)
    {
        if (userAction == null)
        {
            return;
        }

        final String action = userAction.has("action") ? userAction.get("action").getAsString() : "";
        LOG.info("[InteractiveConsoleListener] User action received: {}", action);

        switch (action.toUpperCase())
        {
            case "AUTO":
            case "RUN_ALL":
            case "CONTINUE":
                this.autoRun = true;
                LOG.info("[InteractiveConsoleListener] Resuming auto-run execution mode");
                break;

            case "SKIP":
                if (currentStep != null)
                {
                    currentStep.setStatus(PlaybookStepStatus.SKIPPED);
                    LOG.info("[InteractiveConsoleListener] Step marked as SKIPPED per user request");
                }
                break;

            case "ABORT":
            case "STOP":
                throw new RuntimeException(new ConclusiveFailureException("Interactive test execution aborted by user via Aura Manager"));

            case "RUN":
            case "NEXT":
            case "STEP":
            case "EXECUTE":
            default:
                // Single step execution; run current step and pause again on next StepStartedEvent
                this.autoRun = false;
                LOG.info("[InteractiveConsoleListener] Executing single step; will pause on next step");
                break;
        }
    }
}
