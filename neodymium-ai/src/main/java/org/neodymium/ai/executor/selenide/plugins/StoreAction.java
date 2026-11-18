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
package org.neodymium.ai.executor.selenide.plugins;

import java.util.List;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import com.codeborne.selenide.SelenideElement;
import org.neodymium.ai.util.AiAssertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action plugin that captures text from an element and stores it as a variable
 * in the execution context for later use.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class StoreAction implements BrowserActionPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(StoreAction.class);

    private final ExecutionContext context;

    /**
     * Constructs a StoreAction with the given execution context.
     *
     * @param context the execution context
     */
    public StoreAction(final ExecutionContext context)
    {
        this.context = context;
    }

    /**
     * Captures and stores element text or literal values.
     *
     * @param action the store action
     * @throws Exception if execution fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null)
        {
            return;
        }

        final String variableName = action.getValue();
        if (variableName == null || variableName.isBlank())
        {
            throw new IllegalArgumentException("STORE action requires a 'value' indicating the variable name to store the captured text.");
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
            final SelenideElement element = SelenideElementFinder.findElement(action);
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
                valueToStore = "";
                LOG.debug("   ✅ STORED element variable '{}' with empty value (element text was null)", variableName);
            }
        }

        if (this.context != null && this.context.getSessionData() != null)
        {
            final boolean isSensitive = variableName.toLowerCase().contains("password") || variableName.toLowerCase().contains("secret");
            this.context.getSessionData().putDynamic(variableName, valueToStore, isSensitive);
        }
    }
}
