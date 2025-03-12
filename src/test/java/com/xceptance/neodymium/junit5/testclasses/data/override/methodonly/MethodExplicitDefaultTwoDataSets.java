package com.xceptance.neodymium.junit5.testclasses.data.override.methodonly;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@SuppressBrowsers
public class MethodExplicitDefaultTwoDataSets
{
    @NeodymiumTest
    @DataSet(0)
    public void test1() throws Exception
    {

    }
}
