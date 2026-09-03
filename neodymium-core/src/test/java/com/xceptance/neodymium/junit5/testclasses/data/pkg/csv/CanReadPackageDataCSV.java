package com.xceptance.neodymium.junit5.testclasses.data.pkg.csv;

import java.util.Map;

import org.junit.jupiter.api.Assertions;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

public class CanReadPackageDataCSV
{
    @NeodymiumTest
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assertions.assertEquals(2, data.size());
        Assertions.assertEquals("CSV Value1", data.get("pkgParam1"));
        Assertions.assertEquals("CSV Value2", data.get("pkgParam2"));
    }
}
