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

import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator around an {@link LlmProvider} that caches prompt execution responses in {@link InMemoryLlmCache}
 * using human-readable instruction prompt string keys.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class CachingLlmProvider implements LlmProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CachingLlmProvider.class);

    /**
     * The delegate LLM provider instance to execute live requests on cache misses.
     */
    private final LlmProvider delegate;

    /**
     * Constructs a CachingLlmProvider decorating the given delegate provider.
     *
     * @param delegate the delegate LlmProvider instance
     */
    public CachingLlmProvider(final LlmProvider delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public LlmResponse chat(final LlmRequest request) throws IOException
    {
        if (request == null)
        {
            return this.delegate.chat(request);
        }

        final String cacheKey = buildCacheKey(request);
        final LlmResponse cached = InMemoryLlmCache.get(cacheKey);

        if (cached != null)
        {
            LOGGER.info("[LLM Cache HIT] Prompt: \"{}\" -> Returning cached response ({})",
                request.userMessage(), cached.modelName());
            return new LlmResponse(
                cached.content(),
                new TokenUsage(0, 0, cached.tokenUsage().totalTokenCount()),
                cached.modelName() + " (Cached)"
            );
        }

        LOGGER.info("[LLM Cache MISS] Prompt: \"{}\" -> Executing live LLM provider", request.userMessage());
        final LlmResponse liveResponse = this.delegate.chat(request);

        if (liveResponse != null)
        {
            InMemoryLlmCache.put(cacheKey, liveResponse);
        }

        return liveResponse;
    }

    @Override
    public Set<LlmCapability> getCapabilities()
    {
        return this.delegate.getCapabilities();
    }

    /**
     * Builds a human-readable prompt string cache key from the LLM request.
     *
     * @param request the LLM request
     * @return the human-readable string cache key
     */
    private static String buildCacheKey(final LlmRequest request)
    {
        final String sys = request.systemMessage() == null ? "" : request.systemMessage().trim();
        final String user = request.userMessage() == null ? "" : request.userMessage().trim();
        return sys.isEmpty() ? user : sys + "\n---\n" + user;
    }
}
