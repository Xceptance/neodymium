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
 * A mock implementation of {@link LlmClient} designed for browserless, offline execution testing.
 * <p>
 * Instead of making live HTTP connections to external LLM providers (e.g. via LangChain4j),
 * this client pops pre-configured {@link AiMockResponse} behaviors from a thread-safe FIFO queue.
 * It simulates network latency delays, maps HTTP error codes (e.g. HTTP 429 / 503) into production exceptions,
 * and maintains mock token footprints for verification.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class MockLlmClient extends LlmClient
{
    /**
     * FIFO queue containing pre-configured LLM responses for the test run.
     */
    private final Queue<AiMockResponse> responseQueue = new ConcurrentLinkedQueue<>();
    
    /**
     * Internal mock token statistics tracking input, output, and cache token deltas.
     */
    private final AiStats mockStats = new AiStats();

    /**
     * Constructs a new MockLlmClient with no external credentials.
     */
    public MockLlmClient()
    {
        super();
    }

    /**
     * Enqueues a single mock response behavior at the end of the queue.
     *
     * @param response the mock response to add
     */
    public final void addResponse(final AiMockResponse response)
    {
        if (response != null)
        {
            this.responseQueue.add(response);
        }
    }

    /**
     * Enqueues a list of mock response behaviors in sequence.
     *
     * @param responses the list of mock responses to add
     */
    public final void addResponses(final List<AiMockResponse> responses)
    {
        if (responses != null)
        {
            this.responseQueue.addAll(responses);
        }
    }

    /**
     * Retrieves the mock stats instance associated with this client.
     *
     * @return the mock AI stats
     */
    @Override
    public final AiStats getAiStats()
    {
        return this.mockStats;
    }

    /**
     * Pops and processes the next configured mock response from the queue.
     *
     * @param callMode the active LLM execution mode (e.g. AGENT or PESAP)
     * @return the mock response text
     * @throws IllegalStateException    if the response queue is exhausted
     * @throws RuntimeException         if the response was configured to throw an exception
     * @throws LlmHttpException         if the response was configured to mimic an HTTP failure code
     */
    private String nextResponse(final LlmMode callMode)
    {
        if (callMode == LlmMode.PESAP)
        {
            final AiMockResponse peek = this.responseQueue.peek();
            if (peek != null && peek.getResponseText() != null && peek.getResponseText().contains("contextLevel"))
            {
                final AiMockResponse response = this.responseQueue.poll();
                final long in = response.getInputTokens() != null ? response.getInputTokens() : 0L;
                final long out = response.getOutputTokens() != null ? response.getOutputTokens() : 0L;
                final long cached = response.getCachedTokens() != null ? response.getCachedTokens() : 0L;
                this.mockStats.record(callMode, in, out, cached);
                return response.getResponseText();
            }

            this.mockStats.record(LlmMode.PESAP, 0L, 0L, 0L);
            return "{\"contextLevel\":\"AXTREE\",\"stepType\":\"INTERACTION\",\"expectedTargetTagName\":\"body\",\"pageNavigation\":false,\"requiresJavaMethods\":false,\"direction\":\"\"}";
        }

        final AiMockResponse response = this.responseQueue.poll();
        if (response == null)
        {
            throw new IllegalStateException("MockLlmClient response queue is exhausted.");
        }

        // Simulate network latency delay
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

        // Propagate custom exceptions if configured
        if (response.getException() != null)
        {
            if (response.getException() instanceof RuntimeException)
            {
                throw (RuntimeException) response.getException();
            }
            throw new RuntimeException(response.getException());
        }

        // Propagate HTTP failure codes if configured
        if (response.getHttpStatusCode() != null)
        {
            throw new LlmHttpException(response.getHttpStatusCode(), "Simulated HTTP error code: " + response.getHttpStatusCode());
        }

        // Record mock token footprint deltas
        final long in = response.getInputTokens() != null ? response.getInputTokens() : 0L;
        final long out = response.getOutputTokens() != null ? response.getOutputTokens() : 0L;
        final long cached = response.getCachedTokens() != null ? response.getCachedTokens() : 0L;
        this.mockStats.record(callMode, in, out, cached);

        return response.getResponseText();
    }

    /**
     * Standard chat completion endpoint. Bypasses live networks and returns the next queued response.
     *
     * @param systemPrompt the system instructions
     * @param userPrompt   the user instruction prompt
     * @return the mock response text
     */
    @Override
    public final String chat(final String systemPrompt, final String userPrompt)
    {
        return nextResponse(LlmMode.AGENT);
    }

    /**
     * Mode-specific chat completion endpoint. Bypasses live networks and returns the next queued response.
     *
     * @param callMode     the active LLM call mode
     * @param systemPrompt the system instructions
     * @param userPrompt   the user instruction prompt
     * @return the mock response text
     */
    @Override
    public final String chat(final LlmMode callMode, final String systemPrompt, final String userPrompt)
    {
        return nextResponse(callMode);
    }

    /**
     * Standard vision-based chat completion endpoint. Bypasses live networks and returns the next queued response.
     *
     * @param systemPrompt     the system instructions
     * @param userPrompt       the user instruction prompt
     * @param base64Screenshot the Base64 image payload
     * @return the mock response text
     */
    @Override
    public final String chatWithScreenshot(final String systemPrompt, final String userPrompt, final String base64Screenshot)
    {
        return nextResponse(LlmMode.AGENT);
    }

    /**
     * Mode-specific vision-based chat completion endpoint. Bypasses live networks and returns the next queued response.
     *
     * @param callMode         the active LLM call mode
     * @param systemPrompt     the system instructions
     * @param userPrompt       the user instruction prompt
     * @param base64Screenshot the Base64 image payload
     * @return the mock response text
     */
    @Override
    public final String chatWithScreenshot(final LlmMode callMode, final String systemPrompt, final String userPrompt, final String base64Screenshot)
    {
        return nextResponse(callMode);
    }

    /**
     * Convenience method configuring default system properties for browserless offline testing.
     * Satisfies the API key guard validation checks using "mock-offline-key".
     */
    public static void configureForOffline()
    {
        configureForOffline("mock-offline-key");
    }

    /**
     * Configures the system properties required for offline mock execution.
     *
     * @param apiKey       the mock API key to register
     */
    public static void configureForOffline(final String apiKey)
    {
        System.setProperty("neodymium.ai.apiKey", apiKey);
    }
}
