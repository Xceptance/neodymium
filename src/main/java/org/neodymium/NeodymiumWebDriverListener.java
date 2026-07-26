package org.neodymium;

import org.neodymium.util.ElementHighlightUtils;
import org.neodymium.util.Neodymium;
import org.neodymium.util.SelenideAddons;
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
                ElementHighlightUtils.injectHighlightingJs();
                ElementHighlightUtils.highlightAllElements(by, driver);
            }
        }
        catch (final Throwable e)
        {
            LOGGER.debug("Could not highlight element: {}", e.getMessage());
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
                ElementHighlightUtils.injectHighlightingJs();
                ElementHighlightUtils.highlightAllElements(by, driver);
            }
        }
        catch (final Throwable e)
        {
            LOGGER.debug("Could not highlight element: {}", e.getMessage());
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
                ElementHighlightUtils.injectHighlightingJs();
                SelenideAddons.$safe(() -> ElementHighlightUtils.highlightAllElements(element.findElements(locator), Neodymium.getDriver()));
            }
        }
        catch (final Throwable e)
        {
            LOGGER.debug("Could not highlight element: {}", e.getMessage());
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
                ElementHighlightUtils.injectHighlightingJs();
                SelenideAddons.$safe(() -> ElementHighlightUtils.highlightAllElements(element.findElements(locator), Neodymium.getDriver()));
            }
        }
        catch (final Throwable e)
        {
            LOGGER.debug("Could not highlight element: {}", e.getMessage());
        }
    }
}
