package com.xceptance.neodymium.junit4.testclasses.parameter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class GeneratorVoidReturn
{
    @Parameters
    public static void createData()
    {
    }

    @Parameter
    private String testString;

    @Test
    public void test()
    {

    }
}
