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

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Protocol-agnostic and HTTP presentation utility helper.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraHttpUtils
{
    public static final Gson gson = new Gson();

    private AuraHttpUtils()
    {
    }

    public static String readBody(final HttpExchange exchange) throws IOException
    {
        try (final InputStream is = exchange.getRequestBody())
        {
            final byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public static Map<String, String> getQueryParams(final HttpExchange exchange)
    {
        final String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isEmpty())
        {
            return Map.of();
        }

        final Map<String, String> params = new java.util.HashMap<>();
        for (final String param : query.split("&"))
        {
            final String[] pair = param.split("=", 2);
            if (pair.length > 1)
            {
                params.put(java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
            else if (pair.length == 1)
            {
                params.put(java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }

    public static Map<String, String> getRequestParams(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = new java.util.HashMap<>(getQueryParams(exchange));
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) || "PUT".equalsIgnoreCase(exchange.getRequestMethod()))
        {
            final String body = readBody(exchange);
            if (body != null && !body.trim().isEmpty())
            {
                if (body.trim().startsWith("{"))
                {
                    try
                    {
                        final com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                        for (final String key : json.keySet())
                        {
                            if (!json.get(key).isJsonNull())
                            {
                                params.put(key, json.get(key).getAsString());
                            }
                        }
                    }
                    catch (final Exception e)
                    {
                        // ignore JSON parse error
                    }
                }
                else
                {
                    for (final String param : body.split("&"))
                    {
                        final String[] pair = param.split("=", 2);
                        if (pair.length > 1)
                        {
                            params.put(java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                                    java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                        }
                        else if (pair.length == 1)
                        {
                            params.put(java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8), "");
                        }
                    }
                }
            }
        }
        return params;
    }

    public static void sendResponse(final HttpExchange exchange, final int status, final String contentType,
            final byte[] bytes) throws IOException
    {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.sendResponseHeaders(status, bytes.length);
        try (final OutputStream os = exchange.getResponseBody())
        {
            os.write(bytes);
        }
    }

    public static void sendJsonResponse(final HttpExchange exchange, final int status, final String json)
            throws IOException
    {
        final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.sendResponseHeaders(status, bytes.length);
        try (final OutputStream os = exchange.getResponseBody())
        {
            os.write(bytes);
        }
    }

    public static void sendError(final HttpExchange exchange, final int status, final String message) throws IOException
    {
        final String json = gson.toJson(Map.of("error", message));
        sendJsonResponse(exchange, status, json);
    }

    public static String getMimeType(final String filename)
    {
        final String lower = filename.toLowerCase();
        if (lower.endsWith(".html"))
        {
            return "text/html; charset=UTF-8";
        }
        if (lower.endsWith(".css"))
        {
            return "text/css";
        }
        if (lower.endsWith(".js"))
        {
            return "application/javascript";
        }
        if (lower.endsWith(".png"))
        {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        {
            return "image/jpeg";
        }
        if (lower.endsWith(".svg"))
        {
            return "image/svg+xml";
        }
        if (lower.endsWith(".json"))
        {
            return "application/json";
        }
        if (lower.endsWith(".woff"))
        {
            return "font/woff";
        }
        if (lower.endsWith(".woff2"))
        {
            return "font/woff2";
        }
        if (lower.endsWith(".ttf"))
        {
            return "font/ttf";
        }
        return "application/octet-stream";
    }
}
