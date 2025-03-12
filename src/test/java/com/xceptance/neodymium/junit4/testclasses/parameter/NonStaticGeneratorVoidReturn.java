package com.xceptance.neodymium.junit4.testclasses.parameter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class NonStaticGeneratorVoidReturn
{
    @Parameters
    public void createData()
    {
    }

    @Parameter
    private String testString;

    @Test
    public void test()
    {

    }
}
