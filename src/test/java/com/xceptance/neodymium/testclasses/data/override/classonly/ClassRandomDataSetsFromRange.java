package com.xceptance.neodymium.testclasses.data.override.classonly;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.NeodymiumRunner;
import com.xceptance.neodymium.module.statement.testdata.DataSet;
import com.xceptance.neodymium.module.statement.testdata.RandomDataSets;
import com.xceptance.neodymium.util.DataUtils;

@RunWith(NeodymiumRunner.class)
@DataSet(2)
@DataSet(4)
@DataSet(6)
@DataSet(8)
@RandomDataSets(4)
public class ClassRandomDataSetsFromRange
{
    @Test
    public void test()
    {
        // assert test data is available for the test
        String key = DataUtils.asString("key1");
        Assert.assertTrue("Unexpected test data", key.contains("val"));
        Assert.assertTrue("Random data set is not selected from range", Integer.valueOf(key.replace("val", "")) % 2 == 0);
    }
}
