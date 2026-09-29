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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolDefinition;

/**
 * Unit tests for multi-turn messages and tool integration in {@link LlmRequest} and {@link LlmResponse}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmRequestResponseTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testLegacyRequestSynthesizesMessages()
    {
        final LlmRequest request = new LlmRequest("System prompt", "User query", Collections.emptyList(), ResponseSchema.TEXT, 0.0, 30);
        assertEquals("System prompt", request.systemMessage());
        assertEquals("User query", request.userMessage());
        assertEquals(2, request.messages().size());
        assertEquals(ChatMessage.Role.SYSTEM, request.messages().get(0).role());
        assertEquals("System prompt", request.messages().get(0).content());
        assertEquals(ChatMessage.Role.USER, request.messages().get(1).role());
        assertEquals("User query", request.messages().get(1).content());
        assertFalse(request.hasTools());
        assertFalse(request.isMultiTurn());
    }

    @Test
    public void testMultiTurnRequestWithTools()
    {
        final ToolDefinition toolDef = new ToolDefinition("browser_click", "Clicks element", MAPPER.createObjectNode());
        final List<ChatMessage> messages = List.of(
            ChatMessage.system("Act as tester"),
            ChatMessage.user("Click login"),
            ChatMessage.assistant(null, List.of(new ToolCall("c1", "browser_click", MAPPER.createObjectNode()))),
            ChatMessage.tool("c1", "{\"status\":\"SUCCESS\"}")
        );

        final LlmRequest request = new LlmRequest(messages, List.of(toolDef), 0.0, 30, ReasoningEffort.LOW);
        assertTrue(request.hasTools());
        assertTrue(request.isMultiTurn());
        assertEquals("Act as tester", request.systemMessage());
        assertEquals("Click login", request.userMessage());
        assertEquals(4, request.messages().size());
    }

    @Test
    public void testLlmResponseWithNativeToolCalls()
    {
        final ToolCall toolCall = new ToolCall("call_99", "browser_navigate", MAPPER.createObjectNode());
        final LlmResponse response = new LlmResponse("{}", new TokenUsage(10, 20, 0), "gemini-2.5-flash", List.of(toolCall));

        assertTrue(response.hasToolCalls());
        assertEquals(1, response.toolCalls().size());
        assertEquals("browser_navigate", response.toolCalls().get(0).toolName());
    }
}
