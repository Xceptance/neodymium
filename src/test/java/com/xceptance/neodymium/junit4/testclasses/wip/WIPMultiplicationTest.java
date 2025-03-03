package com.xceptance.neodymium.junit4.testclasses.wip;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.WorkInProgress;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@Browser("Chrome_headless")
@Browser("Chrome_1500x1000_headless")
@RunWith(NeodymiumRunner.class)
public class WIPMultiplicationTest
{
    @WorkInProgress
    @Test
    public void first()
    {
    }

    @Test
    public void second()
    {
    }
}
