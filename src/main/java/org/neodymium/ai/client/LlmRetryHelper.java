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
import org.neodymium.ai.config.AiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility helper executing LLM chat calls with configurable retries and exponential backoff
 * on transient HTTP errors (e.g., 429 rate limits or 5xx server errors).
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmRetryHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LlmRetryHelper.class);

    /**
     * Functional interface representing an LLM chat operation.
     */
    @FunctionalInterface
    public interface LlmCallable
    {
        LlmResponse call() throws IOException;
    }

    private LlmRetryHelper()
    {
        // Prevent instantiation
    }

    /**
     * Executes the given LLM callable with retries for transient errors.
     *
     * @param callable the LLM invocation callable
     * @return the LlmResponse on success
     * @throws IOException if all retries are exhausted or a non-transient error occurs
     */
    public static LlmResponse executeWithRetry(final LlmCallable callable) throws IOException
    {
        final AiConfiguration config = AiConfiguration.getInstance();
        final int maxRetries = Math.max(1, config.getInt("neodymium.ai.llm.maxRetries", 3));
        final long initialDelayMs = Math.max(10L, config.getLong("neodymium.ai.llm.initialRetryDelayMs", 100L));

        int attempt = 0;
        long currentDelayMs = initialDelayMs;

        while (true)
        {
            attempt++;
            try
            {
                return callable.call();
            }
            catch (final Exception e)
            {
                if (attempt >= maxRetries || !isTransientError(e))
                {
                    if (e instanceof IOException)
                    {
                        throw (IOException) e;
                    }
                    throw new IOException("LLM call failed after " + attempt + " attempts: " + e.getMessage(), e);
                }

                LOGGER.warn("LLM request attempt {} failed with transient error ({}). Retrying in {} ms...",
                    attempt, e.getMessage(), currentDelayMs);

                try
                {
                    Thread.sleep(currentDelayMs);
                }
                catch (final InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during LLM retry backoff", ie);
                }

                currentDelayMs *= 2;
            }
        }
    }

    /**
     * Checks if the exception represents a transient failure eligible for retry.
     *
     * @param t the throwable to evaluate
     * @return {@code true} if the exception is a transient error, {@code false} otherwise
     */
    public static boolean isTransientError(final Throwable t)
    {
        if (t == null)
        {
            return false;
        }

        final String msg = t.getMessage();
        if (msg != null)
        {
            final String lower = msg.toLowerCase();
            if (lower.contains("429") || lower.contains("too many requests") ||
                lower.contains("500") || lower.contains("502") ||
                lower.contains("503") || lower.contains("504") ||
                lower.contains("rate limit") || lower.contains("server error") ||
                lower.contains("timeout") || lower.contains("temporarily unavailable"))
            {
                return true;
            }
        }

        return t.getCause() != null && isTransientError(t.getCause());
    }
}
