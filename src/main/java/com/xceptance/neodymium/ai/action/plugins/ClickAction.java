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
import com.xceptance.neodymium.util.layer.ElementCondition;
import com.xceptance.neodymium.util.layer.FoundElement;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.ai.action.SelectorParser;
import com.xceptance.neodymium.util.Neodymium;

public class ClickAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "CLICK"; }

    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        final String normalized = instruction.replaceAll("\\s+", " ").trim();
        if (normalized.startsWith("CLICK "))
        {
            final String target = normalized.substring(6).trim();
            if (target.isEmpty())
            {
                throw new IllegalArgumentException("Selector target for CLICK command cannot be empty");
            }
            final SelectorParser.ParsedSelector parsed = SelectorParser.parse(target);
            return List.of(new Action("CLICK", parsed.getExpression(), "Click " + parsed.getExpression()));
        }
        return null;
    }

    @Override
    public void preCheck(final Action action, final ActionExecutor executor) {
        try {
            final FoundElement element = executor.findElement(action);
            // Modern web frameworks tend to hide the actual input field and show a custom box.
            if ("input".equalsIgnoreCase(element.getTagName()) &&
                ("checkbox".equalsIgnoreCase(element.getAttribute("type")) || "radio".equalsIgnoreCase(element.getAttribute("type"))) &&
                !element.isDisplayed()) {
                element.assertCondition(ElementCondition.exist());
            } else {
                element.assertCondition(ElementCondition.visible());
            }
        } catch (final Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element not found or not visible for target '%s'", action.getTarget()), t);
        }
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions()
    {
        return Neodymium.interaction().prompts().getClickPromptInstructions();
    }

    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor) {
        try {
            final FoundElement element = executor.findElement(action);
            action.setElementContext(executor.extractElementContext(element));
            executor.scrollIntoView(element);
            // FoundElement.click() handles hidden checkboxes/radio buttons natively
            // in both Selenide (setSelected/click) and Playwright (check/click) backends.
            element.click();
        } catch (final ActionExecutor.ActionExecutionException e) {
            throw e;
        } catch (final Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Failed to execute action '%s'", action.getTarget()), t);
        }
    }
}
