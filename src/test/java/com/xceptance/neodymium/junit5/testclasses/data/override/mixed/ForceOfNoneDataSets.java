package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@SuppressBrowsers
public class ForceOfNoneDataSets
{
    @NeodymiumTest
    @DataSet(2)
    public void test1() throws Exception
    {

    }
}
