package com.xceptance.neodymium.junit5.testclasses.data.file.json;

import java.util.Map;

import org.junit.jupiter.api.Assertions;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/set/json/CanReadDataSetJson.json")
public class CanReadDataSetJson
{
    @NeodymiumTest
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assertions.assertEquals(2, data.size());
        Assertions.assertEquals("Json Value1", data.get("testParam1"));
        Assertions.assertEquals("Json Value2", data.get("testParam2"));
    }
}
