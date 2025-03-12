package com.xceptance.neodymium.junit5.testclasses.data.override.classonly;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@SuppressBrowsers
@DataSet(1)
@DataSet(1)
public class ClassMultipleSameDataSet
{
    @NeodymiumTest
    public void test1() throws Exception
    {

    }
}
