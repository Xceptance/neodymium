package com.xceptance.neodymium.junit4.testclasses.parameter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class GeneratorCanNotSetPrivateField
{
    @Parameters
    public static Object[] createData()
    {
        return new Object[]
            {
                123
            };
    }

    @Parameter(0)
    private int aPrivateInteger;

    @Test
    public void test()
    {
    }
}
