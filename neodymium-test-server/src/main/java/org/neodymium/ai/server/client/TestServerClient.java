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
package org.neodymium.ai.server.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Lightweight client for managing isolated test sessions and simulations on
 * running Javalin test server instances via REST.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class TestServerClient
{
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final String baseUrl;
    private final String sessionId;
    private final HttpClient httpClient;

    public TestServerClient(final String baseUrl, final String sessionId)
    {
        if (baseUrl == null || baseUrl.isBlank())
        {
            throw new IllegalArgumentException("baseUrl must not be null or blank");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.sessionId = (sessionId == null || sessionId.isBlank()) ? "default" : sessionId.trim();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .build();
    }

    public static TestServerClient forPort(final int port, final String sessionId)
    {
        return new TestServerClient("http://127.0.0.1:" + port, sessionId);
    }

    public static TestServerClient forUrl(final String baseUrl, final String sessionId)
    {
        return new TestServerClient(baseUrl, sessionId);
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public void fullReset()
    {
        sendPost("/api/test/sessions/" + encode(sessionId) + "/reset");
    }

    public void resetAll()
    {
        fullReset();
    }

    public void resetInventory()
    {
        sendPost("/api/test/sessions/" + encode(sessionId) + "/inventory/reset");
    }

    public void resetUsers()
    {
        sendPost("/api/test/sessions/" + encode(sessionId) + "/users/reset");
    }

    public void resetCarts()
    {
        sendPost("/api/test/sessions/" + encode(sessionId) + "/carts/reset");
    }

    public void resetOrders()
    {
        sendPost("/api/test/sessions/" + encode(sessionId) + "/orders/reset");
    }

    public void setAuraGlanceDelay(final int delayMs)
    {
        sendPost("/api/test/sessions/" + encode(sessionId) + "/auraglance/delay?delayMs=" + delayMs);
    }

    public void setAuraGlanceFailure(final boolean enabled)
    {
        sendPost("/api/test/sessions/" + encode(sessionId) + "/auraglance/failure?enabled=" + enabled);
    }

    public void resetAuraGlance()
    {
        sendPost("/api/test/sessions/" + encode(sessionId) + "/auraglance/reset");
    }

    public void destroy()
    {
        sendDelete("/api/test/sessions/" + encode(sessionId));
    }

    public String getStatus()
    {
        return sendGet("/api/test/sessions/" + encode(sessionId));
    }

    public String getHealth()
    {
        return sendGet("/api/test/health");
    }

    private void sendPost(final String path)
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .timeout(DEFAULT_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        execute(request);
    }

    private void sendDelete(final String path)
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .timeout(DEFAULT_TIMEOUT)
            .DELETE()
            .build();
        execute(request);
    }

    private String sendGet(final String path)
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .timeout(DEFAULT_TIMEOUT)
            .GET()
            .build();
        final HttpResponse<String> response = execute(request);
        return response.body();
    }

    private HttpResponse<String> execute(final HttpRequest request)
    {
        try
        {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400)
            {
                throw new IllegalStateException("TestServerClient request failed [" + response.statusCode() + "]: "
                    + request.uri() + " -> " + response.body());
            }
            return response;
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Network error calling test server at " + request.uri(), e);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted calling test server at " + request.uri(), e);
        }
    }

    private static String encode(final String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
