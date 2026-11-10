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
package com.xceptance.aura;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Launcher for Neodymium Aura Manager.
 * Launches a lightweight splash server on an ephemeral port to provide immediate visual feedback
 * in the user's browser while Spring Boot initializes in the background.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AuraLauncher
{
    static final String SPLASH_ACTIVE_PROPERTY = "aura.launcher.splash.active";

    public static void main(final String[] args) throws Exception
    {
        // When Spring Boot DevTools restart mechanism triggers, it re-invokes main on the "restartedMain" thread.
        // Guard against secondary server creation and duplicate browser tabs by directly delegating to Spring Boot.
        if (Boolean.getBoolean(SPLASH_ACTIVE_PROPERTY) || "restartedMain".equals(Thread.currentThread().getName()))
        {
            AuraManagerApplication.main(args);
            return;
        }

        System.setProperty(SPLASH_ACTIVE_PROPERTY, "true");

        // 1. Start a lightweight HTTP server on an ephemeral port (port 0) on the loopback interface
        final HttpServer server = startSplashServer();
        final int tempPort = server.getAddress().getPort();
        final String loadingUrl = "http://localhost:" + tempPort + "/loading.html";

        // Register JVM shutdown hook to clean up the temporary server upon termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

        // 2. Open default browser to the loading splash screen
        openBrowser(loadingUrl);

        // 3. Hand off control to the Spring Boot application
        try
        {
            AuraManagerApplication.main(args);
        }
        finally
        {
            server.stop(0);
        }
    }

    /**
     * Starts the lightweight HTTP server to serve the splash screen on an ephemeral port.
     *
     * @return the running HttpServer instance
     * @throws IOException if server socket initialization fails
     */
    static HttpServer startSplashServer() throws IOException
    {
        final HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/loading.html", new LoadingHandler());
        server.setExecutor(null);
        server.start();
        return server;
    }

    /**
     * Attempts to open the specified URL in the user's default web browser.
     *
     * @param url target URL to navigate to
     */
    static void openBrowser(final String url)
    {
        try
        {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            {
                Desktop.getDesktop().browse(new URI(url));
            }
            else
            {
                // Linux / headless fallback
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        }
        catch (final Exception e)
        {
            System.err.println("Could not open browser automatically: " + e.getMessage());
        }
    }

    /**
     * Serves loading.html from the JAR resources.
     */
    static class LoadingHandler implements HttpHandler
    {
        @Override
        public void handle(final HttpExchange exchange)
        {
            try
            {
                try (final InputStream is = getClass().getResourceAsStream("/loading.html"))
                {
                    if (is == null)
                    {
                        final String error = "loading.html not found in resources";
                        final byte[] errorBytes = error.getBytes();
                        exchange.sendResponseHeaders(404, errorBytes.length);
                        try (final OutputStream os = exchange.getResponseBody())
                        {
                            os.write(errorBytes);
                        }
                        return;
                    }

                    final byte[] bytes = is.readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (final OutputStream os = exchange.getResponseBody())
                    {
                        os.write(bytes);
                    }
                }
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}