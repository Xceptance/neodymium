package com.xceptance.neodymium.junit4.testclasses.wip;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.WorkInProgress;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class WIPAnnotationTest

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
