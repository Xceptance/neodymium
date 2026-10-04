/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.ai.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Unit test suite validating {@link InteractiveConsoleServer} network binding and CORS origin isolation.
 *
 * @author AI-generated: Claude Sonnet 4.5
 * @author Xceptance GmbH 2026
 */
public class InteractiveConsoleServerTest
{
    private String originalBindAddress;

    @BeforeEach
    public void setUp()
    {
        originalBindAddress = System.getProperty("neodymium.ai.console.bindAddress");
        AiConfiguration.resetInstance();
    }

    @AfterEach
    public void tearDown()
    {
        if (originalBindAddress != null)
        {
            System.setProperty("neodymium.ai.console.bindAddress", originalBindAddress);
        }
        else
        {
            System.clearProperty("neodymium.ai.console.bindAddress");
        }
        AiConfiguration.resetInstance();
    }

    @Test
    public void testIsLocalhostOriginAllowsLocalOrigins()
    {
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("http://localhost"));
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("http://localhost:8080"));
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("https://localhost:18090"));
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("http://127.0.0.1"));
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("http://127.0.0.1:18090"));
        assertTrue(InteractiveConsoleServer.isLocalhostOrigin("null"));
    }

    @Test
    public void testIsLocalhostOriginRejectsExternalOrigins()
    {
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin("https://evil.com"));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin("http://evil.com:8080"));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin("http://localhost.evil.com"));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin("http://127.0.0.1.attacker.org"));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin("http://192.168.1.10:8080"));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin(null));
        assertFalse(InteractiveConsoleServer.isLocalhostOrigin(""));
    }

    @Test
    public void testDefaultBindingIsLoopback() throws IOException
    {
        final InteractiveConsoleEngine engine = new InteractiveConsoleEngine("test-bind");
        InteractiveConsoleServer server = null;
        try
        {
            server = new InteractiveConsoleServer(engine);
            final InetSocketAddress address = server.getAddress();
            assertNotNull(address);
            assertTrue(address.getAddress().isLoopbackAddress(), "Server must bind to a loopback address by default");
            assertEquals("127.0.0.1", address.getAddress().getHostAddress());
        }
        finally
        {
            if (server != null)
            {
                server.stop();
            }
        }
    }
}
