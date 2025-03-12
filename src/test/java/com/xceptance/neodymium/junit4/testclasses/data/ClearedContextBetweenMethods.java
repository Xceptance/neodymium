package com.xceptance.neodymium.junit4.testclasses.data;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.DataUtils;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class ClearedContextBetweenMethods
{
    @Test
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

}
