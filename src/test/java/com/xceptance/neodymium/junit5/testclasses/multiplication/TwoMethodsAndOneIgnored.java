package com.xceptance.neodymium.junit5.testclasses.multiplication;

import org.junit.jupiter.api.Disabled;

import com.xceptance.neodymium.junit5.NeodymiumTest;

public class TwoMethodsAndOneIgnored
{
    @NeodymiumTest
    public void first()
    {

    }

    @NeodymiumTest
    public void second()
    {
    }

    @Disabled
    public void third()
    {

    }
}
