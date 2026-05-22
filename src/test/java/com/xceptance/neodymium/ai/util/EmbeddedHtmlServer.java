/*
 * MIT License
 * 
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.ai.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * A lightweight, embedded HTTP server for testing AI actions locally without external dependencies.
 * It serves static files from the 'src/test/resources/ai-test-pages/' classpath directory.
 */
public final class EmbeddedHtmlServer
{
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedHtmlServer.class);
    private final HttpServer server;
    private final int port;

    /**
     * Creates a new embedded HTTP server bound to a random free port.
     * 
     * @throws IOException if the server cannot be bound or created
     */
    public EmbeddedHtmlServer() throws IOException
    {
        // Port 0 means letting the OS choose a free port
        this.server = HttpServer.create(new InetSocketAddress(0), 0);
        this.port = this.server.getAddress().getPort();
        
        // Serve everything under / mapping to src/test/resources/ai-test-pages/
        this.server.createContext("/", new ResourceHandler());
        this.server.setExecutor(null); // creates a default executor
    }

    /**
     * Starts the embedded server.
     */
    public void start()
    {
        LOG.info("Starting embedded HTML server on port {}", port);
        server.start();
    }

    /**
     * Stops the embedded server immediately.
     */
    public void stop()
    {
        LOG.info("Stopping embedded HTML server on port {}", port);
        server.stop(0);
    }

    /**
     * Gets the port the server is listening on.
     * 
     * @return the port number
     */
    public int getPort()
    {
        return port;
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
}
