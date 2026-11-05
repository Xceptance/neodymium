package com.xceptance.neodymium.junit4.testclasses.multiplication;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class TwoMethodsOneIgnored
{
    @Test
    public void first()
    {

    }

    @Test
    @Ignore
    public void second()
    {
    }
}
