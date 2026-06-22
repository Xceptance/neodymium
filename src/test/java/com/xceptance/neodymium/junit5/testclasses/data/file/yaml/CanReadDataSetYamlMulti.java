package com.xceptance.neodymium.junit5.testclasses.data.file.yaml;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/set/yaml/CanReadDataSetYamlMulti.yaml")
public class CanReadDataSetYamlMulti
{
    @NeodymiumTest
    public void test()
    {
        final Map<String, String> data = Neodymium.getData();
        Assertions.assertTrue(data.size() >= 3);
        
        final String testId = data.get("testId");
        if ("Iteration1".equals(testId))
        {
            Assertions.assertEquals("john@example.com", data.get("user"));
            Assertions.assertEquals("Verify login logic", data.get("steps"));
        }
        else if ("Iteration2".equals(testId))
        {
            Assertions.assertEquals("jane@example.com", data.get("user"));
            Assertions.assertEquals("Local override prompt logic", data.get("steps"));
        }
        else
        {
            Assertions.fail("Unexpected testId: " + testId);
        }
    }
}
