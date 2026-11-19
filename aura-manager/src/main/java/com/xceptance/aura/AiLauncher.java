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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

        if (!isBrowserOpenEnabled())
        {
            System.out.println("[Neodymium AI Manager] Browser auto-open is disabled. Dashboard will be available at http://localhost:8080");
            AuraManagerApplication.main(args);
            return;
        }

        final int serverPort = resolveServerPort(args);
        final boolean clientDetected = probePortForClient(serverPort, 400);

        if (clientDetected)
        {
            System.out.println("[Neodymium AI Manager] Active dashboard tab detected. Suppressing duplicate browser launch.");
            AuraManagerApplication.main(args);
            return;
        }

        // 1. Start a lightweight HTTP server on an ephemeral port (port 0) on the loopback interface
        final HttpServer server = startSplashServer(serverPort);
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

    /**
     * Probes the specified port briefly on the loopback interface to detect whether an active client
     * (such as an open dashboard browser tab polling /api/status) is already connected.
     *
     * @param port the target server port to probe
     * @param timeoutMs the maximum duration in milliseconds to wait for a client connection
     * @return true if a client connected during the probe, false otherwise
     */
    static boolean probePortForClient(final int port, final int timeoutMs)
    {
        try (final ServerSocket serverSocket = new ServerSocket())
        {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
            serverSocket.setSoTimeout(timeoutMs);
            try (final Socket client = serverSocket.accept())
            {
                client.setSoTimeout(1000);
                final OutputStream out = client.getOutputStream();
                final String response = "HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n";
                out.write(response.getBytes(StandardCharsets.US_ASCII));
                out.flush();
                return true;
            }
            catch (final SocketTimeoutException ignored)
            {
                return false;
            }
        }
        catch (final Exception ignored)
        {
            return false;
        }
    }

    /**
     * Resolves the target HTTP server port from system properties, command line arguments, or defaults to 8080.
     *
     * @param args command line arguments
     * @return resolved server port
     */
    static int resolveServerPort(final String[] args)
    {
        final String sysProp = System.getProperty("server.port");
        if (sysProp != null)
        {
            try
            {
                return Integer.parseInt(sysProp.trim());
            }
            catch (final NumberFormatException ignored)
            {
            }
        }
        if (args != null)
        {
            for (final String arg : args)
            {
                if (arg != null && arg.startsWith("--server.port="))
                {
                    try
                    {
                        return Integer.parseInt(arg.substring("--server.port=".length()).trim());
                    }
                    catch (final NumberFormatException ignored)
                    {
                    }
                }
            }
        }
        return 8080;
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
        return startSplashServer(8080);
    }

    /**
     * Starts the lightweight HTTP server to serve the splash screen on an ephemeral port targeting the specified server port.
     *
     * @param targetPort the target Spring Boot HTTP port
     * @return the running HttpServer instance
     * @throws IOException if server socket initialization fails
     */
    static HttpServer startSplashServer(final int targetPort) throws IOException
    {
        final HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/loading.html", new LoadingHandler(targetPort));
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
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            return;
        }
        catch (final Exception ignored)
        {
            // Fall back to gio open or standard xdg-open if setsid is unavailable
        }

        try
        {
            new ProcessBuilder("gio", "open", url)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            return;
        }
        catch (final Exception ignored)
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
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();
    }

    /**
     * Serves loading.html from the JAR resources.
     */
    static class LoadingHandler implements HttpHandler
    {
        private final int targetPort;

        LoadingHandler()
        {
            this(8080);
        }

        LoadingHandler(final int targetPort)
        {
            this.targetPort = targetPort;
        }

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
                        final byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(404, errorBytes.length);
                        try (final OutputStream os = exchange.getResponseBody())
                        {
                            os.write(errorBytes);
                        }
                        return;
                    }

                    String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    if (targetPort != 8080)
                    {
                        html = html.replace("http://localhost:8080", "http://localhost:" + targetPort);
                    }
                    final byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
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
