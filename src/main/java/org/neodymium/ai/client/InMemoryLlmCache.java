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
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe cache store mapping human-readable prompt instruction strings
 * to recorded {@link LlmResponse} instances during test execution.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class InMemoryLlmCache
{
    /**
     * Thread-safe in-memory map holding cached responses.
     */
    private static final Map<String, LlmResponse> CACHE = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private InMemoryLlmCache()
    {
    }

    /**
     * Retrieves a cached response for the given human-readable prompt key.
     *
     * @param key the readable prompt instruction string key
     * @return the cached LlmResponse instance, or null if not found
     */
    public static LlmResponse get(final String key)
    {
        return key == null ? null : CACHE.get(key);
    }

    /**
     * Stores an LLM response under the given human-readable prompt key.
     *
     * @param key the readable prompt instruction string key
     * @param response the LlmResponse instance to cache
     */
    public static void put(final String key, final LlmResponse response)
    {
        if (key != null && response != null)
        {
            CACHE.put(key, response);
        }
    }

    /**
     * Clears all entries from the in-memory cache.
     */
    public static void clear()
    {
        CACHE.clear();
    }
}
