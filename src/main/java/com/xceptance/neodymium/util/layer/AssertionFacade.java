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
import com.codeborne.selenide.SelenideElement;

/**
 * Interface for backend-agnostic assertions.
 * <p>
 * This facade decouples test-level assertions from the underlying driver (e.g., Selenide, Playwright).
 * Implementations are responsible for performing the check and handling errors (e.g., taking screenshots).
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public interface AssertionFacade
{
    /**
     * Asserts that a condition is true.
     *
     * @param message the failure message
     * @param condition the condition to check
     */
    void assertTrue(final String message, final boolean condition);

    /**
     * Asserts that a condition is true.
     *
     * @param condition the condition to check
     */
    void assertTrue(final boolean condition);

    /**
     * Asserts that two objects are equal.
     *
     * @param message the failure message
     * @param expected the expected value
     * @param actual the actual value
     */
    void assertEquals(final String message, final Object expected, final Object actual);

    /**
     * Asserts that two objects are equal.
     *
     * @param expected the expected value
     * @param actual the actual value
     */
    void assertEquals(final Object expected, final Object actual);

    /**
     * Asserts that an element matches specific conditions (Selenide-style 'should').
     *
     * @param element the element to check
     * @param conditions the conditions to verify (e.g., "exist", "visible")
     */
    void should(final SelenideElement element, final String... conditions);

    /**
     * Asserts that an element matches specific conditions within a timeout.
     *
     * @param element the element to check
     * @param timeout the maximum time to wait
     * @param conditions the conditions to verify
     */
    void should(final SelenideElement element, final Duration timeout, final String... conditions);

    /**
     * Asserts that an element has a specific attribute value.
     *
     * @param element the element to check
     * @param attributeName the name of the attribute
     * @param expectedValue the expected attribute value
     */
    void shouldHaveAttribute(final SelenideElement element, final String attributeName, final String expectedValue);
}
