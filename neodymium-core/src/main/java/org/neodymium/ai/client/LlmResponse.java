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
package org.neodymium.ai.client;

import java.util.Collections;
import java.util.List;
import org.neodymium.ai.tool.ToolCall;

/**
 * Immutable record representing the response data returned by an LLM provider.
 * Supports both raw text output and native deserialized tool calls.
 *
 * @param content the raw text or JSON string content returned by the model
 * @param tokenUsage the token metrics for the executed request
 * @param modelName the identifier of the specific model that processed the request
 * @param toolCalls native deserialized tool calls returned by the model, if any
 * @param thinking internal reasoning or thinking process produced by the model, if any
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public record LlmResponse(
    String content,
    TokenUsage tokenUsage,
    String modelName,
    List<ToolCall> toolCalls,
    String thinking
)
{
    /**
     * Canonical constructor performing defensive copying of toolCalls.
     */
    public LlmResponse(
        final String content,
        final TokenUsage tokenUsage,
        final String modelName,
        final List<ToolCall> toolCalls,
        final String thinking
    )
    {
        this.content = content;
        this.tokenUsage = tokenUsage;
        this.modelName = modelName;
        this.toolCalls = toolCalls == null ? Collections.emptyList() : List.copyOf(toolCalls);
        this.thinking = thinking;
    }

    /**
     * Convenience constructor without thinking process.
     */
    public LlmResponse(
        final String content,
        final TokenUsage tokenUsage,
        final String modelName,
        final List<ToolCall> toolCalls
    )
    {
        this(content, tokenUsage, modelName, toolCalls, null);
    }

    /**
     * Convenience constructor without native tool calls or thinking.
     */
    public LlmResponse(
        final String content,
        final TokenUsage tokenUsage,
        final String modelName
    )
    {
        this(content, tokenUsage, modelName, Collections.emptyList(), null);
    }

    /**
     * Whether this response contains one or more native tool calls.
     *
     * @return true if tool calls are present
     */
    public boolean hasToolCalls()
    {
        return this.toolCalls != null && !this.toolCalls.isEmpty();
    }

    /**
     * Whether this response contains thinking or reasoning text.
     *
     * @return true if thinking is present
     */
    public boolean hasThinking()
    {
        return this.thinking != null && !this.thinking.isBlank();
    }
}
