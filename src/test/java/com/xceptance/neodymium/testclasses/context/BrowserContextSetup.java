package com.xceptance.neodymium.testclasses.context;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.NeodymiumRunner;
import com.xceptance.neodymium.module.statement.browser.multibrowser.Browser;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class BrowserContextSetup
{
    @BeforeClass
    public static void beforeClass()
    {
        Assert.assertNull(Neodymium.getBrowserProfileName());
        Assert.assertNull(Neodymium.getBrowserName());
    }

    @Test
    public void test1() throws Exception
    {
        Assert.assertEquals("Chrome_headless", Neodymium.getBrowserProfileName());
        Assert.assertEquals("chrome", Neodymium.getBrowserName());
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertNull(Neodymium.getBrowserProfileName());
        Assert.assertNull(Neodymium.getBrowserName());
    }
}
