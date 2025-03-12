package com.xceptance.neodymium.junit4.testclasses.data.pkg.properties;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class CanReadPackageDataProperties
{
    @Test
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assert.assertEquals(2, data.size());
        Assert.assertEquals("Properties Value1", data.get("pkgParam1"));
        Assert.assertEquals("Properties Value2", data.get("pkgParam2"));
    }
}
