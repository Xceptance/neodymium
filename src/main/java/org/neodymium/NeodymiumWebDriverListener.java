package org.neodymium;

import java.util.Collections;
import java.util.List;
import org.neodymium.util.ElementHighlightUtils;
import org.neodymium.util.Neodymium;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeodymiumWebDriverListener implements WebDriverListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumWebDriverListener.class);

    private static final ThreadLocal<Boolean> IS_HIGHLIGHTING = ThreadLocal.withInitial(() -> false);

    private boolean isHighlightOrOutlineSelector(final By by)
    {
        if (by == null)
        {
            return false;
        }
        final String selector = by.toString();
        return selector.contains("neodymium-highlight-box") || selector.contains("neodymium-outline-box");
    }

    @Override
    public void beforeFindElement(final WebDriver driver, final By by)
    {
        Neodymium.setLastUsedLocator(by);
    }

    @Override
    public void beforeFindElements(final WebDriver driver, final By by)
    {
        Neodymium.setLastUsedLocator(by);
    }

    @Override
    public void beforeFindElement(final WebElement element, final By locator)
    {
        Neodymium.setLastUsedLocator(element, locator);
    }

    @Override
    public void beforeFindElements(final WebElement element, final By locator)
    {
        Neodymium.setLastUsedLocator(element, locator);
    }

    @Override
    public void afterFindElement(final WebDriver driver, final By by, final WebElement result)
    {
        highlightResult(by, result != null ? Collections.singletonList(result) : null, driver);
    }

    @Override
    public void afterFindElements(final WebDriver driver, final By by, final List<WebElement> result)
    {
        highlightResult(by, result, driver);
    }

    @Override
    public void afterFindElement(final WebElement element, final By locator, final WebElement result)
    {
        highlightResult(locator, result != null ? Collections.singletonList(result) : null, Neodymium.hasDriver() ? Neodymium.getDriver() : null);
    }

    @Override
    public void afterFindElements(final WebElement element, final By locator, final List<WebElement> result)
    {
        highlightResult(locator, result, Neodymium.hasDriver() ? Neodymium.getDriver() : null);
    }

    private void highlightResult(final By by, final List<WebElement> elements, final WebDriver driver)
    {
        if (IS_HIGHLIGHTING.get() || elements == null || elements.isEmpty() || driver == null)
        {
            return;
        }

        try
        {
            if (Neodymium.configuration().debuggingHighlightSelectedElements() && !isHighlightOrOutlineSelector(by))
            {
                IS_HIGHLIGHTING.set(true);
                try
                {
                    ElementHighlightUtils.injectHighlightingJs();
                    ElementHighlightUtils.highlightAllElements(elements, driver);
                }
                finally
                {
                    IS_HIGHLIGHTING.set(false);
                }
            }
        }
        catch (final Throwable e)
        {
            LOGGER.debug("Could not highlight element: {}", e.getMessage());
        }
    }
}
