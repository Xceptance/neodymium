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

import java.util.Collections;

import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.event.diagnostic.DiagnosticErrorEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.prompt.PesapPrompt;
import org.neodymium.ai.session.AiSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
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
                this.session, context, this.consoleEngine.getRunId(), stepIndex, "running", null);

            this.consoleEngine.pushState(stateJson);
        }
        else if (event instanceof StepFinishedEvent stepFinished)
        {
            int stepIndex = 0;
            if (context != null && stepFinished.getStep() != null)
            {
                final PlaybookStep target = stepFinished.getStep();
                @SuppressWarnings("unchecked")
                final java.util.List<PlaybookStep> beforeSteps = (java.util.List<PlaybookStep>) context.getTransientData().get("playbook.beforeSteps");
                @SuppressWarnings("unchecked")
                final java.util.List<PlaybookStep> flatSteps = (java.util.List<PlaybookStep>) context.getTransientData().get("playbook.flatSteps");
                @SuppressWarnings("unchecked")
                final java.util.List<PlaybookStep> afterSteps = (java.util.List<PlaybookStep>) context.getTransientData().get("playbook.afterSteps");

                if (beforeSteps != null && beforeSteps.contains(target))
                {
                    stepIndex = beforeSteps.indexOf(target);
                }
                else if (flatSteps != null && flatSteps.contains(target))
                {
                    stepIndex = flatSteps.indexOf(target);
                }
                else if (afterSteps != null && afterSteps.contains(target))
                {
                    stepIndex = afterSteps.indexOf(target);
                }
            }
            final String stateJson = InteractiveStateBuilder.buildStateJson(
                this.session, context, this.consoleEngine.getRunId(), stepIndex, "running");

            this.consoleEngine.pushState(stateJson);
        }
        else if (event instanceof SessionFinishedEvent sessionFinished)
        {
            final String overallStatus = sessionFinished.isSuccess() ? "passed" : "failed";

            if (this.interactive)
            {
                final String pauseId = "pause-final-" + java.util.UUID.randomUUID().toString();
                this.consoleEngine.registerPauseId(pauseId);

                final String stateJson = InteractiveStateBuilder.buildStateJson(
                    this.session, context, this.consoleEngine.getRunId(), 0, overallStatus, pauseId);

                this.consoleEngine.pushState(stateJson);

                LOG.info("[InteractiveConsoleListener] Session finished with status '{}'. Awaiting user final action (pauseId={})", overallStatus, pauseId);
                try
                {
                    final JsonObject userAction = this.consoleEngine.waitForAction(pauseId);
                    handleUserAction(userAction, null, context);
                }
                catch (final InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
            else
            {
                final String stateJson = InteractiveStateBuilder.buildStateJson(
                    this.session, context, this.consoleEngine.getRunId(), 0, overallStatus);

                this.consoleEngine.pushState(stateJson);
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

        int stepIndex = 0;
        if (context != null && step != null)
        {
            @SuppressWarnings("unchecked")
            final java.util.List<PlaybookStep> flatSteps = (java.util.List<PlaybookStep>) context.getTransientData().get("playbook.flatSteps");
            if (flatSteps != null && flatSteps.contains(step))
            {
                stepIndex = flatSteps.indexOf(step);
            }
        }

        final String pauseId = java.util.UUID.randomUUID().toString();
        this.consoleEngine.registerPauseId(pauseId);

        final String stateJson = InteractiveStateBuilder.buildStateJson(
            this.session, context, this.consoleEngine.getRunId(), stepIndex, "paused", pauseId);

        this.consoleEngine.pushState(stateJson);

        LOG.info("[InteractiveConsoleListener] Step failure encountered at step {}. Pausing for user recovery action (pauseId={})", stepIndex, pauseId);
        try
        {
            final JsonObject userAction = this.consoleEngine.waitForAction(pauseId);
            final String action = userAction != null && userAction.has("action") ? userAction.get("action").getAsString().toUpperCase() : "ABORT";
            handleUserAction(userAction, step, context);
            return action;
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return "ABORT";
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

        int stepIndex = 0;
        if (context != null && step != null)
        {
            @SuppressWarnings("unchecked")
            final java.util.List<PlaybookStep> flatSteps = (java.util.List<PlaybookStep>) context.getTransientData().get("playbook.flatSteps");
            if (flatSteps != null && flatSteps.contains(step))
            {
                stepIndex = flatSteps.indexOf(step);
            }
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

        final String pauseId = java.util.UUID.randomUUID().toString();
        this.consoleEngine.registerPauseId(pauseId);

        final String stateJson = InteractiveStateBuilder.buildStateJson(
            this.session, context, this.consoleEngine.getRunId(), stepIndex, "paused", pauseId);

        this.consoleEngine.pushState(stateJson);

        LOG.info("[InteractiveConsoleListener] Pausing for user action review at step {} (pauseId={})", stepIndex, pauseId);
        try
        {
            final JsonObject userAction = this.consoleEngine.waitForAction(pauseId);
            final String action = userAction != null && userAction.has("action") ? userAction.get("action").getAsString().toUpperCase() : "RUN";
            handleUserAction(userAction, step, context);
            return action;
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return "ABORT";
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

            case "EDIT":
            case "UPDATE_STEP":
            case "SAVE_STEP":
                if (currentStep != null)
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
                        if (currentStep.getActions() != null)
                        {
                            currentStep.getActions().clear();
                        }
                        currentStep.setReasoning(null);
                        if (context != null)
                        {
                            context.getTransientData().put("KEY_STEP_EDITED", true);
                        }
                        LOG.info("[InteractiveConsoleListener] Step instruction updated to: \"{}\"", newInst);
                    }
                }
                break;

            case "HEAL":
                this.autoRun = false;
                LOG.info("[InteractiveConsoleListener] AI Healing requested by user for step");
                break;

            case "SUGGEST_FIX":
                this.autoRun = false;
                LOG.info("[InteractiveConsoleListener] AI Fix Suggestion requested by user for step");
                handleSuggestFix(userAction, currentStep, context);
                break;

            case "FINISH":
            case "ACCEPT_FINISH":
                this.autoRun = false;
                LOG.info("[InteractiveConsoleListener] User accepted current state and requested to finish test");
                break;

            case "ABORT":
            case "STOP":
                throw new RuntimeException(new ConclusiveFailureException("Interactive test execution aborted by user via Aura Manager"));

            case "SAVE_EXIT":
            case "DISCARD":
            case "CLOSE":
                this.autoRun = false;
                LOG.info("[InteractiveConsoleListener] Session final action completed ({})", action);
                break;

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

    private void handleSuggestFix(final JsonObject userAction, final PlaybookStep currentStep, final ExecutionContext context)
    {
        final String instruction = currentStep != null ? currentStep.getInstruction() : "";
        String suggestion = instruction;
        try
        {
            if (this.session != null && instruction != null && !instruction.isBlank())
            {
                final PesapPrompt pesapPrompt = new PesapPrompt(instruction, null, null);
                final LlmProvider provider = this.session.getLlmRegistry().getProvider(LlmCapability.PESAP);
                if (provider != null)
                {
                    final AiConfiguration config = AiConfiguration.getInstance();
                    final double temp = config.getTemperature("action");
                    final int timeoutSeconds = config.getTimeoutSeconds("action");

                    final LlmRequest request = new LlmRequest(
                        pesapPrompt.compileSystemMessage(context),
                        pesapPrompt.compileUserMessage(context),
                        Collections.emptyList(),
                        pesapPrompt.getResponseSchema(),
                        temp,
                        timeoutSeconds
                    );

                    final LlmResponse response = provider.chat(request);
                    if (response != null && response.content() != null)
                    {
                        final PesapPrompt.PesapResult result = pesapPrompt.parseResponse(response.content(), context);
                        if (result != null && result.splitSteps() != null && !result.splitSteps().isEmpty())
                        {
                            suggestion = String.join(" and ", result.splitSteps());
                        }
                    }
                }
            }
        }
        catch (final Throwable t)
        {
            LOG.warn("[InteractiveConsoleListener] Failed to generate PESAP suggestion for step: {}", t.getMessage());
        }

        final JsonObject fixEvent = new JsonObject();
        fixEvent.addProperty("originalInstruction", instruction);
        fixEvent.addProperty("suggestedInstruction", suggestion);
        if (this.consoleEngine != null)
        {
            this.consoleEngine.broadcastSseEvent("fixSuggestion", new Gson().toJson(fixEvent));
        }
    }
}
