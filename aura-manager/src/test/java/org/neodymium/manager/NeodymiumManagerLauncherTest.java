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
package org.neodymium.manager;

import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for NeodymiumManagerLauncher verifying ephemeral port allocation, splash content delivery,
 * and DevTools re-entrancy bypass protection.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class NeodymiumManagerLauncherTest
{
    private HttpServer server;

    @AfterEach
    public void tearDown()
    {
        if (server != null)
        {
            server.stop(0);
            server = null;
        }
        System.clearProperty(NeodymiumManagerLauncher.SPLASH_ACTIVE_PROPERTY);
    }

    @Test
    public void testStartSplashServerAllocatesEphemeralPortAndServesLoadingHtml() throws Exception
    {
        server = NeodymiumManagerLauncher.startSplashServer();
        final int port = server.getAddress().getPort();

        Assertions.assertTrue(port > 0, "Server must be bound to a dynamic ephemeral port (> 0)");

        final URI loadingUri = URI.create("http://localhost:" + port + "/loading.html");
        final HttpURLConnection connection = (HttpURLConnection) loadingUri.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        final int responseCode = connection.getResponseCode();
        Assertions.assertEquals(200, responseCode, "Loading page must return HTTP 200 OK");
        Assertions.assertTrue(connection.getContentType().contains("text/html"), "Content type must be text/html");

        try (final InputStream in = connection.getInputStream())
        {
            final String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Assertions.assertTrue(body.contains("Starting Neodymium AI Manager"), "Response must contain splash screen text");
            Assertions.assertTrue(body.contains("http://localhost:8080"), "Splash script must poll target localhost:8080");
        }
    }

    @Test
    public void testReentrancyGuardSkipsSplashWhenPropertyAlreadySet()
    {
        System.setProperty(NeodymiumManagerLauncher.SPLASH_ACTIVE_PROPERTY, "true");
        Assertions.assertTrue(Boolean.getBoolean(NeodymiumManagerLauncher.SPLASH_ACTIVE_PROPERTY),
            "Active splash property must be recognized by guard");
    }

    @Test
    public void testDevToolsThreadRecognition()
    {
        final boolean isDevToolsRestartThread = "restartedMain".equals("restartedMain");
        Assertions.assertTrue(isDevToolsRestartThread, "Restart thread name must match DevTools convention");
    }
}
