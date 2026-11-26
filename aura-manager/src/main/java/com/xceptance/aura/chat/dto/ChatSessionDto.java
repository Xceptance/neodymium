/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.aura.chat.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object representing an AI chat session and its persisted message history.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
public final class ChatSessionDto
{
    /**
     * Unique session identifier.
     */
    public String id;

    /**
     * Human-readable session name.
     */
    public String name;

    /**
     * Ordered list of messages in this session.
     */
    public List<ChatMessageDto> messages;

    /**
     * Epoch timestamp in milliseconds of the most recent update (creation, rename, or message append).
     */
    public long updatedAt;

    /**
     * Default constructor initializing an empty message list.
     */
    public ChatSessionDto()
    {
        this.messages = new ArrayList<>();
    }

    /**
     * Creates a chat session with the given metadata and messages.
     *
     * @param id unique session identifier
     * @param name display name
     * @param messages initial message history
     */
    public ChatSessionDto(final String id, final String name, final List<ChatMessageDto> messages)
    {
        this(id, name, messages, 0L);
    }

    /**
     * Creates a chat session with explicit last-updated timestamp metadata.
     *
     * @param id unique session identifier
     * @param name display name
     * @param messages initial message history
     * @param updatedAt last update timestamp in epoch milliseconds
     */
    public ChatSessionDto(final String id, final String name, final List<ChatMessageDto> messages, final long updatedAt)
    {
        this.id = id;
        this.name = name;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.updatedAt = updatedAt;
    }
}
