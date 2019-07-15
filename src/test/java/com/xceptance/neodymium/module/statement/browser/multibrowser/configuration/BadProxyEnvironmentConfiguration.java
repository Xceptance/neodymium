package com.xceptance.neodymium.module.statement.browser.multibrowser.configuration;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class BadProxyEnvironmentConfiguration
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
