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
package com.xceptance.neodymium.ai.util;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests port fallback mechanism and lifecycle of {@link EmbeddedHtmlServer}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class EmbeddedHtmlServerTest
{
    /**
     * Tests that creating a second server on an occupied port falls back to an available dynamic port.
     *
     * @throws IOException if server fails to initialize
     */
    @Test
    public void testPortFallbackWhenPortOccupied() throws IOException
    {
        final EmbeddedHtmlServer primaryServer = new EmbeddedHtmlServer();
        primaryServer.start();

        try
        {
            final int primaryPort = primaryServer.getPort();
            final int primaryHttpsPort = primaryServer.getHttpsPort();

            Assertions.assertTrue(primaryPort > 0, "Primary HTTP port should be valid");
            Assertions.assertTrue(primaryHttpsPort > 0, "Primary HTTPS port should be valid");

            // Attempt to create secondary server requesting the exact same ports
            final EmbeddedHtmlServer secondaryServer = new EmbeddedHtmlServer(primaryPort, primaryHttpsPort);
            secondaryServer.start();

            try
            {
                final int secondaryPort = secondaryServer.getPort();
                final int secondaryHttpsPort = secondaryServer.getHttpsPort();

                Assertions.assertTrue(secondaryPort > 0, "Secondary HTTP port should be valid");
                Assertions.assertTrue(secondaryHttpsPort > 0, "Secondary HTTPS port should be valid");
                Assertions.assertNotEquals(primaryPort, secondaryPort, "Secondary server should bind to a fallback port");
                Assertions.assertNotEquals(primaryHttpsPort, secondaryHttpsPort, "Secondary server HTTPS should bind to a fallback port");
            }
            finally
            {
                secondaryServer.stop();
            }
        }
        finally
        {
            primaryServer.stop();
        }
    }
}
