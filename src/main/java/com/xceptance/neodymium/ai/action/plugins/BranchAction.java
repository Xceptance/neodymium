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

public class BranchAction implements AiActionPlugin
{
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

    @Override
    public String getPromptInstructions()
    {
        return "BRANCH: Used for conditional logic. Requires 'condition' (array of actions to test, usually ASSERT), 'then' (array of actions to execute if condition is met), and optionally 'else' (array of actions to execute if condition fails).";
    }

    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor) throws ActionExecutionException
    {
        if (action.getCondition() != null && !action.getCondition().isEmpty())
        {
            for (Action condAction : action.getCondition())
            {
                condAction.setSilent(true);
            }
            boolean conditionMet = true;
            try
            {
                executor.executeAll(action.getCondition());
            }
            catch (final Exception | AssertionError e)
            {
                conditionMet = false;
            }

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
}
