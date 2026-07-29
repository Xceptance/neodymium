package com.xceptance.neodymium.junit5.tests.auramanager.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests execution endpoints (run queue, stop, SSE status, disconnect, concurrent operations).
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
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
        server = NeodymiumAuraManager.startServer(18104, false);
        port = server.getAddress().getPort();
        client = HttpClient.newHttpClient();
        resetManagerFields();
    }

    @AfterEach
    public void tearDown()
    {
        NeodymiumAuraManager.stopServer(server);
        resetManagerFields();
    }

    @SuppressWarnings("unchecked")
    private void resetManagerFields()
    {
        try
        {
            final NeodymiumAuraManager manager = NeodymiumAuraManager.getActiveManager(server);
            if (manager != null && manager.getMainHandler() != null)
            {
                final AuraQueueService queueService = manager.getMainHandler().getQueueService();
                if (queueService != null)
                {
                    final Field runningField = AuraQueueService.class.getDeclaredField("runningQueue");
                    runningField.setAccessible(true);
                    final AtomicBoolean runningQueue = (AtomicBoolean) runningField.get(queueService);
                    runningQueue.set(false);

                    final Field stopField = AuraQueueService.class.getDeclaredField("manuallyStopped");
                    stopField.setAccessible(true);
                    final AtomicBoolean manuallyStopped = (AtomicBoolean) stopField.get(queueService);
                    manuallyStopped.set(false);
                }

                final AuraInteractiveService interactiveService = manager.getMainHandler().getInteractiveService();
                if (interactiveService != null)
                {
                    final Field runIdField = AuraInteractiveService.class.getDeclaredField("lastProcessedRunId");
                    runIdField.setAccessible(true);
                    final AtomicReference<String> lastProcessedRunId = (AtomicReference<String>) runIdField.get(interactiveService);
                    lastProcessedRunId.set(null);

                    final Field engineField = AuraInteractiveService.class.getDeclaredField("currentConsoleEngine");
                    engineField.setAccessible(true);
                    final AtomicReference<com.xceptance.neodymium.ai.console.InteractiveConsoleEngine> currentConsoleEngine = (AtomicReference<com.xceptance.neodymium.ai.console.InteractiveConsoleEngine>) engineField.get(interactiveService);
                    currentConsoleEngine.set(null);
                }
            }
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
        Assertions.assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        Assertions.assertTrue(response.body().contains("runControls") || response.body().contains("statsPanel"));
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
        // 1. Simulate active execution by setting runningQueue to true via queueService
        final NeodymiumAuraManager manager = NeodymiumAuraManager.getActiveManager(server);
        Assertions.assertNotNull(manager);
        final AuraQueueService queueService = manager.getMainHandler().getQueueService();
        Assertions.assertNotNull(queueService);

        final Field field = AuraQueueService.class.getDeclaredField("runningQueue");
        field.setAccessible(true);
        final AtomicBoolean runningQueue = (AtomicBoolean) field.get(queueService);
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
        Assertions.assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        Assertions.assertTrue(response.body().contains("runControls") || response.body().contains("statsPanel"));
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

    @Test
    public void testInteractiveStopFlow() throws Exception
    {
        // 1. Initial pushState for run-1 should return status:ok
        final String requestBody1 = gson.toJson(Map.of("runId", "run-1", "step", "starting"));
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/console/internal/pushState"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody1))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        final Map<?, ?> result = gson.fromJson(response.body(), Map.class);
        Assertions.assertEquals("ok", result.get("status"));

        // 2. Trigger stop via POST /api/stop
        final HttpRequest stopRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/stop"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        final HttpResponse<String> stopResponse = client.send(stopRequest, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, stopResponse.statusCode());

        // 3. Subsequent pushState for run-1 should return status:stopped
        final String requestBody2 = gson.toJson(Map.of("runId", "run-1", "step", "middle"));
        final HttpRequest request2 = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/console/internal/pushState"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody2))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response2.statusCode());
        final Map<?, ?> result2 = gson.fromJson(response2.body(), Map.class);
        Assertions.assertEquals("stopped", result2.get("status"));

        // 4. GET waitForAction should immediately return ABORT
        final HttpRequest waitRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/console/internal/waitForAction"))
            .GET()
            .build();
        final HttpResponse<String> waitResponse = client.send(waitRequest, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, waitResponse.statusCode());
        final Map<?, ?> waitResult = gson.fromJson(waitResponse.body(), Map.class);
        Assertions.assertEquals("ABORT", waitResult.get("action"));

        // 5. If we start a NEW run (run-2), pushState should detect the runId change, reset stopped flag and return status:ok
        final String requestBody3 = gson.toJson(Map.of("runId", "run-2", "step", "starting"));
        final HttpRequest request3 = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/console/internal/pushState"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody3))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response3 = client.send(request3, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response3.statusCode());
        final Map<?, ?> result3 = gson.fromJson(response3.body(), Map.class);
        Assertions.assertEquals("ok", result3.get("status"));
    }
}
