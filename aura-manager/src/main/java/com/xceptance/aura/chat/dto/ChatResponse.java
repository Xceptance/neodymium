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

import java.util.List;

/**
 * Data transfer object representing the AI chat response and any UI-refresh actions triggered by tools.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
public final class ChatResponse
{
    /**
     * Final synthesized assistant response message.
     */
    public String message;

    /**
     * Trace of tool invocations and results for the collapsible Thinking Process block.
     */
    public String thinking;

    /**
     * Primary UI action identifier for HTMX {@code HX-Trigger} (e.g., "refresh_files", "refresh_runs").
     */
    public String action;

    /**
     * List of all UI actions triggered during a multi-tool turn.
     */
    public List<String> actions;

    /**
     * Pending destructive confirmations issued during this turn (never includes secret tokens).
     */
    public List<PendingConfirmationDto> pendingConfirmations;

    /**
     * Default constructor for serialization.
     */
    public ChatResponse()
    {
    }

    /**
     * Creates a chat response without UI trigger metadata.
     *
     * @param message final assistant message
     * @param thinking tool execution trace
     */
    public ChatResponse(final String message, final String thinking)
    {
        this(message, thinking, null, null, null);
    }

    /**
     * Creates a chat response with UI trigger metadata.
     *
     * @param message final assistant message
     * @param thinking tool execution trace
     * @param action primary UI action
     * @param actions all UI actions triggered in this turn
     */
    public ChatResponse(final String message, final String thinking, final String action, final List<String> actions)
    {
        this(message, thinking, action, actions, null);
    }

    /**
     * Creates a full chat response with UI trigger and pending confirmation metadata.
     *
     * @param message final assistant message
     * @param thinking tool execution trace
     * @param action primary UI action
     * @param actions all UI actions triggered in this turn
     * @param pendingConfirmations optional list of pending destructive action confirmations
     */
    public ChatResponse(final String message, final String thinking, final String action, final List<String> actions,
                        final List<PendingConfirmationDto> pendingConfirmations)
    {
        this.message = message;
        this.thinking = thinking;
        this.action = action;
        this.actions = actions;
        this.pendingConfirmations = pendingConfirmations;
    }
}
