package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.DataUtils;

@SuppressBrowsers
@DataSet(2)
@DataSet(4)
@DataSet(6)
@DataSet(8)
public class MixRandomDataSetsFromRange
{
    @NeodymiumTest
    @RandomDataSets(4)
    public void test()
    {
        // assert test data is available for the test
        String key = DataUtils.asString("key1");
        Assert.assertTrue("Unexpected test data", key.contains("val"));
        Assert.assertTrue("Random data set is not selected from range", Integer.valueOf(key.replace("val", "")) % 2 == 0);
    }
}
