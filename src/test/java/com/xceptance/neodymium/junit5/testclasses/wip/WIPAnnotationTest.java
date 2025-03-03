package com.xceptance.neodymium.junit5.testclasses.wip;

import com.xceptance.neodymium.common.WorkInProgress;
import com.xceptance.neodymium.junit5.NeodymiumTest;

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
