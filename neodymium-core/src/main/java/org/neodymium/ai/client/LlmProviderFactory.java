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
import java.lang.reflect.InvocationTargetException;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Factory class for instantiating {@link LlmProvider} instances based on configuration settings.
 *
 * @author AI-generated: Gemini 3.8 Flash
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
     * @throws IllegalStateException if provider is not configured, unknown, or cannot be instantiated
     */
    public static LlmProvider createProvider(final String role, final AiConfiguration config)
    {
        final String providerType = config.getProvider(role);
        if (providerType == null || providerType.isBlank())
        {
            throw new IllegalStateException(String.format(
                "LLM provider is not configured for role '%s'. "
                + "Please set 'neodymium.ai.%s.provider' or 'neodymium.ai.provider' "
                + "(e.g. 'gemini', 'mistral', 'vertexaillama', 'mock').",
                role, role
            ));
        }

        final String defaultClass = switch (providerType.trim().toLowerCase())
        {
            case "gemini" -> "org.neodymium.ai.client.GeminiLlmProvider";
            case "mistral" -> "org.neodymium.ai.client.MistralLlmProvider";
            case "vertexaillama", "vertexai" -> "org.neodymium.ai.client.VertexAiLlamaProvider";
            case "mock" -> "org.neodymium.ai.client.MockLlmProvider";
            default -> null;
        };

        final String className = config.getProperty("neodymium.ai.provider." + providerType.trim() + ".class", defaultClass);
        if (className == null || className.isBlank())
        {
            throw new IllegalStateException(String.format(
                "Unknown LLM provider '%s' configured for role '%s'. "
                + "No provider class is configured for 'neodymium.ai.provider.%s.class'. "
                + "Please configure 'neodymium.ai.provider.%s.class' with a fully qualified class name implementing %s, "
                + "or use one of the standard providers ('gemini', 'mistral', 'vertexaillama', 'mock').",
                providerType, role, providerType, providerType, LlmProvider.class.getName()
            ));
        }

        try
        {
            final Class<?> clazz = Class.forName(className.trim());
            if (!LlmProvider.class.isAssignableFrom(clazz))
            {
                throw new IllegalStateException(String.format(
                    "Configured provider class '%s' for provider '%s' (role '%s') does not implement %s.",
                    className, providerType, role, LlmProvider.class.getName()
                ));
            }
            final Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (LlmProvider) constructor.newInstance();
        }
        catch (final ClassNotFoundException e)
        {
            throw new IllegalStateException(String.format(
                "LLM provider class '%s' for provider '%s' (role '%s') was not found on the classpath. "
                + "Please verify that the class name is correct and the dependency is included in your project.",
                className, providerType, role
            ), e);
        }
        catch (final NoSuchMethodException e)
        {
            throw new IllegalStateException(String.format(
                "LLM provider class '%s' for provider '%s' (role '%s') does not provide a public no-arguments constructor.",
                className, providerType, role
            ), e);
        }
        catch (final Exception e)
        {
            final Throwable cause = (e instanceof InvocationTargetException && e.getCause() != null)
                ? e.getCause()
                : e;
            throw new IllegalStateException(String.format(
                "Failed to instantiate LLM provider class '%s' for provider '%s' (role '%s'): %s",
                className, providerType, role, cause.getMessage()
            ), cause);
        }
    }
}
