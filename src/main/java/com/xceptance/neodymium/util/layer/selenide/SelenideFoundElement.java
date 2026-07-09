/*
 * Copyright (c) 2017-2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.util.layer.selenide;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import com.xceptance.neodymium.util.layer.ElementCondition;
import com.xceptance.neodymium.util.layer.FoundElement;
import org.openqa.selenium.WebElement;

/**
 * Selenide-backed implementation of {@link FoundElement}.
 * <p>
 * Wraps a Selenide {@link SelenideElement} and delegates all operations to it.
 * This class is the <strong>only permitted location</strong> inside the Selenide
 * facade package where {@code SelenideElement} and {@code Condition} types are used
 * to implement the backend-agnostic {@link FoundElement} contract.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SelenideFoundElement implements FoundElement
{
    /** Underlying Selenide element handle. */
    private final SelenideElement element;

    /**
     * Creates a new wrapper around the given Selenide element.
     *
     * @param element the Selenide element to wrap; must not be {@code null}
     */
    public SelenideFoundElement(final SelenideElement element)
    {
        this.element = element;
    }

    // ─── Interaction ─────────────────────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public void click()
    {
        element.click();
    }

    /** {@inheritDoc} */
    @Override
    public void hover()
    {
        element.hover();
    }

    /** {@inheritDoc} */
    @Override
    public void clear()
    {
        element.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void sendKeys(final CharSequence text)
    {
        element.sendKeys(text);
    }

    /** {@inheritDoc} */
    @Override
    public void selectOption(final String optionText)
    {
        element.selectOption(optionText);
    }

    /** {@inheritDoc} */
    @Override
    public void scrollIntoView()
    {
        element.scrollIntoView(true);
    }

    // ─── State queries ────────────────────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public String getText()
    {
        return element.getText();
    }

    /** {@inheritDoc} */
    @Override
    public String getTextContent()
    {
        final String tc = element.getAttribute("textContent");
        return tc != null ? tc : "";
    }

    /** {@inheritDoc} */
    @Override
    public String getAttribute(final String name)
    {
        return element.getAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public String getTagName()
    {
        return element.getTagName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDisplayed()
    {
        return element.isDisplayed();
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists()
    {
        return element.exists();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isVisible()
    {
        return element.is(Condition.visible);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHidden()
    {
        return element.is(Condition.hidden);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFocused()
    {
        return element.is(Condition.focused);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSelected()
    {
        return element.isSelected();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getAllAttributes()
    {
        try
        {
            final Map<String, String> attributes = com.codeborne.selenide.WebDriverRunner.driver().executeJavaScript(
                    "var items = {}; " +
                    "for (var i = 0, attrs = arguments[0].attributes; i < attrs.length; i++) { " +
                    "  items[attrs[i].name] = attrs[i].value; " +
                    "} " +
                    "return items;",
                    element.toWebElement());
            return attributes != null ? attributes : new HashMap<>();
        }
        catch (final Exception e)
        {
            return new HashMap<>();
        }
    }

    // ─── Waiting ─────────────────────────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public void waitUntilVisible(final Duration timeout)
    {
        element.shouldBe(Condition.visible, timeout);
    }

    /** {@inheritDoc} */
    @Override
    public void waitUntilExist(final Duration timeout)
    {
        element.should(Condition.exist, timeout);
    }

    // ─── Condition evaluation ─────────────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public boolean matchesCondition(final ElementCondition condition)
    {
        return element.is(toSelenideCondition(condition));
    }

    /** {@inheritDoc} */
    @Override
    public void assertCondition(final ElementCondition condition)
    {
        element.should(toSelenideCondition(condition));
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Translates a backend-agnostic {@link ElementCondition} into the corresponding
     * Selenide {@link WebElementCondition}.
     *
     * @param condition the agnostic condition to translate
     * @return the equivalent Selenide condition
     */
    private WebElementCondition toSelenideCondition(final ElementCondition condition)
    {
        switch (condition.getType())
        {
            case EXIST:
                return Condition.exist;
            case VISIBLE:
                return Condition.visible;
            case HIDDEN:
                return Condition.hidden;
            case FOCUSED:
                return Condition.focused;
            case TEXT_CONTAINS:
                return Condition.partialText(condition.getParam1());
            case TEXT_EXACT:
                return Condition.exactText(condition.getParam1());
            case VALUE_IS:
                return Condition.value(condition.getParam1());
            case ATTR_IS:
                return Condition.attribute(condition.getParam1(), condition.getParam2());
            case ANY_ATTR_CONTAINS:
                return new AnyAttrContainsCondition(condition.getParam1());
            case MATCHES_REGEX:
                return new RegexMatchCondition(condition.getParam1());
            default:
                throw new IllegalArgumentException("Unknown condition type: " + condition.getType());
        }
    }

    // ─── Private Selenide condition implementations ───────────────────────────

    /**
     * Selenide condition that matches if any DOM attribute of the element
     * contains the expected substring.
     */
    private static final class AnyAttrContainsCondition extends WebElementCondition
    {
        private final String expected;

        AnyAttrContainsCondition(final String expected)
        {
            super("AnyAttrContains");
            this.expected = expected;
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
                    for (final Map.Entry<String, String> entry : attributes.entrySet())
                    {
                        final String val = entry.getValue();
                        if (val != null && val.contains(expected))
                        {
                            return new CheckResult(true, "attribute " + entry.getKey() + ": " + val);
                        }
                    }
                }
            }
            catch (final Exception ignored)
            {
            }
            return new CheckResult(false, null);
        }
    }

    /**
     * Selenide condition that matches an element's text, textContent, value, or
     * any attribute against a regular expression pattern.
     */
    private static final class RegexMatchCondition extends WebElementCondition
    {
        private final Pattern pattern;

        RegexMatchCondition(final String regex)
        {
            super("RegexMatch");
            this.pattern = Pattern.compile(regex);
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
                        "} return items;",
                        element);
                if (attributes != null)
                {
                    for (final Map.Entry<String, String> entry : attributes.entrySet())
                    {
                        final String val = entry.getValue();
                        if (val != null && pattern.matcher(val).find())
                        {
                            return new CheckResult(true, "attribute " + entry.getKey() + ": " + val);
                        }
                    }
                }
            }
            catch (final Exception ignored)
            {
            }
            return new CheckResult(false, text);
        }
    }
}
