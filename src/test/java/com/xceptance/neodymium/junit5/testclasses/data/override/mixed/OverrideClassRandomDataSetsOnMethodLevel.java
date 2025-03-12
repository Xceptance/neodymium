package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@SuppressBrowsers
@RandomDataSets(4)
public class OverrideClassRandomDataSetsOnMethodLevel
{
    @NeodymiumTest
    @RandomDataSets(0)
    public void test()
    {
    }
}
