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
package org.neodymium.ai.pipeline;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Exception thrown when the agent tool execution loop detects thrashing or stagnation
 * (e.g. repeated identical tool calls without page state progress).
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class AgentThrashingException extends PipelineException
{
    private static final long serialVersionUID = 1L;

    private final String toolName;
    private final JsonNode arguments;
    private final int consecutiveCalls;

    /**
     * Constructs an AgentThrashingException.
     *
     * @param toolName name of the repeated tool
     * @param arguments arguments of the repeated invocation
     * @param consecutiveCalls count of identical consecutive calls executed
     */
    public AgentThrashingException(
        final String toolName,
        final JsonNode arguments,
        final int consecutiveCalls
    )
    {
        super(String.format("Thrashing breaker triggered: %d consecutive identical tool calls to '%s' with arguments %s",
            consecutiveCalls, toolName, arguments != null ? arguments.toString() : "{}"));
        this.toolName = toolName != null ? toolName : "";
        this.arguments = arguments;
        this.consecutiveCalls = consecutiveCalls;
    }

    /**
     * Gets the tool name that triggered the thrashing breaker.
     *
     * @return tool name
     */
    public String getToolName()
    {
        return this.toolName;
    }

    /**
     * Gets the arguments repeated during thrashing.
     *
     * @return JSON arguments
     */
    public JsonNode getArguments()
    {
        return this.arguments;
    }

    /**
     * Gets the count of identical calls before triggering.
     *
     * @return consecutive call count
     */
    public int getConsecutiveCalls()
    {
        return this.consecutiveCalls;
    }
}
