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
package com.xceptance.neodymium.aura.manager.unit;

import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraManagerQueueController;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.AuraReportingService;
import com.xceptance.neodymium.aura.dto.BrowserGroupDto;
import com.xceptance.neodymium.aura.dto.BrowserProfileDto;
import com.xceptance.neodymium.aura.dto.ChatResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying browser profile classification, icon detection accuracy,
 * and AI-driven browser configuration payloads.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("aura-manager")
public final class AuraBrowserSelectionTest
{
    private AuraManagerQueueController queueController;

    @BeforeAll
    public static void beforeAll()
    {
        System.setProperty("neodymium.aura.test", "true");
    }

    @BeforeEach
    public void setUp()
    {
        final AuraReportingService reportingService = new AuraReportingService();
        final AuraInteractiveService interactiveService = new AuraInteractiveService();
        final AuraQueueService queueService = new AuraQueueService(reportingService, interactiveService);
        final AuraFileService fileService = new AuraFileService();
        this.queueController = new AuraManagerQueueController(queueService, fileService, interactiveService, null);
    }

    @Test
    public void testBrowserProfilesClassification()
    {
        final List<BrowserProfileDto> profiles = queueController.getAvailableBrowserProfiles();
        Assertions.assertNotNull(profiles);
        Assertions.assertFalse(profiles.isEmpty());

        for (final BrowserProfileDto p : profiles)
        {
            Assertions.assertNotNull(p.id);
            Assertions.assertNotNull(p.browser);
            Assertions.assertTrue(List.of("chrome", "firefox", "safari", "edge", "mobile", "other").contains(p.browser),
                    "Browser type '" + p.browser + "' should be one of the known browser categories");
        }
    }

    @Test
    public void testBrowserTypesForProfilesAccurateIcons()
    {
        // When setting only Chrome profiles globally
        queueController.setGlobalBrowserProfiles(Set.of("Chrome_1024x768", "Chrome_1500x1000"));
        final List<String> chromeOnlyIcons = queueController.getBrowserTypesForProfiles(null);
        Assertions.assertEquals(1, chromeOnlyIcons.size());
        Assertions.assertEquals("chrome", chromeOnlyIcons.get(0));

        // When setting custom profiles with only Firefox on an item
        final List<String> customFfIcons = queueController.getBrowserTypesForProfiles(List.of("FF_1024x768"));
        Assertions.assertEquals(1, customFfIcons.size());
        Assertions.assertEquals("firefox", customFfIcons.get(0));

        // When setting Chrome + Firefox
        queueController.setGlobalBrowserProfiles(Set.of("Chrome_1024x768", "FF_1024x768"));
        final List<String> dualIcons = queueController.getBrowserTypesForProfiles(null);
        Assertions.assertEquals(2, dualIcons.size());
        Assertions.assertTrue(dualIcons.contains("chrome"));
        Assertions.assertTrue(dualIcons.contains("firefox"));
    }

    @Test
    public void testGroupedBrowserProfilesSelectedCount()
    {
        queueController.setGlobalBrowserProfiles(Set.of("Chrome_1024x768", "Chrome_1500x1000"));
        final List<BrowserGroupDto> groups = queueController.getGroupedBrowserProfiles(queueController.getGlobalBrowserProfiles());
        Assertions.assertNotNull(groups);

        BrowserGroupDto chromeGroup = null;
        for (final BrowserGroupDto g : groups)
        {
            if ("chrome".equals(g.key))
            {
                chromeGroup = g;
            }
        }
        Assertions.assertNotNull(chromeGroup);
        Assertions.assertEquals(2, chromeGroup.selectedCount);
    }

    @Test
    public void testSingleIconPerBrowserGroupInDashboard() throws IOException, InterruptedException
    {
        final com.sun.net.httpserver.HttpServer server = com.xceptance.neodymium.aura.NeodymiumAuraManager.startServer(0, false);
        final int port = server.getAddress().getPort();
        try
        {
            final java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            final java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://127.0.0.1:" + port + "/"))
                .GET()
                .build();
            final java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            Assertions.assertEquals(200, response.statusCode());
            final String body = response.body();

            // Verify Google Chrome group summary contains only ONE SVG icon, not 6
            final int chromeSummaryIdx = body.indexOf("Google Chrome");
            Assertions.assertTrue(chromeSummaryIdx > 0, "Google Chrome accordion summary must exist");

            final int summaryStartIdx = body.lastIndexOf("<summary", chromeSummaryIdx);
            final int summaryEndIdx = body.indexOf("</summary>", chromeSummaryIdx);
            final String summaryHtml = body.substring(summaryStartIdx, summaryEndIdx);

            final int svgCount = summaryHtml.split("<svg", -1).length - 1;
            Assertions.assertEquals(1, svgCount, "Google Chrome header must contain exactly 1 SVG icon");
        }
        finally
        {
            com.xceptance.neodymium.aura.NeodymiumAuraManager.stopServer(server);
        }
    }
}
