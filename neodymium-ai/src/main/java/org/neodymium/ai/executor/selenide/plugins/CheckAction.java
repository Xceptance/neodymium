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
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import com.codeborne.selenide.SelenideElement;

/**
 * Concrete action plugin to check/select checkboxes and radio buttons.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class CheckAction implements BrowserActionPlugin
{
    /**
     * Constructs a CheckAction.
     */
    public CheckAction()
    {
    }

    /**
     * Checks or selects the target checkbox or radio button if not already selected.
     *
     * @param action the check action
     * @throws Exception if execution fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action != null && action.getTarget() != null && !action.getTarget().isBlank())
        {
            final SelenideElement element = SelenideElementFinder.findElement(action);
            if (!element.isSelected())
            {
                element.click();
            }
        }
    }
}
