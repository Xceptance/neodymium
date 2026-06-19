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
package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Action plugin that implements conditional branching (if-then-else) for AI playbook execution.
 */
public final class BranchAction implements AiActionPlugin
{
    /**
     * The action name identifier.
     */
    public static final String ACTION_NAME = "BRANCH";

    @Override
    public String getActionName()
    {
        return ACTION_NAME;
    }

    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        // We do not directly parse branch logic from a regex string.
        // The LLM must output the structured JSON.
        return null;
    }

    @Override
    public boolean requiresLlm(final Action action)
    {
        return false;
    }
    
    private static final ThreadLocal<Boolean> lastConditionResult = new ThreadLocal<>();

    /**
     * Gets the last condition execution result of a BranchAction on this thread.
     *
     * @return the last condition evaluation result, or null if none
     */
    public static Boolean getLastConditionResult()
    {
        return lastConditionResult.get();
    }

    /**
     * Clears the last condition execution result on this thread.
     */
    public static void clearLastConditionResult()
    {
        lastConditionResult.remove();
    }

    @Override
    public String getPromptInstructions()
    {
        return "BRANCH: Perform conditional branching. Requires 'condition' (an array of actions to test, usually ASSERT), 'then' (an array of actions to execute if the condition is met), and optionally 'else' (an array of actions to execute if the condition fails).";
    }

    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor) throws ActionExecutionException
    {
        final Playbook playbook = Neodymium.getAiPlaybook();
        final int cursorBefore = playbook != null ? playbook.getCursor() : -1;

        if (playbook != null)
        {
            if (playbook.isRecording())
            {
                final PlaybookStep currentStep = playbook.getCurrentStep();
                if (currentStep != null)
                {
                    currentStep.setActions(List.of(action));
                    playbook.setChanged(true);
                }
            }
            playbook.nextStep();
        }

        try
        {
            if (action.getCondition() != null && !action.getCondition().isEmpty())
            {
                for (final Action condAction : action.getCondition())
                {
                    condAction.setSilent(true);
                }
                
                boolean conditionMetValue = true;
                try
                {
                    executor.executeAll(action.getCondition());
                }
                catch (final Exception | AssertionError e)
                {
                    conditionMetValue = false;
                }

                final boolean conditionMet = conditionMetValue;
                lastConditionResult.set(conditionMet);

                if (conditionMet)
                {
                    if (action.getThen() != null)
                    {
                        executor.executeAll(action.getThen());
                    }
                }
                else
                {
                    if (action.getElseActions() != null)
                    {
                        executor.executeAll(action.getElseActions());
                    }
                }
            }
        }
        catch (final Throwable t)
        {
            if (playbook != null)
            {
                playbook.setCursor(cursorBefore);
            }
            throw t;
        }
    }
}

