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
package com.xceptance.neodymium.junit5.testclasses.urlfiltering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;

@Browser("Chrome_1024x768")
public class ExcludeTest extends AbstractNeodymiumTest
{
    @NeodymiumTest
    public void testExcludedUrlIsForbidden()
    {
        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://www.google.com/");
        });
        assertEquals("Opened Link was to forbidden site: https://www.google.com/ ==> expected: <true> but was: <false>", assertionError.getMessage());
    }

    @NeodymiumTest
    public void testLaterExcludedUrlIsForbidden()
    {
        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://github.com/");
        });
        assertEquals("Opened Link was to forbidden site: https://github.com/ ==> expected: <true> but was: <false>", assertionError.getMessage());
    }

    @NeodymiumTest
    public void testNotExcludedUrlIsAllowed()
    {
        Selenide.open("https://www.xceptance.com/en/");
    }

    @NeodymiumTest
    public void testSubDomainIsForbidden()
    {
        Selenide.open("https://www.xceptance.com/en/");
        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://www.xceptance.com/en/news/");
        });
        assertEquals("Opened Link was to forbidden site: https://www.xceptance.com/en/news/ ==> expected: <true> but was: <false>",
                     assertionError.getMessage());
    }

    @NeodymiumTest
    public void testForbiddenRegex()
    {

        AssertionError assertionErrorEn = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://www.xceptance.com/en/contact/");
        });
        assertTrue(assertionErrorEn.getMessage()
                                   .contains("Opened Link was to forbidden site: https://www.xceptance.com/en/contact/"));

        AssertionError assertionErrorDe = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://www.xceptance.com/de/kontakt/");
        });
        assertTrue(assertionErrorDe.getMessage()
                                   .contains("Opened Link was to forbidden site: https://www.xceptance.com/de/kontakt/"));
    }
}
