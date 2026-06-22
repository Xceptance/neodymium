package com.xceptance.neodymium.junit4.testclasses.data.file.yaml;

import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/junit4/testclasses/data/set/yaml/CanReadDataSetYamlMulti.yaml")
public class CanReadDataSetYamlMulti
{
    @Test
    public void test()
    {
        final Map<String, String> data = Neodymium.getData();
        Assert.assertTrue(data.size() >= 3);
        
        final String testId = data.get("testId");
        if ("Iteration1".equals(testId))
        {
            Assert.assertEquals("john@example.com", data.get("user"));
            Assert.assertEquals("Verify login logic", data.get("steps"));
        }
        else if ("Iteration2".equals(testId))
        {
            Assert.assertEquals("jane@example.com", data.get("user"));
            Assert.assertEquals("Local override prompt logic", data.get("steps"));
        }
        else
        {
            Assert.fail("Unexpected testId: " + testId);
        }
    }
}
