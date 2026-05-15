/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
  *
 * // AI-generated: Gemini 2.0 Flash
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
