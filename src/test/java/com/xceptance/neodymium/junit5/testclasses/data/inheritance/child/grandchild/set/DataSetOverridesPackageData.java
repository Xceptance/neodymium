package com.xceptance.neodymium.junit5.testclasses.data.inheritance.child.grandchild.set;

import java.util.Map;

import org.junit.jupiter.api.Assertions;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

public class DataSetOverridesPackageData
{
    @NeodymiumTest
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assertions.assertEquals(4, data.size());
        Assertions.assertEquals("dataSetValue", data.get("pkgInherited"));
        Assertions.assertEquals("parentValue", data.get("parentKey"));
        Assertions.assertEquals("childValue", data.get("childKey"));
        Assertions.assertEquals("grandChildValue", data.get("grandChildKey"));
    }
}
