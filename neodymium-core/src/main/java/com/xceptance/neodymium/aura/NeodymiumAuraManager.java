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
package com.xceptance.neodymium.aura;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.neodymium.util.Neodymium;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Neodymium Aura Manager: A lightweight standalone web server to browse,
 * create, edit, queue, run, and view reports of Neodymium YAML test data files.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class NeodymiumAuraManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumAuraManager.class);
    private static final Map<HttpServer, NeodymiumAuraManager> activeManagers = new ConcurrentHashMap<>();

    private final String serverSessionId = UUID.randomUUID().toString();
    private final Map<String, Long> activeClients = new ConcurrentHashMap<>();
    private final Map<HttpServer, HttpsServer> httpsServers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService shutdownScheduler = Executors
            .newSingleThreadScheduledExecutor(runnable -> {
                final Thread t = new Thread(runnable, "Aura-Shutdown-Scheduler");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
    private ScheduledFuture<?> pendingShutdown = null;

    private final int port;
    private final TemplateEngine templateEngine;
    private final HttpServer httpServer;
    private AuraManagerMainHandler mainHandler;

    private NeodymiumAuraManager(final int port, final HttpServer server)
    {
        this.port = port;
        this.httpServer = server;

        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("com/xceptance/neodymium/aura/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public static NeodymiumAuraManager getActiveManager(final HttpServer server)
    {
        return activeManagers.get(server);
    }

    public AuraManagerMainHandler getMainHandler()
    {
        return mainHandler;
    }

    public static void main(final String[] args)
    {
        try
        {
            final HttpServer server = startServer(18080);
            final int port = server.getAddress().getPort();

            final String serverUrl = "http://localhost:" + port;
            LOGGER.info("========================================================================");
            LOGGER.info("  Neodymium Aura: Standalone Manager Running Successfully!");
            LOGGER.info("========================================================================");
            LOGGER.info("  Dashboard URL: {}", serverUrl);
            LOGGER.info("========================================================================");

            openBrowser(serverUrl);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to start Neodymium Aura Manager", e);
            System.exit(1);
        }
    }

    public static HttpServer startServer(final int startPort) throws IOException
    {
        return startServer(startPort, false);
    }

    public static HttpServer startServer(final int startPort, final boolean enforcePort) throws IOException
    {
        validateEnvironment();

        int resolvedPort = startPort;
        HttpServer server = null;
        if (enforcePort)
        {
            try
            {
                server = HttpServer.create(new InetSocketAddress("0.0.0.0", resolvedPort), 0);
            }
            catch (final IOException e)
            {
                throw new IOException("Failed to start Neodymium Aura Manager: Port " + resolvedPort + " is already in use.",
                        e);
            }
        }
        else
        {
            while (resolvedPort < startPort + 100)
            {
                try
                {
                    server = HttpServer.create(new InetSocketAddress("0.0.0.0", resolvedPort), 0);
                    break;
                }
                catch (final IOException e)
                {
                    resolvedPort++;
                }
            }
        }

        if (server == null)
        {
            throw new IOException("Could not find any free port starting from " + startPort);
        }

        final NeodymiumAuraManager manager = new NeodymiumAuraManager(resolvedPort, server);
        activeManagers.put(server, manager);

        manager.start(enforcePort);

        return server;
    }

    private void start(final boolean enforcePort)
    {
        mainHandler = new AuraManagerMainHandler(this);

        httpServer.createContext("/", mainHandler);
        httpServer.setExecutor(Executors.newCachedThreadPool((final Runnable runnable) -> {
            final Thread t = new Thread(runnable);
            t.setDaemon(true);
            return t;
        }));
        httpServer.start();
        LOGGER.info("[Aura Server] HTTP server started on http://localhost:{}", port);

        // Also start HTTPS secure server on the next port if keystore.p12 is available
        try
        {
            final KeyStore ks = KeyStore.getInstance("PKCS12");
            try (final InputStream ksf = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("keystore.p12"))
            {
                if (ksf != null)
                {
                    ks.load(ksf, "changeit".toCharArray());
                    final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                    kmf.init(ks, "changeit".toCharArray());
                    final SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(kmf.getKeyManagers(), null, null);

                    int httpsPort = port + 1;
                    HttpsServer httpsServer = null;
                    if (enforcePort)
                    {
                        try
                        {
                            httpsServer = HttpsServer.create(new InetSocketAddress("0.0.0.0", httpsPort), 0);
                        }
                        catch (final IOException e)
                        {
                            throw new IOException("Failed to start Neodymium Aura Manager secure server: Port " + httpsPort + " is already in use.", e);
                        }
                    }
                    else
                    {
                        while (httpsPort < port + 100)
                        {
                            try
                            {
                                httpsServer = HttpsServer.create(new InetSocketAddress("0.0.0.0", httpsPort), 0);
                                break;
                            }
                            catch (final IOException e)
                            {
                                httpsPort++;
                            }
                        }
                    }

                    if (httpsServer != null)
                    {
                        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
                        httpsServer.createContext("/", mainHandler);
                        httpsServer.setExecutor(httpServer.getExecutor());
                        httpsServer.start();
                        LOGGER.info("[Aura Server] HTTPS secure server started on https://localhost:{}", httpsPort);
                        httpsServers.put(httpServer, httpsServer);
                    }
                }
            }
        }
        catch (final Exception e)
        {
            LOGGER.warn("[Aura Server] Failed to initialize secure HttpsServer: {}", e.getMessage());
            if (enforcePort && e instanceof IOException)
            {
                stopServer(httpServer);
                throw new RuntimeException(e);
            }
        }
    }

    public static void stopServer(final HttpServer server)
    {
        if (server != null)
        {
            final NeodymiumAuraManager manager = activeManagers.remove(server);
            if (manager != null)
            {
                manager.stop();
            }
            else
            {
                server.stop(0);
            }
        }
    }

    public void stop()
    {
        if (httpServer != null)
        {
            httpServer.stop(0);
            final HttpsServer httpsServer = httpsServers.remove(httpServer);
            if (httpsServer != null)
            {
                httpsServer.stop(0);
            }
        }
        shutdownScheduler.shutdownNow();
    }

    public String getServerSessionId()
    {
        return serverSessionId;
    }

    public int getPort()
    {
        return port;
    }

    public TemplateEngine getTemplateEngine()
    {
        return templateEngine;
    }

    public void registerClient(final String clientId)
    {
        activeClients.put(clientId, System.currentTimeMillis());
        cancelPendingShutdown();
    }

    public void removeClient(final String clientId)
    {
        activeClients.remove(clientId);
        checkShutdownOnDisconnect();
    }

    public void cancelPendingShutdown()
    {
        synchronized (activeClients)
        {
            if (pendingShutdown != null)
            {
                pendingShutdown.cancel(false);
                pendingShutdown = null;
            }
        }
    }

    public void checkShutdownOnDisconnect()
    {
        synchronized (activeClients)
        {
            if (activeClients.isEmpty())
            {
                if (pendingShutdown == null || pendingShutdown.isDone())
                {
                    final int shutdownDelay = Neodymium.aiConfiguration().auraManagerShutdownDelay();
                    if (shutdownDelay < 0)
                    {
                        LOGGER.info("[Aura Server] Shutdown delay is disabled by configuration.");
                        return;
                    }
                    LOGGER.info("[Aura Server] No active clients. Scheduling server shutdown in {} seconds...",
                            shutdownDelay);
                    pendingShutdown = shutdownScheduler.schedule(() -> {
                        synchronized (activeClients)
                        {
                            if (activeClients.isEmpty())
                            {
                                LOGGER.info("[Aura Server] No active clients for {} seconds. Shutting down...",
                                        shutdownDelay);

                                if (System.getProperty("neodymium.aura.test") == null)
                                {
                                    System.exit(0);
                                }
                                else
                                {
                                    LOGGER.info("[Aura Server] Skipping System.exit(0) in test environment.");
                                }
                            }
                        }
                    }, shutdownDelay, TimeUnit.SECONDS);
                }
            }
        }
    }

    private static void validateEnvironment()
    {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null || javaHome.trim().isEmpty() || !new File(javaHome).isDirectory())
        {
            javaHome = System.getProperty("java.home");
        }

        if (javaHome == null || javaHome.trim().isEmpty() || !new File(javaHome).isDirectory())
        {
            LOGGER.error("========================================================================");
            LOGGER.error("  ERROR: JAVA_HOME environment variable is not set or invalid!");
            LOGGER.error("  Please set JAVA_HOME to a valid JDK installation directory.");
            LOGGER.error("========================================================================");
            throw new IllegalStateException("JAVA_HOME environment variable is not set or invalid");
        }

        try
        {
            final String os = System.getProperty("os.name").toLowerCase();
            final String[] checkCmd = os.contains("win") ? new String[] { "cmd.exe", "/c", "mvn", "-v" }
                    : new String[] { "mvn", "-v" };
            final ProcessBuilder pb = new ProcessBuilder(checkCmd);
            final Process p = pb.start();
            p.waitFor();
        }
        catch (final Exception e)
        {
            LOGGER.error("========================================================================");
            LOGGER.error("  ERROR: Maven command 'mvn' is not available in system PATH!");
            LOGGER.error("  Please ensure Maven is installed and 'mvn' is executable.");
            LOGGER.error("========================================================================");
            throw new IllegalStateException("Maven command 'mvn' is not available", e);
        }
    }

    private static void openBrowser(final String url)
    {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        {
            try
            {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
            catch (final Exception e)
            {
                // ignore and fall back
            }
        }

        try
        {
            final String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win"))
            {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            }
            else if (os.contains("mac"))
            {
                Runtime.getRuntime().exec("open " + url);
            }
            else
            {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        }
        catch (final Exception e)
        {
            // ignore
        }
    }

    public static String stripAnsi(final String line)
    {
        if (line == null)
        {
            return null;
        }
        // Strip standard ANSI escape codes
        String clean = line.replaceAll("(?i)\\u001B\\[[;0-9]*[a-zA-Z]", "");
        // Clean up leftover patterns like [1;34m if generated without ESC char
        clean = clean.replaceAll("\\[[0-9]+(;[0-9]+)*[a-zA-Z]", "");
        // Clean up leftover reset codes like [m (but not [INFO], [WARNING] or other
        // words)
        clean = clean.replaceAll("(?i)\\[[a-zA-Z](?![a-zA-Z0-9])", "");
        return clean;
    }
}
