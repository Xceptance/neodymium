package com.xceptance.neodymium;

import com.xceptance.neodymium.util.DebugUtils;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.SelenideAddons;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeodymiumWebDriverListener implements WebDriverListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumWebDriverListener.class);

    private static final ThreadLocal<By> lastHighlightedLocator = new ThreadLocal<>();
    private static final ThreadLocal<Long> lastHighlightTimestamp = new ThreadLocal<>();

    private boolean isHighlightOrOutlineSelector(final By by)
    {
        if (by == null)
        {
            return false;
        }
        final String selector = by.toString();
        return selector.contains("neodymium-highlight-box") || selector.contains("neodymium-outline-box");
    }

    private boolean shouldTriggerHighlight(final By locator)
    {
        if (locator == null)
        {
            return true;
        }
        final long now = System.currentTimeMillis();
        final By lastLocator = lastHighlightedLocator.get();
        final Long lastTimestamp = lastHighlightTimestamp.get();
        if (locator.equals(lastLocator) && lastTimestamp != null && (now - lastTimestamp < 2000))
        {
            return false;
        }
        lastHighlightedLocator.set(locator);
        lastHighlightTimestamp.set(now);
        return true;
    }

    @Override
    public void beforeFindElement(final WebDriver driver, final By by)
    {
        Neodymium.setLastUsedLocator(by);
        try
        {
            if (Neodymium.configuration().debuggingHighlightSelectedElements() && !isHighlightOrOutlineSelector(by) && shouldTriggerHighlight(by))
            {
                DebugUtils.injectHighlightingJs();
                DebugUtils.highlightAllElements(by, driver);
            }
        }
        catch (final Throwable e)
        {
            LOGGER.warn("Could not find element to highlight. If you don't need the highlight, please set the neodymium.debugUtils.highlight to false", e);
        }
    }

    @Override
    public void beforeFindElements(final WebDriver driver, final By by)
    {
        Neodymium.setLastUsedLocator(by);
        try
        {
            if (Neodymium.configuration().debuggingHighlightSelectedElements() && !isHighlightOrOutlineSelector(by) && shouldTriggerHighlight(by))
            {
                DebugUtils.injectHighlightingJs();
                DebugUtils.highlightAllElements(by, driver);
            }
        }
        catch (final Throwable e)
        {
            LOGGER.warn("Could not find element to highlight. If you don't need the highlight, please set the neodymium.debugUtils.highlight to false", e);
        }
    }

    @Override
    public void beforeFindElement(final WebElement element, final By locator)
    {
        Neodymium.setLastUsedLocator(element, locator);
        try
        {
            if (Neodymium.configuration().debuggingHighlightSelectedElements() && Neodymium.hasDriver() && !isHighlightOrOutlineSelector(locator) && shouldTriggerHighlight(locator))
            {
                DebugUtils.injectHighlightingJs();
                SelenideAddons.$safe(() -> DebugUtils.highlightAllElements(element.findElements(locator), Neodymium.getDriver()));
            }
        }
        catch (final Throwable e)
        {
            LOGGER.warn("Could not find element to highlight. If you don't need the highlight, please set the neodymium.debugUtils.highlight to false", e);
        }
    }

    @Override
    public void beforeFindElements(final WebElement element, final By locator)
    {
        Neodymium.setLastUsedLocator(element, locator);
        try
        {
            if (Neodymium.configuration().debuggingHighlightSelectedElements() && Neodymium.hasDriver() && !isHighlightOrOutlineSelector(locator) && shouldTriggerHighlight(locator))
            {
                DebugUtils.injectHighlightingJs();
                SelenideAddons.$safe(() -> DebugUtils.highlightAllElements(element.findElements(locator), Neodymium.getDriver()));
            }
        }
        catch (final Throwable e)
        {
            LOGGER.warn("Could not find element to highlight. If you don't need the highlight, please set the neodymium.debugUtils.highlight to false", e);
        }
    }
}
