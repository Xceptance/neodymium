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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.exception.RetriableException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LlmRetryHelper} verifying bounded retries on transient errors.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
final class LlmRetryHelperTest
{
    @Test
    void testExecuteWithRetry_successfulFirstAttempt() throws IOException
    {
        final LlmResponse expected = new LlmResponse("success response", null, "test-model");
        final LlmResponse response = LlmRetryHelper.executeWithRetry(() -> expected);

        assertNotNull(response);
        assertEquals("success response", response.content());
    }

    @Test
    void testExecuteWithRetry_succeedsAfterTransientErrors() throws IOException
    {
        final AtomicInteger attempts = new AtomicInteger(0);
        final LlmResponse response = LlmRetryHelper.executeWithRetry(() -> {
            final int count = attempts.incrementAndGet();
            if (count < 3)
            {
                throw new IOException("HTTP 429 Too Many Requests");
            }
            return new LlmResponse("healed response", null, "test-model");
        });

        assertEquals(3, attempts.get());
        assertEquals("healed response", response.content());
    }

    @Test
    void testExecuteWithRetry_failsOnNonTransientError()
    {
        final AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(IOException.class, () -> {
            LlmRetryHelper.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw new IllegalArgumentException("Invalid API key parameter");
            });
        });

        assertEquals(1, attempts.get(), "Non-transient error should fail on first attempt.");
    }

    @Test
    void testExecuteWithRetry_succeedsAfterTimeout() throws IOException
    {
        final AtomicInteger attempts = new AtomicInteger(0);
        final LlmResponse response = LlmRetryHelper.executeWithRetry(() -> {
            final int count = attempts.incrementAndGet();
            if (count == 1)
            {
                throw new IOException("Gemini HTTP call timed out after 185 seconds (socket watchdog triggered)",
                    new TimeoutException("Socket read timed out"));
            }
            return new LlmResponse("recovered response", null, "test-model");
        });

        assertEquals(2, attempts.get());
        assertEquals("recovered response", response.content());
    }

    @Test
    void testIsTransientError()
    {
        assertTrue(LlmRetryHelper.isTransientError(new IOException("HTTP 429 Too Many Requests")));
        assertTrue(LlmRetryHelper.isTransientError(new RuntimeException("503 Service Unavailable")));
        assertTrue(LlmRetryHelper.isTransientError(new Exception("Server Error 500")));
        assertTrue(LlmRetryHelper.isTransientError(new TimeoutException("Operation timed out")));
        assertTrue(LlmRetryHelper.isTransientError(new HttpTimeoutException("request timed out")));
        assertTrue(LlmRetryHelper.isTransientError(new SocketTimeoutException("Read timed out")));
        assertTrue(LlmRetryHelper.isTransientError(new RetriableException("retriable exception")));
        assertTrue(LlmRetryHelper.isTransientError(new ConnectException("Connection refused")));
        assertTrue(LlmRetryHelper.isTransientError(new SocketException("Broken pipe")));
        assertTrue(LlmRetryHelper.isTransientError(new IOException("Connection reset by peer")));
    }
}
