package com.xceptance.neodymium.junit4.testclasses.context;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.neodymium.common.testdata.DataSet;
import org.neodymium.common.testdata.SuppressDataSets;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ContextGetsCleared
{
    @Test
    @DataSet
    public void test1() throws Exception
    {
        // there is one data set -> key1 = val1
        Assert.assertEquals("val1", Neodymium.dataValue("key1"));
    }

    @Test
    @SuppressDataSets
    public void test2() throws Exception
    {
        // suppressing data sets makes sure we don't
        Assert.assertNull(Neodymium.dataValue("key1"));
    }
}
