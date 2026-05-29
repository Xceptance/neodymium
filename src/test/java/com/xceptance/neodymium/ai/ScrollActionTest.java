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
package com.xceptance.neodymium.ai;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import com.codeborne.selenide.Condition;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Validates the behavior of the ScrollAction AI plugin.
 */
public final class ScrollActionTest extends BaseAiTest
{
    /**
     * Verifies scrolling page or containers to reveal and click off-screen elements.
     */
    @NeodymiumTest
    public void testScrollHappyPath()
    {
        open(currentTestUrl);

        // Scroll down the page to bring the button into view
        Neodymium.ai().execute("Scroll down the page.");

        // Click the revealed button at the bottom
        Neodymium.ai().execute("Click on the 'Bottom Button' button.");

        // Verify the action worked by checking the result
        $("#result").shouldHave(Condition.exactText("Scrolled and clicked bottom button!"));
    }
}
