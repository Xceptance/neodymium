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

public class TypeAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "TYPE"; }

    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        final String normalized = instruction.replaceAll("\\s+", " ").trim();
        final java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
            "^TYPE\\s+(?:\"([^\"]*)\"|([^\"]+?))\\s+(?i)into\\s+(.+)$").matcher(normalized);
        if (matcher.matches())
        {
            final String rawValue = matcher.group(1) != null ? matcher.group(1) : matcher.group(2).trim();
            final String target = matcher.group(3).trim();
            if (target.isEmpty())
            {
                throw new IllegalArgumentException("Selector target for TYPE command cannot be empty");
            }
            final SelectorParser.ParsedSelector parsed = SelectorParser.parse(target);
            return List.of(new Action("TYPE", parsed.getExpression(), List.of(rawValue), "Type '" + rawValue + "' into " + parsed.getExpression()));
        }
        return null;
    }

    @Override
    public void preCheck(final Action action, final ActionExecutor executor) {
        try {
            executor.findElement(action).assertCondition(ElementCondition.visible());
        } catch (final Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element not found or not visible for target '%s'", action.getTarget()), t);
        }
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions()
    {
        return Neodymium.interaction().prompts().getTypePromptInstructions();
    }

    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor) {
        try {
            final FoundElement element = executor.findElement(action);
            action.setElementContext(executor.extractElementContext(element));
            executor.scrollIntoView(element);
            element.clear();
            element.sendKeys(action.getValue());
        } catch (final ActionExecutor.ActionExecutionException e) {
            throw e;
        } catch (final Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Failed to type into target '%s'", action.getTarget()), t);
        }
    }
}
