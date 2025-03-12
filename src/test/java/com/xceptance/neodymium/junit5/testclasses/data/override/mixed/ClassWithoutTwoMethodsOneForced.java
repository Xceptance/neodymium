package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.common.testdata.SuppressDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@SuppressBrowsers
@SuppressDataSets
public class ClassWithoutTwoMethodsOneForced
{
    @NeodymiumTest
    @DataSet(1)
    public void test1() throws Exception
    {

    }

    @NeodymiumTest
    public void test2() throws Exception
    {

    }
}
