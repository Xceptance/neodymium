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
import com.xceptance.neodymium.ai.action.AiActionPlugin;

/**
 * Action plugin that signals a dynamic step split during execution.
 * Conceptually, it represents a boundary where a compound instruction has been split.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SplitAction implements AiActionPlugin
{
    /**
     * The unique identifier name for this action type.
     */
    public static final String ACTION_NAME = "SPLIT";

    /**
     * Gets the unique name of this action plugin.
     *
     * @return the action name
     */
    @Override
    public String getActionName()
    {
        return ACTION_NAME;
    }

    /**
     * Parses a direct string instruction (not supported by this plugin).
     *
     * @param instruction the raw instruction text
     * @return a list of Actions or null if not supported
     */
    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        return null;
    }

    /**
     * Checks if this action requires LLM processing to execute.
     *
     * @param action the action to evaluate
     * @return true if LLM is needed, false otherwise
     */
    @Override
    public boolean requiresLlm(final Action action)
    {
        return false;
    }

    /**
     * Gets the prompt instructions describing how the AI should format this action.
     *
     * @return the prompt instruction string
     */
    @Override
    public String getPromptInstructions()
    {
        return "SPLIT: Split a compound, multi-stage instruction into two sequential steps when the second part cannot be reliably executed or identified yet (v=remaining instruction text).";
    }

    /**
     * Executes the split action. Since splitting is handled as a structural change
     * in the agent loop, this method is a no-op.
     *
     * @param action       the AI-generated action definition
     * @param testInstance the test class instance
     * @param executor     the underlying executor instance
     */
    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        // No-op: handled in agent execution loops
    }
}
