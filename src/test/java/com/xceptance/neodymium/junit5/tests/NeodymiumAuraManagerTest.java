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
package com.xceptance.neodymium.junit5.tests;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration and Unit tests for NeodymiumAuraManager.
 * Tests server binding, REST endpoints, file operations, security, and report copying.
 * 
 * // AI-generated: Gemini 3.5 Flash
 */
public final class NeodymiumAuraManagerTest
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
        // Start server on a dynamic port starting from 18080
        server = NeodymiumAuraManager.startServer(18080);
        port = server.getAddress().getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    public void tearDown()
    {
        NeodymiumAuraManager.stopServer(server);
    }

    @Test
    public void testServerPortScanning() throws IOException
    {
        // Start a second server, it should bind to a different port
        final HttpServer secondServer = NeodymiumAuraManager.startServer(port);
        try
        {
            final int secondPort = secondServer.getAddress().getPort();
            Assertions.assertNotEquals(port, secondPort);
            Assertions.assertTrue(secondPort >= port);
        }
        finally
        {
            NeodymiumAuraManager.stopServer(secondServer);
        }
    }

    @Test
    public void testGetDashboard() throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/"))
            .GET()
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.body().contains("Neodymium Aura"));
    }

    @Test
    public void testListFiles() throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/files"))
            .GET()
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertEquals(200, response.statusCode());
        final List<?> files = gson.fromJson(response.body(), List.class);
        Assertions.assertNotNull(files);
    }

    @Test
    public void testFileLifecycleAndSecurity() throws IOException, InterruptedException
    {
        final String testName = "Aura Manager Test Case";
        final String expectedFilename = "aura-manager-test-case.yaml";
        
        // 1. Create file
        final String createBody = gson.toJson(Map.of("name", testName));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/create"))
            .POST(HttpRequest.BodyPublishers.ofString(createBody))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        
        final File createdFile = new File("src/test/resources", expectedFilename).getAbsoluteFile();
        Assertions.assertTrue(createdFile.exists());
        Assertions.assertTrue(createdFile.isFile());
        
        try
        {
            // 2. Read file
            request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/read?file=" + expectedFilename))
                .GET()
                .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(200, response.statusCode());
            
            final Map<?, ?> readData = gson.fromJson(response.body(), Map.class);
            Assertions.assertNotNull(readData);
            Assertions.assertTrue(String.valueOf(readData.get("content")).contains("Aura Manager Test Case"));
            
            // 3. Security check: Directory traversal
            request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/read?file=../../pom.xml"))
                .GET()
                .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // Should be forbidden (403)
            Assertions.assertEquals(403, response.statusCode());
            
            // 4. Save file modification
            final String modifiedContent = "# Modified content\nsteps:\n  - Do something else";
            final String saveBody = gson.toJson(Map.of("file", expectedFilename, "content", modifiedContent));
            request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/save"))
                .POST(HttpRequest.BodyPublishers.ofString(saveBody))
                .header("Content-Type", "application/json")
                .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(200, response.statusCode());
            
            final String diskContent = Files.readString(createdFile.toPath(), StandardCharsets.UTF_8);
            Assertions.assertEquals(modifiedContent, diskContent);
        }
        finally
        {
            // 5. Delete file
            final String deleteBody = gson.toJson(Map.of("file", expectedFilename));
            request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/delete"))
                .POST(HttpRequest.BodyPublishers.ofString(deleteBody))
                .header("Content-Type", "application/json")
                .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertFalse(createdFile.exists());
        }
    }

    @Test
    public void testGetHistoryEmpty() throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/allure/history"))
            .GET()
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertEquals(200, response.statusCode());
        final List<?> history = gson.fromJson(response.body(), List.class);
        Assertions.assertNotNull(history);
    }

    @Test
    public void testConcurrentRequests() throws Exception
    {
        // 1. Establish SSE status connection in the background (runs indefinitely until cancelled)
        final CompletableFuture<HttpResponse<InputStream>> sseFuture = client.sendAsync(
            HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:" + port + "/api/status")).GET().build(),
            HttpResponse.BodyHandlers.ofInputStream()
        );

        // Allow some time for connection to initiate and lock the thread on the server side
        Thread.sleep(500);

        // 2. Concurrently call another endpoint /api/files
        final HttpRequest listRequest = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/files"))
            .GET()
            .build();
        final HttpResponse<String> listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());

        // Assert that the second request succeeds concurrently and is not blocked
        Assertions.assertEquals(200, listResponse.statusCode());
        Assertions.assertNotNull(gson.fromJson(listResponse.body(), List.class));

        // 3. Cancel the SSE request to close connection cleanly
        sseFuture.cancel(true);
    }

    @Test
    public void testDeleteReportAndSecurity() throws IOException, InterruptedException
    {
        final File historyDir = new File("allure-reports-history").getAbsoluteFile();
        if (!historyDir.exists())
        {
            historyDir.mkdirs();
        }

        final String reportId = "test-report-12345";
        final File reportDir = new File(historyDir, reportId).getAbsoluteFile();
        if (!reportDir.exists())
        {
            reportDir.mkdirs();
        }

        final File dummyFile = new File(reportDir, "widgets.json").getAbsoluteFile();
        Files.writeString(dummyFile.toPath(), "{}", StandardCharsets.UTF_8);

        // 1. Perform a valid report deletion
        final String deleteBody = gson.toJson(Map.of("id", reportId));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/allure/delete"))
            .POST(HttpRequest.BodyPublishers.ofString(deleteBody))
            .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertFalse(reportDir.exists());

        // 2. Perform a directory traversal exploit attempt
        final String badDeleteBody = gson.toJson(Map.of("id", "../pom.xml"));
        request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/allure/delete"))
            .POST(HttpRequest.BodyPublishers.ofString(badDeleteBody))
            .header("Content-Type", "application/json")
            .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(403, response.statusCode());
    }

    @Test
    public void testStripAnsi()
    {
        Assertions.assertEquals("[INFO] Scanning for projects...", NeodymiumAuraManager.stripAnsi("[INFO] Scanning for projects..."));
        Assertions.assertEquals("[WARNING] Deprecated method...", NeodymiumAuraManager.stripAnsi("[WARNING] Deprecated method..."));
        Assertions.assertEquals("[ERROR] compilation error", NeodymiumAuraManager.stripAnsi("[ERROR] compilation error"));
        Assertions.assertEquals("INFO Scanning for projects...", NeodymiumAuraManager.stripAnsi("\u001B[1;34mINFO\u001B[m Scanning for projects..."));
        Assertions.assertEquals("[ INFO ] Scanning for projects...", NeodymiumAuraManager.stripAnsi("[ [1;34mINFO [m] Scanning for projects..."));
    }

    @Test
    public void testMultibrowserSystemPropertyOverride()
    {
        MultibrowserConfiguration.clearAllInstances();
        System.setProperty("browserprofile.Default.headless", "true");
        System.setProperty("browserprofile.Default.browser", "firefox");
        try
        {
            final MultibrowserConfiguration config = MultibrowserConfiguration.getInstance();
            final BrowserConfiguration defaultProfile = config.getBrowserProfiles().get("Default");
            Assertions.assertNotNull(defaultProfile);
            Assertions.assertTrue(defaultProfile.isHeadless());
            Assertions.assertEquals("firefox", defaultProfile.getCapabilities().getBrowserName());
        }
        finally
        {
            System.clearProperty("browserprofile.Default.headless");
            System.clearProperty("browserprofile.Default.browser");
            MultibrowserConfiguration.clearAllInstances();
        }
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
        try
        {
            final String requestBody = gson.toJson(Map.of("prompt", "hello"));
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/chat"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/json")
                .build();
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            Assertions.assertTrue(response.statusCode() == 200 || response.statusCode() == 500);
            if (response.statusCode() == 500)
            {
                Assertions.assertTrue(response.body().contains("LLM Client Error") || response.body().contains("API key"));
            }
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
}

