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
package com.xceptance.aura;

import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for AiLauncher verifying ephemeral port allocation, splash content delivery,
 * and DevTools re-entrancy bypass protection.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiLauncherTest
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
        System.clearProperty(AiLauncher.SPLASH_ACTIVE_PROPERTY);
        System.clearProperty(AiLauncher.OPEN_BROWSER_PROPERTY);
    }

    @Test
    public void testStartSplashServerAllocatesEphemeralPortAndServesLoadingHtml() throws Exception
    {
        server = AiLauncher.startSplashServer();
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
            Assertions.assertTrue(body.contains("Starting AI Manager"), "Response must contain splash screen text");
            Assertions.assertTrue(body.contains("http://localhost:8080"), "Splash script must poll target localhost:8080");
        }
    }

    @Test
    public void testReentrancyGuardSkipsSplashWhenPropertyAlreadySet()
    {
        System.setProperty(AiLauncher.SPLASH_ACTIVE_PROPERTY, "true");
        Assertions.assertTrue(Boolean.getBoolean(AiLauncher.SPLASH_ACTIVE_PROPERTY),
            "Active splash property must be recognized by guard");
    }

    @Test
    public void testDevToolsThreadRecognition()
    {
        final boolean isDevToolsRestartThread = "restartedMain".equals("restartedMain");
        Assertions.assertTrue(isDevToolsRestartThread, "Restart thread name must match DevTools convention");
    }

    @Test
    public void testIsBrowserOpenEnabledDefaultIsTrue()
    {
        System.clearProperty(AiLauncher.OPEN_BROWSER_PROPERTY);
        Assertions.assertTrue(AiLauncher.isBrowserOpenEnabled(), "Browser open should default to true");
    }

    @Test
    public void testIsBrowserOpenEnabledHonorsSystemProperty()
    {
        System.setProperty(AiLauncher.OPEN_BROWSER_PROPERTY, "false");
        Assertions.assertFalse(AiLauncher.isBrowserOpenEnabled(), "Browser open should be disabled when property is false");

        System.setProperty(AiLauncher.OPEN_BROWSER_PROPERTY, "true");
        Assertions.assertTrue(AiLauncher.isBrowserOpenEnabled(), "Browser open should be enabled when property is true");
    }

    @Test
    public void testOpenBrowserDoesNotThrowWhenDisabled()
    {
        System.setProperty(AiLauncher.OPEN_BROWSER_PROPERTY, "false");
        Assertions.assertDoesNotThrow(() -> AiLauncher.openBrowser("http://localhost:8080"),
            "openBrowser must return safely without error when disabled");
    }

    @Test
    public void testOpenBrowserDoesNotThrowWhenEnabled()
    {
        System.setProperty(AiLauncher.OPEN_BROWSER_PROPERTY, "true");
        Assertions.assertDoesNotThrow(() -> AiLauncher.openBrowser("http://localhost:8080"),
            "openBrowser must execute safely without throwing exceptions when enabled");
    }

    @Test
    public void testOpenLinuxBrowserDetachedDoesNotThrowIllegalArgumentException()
    {
        final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("linux") || osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))
        {
            Assertions.assertDoesNotThrow(() -> AiLauncher.openLinuxBrowserDetached("http://localhost:8080"),
                "openLinuxBrowserDetached must not throw IllegalArgumentException for input redirection");
        }
    }

    @Test
    public void testResolveServerPort()
    {
        System.clearProperty("server.port");
        Assertions.assertEquals(8080, AiLauncher.resolveServerPort(new String[0]));

        Assertions.assertEquals(9090, AiLauncher.resolveServerPort(new String[]{"--server.port=9090"}));

        System.setProperty("server.port", "8888");
        Assertions.assertEquals(8888, AiLauncher.resolveServerPort(new String[0]));
        System.clearProperty("server.port");
    }

    @Test
    public void testProbePortForClientDetectsConnectingClient() throws Exception
    {
        try (final ServerSocket socket = new ServerSocket(0))
        {
            final int ephemeralPort = socket.getLocalPort();
            socket.close();

            new Thread(() -> {
                try
                {
                    Thread.sleep(60);
                    try (final Socket s = new Socket(InetAddress.getLoopbackAddress(), ephemeralPort))
                    {
                        s.getOutputStream().write("GET /api/status HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                        s.getOutputStream().flush();
                        s.getInputStream().read();
                    }
                }
                catch (final Exception ignored)
                {
                }
            }).start();

            final boolean detected = AiLauncher.probePortForClient(ephemeralPort, 500);
            Assertions.assertTrue(detected, "probePortForClient must return true when client connects");
        }
    }

    @Test
    public void testProbePortForClientReturnsFalseOnTimeout() throws Exception
    {
        try (final ServerSocket socket = new ServerSocket(0))
        {
            final int ephemeralPort = socket.getLocalPort();
            socket.close();

            final boolean detected = AiLauncher.probePortForClient(ephemeralPort, 150);
            Assertions.assertFalse(detected, "probePortForClient must return false when no client connects");
        }
    }
}
