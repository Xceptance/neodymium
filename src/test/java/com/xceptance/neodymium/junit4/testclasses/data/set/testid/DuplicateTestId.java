package com.xceptance.neodymium.junit4.testclasses.data.set.testid;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class DuplicateTestId
{
    static int dataSet = 1;

    @Test
    public void testDuplicateTestId() throws Exception
    {
        if (dataSet == 1)
        {
            Assert.assertEquals("id1", Neodymium.dataValue("testId"));
            Assert.assertEquals("value1", Neodymium.dataValue("testParam1"));
        }
        else if (dataSet == 2)
        {
            Assert.assertEquals("id2", Neodymium.dataValue("testId"));
            Assert.assertEquals("value2", Neodymium.dataValue("testParam1"));
        }
        else if (dataSet == 3)
        {
            Assert.assertEquals("id1", Neodymium.dataValue("testId"));
            Assert.assertEquals("value3", Neodymium.dataValue("testParam1"));
        }
        dataSet++;
    }

    @Test
    @DataSet(id = "id1")
    public void testDuplicateTestIdByAnnotation1() throws Exception
    {
        Assert.assertEquals("id1", Neodymium.dataValue("testId"));
        Assert.assertEquals("value1", Neodymium.dataValue("testParam1"));
    }

    @Test
    @DataSet(id = "id2")
    public void testDuplicateTestIdByAnnotation2() throws Exception
    {
        Assert.assertEquals("id2", Neodymium.dataValue("testId"));
        Assert.assertEquals("value2", Neodymium.dataValue("testParam1"));
    }

    @Test
    @DataSet(3)
    public void testDuplicateTestIdByAnnotation3() throws Exception
    {
        Assert.assertEquals("id1", Neodymium.dataValue("testId"));
        Assert.assertEquals("value3", Neodymium.dataValue("testParam1"));
    }
}
