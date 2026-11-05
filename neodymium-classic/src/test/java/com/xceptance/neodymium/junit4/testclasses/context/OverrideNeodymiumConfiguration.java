package com.xceptance.neodymium.junit4.testclasses.context;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OverrideNeodymiumConfiguration
{
    @Test
    public void testStandardConfigurationStillAvailable() throws Exception
    {
        Assert.assertEquals("/some/internetexplorer/path/just/for/test/purpose", Neodymium.configuration().getIeDriverPath());
    }

    @Test
    public void testConfigurationContainsOverriddenParts() throws Exception
    {
        Assert.assertEquals("/some/edge/path/just/for/test/newPurpose", Neodymium.configuration().getEdgeDriverPath());
    }
}
