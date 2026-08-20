package com.xceptance.neodymium.junit4.testclasses.data.override.mixed;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.testdata.RandomDataSets;
import org.neodymium.junit4.NeodymiumRunner;

@RandomDataSets(4)
@RunWith(NeodymiumRunner.class)
public class OverrideClassRandomDataSetsOnMethodLevel
{
    @Test
    @RandomDataSets(0)
    public void test()
    {
    }
}
