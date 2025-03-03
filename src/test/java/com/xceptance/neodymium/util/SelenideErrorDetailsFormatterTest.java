package com.xceptance.neodymium.util;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.CollectionCondition;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
public class SelenideErrorDetailsFormatterTest
{
    @NeodymiumTest
    public void testErrorDetailesDeactivated()
    {
        Neodymium.configuration().setProperty("neodymium.report.showSelenideErrorDetails", "false");
        Assertions.assertFalse(Neodymium.configuration().showSelenideErrorDetails());

        open("http://www.xceptance.com");
        try
        {
            $("#someUnknownIDForThisSepcificTest").should(exist, Duration.ZERO);
            Assertions.fail("Expected an AssertionError, which was not thrown!");
        }
        catch (AssertionError e)
        {
            Assertions.assertEquals("Element not found {#someUnknownIDForThisSepcificTest}\n"
                                    + "Expected: exist\n"
                                    + "(Timeout: 0 ms.)", e.getMessage(), "Wrong exception Message catched!");
        }
    }

    @NeodymiumTest
    public void testErrorDetailesActivated()
    {
        Neodymium.configuration().setProperty("neodymium.report.showSelenideErrorDetails", "true");
        Assertions.assertTrue(Neodymium.configuration().showSelenideErrorDetails());

        try
        {
            $("#someUnknownIDForThisSepcificTest").should(exist, Duration.ZERO);
            Assertions.fail("Expected an AssertionError, which was not thrown!");
        }
        catch (AssertionError e)
        {
            assertExpectedMessageContains(e.getMessage(), "Element not found {#someUnknownIDForThisSepcificTest}\n");
            assertExpectedMessageContains(e.getMessage(), "Expected: exist\n");
            assertExpectedMessageContains(e.getMessage(), "Screenshot: ");
            assertExpectedMessageContains(e.getMessage(), "Page source: ");
            assertExpectedMessageContains(e.getMessage(), "Timeout: 0 ms.\n");
            assertExpectedMessageContains(e.getMessage(),
                                          "Caused by: NoSuchElementException: no such element: Unable to locate element: {\"method\":\"css selector\",\"selector\":\"#someUnknownIDForThisSepcificTest\"}");
        }
    }

    @NeodymiumTest
    public void testCollectionErrorDetailesDeactivated()
    {
        Neodymium.configuration().setProperty("neodymium.report.showSelenideErrorDetails", "false");
        Assertions.assertFalse(Neodymium.configuration().showSelenideErrorDetails());

        open("http://www.xceptance.com");
        try
        {
            $$("#someUnknownIDForThisSepcificTest").shouldHave(CollectionCondition.sizeGreaterThan(5), Duration.ZERO);
            Assertions.fail("Expected an AssertionError, which was not thrown!");
        }
        catch (AssertionError e)
        {
            Assertions.assertEquals("List size mismatch: expected: > 5, actual: 0, collection: #someUnknownIDForThisSepcificTest\n"
                                    + "(Timeout: 0 ms.)", e.getMessage(), "Wrong exception Message catched!");
        }
    }

    @NeodymiumTest
    public void testCollectionErrorDetailesActivated()
    {
        Neodymium.configuration().setProperty("neodymium.report.showSelenideErrorDetails", "true");
        Neodymium.configuration().setProperty("neodymium.screenshots.enableAdvancedScreenshots", "false");
        Assertions.assertTrue(Neodymium.configuration().showSelenideErrorDetails());

        try
        {
            $$("#someUnknownIDForThisSepcificTest").shouldHave(CollectionCondition.sizeGreaterThan(5), Duration.ZERO);
            Assertions.fail("Expected an AssertionError, which was not thrown!");
        }
        catch (AssertionError e)
        {
            assertExpectedMessageContains(e.getMessage(), "List size mismatch: expected: > 5, actual: 0, collection: #someUnknownIDForThisSepcificTest\n");
            assertExpectedMessageContains(e.getMessage(), "Page source: ");
            assertExpectedMessageContains(e.getMessage(), "Timeout: 0 ms.");
        }
    }

    private void assertExpectedMessageContains(String errorMessage, String expectedContent)
    {
        Assertions.assertTrue(errorMessage.contains(expectedContent),
                              "Unexpected exception Message catched: " + errorMessage + "\n, missing content: " + expectedContent);
    }

}
