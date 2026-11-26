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
package com.xceptance.neodymium.util;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import java.io.IOException;
import java.time.Duration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
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
    private static LocalTestHttpServer server;

    @BeforeClass
    public static void startServer() throws IOException
    {
        server = LocalTestHttpServer.startWithHtml("<html><body><h1>Error Details Test</h1></body></html>");
    }

    @AfterClass
    public static void stopServer()
    {
        if (server != null)
        {
            server.close();
        }
    }

    @Test
    public void testErrorDetailesDeactivated()
    {
        Neodymium.configuration().setProperty("neodymium.report.showSelenideErrorDetails", "false");
        Assertions.assertFalse(Neodymium.configuration().showSelenideErrorDetails());

        open(server.url("/"));
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

        open(server.url("/"));
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
