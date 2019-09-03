package com.xceptance.neodymium.testclasses.data.file.json;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.NeodymiumRunner;
import com.xceptance.neodymium.module.statement.testdata.DataFile;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/testclasses/data/set/json/CanReadDataSetJson.json")
public class CanReadDataSetJson
{
    @Test
    public void test()
    {
        Map<String, String> data = Neodymium.getData();
        Assert.assertEquals(2, data.size());
        Assert.assertEquals("Json Value1", data.get("testParam1"));
        Assert.assertEquals("Json Value2", data.get("testParam2"));
    }
}
