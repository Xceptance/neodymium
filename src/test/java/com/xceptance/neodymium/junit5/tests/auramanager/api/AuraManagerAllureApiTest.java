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
 * Tests Allure report endpoints: generating reports, reading history, serving report files, and deleting reports.
 */
public final class AuraManagerAllureApiTest
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
        server = NeodymiumAuraManager.startServer(18106, true);
        port = server.getAddress().getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    public void tearDown()
    {
        NeodymiumAuraManager.stopServer(server);
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
    public void testGenerateAllureReport() throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/allure/generate"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertEquals(200, response.statusCode());
        final Map<?, ?> result = gson.fromJson(response.body(), Map.class);
        Assertions.assertEquals(true, result.get("success"));
    }

    @Test
    public void testServeReportFileAndSecurity() throws IOException, InterruptedException
    {
        final File historyDir = new File("allure-reports-history").getAbsoluteFile();
        if (!historyDir.exists())
        {
            historyDir.mkdirs();
        }

        final String reportId = "test-report-serve-123";
        final File reportDir = new File(historyDir, reportId).getAbsoluteFile();
        if (!reportDir.exists())
        {
            reportDir.mkdirs();
        }

        final File dummyFile = new File(reportDir, "index.html").getAbsoluteFile();
        Files.writeString(dummyFile.toPath(), "<html>dummy report</html>", StandardCharsets.UTF_8);

        try
        {
            // 1. Valid request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/allure/report/" + reportId + "/index.html"))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertTrue(response.body().contains("dummy report"));

            // 2. Directory traversal attempt
            request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/allure/report/../../pom.xml"))
                .GET()
                .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(403, response.statusCode());
        }
        finally
        {
            if (dummyFile.exists())
            {
                dummyFile.delete();
            }
            if (reportDir.exists())
            {
                reportDir.delete();
            }
        }
    }

    @Test
    public void testServeReportNonExistentFile() throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/allure/report/test-report-nonexistent-999/index.html"))
            .GET()
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, response.statusCode());
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
    public void testDeleteNonExistentReport() throws IOException, InterruptedException
    {
        final String deleteBody = gson.toJson(Map.of("id", "test-report-nonexistent-999"));
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/allure/delete"))
            .POST(HttpRequest.BodyPublishers.ofString(deleteBody))
            .header("Content-Type", "application/json")
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, response.statusCode());
    }
}
