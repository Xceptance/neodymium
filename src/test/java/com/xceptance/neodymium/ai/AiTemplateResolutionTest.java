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
package com.xceptance.neodymium.ai;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.LookupDetails;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.common.testdata.TestData;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Test cases verifying template placeholder resolution and variable lookup logging.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class AiTemplateResolutionTest extends BaseAiOfflineTest
{
    /**
     * Verifies that the variable resolution system parses template placeholders
     * (e.g. {@code ${username}}) from the active TestData map, handles nested placeholders,
     * and compiles the precise details of every variable resolution (key, value, source,
     * localization status) into a lookup collector.
     */
    @Test
    public final void testTemplateResolutionAndLookups()
    {
        final TestData data = Neodymium.getData();
        data.put("username", "demoUser123");
        data.put("password", "demoPass456");
        data.put("nested", "${username}_secure");

        final List<LookupDetails> lookupsCollector = new ArrayList<>();
        final String resolved = AiBrowser.resolveTestDataToPrompt("Login using ${username} and ${nested}", lookupsCollector);

        Assertions.assertEquals("Login using demoUser123 and demoUser123_secure", resolved);
        Assertions.assertEquals(3, lookupsCollector.size());

        final LookupDetails userLookup = lookupsCollector.get(0);
        Assertions.assertEquals("username", userLookup.getKey());
        Assertions.assertEquals("demoUser123", userLookup.getResolvedValue());
        Assertions.assertEquals("TestData Map", userLookup.getSource());
        Assertions.assertFalse(userLookup.isLocalized());

        final LookupDetails nestedLookup = lookupsCollector.get(1);
        Assertions.assertEquals("username", nestedLookup.getKey());
        Assertions.assertEquals("demoUser123", nestedLookup.getResolvedValue());
        Assertions.assertEquals("TestData Map", nestedLookup.getSource());

        final LookupDetails fullNestedLookup = lookupsCollector.get(2);
        Assertions.assertEquals("nested", fullNestedLookup.getKey());
        Assertions.assertEquals("demoUser123_secure", fullNestedLookup.getResolvedValue());
        Assertions.assertEquals("TestData Map", fullNestedLookup.getSource());
    }

    /**
     * Assert that instructions containing dynamic placeholders resolve variables from the correct,
     * authorized scope sources.
     */
    @Test
    public final void testTestDataVariableResolutionScope()
    {
        // Set up test data variables in Neodymium
        Neodymium.getData().put("accountEmail", "user@neodymium.com");

        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [],
                      "d": true
                    }
                    """)
                .build());

        // Execute instruction containing placeholder
        final AiExecutionResult result = this.mockBrowser.execute("Type '${accountEmail}' into email input");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(1, result.getLookups().size());

        final LookupDetails lookup = result.getLookups().get(0);
        Assertions.assertEquals("accountEmail", lookup.getKey());
        Assertions.assertEquals("user@neodymium.com", lookup.getResolvedValue());
        Assertions.assertEquals("TestData Map", lookup.getSource()); // Asserts it resolved from standard test data scope
    }

    /**
     * Verifies that placeholder keys are resolved case-insensitively.
     */
    @Test
    public final void testCaseInsensitiveResolution()
    {
        final TestData data = Neodymium.getData();
        data.put("username", "demoUser123");

        final List<LookupDetails> lookupsCollector = new ArrayList<>();
        final String resolved = AiBrowser.resolveTestDataToPrompt("Login using ${USERname}", lookupsCollector);

        Assertions.assertEquals("Login using demoUser123", resolved);
        Assertions.assertEquals(1, lookupsCollector.size());
        Assertions.assertEquals("USERname", lookupsCollector.get(0).getKey());
    }

    /**
     * Verifies that if both exact match and case-insensitive match are in the map,
     * the exact case-sensitive match is preferred.
     */
    @Test
    public final void testExactMatchPreferredOverCaseInsensitive()
    {
        final TestData data = Neodymium.getData();
        data.put("username", "exactCaseMatched");
        data.put("userName", "wrongCaseMatched");

        final List<LookupDetails> lookupsCollector = new ArrayList<>();
        
        // Query exact match for username
        final String resolvedExact = AiBrowser.resolveTestDataToPrompt("Login using ${username}", lookupsCollector);
        Assertions.assertEquals("Login using exactCaseMatched", resolvedExact);
        
        lookupsCollector.clear();
        
        // Query exact match for userName
        final String resolvedWrong = AiBrowser.resolveTestDataToPrompt("Login using ${userName}", lookupsCollector);
        Assertions.assertEquals("Login using wrongCaseMatched", resolvedWrong);
    }

    /**
     * Verifies that nested JSONPath structures are resolved successfully.
     */
    @Test
    public final void testJsonPathResolution()
    {
        final TestData data = Neodymium.getData();
        
        // Put nested structure in TestData as a JSON String so it parses to a JsonObject
        data.put("user", "{\"profile\": {\"address\": {\"zip\": \"12345\"}}}");

        final List<LookupDetails> lookupsCollector = new ArrayList<>();
        final String resolved = AiBrowser.resolveTestDataToPrompt("Zip code: ${user.profile.address.zip}", lookupsCollector);

        Assertions.assertEquals("Zip code: 12345", resolved);
        Assertions.assertEquals(1, lookupsCollector.size());
        Assertions.assertEquals("JSONPath Query", lookupsCollector.get(0).getSource());
    }

    /**
     * Verifies that when a key is missing from TestData, it falls back to Neodymium.configuration().
     */
    @Test
    public final void testNeodymiumConfigurationFallback()
    {
        Neodymium.configuration().setProperty("neodymium.ai.testFallbackProp", "fallback-configured-value");
        try
        {
            final List<LookupDetails> lookupsCollector = new ArrayList<>();
            final String resolved = AiBrowser.resolveTestDataToPrompt("Config value: ${neodymium.ai.testFallbackProp}", lookupsCollector);

            Assertions.assertEquals("Config value: fallback-configured-value", resolved);
            Assertions.assertEquals(1, lookupsCollector.size());
            Assertions.assertEquals("Neodymium Configuration", lookupsCollector.get(0).getSource());
        }
        finally
        {
            Neodymium.configuration().setProperty("neodymium.ai.testFallbackProp", null);
        }
    }

    /**
     * Verifies that self-referential placeholders do not cause a StackOverflowError and are halted by the depth-10 guard.
     */
    @Test
    public final void testInfiniteRecursionPlaceholderGuard()
    {
        final TestData data = Neodymium.getData();
        data.put("a", "${b}");
        data.put("b", "${a}");

        // This should not cause stack overflow, it should safely return the placeholder itself or null fallback
        final String resolved = AiBrowser.resolveTestDataToPrompt("Recurse: ${a}");
        Assertions.assertTrue(resolved.contains("${a}") || resolved.contains("${b}"));
    }

    /**
     * Verifies that variables are localized via Neodymium.tryLocalizedText and log localized = true.
     */
    @Test
    public final void testLocalizationLookup() throws Exception
    {
        final String originalFile = System.getProperty("neodymium.localization.file");
        final File tempFile = File.createTempFile("localization-test-", ".yaml", new File("config"));
        
        try
        {
            // Write temporary localization properties
            try (final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8")))
            {
                bw.write("default:");
                bw.newLine();
                bw.write("  login.btn: Inbound login");
                bw.newLine();
            }

            System.setProperty("neodymium.localization.file", tempFile.getPath());
            Neodymium.clearThreadContext(); // re-initialize localization engine

            final TestData data = Neodymium.getData();
            data.put("username", "login.btn");

            final List<LookupDetails> lookupsCollector = new ArrayList<>();
            final String resolved = AiBrowser.resolveTestDataToPrompt("Click ${username}", lookupsCollector);

            Assertions.assertEquals("Click Inbound login", resolved);
            Assertions.assertEquals(1, lookupsCollector.size());
            
            final LookupDetails lookup = lookupsCollector.get(0);
            Assertions.assertEquals("username", lookup.getKey());
            Assertions.assertEquals("Inbound login", lookup.getResolvedValue());
            Assertions.assertTrue(lookup.isLocalized());
            Assertions.assertEquals("Localization File", lookup.getSource());
        }
        finally
        {
            if (tempFile.exists())
            {
                tempFile.delete();
            }
            if (originalFile != null)
            {
                System.setProperty("neodymium.localization.file", originalFile);
            }
            else
            {
                System.clearProperty("neodymium.localization.file");
            }
            Neodymium.clearThreadContext();
        }
    }
}
