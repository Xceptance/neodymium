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
package com.xceptance.neodymium.ai.testing;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.xceptance.neodymium.ai.core.AiStats;
import com.xceptance.neodymium.ai.core.LlmClient;
import com.xceptance.neodymium.ai.core.LlmHttpException;
import com.xceptance.neodymium.ai.core.LlmMode;

/**
 * A mock LLM client running completely offline by popping configured sequential mock responses.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class MockLlmClient extends LlmClient
{
    private final Queue<AiMockResponse> responseQueue = new ConcurrentLinkedQueue<>();
    private final AiStats mockStats = new AiStats();

    public MockLlmClient()
    {
        super();
    }

    public final void addResponse(final AiMockResponse response)
    {
        if (response != null)
        {
            this.responseQueue.add(response);
        }
    }

    public final void addResponses(final List<AiMockResponse> responses)
    {
        if (responses != null)
        {
            this.responseQueue.addAll(responses);
        }
    }

    @Override
    public final AiStats getAiStats()
    {
        return this.mockStats;
    }

    private String nextResponse(final LlmMode callMode)
    {
        final AiMockResponse response = this.responseQueue.poll();
        if (response == null)
        {
            throw new IllegalStateException("MockLlmClient response queue is exhausted.");
        }

        // Simulate latency
        if (response.getDelayMs() > 0)
        {
            try
            {
                Thread.sleep(response.getDelayMs());
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        // Handle exceptions
        if (response.getException() != null)
        {
            if (response.getException() instanceof RuntimeException)
            {
                throw (RuntimeException) response.getException();
            }
            throw new RuntimeException(response.getException());
        }

        // Handle HTTP failures
        if (response.getHttpStatusCode() != null)
        {
            throw new LlmHttpException(response.getHttpStatusCode(), "Simulated HTTP error code: " + response.getHttpStatusCode());
        }

        // Record simulated tokens
        final long in = response.getInputTokens() != null ? response.getInputTokens() : 0L;
        final long out = response.getOutputTokens() != null ? response.getOutputTokens() : 0L;
        final long cached = response.getCachedTokens() != null ? response.getCachedTokens() : 0L;
        this.mockStats.record(callMode, in, out, cached);

        return response.getResponseText();
    }

    @Override
    public final String chat(final String systemPrompt, final String userPrompt)
    {
        return nextResponse(LlmMode.AGENT);
    }

    @Override
    public final String chat(final LlmMode callMode, final String systemPrompt, final String userPrompt)
    {
        return nextResponse(callMode);
    }

    @Override
    public final String chatWithScreenshot(final String systemPrompt, final String userPrompt, final String base64Screenshot)
    {
        return nextResponse(LlmMode.AGENT);
    }

    @Override
    public final String chatWithScreenshot(final LlmMode callMode, final String systemPrompt, final String userPrompt, final String base64Screenshot)
    {
        return nextResponse(callMode);
    }

    public static void configureForOffline()
    {
        configureForOffline("mock-offline-key", false);
    }

    public static void configureForOffline(final String apiKey, final boolean pesapEnabled)
    {
        System.setProperty("neodymium.ai.apiKey", apiKey);
        System.setProperty("neodymium.ai.pesap.enabled", String.valueOf(pesapEnabled));
    }
}
