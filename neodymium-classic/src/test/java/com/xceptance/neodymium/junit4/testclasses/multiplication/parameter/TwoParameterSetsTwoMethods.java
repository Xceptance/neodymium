package com.xceptance.neodymium.junit4.testclasses.multiplication.parameter;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class TwoParameterSetsTwoMethods
{
    @Parameters
    public static List<Object[]> createData()
    {
        List<Object[]> iterations = new LinkedList<>();
        Object[] data = new Object[]
            {
                "data"
            };
        iterations.add(data);
        iterations.add(data);

        return iterations;
    }

    @Parameter
    public String parameter;

    @Test
    public void first()
    {

    }

    @Test
    public void second()
    {

    }
}
