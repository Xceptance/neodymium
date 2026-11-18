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
            if (Neodymium.shouldHighlightElements(driver) && !isHighlightOrOutlineSelector(by))
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
