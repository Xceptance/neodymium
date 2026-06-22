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
package com.xceptance.neodymium.ai.core;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xceptance.neodymium.ai.util.EmbeddedHtmlServer;

/**
 * A utility JUnit 5 test class to spin up the local Neodymium Aura AI test pages server
 * within the standard JUnit classpath and classloader container.
 * Keeps the server running indefinitely to allow manual interaction and visual inspection of all test pages.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class RunServerTest
{
    @Disabled
    @Test
    public void runServerIndefinitely() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer();
        server.start();

        System.out.println("========================================================================");
        System.out.println("  Neodymium Aura AI: Embedded HTML Server Running via JUnit!");
        System.out.println("========================================================================");
        System.out.println("  Access the test applications via the following URLs:");
        System.out.println();
        System.out.println("  [HTTP Contexts]");
        System.out.println("    - Starter Hub Portal: http://localhost:" + server.getPort() + "/");
        System.out.println("    - Shop Home:          http://localhost:" + server.getPort() + "/AuraGlanceTest/shop/index.html");
        System.out.println("    - Forms Demo:         http://localhost:" + server.getPort() + "/AuraGlanceTest/shop/forms.html");
        System.out.println("    - Dashboard:          http://localhost:" + server.getPort() + "/AuraGlanceTest/dashboard/index.html");
        System.out.println("    - Accessibility:      http://localhost:" + server.getPort() + "/AuraGlanceTest/a11y/index.html");
        System.out.println("    - React SPA:          http://localhost:" + server.getPort() + "/AuraGlanceTest/spa/index.html");
        System.out.println("    - VÉRLA Perfect Store: http://localhost:" + server.getPort() + "/verla-perfect/index.html");
        System.out.println("    - VÉRLA Normal Store:  http://localhost:" + server.getPort() + "/verla-normal/index.html");
        System.out.println("    - VÉRLA Bad Store:     http://localhost:" + server.getPort() + "/verla-bad/index.html");
        System.out.println();
        System.out.println("  [HTTPS Secure Contexts]");
        System.out.println("    - Starter Hub Portal: https://localhost:" + server.getHttpsPort() + "/");
        System.out.println("    - Shop Home:          https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/shop/index.html");
        System.out.println("    - Forms Demo:         https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/shop/forms.html");
        System.out.println("    - Dashboard:          https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/dashboard/index.html");
        System.out.println("    - Accessibility:      https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/a11y/index.html");
        System.out.println("    - React SPA:          https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/spa/index.html");
        System.out.println("    - VÉRLA Perfect Store: https://localhost:" + server.getHttpsPort() + "/verla-perfect/index.html");
        System.out.println("    - VÉRLA Normal Store:  https://localhost:" + server.getHttpsPort() + "/verla-normal/index.html");
        System.out.println("    - VÉRLA Bad Store:     https://localhost:" + server.getHttpsPort() + "/verla-bad/index.html");
        System.out.println();
        System.out.println("  NOTE: For HTTPS, you will get a self-signed certificate warning.");
        System.out.println("        You can safely bypass this or run with Chrome's '--ignore-certificate-errors' flag.");
        System.out.println("========================================================================");
        System.out.println("  This server will remain active until you stop/cancel this task.");
        System.out.println("========================================================================");

        // Keep the server running indefinitely within the test thread
        while (true)
        {
            Thread.sleep(10000);
        }
    }

    @Disabled
    @Test
    public void regenerateProductCatalogFile() throws Exception
    {
        final String json;
        try (final InputStream is = EmbeddedHtmlServer.class.getClassLoader().getResourceAsStream("ai-test-pages/verla-catalog.json"))
        {
            if (is == null)
            {
                throw new java.io.FileNotFoundException("verla-catalog.json not found in classpath resources");
            }
            json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        }

        final Gson gson = new Gson();
        final EmbeddedHtmlServer.CatalogConfig config = gson.fromJson(json, EmbeddedHtmlServer.CatalogConfig.class);
        final List<EmbeddedHtmlServer.Product> products = EmbeddedHtmlServer.generateCatalogProducts(config);

        final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        final String productsJson = prettyGson.toJson(products);

        final java.nio.file.Path targetPath = java.nio.file.Path.of(System.getProperty("user.dir"), "src/test/resources/ai-test-pages/verla-products.json");
        java.nio.file.Files.writeString(targetPath, productsJson, StandardCharsets.UTF_8);

        System.out.println("Successfully regenerated product catalog file at: " + targetPath.toAbsolutePath());
    }
}
