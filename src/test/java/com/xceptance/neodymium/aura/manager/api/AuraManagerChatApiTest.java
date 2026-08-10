package com.xceptance.neodymium.aura.manager.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;

/**
 * Tests the Aura Assistant chat/LLM endpoints.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("api")
@Tag("aura-manager")
public final class AuraManagerChatApiTest
{
    private HttpServer server;
    private int port;
    private HttpClient client;
    private final Gson gson = new Gson();

    @BeforeAll
    public static void beforeAll()
    {
        System.setProperty("neodymium.aura.test", "true");
    }

    @BeforeEach
    public void setUp() throws IOException
    {
        server = NeodymiumAuraManager.startServer(18108, false);
        port = server.getAddress().getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    public void tearDown()
    {
        NeodymiumAuraManager.stopServer(server);
    }

    @Test
    public void testChatMissingPrompt() throws IOException, InterruptedException
    {
        final String requestBody = gson.toJson(Map.of("prompt", ""));
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/chat"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(response.body().contains("prompt"));
    }

    @Test
    public void testChatMissingApiKey() throws IOException, InterruptedException
    {
        final String originalKey = System.getProperty("neodymium.ai.apiKey");
        System.setProperty("neodymium.ai.apiKey", "");
        try
        {
            final String requestBody = gson.toJson(Map.of("prompt", "hello"));
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/chat"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .build();
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            Assertions.assertEquals(400, response.statusCode());
            Assertions.assertTrue(response.body().contains("API Key is missing or invalid"));
        }
        finally
        {
            if (originalKey != null)
            {
                System.setProperty("neodymium.ai.apiKey", originalKey);
            }
            else
            {
                System.clearProperty("neodymium.ai.apiKey");
            }
        }
    }

    @Test
    public void testChatWithDummyApiKeyReturnsError() throws IOException, InterruptedException
    {
        final String originalKey = System.getProperty("neodymium.ai.apiKey");
        System.setProperty("neodymium.ai.apiKey", "invalid_dummy_key");
        System.setProperty("neodymium.ai.gemini.apiKey", "invalid_dummy_key");
        try
        {
            final String requestBody = gson.toJson(Map.of("prompt", "hello"));
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/chat"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .build();
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Assertions.assertTrue(response.statusCode() == 200, "Expected status code 200, got: " + response.statusCode());
            final String lowerBody = response.body().toLowerCase();
            Assertions.assertTrue(lowerBody.contains("api key") || lowerBody.contains("invalid") || lowerBody.contains("error") || lowerBody.contains("llm"));
        }
        finally
        {
            if (originalKey != null)
            {
                System.setProperty("neodymium.ai.apiKey", originalKey);
            }
            else
            {
                System.clearProperty("neodymium.ai.apiKey");
            }
        }
    }
}
