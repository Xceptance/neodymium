package com.xceptance.neodymium.junit4.testclasses.data.set.xml;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class CanReadDataSetXML
{
    @Test
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assert.assertEquals(2, data.size());
        Assert.assertEquals("XML Value1", data.get("testParam1"));
        Assert.assertEquals("XML Value2", data.get("testParam2"));
    }
}
