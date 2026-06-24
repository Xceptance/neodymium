/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.ai.action;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import com.codeborne.selenide.Condition;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Validates the behavior of the SwitchWindowAction AI plugin.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
public final class SwitchWindowActionTest extends BaseAiTest
{
    @NeodymiumTest
    public final void testSwitchWindow()
    {
        System.setProperty("neodymium.ai.offline", "true");
        try
        {
            open(currentTestUrl);

            // Click button to open new window
            $("#open-window").click();

            // Switch to the new window using AI (auto-select newest window)
            Neodymium.ai().execute("Switch to the new window");

            // Verify we are in the new window
            $("#welcome").shouldHave(Condition.exactText("Welcome to the new window!"));

            // Switch back using index
            Neodymium.ai().execute("Switch to window 0");

            // Verify we are back
            $("#open-window").should(Condition.exist);

            // Open the window again
            $("#open-window").click();

            // Switch to the new window by index prefix
            Neodymium.ai().execute("Switch to window win_1");

            // Verify we are in the new window
            $("#welcome").should(Condition.exist);

            // Switch back using title
            Neodymium.ai().execute("Switch to window 'Original Window'");

            // Verify we are back
            $("#open-window").should(Condition.exist);
        }
        finally
        {
            System.clearProperty("neodymium.ai.offline");
        }
    }
}
