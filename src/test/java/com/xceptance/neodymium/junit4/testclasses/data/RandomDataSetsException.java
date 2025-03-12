package com.xceptance.neodymium.junit4.testclasses.data;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
@RandomDataSets(4)
public class RandomDataSetsException
{
    @Test
    public void test()
    {
    }
}
