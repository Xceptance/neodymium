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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LlmRegistry}.
 * Validates provider registration, capability routing, default fallback provider,
 * and clear operations.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class LlmRegistryTest
{
    @Test
    public void testRegisterAndRetrieveByCapability()
    {
        final LlmRegistry registry = new LlmRegistry();
        final MockLlmProvider mockProvider = new MockLlmProvider(Set.of(LlmCapability.TEXT_ONLY, LlmCapability.VISION));

        registry.registerProvider(mockProvider);

        final LlmProvider textProvider = registry.getProvider(LlmCapability.TEXT_ONLY);
        final LlmProvider visionProvider = registry.getProvider(LlmCapability.VISION);

        assertNotNull(textProvider, "Provider for TEXT_ONLY capability should not be null.");
        assertNotNull(visionProvider, "Provider for VISION capability should not be null.");
        assertSame(mockProvider, textProvider, "Registered provider should match.");
        assertSame(mockProvider, visionProvider, "Registered provider should match.");
    }

    @Test
    public void testExplicitCapabilityRegistration()
    {
        final LlmRegistry registry = new LlmRegistry();
        final MockLlmProvider mockProvider = new MockLlmProvider();

        registry.registerProvider(LlmCapability.VERIFICATION, mockProvider);

        final LlmProvider verificationProvider = registry.getProvider(LlmCapability.VERIFICATION);
        assertNotNull(verificationProvider, "Provider registered for explicit capability should be returned.");
        assertSame(mockProvider, verificationProvider, "Explicitly registered provider should match.");
    }

    @Test
    public void testDefaultProviderFallback()
    {
        final LlmRegistry registry = new LlmRegistry();
        final MockLlmProvider defaultProvider = new MockLlmProvider();

        registry.setDefaultProvider(defaultProvider);

        assertSame(defaultProvider, registry.getDefaultProvider(), "Default provider should match.");
        final LlmProvider fallbackProvider = registry.getProvider(LlmCapability.PESAP);
        assertSame(defaultProvider, fallbackProvider, "Fallback provider should be used when specific capability is unmapped.");
    }

    @Test
    public void testNullProviderHandling()
    {
        final LlmRegistry registry = new LlmRegistry();

        registry.registerProvider(null, null);
        registry.registerProvider((LlmProvider) null);
        registry.setDefaultProvider(null);

        assertNull(registry.getDefaultProvider(), "Default provider should be null when unset.");
        assertThrows(IllegalStateException.class, () -> registry.getProvider(LlmCapability.TEXT_ONLY), "Unmapped capability without default should throw IllegalStateException.");
    }
}
