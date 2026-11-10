package com.xceptance.neodymium.junit5.testclasses.browser;

import java.util.ArrayList;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
@Browser("FF_1024x768")
@Browser("FF_1500x1000")
@RandomBrowsers(2)
public class FixedRandomnessOfRandomBrowser
{
    private static ArrayList<String> browsers = new ArrayList<>();

    @NeodymiumTest
    public void test1()
    {
        browsers.add(Neodymium.getBrowserProfileName());
    }

    @NeodymiumTest
    @SuppressBrowsers
    public void test2()
    {
        Assert.assertEquals(2, browsers.size());
        Assert.assertEquals("Chrome_1500x1000", browsers.get(0));
        Assert.assertEquals("FF_1500x1000", browsers.get(1));
    }
}
