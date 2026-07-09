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
package com.xceptance.neodymium.util.layer;

import java.time.Duration;
import java.util.Map;

/**
 * Backend-agnostic abstraction for a located DOM element, used throughout the AI action layer.
 * <p>
 * Implementations exist for each driver backend:
 * </p>
 * <ul>
 *   <li>{@code SelenideFoundElement} — wraps a Selenide {@code SelenideElement}</li>
 *   <li>{@code PlaywrightFoundElement} — wraps a Playwright {@code Locator}</li>
 * </ul>
 * <p>
 * All method contracts follow the DOM-ready assumption: the element reference is expected to be
 * valid at the time of the call. Implementations should throw a meaningful runtime exception if
 * the element cannot be interacted with.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public interface FoundElement
{
    // ─── Interaction ─────────────────────────────────────────────────────────

    /**
     * Clicks the element.
     */
    void click();

    /**
     * Hovers the mouse pointer over the element without clicking.
     */
    void hover();

    /**
     * Clears the current value of an input or textarea element.
     */
    void clear();

    /**
     * Types the given character sequence into an input element.
     *
     * @param text the text to type
     */
    void sendKeys(CharSequence text);

    /**
     * Selects the option matching the given visible text in a {@code <select>} element.
     *
     * @param optionText the visible text of the option to select
     */
    void selectOption(String optionText);

    /**
     * Scrolls the element into the visible viewport using a smooth animation.
     */
    void scrollIntoView();

    // ─── State queries ────────────────────────────────────────────────────────

    /**
     * Returns the element's visible text content (equivalent to {@code innerText}).
     *
     * @return text content, or an empty string if none
     */
    String getText();

    /**
     * Returns the raw text content of the element including hidden children
     * (equivalent to {@code textContent}).
     *
     * @return raw text content, or an empty string if none
     */
    String getTextContent();

    /**
     * Returns the value of the named DOM attribute.
     *
     * @param name the attribute name
     * @return the attribute value, or {@code null} if absent
     */
    String getAttribute(String name);

    /**
     * Returns the HTML tag name of the element (lower-cased).
     *
     * @return tag name, e.g. {@code "input"}, {@code "button"}
     */
    String getTagName();

    /**
     * Returns {@code true} if the element is rendered and visible in the viewport.
     *
     * @return {@code true} if visible
     */
    boolean isDisplayed();

    /**
     * Returns {@code true} if the element exists in the DOM.
     *
     * @return {@code true} if present
     */
    boolean exists();

    /**
     * Returns {@code true} if the element is currently visible.
     * Equivalent to {@link #isDisplayed()} but semantically aligned with the
     * {@link ElementCondition.Type#VISIBLE} condition.
     *
     * @return {@code true} if visible
     */
    boolean isVisible();

    /**
     * Returns {@code true} if the element is currently hidden or absent.
     *
     * @return {@code true} if hidden
     */
    boolean isHidden();

    /**
     * Returns {@code true} if the element currently holds keyboard focus.
     *
     * @return {@code true} if focused
     */
    boolean isFocused();

    /**
     * Returns {@code true} if the element is selected (checkboxes, radio buttons, options).
     *
     * @return {@code true} if selected
     */
    boolean isSelected();

    /**
     * Returns all DOM attributes of the element as a name-to-value map.
     *
     * @return immutable map of attribute names to values; empty if none or on error
     */
    Map<String, String> getAllAttributes();

    // ─── Waiting ─────────────────────────────────────────────────────────────

    /**
     * Blocks until the element is visible, up to the given timeout.
     *
     * @param timeout maximum wait duration
     * @throws RuntimeException if the element is not visible within the timeout
     */
    void waitUntilVisible(Duration timeout);

    /**
     * Blocks until the element exists in the DOM, up to the given timeout.
     *
     * @param timeout maximum wait duration
     * @throws RuntimeException if the element does not appear within the timeout
     */
    void waitUntilExist(Duration timeout);

    // ─── Condition evaluation ─────────────────────────────────────────────────

    /**
     * Evaluates the given condition immediately without waiting.
     *
     * @param condition the condition to test
     * @return {@code true} if the condition is satisfied right now
     */
    boolean matchesCondition(ElementCondition condition);

    /**
     * Asserts the given condition, waiting up to the configured element timeout.
     * Throws a meaningful assertion error if the condition is not satisfied in time.
     *
     * @param condition the condition to assert
     * @throws AssertionError    if the condition is not met within the timeout
     * @throws RuntimeException  if element interaction fails
     */
    void assertCondition(ElementCondition condition);
}
