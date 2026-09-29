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

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LlmRequest}, verifying multi-turn detection and
 * dialogue conversation formatting that separates system prompt instructions from user dialogue turns.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class LlmRequestTest
{
    @Test
    public void testDialogueConversationFormattedExcludesSystemPrompt()
    {
        final ChatMessage sysMsg = ChatMessage.system("System prompt instructions for testing");
        final ChatMessage userMsg = ChatMessage.user("Please click the button");
        final ChatMessage asstMsg = ChatMessage.assistant("Clicking button now");
        final ChatMessage toolMsg = ChatMessage.tool("call_1", "browser_click", "{\"status\":\"SUCCESS\"}");

        final LlmRequest request = new LlmRequest(
            List.of(sysMsg, userMsg, asstMsg, toolMsg),
            Collections.emptyList(),
            0.0,
            30,
            ReasoningEffort.LOW
        );

        assertTrue(request.isMultiTurn(), "Should be identified as multi-turn conversation.");

        final String fullFormatted = request.fullConversationFormatted();
        assertTrue(fullFormatted.contains("ChatMessage[SYSTEM]:\nSystem prompt instructions for testing"),
            "fullConversationFormatted should include the system prompt.");

        final String dialogueFormatted = request.dialogueConversationFormatted();
        assertFalse(dialogueFormatted.contains("ChatMessage[SYSTEM]"),
            "dialogueConversationFormatted must NOT include ChatMessage[SYSTEM].");
        assertFalse(dialogueFormatted.contains("System prompt instructions for testing"),
            "dialogueConversationFormatted must NOT contain the system prompt text.");

        assertTrue(dialogueFormatted.contains("ChatMessage[USER]:\nPlease click the button"),
            "dialogueConversationFormatted must contain the user turn.");
        assertTrue(dialogueFormatted.contains("ChatMessage[ASSISTANT]:\nClicking button now"),
            "dialogueConversationFormatted must contain the assistant turn.");
        assertTrue(dialogueFormatted.contains("ChatMessage[TOOL (tool=browser_click, id=call_1)]:\n{\"status\":\"SUCCESS\"}"),
            "dialogueConversationFormatted must contain the tool turn.");
    }

    @Test
    public void testDialogueConversationFormattedSingleTurnFallback()
    {
        final LlmRequest request = new LlmRequest(
            "System instructions",
            "Perform single action",
            Collections.emptyList(),
            ResponseSchema.TEXT,
            0.0,
            30
        );

        assertFalse(request.isMultiTurn(), "Standard prompt should not be multi-turn.");
        final String dialogueFormatted = request.dialogueConversationFormatted();
        assertFalse(dialogueFormatted.contains("ChatMessage[SYSTEM]"),
            "dialogueConversationFormatted must not include system message.");
        assertTrue(dialogueFormatted.contains("Perform single action"),
            "dialogueConversationFormatted should retain user message.");
    }

    @Test
    public void testDialogueConversationFormattedWithOnlySystemPrompt()
    {
        final ChatMessage sysMsg = ChatMessage.system("Only system instructions");
        final LlmRequest request = new LlmRequest(
            List.of(sysMsg),
            Collections.emptyList(),
            0.0,
            30,
            ReasoningEffort.LOW
        );

        assertFalse(request.isMultiTurn(), "Single message is not multi-turn.");
        final String dialogueFormatted = request.dialogueConversationFormatted();
        assertEquals("", dialogueFormatted,
            "dialogueConversationFormatted should return empty string when no dialogue messages exist.");
    }
}
