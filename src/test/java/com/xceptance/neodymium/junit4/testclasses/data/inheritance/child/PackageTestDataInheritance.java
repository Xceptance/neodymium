package com.xceptance.neodymium.junit4.testclasses.data.inheritance.child;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class PackageTestDataInheritance
{
    @Test
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assert.assertEquals(3, data.size());
        Assert.assertEquals("child", data.get("pkgInherited"));
        Assert.assertEquals("parentValue", data.get("parentKey"));
        Assert.assertEquals("childValue", data.get("childKey"));
    }
}
