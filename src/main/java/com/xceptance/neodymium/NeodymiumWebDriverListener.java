package com.xceptance.neodymium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.util.DebugUtils;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.SelenideAddons;

public class NeodymiumWebDriverListener implements WebDriverListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumWebDriverListener.class);

    @Override
    public void beforeFindElement(WebDriver driver, By by)
    {
        Neodymium.setLastUsedLocator(by);
        try
        {
            if (Neodymium.configuration().debuggingHighlightSelectedElements())
            {
                DebugUtils.injectHighlightingJs();
                DebugUtils.highlightAllElements(by, driver);
            }
        }
        catch (Throwable e)
        {
            LOGGER.warn("Could not find element to highlight. If you don't need the highlight, please set the neodymium.debugUtils.highlight to false", e);
        }
    }

    @Override
    public void beforeFindElements(WebDriver driver, By by)
    {
        Neodymium.setLastUsedLocator(by);
        try
        {
            if (Neodymium.configuration().debuggingHighlightSelectedElements())
            {
                DebugUtils.injectHighlightingJs();
                DebugUtils.highlightAllElements(by, driver);
            }
        }
        catch (Throwable e)
        {
            LOGGER.warn("Could not find element to highlight. If you don't need the highlight, please set the neodymium.debugUtils.highlight to false", e);
        }
    }

    @Override
    public void beforeFindElement(WebElement element, By locator)
    {
        Neodymium.setLastUsedLocator(element, locator);
        try
        {
            if (Neodymium.configuration().debuggingHighlightSelectedElements())
            {
                DebugUtils.injectHighlightingJs();
                SelenideAddons.$safe(() -> DebugUtils.highlightAllElements(element.findElements(locator), Neodymium.getDriver()));
            }
        }
        catch (Throwable e)
        {
            LOGGER.warn("Could not find element to highlight. If you don't need the highlight, please set the neodymium.debugUtils.highlight to false", e);
        }
    }

    @Override
    public void beforeFindElements(WebElement element, By locator)
    {
        Neodymium.setLastUsedLocator(element, locator);
        try
        {
            if (Neodymium.configuration().debuggingHighlightSelectedElements())
            {
                DebugUtils.injectHighlightingJs();
                SelenideAddons.$safe(() -> DebugUtils.highlightAllElements(element.findElements(locator), Neodymium.getDriver()));
            }
        }
        catch (Throwable e)
        {
            LOGGER.warn("Could not find element to highlight. If you don't need the highlight, please set the neodymium.debugUtils.highlight to false", e);
        }
    }
}
