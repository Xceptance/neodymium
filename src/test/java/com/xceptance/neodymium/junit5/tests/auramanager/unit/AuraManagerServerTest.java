package com.xceptance.neodymium.junit5.tests.auramanager.unit;

import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests server binding, dashboard serving, and browser configuration overrides.
 */
public final class AuraManagerServerTest
{
    private HttpServer server;
    private int port;
    private HttpClient client;

    @BeforeAll
    public static void beforeAll()
    {
        System.setProperty("neodymium.aura.test", "true");
    }

    @BeforeEach
    public void setUp() throws IOException
    {
        server = NeodymiumAuraManager.startServer(18110, true);
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
    public void testEnforcedPortConflictThrowsException()
    {
        final IOException exception = Assertions.assertThrows(IOException.class, () -> {
            NeodymiumAuraManager.startServer(port, true);
        });
        Assertions.assertTrue(exception.getMessage().contains("is already in use"));
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
}
