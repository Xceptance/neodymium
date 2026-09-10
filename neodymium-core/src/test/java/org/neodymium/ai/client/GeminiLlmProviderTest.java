/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.AiConfiguration;


/**
 * Unit tests for {@link GeminiLlmProvider}.
 * Validates constructor validation, system property resolution, and capability set declarations.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class GeminiLlmProviderTest
{
    private String previousKey;

    @BeforeEach
    public void setUp()
    {
        this.previousKey = System.getProperty("neodymium.ai.gemini.apiKey");
    }

    @AfterEach
    public void tearDown()
    {
        if (this.previousKey != null)
        {
            System.setProperty("neodymium.ai.gemini.apiKey", this.previousKey);
        }
        else
        {
            System.clearProperty("neodymium.ai.gemini.apiKey");
        }
    }

    @Test
    public void testCapabilitiesDeclaration()
    {
        System.setProperty("neodymium.ai.gemini.apiKey", "mock-gemini-key");
        final GeminiLlmProvider provider = new GeminiLlmProvider();
        final Set<LlmCapability> capabilities = provider.getCapabilities();

        assertNotNull(capabilities, "Capabilities should not be null.");
        assertTrue(capabilities.contains(LlmCapability.TEXT_ONLY), "Should support TEXT_ONLY.");
        assertTrue(capabilities.contains(LlmCapability.VISION), "Should support VISION.");
        assertTrue(capabilities.contains(LlmCapability.EXECUTION), "Should support EXECUTION.");
        assertTrue(capabilities.contains(LlmCapability.PESAP), "Should support PESAP.");
        assertTrue(capabilities.contains(LlmCapability.VERIFICATION), "Should support VERIFICATION.");
    }

    @Test
    public void testMissingApiKeyThrowsException()
    {
        System.setProperty("neodymium.ai.gemini.apiKey", "");
        System.setProperty("neodymium.ai.apiKey", "");
        AiConfiguration.resetInstance();
        final String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey == null || envKey.isBlank())
        {
            assertThrows(IllegalArgumentException.class, () -> {
                new GeminiLlmProvider();
            }, "Missing API key should throw IllegalArgumentException.");
        }
    }

    @Test
    public void testIncludeThoughtsConfiguration()
    {
        System.setProperty("neodymium.ai.gemini.apiKey", "mock-gemini-key");
        try
        {
            System.setProperty("neodymium.ai.gemini.includeThoughts", "true");
            AiConfiguration.resetInstance();
            assertTrue(AiConfiguration.getInstance().isIncludeThoughts(), "isIncludeThoughts should be true when configured.");
            final GeminiLlmProvider providerWithThoughts = new GeminiLlmProvider();
            assertNotNull(providerWithThoughts, "Provider should initialize with includeThoughts=true.");
        }
        finally
        {
            System.clearProperty("neodymium.ai.gemini.includeThoughts");
            AiConfiguration.resetInstance();
        }
    }

    @Test
    public void testRepairThoughtSignaturesPropagatesTurnSignatureToParallelCalls() throws Exception
    {
        final String requestJson = """
            {
              "contents": [
                {
                  "role": "model",
                  "parts": [
                    {
                      "functionCall": {
                        "name": "browser_query_dom",
                        "args": {"selector": "#test"}
                      },
                      "thoughtSignature": "valid_signature_123"
                    },
                    {
                      "functionCall": {
                        "name": "browser_scroll",
                        "args": {"direction": "down"}
                      }
                    },
                    {
                      "functionCall": {
                        "name": "browser_click",
                        "args": {"target": "btn"}
                      }
                    }
                  ]
                }
              ]
            }
            """;

        final HttpRequest request = HttpRequest.builder()
            .method(HttpMethod.POST)
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5:generateContent")
            .body(requestJson)
            .build();

        final HttpRequest repaired = GeminiLlmProvider.repairThoughtSignatures(request);
        assertNotNull(repaired);
        assertNotNull(repaired.body());

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode root = mapper.readTree(repaired.body());
        final JsonNode parts = root.get("contents").get(0).get("parts");

        assertEquals("valid_signature_123", parts.get(0).get("thoughtSignature").asText());
        assertEquals("valid_signature_123", parts.get(1).get("thoughtSignature").asText(),
            "Sibling tool call at index 1 must inherit the turn's thoughtSignature.");
        assertEquals("valid_signature_123", parts.get(2).get("thoughtSignature").asText(),
            "Sibling tool call at index 2 (parallel call) must inherit the turn's thoughtSignature.");
    }

    @Test
    public void testRepairThoughtSignaturesInjectsSentinelWhenNoSignaturePresent() throws Exception
    {
        final String requestJson = """
            {
              "contents": [
                {
                  "role": "model",
                  "parts": [
                    {
                      "functionCall": {
                        "name": "browser_click",
                        "args": {"target": "btn"}
                      }
                    }
                  ]
                }
              ]
            }
            """;

        final HttpRequest request = HttpRequest.builder()
            .method(HttpMethod.POST)
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5:generateContent")
            .body(requestJson)
            .build();

        final HttpRequest repaired = GeminiLlmProvider.repairThoughtSignatures(request);
        assertNotNull(repaired);

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode root = mapper.readTree(repaired.body());
        final JsonNode parts = root.get("contents").get(0).get("parts");

        assertEquals("skip_thought_signature_validator", parts.get(0).get("thoughtSignature").asText(),
            "Missing signature must fallback to Google's skip_thought_signature_validator sentinel.");
    }

    @Test
    public void testRepairThoughtSignaturesIgnoresNonFunctionCallRequests()
    {
        final String requestJson = """
            {
              "contents": [
                {
                  "role": "user",
                  "parts": [
                    {"text": "Hello world"}
                  ]
                }
              ]
            }
            """;

        final HttpRequest request = HttpRequest.builder()
            .method(HttpMethod.POST)
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5:generateContent")
            .body(requestJson)
            .build();

        final HttpRequest repaired = GeminiLlmProvider.repairThoughtSignatures(request);
        assertSame(request, repaired, "Non-functionCall requests should be returned unmodified without overhead.");
    }
}
