package com.xceptance.neodymium.junit5.testclasses.data.inheritance.child;

import java.util.Map;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@SuppressBrowsers
public class PackageTestDataInheritance
{
    @NeodymiumTest
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assertions.assertEquals(3, data.size());
        Assertions.assertEquals("child", data.get("pkgInherited"));
        Assertions.assertEquals("parentValue", data.get("parentKey"));
        Assertions.assertEquals("childValue", data.get("childKey"));
    }
}
