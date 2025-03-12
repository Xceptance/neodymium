package com.xceptance.neodymium.junit4.testclasses.wip;

import org.junit.Test;

import com.xceptance.neodymium.common.WorkInProgress;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;

@SuppressBrowsers
public class WIPAnnotationChildTest extends WIPAnnotationTest
{
    @WorkInProgress
    @Test
    public void third()
    {
    }
}
