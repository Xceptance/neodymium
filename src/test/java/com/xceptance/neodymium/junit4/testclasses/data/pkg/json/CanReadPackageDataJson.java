package com.xceptance.neodymium.junit4.testclasses.data.pkg.json;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class CanReadPackageDataJson
{
    @Test
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assert.assertEquals(2, data.size());
        Assert.assertEquals("Json Value1", data.get("pkgParam1"));
        Assert.assertEquals("Json Value2", data.get("pkgParam2"));
    }
}
