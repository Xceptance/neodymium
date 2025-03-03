package com.xceptance.neodymium.junit5.tests;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.DataUtils;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_headless")
public class CleanedContextBetweenMethodsTest
{
    @NeodymiumTest
    public void test()
    {
        if (DataUtils.asString("testId").equals("fist set"))
        {
            Assert.assertEquals("Test data is not matching the test expectations", "val1", DataUtils.asString("key1"));
            Assert.assertEquals("Test data is not matching the test expectations", "val2", DataUtils.asString("key2"));
        }
        else
        {
            Assert.assertEquals("Test data is not overwritten", "new val", DataUtils.asString("key1"));
            Assert.assertNull("Test data context is not cleared", DataUtils.asString("key2", null));
        }
    }

    @AfterEach
    public void after()
    {
        Assert.assertNotNull("Browser closed too early", Neodymium.getDriver());
    }
}
