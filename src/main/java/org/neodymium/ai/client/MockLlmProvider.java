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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Mock implementation of {@link LlmProvider} used for hermetic, network-isolated unit testing.
 * Allows test fixtures to enqueue canned responses to be returned in sequence.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class MockLlmProvider implements LlmProvider
{
    /**
     * The thread-safe queue of canned responses to be consumed sequentially.
     */
    private final Queue<LlmResponse> responseQueue = new ConcurrentLinkedQueue<>();

    /**
     * The set of capabilities declared by this mock provider.
     */
    private final Set<LlmCapability> capabilities;

    /**
     * Constructs a MockLlmProvider with standard default capabilities
     * (TEXT_ONLY, VISION, EXECUTION).
     */
    public MockLlmProvider()
    {
        this.capabilities = Set.of(LlmCapability.TEXT_ONLY, LlmCapability.VISION, LlmCapability.EXECUTION);
    }

    /**
     * Constructs a MockLlmProvider with a custom set of capabilities.
     *
     * @param capabilities the specific capabilities this provider should support
     */
    public MockLlmProvider(final Set<LlmCapability> capabilities)
    {
        this.capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }

    /**
     * Enqueues a canned response to be returned by a subsequent chat call.
     *
     * @param response the canned response to queue
     */
    public void addResponse(final LlmResponse response)
    {
        this.responseQueue.add(response);
    }

    /**
     * Dequeues the next canned response.
     *
     * @param request the LLM request context (ignored in mock)
     * @return the enqueued response
     * @throws IOException if no enqueued responses remain in the queue
     */
    @Override
    public LlmResponse chat(final LlmRequest request) throws IOException
    {
        final LlmResponse next = this.responseQueue.poll();
        if (next == null)
        {
            throw new IOException("MockLlmProvider has no queued responses left.");
        }
        return next;
    }

    /**
     * Gets the capabilities declared by this provider.
     *
     * @return the set of supported LlmCapabilities
     */
    @Override
    public Set<LlmCapability> getCapabilities()
    {
        return this.capabilities;
    }
}
