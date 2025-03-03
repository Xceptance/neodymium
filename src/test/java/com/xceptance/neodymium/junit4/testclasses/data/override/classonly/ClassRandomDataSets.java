package com.xceptance.neodymium.junit4.testclasses.data.override.classonly;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.DataUtils;

@RunWith(NeodymiumRunner.class)
@RandomDataSets(4)
public class ClassRandomDataSets
{
    @Test
    public void test()
    {
        // assert test data is available for the test
        Assert.assertTrue(DataUtils.asString("key1").contains("val"));
    }
}
