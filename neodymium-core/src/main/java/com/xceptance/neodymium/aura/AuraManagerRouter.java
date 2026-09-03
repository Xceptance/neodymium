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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clean, lightweight, enterprise-ready HTTP routing registry and dispatcher
 * for the Neodymium Aura Manager standalone server.
 *
 * @author AI-generated: Antigravity AI
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerRouter implements HttpHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraManagerRouter.class);

    private final Map<String, HttpHandler> exactRoutes = new ConcurrentHashMap<>();
    private final List<PrefixRoute> prefixRoutes = new CopyOnWriteArrayList<>();

    public AuraManagerRouter GET(final String path, final HttpHandler handler)
    {
        exactRoutes.put("GET:" + path, handler);
        return this;
    }

    public AuraManagerRouter POST(final String path, final HttpHandler handler)
    {
        exactRoutes.put("POST:" + path, handler);
        return this;
    }

    public AuraManagerRouter prefix(final String method, final String prefix, final HttpHandler handler)
    {
        prefixRoutes.add(new PrefixRoute(method, prefix, handler));
        return this;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException
    {
        final String path = exchange.getRequestURI().getPath();
        final String method = exchange.getRequestMethod().toUpperCase();

        // Enable CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(method))
        {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try
        {
            HttpHandler handler = exactRoutes.get(method + ":" + path);
            if (handler == null)
            {
                for (final PrefixRoute route : prefixRoutes)
                {
                    if (route.matches(method, path))
                    {
                        handler = route.handler;
                        break;
                    }
                }
            }

            if (handler != null)
            {
                handler.handle(exchange);
            }
            else
            {
                sendError(exchange, 404, "Endpoint not found: " + method + " " + path);
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("[Aura Server] Error processing request on path {}: {}", path, e.getMessage(), e);
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void sendError(final HttpExchange exchange, final int statusCode, final String message) throws IOException
    {
        final byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (final OutputStream os = exchange.getResponseBody())
        {
            os.write(bytes);
        }
    }

    private static final class PrefixRoute
    {
        final String method;
        final String prefix;
        final HttpHandler handler;

        PrefixRoute(final String method, final String prefix, final HttpHandler handler)
        {
            this.method = method != null ? method.toUpperCase() : null;
            this.prefix = prefix;
            this.handler = handler;
        }

        boolean matches(final String requestMethod, final String requestPath)
        {
            if (method != null && !method.equals(requestMethod))
            {
                return false;
            }
            return requestPath.startsWith(prefix);
        }
    }
}
