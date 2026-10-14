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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import io.javalin.http.Context;

/**
 * Adapter allowing low-level HTTP handlers to execute seamlessly on top of Javalin's
 * Jetty request/response pipeline.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class JavalinHttpExchange extends HttpExchange
{
    private final Context ctx;
    private final Headers requestHeaders = new Headers();
    private final Headers responseHeaders = new Headers();
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private final AtomicBoolean flushed = new AtomicBoolean(false);
    private int responseCode = 200;

    public JavalinHttpExchange(final Context ctx)
    {
        this.ctx = ctx;
        for (final Map.Entry<String, String> entry : ctx.headerMap().entrySet())
        {
            this.requestHeaders.add(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Headers getRequestHeaders()
    {
        return requestHeaders;
    }

    @Override
    public Headers getResponseHeaders()
    {
        return responseHeaders;
    }

    @Override
    public URI getRequestURI()
    {
        final String qs = ctx.queryString();
        return URI.create(ctx.path() + (qs != null && !qs.isEmpty() ? "?" + qs : ""));
    }

    @Override
    public String getRequestMethod()
    {
        return ctx.method().name();
    }

    @Override
    public HttpContext getHttpContext()
    {
        return null;
    }

    @Override
    public void close()
    {
        flushToJavalin();
    }

    @Override
    public InputStream getRequestBody()
    {
        final byte[] body = ctx.bodyAsBytes();
        return new ByteArrayInputStream(body);
    }

    @Override
    public OutputStream getResponseBody()
    {
        return responseBody;
    }

    @Override
    public void sendResponseHeaders(final int rCode, final long responseLength) throws IOException
    {
        this.responseCode = rCode;
    }

    @Override
    public InetSocketAddress getRemoteAddress()
    {
        return new InetSocketAddress(ctx.ip(), 0);
    }

    @Override
    public int getResponseCode()
    {
        return responseCode;
    }

    @Override
    public InetSocketAddress getLocalAddress()
    {
        return new InetSocketAddress(ctx.host(), ctx.port());
    }

    @Override
    public String getProtocol()
    {
        return ctx.protocol();
    }

    @Override
    public Object getAttribute(final String name)
    {
        return ctx.attribute(name);
    }

    @Override
    public void setAttribute(final String name, final Object value)
    {
        ctx.attribute(name, value);
    }

    @Override
    public void setStreams(final InputStream i, final OutputStream o)
    {
    }

    @Override
    public HttpPrincipal getPrincipal()
    {
        return null;
    }

    public void flushToJavalin()
    {
        if (!flushed.compareAndSet(false, true))
        {
            return;
        }
        ctx.status(responseCode);
        for (final Map.Entry<String, List<String>> entry : responseHeaders.entrySet())
        {
            final String name = entry.getKey();
            for (final String value : entry.getValue())
            {
                ctx.res().addHeader(name, value);
            }
        }
        final byte[] bytes = responseBody.toByteArray();
        if (bytes.length > 0)
        {
            try
            {
                ctx.res().getOutputStream().write(bytes);
                ctx.res().getOutputStream().flush();
            }
            catch (final IOException e)
            {
                throw new RuntimeException("Error flushing response to Javalin", e);
            }
        }
    }
}
