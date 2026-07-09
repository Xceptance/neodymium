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
package com.xceptance.neodymium.util.layer.playwright;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.xceptance.neodymium.util.layer.ElementCondition;
import com.xceptance.neodymium.util.layer.FoundElement;

/**
 * Playwright-backed implementation of {@link FoundElement}.
 * <p>
 * Wraps a Playwright {@link Locator} and delegates all operations to the Playwright API.
 * This class is the <strong>only permitted location</strong> inside the Playwright facade
 * package where {@code Locator} types are used to implement the backend-agnostic
 * {@link FoundElement} contract.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class PlaywrightFoundElement implements FoundElement
{
    /** Underlying Playwright locator handle. */
    private final Locator locator;

    /**
     * Creates a new wrapper around the given Playwright locator.
     *
     * @param locator the Playwright locator to wrap; must not be {@code null}
     */
    public PlaywrightFoundElement(final Locator locator)
    {
        this.locator = locator;
    }

    // ─── Interaction ─────────────────────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public void click()
    {
        locator.first().click();
    }

    /** {@inheritDoc} */
    @Override
    public void hover()
    {
        locator.first().hover();
    }

    /** {@inheritDoc} */
    @Override
    public void clear()
    {
        locator.first().clear();
    }

    /** {@inheritDoc} */
    @Override
    public void sendKeys(final CharSequence text)
    {
        locator.first().type(text.toString());
    }

    /** {@inheritDoc} */
    @Override
    public void selectOption(final String optionText)
    {
        locator.first().selectOption(optionText);
    }

    /** {@inheritDoc} */
    @Override
    public void scrollIntoView()
    {
        locator.first().scrollIntoViewIfNeeded();
    }

    // ─── State queries ────────────────────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public String getText()
    {
        final String text = locator.first().textContent();
        return text != null ? text.trim() : "";
    }

    /** {@inheritDoc} */
    @Override
    public String getTextContent()
    {
        final String tc = locator.first().textContent();
        return tc != null ? tc : "";
    }

    /** {@inheritDoc} */
    @Override
    public String getAttribute(final String name)
    {
        return locator.first().getAttribute(name);
    }

    /** {@inheritDoc} */
    @Override
    public String getTagName()
    {
        // Playwright does not expose a direct getTagName, so evaluate via JS.
        final Object tag = locator.first().evaluate("el => el.tagName.toLowerCase()");
        return tag != null ? tag.toString() : "";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDisplayed()
    {
        return locator.first().isVisible();
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists()
    {
        return locator.count() > 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isVisible()
    {
        return locator.first().isVisible();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHidden()
    {
        return locator.first().isHidden();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFocused()
    {
        // Playwright uses evaluate to detect focus because there is no direct isFocused() API.
        final Object result = locator.first().evaluate("el => el === document.activeElement");
        return Boolean.TRUE.equals(result);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSelected()
    {
        return locator.first().isChecked();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getAllAttributes()
    {
        try
        {
            @SuppressWarnings("unchecked")
            final Map<String, Object> raw = (Map<String, Object>) locator.first()
                    .evaluate("el => { var m = {}; for (var i = 0; i < el.attributes.length; i++) { m[el.attributes[i].name] = el.attributes[i].value; } return m; }");
            final Map<String, String> result = new HashMap<>();
            if (raw != null)
            {
                for (final Map.Entry<String, Object> entry : raw.entrySet())
                {
                    result.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
                }
            }
            return result;
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
        locator.first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeout.toMillis()));
    }

    /** {@inheritDoc} */
    @Override
    public void waitUntilExist(final Duration timeout)
    {
        locator.first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED)
                .setTimeout(timeout.toMillis()));
    }

    // ─── Condition evaluation ─────────────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public boolean matchesCondition(final ElementCondition condition)
    {
        try
        {
            return evaluateConditionNow(condition);
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void assertCondition(final ElementCondition condition)
    {
        // Use Playwright's built-in assertion APIs where available, otherwise evaluate directly.
        try
        {
            // Re-use matchesCondition; Playwright's waitFor handles timing automatically.
            switch (condition.getType())
            {
                case VISIBLE:
                    waitUntilVisible(Duration.ofSeconds(10));
                    return;
                case EXIST:
                    waitUntilExist(Duration.ofSeconds(10));
                    return;
                case HIDDEN:
                    locator.first().waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(10_000));
                    return;
                default:
                    // For text/attribute/regex conditions, poll until satisfied.
                    final long deadline = System.currentTimeMillis() + 10_000;
                    while (!evaluateConditionNow(condition))
                    {
                        if (System.currentTimeMillis() > deadline)
                        {
                            throw new AssertionError("Condition " + condition + " not met within 10 seconds");
                        }
                        try { Thread.sleep(200); } catch (final InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
            }
        }
        catch (final AssertionError ae)
        {
            throw ae;
        }
        catch (final Exception e)
        {
            throw new AssertionError("Condition " + condition + " could not be evaluated: " + e.getMessage(), e);
        }
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Evaluates the given condition once without any retry/timeout.
     *
     * @param condition the condition to evaluate
     * @return {@code true} if the condition is currently satisfied
     */
    private boolean evaluateConditionNow(final ElementCondition condition)
    {
        switch (condition.getType())
        {
            case EXIST:
                return exists();
            case VISIBLE:
                return isVisible();
            case HIDDEN:
                return isHidden();
            case FOCUSED:
                return isFocused();
            case TEXT_CONTAINS:
            {
                final String text = getText();
                final String tc = getTextContent();
                final String p = condition.getParam1();
                return (text != null && text.contains(p)) || (tc != null && tc.contains(p));
            }
            case TEXT_EXACT:
            {
                final String text = getText();
                return text != null && text.equals(condition.getParam1());
            }
            case VALUE_IS:
            {
                final String val = getAttribute("value");
                return condition.getParam1().equals(val);
            }
            case ANY_ATTR_CONTAINS:
            {
                final String sub = condition.getParam1();
                for (final String val : getAllAttributes().values())
                {
                    if (val != null && val.contains(sub))
                    {
                        return true;
                    }
                }
                return false;
            }
            case ATTR_IS:
            {
                final String actual = getAttribute(condition.getParam1());
                return condition.getParam2().equals(actual);
            }
            case MATCHES_REGEX:
            {
                final Pattern p = Pattern.compile(condition.getParam1());
                final String text = getText();
                if (text != null && p.matcher(text).find()) return true;
                final String tc = getTextContent();
                if (tc != null && p.matcher(tc).find()) return true;
                final String val = getAttribute("value");
                if (val != null && p.matcher(val).find()) return true;
                for (final String attrVal : getAllAttributes().values())
                {
                    if (attrVal != null && p.matcher(attrVal).find()) return true;
                }
                return false;
            }
            default:
                return false;
        }
    }
}
