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
package com.xceptance.neodymium.util;

import java.time.Duration;

import com.codeborne.selenide.SelenideElement;

/**
 * Static entry point for assertions within the Neodymium framework.
 * <p>
 * This class provides backend-agnostic assertions that automatically handle framework
 * features like failure screenshots and page source capture. It delegates to the
 * active {@link com.xceptance.neodymium.util.layer.AssertionFacade}.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class NeodymiumAssertion
{
    private NeodymiumAssertion()
    {
    }

    /**
     * Asserts that a condition is true.
     *
     * @param condition the condition to check
     */
    public static void assertTrue(final boolean condition)
    {
        Neodymium.interaction().assertions().assertTrue(condition);
    }

    /**
     * Asserts that a condition is true.
     *
     * @param message failure message
     * @param condition the condition to check
     */
    public static void assertTrue(final String message, final boolean condition)
    {
        Neodymium.interaction().assertions().assertTrue(message, condition);
    }

    /**
     * Asserts that two objects are equal.
     *
     * @param expected expected value
     * @param actual actual value
     */
    public static void assertEquals(final Object expected, final Object actual)
    {
        Neodymium.interaction().assertions().assertEquals(expected, actual);
    }

    /**
     * Asserts that two objects are equal.
     *
     * @param message failure message
     * @param expected expected value
     * @param actual actual value
     */
    public static void assertEquals(final String message, final Object expected, final Object actual)
    {
        Neodymium.interaction().assertions().assertEquals(message, expected, actual);
    }

    /**
     * Asserts that an element matches specific conditions (e.g., "exist", "visible").
     *
     * @param element the element to check
     * @param conditions conditions to verify
     */
    public static void should(final SelenideElement element, final String... conditions)
    {
        Neodymium.interaction().assertions().should(element, conditions);
    }

    /**
     * Asserts that an element matches specific conditions within a timeout.
     *
     * @param element the element to check
     * @param timeout maximum time to wait
     * @param conditions conditions to verify
     */
    public static void should(final SelenideElement element, final Duration timeout, final String... conditions)
    {
        Neodymium.interaction().assertions().should(element, timeout, conditions);
    }

    /**
     * Asserts that an element matches specific conditions (alias for {@link #should}).
     *
     * @param element the element to check
     * @param conditions conditions to verify
     */
    public static void shouldBe(final SelenideElement element, final String... conditions)
    {
        should(element, conditions);
    }

    /**
     * Asserts that an element matches specific conditions within a timeout (alias for {@link #should}).
     *
     * @param element the element to check
     * @param timeout maximum time to wait
     * @param conditions conditions to verify
     */
    public static void shouldBe(final SelenideElement element, final Duration timeout, final String... conditions)
    {
        should(element, timeout, conditions);
    }

    /**
     * Asserts that an element has a specific attribute value.
     *
     * @param element the element to check
     * @param attributeName name of the attribute
     * @param expectedValue expected value
     */
    public static void shouldHaveAttribute(final SelenideElement element, final String attributeName, final String expectedValue)
    {
        Neodymium.interaction().assertions().shouldHaveAttribute(element, attributeName, expectedValue);
    }
}
