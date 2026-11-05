package com.xceptance.neodymium.junit5.testclasses.multibrowser;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserWithoutAvailableEnvironment
{
    @NeodymiumTest
    @Browser("Galaxy_Note3_Emulation")
    public void testWithUnavailableEnvironment()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Assertions.assertEquals("Galaxy_Note3_Emulation", Neodymium.getBrowserProfileName());
        });
    }
}
