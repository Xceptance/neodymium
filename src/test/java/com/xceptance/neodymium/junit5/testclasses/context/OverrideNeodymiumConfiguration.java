package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@SuppressBrowsers
public class OverrideNeodymiumConfiguration
{
    @NeodymiumTest
    public void testStandardConfigurationStillAvailable() throws Exception
    {
        System.out.println(Neodymium.configuration().getIeDriverPath());
        Assertions.assertEquals("/some/internetexplorer/path/just/for/test/purpose", Neodymium.configuration().getIeDriverPath());
    }

    @NeodymiumTest
    public void testConfigurationContainsOverriddenParts() throws Exception
    {
        System.out.println(Neodymium.configuration().getEdgeDriverPath());
        Assertions.assertEquals("/some/edge/path/just/for/test/newPurpose", Neodymium.configuration().getEdgeDriverPath());
    }
}
