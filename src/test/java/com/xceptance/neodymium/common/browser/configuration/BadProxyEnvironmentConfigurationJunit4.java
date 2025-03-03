package com.xceptance.neodymium.common.browser.configuration;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.xceptance.neodymium.junit5.NeodymiumTest;

public class BadProxyEnvironmentConfigurationJunit4
{
    @Test
    public void testProxyWithoutHost()
    {
        Properties properties = new Properties();
        properties.put("browserprofile.testEnvironment.badProxy.proxy", "true");
        properties.put("browserprofile.testEnvironment.badProxy.proxy.host", "");
        properties.put("browserprofile.testEnvironment.badProxy.proxy.port", "0815");
        try
        {
            new TestEnvironment(properties, "browserprofile.testEnvironment.badProxy");
        }
        catch (RuntimeException e)
        {
            Assert.assertEquals("The proxy host configured for environment: \"browserprofile.testEnvironment.badProxy\" needs to be set.", e.getMessage());
        }
    }

    @Test
    public void testProxyWithoutPort()
    {
        Properties properties = new Properties();
        properties.put("browserprofile.testEnvironment.badProxy.proxy", "true");
        properties.put("browserprofile.testEnvironment.badProxy.proxy.host", "somehost");
        try
        {
            new TestEnvironment(properties, "browserprofile.testEnvironment.badProxy");
        }
        catch (RuntimeException e)
        {
            Assert.assertEquals("The proxy port configured for environment: \"browserprofile.testEnvironment.badProxy\" needs to be set.", e.getMessage());
        }
    }

    @Test
    public void testProxyWithCharacterPort()
    {
        Properties properties = new Properties();
        properties.put("browserprofile.testEnvironment.badProxy.proxy", "true");
        properties.put("browserprofile.testEnvironment.badProxy.proxy.host", "somehost");
        properties.put("browserprofile.testEnvironment.badProxy.proxy.port", "NAN");
        try
        {
            new TestEnvironment(properties, "browserprofile.testEnvironment.badProxy");
        }
        catch (RuntimeException e)
        {
            Assert.assertEquals("The proxy port configured for environment: \"browserprofile.testEnvironment.badProxy\" needs to be an Integer.",
                                e.getMessage());
        }
    }
}
