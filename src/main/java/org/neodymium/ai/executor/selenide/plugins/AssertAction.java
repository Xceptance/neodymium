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
                if (action.isRegex())
                {
                    final Pattern pattern = Pattern.compile(cleanRegexPattern(expected), Pattern.DOTALL | Pattern.MULTILINE);
                    Selenide.Wait().until(d -> (d.getCurrentUrl() != null && pattern.matcher(d.getCurrentUrl()).find())
                            || (d.getTitle() != null && pattern.matcher(d.getTitle()).find()));
                    LOG.debug("   ✅ URL/Title Regex Assertion passed for: '{}'", expected);
                }
                else
                {
                    // Wait until the current page URL or title updates and matches/contains the expected string
                    Selenide.Wait().until(d -> (d.getCurrentUrl() != null && d.getCurrentUrl().contains(expected))
                            || (d.getTitle() != null && d.getTitle().contains(expected)));
                    LOG.debug("   ✅ URL/Title Assertion passed for: '{}'", expected);
                }
            }
            catch (final TimeoutException e)
            {
                final String actualUrl = WebDriverRunner.url();
                SelenideAddons.wrapAssertionError(() ->
                {
                    if (action.isRegex())
                    {
                        throw new AssertionError(String.format("Assertion failed: Expected URL to match regex '%s' but was '%s'", expected, actualUrl), e);
                    }
                    else
                    {
                        throw new AssertionError(String.format("Assertion failed: Expected URL to contain '%s' but was '%s'", expected, actualUrl), e);
                    }
                });
            }
            return;
        }

        // Handle Title assertions (matching target names like "title" or "pageTitle")
        if ("title".equalsIgnoreCase(action.getTarget()) || "pageTitle".equalsIgnoreCase(action.getTarget()))
        {
            if (expected == null)
            {
                throw new RuntimeException("Title assertion requires a 'value' (the expected title)");
            }

            try
            {
                if (action.isRegex())
                {
                    final Pattern pattern = Pattern.compile(cleanRegexPattern(expected), Pattern.DOTALL | Pattern.MULTILINE);
                    Selenide.Wait().until(d -> d.getTitle() != null && pattern.matcher(d.getTitle()).find());
                    LOG.debug("   ✅ Title Regex Assertion passed for: '{}'", expected);
                }
                else
                {
                    Selenide.Wait().until(d -> d.getTitle() != null && d.getTitle().contains(expected));
                    LOG.debug("   ✅ Title Assertion passed for: '{}'", expected);
                }
            }
            catch (final TimeoutException e)
            {
                final String actualTitle = Selenide.title();
                SelenideAddons.wrapAssertionError(() ->
                {
                    if (action.isRegex())
                    {
                        throw new AssertionError(String.format("Assertion failed: Expected Title to match regex '%s' but was '%s'", expected, actualTitle), e);
                    }
                    else
                    {
                        throw new AssertionError(String.format("Assertion failed: Expected Title to contain '%s' but was '%s'", expected, actualTitle), e);
                    }
                });
            }
            return;
        }

        // Handle Element assertions
        final boolean isAbsenceCheck = "hidden".equalsIgnoreCase(expected) || "[hidden]".equalsIgnoreCase(expected)
                || "absent".equalsIgnoreCase(expected) || "[absent]".equalsIgnoreCase(expected)
                || "not_exist".equalsIgnoreCase(expected) || "[not_exist]".equalsIgnoreCase(expected)
                || "not_exists".equalsIgnoreCase(expected) || "[not_exists]".equalsIgnoreCase(expected)
                || "invisible".equalsIgnoreCase(expected) || "[invisible]".equalsIgnoreCase(expected);

        final SelenideElement element = SelenideElementFinder.findElement(action);

        if (isAbsenceCheck)
        {
            element.should(Condition.or("Element is hidden or non-existent", Condition.hidden, Condition.not(Condition.exist)));
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
                final Boolean isFocused = Selenide.executeJavaScript(
                        "return document.activeElement === arguments[0] || (arguments[0].matches && arguments[0].matches(':focus'));",
                        element);
                if (!Boolean.TRUE.equals(isFocused))
                {
                    element.shouldBe(Condition.focused);
                }
            }
            // Visibility assertion: Verify that the targeted element is visible/present on the page
            else if ("visible".equalsIgnoreCase(expected) || "[visible]".equalsIgnoreCase(expected) || "present".equalsIgnoreCase(expected) || "[present]".equalsIgnoreCase(expected))
            {
                element.shouldBe(Condition.visible);
            }
            // Checked assertion: Verify that checkbox/radio button is checked
            else if ("checked".equalsIgnoreCase(expected) || "[checked]".equalsIgnoreCase(expected))
            {
                element.shouldBe(Condition.checked);
            }
            // Unchecked assertion: Verify that checkbox/radio button is unchecked
            else if ("unchecked".equalsIgnoreCase(expected) || "[unchecked]".equalsIgnoreCase(expected) || "not_checked".equalsIgnoreCase(expected) || "[not_checked]".equalsIgnoreCase(expected))
            {
                element.shouldNotBe(Condition.checked);
            }
            // Disabled assertion: Verify that input/button/element is disabled
            else if ("disabled".equalsIgnoreCase(expected) || "[disabled]".equalsIgnoreCase(expected))
            {
                element.shouldBe(Condition.disabled);
            }
            // Enabled assertion: Verify that input/button/element is enabled
            else if ("enabled".equalsIgnoreCase(expected) || "[enabled]".equalsIgnoreCase(expected))
            {
                element.shouldBe(Condition.enabled);
            }
            // Selected assertion: Verify that select option / ARIA option is selected
            else if ("selected".equalsIgnoreCase(expected) || "[selected]".equalsIgnoreCase(expected))
            {
                if ("SELECT".equalsIgnoreCase(element.getTagName()))
                {
                    element.getSelectedOption().shouldBe(Condition.exist);
                }
                else
                {
                    element.shouldBe(Condition.selected);
                }
            }
            // Readonly assertion: Verify that input is readonly
            else if ("readonly".equalsIgnoreCase(expected) || "[readonly]".equalsIgnoreCase(expected) || "read_only".equalsIgnoreCase(expected) || "[read_only]".equalsIgnoreCase(expected))
            {
                element.shouldBe(Condition.readonly);
            }
            // Editable assertion: Verify that input is editable
            else if ("editable".equalsIgnoreCase(expected) || "[editable]".equalsIgnoreCase(expected))
            {
                element.shouldBe(Condition.editable);
            }
            // Checkbox/radio boolean state shortcut: if expected is "true" or "false" on a checkbox or radio button
            else if (("true".equalsIgnoreCase(expected) || "false".equalsIgnoreCase(expected)) && isCheckboxOrRadio(element))
            {
                if ("true".equalsIgnoreCase(expected))
                {
                    element.shouldBe(Condition.checked);
                }
                else
                {
                    element.shouldNotBe(Condition.checked);
                }
            }
            // Content assertions: verify text, regex matching, or attribute contents of the target element
            else
            {
                final WebElementCondition cond;
                if (action.isRegex())
                {
                    cond = new RegexMatch(expected);
                }
                else
                {
                    // Check if value is format attrName=attrValue (e.g. class="active" or placeholder="search")
                    final Matcher attributeMatcher = Pattern.compile("^([a-zA-Z0-9_-]+)=[\"']?(.*?)[\"']?$").matcher(expected);
                    if (attributeMatcher.matches())
                    {
                        final String rawAttrName = attributeMatcher.group(1);
                        final String rawAttrValue = attributeMatcher.group(2);
                        final String attrName = rawAttrName.toLowerCase();
                        final String attrValue = rawAttrValue.toLowerCase();

                        if ("checked".equals(attrName))
                        {
                            if ("false".equals(attrValue))
                            {
                                element.shouldNotBe(Condition.checked);
                            }
                            else
                            {
                                element.shouldBe(Condition.checked);
                            }
                            return;
                        }
                        if ("disabled".equals(attrName))
                        {
                            if ("false".equals(attrValue))
                            {
                                element.shouldBe(Condition.enabled);
                            }
                            else
                            {
                                element.shouldBe(Condition.disabled);
                            }
                            return;
                        }
                        if ("enabled".equals(attrName))
                        {
                            if ("false".equals(attrValue))
                            {
                                element.shouldBe(Condition.disabled);
                            }
                            else
                            {
                                element.shouldBe(Condition.enabled);
                            }
                            return;
                        }
                        if ("selected".equals(attrName))
                        {
                            if ("false".equals(attrValue))
                            {
                                element.shouldNotBe(Condition.selected);
                            }
                            else
                            {
                                element.shouldBe(Condition.selected);
                            }
                            return;
                        }
                        if ("readonly".equals(attrName) || "read_only".equals(attrName))
                        {
                            if ("false".equals(attrValue))
                            {
                                element.shouldBe(Condition.editable);
                            }
                            else
                            {
                                element.shouldBe(Condition.readonly);
                            }
                            return;
                        }

                        cond = Condition.or("Assertion for " + expected,
                                Condition.attribute(rawAttrName, rawAttrValue),
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

                element.should(cond);
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
        final String msg = String.format("Assertion failed: '%s' not found in common or element attributes. Found: [%s]", expected, actualDetails);
        if (e instanceof AssertionError ae)
        {
            throw ae;
        }
        throw new AssertionError(msg, e);
    }


    /**
     * Sanitizes and normalizes a regular expression string for LLM-produced patterns.
     * Strips leading/trailing slashes, cleans over-escaped dollar signs, and escapes unescaped currency amounts.
     *
     * @param regex the raw regex pattern
     * @return cleaned regex pattern
     */
    public static String cleanRegexPattern(final String regex)
    {
        String cleanRegex = regex != null ? regex : "";
        if (cleanRegex.startsWith("/") && cleanRegex.endsWith("/") && cleanRegex.length() > 2)
        {
            cleanRegex = cleanRegex.substring(1, cleanRegex.length() - 1);
        }
        if (cleanRegex.contains("\\\\$"))
        {
            cleanRegex = cleanRegex.replace("\\\\$", "\\$");
        }
        // Escape unescaped currency dollar signs (e.g. $27.58) so regex matching handles literal amounts
        cleanRegex = cleanRegex.replaceAll("(?<!\\\\)\\$(\\d)", "\\\\\\$$1");
        return cleanRegex;
    }

    private static final class RegexMatch extends WebElementCondition
    {
        private final Pattern pattern;

        public RegexMatch(final String regex)
        {
            super("RegexMatch");
            final String cleanRegex = cleanRegexPattern(regex);
            this.pattern = Pattern.compile(cleanRegex, Pattern.DOTALL | Pattern.MULTILINE);
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

    private static boolean isCheckboxOrRadio(final SelenideElement element)
    {
        try
        {
            final String tagName = element.getTagName();
            if ("input".equalsIgnoreCase(tagName))
            {
                final String type = element.getAttribute("type");
                return "checkbox".equalsIgnoreCase(type) || "radio".equalsIgnoreCase(type);
            }
        }
        catch (final Exception e)
        {
            // Ignore
        }
        return false;
    }
}
