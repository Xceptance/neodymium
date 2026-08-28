package com.xceptance.neodymium.junit5.tests.auramanager.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests execution endpoints (run queue, stop, SSE status, disconnect, concurrent operations).
 */
public final class AuraManagerExecutionApiTest
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
        server = NeodymiumAuraManager.startServer(18104, true);
        port = server.getAddress().getPort();
        client = HttpClient.newHttpClient();
        resetRunningQueue();
    }

    @AfterEach
    public void tearDown()
    {
        NeodymiumAuraManager.stopServer(server);
        resetRunningQueue();
    }

    private void resetRunningQueue()
    {
        try
        {
            final Field field = NeodymiumAuraManager.class.getDeclaredField("runningQueue");
            field.setAccessible(true);
            final AtomicBoolean runningQueue = (AtomicBoolean) field.get(null);
            runningQueue.set(false);
        }
        catch (final Exception e)
        {
            // Ignore reflection issues
        }
    }

    @Test
    public void testRunQueueWithDatasets() throws IOException, InterruptedException
    {
        final Map<String, Object> datasetSelection = Map.of(
            "file", "dummy-test.yml",
            "id", "1"
        );
        final Map<String, Object> requestBodyMap = Map.of(
            "datasets", List.of(datasetSelection),
            "headless", true,
            "interactive", false,
            "allure", false
        );
        final String requestBody = gson.toJson(requestBodyMap);
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/run"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, response.statusCode());
        final Map<?, ?> result = gson.fromJson(response.body(), Map.class);
        Assertions.assertEquals(true, result.get("success"));
    }

    @Test
    public void testRunQueueMissingParameters() throws IOException, InterruptedException
    {
        final Map<String, Object> requestBodyMap = Map.of(
            "headless", true,
            "interactive", false
        );
        final String requestBody = gson.toJson(requestBodyMap);
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/run"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(400, response.statusCode());
    }

    @Test
    public void testRunQueueWhenAlreadyRunning() throws Exception
    {
        // 1. Simulate active execution by setting runningQueue to true via reflection
        final Field field = NeodymiumAuraManager.class.getDeclaredField("runningQueue");
        field.setAccessible(true);
        final AtomicBoolean runningQueue = (AtomicBoolean) field.get(null);
        runningQueue.set(true);

        // 2. Post a run request, which should be rejected with 409 Conflict
        final Map<String, Object> datasetSelection = Map.of("file", "dummy-test.yml", "id", "1");
        final String requestBody = gson.toJson(Map.of("datasets", List.of(datasetSelection)));
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/run"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(409, response.statusCode());
        Assertions.assertTrue(response.body().contains("already executing"));
    }

    @Test
    public void testStopProcess() throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/stop"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertEquals(200, response.statusCode());
        final Map<?, ?> result = gson.fromJson(response.body(), Map.class);
        Assertions.assertEquals(true, result.get("success"));
    }

    @Test
    public void testConcurrentRequests() throws Exception
    {
        final CompletableFuture<HttpResponse<InputStream>> sseFuture = client.sendAsync(
            HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/api/status")).GET().build(),
            HttpResponse.BodyHandlers.ofInputStream()
        );

        Thread.sleep(500);

        final HttpRequest listRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/files"))
            .GET()
            .build();
        final HttpResponse<String> listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, listResponse.statusCode());
        Assertions.assertNotNull(gson.fromJson(listResponse.body(), List.class));

        sseFuture.cancel(true);
    }

    @Test
    public void testDisconnectClient() throws Exception
    {
        final CompletableFuture<HttpResponse<InputStream>> sseFuture = client.sendAsync(
            HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/api/status?clientId=test-client-123")).GET().build(),
            HttpResponse.BodyHandlers.ofInputStream()
        );

        Thread.sleep(500);

        final HttpRequest disconnectRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/disconnect?clientId=test-client-123"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        final HttpResponse<String> disconnectResponse = client.send(disconnectRequest, HttpResponse.BodyHandlers.ofString());

        Assertions.assertEquals(200, disconnectResponse.statusCode());
        final Map<?, ?> result = gson.fromJson(disconnectResponse.body(), Map.class);
        Assertions.assertEquals(true, result.get("success"));

        Thread.sleep(500);
    }
}
