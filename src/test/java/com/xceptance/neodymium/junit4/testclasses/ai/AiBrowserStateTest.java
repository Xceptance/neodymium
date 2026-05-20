package com.xceptance.neodymium.junit4.testclasses.ai;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class AiBrowserStateTest
{
    @Test
    public void testAiBrowserInitialized()
    {
        Assert.assertNotNull("AiBrowser should be initialized and bound to the thread context", Neodymium.ai());
    }
}
