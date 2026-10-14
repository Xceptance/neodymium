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
package org.neodymium.ai.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.neodymium.ai.server.actionfixtures.ActionFixturesApp;
import org.neodymium.ai.server.auraglance.AuraGlanceApp;
import org.neodymium.ai.server.client.TestServerClient;
import org.neodymium.ai.server.controller.SessionControlController;
import org.neodymium.ai.server.session.SessionManager;
import org.neodymium.ai.server.session.TestSession;
import org.neodymium.ai.server.verla.VerlaApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;

/**
 * Modern embedded test server based on Javalin 6 and Jetty 11, hosting Aura Glance
 * sandbox challenges, multi-tenant test sessions, opaque anti-leak cookies, dynamic port allocation,
 * and REST control endpoints.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class JavalinTestServer
{
    private static final Logger LOG = LoggerFactory.getLogger(JavalinTestServer.class);

    public static final int DEFAULT_HTTP_PORT = 8542;
    public static final int DEFAULT_HTTPS_PORT = 8543;

    private final Javalin app;
    private final SessionManager sessionManager;
    private final SessionControlController controlController;
    private final AuraGlanceApp auraGlanceApp;
    private final ActionFixturesApp actionFixturesApp;
    private final VerlaApp verlaApp;

    private final AtomicReference<ServerConnector> httpConnectorRef = new AtomicReference<>();
    private final AtomicReference<ServerConnector> httpsConnectorRef = new AtomicReference<>();

    private final int requestedHttpPort;
    private final int requestedHttpsPort;

    private volatile boolean running;

    /**
     * Creates a new test server configured with ports from system properties
     * ({@code neodymium.ai.http.port} / {@code neodymium.ai.https.port}) or defaults.
     *
     * @throws IOException if server initialization fails
     */
    public JavalinTestServer() throws IOException
    {
        this(Integer.getInteger("neodymium.ai.http.port", DEFAULT_HTTP_PORT),
             Integer.getInteger("neodymium.ai.https.port", DEFAULT_HTTPS_PORT));
    }

    /**
     * Creates a new test server bound to the specified HTTP and HTTPS ports.
     * Passing 0 binds to dynamic ephemeral free ports. Passing a negative HTTPS port disables HTTPS.
     *
     * @param httpPort the HTTP port to bind, or 0 for dynamic
     * @param httpsPort the HTTPS port to bind, 0 for dynamic, or negative to disable
     * @throws IOException if server initialization fails
     */
    public JavalinTestServer(final int httpPort, final int httpsPort) throws IOException
    {
        this.requestedHttpPort = resolveAvailablePort(Math.max(0, httpPort), "HTTP");
        this.requestedHttpsPort = httpsPort < 0 ? -1 : resolveAvailablePort(httpsPort, "HTTPS");

        this.sessionManager = new SessionManager();
        this.controlController = new SessionControlController(this.sessionManager);
        this.auraGlanceApp = new AuraGlanceApp(this.sessionManager);
        this.actionFixturesApp = new ActionFixturesApp();
        this.verlaApp = new VerlaApp(this.sessionManager);

        this.app = configureJavalin();
    }

    private static int resolveAvailablePort(final int requestedPort, final String protocol)
    {
        if (requestedPort <= 0)
        {
            return 0;
        }
        try (final ServerSocket socket = new ServerSocket())
        {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress("127.0.0.1", requestedPort));
            return requestedPort;
        }
        catch (final IOException e)
        {
            LOG.warn("Requested {} port {} is unavailable ({}). Falling back to dynamic ephemeral port (0).",
                protocol, requestedPort, e.getMessage());
            return 0;
        }
    }

    private Javalin configureJavalin()
    {
        final Javalin server = Javalin.create(config ->
        {
            config.showJavalinBanner = false;
            config.jsonMapper(new JavalinGsonMapper());

            // Thymeleaf file renderer for dynamic HTML templates
            final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
            templateResolver.setPrefix("ai-test-pages/");
            templateResolver.setTemplateMode(TemplateMode.HTML);
            templateResolver.setCharacterEncoding("UTF-8");
            templateResolver.setCacheable(false);

            final TemplateEngine templateEngine = new TemplateEngine();
            templateEngine.setTemplateResolver(templateResolver);

            config.fileRenderer(new JavalinThymeleaf(templateEngine));

            // Static files fallback for shared fixtures and assets
            config.staticFiles.add(staticFiles ->
            {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/ai-test-pages";
                staticFiles.location = Location.CLASSPATH;
                staticFiles.precompress = false;
            });

            // HTTP Jetty Connector
            config.jetty.addConnector((jettyServer, httpConfig) ->
            {
                final ServerConnector connector = new ServerConnector(jettyServer, new HttpConnectionFactory(httpConfig));
                connector.setHost("127.0.0.1");
                connector.setPort(requestedHttpPort);
                httpConnectorRef.set(connector);
                return connector;
            });

            // HTTPS Jetty Connector (PKCS12)
            if (requestedHttpsPort >= 0)
            {
                config.jetty.addConnector((jettyServer, httpConfig) ->
                {
                    final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
                    httpsConfig.addCustomizer(new SecureRequestCustomizer());

                    final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
                    try
                    {
                        final KeyStore ks = KeyStore.getInstance("PKCS12");
                        try (final InputStream ksf = JavalinTestServer.class.getClassLoader().getResourceAsStream("keystore.p12"))
                        {
                            if (ksf == null)
                            {
                                throw new IOException("keystore.p12 resource not found on classpath.");
                            }
                            ks.load(ksf, "changeit".toCharArray());
                        }
                        sslContextFactory.setKeyStore(ks);
                        sslContextFactory.setKeyStorePassword("changeit");
                    }
                    catch (final Exception e)
                    {
                        throw new RuntimeException("Failed to load keystore.p12 for Javalin HTTPS connector", e);
                    }

                    final ServerConnector sslConnector = new ServerConnector(jettyServer,
                        new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                        new HttpConnectionFactory(httpsConfig));
                    sslConnector.setHost("127.0.0.1");
                    sslConnector.setPort(requestedHttpsPort);
                    httpsConnectorRef.set(sslConnector);
                    return sslConnector;
                });
            }
        });

        // Request interception: Session mapping and clean URL redirect
        server.before(this::handleBefore);

        // REST Control Endpoints
        server.post("/api/test/sessions/{id}/reset", controlController::handleFullReset);
        server.post("/api/test/sessions/{id}/inventory/reset", controlController::handleInventoryReset);
        server.post("/api/test/sessions/{id}/users/reset", controlController::handleUsersReset);
        server.post("/api/test/sessions/{id}/carts/reset", controlController::handleCartsReset);
        server.post("/api/test/sessions/{id}/orders/reset", controlController::handleOrdersReset);
        server.get("/api/test/sessions/{id}", controlController::handleGetStatus);
        server.delete("/api/test/sessions/{id}", controlController::handleDeleteSession);
        server.post("/api/test/reset", controlController::handleGlobalReset);
        server.get("/api/test/health", controlController::handleHealth);

        // Register Aura Glance application routes and simulation controls
        auraGlanceApp.registerRoutes(server);

        // Register Action Test fixture routes
        actionFixturesApp.registerRoutes(server);

        // Register VÉRLA Storefront application routes and APIs
        verlaApp.registerRoutes(server);

        return server;
    }

    private void handleBefore(final Context ctx)
    {
        final String path = ctx.path();
        if (path.startsWith("/api/test/"))
        {
            return;
        }

        final String customSession = ctx.queryParam("customSession");
        if (customSession != null && !customSession.isBlank())
        {
            final String customId = customSession.trim();
            sessionManager.getOrCreateSession(customId);
            final String opaqueToken = sessionManager.getOrCreateOpaqueToken(customId);

            final Cookie cookie = new Cookie(SessionManager.COOKIE_NAME, opaqueToken, "/", 3600, false);
            cookie.setHttpOnly(true);
            ctx.cookie(cookie);

            final String method = ctx.method().name();
            if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method))
            {
                final Map<String, List<String>> queryParams = new LinkedHashMap<>(ctx.queryParamMap());
                queryParams.remove("customSession");

                final StringBuilder cleanUrl = new StringBuilder(ctx.path());
                if (!queryParams.isEmpty())
                {
                    cleanUrl.append("?");
                    boolean first = true;
                    for (final Map.Entry<String, List<String>> entry : queryParams.entrySet())
                    {
                        for (final String val : entry.getValue())
                        {
                            if (!first)
                            {
                                cleanUrl.append("&");
                            }
                            first = false;
                            cleanUrl.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                            cleanUrl.append("=");
                            cleanUrl.append(URLEncoder.encode(val, StandardCharsets.UTF_8));
                        }
                    }
                }
                ctx.redirect(cleanUrl.toString(), HttpStatus.FOUND);
                return;
            }
        }

        final String cookieToken = ctx.cookie(SessionManager.COOKIE_NAME);
        final TestSession session = sessionManager.resolveSession(cookieToken);
        ctx.attribute("testSession", session);
    }

    /**
     * Starts the Javalin test server.
     */
    public synchronized void start()
    {
        if (this.running)
        {
            return;
        }
        LOG.info("Starting Javalin test server on requested ports [http={}, https={}]", requestedHttpPort, requestedHttpsPort);
        try
        {
            this.app.start();
        }
        catch (final Exception e)
        {
            final boolean isBindError = (e instanceof BindException)
                || (e.getCause() instanceof BindException)
                || (e.getMessage() != null && e.getMessage().contains("Address already in use"));
            if (isBindError)
            {
                LOG.warn("Port collision during Javalin startup. Re-binding connectors to dynamic ephemeral ports (0).");
                final ServerConnector httpConn = httpConnectorRef.get();
                if (httpConn != null)
                {
                    httpConn.setPort(0);
                }
                final ServerConnector httpsConn = httpsConnectorRef.get();
                if (httpsConn != null)
                {
                    httpsConn.setPort(0);
                }
                this.app.start();
            }
            else
            {
                throw e;
            }
        }
        this.running = true;
        LOG.info("Javalin test server started on [http={}, https={}]", getPort(), getHttpsPort());
    }

    /**
     * Stops the Javalin test server.
     */
    public synchronized void stop()
    {
        if (!this.running)
        {
            return;
        }
        LOG.info("Stopping Javalin test server on [http={}, https={}]", getPort(), getHttpsPort());
        this.app.stop();
        this.running = false;
    }

    public boolean isRunning()
    {
        return this.running;
    }

    /**
     * Returns the bound local HTTP port.
     *
     * @return the HTTP port, or 0 if not running
     */
    public int getPort()
    {
        final ServerConnector connector = httpConnectorRef.get();
        return connector != null ? connector.getLocalPort() : 0;
    }

    /**
     * Returns the bound local HTTPS port.
     *
     * @return the HTTPS port, or 0 if not running or HTTPS disabled
     */
    public int getHttpsPort()
    {
        final ServerConnector connector = httpsConnectorRef.get();
        return connector != null ? connector.getLocalPort() : 0;
    }

    public ServerConnector getHttpConnector()
    {
        return httpConnectorRef.get();
    }

    public String getHttpUrl()
    {
        return "http://127.0.0.1:" + getPort();
    }

    public String getHttpsUrl()
    {
        return "https://127.0.0.1:" + getHttpsPort();
    }

    public SessionManager getSessionManager()
    {
        return sessionManager;
    }

    public TestSession getSession(final String customSessionId)
    {
        return sessionManager.getSession(customSessionId);
    }

    public TestServerClient getClient(final String customSessionId)
    {
        return TestServerClient.forPort(getPort(), customSessionId);
    }

    public TestServerClient getDefaultClient()
    {
        return TestServerClient.forPort(getPort(), SessionManager.DEFAULT_SESSION_ID);
    }

    public AuraGlanceApp getAuraGlanceApp()
    {
        return auraGlanceApp;
    }

    public ActionFixturesApp getActionFixturesApp()
    {
        return actionFixturesApp;
    }

    public VerlaApp getVerlaApp()
    {
        return verlaApp;
    }

    public void resetAll()
    {
        sessionManager.resetAllSessions();
    }

    public void resetInventory()
    {
        sessionManager.getDefaultSession().resetInventory();
    }

    public void resetCarts()
    {
        sessionManager.getDefaultSession().resetCarts();
    }

    public void resetOrders()
    {
        sessionManager.getDefaultSession().resetOrders();
    }

    public void resetSessions()
    {
        sessionManager.getDefaultSession().resetSessions();
    }

    public void resetUsers()
    {
        sessionManager.getDefaultSession().resetUsers();
    }

    public static void main(final String[] args)
    {
        try
        {
            int httpPort = Integer.getInteger("neodymium.ai.http.port", DEFAULT_HTTP_PORT);
            int httpsPort = Integer.getInteger("neodymium.ai.https.port", DEFAULT_HTTPS_PORT);

            for (int i = 0; i < args.length; i++)
            {
                if ("--port".equals(args[i]) && i + 1 < args.length)
                {
                    httpPort = Integer.parseInt(args[++i]);
                }
                else if ("--httpsPort".equals(args[i]) && i + 1 < args.length)
                {
                    httpsPort = Integer.parseInt(args[++i]);
                }
            }

            final JavalinTestServer server = new JavalinTestServer(httpPort, httpsPort);
            server.start();

            System.out.println("========================================================================");
            System.out.println("  Neodymium Javalin Test Server Running (Aura Glance Hub)");
            System.out.println("========================================================================");
            System.out.println("  Access the Aura Glance test applications via the following URLs:");
            System.out.println();
            System.out.println("  [HTTP Contexts]");
            System.out.println("    - Starter Hub Portal:         http://localhost:" + server.getPort() + "/AuraGlanceTest/index.html");
            System.out.println("    - Shop Home:                  http://localhost:" + server.getPort() + "/AuraGlanceTest/shop/index.html");
            System.out.println("    - Forms Demo:                 http://localhost:" + server.getPort() + "/AuraGlanceTest/shop/forms.html");
            System.out.println("    - Dashboard:                  http://localhost:" + server.getPort() + "/AuraGlanceTest/dashboard/index.html");
            System.out.println("    - Accessibility:              http://localhost:" + server.getPort() + "/AuraGlanceTest/a11y/index.html");
            System.out.println("    - React SPA:                  http://localhost:" + server.getPort() + "/AuraGlanceTest/spa/index.html");
            System.out.println("    - Scenario Playground:        http://localhost:" + server.getPort() + "/AuraGlanceTest/shop/sandbox/index.html");
            System.out.println("    - Action Fixtures Hub:        http://localhost:" + server.getPort() + "/action-test-pages.html");
            System.out.println("    - VÉRLA Tailwind Store:       http://localhost:" + server.getPort() + "/verla-tailwind/index.html");
            System.out.println("    - VÉRLA Tailwind (by Claude): http://localhost:" + server.getPort() + "/verla-tailwind-by-claude/index.html");
            System.out.println("    - VÉRLA Apparel Store:        http://localhost:" + server.getPort() + "/verla-apparel/index.html");
            System.out.println("    - VÉRLA Perfect Store:        http://localhost:" + server.getPort() + "/verla-perfect/index.html");
            System.out.println("    - VÉRLA Normal Store:         http://localhost:" + server.getPort() + "/verla-normal/index.html");
            System.out.println("    - VÉRLA Bad Store:            http://localhost:" + server.getPort() + "/verla-bad/index.html");
            System.out.println("    - VÉRLA Modern Bad (WCAG):    http://localhost:" + server.getPort() + "/verla-modern-bad/index.html");
            System.out.println("    - VÉRLA Modern Bad (No WCAG): http://localhost:" + server.getPort() + "/verla-modern-bad-nowcag/index.html");
            System.out.println("    - VÉRLA PWA Chaos Store:      http://localhost:" + server.getPort() + "/verla-pwa-chaos/index.html");
            System.out.println("    - VÉRLA Headless Store:       http://localhost:" + server.getPort() + "/verla-headless/index.html");
            System.out.println("    - VÉRLA Apocalypse Store:     http://localhost:" + server.getPort() + "/verla-apocalypse/index.html");
            System.out.println();
            if (server.getHttpsPort() > 0)
            {
                System.out.println("  [HTTPS Secure Contexts]");
                System.out.println("    - Starter Hub Portal:         https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/index.html");
                System.out.println("    - Shop Home:                  https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/shop/index.html");
                System.out.println("    - Forms Demo:                 https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/shop/forms.html");
                System.out.println("    - Dashboard:                  https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/dashboard/index.html");
                System.out.println("    - Accessibility:              https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/a11y/index.html");
                System.out.println("    - React SPA:                  https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/spa/index.html");
                System.out.println("    - Action Fixtures Hub:        https://localhost:" + server.getHttpsPort() + "/action-test-pages.html");
                System.out.println("    - VÉRLA Tailwind Store:       https://localhost:" + server.getHttpsPort() + "/verla-tailwind/index.html");
                System.out.println("    - VÉRLA Tailwind (by Claude): https://localhost:" + server.getHttpsPort() + "/verla-tailwind-by-claude/index.html");
                System.out.println("    - VÉRLA Apparel Store:        https://localhost:" + server.getHttpsPort() + "/verla-apparel/index.html");
                System.out.println("    - VÉRLA Perfect Store:        https://localhost:" + server.getHttpsPort() + "/verla-perfect/index.html");
                System.out.println("    - VÉRLA Normal Store:         https://localhost:" + server.getHttpsPort() + "/verla-normal/index.html");
                System.out.println("    - VÉRLA Bad Store:            https://localhost:" + server.getHttpsPort() + "/verla-bad/index.html");
                System.out.println("    - VÉRLA Modern Bad (WCAG):    https://localhost:" + server.getHttpsPort() + "/verla-modern-bad/index.html");
                System.out.println("    - VÉRLA Modern Bad (No WCAG): https://localhost:" + server.getHttpsPort() + "/verla-modern-bad-nowcag/index.html");
                System.out.println("    - VÉRLA PWA Chaos Store:      https://localhost:" + server.getHttpsPort() + "/verla-pwa-chaos/index.html");
                System.out.println("    - VÉRLA Headless Store:       https://localhost:" + server.getHttpsPort() + "/verla-headless/index.html");
                System.out.println("    - VÉRLA Apocalypse Store:     https://localhost:" + server.getHttpsPort() + "/verla-apocalypse/index.html");
                System.out.println();
            }
            System.out.println("  [REST Control Endpoints]");
            System.out.println("    - Health Check:               http://localhost:" + server.getPort() + "/api/test/health");
            System.out.println("    - Global State Reset:         POST http://localhost:" + server.getPort() + "/api/test/reset");
            System.out.println("    - Session Reset:              POST http://localhost:" + server.getPort() + "/api/test/sessions/{id}/reset");
            System.out.println();
            System.out.println("  Press Ctrl+C to terminate the server.");
            System.out.println("========================================================================");

            final CountDownLatch latch = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                server.stop();
                latch.countDown();
            }));
            latch.await();
        }
        catch (final Exception e)
        {
            LOG.error("Failed to start Javalin test server", e);
            System.exit(1);
        }
    }
}
