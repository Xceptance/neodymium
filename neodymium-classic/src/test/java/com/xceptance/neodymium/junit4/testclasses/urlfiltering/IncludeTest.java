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
public class IncludeTest extends NeodymiumTest
{
    @Test
    public void testIncludedUrlIsAllowed()
    {
        Selenide.open("https://www.google.com/");
    }

    @Test
    public void testLaterIncludedUrlIsAllowed()
    {
        Selenide.open("https://github.com");
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
    public void testIncludedRegexIsAllowed()
    {
        Selenide.open("https://www.xceptance.com/en/contact/");

        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://www.xceptance.com/en/");
        });
        assertEquals("Opened Link was outside permitted URLs: https://www.xceptance.com/en/ ==> expected: <true> but was: <false>",
                     assertionError.getMessage());
    }
}
