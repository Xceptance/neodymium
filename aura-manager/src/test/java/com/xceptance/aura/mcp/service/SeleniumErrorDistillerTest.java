/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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

    @Test
    public void testExtractDomSnippetVariousTags()
    {
        final String rawErrorSelect = """
            org.openqa.selenium.ElementNotInteractableException: element not interactable
              (Session info: chrome=122.0.6261.94)
            Element: <select name="country" class="form-select" id="country-picker">
            at com.xceptance.neodymium.util.Neodymium.find(Neodymium.java:85)
            """;
        final DistilledFailureDto resultSelect = distiller.distill("SelectTest", rawErrorSelect, List.of());
        Assertions.assertNotNull(resultSelect.getDomSnippet());
        Assertions.assertEquals("<select name=\"country\" class=\"form-select\" id=\"country-picker\">", resultSelect.getDomSnippet());

        final String rawErrorTextarea = """
            org.openqa.selenium.NoSuchElementException: could not find element
            <textarea id="comments" rows="5"></textarea>
            """;
        final DistilledFailureDto resultTextarea = distiller.distill("TextareaTest", rawErrorTextarea, List.of());
        Assertions.assertNotNull(resultTextarea.getDomSnippet());
        Assertions.assertEquals("<textarea id=\"comments\" rows=\"5\">", resultTextarea.getDomSnippet());

        final String rawErrorAnchor = """
            org.openqa.selenium.ElementClickInterceptedException: element click intercepted
            <a href="/login" class="nav-link">
            """;
        final DistilledFailureDto resultAnchor = distiller.distill("AnchorTest", rawErrorAnchor, List.of());
        Assertions.assertNotNull(resultAnchor.getDomSnippet());
        Assertions.assertEquals("<a href=\"/login\" class=\"nav-link\">", resultAnchor.getDomSnippet());

        final String rawErrorAnchorBoolean = """
            org.openqa.selenium.ElementClickInterceptedException: element click intercepted
            <a download>
            """;
        final DistilledFailureDto resultAnchorBoolean = distiller.distill("AnchorBooleanTest", rawErrorAnchorBoolean, List.of());
        Assertions.assertNotNull(resultAnchorBoolean.getDomSnippet());
        Assertions.assertEquals("<a download>", resultAnchorBoolean.getDomSnippet());

        final String rawErrorParagraph = """
            org.openqa.selenium.NoSuchElementException: could not find element
            <p hidden>
            """;
        final DistilledFailureDto resultParagraph = distiller.distill("ParagraphTest", rawErrorParagraph, List.of());
        Assertions.assertNotNull(resultParagraph.getDomSnippet());
        Assertions.assertEquals("<p hidden>", resultParagraph.getDomSnippet());

        final String rawErrorIcon = """
            org.openqa.selenium.ElementClickInterceptedException: element click intercepted
            <i class="fa fa-trash">
            """;
        final DistilledFailureDto resultIcon = distiller.distill("IconTest", rawErrorIcon, List.of());
        Assertions.assertNotNull(resultIcon.getDomSnippet());
        Assertions.assertEquals("<i class=\"fa fa-trash\">", resultIcon.getDomSnippet());

        final String rawErrorCustomElement = """
            org.openqa.selenium.NoSuchElementException: element: <custom-card id="user-123" class="active"> is not attached
            """;
        final DistilledFailureDto resultCustomElement = distiller.distill("CustomElementTest", rawErrorCustomElement, List.of());
        Assertions.assertNotNull(resultCustomElement.getDomSnippet());
        Assertions.assertEquals("<custom-card id=\"user-123\" class=\"active\">", resultCustomElement.getDomSnippet());
    }

    @Test
    public void testIgnoreJavaGenericsAndConstructorsInDomSnippet()
    {
        final String rawErrorJava = """
            java.lang.NoSuchMethodError: java.util.List<String> com.example.Service.getItems()
            at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:502)
            at com.xceptance.neodymium.MyClass.<init>(MyClass.java:24)
            at com.example.GenericService.process(GenericService.java:50)
            Caused by: java.lang.ClassCastException: class java.util.Map<A, B> cannot be cast to List<A>
            at com.example.Handler.<P>handle(Handler.java:12)
            at com.example.TableHandler.process(TableHandler.java:99) [List<TD>, Pair<TR, TH>, Collection<LI>]
            at com.example.BoundedHandler.map(BoundedHandler.java:42) [List<A extends Number>, Map<P extends Serializable, Object>, List<TD super Object>]
            """;
        final DistilledFailureDto result = distiller.distill("GenericsTest", rawErrorJava, List.of());
        Assertions.assertNull(result.getDomSnippet(), "Java generics and constructors should not be parsed as DOM snippets");
    }
}
