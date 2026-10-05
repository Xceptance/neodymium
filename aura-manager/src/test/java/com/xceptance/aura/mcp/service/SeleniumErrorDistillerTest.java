package com.xceptance.aura.mcp.service;

import com.xceptance.aura.mcp.dto.DistilledFailureDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for SeleniumErrorDistiller stack trace pruning, locator extraction, and token budget enforcement.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class SeleniumErrorDistillerTest
{
    private SeleniumErrorDistiller distiller;

    @BeforeEach
    public void setUp()
    {
        distiller = new SeleniumErrorDistiller();
    }

    @Test
    public void testDistillNoSuchElementException()
    {
        final String rawError = """
            org.openqa.selenium.NoSuchElementException: no such element: Unable to locate element: {"method":"css selector","selector":"button#submit-order"}
              (Session info: chrome=122.0.6261.94)
            For documentation on this error, please visit: https://www.selenium.dev/documentation/webdriver/troubleshooting/errors#no-such-element-exception
            Build info: version: '4.18.1'
            at org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec.createException(W3CHttpResponseCodec.java:200)
            at org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec.decode(W3CHttpResponseCodec.java:133)
            at org.openqa.selenium.remote.HttpCommandExecutor.execute(HttpCommandExecutor.java:191)
            at com.xceptance.neodymium.util.Neodymium.find(Neodymium.java:85)
            at com.xceptance.neodymium.aura.AuraExecutor.executeStep(AuraExecutor.java:142)
            at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
            at java.base/java.lang.reflect.Method.invoke(Method.java:580)
            """;

        final DistilledFailureDto result = distiller.distill("OrderTest", rawError, List.of());

        Assertions.assertNotNull(result);
        Assertions.assertEquals("ELEMENT_NOT_FOUND: button#submit-order", result.getClusterCategory());
        Assertions.assertEquals("button#submit-order", result.getLocator());
        Assertions.assertTrue(result.getCleanedStackTrace().contains("Neodymium.find"));
        Assertions.assertFalse(result.getCleanedStackTrace().contains("DirectMethodHandleAccessor"));
        Assertions.assertFalse(result.getCleanedStackTrace().contains("W3CHttpResponseCodec"));
        Assertions.assertTrue(result.getCleanedStackTrace().length() <= 1500);
    }

    @Test
    public void testDistillTimeoutException()
    {
        final String rawError = """
            org.openqa.selenium.TimeoutException: Expected condition failed: waiting for visibility of element located by By.xpath: //input[@name='q'] (tried for 10 second(s))
            at org.openqa.selenium.support.ui.WebDriverWait.timeoutException(WebDriverWait.java:84)
            at org.openqa.selenium.support.ui.FluentWait.until(FluentWait.java:228)
            at com.xceptance.neodymium.aura.AuraExecutor.run(AuraExecutor.java:99)
            """;

        final DistilledFailureDto result = distiller.distill("SearchTimeoutTest", rawError, List.of());

        Assertions.assertNotNull(result);
        Assertions.assertEquals("TIMEOUT: //input[@name='q']", result.getClusterCategory());
        Assertions.assertEquals("//input[@name='q']", result.getLocator());
    }

    @Test
    public void testTokenBudgetEnforcedOnHugeStackTrace()
    {
        final StringBuilder hugeTrace = new StringBuilder("org.openqa.selenium.WebDriverException: Error\n");
        for (int i = 0; i < 200; i++)
        {
            hugeTrace.append("at com.xceptance.neodymium.StepRunner.step").append(i).append("(StepRunner.java:").append(i).append(")\n");
        }

        final DistilledFailureDto result = distiller.distill("LargeTraceTest", hugeTrace.toString(), List.of());
        Assertions.assertTrue(result.getCleanedStackTrace().length() <= 1500, "Must be capped at 1,500 chars");
        Assertions.assertTrue(result.getCleanedStackTrace().endsWith("... [TRUNCATED]"));
    }
}
