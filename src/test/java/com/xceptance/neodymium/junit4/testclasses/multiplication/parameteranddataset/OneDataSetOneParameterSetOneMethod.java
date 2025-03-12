package com.xceptance.neodymium.junit4.testclasses.multiplication.parameteranddataset;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class OneDataSetOneParameterSetOneMethod
{
    @Parameters
    public static List<Object[]> createData()
    {
        List<Object[]> iterations = new LinkedList<>();

        iterations.add(new Object[]
            {
                "data"
            });

        return iterations;
    }

    @Parameter
    public String parameter;

    @Test
    public void first()
    {
    }
}
