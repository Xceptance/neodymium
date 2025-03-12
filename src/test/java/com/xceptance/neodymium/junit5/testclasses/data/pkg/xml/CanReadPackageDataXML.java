package com.xceptance.neodymium.junit5.testclasses.data.pkg.xml;

import java.util.Map;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@SuppressBrowsers
public class CanReadPackageDataXML
{
    @NeodymiumTest
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assertions.assertEquals(2, data.size());
        Assertions.assertEquals("XML Value1", data.get("pkgParam1"));
        Assertions.assertEquals("XML Value2", data.get("pkgParam2"));
    }
}
