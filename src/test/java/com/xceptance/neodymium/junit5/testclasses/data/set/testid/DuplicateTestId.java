package com.xceptance.neodymium.junit5.testclasses.data.set.testid;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.util.Neodymium;

public class DuplicateTestId
{
    static int dataSet = 1;

    @NeodymiumTest
    public void testDuplicateTestId() throws Exception
    {
        if (dataSet == 1)
        {
            Assertions.assertEquals("id1", Neodymium.dataValue("testId"));
            Assertions.assertEquals("value1", Neodymium.dataValue("testParam1"));
        }
        else if (dataSet == 2)
        {
            Assertions.assertEquals("id2", Neodymium.dataValue("testId"));
            Assertions.assertEquals("value2", Neodymium.dataValue("testParam1"));
        }
        else if (dataSet == 3)
        {
            Assertions.assertEquals("id1", Neodymium.dataValue("testId"));
            Assertions.assertEquals("value3", Neodymium.dataValue("testParam1"));
        }
        dataSet++;
    }

    @NeodymiumTest
    @DataSet(id = "id1")
    public void testDuplicateTestIdByAnnotation1() throws Exception
    {
        Assertions.assertEquals("id1", Neodymium.dataValue("testId"));
        Assertions.assertEquals("value1", Neodymium.dataValue("testParam1"));
    }

    @NeodymiumTest
    @DataSet(id = "id2")
    public void testDuplicateTestIdByAnnotation2() throws Exception
    {
        Assertions.assertEquals("id2", Neodymium.dataValue("testId"));
        Assertions.assertEquals("value2", Neodymium.dataValue("testParam1"));
    }

    @NeodymiumTest
    @DataSet(3)
    public void testDuplicateTestIdByAnnotation3() throws Exception
    {
        Assertions.assertEquals("id1", Neodymium.dataValue("testId"));
        Assertions.assertEquals("value3", Neodymium.dataValue("testParam1"));
    }
}
