package com.xceptance.neodymium.junit4.testclasses.parameter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class GeneratorObjectReturn
{
    @Parameters
    public static Object createData()
    {
        return null;
    }

    @Parameter
    public Integer testInt;

    @Test
    public void test()
    {
        Assert.assertNull(testInt);
    }
}
