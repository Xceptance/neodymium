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
  *
 * // AI-generated: Gemini 2.0 Flash
*/
package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.util.SelenideAddons;

public class AssertAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(AssertAction.class);

    public static final String ACTION_NAME = "ASSERT";

    @Override
    public String getActionName() { return ACTION_NAME; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { 
        return "ASSERT: Verify element or page state. Requires \"target\".\n" +
               "  For elements: Provide \"target\" (locator string, prefer id attribute over `data-neodymium-automation-id`, over CSS selector, XPath, or text label). Optional \"value\" for text content check.\n" +
               "    If \"value\" is provided, assert that the element's text contains the value.\n" +
               "    If trying to check if an element is visible use \"visible\" as value.\n" +
               "    If asked to verify a text, choose an element, that contains this text.\n" +
               "    If \"value\" is null, assert that the element exists and is visible.\n" +
               "  For URL: Provide \"url\" or \"currentUrl\" as \"target\", and the expected URL as \"value\"."; 
    }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        final String expected = action.getValue();
        if ("url".equalsIgnoreCase(action.getTarget()) || "currentUrl".equalsIgnoreCase(action.getTarget()) || "pageUrl".equalsIgnoreCase(action.getTarget())) {
            if (expected == null) {
                throw new ActionExecutor.ActionExecutionException("URL assertion requires a 'value' (the expected URL)");
            }
            try {
                Selenide.Wait().until(d -> d.getCurrentUrl() != null && d.getCurrentUrl().contains(expected));
                LOG.debug("   ✅ URL Assertion passed for: '{}'", expected);
            } catch (org.openqa.selenium.TimeoutException e) {
                String actualUrl = com.codeborne.selenide.WebDriverRunner.url();
                SelenideAddons.wrapAssertionError(() -> {
                    throw new AssertionError(String.format("Assertion failed: Expected URL to contain '%s' but was '%s'", expected, actualUrl), e);
                });
            }
            return;
        }

        final SelenideElement element = executor.findElement(action);

        if (expected == null) {
            element.should(Condition.exist);
            LOG.debug("   ✅ Element exists: {}", action);
            return;
        }

        try {
            if ("visible".equals(expected)) {
                element.shouldBe(Condition.visible);
            } else {
                element.should(Condition.or("Assertion for " + expected,
                        Condition.exactText(expected),
                        Condition.partialText(expected),
                        Condition.value(expected),
                        new ActionExecutor.PartialTextContent(expected),
                        Condition.attribute("href", expected),
                        Condition.attribute("alt", expected),
                        Condition.attribute("src", expected),
                        Condition.attribute("title", expected),
                        Condition.attribute("placeholder", expected),
                        new ActionExecutor.DataAttributeMatches("data-.*", ".*" + Pattern.quote(expected) + ".*")));
            }
            LOG.debug("   ✅ Assertion passed for: '{}'", expected);
        } catch (Throwable e) {
            String actualDetails = String.format("Text: '%s', Value: '%s', Alt: '%s'", element.getText(), element.getValue(), element.getAttribute("alt"));
            SelenideAddons.wrapAssertionError(() -> {
                throw new AssertionError(String.format("Assertion failed: '%s' not found in common attributes. Found: [%s]", expected, actualDetails), e);
            });
        }
    }
}
