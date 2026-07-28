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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.AiConfiguration;

/**
 * TDD test suite verifying LLM client models, registry capability-routing,
 * configuration hierarchy, and provider bootstrapping.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmClientAndRegistryTest
{
    /**
     * Constructs a default test instance.
     */
    public LlmClientAndRegistryTest()
    {
    }

    /**
     * Verifies that the LlmRequest performs defensive copies of attachment lists.
     */
    @Test
    public void testLlmRequestImmutability()
    {
        final List<SutAttachment> mutableList = new ArrayList<>();
        mutableList.add(new SutAttachment("image/png", "path/1.png", null));

        final LlmRequest request = new LlmRequest(
            "system-prompt",
            "user-prompt",
            mutableList,
            ResponseSchema.TEXT,
            0.0,
            60
        );

        // Modifying the original list must not affect the request's internal list
        mutableList.add(new SutAttachment("image/png", "path/2.png", null));

        assertEquals(1, request.attachments().size());
        assertEquals("path/1.png", request.attachments().get(0).filePath());
    }

    /**
     * Verifies that LlmRegistry correctly resolves capability providers or falls back to defaults.
     */
    @Test
    public void testLlmRegistryResolution()
    {
        final LlmRegistry registry = new LlmRegistry();

        final LlmProvider textProvider = new MockLlmProvider(Set.of(LlmCapability.TEXT_ONLY));
        final LlmProvider visionProvider = new MockLlmProvider(Set.of(LlmCapability.VISION));
        final LlmProvider fallbackProvider = new MockLlmProvider(Set.of(LlmCapability.TEXT_ONLY));

        registry.registerProvider(LlmCapability.TEXT_ONLY, textProvider);
        registry.registerProvider(LlmCapability.VISION, visionProvider);
        registry.setDefaultProvider(fallbackProvider);

        // 1. Specialized lookup
        assertEquals(textProvider, registry.getProvider(LlmCapability.TEXT_ONLY));
        assertEquals(visionProvider, registry.getProvider(LlmCapability.VISION));

        // 2. Fallback lookup (routing PESAP to default fallback provider)
        assertEquals(fallbackProvider, registry.getProvider(LlmCapability.PESAP));

        // 3. Exception if fallback is removed and no provider matches
        registry.setDefaultProvider(null);
        assertThrows(IllegalStateException.class, () -> {
            registry.getProvider(LlmCapability.PESAP);
        });
    }

    /**
     * Verifies that MockLlmProvider enqueues and dequeues canned responses in FIFO order.
     *
     * @throws IOException if chat execution fails
     */
    @Test
    public void testMockLlmProviderQueue() throws IOException
    {
        final MockLlmProvider mock = new MockLlmProvider();
        final TokenUsage usage = new TokenUsage(10, 20, 30);
        
        final LlmResponse response1 = new LlmResponse("Response One", usage, "mock-model");
        final LlmResponse response2 = new LlmResponse("Response Two", usage, "mock-model");

        mock.addResponse(response1);
        mock.addResponse(response2);

        final LlmRequest request = new LlmRequest("sys", "user", List.of(), ResponseSchema.TEXT, 0.0, 30);

        assertEquals("Response One", mock.chat(request).content());
        assertEquals("Response Two", mock.chat(request).content());
        
        // Assert that calling chat when queue is empty throws IOException
        assertThrows(IOException.class, () -> {
            mock.chat(request);
        });
    }

    /**
     * Verifies that AiConfiguration correctly loads defaults and overlays role-specific overrides.
     */
    @Test
    public void testAiConfigurationHierarchy()
    {
        // Inject system properties for override testing
        System.setProperty("neodymium.ai.model", "global-model");
        System.setProperty("neodymium.ai.vision.model", "vision-model");
        System.setProperty("neodymium.ai.vision.temperature", "0.7");
        System.setProperty("neodymium.ai.pesap.provider", "mock");

        final AiConfiguration config = new AiConfiguration();

        // 1. Check global default
        assertEquals("global-model", config.getProperty("neodymium.ai.model", "fallback"));
        assertEquals(180, config.getTimeoutSeconds("execution"));

        // 2. Check role specific model overrides
        assertEquals("vision-model", config.getModel("vision"));
        assertEquals("global-model", config.getModel("execution")); // falls back to global

        // 3. Check role specific double overrides
        assertEquals(0.7, config.getTemperature("vision"));
        assertEquals(0.0, config.getTemperature("execution")); // falls back to default 0.0

        // 4. Check role specific provider overrides
        assertEquals("mock", config.getProvider("pesap"));
        assertEquals("gemini", config.getProvider("vision")); // falls back to global default

        // Clean up system properties
        System.clearProperty("neodymium.ai.model");
        System.clearProperty("neodymium.ai.vision.model");
        System.clearProperty("neodymium.ai.vision.temperature");
        System.clearProperty("neodymium.ai.pesap.provider");
    }

    /**
     * Verifies that registry bootstrapping instantiates role providers correctly.
     */
    @Test
    public void testLlmRegistryBootstrapping()
    {
        System.setProperty("neodymium.ai.provider", "mock");
        System.setProperty("neodymium.ai.vision.provider", "mock");

        final AiConfiguration config = new AiConfiguration();
        final LlmRegistry registry = new LlmRegistry();

        LlmRegistry.bootstrap(registry, config);

        assertNotNull(registry.getDefaultProvider());
        assertTrue(registry.getDefaultProvider() instanceof MockLlmProvider);
        assertNotNull(registry.getProvider(LlmCapability.VISION));

        System.clearProperty("neodymium.ai.provider");
        System.clearProperty("neodymium.ai.vision.provider");
    }

    /**
     * Verifies that bootstrapping correctly instantiates capability-specific overrides.
     */
    @Test
    public void testCapabilitySpecificBootstrapOverrides()
    {
        System.setProperty("neodymium.ai.provider", "mock");
        System.setProperty("neodymium.ai.verification.provider", "mock");
        System.setProperty("neodymium.ai.verification.model", "gemini-2.5-pro");

        final AiConfiguration config = new AiConfiguration();
        final LlmRegistry registry = new LlmRegistry();

        LlmRegistry.bootstrap(registry, config);

        assertNotNull(registry.getDefaultProvider());
        assertNotNull(registry.getProvider(LlmCapability.VERIFICATION));
        assertTrue(registry.getRegisteredCapabilities().contains(LlmCapability.VERIFICATION));

        System.clearProperty("neodymium.ai.provider");
        System.clearProperty("neodymium.ai.verification.provider");
        System.clearProperty("neodymium.ai.verification.model");
    }

    /**
     * Verifies that visual alias correctly resolves and registers overrides for the VISION capability.
     */
    @Test
    public void testVisualAliasBootstrapOverrides()
    {
        System.setProperty("neodymium.ai.provider", "mock");
        System.setProperty("neodymium.ai.visual.provider", "mock");

        final AiConfiguration config = new AiConfiguration();
        final LlmRegistry registry = new LlmRegistry();

        LlmRegistry.bootstrap(registry, config);

        assertNotNull(registry.getProvider(LlmCapability.VISION));
        assertTrue(registry.getRegisteredCapabilities().contains(LlmCapability.VISION));

        System.clearProperty("neodymium.ai.provider");
        System.clearProperty("neodymium.ai.visual.provider");
    }
}
