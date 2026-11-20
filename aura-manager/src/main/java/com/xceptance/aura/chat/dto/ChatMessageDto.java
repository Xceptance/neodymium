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

/**
 * Data transfer object representing a single chat message with role, content, and optional thinking trace.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
public final class ChatMessageDto
{
    /**
     * Sender role (e.g., "user" or "ai").
     */
    public String role;

    /**
     * Message body text (may contain markdown/HTML).
     */
    public String content;

    /**
     * Optional tool execution / reasoning trace displayed in the collapsible Thinking Process block.
     */
    public String thinking;

    /**
     * Indicates whether this message represents a transient error excluded from LLM replay history.
     */
    public boolean error;

    /**
     * Default constructor for JSON deserialization.
     */
    public ChatMessageDto()
    {
    }

    /**
     * Creates a chat message without a thinking trace.
     *
     * @param role the sender role
     * @param content the message content
     */
    public ChatMessageDto(final String role, final String content)
    {
        this.role = role;
        this.content = content;
    }

    /**
     * Creates a chat message with a thinking trace.
     *
     * @param role the sender role
     * @param content the message content
     * @param thinking the tool execution or reasoning trace
     */
    public ChatMessageDto(final String role, final String content, final String thinking)
    {
        this(role, content, thinking, false);
    }

    /**
     * Creates a chat message with a thinking trace and explicit error flag.
     *
     * @param role the sender role
     * @param content the message content
     * @param thinking the tool execution or reasoning trace
     * @param error {@code true} if the message reports a failed turn and should be skipped in LLM history
     */
    public ChatMessageDto(final String role, final String content, final String thinking, final boolean error)
    {
        this.role = role;
        this.content = content;
        this.thinking = thinking;
        this.error = error;
    }
}
