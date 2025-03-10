package com.xceptance.neodymium.junit5.testclasses.urlfiltering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;

@Browser("Chrome_1024x768")
public class IncludeOverExcludeTest extends AbstractNeodymiumTest
{
    @NeodymiumTest
    public void testIncludedUrlIsAllowed()
    {
        Selenide.open("https://www.google.com/");
    }

    @NeodymiumTest
    public void testNotIncludedUrlIsForbidden()
    {
        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            Selenide.open("https://www.xceptance.com/en/");
        });
        assertEquals("Opened Link was outside permitted URLs: https://www.xceptance.com/en/ ==> expected: <true> but was: <false>",
                     assertionError.getMessage());
    }

    @NeodymiumTest
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
