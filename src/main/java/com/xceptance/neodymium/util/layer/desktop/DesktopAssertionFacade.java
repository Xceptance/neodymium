package com.xceptance.neodymium.util.layer.desktop;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.util.layer.AssertionFacade;

/**
 * Desktop-backed implementation of {@link AssertionFacade}.
 * <p>
 * Standard assertions are delegated to JUnit. Element-based assertions (should)
 * are not fully supported in the visual desktop layer as it lacks a DOM.
 * </p>
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class DesktopAssertionFacade implements AssertionFacade
{
    @Override
    public void assertTrue(final String message, final boolean condition)
    {
        Assertions.assertTrue(condition, message);
    }

    @Override
    public void assertTrue(final boolean condition)
    {
        Assertions.assertTrue(condition);
    }

    @Override
    public void assertEquals(final String message, final Object expected, final Object actual)
    {
        Assertions.assertEquals(expected, actual, message);
    }

    @Override
    public void assertEquals(final Object expected, final Object actual)
    {
        Assertions.assertEquals(expected, actual);
    }

    @Override
    public void should(final SelenideElement element, final String... conditions)
    {
        throw new UnsupportedOperationException("DOM element assertions (should) are not supported in the visual desktop layer.");
    }

    @Override
    public void should(final SelenideElement element, final Duration timeout, final String... conditions)
    {
        throw new UnsupportedOperationException("DOM element assertions (should) are not supported in the visual desktop layer.");
    }

    @Override
    public void shouldHaveAttribute(final SelenideElement element, final String attributeName, final String expectedValue)
    {
        throw new UnsupportedOperationException("Attribute assertions are not supported in the visual desktop layer.");
    }
}
