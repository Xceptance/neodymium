package com.xceptance.neodymium.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class JavaScriptUtilsTest
{
    @Test
    public void testWaitingAnimationSelectorUnconfigured()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");
        Selenide.open("https://blog.xceptance.com/");
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("The waiting animation selector is not set, timeout shouldn't be reached.", end - start < Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingAnimationSelectorExistsOnPage()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.animationSelector", "#main-content");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");

        Selenide.open("https://blog.xceptance.com/");
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("The waiting animation selector is available on the site, timeout should be reached.",
                   end - start > Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingAnimationSelectorUnavailableOnPage()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.animationSelector", ".cantFindThisClass");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");

        Selenide.open("https://blog.xceptance.com/");
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("The waiting animation selector is unavailable on the site, timeout shouldn't be reached.",
                   end - start < Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingJQueryIsRequired()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.jQueryIsRequired", "true");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");

        Selenide.open("https://www.google.com/");
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("jQuery is unavailable on the site, timeout should be reached.",
                   end - start > Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingJQueryIsNotRequired()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.jQueryIsRequired", "false");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");

        Selenide.open("https://www.google.com/");
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("jQuery is  not required, so the timeout shouldn't be reached.",
                   end - start < Neodymium.configuration().javaScriptTimeout());
    }
}
