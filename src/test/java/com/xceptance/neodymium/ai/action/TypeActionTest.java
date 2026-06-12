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
 * Validates the behavior of the TypeAction AI plugin, testing both inputs and textareas explicitly.
 */
public final class TypeActionTest extends BaseAiTest
{
    /**
     * Verifies typing text into both input text fields and textarea fields.
     */
    @NeodymiumTest
    public void testTypeHappyPath()
    {
        open(currentTestUrl);

        // Type into the standard input text field
        Neodymium.ai().execute("Type 'John' into the first name field.");

        // Type into the textarea field
        Neodymium.ai().execute("Type 'This is a test comment.' into the comments field.");

        // Click the submit button
        Neodymium.ai().execute("Click on the 'Submit Details' button.");

        // Verify the values were combined and submitted successfully
        $("#result").shouldHave(Condition.exactText("Submitted: John - This is a test comment."));
    }
}
