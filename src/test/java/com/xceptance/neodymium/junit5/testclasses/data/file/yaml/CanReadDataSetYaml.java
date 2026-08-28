package com.xceptance.neodymium.junit5.testclasses.data.file.yaml;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.neodymium.common.testdata.DataFile;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/set/yaml/CanReadDataSetYaml.yaml")
public class CanReadDataSetYaml
{
    @NeodymiumTest
    public void test()
    {
        final Map<String, String> data = Neodymium.getData();
        Assertions.assertEquals("john@example.com", data.get("user"));
        Assertions.assertEquals("password123", data.get("password"));
        Assertions.assertEquals("Verify login logic", data.get("steps"));
        Assertions.assertEquals(4, data.size());
    }
}
