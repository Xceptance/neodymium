package com.xceptance.neodymium.junit5.testclasses.wip;

import org.neodymium.common.WorkInProgress;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
@Browser("Chrome_1500x1000_headless")
public class WIPMultiplicationTest
{
    @WorkInProgress
    @NeodymiumTest
    public void first()
    {
    }

    @NeodymiumTest
    public void second()
    {
    }
}
