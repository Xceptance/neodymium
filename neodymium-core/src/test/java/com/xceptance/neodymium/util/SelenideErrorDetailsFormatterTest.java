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
package com.xceptance.neodymium.util;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import java.time.Duration;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import com.codeborne.selenide.CollectionCondition;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.retry.Retry;
import org.neodymium.junit4.NeodymiumRunner;

@Retry(exceptions =
{
  "SessionNotCreated Could not start a new session"
})
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class SelenideErrorDetailsFormatterTest
{
    @Test
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
                                    + "(Timeout: 0ms)", e.getMessage(), "Wrong exception Message catched!");
        }
    }

    @Test
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
            assertExpectedMessageContains(e.getMessage(), "Timeout: 0ms\n");
            assertExpectedMessageContains(e.getMessage(),
                                          "Caused by: NoSuchElementException: no such element: Unable to locate element: {\"method\":\"css selector\",\"selector\":\"#someUnknownIDForThisSepcificTest\"}");
        }
    }

    @Test
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
                                    + "(Timeout: 0ms)", e.getMessage(), "Wrong exception Message catched!");
        }
    }

    @Test
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
            assertExpectedMessageContains(e.getMessage(), "Timeout: 0ms");
        }
    }

    private void assertExpectedMessageContains(String errorMessage, String expectedContent)
    {
        Assertions.assertTrue(errorMessage.contains(expectedContent),
                              "Unexpected exception Message catched: " + errorMessage + "\n, missing content: " + expectedContent);
    }

}
