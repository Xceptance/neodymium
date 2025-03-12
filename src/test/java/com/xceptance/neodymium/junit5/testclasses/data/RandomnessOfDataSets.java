package com.xceptance.neodymium.junit5.testclasses.data;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.common.testdata.SuppressDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.DataUtils;

/**
 * This test may fail but the probability for it is very low
 */
@SuppressBrowsers
public class RandomnessOfDataSets
{
    private static List<String> datasets = new ArrayList<String>();

    @NeodymiumTest
    @RandomDataSets(10)
    public void testChoosingRandomDataSets()
    {
        // assert test data is available for the test
        Assert.assertTrue(DataUtils.asString("key1").contains("val"));
        datasets.add(DataUtils.asString("key1"));
    }

    @NeodymiumTest
    @SuppressDataSets
    public void testOrderOfChosenDataSetsHasChanged()
    {
        // assert that at least one of data sets is not on the same position as it stays in the test data sheet
        boolean changedOrder = false;
        for (int i = 1; i < datasets.size(); i++)
        {
            if (!datasets.get(i).equals("val" + (i + 1)))
            {
                changedOrder = true;
                break;
            }
        }
        Assert.assertTrue(changedOrder);
    }
}
