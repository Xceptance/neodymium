package com.xceptance.neodymium.junit4.testclasses.data.file.yaml;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/junit4/testclasses/data/set/yaml/CanReadDataSetYaml.yaml")
public class CanReadDataSetYaml
{
    @Test
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assert.assertEquals("john@example.com", data.get("user"));
        Assert.assertEquals("password123", data.get("password"));
        Assert.assertEquals("Verify login logic", data.get("prompt"));
        Assert.assertEquals(3, data.size());
    }
}
