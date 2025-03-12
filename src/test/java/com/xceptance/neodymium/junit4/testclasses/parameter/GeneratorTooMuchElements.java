package com.xceptance.neodymium.junit4.testclasses.parameter;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class GeneratorTooMuchElements
{
    @Parameters
    public static List<Integer[]> createData()
    {
        List<Integer[]> iterations = new ArrayList<>(1);
        iterations.add(new Integer[]
            {
                5, 6
            });

        return iterations;
    }

    @Parameter
    public Integer testInt;

    @Test
    public void test()
    {
        Assert.fail("method shouldn't be called");
    }
}
