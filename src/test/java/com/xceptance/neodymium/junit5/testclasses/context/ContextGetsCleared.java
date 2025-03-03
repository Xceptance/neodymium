package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.common.testdata.SuppressDataSets;
import com.xceptance.neodymium.util.Neodymium;

public class ContextGetsCleared
{
    @NeodymiumTest
    @DataSet
    public void test1() throws Exception
    {
        // there is one data set -> key1 = val1
        Assertions.assertEquals("val1", Neodymium.dataValue("key1"));
    }

    @NeodymiumTest
    @SuppressDataSets
    public void test2() throws Exception
    {
        // suppressing data sets makes sure we don't
        Assertions.assertNull(Neodymium.dataValue("key1"));
    }
}
