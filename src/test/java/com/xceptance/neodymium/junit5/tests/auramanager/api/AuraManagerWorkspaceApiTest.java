package com.xceptance.neodymium.junit5.tests.auramanager.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests workspace file operations (list, read, save, create, delete) and directory traversal protection.
 */
public final class AuraManagerWorkspaceApiTest
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
        server = NeodymiumAuraManager.startServer(18102, true);
        port = server.getAddress().getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    public void tearDown()
    {
        NeodymiumAuraManager.stopServer(server);
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
    public void testReadNonExistentFile() throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/read?file=does-not-exist-file-12345.yaml"))
            .GET()
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, response.statusCode());
    }

    @Test
    public void testReadEmptyFilename() throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/read?file="))
            .GET()
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(400, response.statusCode());
    }

    @Test
    public void testDeleteNonExistentFile() throws IOException, InterruptedException
    {
        final String deleteBody = gson.toJson(Map.of("file", "does-not-exist-file-12345.yaml"));
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/delete"))
            .POST(HttpRequest.BodyPublishers.ofString(deleteBody))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, response.statusCode());
    }

    @Test
    public void testCreateEmptyFilename() throws IOException, InterruptedException
    {
        final String createBody = gson.toJson(Map.of("name", ""));
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/create"))
            .POST(HttpRequest.BodyPublishers.ofString(createBody))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(400, response.statusCode());
    }

    @Test
    public void testCreateDuplicateFile() throws IOException, InterruptedException
    {
        final String testName = "Duplicate Test Case";
        final String expectedFilename = "duplicate-test-case.yaml";
        
        final String createBody = gson.toJson(Map.of("name", testName));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/create"))
            .POST(HttpRequest.BodyPublishers.ofString(createBody))
            .header("Content-Type", "application/json")
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        
        try
        {
            // Try to create again
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(400, response.statusCode());
            Assertions.assertTrue(response.body().contains("already exists"));
        }
        finally
        {
            final File file = new File("src/test/resources", expectedFilename);
            if (file.exists())
            {
                file.delete();
            }
        }
    }

    @Test
    public void testSaveInvalidParameters() throws IOException, InterruptedException
    {
        final String saveBody = gson.toJson(Map.of("file", "test.yaml")); // missing content
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/save"))
            .POST(HttpRequest.BodyPublishers.ofString(saveBody))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(400, response.statusCode());
    }

    @Test
    public void testSaveDirectoryTraversal() throws IOException, InterruptedException
    {
        final String saveBody = gson.toJson(Map.of("file", "../../pom.xml", "content", "hack"));
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/save"))
            .POST(HttpRequest.BodyPublishers.ofString(saveBody))
            .header("Content-Type", "application/json")
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(403, response.statusCode());
    }
}
