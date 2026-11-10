package com.xceptance.neodymium.junit4.testclasses.parameter;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class GeneratorCanSetStaticField
{
    @Parameters
    public static List<Object> createData()
    {
        List<Object> data = new ArrayList<>(1);
        data.add("a string");

        return data;
    }

    @Parameter
    public static String aString;

    @Test
    public void test()
    {
        Assert.assertEquals("a string", aString);
    }
}
