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
package org.neodymium.ai.tool.guard;

import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolResult;

/**
 * Result of intercepting a proposed tool call by a guard or quality judge.
 *
 * @param decision outcome decision (ALLOW, REJECT, or DELIBERATED)
 * @param reason explanation or rationale for the verdict
 * @param adjustedCall optional adjusted tool call if deliberation modified targets or parameters
 * @param rejectionResult immediate error result to return if rejected
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public record InterceptionVerdict(
        Decision decision,
        String reason,
        ToolCall adjustedCall,
        ToolResult rejectionResult)
{
    /**
     * Categorical decision type for tool interception.
     */
    public enum Decision
    {
        ALLOW,
        REJECT,
        DELIBERATED
    }

    /**
     * Creates an ALLOW verdict with the specified rationale.
     *
     * @param reason rationale for allowing execution
     * @return allowed verdict
     */
    public static InterceptionVerdict allow(final String reason)
    {
        return new InterceptionVerdict(Decision.ALLOW, reason, null, null);
    }

    /**
     * Creates a REJECT verdict returning an immediate error result.
     *
     * @param callId tool call id
     * @param reason policy or validation violation reason
     * @return rejection verdict
     */
    public static InterceptionVerdict reject(final String callId, final String reason)
    {
        return new InterceptionVerdict(Decision.REJECT, reason, null, ToolResult.policyViolation(callId, reason));
    }

    /**
     * Creates a DELIBERATED verdict replacing or adjusting the proposed call.
     *
     * @param adjustedCall revised tool call selected by deliberation
     * @param reason explanation of deliberation choice
     * @return deliberated verdict
     */
    public static InterceptionVerdict deliberated(final ToolCall adjustedCall, final String reason)
    {
        return new InterceptionVerdict(Decision.DELIBERATED, reason, adjustedCall, null);
    }

    /**
     * Returns whether the tool call is permitted to execute (either unchanged or deliberated).
     *
     * @return true if allowed or deliberated
     */
    public boolean isAllowed()
    {
        return this.decision == Decision.ALLOW || this.decision == Decision.DELIBERATED;
    }

    /**
     * Returns the effective tool call to execute, which is either the adjusted call if deliberated
     * or the original proposed call.
     *
     * @param originalCall the original proposed tool call
     * @return effective tool call
     */
    public ToolCall getEffectiveCall(final ToolCall originalCall)
    {
        return this.adjustedCall != null ? this.adjustedCall : originalCall;
    }
}
