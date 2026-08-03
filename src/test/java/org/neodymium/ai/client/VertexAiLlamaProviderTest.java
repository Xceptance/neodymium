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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link VertexAiLlamaProvider}.
 * Validates initialization properties and capabilities declarations.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class VertexAiLlamaProviderTest
{
    private String previousKey;

    @BeforeEach
    public void setUp()
    {
        this.previousKey = System.getProperty("neodymium.ai.vertex.apiKey");
    }

    @AfterEach
    public void tearDown()
    {
        if (this.previousKey != null)
        {
            System.setProperty("neodymium.ai.vertex.apiKey", this.previousKey);
        }
        else
        {
            System.clearProperty("neodymium.ai.vertex.apiKey");
        }
    }

    @Test
    public void testCapabilitiesDeclaration()
    {
        System.setProperty("neodymium.ai.vertex.apiKey", "mock-vertex-key");
        final VertexAiLlamaProvider provider = new VertexAiLlamaProvider();
        final Set<LlmCapability> capabilities = provider.getCapabilities();

        assertNotNull(capabilities, "Capabilities should not be null.");
        assertTrue(capabilities.contains(LlmCapability.TEXT_ONLY), "Should support TEXT_ONLY.");
        assertTrue(capabilities.contains(LlmCapability.VISION), "Should support VISION.");
        assertTrue(capabilities.contains(LlmCapability.EXECUTION), "Should support EXECUTION.");
    }

    @Test
    public void testMissingApiKeyThrowsException()
    {
        System.clearProperty("neodymium.ai.vertex.apiKey");
        final String envKey = System.getenv("VERTEX_API_KEY");
        if (envKey == null || envKey.isBlank())
        {
            assertThrows(IllegalArgumentException.class, () -> {
                new VertexAiLlamaProvider();
            }, "Missing API key should throw IllegalArgumentException.");
        }
    }
}
