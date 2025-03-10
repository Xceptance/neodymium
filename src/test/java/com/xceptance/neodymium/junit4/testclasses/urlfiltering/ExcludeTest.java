package com.xceptance.neodymium.junit4.testclasses.urlfiltering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
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
