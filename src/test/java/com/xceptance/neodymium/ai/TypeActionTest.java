/*
 * MIT License
 * 
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * // AI-generated: Antigravity (Gemini 3.5 Flash)
 */
package com.xceptance.neodymium.ai;

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
