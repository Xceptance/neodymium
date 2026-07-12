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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.structural.SequenceStep;
import org.neodymium.ai.playbook.PlaybookParser;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.prompt.ActionSanitizer;
import org.neodymium.ai.prompt.AiPrompt;
import org.neodymium.ai.prompt.DefaultActionSanitizer;
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
        final AiSession session = (AiSession) context.getTransientData().get("session");
        // Retrieve SUT target executor driving browser/REST operations
        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get("targetExecutor");

        if (session == null || executor == null)
        {
            return;
        }

        // Retrieve the result returned by the prior CallLlmStep execution
        final Object result = context.getTransientData().get("lastLlmResult");
        if (result == null)
        {
            return;
        }

        // Initialize or fetch the concurrent recording collection tracking all executed playbooks actions
        @SuppressWarnings("unchecked")
        final List<Action> recordedActions = (List<Action>) context.getTransientData()
            .computeIfAbsent("recording", k -> new CopyOnWriteArrayList<>());

        // Process single actions or lists of actions dynamically returned by model response
        if (result instanceof List<?> list)
        {
            for (final Object obj : list)
            {
                if (obj instanceof Action action)
                {
                    executeSingleAction(action, executor, recordedActions, session, context);
                }
            }
        }
        else if (result instanceof Action action)
        {
            executeSingleAction(action, executor, recordedActions, session, context);
        }
    }

    /**
     * Helper executing a single action, applying sanitization, logging, and throwing healing exceptions on failure.
     */
    private void executeSingleAction(
        final Action action,
        final TargetExecutor executor,
        final List<Action> recordedActions,
        final AiSession session,
        final ExecutionContext context
    ) throws PipelineException
    {
        // Intercept INCLUDE control actions to perform dynamic runtime inclusion expansion
        if (action.getType().equalsIgnoreCase("INCLUDE"))
        {
            executeIncludeAction(action, session, context, recordedActions);
            return;
        }

        try
        {
            // Execute SUT action via targeted SUT driver
            executor.execute(action);
            
            // Mask any raw sensitive inputs dynamically matching SessionData variable keys
            final Action sanitized = this.actionSanitizer.sanitize(action, context.getSessionData());
            
            // Log to local recording and dispatch verification updates to active event listeners
            recordedActions.add(sanitized);
            session.getEventBus().dispatch(new ActionExecutedEvent(sanitized, true));
        }
        catch (final IOException e)
        {
            // Dispatch failed event status and throw HealingRequiredException to initiate recovery
            session.getEventBus().dispatch(new ActionExecutedEvent(action, false));
            throw new HealingRequiredException("Action execution failed against SUT: " + action.getDescription(), e);
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
        final PlaybookResourceManager manager = (PlaybookResourceManager) context.getTransientData().get("resourceManager");
        if (manager == null)
        {
            throw new ConclusiveFailureException("No PlaybookResourceManager registered in ExecutionContext transient data");
        }

        // Retrieve or instantiate the default YamlPlaybookParser
        PlaybookParser parser = (PlaybookParser) context.getTransientData().get("playbookParser");
        if (parser == null)
        {
            parser = new YamlPlaybookParser();
        }

        // Fetch thread-safe stack listing active includes to detect cycle inclusions
        @SuppressWarnings("unchecked")
        final List<String> runtimeStack = (List<String>) context.getTransientData()
            .computeIfAbsent("runtimeIncludeStack", k -> new ArrayList<>());

        if (runtimeStack.contains(path))
        {
            throw new ConclusiveFailureException("Circular dynamic inclusion detected: " + String.join(" -> ", runtimeStack) + " -> " + path);
        }

        // Resolve absolute or relative path context based on active parent directory
        final String currentParent = (String) context.getTransientData().getOrDefault("currentPlaybookIdentifier", "");
        final String resolvedIdentifier = manager.resolveInclude(currentParent, path);

        // Add to callstack before parsing to cover circular validations
        runtimeStack.add(path);
        try
        {
            // Parse included YAML playbook target
            final Playbook playbook = parser.parse(resolvedIdentifier, manager);
            final List<PlaybookStep> playbookSteps = playbook.getSteps();

            // Push included sub-steps onto LIFO stack in reverse order to ensure sequential execution
            for (int i = playbookSteps.size() - 1; i >= 0; i--)
            {
                final PlaybookStep step = playbookSteps.get(i);
                final PipelineStep stepPipeline = mapPlaybookStepToPipelineStep(step, session, context);
                context.pushStep(stepPipeline);
            }
            
            // Parameterize and log the INCLUDE step record in history
            final Action sanitized = this.actionSanitizer.sanitize(action, context.getSessionData());
            recordedActions.add(sanitized);
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
    private PipelineStep mapPlaybookStepToPipelineStep(
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
            contextState.getTransientData().put("currentPlaybookStep", step);
            contextState.getTransientData().put("currentInstruction", step.getInstruction());

            @SuppressWarnings("unchecked")
            final AiPrompt<List<Action>> activePrompt = (AiPrompt<List<Action>>) contextState.getTransientData()
                .get("activePrompt");

            if (activePrompt == null)
            {
                throw new ConclusiveFailureException("No active prompt template registered in ExecutionContext transient data");
            }

            // Standard step loop sequence: CaptureStateStep -> CallLlmStep -> ExecuteActionsStep
            final CaptureStateStep captureStep = new CaptureStateStep();
            final CallLlmStep<List<Action>> llmStep = new CallLlmStep<>(activePrompt, LlmCapability.TEXT_ONLY);
            final ExecuteActionsStep executeStep = new ExecuteActionsStep();

            // Push to context stack in reverse order (LIFO)
            contextState.pushStep(executeStep);
            contextState.pushStep(llmStep);
            contextState.pushStep(captureStep);
        };
    }
}
