package com.xceptance.neodymium.junit5.testclasses.wip;

import org.neodymium.common.WorkInProgress;
import org.neodymium.junit5.NeodymiumTest;

public class WIPAnnotationTest
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
