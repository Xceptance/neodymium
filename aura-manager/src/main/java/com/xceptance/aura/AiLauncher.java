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
import java.util.Locale;

/**
 * Launcher for Neodymium AI Manager.
 * Launches a lightweight splash server on an ephemeral port to provide immediate visual feedback
 * in the user's browser while Spring Boot initializes in the background.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AiLauncher
{
    static final String SPLASH_ACTIVE_PROPERTY = "ai.launcher.splash.active";
    static final String OPEN_BROWSER_PROPERTY = "neodymium.ai.openBrowser";
    static final String LEGACY_OPEN_BROWSER_PROPERTY = "neodymium.aura.openBrowser";

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

        if (isBrowserOpenEnabled())
        {
            // 1. Start a lightweight HTTP server on an ephemeral port (port 0) on the loopback interface
            final HttpServer server = startSplashServer();
            final int tempPort = server.getAddress().getPort();
            final String loadingUrl = "http://localhost:" + tempPort + "/loading.html";

            // Register JVM shutdown hook to clean up the temporary server upon termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

            // 2. Open default browser to the loading splash screen in a detached process
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
        else
        {
            System.out.println("[Neodymium AI Manager] Browser auto-open is disabled. Dashboard will be available at http://localhost:8080");
            AuraManagerApplication.main(args);
        }
    }

    /**
     * Checks if browser auto-launch is enabled via system property or environment variable.
     *
     * @return true if browser should be opened automatically, false otherwise
     */
    static boolean isBrowserOpenEnabled()
    {
        final String openBrowserVal = System.getProperty(OPEN_BROWSER_PROPERTY, System.getProperty(LEGACY_OPEN_BROWSER_PROPERTY));
        if (openBrowserVal != null)
        {
            return Boolean.parseBoolean(openBrowserVal);
        }
        final String envVal = System.getenv("AURA_NO_BROWSER");
        final String envAiVal = System.getenv("NEODYMIUM_AI_NO_BROWSER");
        if ("true".equalsIgnoreCase(envVal) || "true".equalsIgnoreCase(envAiVal) || "1".equals(envVal) || "1".equals(envAiVal))
        {
            return false;
        }
        return true;
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
     * Attempts to open the specified URL in the user's default web browser in a detached process
     * so that terminating the server does not kill the browser session.
     *
     * @param url target URL to navigate to
     */
    static void openBrowser(final String url)
    {
        if (!isBrowserOpenEnabled())
        {
            return;
        }

        final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try
        {
            if (osName.contains("linux") || osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))
            {
                openLinuxBrowserDetached(url);
            }
            else if (osName.contains("mac"))
            {
                new ProcessBuilder("open", url)
                    .redirectInput(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            }
            else if (osName.contains("win"))
            {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                {
                    Desktop.getDesktop().browse(new URI(url));
                }
                else
                {
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url)
                        .redirectInput(ProcessBuilder.Redirect.DISCARD)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                }
            }
            else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            {
                Desktop.getDesktop().browse(new URI(url));
            }
            else
            {
                new ProcessBuilder("xdg-open", url)
                    .redirectInput(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            }
        }
        catch (final Exception e)
        {
            System.err.println("Could not open browser automatically: " + e.getMessage());
        }
    }

    /**
     * Opens the browser on Linux/Unix detached from the current process group via setsid.
     *
     * @param url target URL to navigate to
     * @throws IOException if spawning the process fails
     */
    static void openLinuxBrowserDetached(final String url) throws IOException
    {
        // Try setsid xdg-open to decouple from foreground terminal process group (so Ctrl+C won't kill browser)
        try
        {
            new ProcessBuilder("setsid", "xdg-open", url)
                .redirectInput(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            return;
        }
        catch (final IOException ignored)
        {
            // Fall back to gio open or standard xdg-open if setsid is unavailable
        }

        try
        {
            new ProcessBuilder("gio", "open", url)
                .redirectInput(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            return;
        }
        catch (final IOException ignored)
        {
            // Fall through to Desktop browse / plain xdg-open
        }

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        {
            try
            {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
            catch (final Exception ignored)
            {
                // Fall through
            }
        }

        new ProcessBuilder("xdg-open", url)
            .redirectInput(ProcessBuilder.Redirect.DISCARD)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();
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
