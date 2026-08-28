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

import java.lang.reflect.Constructor;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Factory class for instantiating {@link LlmProvider} instances based on configuration settings.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmProviderFactory
{
    private LlmProviderFactory()
    {
    }

    /**
     * Creates an {@link LlmProvider} instance for a specific role based on the configuration.
     *
     * @param role the execution role (e.g., "pesap", "execution", "vision", "audit")
     * @param config the configuration provider
     * @return the instantiated provider
     */
    public static LlmProvider createProvider(final String role, final AiConfiguration config)
    {
        final String providerType = config.getProvider(role);
        if ("mock".equalsIgnoreCase(providerType))
        {
            return new MockLlmProvider();
        }
        
        // Dynamically load class name if configured
        final String className = config.getProperty("neodymium.ai.provider." + providerType + ".class", null);
        if (className != null)
        {
            try
            {
                final Class<?> clazz = Class.forName(className);
                final Constructor<?> constructor = clazz.getDeclaredConstructor();
                return (LlmProvider) constructor.newInstance();
            }
            catch (final Exception e)
            {
                // Fall back to MockLlmProvider if class loading fails
                return new MockLlmProvider();
            }
        }
        
        // Default to a MockLlmProvider for testing purposes
        return new MockLlmProvider();
    }
}
