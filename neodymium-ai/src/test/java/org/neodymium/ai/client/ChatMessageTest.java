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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.tool.ToolCall;

/**
 * Tests for the {@link ChatMessage} record and factory methods.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class ChatMessageTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testSystemMessage()
    {
        final ChatMessage msg = ChatMessage.system("System instructions");
        assertEquals(ChatMessage.Role.SYSTEM, msg.role());
        assertEquals("System instructions", msg.content());
        assertTrue(msg.toolCalls().isEmpty());
    }

    @Test
    public void testUserMessage()
    {
        final ChatMessage msg = ChatMessage.user("Hello world");
        assertEquals(ChatMessage.Role.USER, msg.role());
        assertEquals("Hello world", msg.content());
    }

    @Test
    public void testAssistantMessageWithTools()
    {
        final ToolCall call = new ToolCall("call_1", "browser_click", MAPPER.createObjectNode());
        final ChatMessage msg = ChatMessage.assistant("Clicking button", List.of(call));
        assertEquals(ChatMessage.Role.ASSISTANT, msg.role());
        assertEquals("Clicking button", msg.content());
        assertEquals(1, msg.toolCalls().size());
        assertEquals("browser_click", msg.toolCalls().get(0).toolName());
    }

    @Test
    public void testToolResultMessage()
    {
        final ChatMessage msg = ChatMessage.tool("call_1", "{\"status\":\"SUCCESS\"}");
        assertEquals(ChatMessage.Role.TOOL, msg.role());
        assertEquals("call_1", msg.toolCallId());
        assertEquals("{\"status\":\"SUCCESS\"}", msg.content());
        assertNotNull(msg.attachments());
    }

    @Test
    public void testToStringFormatting()
    {
        final ChatMessage sysMsg = ChatMessage.system("System prompt instructions");
        assertEquals("ChatMessage[SYSTEM]:\nSystem prompt instructions", sysMsg.toString());

        final ChatMessage userMsg = ChatMessage.user("Test instruction step");
        assertEquals("ChatMessage[USER]:\nTest instruction step", userMsg.toString());

        final ToolCall call = new ToolCall("call_123", "browser_click", MAPPER.createObjectNode().put("target", "#btn"));
        final ChatMessage assistantMsg = ChatMessage.assistant("Clicking button", List.of(call));
        assertTrue(assistantMsg.toString().contains("ChatMessage[ASSISTANT]:"));
        assertTrue(assistantMsg.toString().contains("Clicking button"));
        assertTrue(assistantMsg.toString().contains("Tool Calls: [browser_click({\"target\":\"#btn\"})]"));

        final ChatMessage toolMsg = ChatMessage.tool("call_123", "browser_click", "{\"status\":\"SUCCESS\"}");
        assertTrue(toolMsg.toString().contains("ChatMessage[TOOL (tool=browser_click, id=call_123)]:"));
        assertTrue(toolMsg.toString().contains("{\"status\":\"SUCCESS\"}"));
    }

    @Test
    public void testFormatConversation()
    {
        assertEquals("[]", ChatMessage.formatConversation(null));
        assertEquals("[]", ChatMessage.formatConversation(List.of()));

        final ChatMessage sysMsg = ChatMessage.system("Instructions");
        final ChatMessage userMsg = ChatMessage.user("Do step");
        final String formatted = ChatMessage.formatConversation(List.of(sysMsg, userMsg));

        assertTrue(formatted.startsWith("ChatMessage[SYSTEM]:\nInstructions"));
        assertTrue(formatted.contains("\n\nChatMessage[USER]:\nDo step"));
    }
}
