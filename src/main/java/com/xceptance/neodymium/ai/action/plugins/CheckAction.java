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
