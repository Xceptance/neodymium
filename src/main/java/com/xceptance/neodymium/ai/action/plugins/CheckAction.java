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
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

/**
 * AI action plugin to check/select checkboxes and radio buttons.
 * 
 * // AI-generated: Antigravity (Gemini 3.5 Flash)
 */
public class CheckAction implements AiActionPlugin
{
    @Override
    public String getActionName()
    {
        return "CHECK";
    }

    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
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
        return "CHECK: Check if an element contains expected text or is in an expected state.";
    }

    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        if (action.getTarget() != null && !action.getTarget().isBlank())
        {
            final SelenideElement element = executor.findElement(action);
            if (!element.isSelected())
            {
                element.click();
            }
        }
    }
}
