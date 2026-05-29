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
 * Validates the behavior of the SelectAction AI plugin.
 */
public final class SelectActionTest extends BaseAiTest
{
    /**
     * Verifies selecting dropdown options.
     */
    @NeodymiumTest
    public void testSelectHappyPath()
    {
        open(currentTestUrl);

        // Select the option from the dropdown menu
        Neodymium.ai().execute("Select 'Germany' from the Country dropdown.");

        // Click submit
        Neodymium.ai().execute("Click on the 'Submit Country' button.");

        // Verify the selection was made successfully
        $("#result").shouldHave(Condition.exactText("Selected Country: Germany"));
    }
}
