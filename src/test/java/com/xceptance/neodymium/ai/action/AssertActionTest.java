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

import static com.codeborne.selenide.Selenide.open;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Validates the behavior of the AssertAction AI plugin.
 */
public final class AssertActionTest extends BaseAiTest
{
    /**
     * Verifies positive assertions of text, title, visibility, and invisibility.
     */
    @NeodymiumTest
    public void testAssertHappyPath()
    {
        open(currentTestUrl);

        // Verify page title
        Neodymium.ai().execute("Assert that the page title is 'Assert Action Test'.");

        // Verify element text
        Neodymium.ai().execute("Verify that the welcome message is 'Welcome to our web store!'.");

        // Verify element visibility
        Neodymium.ai().execute("Assert that the 'Clickable Button' button is visible.");

        // Verify element invisibility
        Neodymium.ai().execute("Verify that the 'Secret Button' button is hidden.");
    }
}
