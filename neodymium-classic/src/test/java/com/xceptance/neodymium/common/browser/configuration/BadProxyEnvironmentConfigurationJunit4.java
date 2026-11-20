/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
