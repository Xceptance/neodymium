package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
@RunWith(NeodymiumRunner.class)
public class InstantiateDataSetExceptionNoDataSetFound
{
    @Test
    @DataSet(1)
    public void test1()
    {
        // there is no sixth data set (out of bounds)
    }
}