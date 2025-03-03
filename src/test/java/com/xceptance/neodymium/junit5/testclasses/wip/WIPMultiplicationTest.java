package com.xceptance.neodymium.junit5.testclasses.wip;

import com.xceptance.neodymium.common.WorkInProgress;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

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
