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
        final AiSession session = (AiSession) context.getTransientData().get("session");
        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get("targetExecutor");

        if (session == null || executor == null)
        {
            return;
        }

        final Object result = context.getTransientData().get("lastLlmResult");
        if (result == null)
        {
            return;
        }

        @SuppressWarnings("unchecked")
        final List<Action> recordedActions = (List<Action>) context.getTransientData()
            .computeIfAbsent("recording", k -> new CopyOnWriteArrayList<>());

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
        if (action.getType().equalsIgnoreCase("INCLUDE"))
        {
            executeIncludeAction(action, session, context, recordedActions);
            return;
        }

        try
        {
            executor.execute(action);
            final Action sanitized = this.actionSanitizer.sanitize(action, context.getSessionData());
            recordedActions.add(sanitized);
            session.getEventBus().dispatch(new ActionExecutedEvent(sanitized, true));
        }
        catch (final IOException e)
        {
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
        final String pathTemp = action.getTarget();
        final String path = (pathTemp == null || pathTemp.trim().isEmpty()) ? action.getValue() : pathTemp;
        if (path == null || path.trim().isEmpty())
        {
            throw new ConclusiveFailureException("INCLUDE action target path is null or empty");
        }

        PlaybookResourceManager manager = (PlaybookResourceManager) context.getTransientData().get("resourceManager");
        if (manager == null)
        {
            throw new ConclusiveFailureException("No PlaybookResourceManager registered in ExecutionContext transient data");
        }

        PlaybookParser parser = (PlaybookParser) context.getTransientData().get("playbookParser");
        if (parser == null)
        {
            parser = new YamlPlaybookParser();
        }

        @SuppressWarnings("unchecked")
        final List<String> runtimeStack = (List<String>) context.getTransientData()
            .computeIfAbsent("runtimeIncludeStack", k -> new ArrayList<>());

        if (runtimeStack.contains(path))
        {
            throw new ConclusiveFailureException("Circular dynamic inclusion detected: " + String.join(" -> ", runtimeStack) + " -> " + path);
        }

        final String currentParent = (String) context.getTransientData().getOrDefault("currentPlaybookIdentifier", "");
        final String resolvedIdentifier = manager.resolveInclude(currentParent, path);

        runtimeStack.add(path);
        try
        {
            final Playbook playbook = parser.parse(resolvedIdentifier, manager);
            final List<PlaybookStep> playbookSteps = playbook.getSteps();

            // Push steps in reverse order onto the LIFO stack to execute them in forward order
            for (int i = playbookSteps.size() - 1; i >= 0; i--)
            {
                final PlaybookStep step = playbookSteps.get(i);
                final PipelineStep stepPipeline = mapPlaybookStepToPipelineStep(step, session, context);
                context.pushStep(stepPipeline);
            }
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
        if (step.isComposite())
        {
            final List<PipelineStep> subPipelineSteps = new ArrayList<>();
            for (final PlaybookStep subStep : step.getSubSteps())
            {
                subPipelineSteps.add(mapPlaybookStepToPipelineStep(subStep, session, context));
            }
            return new SequenceStep(subPipelineSteps);
        }

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

            final CaptureStateStep captureStep = new CaptureStateStep();
            final CallLlmStep<List<Action>> llmStep = new CallLlmStep<>(activePrompt, LlmCapability.TEXT_ONLY);
            final ExecuteActionsStep executeStep = new ExecuteActionsStep();

            contextState.pushStep(executeStep);
            contextState.pushStep(llmStep);
            contextState.pushStep(captureStep);
        };
    }
}
