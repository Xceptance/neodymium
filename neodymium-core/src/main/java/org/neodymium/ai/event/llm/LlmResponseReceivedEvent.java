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
package org.neodymium.ai.event.llm;

import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.event.EventCategory;
import org.neodymium.ai.event.ExecutionEvent;

/**
 * Event dispatched upon receiving a completion response payload back from an LLM provider.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmResponseReceivedEvent extends ExecutionEvent
{
    /**
     * The initial outgoing request.
     */
    private final LlmRequest request;

    /**
     * The response returned by the LLM provider.
     */
    private final LlmResponse response;

    /**
     * The API roundtrip latency duration in milliseconds.
     */
    private final long durationMs;

    /**
     * The capability or role associated with this LLM call.
     */
    private final String capability;

    /**
     * Constructs an LlmResponseReceivedEvent.
     *
     * @param request the initial outgoing request
     * @param response the response returned by the provider
     * @param durationMs roundtrip latency duration in ms
     * @param capability capability name
     */
    public LlmResponseReceivedEvent(
        final LlmRequest request,
        final LlmResponse response,
        final long durationMs,
        final String capability
    )
    {
        super();
        this.request = request;
        this.response = response;
        this.durationMs = durationMs;
        this.capability = capability;
    }

    /**
     * Gets the initial outgoing LLM request.
     *
     * @return the LlmRequest
     */
    public LlmRequest getRequest()
    {
        return this.request;
    }

    /**
     * Gets the LLM response payload.
     *
     * @return the LlmResponse
     */
    public LlmResponse getResponse()
    {
        return this.response;
    }

    /**
     * Gets the request duration in milliseconds.
     *
     * @return latency duration in ms
     */
    public long getDurationMs()
    {
        return this.durationMs;
    }

    /**
     * Gets the capability name associated with this LLM call.
     *
     * @return the capability string
     */
    public String getCapability()
    {
        return this.capability;
    }

    @Override
    public String getEventType()
    {
        return "llm.response_received";
    }

    @Override
    public EventCategory getCategory()
    {
        return EventCategory.LLM;
    }
}
