package com.xceptance.neodymium.junit5.tests.auramanager.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.util.Neodymium;
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
 * Tests Reporting history, report serving, compilation triggers, and report deletions.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerReportingApiTest
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
            .uri(URI.create("http://127.0.0.1:" + port + "/api/reporting/history"))
            .GET()
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertEquals(200, response.statusCode());
        final List<?> history = gson.fromJson(response.body(), List.class);
        Assertions.assertNotNull(history);
    }

    @Test
    public void testGenerateReportingReport() throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/reporting/generate"))
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
        final File historyDir = new com.xceptance.neodymium.aura.AuraReportingService().getReportHistoryDir();
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

        final File allureDir = new File(reportDir, "allure-report");
        allureDir.mkdirs();
        final File dummyFile = new File(allureDir, "index.html").getAbsoluteFile();
        Files.writeString(dummyFile.toPath(), "<html>dummy report</html>", StandardCharsets.UTF_8);

        try
        {
            // 1. Valid request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/reporting/report/" + reportId + "/allure-report/index.html"))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Assertions.assertEquals(200, response.statusCode());
            Assertions.assertTrue(response.body().contains("dummy report"));

            // 2. Directory traversal attempt
            request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/reporting/report/../../pom.xml"))
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
            if (allureDir.exists())
            {
                allureDir.delete();
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
            .uri(URI.create("http://127.0.0.1:" + port + "/api/reporting/report/test-report-nonexistent-999/index.html"))
            .GET()
            .build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, response.statusCode());
    }

    @Test
    public void testDeleteReportAndSecurity() throws IOException, InterruptedException
    {
        final File historyDir = new com.xceptance.neodymium.aura.AuraReportingService().getReportHistoryDir();
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
            .uri(URI.create("http://127.0.0.1:" + port + "/api/reporting/delete"))
            .POST(HttpRequest.BodyPublishers.ofString(deleteBody))
            .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertFalse(reportDir.exists());

        // 2. Perform a directory traversal exploit attempt
        final String badDeleteBody = gson.toJson(Map.of("id", "../pom.xml"));
        request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:" + port + "/api/reporting/delete"))
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
            .uri(URI.create("http://127.0.0.1:" + port + "/api/reporting/delete"))
            .POST(HttpRequest.BodyPublishers.ofString(deleteBody))
            .header("Content-Type", "application/json")
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(404, response.statusCode());
    }

    @Test
    public void testReportHistoryDirResolution()
    {
        // 1. Verify default relative path configuration
        final File defaultDir = new com.xceptance.neodymium.aura.AuraReportingService().getReportHistoryDir();
        Assertions.assertNotNull(defaultDir);
        Assertions.assertTrue(defaultDir.isAbsolute());
        Assertions.assertEquals("report-history", defaultDir.getName());

        // 2. Verify custom absolute path configuration override (via system property)
        final String tempAbsoluteDir = System.getProperty("java.io.tmpdir") + File.separator + "absolute-report-history-test";
        System.setProperty("neodymium.aura.reportHistoryDir", tempAbsoluteDir);
        // Refresh configuration context
        Neodymium.clearThreadContext();

        try
        {
            final File resolvedAbsoluteDir = new com.xceptance.neodymium.aura.AuraReportingService().getReportHistoryDir();
            Assertions.assertEquals(new File(tempAbsoluteDir).getAbsoluteFile(), resolvedAbsoluteDir);
        }
        finally
        {
            System.clearProperty("neodymium.aura.reportHistoryDir");
            Neodymium.clearThreadContext();
        }
    }
}
