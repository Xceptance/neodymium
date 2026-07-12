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
 * Concrete action plugin executing SCROLL browser commands.
 * Supports scrolling target elements into view or scrolling the window context.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ScrollAction implements BrowserActionPlugin
{
    /**
     * Constructs a ScrollAction.
     */
    public ScrollAction()
    {
    }

    /**
     * Scrolls the window or target element matching the selector.
     *
     * @param action the scroll action
     * @throws Exception if scrolling fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null)
        {
            return;
        }

        final String target = action.getTarget();
        if (target == null || target.trim().isEmpty())
        {
            final String value = action.getValue() != null ? action.getValue().toLowerCase().trim() : "";
            if ("bottom".equals(value))
            {
                Selenide.executeJavaScript("window.scrollTo(0, document.body.scrollHeight)");
            }
            else if ("top".equals(value))
            {
                Selenide.executeJavaScript("window.scrollTo(0, 0)");
            }
            else
            {
                Selenide.executeJavaScript("window.scrollBy(0, 500)");
            }
        }
        else
        {
            Selenide.$(target).scrollIntoView(true);
        }
    }
}
