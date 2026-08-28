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

import java.lang.reflect.Method;
import org.neodymium.ai.junit.AiLlmCache;

/**
 * Utility helper that handles {@link AiLlmCache} detection, thread stack inspection,
 * and dynamic registry provider decoration.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmCacheHelper
{
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private LlmCacheHelper()
    {
    }

    /**
     * Checks if {@link AiLlmCache} is active on any test class or test method executing on the current thread stack.
     * Uses the active thread context classloader to resolve test classes dynamically.
     *
     * @return true if AiLlmCache annotation is present on the active test scope, false otherwise
     */
    public static boolean isCacheActiveOnCurrentThread()
    {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final ClassLoader targetCl = cl != null ? cl : LlmCacheHelper.class.getClassLoader();
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        for (final StackTraceElement elem : stack)
        {
            try
            {
                final Class<?> clazz = Class.forName(elem.getClassName(), false, targetCl);
                if (clazz.isAnnotationPresent(AiLlmCache.class))
                {
                    return true;
                }
                for (final Method m : clazz.getDeclaredMethods())
                {
                    if (m.getName().equals(elem.getMethodName()) && m.isAnnotationPresent(AiLlmCache.class))
                    {
                        return true;
                    }
                }
            }
            catch (final Throwable t)
            {
                // Skip unresolvable stack frame classes
            }
        }
        return false;
    }

    /**
     * Wraps all registered providers in the given registry with {@link CachingLlmProvider} if {@link AiLlmCache} is active.
     *
     * @param registry the LLM provider registry to decorate
     */
    public static void wrapRegistryIfActive(final LlmRegistry registry)
    {
        if (registry != null && isCacheActiveOnCurrentThread())
        {
            registry.wrapProviders(provider -> provider instanceof CachingLlmProvider ? provider : new CachingLlmProvider(provider));
        }
    }
}
