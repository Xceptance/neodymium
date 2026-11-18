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
import org.neodymium.ai.event.EventCategory;
import org.neodymium.ai.event.ExecutionEvent;

/**
 * Event dispatched immediately before sending an API request payload to an LLM provider.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmRequestSentEvent extends ExecutionEvent
{
    /**
     * The outgoing LLM request.
     */
    private final LlmRequest request;

    /**
     * The capability or role associated with this LLM call.
     */
    private final String capability;

    /**
     * Constructs an LlmRequestSentEvent.
     *
     * @param request the outgoing LLM request
     * @param capability the capability name
     */
    public LlmRequestSentEvent(final LlmRequest request, final String capability)
    {
        super();
        this.request = request;
        this.capability = capability;
    }

    /**
     * Gets the outgoing LLM request payload.
     *
     * @return the LlmRequest
     */
    public LlmRequest getRequest()
    {
        return this.request;
    }

    /**
     * Gets the capability name associated with the LLM call.
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
        return "llm.request_sent";
    }

    @Override
    public EventCategory getCategory()
    {
        return EventCategory.LLM;
    }
}
