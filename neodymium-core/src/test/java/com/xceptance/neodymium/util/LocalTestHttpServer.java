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
package com.xceptance.neodymium.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;

/**
 * Lightweight embedded HTTP server for offline utility tests in {@code neodymium-core}.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
final class LocalTestHttpServer implements AutoCloseable
{
    private final HttpServer server;

    private final String host;

    private LocalTestHttpServer(final HttpServer server, final String host)
    {
        this.server = server;
        this.host = host;
    }

    /**
     * Starts an embedded HTTP server on an ephemeral {@code localhost} port serving a single HTML body at {@code /}.
     *
     * @param htmlBody
     *            HTML content to return with status 200
     * @return running {@link LocalTestHttpServer} instance
     * @throws IOException
     *             if the server socket cannot be bound
     */
    static LocalTestHttpServer startWithHtml(final String htmlBody) throws IOException
    {
        return startWithRoutes("localhost", Map.of("/", htmlBody));
    }

    /**
     * Starts an embedded HTTP server on an ephemeral {@code localhost} port serving the given path-to-HTML routes.
     *
     * @param routes
     *            mapping of URL paths (e.g., {@code "/blog"}) to HTML response bodies
     * @return running {@link LocalTestHttpServer} instance
     * @throws IOException
     *             if the server socket cannot be bound
     */
    static LocalTestHttpServer startWithRoutes(final Map<String, String> routes) throws IOException
    {
        return startWithRoutes("localhost", routes);
    }

    /**
     * Starts an embedded HTTP server bound to the local host's non-loopback address (or {@code localhost} if
     * unavailable), useful when testing browser proxies that bypass {@code localhost}.
     *
     * @param routes
     *            mapping of URL paths to HTML response bodies
     * @return running {@link LocalTestHttpServer} instance
     * @throws IOException
     *             if the server socket cannot be bound
     */
    static LocalTestHttpServer startWithRoutesOnLocalHostAddress(final Map<String, String> routes) throws IOException
    {
        final String hostAddress = resolveNonLoopbackIpv4Address();
        return startWithRoutes(hostAddress, routes);
    }

    private static String resolveNonLoopbackIpv4Address() throws IOException
    {
        final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements())
        {
            final NetworkInterface iface = interfaces.nextElement();
            if (!iface.isUp() || iface.isLoopback())
            {
                continue;
            }
            final Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements())
            {
                final InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                {
                    return addr.getHostAddress();
                }
            }
        }
        return InetAddress.getLocalHost().getHostAddress();
    }

    private static LocalTestHttpServer startWithRoutes(final String bindHost, final Map<String, String> routes) throws IOException
    {
        final HttpServer httpServer = HttpServer.create(new InetSocketAddress(bindHost, 0), 0);
        for (final Map.Entry<String, String> entry : routes.entrySet())
        {
            final byte[] bodyBytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
            httpServer.createContext(entry.getKey(), exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bodyBytes.length);
                try (OutputStream out = exchange.getResponseBody())
                {
                    out.write(bodyBytes);
                }
            });
        }
        httpServer.start();
        return new LocalTestHttpServer(httpServer, bindHost);
    }

    /**
     * Returns the full {@code http://<host>:<port><path>} URL for the given path.
     *
     * @param path
     *            path starting with {@code /}
     * @return full local URL
     */
    String url(final String path)
    {
        return "http://" + this.host + ":" + this.server.getAddress().getPort() + path;
    }

    @Override
    public void close()
    {
        this.server.stop(0);
    }
}
