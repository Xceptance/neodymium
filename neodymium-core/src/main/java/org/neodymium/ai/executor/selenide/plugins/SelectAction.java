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

import com.codeborne.selenide.SelenideElement;
import java.util.List;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Concrete action plugin executing SELECT dropdown option commands.
 * Supports selection by visible option text, numerical index, value attribute,
 * and resilient fuzzy/prefix matching.
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
            final String value = action.getValue();
            final SelenideElement element = SelenideElementFinder.findElement(action);
            try
            {
                final int index = Integer.parseInt(value);
                element.selectOption(index);
            }
            catch (final NumberFormatException e)
            {
                final boolean selected = selectOptionFuzzy(element, value);
                if (!selected)
                {
                    element.selectOption(value);
                }
            }
        }
    }

    private static boolean selectOptionFuzzy(final SelenideElement selectElement, final String targetText)
    {
        if (targetText == null || targetText.isBlank())
        {
            return false;
        }

        final Select select = new Select(selectElement.getWrappedElement());
        final List<WebElement> options = select.getOptions();
        final String normalizedTarget = targetText.trim().toLowerCase();

        // Pass 1: exact trimmed case-insensitive text match
        for (final WebElement opt : options)
        {
            final String optText = opt.getText();
            if (optText != null && optText.trim().equalsIgnoreCase(targetText.trim()))
            {
                select.selectByVisibleText(optText);
                return true;
            }
        }

        // Pass 2: prefix match (e.g. "Canada (EN)" matches "Canada (EN) (CAD $)")
        for (final WebElement opt : options)
        {
            final String optText = opt.getText();
            if (optText != null && optText.trim().toLowerCase().startsWith(normalizedTarget))
            {
                select.selectByVisibleText(optText);
                return true;
            }
        }

        // Pass 3: contains match
        for (final WebElement opt : options)
        {
            final String optText = opt.getText();
            if (optText != null && optText.toLowerCase().contains(normalizedTarget))
            {
                select.selectByVisibleText(optText);
                return true;
            }
        }

        // Pass 4: value attribute case-insensitive / prefix match
        for (final WebElement opt : options)
        {
            final String optVal = opt.getAttribute("value");
            if (optVal != null && (optVal.trim().equalsIgnoreCase(targetText.trim()) || optVal.toLowerCase().startsWith(normalizedTarget)))
            {
                select.selectByValue(optVal);
                return true;
            }
        }

        return false;
    }
}

