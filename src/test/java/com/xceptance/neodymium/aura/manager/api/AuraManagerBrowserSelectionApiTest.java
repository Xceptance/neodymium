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
package com.xceptance.neodymium.aura.manager.api;

import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests browser profile endpoints, multi-selection toggles, presets, and per-item profile overrides.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Tag("api")
@Tag("aura-manager")
public final class AuraManagerBrowserSelectionApiTest
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
        server = NeodymiumAuraManager.startServer(18105, false);
        port = server.getAddress().getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    public void tearDown()
    {
        NeodymiumAuraManager.stopServer(server);
    }

    @Test
    public final void testToggleBrowserProfile() throws IOException, InterruptedException
    {
        final String jsonBody = "{\"profile\":\"Chrome_1024x768\"}";
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/config/browser/toggle"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.body().contains("Browser Profiles"));
    }

    @Test
    public final void testBrowserPresets() throws IOException, InterruptedException
    {
        // 1. Preset desktop
        final HttpRequest reqDesktop = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/config/browser/preset?preset=desktop"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        final HttpResponse<String> resDesktop = client.send(reqDesktop, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resDesktop.statusCode());
        Assertions.assertTrue(resDesktop.body().contains("Browser Profiles"));

        // 2. Preset clear
        final HttpRequest reqClear = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/config/browser/preset?preset=clear"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        final HttpResponse<String> resClear = client.send(reqClear, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resClear.statusCode());
        Assertions.assertTrue(resClear.body().contains("0 active") || resClear.body().contains("0 selected"));

        // 3. Preset all
        final HttpRequest reqAll = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/config/browser/preset?preset=all"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        final HttpResponse<String> resAll = client.send(reqAll, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resAll.statusCode());
    }

    @Test
    public final void testItemBrowserProfilesOverride() throws IOException, InterruptedException
    {
        // First add an item to the queue
        final String toggleQueueBody = "{\"file\":\"test-sample.yaml\",\"id\":\"dataset1\"}";
        final HttpRequest reqQueue = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/queue/toggle"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toggleQueueBody))
                .build();
        client.send(reqQueue, HttpResponse.BodyHandlers.ofString());

        // Update item browser profiles with custom profile
        final String formBody = "index=0&inherit=false&profiles=Chrome_1024x768,FF_1024x768";
        final HttpRequest reqItemBrowser = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/queue/item-browser"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        final HttpResponse<String> resItem = client.send(reqItemBrowser, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resItem.statusCode());
        Assertions.assertTrue(resItem.body().contains("queue-browser-pill"));
        Assertions.assertTrue(resItem.body().contains("customized"));

        // Set back to inherit
        final String formBodyInherit = "index=0&inherit=true&profiles=";
        final HttpRequest reqInherit = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/queue/item-browser"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBodyInherit))
                .build();
        final HttpResponse<String> resInherit = client.send(reqInherit, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, resInherit.statusCode());
        Assertions.assertTrue(resInherit.body().contains("Global ("));
    }
}
