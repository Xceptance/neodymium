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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link JavalinTestServer} verifying dynamic delivery of
 * isolated action test fixtures and sandbox pages from Thymeleaf templates.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class JavalinActionFixturesServerTest
{
    private static JavalinTestServer server;
    private static HttpClient client;

    @BeforeAll
    public static void setUp() throws Exception
    {
        server = new JavalinTestServer(0, -1);
        server.start();

        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    @AfterAll
    public static void tearDown()
    {
        if (server != null && server.isRunning())
        {
            server.stop();
        }
    }

    @Test
    public void testActionFixtureTemplateDelivery() throws Exception
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(server.getHttpUrl() + "/ClickActionTest/testClickStandardButton.html"))
            .GET()
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        final String contentType = response.headers().firstValue("content-type").orElse("");
        assertTrue(contentType.contains("text/html"), "Content-Type should be text/html");

        final String body = response.body();
        assertNotNull(body);
        assertTrue(body.contains("<h1>Click Action Test</h1>"), "Body should contain heading");
        assertTrue(body.contains("id=\"btn-submit\""), "Body should contain button element");
        assertTrue(body.contains("Submit Order"), "Body should contain button label");
    }

    @Test
    public void testMultiLevelActionFixtureTemplateDelivery() throws Exception
    {
        // 1. Parent frameset page
        final HttpRequest parentRequest = HttpRequest.newBuilder()
            .uri(URI.create(server.getHttpUrl() + "/NestedFrameTest/testNestedFrames.html"))
            .GET()
            .build();

        final HttpResponse<String> parentResponse = client.send(parentRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, parentResponse.statusCode());
        assertTrue(parentResponse.body().contains("frame1.html"), "Parent should reference frame1.html");

        // 2. Child iframe template
        final HttpRequest frameRequest = HttpRequest.newBuilder()
            .uri(URI.create(server.getHttpUrl() + "/NestedFrameTest/frame1.html"))
            .GET()
            .build();

        final HttpResponse<String> frameResponse = client.send(frameRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, frameResponse.statusCode());
        assertTrue(frameResponse.body().contains("frame2.html"), "Frame1 should reference frame2.html");
    }

    @Test
    public void testActionIndexTemplateDelivery() throws Exception
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(server.getHttpUrl() + "/action-test-pages.html"))
            .GET()
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        final String contentType = response.headers().firstValue("content-type").orElse("");
        assertTrue(contentType.contains("text/html"), "Content-Type should be text/html");

        final String body = response.body();
        assertNotNull(body);
        assertTrue(body.contains("Aura Sandbox - Isolated Action Test Pages"), "Body should contain index title");
        assertTrue(body.contains("ClickActionTest"), "Body should link to ClickActionTest");
    }

    @Test
    public void testAuraGlanceDynamicTemplateDelivery() throws Exception
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(server.getHttpUrl() + "/AuraGlanceTest/index.html"))
            .GET()
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        final String contentType = response.headers().firstValue("content-type").orElse("");
        assertTrue(contentType.contains("text/html"), "Content-Type should be text/html");

        final String body = response.body();
        assertNotNull(body);
        assertTrue(body.contains("Neodymium Aura AI - Aura Test Suite Hub"), "Body should contain Aura Glance title");
    }

    @Test
    public void testNonExistentTemplateReturns404() throws Exception
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(server.getHttpUrl() + "/ClickActionTest/nonExistentPage.html"))
            .GET()
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    public void testDirectoryTraversalBlocked() throws Exception
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(server.getHttpUrl() + "/ClickActionTest/..%2F..%2Fpom.xml"))
            .GET()
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertFalse(response.statusCode() == 200, "Directory traversal must not return 200 OK");
        assertTrue(response.statusCode() == 403 || response.statusCode() == 400 || response.statusCode() == 404,
            "Directory traversal should be rejected with 403, 400, or 404");
    }

    @Test
    public void testStaticAssetDeliveryNotCorruptedByThymeleaf() throws Exception
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(server.getHttpUrl() + "/AuraGlanceTest/shared/style.css"))
            .GET()
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        final String contentType = response.headers().firstValue("content-type").orElse("");
        assertTrue(contentType.contains("text/css"), "CSS asset Content-Type should be text/css");

        final String body = response.body();
        assertNotNull(body);
        assertTrue(body.contains(":root"), "CSS body should contain root CSS custom properties");
    }
}
