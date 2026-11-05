package com.xceptance.neodymium.junit4.testclasses.multiplication.browser;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("first_browser")
@Browser("second_browser")
public class TwoBrowserOneMethod
{
    @Test
    public void first()
    {
    }
}
