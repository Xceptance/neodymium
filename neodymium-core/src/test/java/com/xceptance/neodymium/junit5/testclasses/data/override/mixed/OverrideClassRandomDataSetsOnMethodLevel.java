package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import org.neodymium.common.testdata.RandomDataSets;
import org.neodymium.junit5.NeodymiumTest;

@RandomDataSets(4)
public class OverrideClassRandomDataSetsOnMethodLevel
{
    @NeodymiumTest
    @RandomDataSets(0)
    public void test()
    {
    }
}
