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
package com.xceptance.neodymium.ai.core;

/**
 * Details of a context level escalation that occurred during execution.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class EscalationDetails
{
    private final ContextLevel fromLevel;
    private final ContextLevel toLevel;
    private final boolean llmRequested;
    private final String reason;

    public EscalationDetails(
            final ContextLevel fromLevel,
            final ContextLevel toLevel,
            final boolean llmRequested,
            final String reason)
    {
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
        this.llmRequested = llmRequested;
        this.reason = reason;
    }

    public final ContextLevel getFromLevel()
    {
        return this.fromLevel;
    }

    public final ContextLevel getToLevel()
    {
        return this.toLevel;
    }

    public final boolean isLlmRequested()
    {
        return this.llmRequested;
    }

    public final String getReason()
    {
        return this.reason;
    }
}
