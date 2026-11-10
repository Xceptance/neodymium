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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.server.client.TestServerClient;
import org.neodymium.ai.server.session.SessionManager;

/**
 * Integration and unit tests for {@link JavalinTestServer} validating Aura Glance
 * test hub routing, sandboxes, directory redirects, simulation states, and dual connectors.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class JavalinAuraGlanceServerTest
{
    private static JavalinTestServer server;
    private static HttpClient client;
    private static HttpClient trustAllSslClient;

    @BeforeAll
    public static void setUp() throws Exception
    {
        server = new JavalinTestServer(0, 0);
        server.start();

        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

        final SSLParameters sslParams = new SSLParameters();
        sslParams.setEndpointIdentificationAlgorithm("");

        trustAllSslClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .sslContext(createTrustAllSslContext())
            .sslParameters(sslParams)
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
    public void testServerLifecycleAndPorts()
    {
        assertTrue(server.isRunning(), "Server should be running");
        assertTrue(server.getPort() > 0, "HTTP port should be bound to a positive port");
        assertTrue(server.getHttpsPort() > 0, "HTTPS port should be bound to a positive port");
        assertTrue(server.getHttpUrl().startsWith("http://127.0.0.1:"));
        assertTrue(server.getHttpsUrl().startsWith("https://127.0.0.1:"));
    }

    @Test
    public void testRootRedirects() throws Exception
    {
        final HttpResponse<String> rootResponse = get(server.getHttpUrl() + "/");
        assertEquals(302, rootResponse.statusCode());
        assertEquals("/AuraGlanceTest/index.html", rootResponse.headers().firstValue("location").orElse(""));

        final HttpResponse<String> indexResponse = get(server.getHttpUrl() + "/index.html");
        assertEquals(302, indexResponse.statusCode());
        assertEquals("/AuraGlanceTest/index.html", indexResponse.headers().firstValue("location").orElse(""));

        final HttpResponse<String> auraGlanceDirResponse = get(server.getHttpUrl() + "/AuraGlanceTest");
        assertEquals(302, auraGlanceDirResponse.statusCode());
        assertEquals("/AuraGlanceTest/index.html", auraGlanceDirResponse.headers().firstValue("location").orElse(""));

        final HttpResponse<String> auraGlanceDirSlashResponse = get(server.getHttpUrl() + "/AuraGlanceTest/");
        assertEquals(302, auraGlanceDirSlashResponse.statusCode());
        assertEquals("/AuraGlanceTest/index.html", auraGlanceDirSlashResponse.headers().firstValue("location").orElse(""));

        final HttpResponse<String> shopDirResponse = get(server.getHttpUrl() + "/AuraGlanceTest/shop");
        assertEquals(302, shopDirResponse.statusCode());
        assertEquals("/AuraGlanceTest/shop/index.html", shopDirResponse.headers().firstValue("location").orElse(""));

        final HttpResponse<String> sandboxDirResponse = get(server.getHttpUrl() + "/AuraGlanceTest/shop/sandbox");
        assertEquals(302, sandboxDirResponse.statusCode());
        assertEquals("/AuraGlanceTest/shop/sandbox/index.html", sandboxDirResponse.headers().firstValue("location").orElse(""));
    }

    @Test
    public void testServeAuraGlancePagesAndSandboxes() throws Exception
    {
        // 1. Aura Glance Hub
        final HttpResponse<String> hubResponse = get(server.getHttpUrl() + "/AuraGlanceTest/index.html");
        assertEquals(200, hubResponse.statusCode());
        assertTrue(hubResponse.headers().firstValue("content-type").orElse("").contains("text/html"));
        assertTrue(hubResponse.body().contains("Neodymium Aura AI - Aura Test Suite Hub"));

        // 2. Shared CSS
        final HttpResponse<String> cssResponse = get(server.getHttpUrl() + "/AuraGlanceTest/shared/style.css");
        assertEquals(200, cssResponse.statusCode());
        assertTrue(cssResponse.headers().firstValue("content-type").orElse("").contains("text/css"));

        // 3. Shop forms
        final HttpResponse<String> formsResponse = get(server.getHttpUrl() + "/AuraGlanceTest/shop/forms.html");
        assertEquals(200, formsResponse.statusCode());
        assertTrue(formsResponse.body().contains("Aura Form Interactions"));

        // 4. Sandbox challenge: Autocomplete
        final HttpResponse<String> autoResponse = get(server.getHttpUrl() + "/AuraGlanceTest/shop/sandbox/autocomplete.html");
        assertEquals(200, autoResponse.statusCode());
        assertTrue(autoResponse.body().contains("Autocomplete"));

        // 5. Sandbox challenge: File upload
        final HttpResponse<String> uploadResponse = get(server.getHttpUrl() + "/AuraGlanceTest/shop/sandbox/file-upload.html");
        assertEquals(200, uploadResponse.statusCode());
        assertTrue(uploadResponse.body().contains("File Upload"));

        // 6. Non-existent resource returns 404
        final HttpResponse<String> notFoundResponse = get(server.getHttpUrl() + "/AuraGlanceTest/nonexistent-fixture.html");
        assertEquals(404, notFoundResponse.statusCode());
    }

    @Test
    public void testDirectoryTraversalBlocked() throws Exception
    {
        final HttpResponse<String> traversalResponse = get(server.getHttpUrl() + "/AuraGlanceTest/../pom.xml");
        assertTrue(traversalResponse.statusCode() == 403 || traversalResponse.statusCode() == 404);
    }

    @Test
    public void testSessionIsolationAndOpaqueCookie() throws Exception
    {
        final String sessionUrl = server.getHttpUrl() + "/AuraGlanceTest/index.html?customSession=isolated-session-123";
        final HttpResponse<String> redirectResponse = get(sessionUrl);

        assertEquals(302, redirectResponse.statusCode());
        assertEquals("/AuraGlanceTest/index.html", redirectResponse.headers().firstValue("location").orElse(""));

        final String setCookie = redirectResponse.headers().firstValue("set-cookie").orElse("");
        assertTrue(setCookie.contains(SessionManager.COOKIE_NAME + "="), "Should set opaque session cookie");
        assertFalse(setCookie.contains("isolated-session-123"), "Opaque token must not leak custom session ID to LLM");

        final String cookieValue = extractCookieValue(setCookie, SessionManager.COOKIE_NAME);
        assertNotNull(cookieValue);

        // Subsequent request using the cookie maps to the same session
        final HttpRequest requestWithCookie = HttpRequest.newBuilder()
            .uri(URI.create(server.getHttpUrl() + "/AuraGlanceTest/index.html"))
            .header("Cookie", SessionManager.COOKIE_NAME + "=" + cookieValue)
            .GET()
            .build();
        final HttpResponse<String> normalResponse = client.send(requestWithCookie, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, normalResponse.statusCode());

        // Verify session manager tracked the session
        assertNotNull(server.getSession("isolated-session-123"));
    }

    @Test
    public void testAuraGlanceSimulationInjection() throws Exception
    {
        final String customSessionId = "sim-session-456";
        final TestServerClient testClient = server.getClient(customSessionId);

        // Initial status: health is UP
        assertEquals("{\"status\":\"UP\"}", testClient.getHealth());

        // Obtain opaque cookie for this session
        final HttpResponse<String> initResponse = get(server.getHttpUrl() + "/AuraGlanceTest/index.html?customSession=" + customSessionId);
        final String cookieHeader = SessionManager.COOKIE_NAME + "=" + extractCookieValue(initResponse.headers().firstValue("set-cookie").orElse(""), SessionManager.COOKIE_NAME);

        // Inject simulated failure for customSessionId
        testClient.setAuraGlanceFailure(true);

        // Request with this session cookie should return 500
        final HttpRequest failedReq = HttpRequest.newBuilder()
            .uri(URI.create(server.getHttpUrl() + "/AuraGlanceTest/index.html"))
            .header("Cookie", cookieHeader)
            .GET()
            .build();
        final HttpResponse<String> failedResp = client.send(failedReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(500, failedResp.statusCode());
        assertTrue(failedResp.body().contains("Simulated Aura Glance Server Failure"));

        // Default session request without cookie continues to return 200 (isolated!)
        final HttpResponse<String> defaultResp = get(server.getHttpUrl() + "/AuraGlanceTest/index.html");
        assertEquals(200, defaultResp.statusCode());

        // Reset simulation for customSessionId
        testClient.resetAuraGlance();
        final HttpResponse<String> restoredResp = client.send(failedReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, restoredResp.statusCode());
    }

    @Test
    public void testHttpsConnector() throws Exception
    {
        final String httpsUrl = "https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/index.html";
        final HttpResponse<String> httpsResp = trustAllSslClient.send(
            HttpRequest.newBuilder().uri(URI.create(httpsUrl)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, httpsResp.statusCode());
        assertTrue(httpsResp.body().contains("Neodymium Aura AI - Aura Test Suite Hub"));
    }

    @Test
    public void testProtocolIntrospectionSimulationEndpoints() throws IOException, InterruptedException
    {
        final HttpResponse<String> faultyResp = get(server.getHttpUrl() + "/AuraGlanceTest/api/faulty-backend");
        assertEquals(500, faultyResp.statusCode());
        assertTrue(faultyResp.body().contains("Internal server fault"));
        assertEquals("application/json", faultyResp.headers().firstValue("Content-Type").orElse("").split(";")[0].trim());

        final HttpResponse<String> missingResp = get(server.getHttpUrl() + "/AuraGlanceTest/api/missing-endpoint");
        assertEquals(404, missingResp.statusCode());
        assertTrue(missingResp.body().contains("Resource missing"));
        assertEquals("application/json", missingResp.headers().firstValue("Content-Type").orElse("").split(";")[0].trim());

        final HttpResponse<String> analyticsResp = get(server.getHttpUrl() + "/AuraGlanceTest/api/analytics");
        assertEquals(404, analyticsResp.statusCode());
        assertTrue(analyticsResp.body().contains("Ignored telemetry"));
        assertEquals("application/json", analyticsResp.headers().firstValue("Content-Type").orElse("").split(";")[0].trim());

        // 4. Challenge page is served
        final HttpResponse<String> pageResp = get(server.getHttpUrl() + "/AuraGlanceTest/shop/sandbox/protocol-introspection.html");
        assertEquals(200, pageResp.statusCode());
        assertTrue(pageResp.body().contains("Protocol & Console Introspection Challenge"));
        assertTrue(pageResp.body().contains("trigger500Btn"));
        assertTrue(pageResp.body().contains("trigger404Btn"));
        assertTrue(pageResp.body().contains("triggerJsErrorBtn"));
        assertTrue(pageResp.body().contains("triggerConsoleErrorBtn"));
        assertTrue(pageResp.body().contains("triggerAnalyticsBtn"));
    }

    private static HttpResponse<String> get(final String url) throws IOException, InterruptedException
    {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String extractCookieValue(final String setCookieHeader, final String cookieName)
    {
        final String prefix = cookieName + "=";
        for (final String part : setCookieHeader.split(";"))
        {
            final String trimmed = part.trim();
            if (trimmed.startsWith(prefix))
            {
                return trimmed.substring(prefix.length());
            }
        }
        return null;
    }

    private static SSLContext createTrustAllSslContext() throws GeneralSecurityException
    {
        final TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager()
            {
                @Override
                public X509Certificate[] getAcceptedIssuers()
                {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(final X509Certificate[] certs, final String authType)
                {
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] certs, final String authType)
                {
                }
            }
        };

        final SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        return sc;
    }
}
