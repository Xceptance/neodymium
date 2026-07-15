/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.executor.rest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.ActionDefinition;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;

/**
 * Concrete target executor driving REST API endpoints using JDK HttpClient.
 * Automatically injects registered Basic and Bearer auth headers into outgoing requests.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class RestTargetExecutor implements TargetExecutor
{
    /**
     * The HTTP client used for REST requests.
     */
    private final HttpClient httpClient;

    /**
     * Map storing registered authorization headers.
     */
    private final Map<String, String> authHeaders = new ConcurrentHashMap<>();

    /**
     * The response body text of the last executed REST call.
     */
    private volatile String lastResponseBody = "{}";

    /**
     * The status code of the last executed REST call.
     */
    private volatile int lastResponseStatus = 200;

    /**
     * Constructs a RestTargetExecutor with a default HttpClient configuration.
     */
    public RestTargetExecutor()
    {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Registers username/password credentials for HTTP Basic Authentication.
     *
     * @param username the authentication username
     * @param password the authentication password
     */
    public void registerBasicAuth(final String username, final String password)
    {
        if (username != null && password != null)
        {
            final String credentials = username + ":" + password;
            final String base64 = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            this.authHeaders.put("Authorization", "Basic " + base64);
        }
    }

    /**
     * Registers a Bearer token for HTTP Bearer Authentication.
     *
     * @param token the bearer token
     */
    public void registerBearerToken(final String token)
    {
        if (token != null)
        {
            this.authHeaders.put("Authorization", "Bearer " + token);
        }
    }

    /**
     * Captures the state of the REST API (returns the last response body).
     *
     * @return the captured REST state representation
     * @throws IOException if state capture fails
     */
    @Override
    public SutState captureState(final org.neodymium.ai.executor.selenide.ContextLevel level) throws IOException
    {
        return new MockSutState(this.lastResponseBody, String.valueOf(this.lastResponseStatus));
    }

    /**
     * Executes HTTP REST operations (GET, POST, PUT, DELETE) on the target URI.
     *
     * @param action the executable HTTP action
     * @throws IOException if the REST call fails
     */
    @Override
    public void execute(final Action action) throws IOException
    {
        if (action == null)
        {
            return;
        }

        final String method = action.getType().toUpperCase();
        final String targetUrl = action.getTarget();
        final String bodyContent = action.getValue();

        try
        {
            final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(30));

            // Inject registered auth headers
            for (final Map.Entry<String, String> header : this.authHeaders.entrySet())
            {
                builder.header(header.getKey(), header.getValue());
            }

            final HttpRequest.BodyPublisher bodyPublisher = (bodyContent == null) 
                ? HttpRequest.BodyPublishers.noBody() 
                : HttpRequest.BodyPublishers.ofString(bodyContent);

            switch (method)
            {
                case "GET":
                    builder.GET();
                    break;
                case "POST":
                    builder.POST(bodyPublisher);
                    break;
                case "PUT":
                    builder.PUT(bodyPublisher);
                    break;
                case "DELETE":
                    builder.DELETE();
                    break;
                default:
                    throw new IOException("Unsupported REST method type: " + method);
            }

            final HttpResponse<String> response = this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            this.lastResponseBody = response.body();
            this.lastResponseStatus = response.statusCode();

            if (response.statusCode() >= 400)
            {
                throw new IOException("REST endpoint returned error status: " + response.statusCode() + " - " + response.body());
            }
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException("REST execution interrupted", e);
        }
        catch (final Exception e)
        {
            throw new IOException("Failed executing REST action: " + action.getDescription(), e);
        }
    }

    /**
     * Retrieves the set of supported REST actions.
     *
     * @return the set of supported ActionDefinitions
     */
    @Override
    public Set<ActionDefinition> getSupportedActions()
    {
        return Set.of(
            new ActionDefinition("GET", "Execute HTTP GET request", Collections.emptyMap()),
            new ActionDefinition("POST", "Execute HTTP POST request", Collections.emptyMap()),
            new ActionDefinition("PUT", "Execute HTTP PUT request", Collections.emptyMap()),
            new ActionDefinition("DELETE", "Execute HTTP DELETE request", Collections.emptyMap())
        );
    }
}
