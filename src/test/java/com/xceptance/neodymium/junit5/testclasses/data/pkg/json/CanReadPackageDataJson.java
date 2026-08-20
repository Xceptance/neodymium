package com.xceptance.neodymium.junit5.testclasses.data.pkg.json;

import java.util.Map;

import org.junit.jupiter.api.Assertions;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

public class CanReadPackageDataJson
{
    @NeodymiumTest
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assertions.assertEquals(2, data.size());
        Assertions.assertEquals("Json Value1", data.get("pkgParam1"));
        Assertions.assertEquals("Json Value2", data.get("pkgParam2"));
    }
}
