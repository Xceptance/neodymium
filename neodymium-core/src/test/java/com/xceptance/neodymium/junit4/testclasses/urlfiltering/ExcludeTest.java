/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit4.testclasses.urlfiltering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class ExcludeTest extends NeodymiumTest
{
    @Test
    public void testFirstUrlIsForbidden()
    {
        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://www.google.com/");
        });
        assertEquals("Opened Link was to forbidden site: https://www.google.com/ ==> expected: <true> but was: <false>", assertionError.getMessage());

    }

    @Test
    public void testSecondUrlIsForbidden()
    {
        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://github.com/");
        });
        assertEquals("Opened Link was to forbidden site: https://github.com/ ==> expected: <true> but was: <false>", assertionError.getMessage());
    }

    @Test
    public void testNotExcludedUrlIsAllowed()
    {
        Selenide.open("https://www.xceptance.com/en/");
    }

    @Test
    public void testSubDomainIsForbidden()
    {
        Selenide.open("https://www.xceptance.com/en/");
        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://www.xceptance.com/en/news/");
        });
        assertEquals("Opened Link was to forbidden site: https://www.xceptance.com/en/news/ ==> expected: <true> but was: <false>",
                     assertionError.getMessage());
    }

    @Test
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
