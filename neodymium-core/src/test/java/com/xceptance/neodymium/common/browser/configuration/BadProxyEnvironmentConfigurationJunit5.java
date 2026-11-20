/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.common.browser.configuration;

import java.util.Properties;

import org.junit.Assert;

import org.neodymium.junit5.NeodymiumTest;

public class BadProxyEnvironmentConfigurationJunit5
{
    @NeodymiumTest
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

    @NeodymiumTest
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

    @NeodymiumTest
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
