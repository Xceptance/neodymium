/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
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
                    final Matcher attributeMatcher = Pattern.compile("^([a-zA-Z0-9_-]+)=[\"']?(.*?)[\"']?$").matcher(expected);
                    if (attributeMatcher.matches())
                    {
                        final String attrName = attributeMatcher.group(1);
                        final String attrValue = attributeMatcher.group(2);
                        cond = Condition.or("Assertion for " + expected,
                                Condition.attribute(attrName, attrValue),
                                Condition.exactText(expected),
                                Condition.partialText(expected),
                                Condition.value(expected),
                                new PartialTextContent(expected));
                    }
                    else
                    {
                        // Create a composite condition that searches for the expected text across 
                        // all common visible attributes, input values, text content areas, and all actual DOM attributes.
                        cond = Condition.or("Assertion for " + expected,
                                Condition.exactText(expected),
                                Condition.partialText(expected),
                                Condition.value(expected),
                                new PartialTextContent(expected),
                                new AnyAttributeContains(expected));
                    }
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
            String attributesStr = "Error retrieving attributes";
            try
            {
                final Map<String, String> attributes = Selenide.executeJavaScript(
                        "var items = {}; " +
                                "for (var i = 0, attrs = arguments[0].attributes; i < attrs.length; i++) { " +
                                "  items[attrs[i].name] = attrs[i].value; " +
                                "} " +
                                "return items;",
                        element);
                attributesStr = attributes != null ? attributes.toString() : "{}";
            }
            catch (final Exception ex)
            {
                // Ignore JS execution errors
            }
            final String actualDetails = String.format("Text: '%s', Value: '%s', Attributes: %s", element.getText(), element.getValue(), attributesStr);
            SelenideAddons.wrapAssertionError(() ->
            {
                throw new AssertionError(String.format("Assertion failed: '%s' not found in common or element attributes. Found: [%s]", expected, actualDetails), e);
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

            // 4. Check all DOM attributes
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
                        final String val = entry.getValue();
                        if (val != null && pattern.matcher(val).find())
                        {
                            return new CheckResult(true, String.format("attribute %s: %s", entry.getKey(), val));
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

    /**
     * Selenide condition that matches if any of the element's actual DOM attributes
     * (e.g. type, id, name, class, custom attributes, etc.) contains the expected value.
     */
    private static final class AnyAttributeContains extends WebElementCondition
    {
        private final String expectedValue;

        public AnyAttributeContains(final String expectedValue)
        {
            super("AnyAttributeContains");
            this.expectedValue = expectedValue;
        }

        @Override
        public CheckResult check(final Driver driver, final WebElement element)
        {
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
                        final String val = entry.getValue();
                        if (val != null && val.contains(expectedValue))
                        {
                            return new CheckResult(true, String.format("attribute %s: %s", entry.getKey(), val));
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                // Ignore JS execution errors
            }
            return new CheckResult(false, null);
        }
    }
}
