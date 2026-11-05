package com.xceptance.neodymium.junit5.testclasses.data.override.classonly;

import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Assert;

@RandomDataSets(4)
public class ClassRandomDataSets
{
    @NeodymiumTest
    public void test()
    {
        // assert test data is available for the test
        Assert.assertTrue(Neodymium.getData().asString("key1").contains("val"));
    }
}
