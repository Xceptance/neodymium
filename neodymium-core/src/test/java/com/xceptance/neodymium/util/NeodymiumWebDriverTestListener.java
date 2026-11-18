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
package com.xceptance.neodymium.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.neodymium.NeodymiumWebDriverListener;

public class NeodymiumWebDriverTestListener extends NeodymiumWebDriverListener
{
    public int implicitWaitCount = 0;

    public NeodymiumWebDriverTestListener()
    {
        boolean allFound = true;
        final List<Method> parentMethods = Arrays.stream(NeodymiumWebDriverListener.class.getDeclaredMethods())
            .filter(m -> !m.isSynthetic() && !java.lang.reflect.Modifier.isPrivate(m.getModifiers()))
            .toList();
        final List<Method> childMethods = Arrays.stream(NeodymiumWebDriverTestListener.class.getDeclaredMethods())
            .filter(m -> !m.isSynthetic() && !java.lang.reflect.Modifier.isPrivate(m.getModifiers()))
            .toList();

        for (final Method parentMethod : parentMethods)
        {
            boolean foundMethod = false;
            for (final Method childMethod : childMethods)
            {
                if (parentMethod.getName().equals(childMethod.getName()))
                {
                    foundMethod = true;
                    break;
                }
            }
            allFound &= foundMethod;
        }
        Assert.assertEquals("Test classes do not contain the same number of listeners", parentMethods.size(), childMethods.size());
        Assert.assertTrue("Test classes do not contain the same listeners", allFound);
    }

    @Override
    public void beforeFindElement(final WebDriver driver, final By by)
    {
        implicitWaitCount++;
        super.beforeFindElement(driver, by);
    }

    @Override
    public void beforeFindElements(final WebDriver driver, final By by)
    {
        super.beforeFindElements(driver, by);
        implicitWaitCount++;
    }

    @Override
    public void beforeFindElement(final WebElement element, final By locator)
    {
        super.beforeFindElement(element, locator);
        // required to have equal number of methods with NeodymiumWebDriverListener
        SelenideAddons.$safe(() -> implicitWaitCount++);
    }

    @Override
    public void beforeFindElements(final WebElement element, final By locator)
    {
        super.beforeFindElements(element, locator);
        // required to have equal number of methods with NeodymiumWebDriverListener
        SelenideAddons.$safe(() -> implicitWaitCount++);
    }

    @Override
    public void afterFindElement(final WebDriver driver, final By by, final WebElement result)
    {
        super.afterFindElement(driver, by, result);
    }

    @Override
    public void afterFindElements(final WebDriver driver, final By by, final List<WebElement> result)
    {
        super.afterFindElements(driver, by, result);
    }

    @Override
    public void afterFindElement(final WebElement element, final By locator, final WebElement result)
    {
        super.afterFindElement(element, locator, result);
    }

    @Override
    public void afterFindElements(final WebElement element, final By locator, final List<WebElement> result)
    {
        super.afterFindElements(element, locator, result);
    }
}
