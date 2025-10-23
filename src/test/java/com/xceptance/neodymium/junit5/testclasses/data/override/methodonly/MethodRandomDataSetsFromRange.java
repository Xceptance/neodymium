package com.xceptance.neodymium.junit5.testclasses.data.override.methodonly;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Assert;

public class MethodRandomDataSetsFromRange
{
    @NeodymiumTest
    @DataSet(2)
    @DataSet(4)
    @DataSet(6)
    @DataSet(8)
    @RandomDataSets(4)
    public void test()
    {
        // assert test data is available for the test
        String key = Neodymium.getData().asString("key1");
        Assert.assertTrue("Unexpected test data", key.contains("val"));
        Assert.assertTrue("Random data set is not selected from range", Integer.valueOf(key.replace("val", "")) % 2 == 0);
    }
}
