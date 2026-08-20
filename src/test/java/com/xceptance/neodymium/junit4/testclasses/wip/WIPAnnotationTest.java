package com.xceptance.neodymium.junit4.testclasses.wip;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.WorkInProgress;
import org.neodymium.junit4.NeodymiumRunner;

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
