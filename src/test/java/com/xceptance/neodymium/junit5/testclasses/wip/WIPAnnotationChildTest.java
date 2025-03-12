package com.xceptance.neodymium.junit5.testclasses.wip;

import com.xceptance.neodymium.common.WorkInProgress;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@SuppressBrowsers
public class WIPAnnotationChildTest extends WIPAnnotationTest
{
    @WorkInProgress
    @NeodymiumTest
    public void third()
    {
    }
}
