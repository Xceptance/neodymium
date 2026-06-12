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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;

/**
 * A lightweight, embedded HTTP server for testing AI actions locally without external dependencies.
 * It serves static files from the 'src/test/resources/ai-test-pages/' classpath directory.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
public final class EmbeddedHtmlServer
{
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedHtmlServer.class);
    private final HttpServer server;
    private final HttpsServer httpsServer;
    private final int port;
    private final int httpsPort;

    /**
     * Creates a new embedded HTTP and HTTPS server bound to random free ports.
     * 
     * @throws IOException if the server cannot be bound or created
     */
    public EmbeddedHtmlServer() throws IOException
    {
        // 1. Create standard HTTP server on a random free port
        this.server = HttpServer.create(new InetSocketAddress(0), 0);
        this.port = this.server.getAddress().getPort();
        
        final ResourceHandler resourceHandler = new ResourceHandler();
        this.server.createContext("/", resourceHandler);
        this.server.setExecutor(null); // creates a default executor

        // 2. Create secure HTTPS server on another random free port
        this.httpsServer = HttpsServer.create(new InetSocketAddress(0), 0);
        this.httpsPort = this.httpsServer.getAddress().getPort();

        try
        {
            final KeyStore ks = KeyStore.getInstance("PKCS12");
            try (final InputStream ksf = Thread.currentThread().getContextClassLoader().getResourceAsStream("keystore.p12"))
            {
                if (ksf == null)
                {
                    throw new IOException("keystore.p12 resource not found on classpath.");
                }
                ks.load(ksf, "changeit".toCharArray());
            }

            final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "changeit".toCharArray());

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            this.httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            this.httpsServer.createContext("/", resourceHandler);
            this.httpsServer.setExecutor(null);
        }
        catch (final Exception e)
        {
            throw new IOException("Failed to initialize secure HttpsServer with self-signed certificate", e);
        }
    }

    /**
     * Starts the embedded HTTP and HTTPS servers.
     */
    public void start()
    {
        LOG.info("Starting embedded HTML HTTP server on port {}", port);
        server.start();
        LOG.info("Starting embedded HTML HTTPS server on port {}", httpsPort);
        httpsServer.start();
    }

    /**
     * Stops both embedded servers immediately.
     */
    public void stop()
    {
        LOG.info("Stopping embedded HTML HTTP server on port {}", port);
        server.stop(0);
        LOG.info("Stopping embedded HTML HTTPS server on port {}", httpsPort);
        httpsServer.stop(0);
    }

    /**
     * Gets the port the HTTP server is listening on.
     * 
     * @return the HTTP port number
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Gets the port the HTTPS server is listening on.
     * 
     * @return the HTTPS port number
     */
    public int getHttpsPort()
    {
        return httpsPort;
    }

    /**
     * Internal handler to map HTTP requests to classpath resources.
     */
    private static final class ResourceHandler implements HttpHandler
    {
        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            String path = exchange.getRequestURI().getPath();
            
            if (path.equals("/"))
            {
                path = "index.html";
            }
            else if (path.startsWith("/"))
            {
                path = path.substring(1);
            }
            
            final String resourcePath = "ai-test-pages/" + path;
            
            try (final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath))
            {
                if (is == null)
                {
                    LOG.warn("Resource not found: {}", resourcePath);
                    final String response = "404 Not Found";
                    exchange.sendResponseHeaders(404, response.length());
                    try (final OutputStream os = exchange.getResponseBody())
                    {
                        os.write(response.getBytes());
                    }
                    return;
                }
                
                final byte[] bytes = is.readAllBytes();
                String contentType = "text/plain";
                
                if (path.endsWith(".html"))
                {
                    contentType = "text/html; charset=UTF-8";
                }
                else if (path.endsWith(".css"))
                {
                    contentType = "text/css";
                }
                else if (path.endsWith(".js"))
                {
                    contentType = "application/javascript";
                }
                else if (path.endsWith(".png"))
                {
                    contentType = "image/png";
                }
                else if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
                {
                    contentType = "image/jpeg";
                }
                else if (path.endsWith(".gif"))
                {
                    contentType = "image/gif";
                }
                else if (path.endsWith(".svg"))
                {
                    contentType = "image/svg+xml";
                }
                else if (path.endsWith(".pdf"))
                {
                    contentType = "application/pdf";
                    final String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                }
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                
                try (final OutputStream os = exchange.getResponseBody())
                {
                    os.write(bytes);
                }
            }
            catch (final Exception e)
            {
                LOG.error("Error serving resource: {}", resourcePath, e);
                final String response = "500 Internal Server Error";
                exchange.sendResponseHeaders(500, response.length());
                try (final OutputStream os = exchange.getResponseBody())
                {
                    os.write(response.getBytes());
                }
            }
        }
    }

    /**
     * Main entry point to run the server standalone from the command line/terminal.
     * Starts the HTTP and HTTPS servers on random free ports
     * and prints access URLs for the test applications.
     * 
     * @param args command line arguments (ignored)
     */
    public static void main(final String[] args)
    {
        try
        {
            final EmbeddedHtmlServer server = new EmbeddedHtmlServer();
            server.start();

            System.out.println("========================================================================");
            System.out.println("  Neodymium Aura AI: Embedded HTML Server Running Successfully!");
            System.out.println("========================================================================");
            System.out.println("  Access the test applications via the following URLs:");
            System.out.println();
            System.out.println("  [HTTP Contexts]");
            System.out.println("    - Starter Hub Home:  http://localhost:" + server.getPort() + "/AuraGlanceTest/index.html");
            System.out.println("    - Shop Home:         http://localhost:" + server.getPort() + "/AuraGlanceTest/shop/index.html");
            System.out.println("    - Forms Demo:        http://localhost:" + server.getPort() + "/AuraGlanceTest/shop/forms.html");
            System.out.println("    - Dashboard:         http://localhost:" + server.getPort() + "/AuraGlanceTest/dashboard/index.html");
            System.out.println("    - Accessibility:     http://localhost:" + server.getPort() + "/AuraGlanceTest/a11y/index.html");
            System.out.println("    - React SPA:         http://localhost:" + server.getPort() + "/AuraGlanceTest/spa/index.html");
            System.out.println();
            System.out.println("  [HTTPS Secure Contexts]");
            System.out.println("    - Starter Hub Home:  https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/index.html");
            System.out.println("    - Shop Home:         https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/shop/index.html");
            System.out.println("    - Forms Demo:        https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/shop/forms.html");
            System.out.println("    - Dashboard:         https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/dashboard/index.html");
            System.out.println("    - Accessibility:     https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/a11y/index.html");
            System.out.println("    - React SPA:         https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/spa/index.html");
            System.out.println();
            System.out.println("  NOTE: For HTTPS, you will get a self-signed certificate warning.");
            System.out.println("        You can safely bypass this or run with Chrome's '--ignore-certificate-errors' flag.");
            System.out.println("========================================================================");
            System.out.println("  Press Ctrl+C to terminate the server.");
            System.out.println("========================================================================");

            // Keep the main thread alive
            Thread.currentThread().join();
        }
        catch (final Exception e)
        {
            System.err.println("Failed to start Embedded HTML Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
