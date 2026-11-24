/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.junit4.testclasses.urlfiltering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class IncludeOverExcludeTest extends NeodymiumTest
{
    @Test
    public void testIncludedUrlIsAllowed()
    {
        Selenide.open("https://www.google.com/");
    }

    @Test
    public void testNotIncludedUrlIsForbidden()
    {
        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://www.xceptance.com/en/");
        });
        assertEquals("Opened Link was outside permitted URLs: https://www.xceptance.com/en/ ==> expected: <true> but was: <false>",
                     assertionError.getMessage());
    }

    @Test
    public void testExcludedSubDomainIsForbidden()
    {
        Selenide.open("https://github.com/");

        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://github.com/Xceptance/neodymium");
        });
        assertEquals("Opened Link was to forbidden site: https://github.com/Xceptance/neodymium ==> expected: <true> but was: <false>",
                     assertionError.getMessage());
    }
}
