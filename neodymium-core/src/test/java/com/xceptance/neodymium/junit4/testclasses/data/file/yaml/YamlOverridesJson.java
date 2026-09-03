package com.xceptance.neodymium.junit4.testclasses.data.file.yaml;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class YamlOverridesJson
{
    @Test
    public void testPriority()
    {
        Map<String, String> data = Neodymium.getData();
        Assert.assertEquals("yamlValue", data.get("priorityKey"));
    }
}
