package com.xceptance.neodymium.junit5.testclasses.multiplication;

import org.junit.jupiter.api.Disabled;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@SuppressBrowsers
public class TwoMethodsOneIgnored
{
    @NeodymiumTest
    public void first()
    {

    }

    @NeodymiumTest
    @Disabled
    public void second()
    {
    }
}
