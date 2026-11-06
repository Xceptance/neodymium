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
        Method[] parentMethods = NeodymiumWebDriverListener.class.getDeclaredMethods();
        Method[] childMethods = NeodymiumWebDriverTestListener.class.getDeclaredMethods();

        for (int i = 0; i < parentMethods.length; i++)
        {
            boolean foundMethod = false;
            for (int j = 0; j < childMethods.length; j++)
            {
                if (parentMethods[i].getName().equals(childMethods[j].getName()))
                {
                    foundMethod = true;
                    break;
                }
            }
            allFound &= foundMethod;
        }
        Assert.assertEquals("Test classes do not contain the same number of listeners", parentMethods.length, childMethods.length);
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
