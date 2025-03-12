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
public class GeneratorTooFewElements
{
    @Parameters
    public static Iterable<Integer> createData()
    {
        List<Integer> list = new ArrayList<>(1);
        list.add(Integer.valueOf(5));
        return list;
    }

    @Parameter
    public Integer testInt;

    @Parameter(1)
    public Integer testInt2;

    @Test
    public void test()
    {
        Assert.fail("method shouldn't be called");
    }
}
