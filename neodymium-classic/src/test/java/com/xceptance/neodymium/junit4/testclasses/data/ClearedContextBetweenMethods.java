package com.xceptance.neodymium.junit4.testclasses.data;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NeodymiumRunner.class)
public class ClearedContextBetweenMethods
{
    @Test
    public void test()
    {
        if (Neodymium.getData().asString("testId").equals("fist set"))
        {
            Assert.assertEquals("Test data is not matching the test expectations", "val1", Neodymium.getData().asString("key1"));
            Assert.assertEquals("Test data is not matching the test expectations", "val2", Neodymium.getData().asString("key2"));
        }
        else
        {
            Assert.assertEquals("Test data is not overwritten", "new val", Neodymium.getData().asString("key1"));
            Assert.assertNull("Test data context is not cleared", Neodymium.getData().asString("key2", null));
        }
    }

}
