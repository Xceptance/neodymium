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
import java.util.regex.PatternSyntaxException;
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

        final String type = action.getType() != null ? action.getType().toUpperCase() : "ASSERT";
        final String expected = action.getValue();

        // Handle URL assertions
        if ("ASSERT_URL".equals(type) || ("ASSERT".equals(type) && ("url".equalsIgnoreCase(action.getTarget()) || "currentUrl".equalsIgnoreCase(action.getTarget()) || "pageUrl".equalsIgnoreCase(action.getTarget()))))
        {
            executeUrlAssertion(action, expected);
            return;
        }

        // Handle Title assertions
        if ("ASSERT_TITLE".equals(type) || ("ASSERT".equals(type) && ("title".equalsIgnoreCase(action.getTarget()) || "pageTitle".equalsIgnoreCase(action.getTarget()))))
        {
            executeTitleAssertion(action, expected);
            return;
        }

        final SelenideElement element = SelenideElementFinder.findElement(action);

        try
        {
            switch (type)
            {
                case "ASSERT_EXISTS", "ASSERT_VISIBLE" ->
                {
                    element.shouldBe(Condition.visible);
                    LOG.debug("   ✅ Element exists/visible: {}", action);
                    return;
                }
                case "ASSERT_ABSENT", "ASSERT_HIDDEN" ->
                {
                    element.should(Condition.or("Element is hidden or non-existent", Condition.hidden, Condition.not(Condition.exist)));
                    LOG.debug("   ✅ Element absent/hidden: {}", action);
                    return;
                }
                case "ASSERT_CHECKED" ->
                {
                    element.shouldBe(Condition.checked);
                    LOG.debug("   ✅ Element checked: {}", action);
                    return;
                }
                case "ASSERT_UNCHECKED" ->
                {
                    element.shouldNotBe(Condition.checked);
                    LOG.debug("   ✅ Element unchecked: {}", action);
                    return;
                }
                case "ASSERT_DISABLED" ->
                {
                    element.shouldBe(Condition.disabled);
                    LOG.debug("   ✅ Element disabled: {}", action);
                    return;
                }
                case "ASSERT_ENABLED" ->
                {
                    element.shouldBe(Condition.enabled);
                    LOG.debug("   ✅ Element enabled: {}", action);
                    return;
                }
                case "ASSERT_FOCUSED" ->
                {
                    final Boolean isFocused = Selenide.executeJavaScript(
                            "return document.activeElement === arguments[0] || (arguments[0].matches && arguments[0].matches(':focus'));",
                            element);
                    if (!Boolean.TRUE.equals(isFocused))
                    {
                        element.shouldBe(Condition.focused);
                    }
                    LOG.debug("   ✅ Element focused: {}", action);
                    return;
                }
                case "ASSERT_SELECTED" ->
                {
                    if ("SELECT".equalsIgnoreCase(element.getTagName()))
                    {
                        element.getSelectedOption().shouldBe(Condition.exist);
                    }
                    else
                    {
                        element.shouldBe(Condition.selected);
                    }
                    LOG.debug("   ✅ Element selected: {}", action);
                    return;
                }
                case "ASSERT_READONLY" ->
                {
                    element.shouldBe(Condition.readonly);
                    LOG.debug("   ✅ Element readonly: {}", action);
                    return;
                }
                case "ASSERT_EDITABLE" ->
                {
                    element.shouldBe(Condition.editable);
                    LOG.debug("   ✅ Element editable: {}", action);
                    return;
                }
                case "ASSERT_VALUE" ->
                {
                    element.shouldHave(Condition.value(expected != null ? expected : ""));
                    LOG.debug("   ✅ Element value matches '{}': {}", expected, action);
                    return;
                }
                case "ASSERT_TEXT" ->
                {
                    executeTextAssertion(element, action, expected);
                    return;
                }
                default ->
                {
                    // Generic "ASSERT" legacy dispatch for backward compatibility
                    executeLegacyAssert(element, action, expected);
                }
            }
        }
        catch (final Throwable e)
        {
            wrapAndRethrow(element, expected != null ? expected : type, e);
        }
    }

    private void executeUrlAssertion(final Action action, final String expected)
    {
        if (expected == null)
        {
            throw new RuntimeException("URL assertion requires a 'value' (the expected URL)");
        }

        try
        {
            if (action.isRegex())
            {
                final String cleanExpected = cleanRegexPattern(expected);
                Pattern pattern;
                try
                {
                    pattern = Pattern.compile(cleanExpected, Pattern.DOTALL | Pattern.MULTILINE);
                }
                catch (final PatternSyntaxException e)
                {
                    pattern = Pattern.compile(Pattern.quote(cleanExpected), Pattern.DOTALL | Pattern.MULTILINE);
                }
                final Pattern finalPattern = pattern;
                Selenide.Wait().until(d -> (d.getCurrentUrl() != null && (finalPattern.matcher(d.getCurrentUrl()).find() || d.getCurrentUrl().contains(cleanExpected)))
                        || (d.getTitle() != null && (finalPattern.matcher(d.getTitle()).find() || d.getTitle().contains(cleanExpected))));
                LOG.debug("   ✅ URL/Title Regex Assertion passed for: '{}'", expected);
            }
            else
            {
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
    }

    private void executeTitleAssertion(final Action action, final String expected)
    {
        if (expected == null)
        {
            throw new RuntimeException("Title assertion requires a 'value' (the expected title)");
        }

        try
        {
            if (action.isRegex())
            {
                final String cleanExpected = cleanRegexPattern(expected);
                Pattern pattern;
                try
                {
                    pattern = Pattern.compile(cleanExpected, Pattern.DOTALL | Pattern.MULTILINE);
                }
                catch (final PatternSyntaxException e)
                {
                    pattern = Pattern.compile(Pattern.quote(cleanExpected), Pattern.DOTALL | Pattern.MULTILINE);
                }
                final Pattern finalPattern = pattern;
                Selenide.Wait().until(d -> d.getTitle() != null && (finalPattern.matcher(d.getTitle()).find() || d.getTitle().contains(cleanExpected)));
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
    }

    private void executeTextAssertion(final SelenideElement element, final Action action, final String expected)
    {
        final String matchText = expected != null ? expected : "";
        final WebElementCondition cond;
        if (action.isRegex())
        {
            cond = new RegexMatch(matchText);
        }
        else
        {
            cond = Condition.or("Assertion for text " + matchText,
                    Condition.exactText(matchText),
                    Condition.partialText(matchText),
                    Condition.value(matchText),
                    new PartialTextContent(matchText),
                    new AnyAttributeContains(matchText));
        }
        element.should(cond);
        LOG.debug("   ✅ Text Assertion passed for: '{}'", matchText);
    }

    private void executeLegacyAssert(final SelenideElement element, final Action action, final String expected)
    {
        final boolean isAbsenceCheck = "hidden".equalsIgnoreCase(expected) || "[hidden]".equalsIgnoreCase(expected)
                || "absent".equalsIgnoreCase(expected) || "[absent]".equalsIgnoreCase(expected)
                || "not_exist".equalsIgnoreCase(expected) || "[not_exist]".equalsIgnoreCase(expected)
                || "not_exists".equalsIgnoreCase(expected) || "[not_exists]".equalsIgnoreCase(expected)
                || "invisible".equalsIgnoreCase(expected) || "[invisible]".equalsIgnoreCase(expected);

        if (isAbsenceCheck)
        {
            element.should(Condition.or("Element is hidden or non-existent", Condition.hidden, Condition.not(Condition.exist)));
            return;
        }

        if (expected == null || expected.isEmpty())
        {
            element.should(Condition.exist);
            LOG.debug("   ✅ Element exists: {}", action);
            return;
        }

        // Focus assertion
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
        // Visibility assertion
        else if ("visible".equalsIgnoreCase(expected) || "[visible]".equalsIgnoreCase(expected) || "present".equalsIgnoreCase(expected) || "[present]".equalsIgnoreCase(expected))
        {
            element.shouldBe(Condition.visible);
        }
        // Checked assertion
        else if ("checked".equalsIgnoreCase(expected) || "[checked]".equalsIgnoreCase(expected))
        {
            element.shouldBe(Condition.checked);
        }
        // Unchecked assertion
        else if ("unchecked".equalsIgnoreCase(expected) || "[unchecked]".equalsIgnoreCase(expected) || "not_checked".equalsIgnoreCase(expected) || "[not_checked]".equalsIgnoreCase(expected))
        {
            element.shouldNotBe(Condition.checked);
        }
        // Disabled assertion
        else if ("disabled".equalsIgnoreCase(expected) || "[disabled]".equalsIgnoreCase(expected))
        {
            element.shouldBe(Condition.disabled);
        }
        // Enabled assertion
        else if ("enabled".equalsIgnoreCase(expected) || "[enabled]".equalsIgnoreCase(expected))
        {
            element.shouldBe(Condition.enabled);
        }
        // Selected assertion
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
        // Readonly assertion
        else if ("readonly".equalsIgnoreCase(expected) || "[readonly]".equalsIgnoreCase(expected) || "read_only".equalsIgnoreCase(expected) || "[read_only]".equalsIgnoreCase(expected))
        {
            element.shouldBe(Condition.readonly);
        }
        // Editable assertion
        else if ("editable".equalsIgnoreCase(expected) || "[editable]".equalsIgnoreCase(expected))
        {
            element.shouldBe(Condition.editable);
        }
        // Checkbox/radio boolean state shortcut
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
        // Content assertions
        else
        {
            final WebElementCondition cond;
            if (action.isRegex())
            {
                cond = new RegexMatch(expected);
            }
            else
            {
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
        LOG.debug("   ✅ Legacy Assertion passed for: '{}'", expected);
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
     * Sanitizes and normalizes a regular expression string for LLM-produced patterns in an application-agnostic manner.
     * Strips enclosing slashes and normalizes over-escaped dollar signs.
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
        return cleanRegex;
    }

    private static final class RegexMatch extends WebElementCondition
    {
        private final String cleanText;
        private final Pattern pattern;

        public RegexMatch(final String regex)
        {
            super("RegexMatch");
            this.cleanText = cleanRegexPattern(regex);
            Pattern compiled;
            try
            {
                compiled = Pattern.compile(this.cleanText, Pattern.DOTALL | Pattern.MULTILINE);
            }
            catch (final PatternSyntaxException e)
            {
                compiled = Pattern.compile(Pattern.quote(this.cleanText), Pattern.DOTALL | Pattern.MULTILINE);
            }
            this.pattern = compiled;
        }

        @Override
        public CheckResult check(final Driver driver, final WebElement element)
        {
            final String text = element.getText();
            if (matches(text))
            {
                return new CheckResult(true, text);
            }

            final String textContent = element.getAttribute("textContent");
            if (matches(textContent))
            {
                return new CheckResult(true, textContent);
            }

            final String value = element.getAttribute("value");
            if (matches(value))
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
                        if (matches(val))
                        {
                            return new CheckResult(true, String.format("attribute %s: %s", entry.getKey(), val));
                        }
                    }
                }
            }
            catch (final Exception ignored)
            {
            }

            return new CheckResult(false, null);
        }

        private boolean matches(final String actual)
        {
            if (actual == null)
            {
                return false;
            }
            // 1. Primary: match compiled regex
            if (pattern.matcher(actual).find())
            {
                return true;
            }
            // 2. Fallback on match failure: check literal substring containment
            return !cleanText.isEmpty() && actual.contains(cleanText);
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
