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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying the in-memory LLM response caching behavior of {@link CachingLlmProvider}
 * and {@link InMemoryLlmCache}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmCacheTest
{
    @BeforeEach
    public void setup()
    {
        InMemoryLlmCache.clear();
    }

    @Test
    public void testCachingLlmProviderCachesIdenticalPrompts() throws IOException
    {
        final AtomicInteger callCounter = new AtomicInteger(0);

        final LlmProvider mockDelegate = new LlmProvider()
        {
            @Override
            public LlmResponse chat(final LlmRequest request)
            {
                callCounter.incrementAndGet();
                return new LlmResponse("Recorded Action JSON", new TokenUsage(100, 50, 150), "mock-provider");
            }

            @Override
            public Set<LlmCapability> getCapabilities()
            {
                return Set.of(LlmCapability.TEXT_ONLY);
            }
        };

        final CachingLlmProvider cachingProvider = new CachingLlmProvider(mockDelegate);
        final LlmRequest request = new LlmRequest("System instruction", "Open homepage", Collections.emptyList(), null, 0.0, 30);

        // First call: Cache miss -> delegates to live LLM
        final LlmResponse response1 = cachingProvider.chat(request);
        assertEquals(1, callCounter.get());
        assertEquals("Recorded Action JSON", response1.content());

        // Second call: Cache hit -> returns cached response instantly
        final LlmResponse response2 = cachingProvider.chat(request);
        assertEquals(1, callCounter.get(), "Delegate should not be called again on cache hit");
        assertEquals("Recorded Action JSON", response2.content());
        assertTrue(response2.modelName().contains("(Cached)"));
    }

    @Test
    public void testInMemoryLlmCacheClear()
    {
        InMemoryLlmCache.put("prompt-key-1", new LlmResponse("data", new TokenUsage(1, 1, 2), "model"));
        assertEquals("data", InMemoryLlmCache.get("prompt-key-1").content());

        InMemoryLlmCache.clear();
        assertEquals(null, InMemoryLlmCache.get("prompt-key-1"));
    }
}
