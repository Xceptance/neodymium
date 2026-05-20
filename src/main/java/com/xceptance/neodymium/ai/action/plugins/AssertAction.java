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
import java.util.Map;
import java.util.regex.Pattern;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.WebElementCondition;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.ActionExecutor.DataAttributeMatches;
import com.xceptance.neodymium.ai.action.ActionExecutor.PartialTextContent;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.util.SelenideAddons;

/**
 * Plugin action that asserts the presence, visibility, or specific content of a given element,
 * or validates the current URL of the page.
 */
public final class AssertAction implements AiActionPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(AssertAction.class);

    /**
     * The unique identifier name for this action type.
     */
    public static final String ACTION_NAME = "ASSERT";

    /**
     * Gets the unique name of this action plugin.
     *
     * @return the action name
     */
    @Override
    public String getActionName()
    {
        return ACTION_NAME;
    }

    /**
     * Parses a direct string instruction (not supported by this plugin).
     *
     * @param instruction the raw instruction text
     * @return a list of Actions or null if not supported
     */
    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        return null;
    }

    /**
     * Checks if this action requires LLM processing to execute.
     *
     * @param action the action to evaluate
     * @return true if LLM is needed, false otherwise
     */
    @Override
    public boolean requiresLlm(final Action action)
    {
        return false;
    }

    /**
     * Gets the prompt instructions describing how the AI should format this action.
     *
     * @return the prompt instruction string
     */
    @Override
    public String getPromptInstructions()
    {
        return """
               ASSERT: Verify element or page state. Requires "target".
                 For elements: Provide "target" (locator string, prefer id attribute over `data-neo-ref`, over CSS selector, XPath, or text label). Optional "value" for text content check.
                   If "value" is provided, assert that the element's text contains the value.
                   If trying to check if an element is visible use "visible" as value.
                   If asked to verify a text, choose an element, that contains this text.
                   If "value" is null, assert that the element exists and is visible.
                 For URL: Provide "url" or "currentUrl" as "target", and the expected URL as "value".\
               """;
    }

    /**
     * Executes the assertion action. Validates either the current page URL or the state of a specific DOM element.
     *
     * @param action       the AI-generated action definition
     * @param testInstance the test class instance (not directly used)
     * @param executor     the underlying executor instance providing element resolution
     */
    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        final String expected = action.getValue();
        
        // Handle URL assertions
        if ("url".equalsIgnoreCase(action.getTarget()) || "currentUrl".equalsIgnoreCase(action.getTarget()) || "pageUrl".equalsIgnoreCase(action.getTarget()))
        {
            if (expected == null)
            {
                throw new ActionExecutionException("URL assertion requires a 'value' (the expected URL)");
            }
            
            if (action.isSilent())
            {
                final String actualUrl = WebDriverRunner.url();
                if (actualUrl == null || !actualUrl.contains(expected))
                {
                    throw new ActionExecutionException("Silent condition not met: URL does not contain '" + expected + "'");
                }
                LOG.debug("   ✅ Silent URL Assertion passed for: '{}'", expected);
                return;
            }

            try
            {
                Selenide.Wait().until(d -> d.getCurrentUrl() != null && d.getCurrentUrl().contains(expected));
                LOG.debug("   ✅ URL Assertion passed for: '{}'", expected);
            }
            catch (final TimeoutException e)
            {
                // We catch Selenium's TimeoutException to wrap it in a standard AssertionError
                // so the test framework correctly identifies it as a validation failure rather than a framework crash.
                final String actualUrl = WebDriverRunner.url();
                SelenideAddons.wrapAssertionError(() ->
                {
                    throw new AssertionError(String.format("Assertion failed: Expected URL to contain '%s' but was '%s'", expected, actualUrl), e);
                });
            }
            return;
        }

        // Handle Element assertions
        final SelenideElement element = executor.findElement(action);

        if (expected == null)
        {
            if (action.isSilent())
            {
                if (!element.is(Condition.exist))
                {
                    throw new ActionExecutionException("Silent condition not met: Element does not exist");
                }
            }
            else
            {
                element.should(Condition.exist);
            }
            LOG.debug("   ✅ Element exists: {}", action);
            return;
        }

        try
        {
            if ("visible".equals(expected))
            {
                if (action.isSilent())
                {
                    if (!element.is(Condition.visible))
                    {
                        throw new ActionExecutionException("Silent condition not met: Element not visible");
                    }
                }
                else
                {
                    element.shouldBe(Condition.visible);
                }
            }
            else
            {
                final WebElementCondition cond;
                if (isRegexPattern(expected))
                {
                    cond = new RegexMatch(expected);
                }
                else
                {
                    // Create a composite condition that searches for the expected text across 
                    // all common visible attributes, input values, and text content areas.
                    cond = Condition.or("Assertion for " + expected,
                            Condition.exactText(expected),
                            Condition.partialText(expected),
                            Condition.value(expected),
                            new PartialTextContent(expected),
                            Condition.attribute("href", expected),
                            Condition.attribute("alt", expected),
                            Condition.attribute("src", expected),
                            Condition.attribute("title", expected),
                            Condition.attribute("placeholder", expected),
                            new DataAttributeMatches("data-.*", ".*" + Pattern.quote(expected) + ".*"));
                }

                if (action.isSilent())
                {
                    if (!element.is(cond))
                    {
                        throw new ActionExecutionException("Silent condition not met: Element does not match '" + expected + "'");
                    }
                }
                else
                {
                    element.should(cond);
                }
            }
            LOG.debug("   ✅ Assertion passed for: '{}'", expected);
        }
        catch (final ActionExecutionException e)
        {
            throw e;
        }
        catch (final Throwable e)
        {
            final String actualDetails = String.format("Text: '%s', Value: '%s', Alt: '%s'", element.getText(), element.getValue(), element.getAttribute("alt"));
            SelenideAddons.wrapAssertionError(() ->
            {
                throw new AssertionError(String.format("Assertion failed: '%s' not found in common attributes. Found: [%s]", expected, actualDetails), e);
            });
        }
    }

    /**
     * Determines whether the given string is likely intended as a regular expression pattern
     * rather than a literal value.
     *
     * @param str the string to evaluate
     * @return true if the string appears to be a regex pattern, false otherwise
     */
    private boolean isRegexPattern(final String str)
    {
        if (str == null)
        {
            return false;
        }
        return str.contains("\\") || str.contains("[") || str.contains("]") || str.contains("{") || str.contains("}")
                || str.contains(".*") || str.contains(".+") || str.contains("|") || str.startsWith("^") || str.endsWith("$");
    }

    /**
     * Selenide condition that matches an element's text, textContent, value, or common/data attributes
     * against a regular expression pattern.
     */
    private static final class RegexMatch extends WebElementCondition
    {
        private final Pattern pattern;

        /**
         * Constructor.
         *
         * @param regex the regular expression pattern string
         */
        public RegexMatch(final String regex)
        {
            super("RegexMatch");
            this.pattern = Pattern.compile(regex);
        }

        @Override
        public CheckResult check(final Driver driver, final WebElement element)
        {
            // 1. Text of the element
            final String text = element.getText();
            if (text != null && pattern.matcher(text).find())
            {
                return new CheckResult(true, text);
            }

            // 2. textContent of the element
            final String textContent = element.getAttribute("textContent");
            if (textContent != null && pattern.matcher(textContent).find())
            {
                return new CheckResult(true, textContent);
            }

            // 3. Value of the element (e.g. input field)
            final String value = element.getAttribute("value");
            if (value != null && pattern.matcher(value).find())
            {
                return new CheckResult(true, value);
            }

            // 4. Common attributes: href, alt, src, title, placeholder
            final String[] attrs = {"href", "alt", "src", "title", "placeholder"};
            for (final String attr : attrs)
            {
                final String val = element.getAttribute(attr);
                if (val != null && pattern.matcher(val).find())
                {
                    return new CheckResult(true, String.format("attribute %s: %s", attr, val));
                }
            }

            // 5. Data attributes
            try
            {
                final Map<String, String> attributes = driver.executeJavaScript(
                        "var items = {}; " +
                                "for (var i = 0, attrs = arguments[0].attributes; i < attrs.length; i++) { " +
                                "  items[attrs[i].name] = attrs[i].value; " +
                                "} " +
                                "return items;",
                        element);
                if (attributes != null)
                {
                    for (final var entry : attributes.entrySet())
                    {
                        final String name = entry.getKey();
                        if (name != null && name.startsWith("data-"))
                        {
                            final String val = entry.getValue();
                            if (val != null && pattern.matcher(val).find())
                            {
                                return new CheckResult(true, String.format("data-attribute %s: %s", name, val));
                            }
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                // Ignore JS execution errors
            }

            return new CheckResult(false, text);
        }
    }
}
