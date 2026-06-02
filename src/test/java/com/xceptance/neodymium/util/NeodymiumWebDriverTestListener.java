package com.xceptance.neodymium.util;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.xceptance.neodymium.NeodymiumWebDriverListener;

public class NeodymiumWebDriverTestListener extends NeodymiumWebDriverListener
{
    public int implicitWaitCount = 0;

    public NeodymiumWebDriverTestListener()
    {
        boolean allFound = true;
        final java.util.List<Method> parentMethods = java.util.Arrays.stream(NeodymiumWebDriverListener.class.getDeclaredMethods())
            .filter(m -> !m.isSynthetic() && !java.lang.reflect.Modifier.isPrivate(m.getModifiers()))
            .toList();
        final java.util.List<Method> childMethods = java.util.Arrays.stream(NeodymiumWebDriverTestListener.class.getDeclaredMethods())
            .filter(m -> !m.isSynthetic() && !java.lang.reflect.Modifier.isPrivate(m.getModifiers()))
            .toList();

        for (Method parentMethod : parentMethods)
        {
            boolean foundMethod = false;
            for (Method childMethod : childMethods)
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
    public void beforeFindElement(WebDriver driver, By by)
    {
        implicitWaitCount++;
        super.beforeFindElement(driver, by);
    }

    @Override
    public void beforeFindElements(WebDriver driver, By by)
    {
        super.beforeFindElements(driver, by);
        implicitWaitCount++;
    }

    @Override
    public void beforeFindElement(WebElement element, By locator)
    {
        super.beforeFindElement(element, locator);
        // required to have equal number of methods with NeodymiumWebDriverListener
        SelenideAddons.$safe(() -> implicitWaitCount++);
    }

    @Override
    public void beforeFindElements(WebElement element, By locator)
    {
        super.beforeFindElements(element, locator);
        // required to have equal number of methods with NeodymiumWebDriverListener
        SelenideAddons.$safe(() -> implicitWaitCount++);
    }
}
