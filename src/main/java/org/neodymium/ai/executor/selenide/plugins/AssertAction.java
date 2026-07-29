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
package org.neodymium.ai.executor.selenide.plugins;

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
import org.neodymium.ai.action.Action;
import org.neodymium.util.SelenideAddons;

import org.neodymium.ai.executor.selenide.SelenideElementFinder;

/**
 * Concrete action plugin executing ASSERT commands to verify page states, element presence, visibility, focus, and content.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class AssertAction implements BrowserActionPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(AssertAction.class);

    /**
     * Constructs an AssertAction.
     */
    public AssertAction()
    {
    }

    /**
     * Executes the assert action on the page.
     *
     * @param action the assert action
     * @throws Exception if execution fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null || action.getTarget() == null)
        {
            return;
        }

        final String expected = action.getValue();

        // Handle URL assertions (matching target names like "url", "currentUrl", or "pageUrl")
        if ("url".equalsIgnoreCase(action.getTarget()) || "currentUrl".equalsIgnoreCase(action.getTarget()) || "pageUrl".equalsIgnoreCase(action.getTarget()))
        {
            if (expected == null)
            {
                throw new RuntimeException("URL assertion requires a 'value' (the expected URL)");
            }

            try
            {
                // Wait until the current page URL updates and matches/contains the expected string
                Selenide.Wait().until(d -> d.getCurrentUrl() != null && d.getCurrentUrl().contains(expected));
                LOG.debug("   ✅ URL Assertion passed for: '{}'", expected);
            }
            catch (final TimeoutException e)
            {
                final String actualUrl = WebDriverRunner.url();
                SelenideAddons.wrapAssertionError(() ->
                {
                    throw new AssertionError(String.format("Assertion failed: Expected URL to contain '%s' but was '%s'", expected, actualUrl), e);
                });
            }
            return;
        }

        // Handle Element assertions
        final boolean isAbsenceCheck = "hidden".equalsIgnoreCase(expected) || "[hidden]".equalsIgnoreCase(expected)
                || "absent".equalsIgnoreCase(expected) || "[absent]".equalsIgnoreCase(expected);

        final SelenideElement element = SelenideElementFinder.findElement(action.getTarget());

        if (isAbsenceCheck)
        {
            element.shouldBe(Condition.hidden);
            return;
        }

        // If no value is specified, assert that the target element simply exists on the page
        if (expected == null)
        {
            element.should(Condition.exist);
            LOG.debug("   ✅ Element exists: {}", action);
            return;
        }

        try
        {
            // Focus assertion: Verify that the targeted element is currently focused (document.activeElement)
            if ("focused".equalsIgnoreCase(expected) || "[focused]".equalsIgnoreCase(expected))
            {
                element.shouldBe(Condition.focused);
            }
            // Visibility assertion: Verify that the targeted element is visible/present on the page
            else if ("visible".equalsIgnoreCase(expected) || "[visible]".equalsIgnoreCase(expected) || "present".equalsIgnoreCase(expected) || "[present]".equalsIgnoreCase(expected))
            {
                element.shouldBe(Condition.visible);
            }
            // Content assertions: verify text, regex matching, or attribute contents of the target element
            else
            {
                final WebElementCondition cond;
                if (isRegexPattern(expected))
                {
                    cond = new RegexMatch(expected);
                }
                else
                {
                    // Check if value is format attrName=attrValue (e.g. class="active" or placeholder="search")
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
                        cond = Condition.or("Assertion for " + expected,
                                Condition.exactText(expected),
                                Condition.partialText(expected),
                                Condition.value(expected),
                                new PartialTextContent(expected),
                                new AnyAttributeContains(expected));
                    }
                }

                element.should(cond, java.time.Duration.ofMillis(Math.max(com.codeborne.selenide.Configuration.timeout, 6000)));
            }
            LOG.debug("   ✅ Assertion passed for: '{}'", expected);
        }
        catch (final Throwable e)
        {
            wrapAndRethrow(element, expected, e);
        }
    }

    private void wrapAndRethrow(final SelenideElement element, final String expected, final Throwable e)
    {
        String attributesStr = "Error retrieving attributes";
        String actualText = "";
        String actualValue = "";
        try
        {
            actualText = element.getText();
        }
        catch (final Exception ex)
        {
            actualText = "<unable to fetch: " + ex.getMessage() + ">";
        }
        try
        {
            actualValue = element.getValue();
        }
        catch (final Exception ex)
        {
            actualValue = "<unable to fetch: " + ex.getMessage() + ">";
        }
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
        final String actualDetails = String.format("Text: '%s', Value: '%s', Attributes: %s", actualText, actualValue, attributesStr);
        SelenideAddons.wrapAssertionError(() ->
        {
            throw new AssertionError(String.format("Assertion failed: '%s' not found in common or element attributes. Found: [%s]", expected, actualDetails), e);
        });
    }

    private boolean isRegexPattern(final String str)
    {
        if (str == null)
        {
            return false;
        }
        if (str.startsWith("/") && str.endsWith("/") && str.length() > 2)
        {
            return true;
        }
        return str.contains("\\") || str.contains("[") || str.contains("]") || str.contains("{") || str.contains("}")
                || str.contains(".*") || str.contains(".+") || str.contains("|") || str.startsWith("^") || str.endsWith("$");
    }

    private static final class RegexMatch extends WebElementCondition
    {
        private final Pattern pattern;

        public RegexMatch(final String regex)
        {
            super("RegexMatch");
            String cleanRegex = regex;
            if (cleanRegex.startsWith("/") && cleanRegex.endsWith("/") && cleanRegex.length() > 2)
            {
                cleanRegex = cleanRegex.substring(1, cleanRegex.length() - 1);
            }
            this.pattern = Pattern.compile(cleanRegex);
        }

        @Override
        public CheckResult check(final Driver driver, final WebElement element)
        {
            final String text = element.getText();
            if (text != null && pattern.matcher(text).find())
            {
                return new CheckResult(true, text);
            }

            final String textContent = element.getAttribute("textContent");
            if (textContent != null && pattern.matcher(textContent).find())
            {
                return new CheckResult(true, textContent);
            }

            final String value = element.getAttribute("value");
            if (value != null && pattern.matcher(value).find())
            {
                return new CheckResult(true, value);
            }

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

            return new CheckResult(false, null);
        }
    }

    private static final class PartialTextContent extends WebElementCondition
    {
        private final String value;

        public PartialTextContent(final String value)
        {
            super("PartialTextContent");
            this.value = value;
        }

        @Override
        public CheckResult check(final Driver driver, final WebElement element)
        {
            final String attribute = element.getAttribute("textContent");
            final boolean found = attribute != null && attribute.contains(value);
            return new CheckResult(found, found ? attribute : null);
        }
    }

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
