/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
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

    @Override
    public void beforeFindElement(WebDriver driver, By by)
    {
        Neodymium.setLastUsedLocator(by);
        try
        {
            if (Neodymium.shouldHighlightElements(driver))
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
            if (Neodymium.shouldHighlightElements(driver))
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
            if (Neodymium.shouldHighlightElements(Neodymium.hasDriver() ? Neodymium.getDriver() : null) && Neodymium.hasDriver())
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
            if (Neodymium.shouldHighlightElements(Neodymium.hasDriver() ? Neodymium.getDriver() : null) && Neodymium.hasDriver())
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
