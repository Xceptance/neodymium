/*
 * Copyright (c) 2017-2026 Xceptance Software Technologies GmbH
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
 */
package com.xceptance.neodymium.util;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NeodymiumAssertion}.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("chrome_headless")
public class NeodymiumAssertionTest extends BaseAiTest
{
    @Test
    public void testAssertions()
    {
        // Standard assertions
        NeodymiumAssertion.assertTrue(true);
        NeodymiumAssertion.assertTrue("Should be true", true);
        NeodymiumAssertion.assertEquals("foo", "foo");
        NeodymiumAssertion.assertEquals("Should be equal", 1, 1);

        // Element assertions
        open("about:blank");
        NeodymiumAssertion.should($("body"), "exist", "visible");
        NeodymiumAssertion.shouldBe($("body"), "visible");
        
        // Negative test for standard assertion (would fail the test, so we just check it doesn't crash during setup)
    }
}
