/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.aura.chat.dto;

/**
 * UI-safe summary of a pending destructive tool confirmation held server-side for a chat session.
 * Never exposes the underlying single-use confirmation token to templates or client scripts.
 *
 * @author AI-generated: Claude Opus 4.6
 * @author Xceptance GmbH 2026
 */
public final class PendingConfirmationDto
{
    /**
     * Opaque identifier referencing the server-held confirmation entry within a session.
     */
    public String actionId;

    /**
     * Name of the tool awaiting confirmation (e.g., {@code delete_test} or {@code cancel_run}).
     */
    public String toolName;

    /**
     * Target test path or run identifier affected by the action.
     */
    public String target;

    /**
     * Expiration timestamp in epoch milliseconds.
     */
    public long expiresAtEpochMs;

    /**
     * Default constructor for serialization.
     */
    public PendingConfirmationDto()
    {
    }

    /**
     * Creates a UI-safe pending confirmation summary.
     *
     * @param actionId opaque action identifier
     * @param toolName destructive tool name
     * @param target target test path or run ID
     * @param expiresAtEpochMs expiration epoch timestamp in milliseconds
     */
    public PendingConfirmationDto(final String actionId, final String toolName, final String target,
                                  final long expiresAtEpochMs)
    {
        this.actionId = actionId;
        this.toolName = toolName;
        this.target = target;
        this.expiresAtEpochMs = expiresAtEpochMs;
    }
}
