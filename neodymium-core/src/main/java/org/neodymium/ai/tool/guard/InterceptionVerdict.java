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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolResult;

/**
 * Result of intercepting a proposed tool call by a guard or quality judge.
 *
 * @param decision outcome decision (ALLOW, REJECT, DELIBERATED, or RETRY_WITH_FEEDBACK)
 * @param reason explanation or rationale for the verdict
 * @param adjustedCall optional adjusted tool call if deliberation modified targets or parameters
 * @param rejectionResult immediate error result to return if rejected or feedback requested
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public record InterceptionVerdict(
        Decision decision,
        String reason,
        ToolCall adjustedCall,
        ToolResult rejectionResult)
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Categorical decision type for tool interception.
     */
    public enum Decision
    {
        ALLOW,
        REJECT,
        DELIBERATED,
        RETRY_WITH_FEEDBACK
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
     * Creates a RETRY_WITH_FEEDBACK verdict advising the agent of a fragile/poor locator and providing recovery guidance.
     *
     * @param callId tool call id
     * @param reason explanation of locator quality issue
     * @param recommendation actionable recovery tip (e.g. recommend mark_elements)
     * @return retry with feedback verdict
     */
    public static InterceptionVerdict retryWithFeedback(final String callId, final String reason, final String recommendation)
    {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("status", "ERROR");
        node.put("error", reason);
        if (recommendation != null && !recommendation.isBlank())
        {
            node.put("recommendation", recommendation);
        }
        final ToolResult result = ToolResult.error(callId, node.toString());
        return new InterceptionVerdict(Decision.RETRY_WITH_FEEDBACK, reason, null, result);
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
