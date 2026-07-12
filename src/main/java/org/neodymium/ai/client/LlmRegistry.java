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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Thread-safe registry that maps LLM capabilities to their corresponding
 * provider instances, supporting dynamic capability routing and fallback resolution.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmRegistry
{
    /**
     * Map storing registered providers indexed by capability.
     */
    private final Map<LlmCapability, LlmProvider> providers = new ConcurrentHashMap<>();

    /**
     * The designated default provider to fall back on.
     */
    private volatile LlmProvider defaultProvider;

    /**
     * Constructs a default LlmRegistry.
     */
    public LlmRegistry()
    {
    }

    /**
     * Registers a provider instance for a specific capability.
     *
     * @param capability the capability target
     * @param provider the provider to associate with the capability
     */
    public void registerProvider(final LlmCapability capability, final LlmProvider provider)
    {
        if (capability != null && provider != null)
        {
            this.providers.put(capability, provider);
        }
    }

    /**
     * Registers all capabilities supported by a provider.
     *
     * @param provider the provider to register
     */
    public void registerProvider(final LlmProvider provider)
    {
        if (provider != null)
        {
            for (final LlmCapability capability : provider.getCapabilities())
            {
                this.providers.put(capability, provider);
            }
        }
    }

    /**
     * Sets the default provider to fall back to when no specialized provider is registered.
     *
     * @param provider the fallback provider instance
     */
    public void setDefaultProvider(final LlmProvider provider)
    {
        this.defaultProvider = provider;
    }

    /**
     * Retrieves the default fallback provider.
     *
     * @return the default provider instance, or null if none is configured
     */
    public LlmProvider getDefaultProvider()
    {
        return this.defaultProvider;
    }

    /**
     * Resolves the active provider dynamically for a required capability.
     * Falls back to the default provider if no specialized matching provider is found.
     *
     * @param requiredCapability the capability required by the caller
     * @return the resolved LlmProvider instance
     * @throws IllegalStateException if no provider matches the capability and no default is configured
     */
    public LlmProvider getProvider(final LlmCapability requiredCapability)
    {
        final LlmProvider provider = this.providers.get(requiredCapability);
        if (provider != null)
        {
            return provider;
        }

        final LlmProvider fallback = this.defaultProvider;
        if (fallback != null)
        {
            return fallback;
        }

        throw new IllegalStateException("No provider registered for capability " 
            + requiredCapability + " and no default provider set.");
    }

    /**
     * Gets all currently registered capabilities.
     *
     * @return the set of registered LlmCapabilities
     */
    public Set<LlmCapability> getRegisteredCapabilities()
    {
        return this.providers.keySet();
    }

    /**
     * Bootstraps the registry by instantiating role-specific providers based on configuration overrides.
     *
     * @param registry the registry instance to populate
     * @param config the active configuration loaded from properties files
     */
    public static void bootstrap(final LlmRegistry registry, final AiConfiguration config)
    {
        if (registry != null && config != null)
        {
            // 1. Resolve and register the global fallback default provider
            final LlmProvider defaultProvider = LlmProviderFactory.createProvider("global", config);
            registry.setDefaultProvider(defaultProvider);

            // 2. Resolve and register role-specific providers: "pesap", "execution", "vision", "audit"
            final String[] roles = {"pesap", "execution", "vision", "audit"};
            for (final String role : roles)
            {
                final LlmProvider roleProvider = LlmProviderFactory.createProvider(role, config);
                for (final LlmCapability capability : roleProvider.getCapabilities())
                {
                    registry.registerProvider(capability, roleProvider);
                }
            }
        }
    }
}
