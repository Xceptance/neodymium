package com.xceptance.neodymium.junit4.testclasses.multibrowser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class BrowserWithoutAvailableEnvironment
{
    @Test
    @Browser("Galaxy_Note3_Emulation")
    public void testWithUnavailableEnvironment()
    {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            Assert.assertEquals("Galaxy_Note3_Emulation", Neodymium.getBrowserProfileName());
        });
    }
}
