package com.xceptance.neodymium.junit5.testclasses.data.file.yaml;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

public class YamlOverridesJson
{
    @NeodymiumTest
    public void testPriority()
    {
        Map<String, String> data = Neodymium.getData();
        Assertions.assertEquals("yamlValue", data.get("priorityKey"));
    }
}
