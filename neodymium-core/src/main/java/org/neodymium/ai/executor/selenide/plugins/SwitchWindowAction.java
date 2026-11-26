/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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

import com.codeborne.selenide.WebDriverRunner;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.openqa.selenium.WebDriver;

/**
 * Concrete action plugin executing SWITCH_WINDOW commands.
 * Switches browser/WebDriver context focus to other tabs or windows by index, title, or newest handle.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SwitchWindowAction implements BrowserActionPlugin
{
    /**
     * Constructs a SwitchWindowAction.
     */
    public SwitchWindowAction()
    {
    }

    /**
     * Switches the WebDriver window focus.
     *
     * @param action the switch window action
     * @throws Exception if focus switching fails or index/title is not found
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null)
        {
            return;
        }

        final WebDriver driver = WebDriverRunner.getWebDriver();
        final String target = action.getTarget();
        final String value = action.getValue();
        final String param = (target != null && !target.trim().isEmpty()) ? target.trim() :
                             (value != null && !value.trim().isEmpty()) ? value.trim() : null;

        BrowserToolProvider.switchWindow(driver, param);
    }
}

