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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.ai.util.AiAssertions;

/**
 * Plugin action that captures text from an element and stores it as a variable
 * in the execution context for later use.
 */
public final class StoreAction implements AiActionPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(StoreAction.class);

    public static final String ACTION_NAME = "STORE";

    @Override
    public String getActionName()
    {
        return ACTION_NAME;
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
        return "STORE: Capture text from an element or store a literal/visually-identified value to use later. If capturing from an element, requires \"target\" and a single \"value\" (the variable name). If storing a literal/visually-identified value (e.g. from a visual inspection of a non-text element), requires \"value\" as a JSON array of two strings: [\"variableName\", \"literalValue\"] (and \"target\" can be omitted/null). The stored value can be used later as ${variableName}. If the stored value is a number or price for subsequent calculations, set \"adjust\": true.";
    }

    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        final String variableName = action.getValue();
        if (variableName == null || variableName.isBlank())
        {
            throw new ActionExecutionException("STORE action requires a 'value' indicating the variable name to store the captured text.");
        }

        final List<String> values = action.getValues();
        final String valueToStore;

        if (values != null && values.size() >= 2)
        {
            final String literalValue = values.get(1);
            if (action.getAdjust())
            {
                valueToStore = AiAssertions.normalizeNumericOrPrice(literalValue);
            }
            else
            {
                valueToStore = literalValue;
            }
            LOG.debug("   ✅ STORED literal variable '{}' with value '{}'", variableName, valueToStore);
        }
        else
        {
            final SelenideElement element = executor.findElement(action);
            final String text = element.getText();
            
            if (text != null)
            {
                final String trimmedText = text.trim();
                if (action.getAdjust())
                {
                    valueToStore = AiAssertions.normalizeNumericOrPrice(trimmedText);
                }
                else
                {
                    valueToStore = trimmedText;
                }
                LOG.debug("   ✅ STORED element variable '{}' with value '{}'", variableName, valueToStore);
            }
            else
            {
                throw new ActionExecutionException("STORE action failed: element text is null");
            }
        }

        executor.setVariable(variableName, valueToStore);
    }
}
