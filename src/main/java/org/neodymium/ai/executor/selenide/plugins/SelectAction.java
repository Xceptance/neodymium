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

import org.neodymium.ai.action.Action;
import com.codeborne.selenide.Selenide;

/**
 * Concrete action plugin executing SELECT dropdown option commands.
 * Supports selection by visible option text or numerical index.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SelectAction implements BrowserActionPlugin
{
    /**
     * Constructs a SelectAction.
     */
    public SelectAction()
    {
    }

    /**
     * Selects an option in dropdown element matching the target selector.
     *
     * @param action the select action
     * @throws Exception if selection fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action != null && action.getTarget() != null && action.getValue() != null)
        {
            final String target = action.getTarget();
            final String value = action.getValue();
            try
            {
                final int index = Integer.parseInt(value);
                Selenide.$(target).selectOption(index);
            }
            catch (final NumberFormatException e)
            {
                Selenide.$(target).selectOption(value);
            }
        }
    }
}
