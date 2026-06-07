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
import com.xceptance.neodymium.ai.BaseAiTest;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import com.codeborne.selenide.Condition;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Validates the behavior of the CheckAction AI plugin, testing both checkboxes and radio buttons.
 */
public final class CheckActionTest extends BaseAiTest
{
    /**
     * Verifies checking and selecting both checkbox and radio button elements.
     */
    @NeodymiumTest
    public void testCheckHappyPath()
    {
        open(currentTestUrl);

        // Check the unchecked checkbox element
        Neodymium.ai().execute("Check the 'Subscribe to newsletter' checkbox.");

        // Check/Select the contact method radio button
        Neodymium.ai().execute("Check the 'Email' radio button.");

        // Click submit
        Neodymium.ai().execute("Click on the 'Submit Preferences' button.");

        // Verify the states were captured and submitted successfully
        $("#result").shouldHave(Condition.exactText("Newsletter: true, Contact: email"));
    }
}
