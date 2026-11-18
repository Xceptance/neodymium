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
 * Represents a structured message in a native multi-turn conversation dialogue.
 *
 * @param role the participant role (SYSTEM, USER, ASSISTANT, TOOL)
 * @param content textual content of the message
 * @param toolCalls structured tool calls proposed by an assistant message
 * @param toolCallId the identifier of the tool call this message is responding to (for TOOL role)
 * @param toolName the optional name of the tool called (for TOOL role)
 * @param attachments optional binary/multimodal attachments (e.g. screenshots)
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public record ChatMessage(
    Role role,
    String content,
    List<ToolCall> toolCalls,
    String toolCallId,
    String toolName,
    List<SutAttachment> attachments
)
{
    /**
     * Enumerates supported message participant roles.
     */
    public enum Role
    {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    /**
     * Canonical constructor performing defensive copying of collections.
     */
    public ChatMessage(
        final Role role,
        final String content,
        final List<ToolCall> toolCalls,
        final String toolCallId,
        final String toolName,
        final List<SutAttachment> attachments
    )
    {
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls == null ? Collections.emptyList() : List.copyOf(toolCalls);
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.attachments = attachments == null ? Collections.emptyList() : List.copyOf(attachments);
    }

    /**
     * 5-argument constructor without toolName.
     */
    public ChatMessage(
        final Role role,
        final String content,
        final List<ToolCall> toolCalls,
        final String toolCallId,
        final List<SutAttachment> attachments
    )
    {
        this(role, content, toolCalls, toolCallId, null, attachments);
    }

    /**
     * Creates a system message with text content.
     *
     * @param content system prompt instructions
     * @return new system ChatMessage
     */
    public static ChatMessage system(final String content)
    {
        return new ChatMessage(Role.SYSTEM, content, null, null, null, null);
    }

    /**
     * Creates a user message with text content.
     *
     * @param content user instruction
     * @return new user ChatMessage
     */
    public static ChatMessage user(final String content)
    {
        return new ChatMessage(Role.USER, content, null, null, null, null);
    }

    /**
     * Creates a user message with text content and visual attachments.
     *
     * @param content user instruction
     * @param attachments visual attachments
     * @return new user ChatMessage
     */
    public static ChatMessage user(final String content, final List<SutAttachment> attachments)
    {
        return new ChatMessage(Role.USER, content, null, null, null, attachments);
    }

    /**
     * Creates an assistant message with text content.
     *
     * @param content assistant text
     * @return new assistant ChatMessage
     */
    public static ChatMessage assistant(final String content)
    {
        return new ChatMessage(Role.ASSISTANT, content, null, null, null, null);
    }

    /**
     * Creates an assistant message with tool calls.
     *
     * @param toolCalls proposed tool calls
     * @return new assistant ChatMessage
     */
    public static ChatMessage assistant(final List<ToolCall> toolCalls)
    {
        return new ChatMessage(Role.ASSISTANT, null, toolCalls, null, null, null);
    }

    /**
     * Creates an assistant message with optional text content and tool calls.
     *
     * @param content assistant reasoning or response
     * @param toolCalls proposed tool calls
     * @return new assistant ChatMessage
     */
    public static ChatMessage assistant(final String content, final List<ToolCall> toolCalls)
    {
        return new ChatMessage(Role.ASSISTANT, content, toolCalls, null, null, null);
    }

    /**
     * Creates a tool execution result message.
     *
     * @param toolCallId the id of the tool call being answered
     * @param content structured JSON result string
     * @return new tool ChatMessage
     */
    public static ChatMessage tool(final String toolCallId, final String content)
    {
        return new ChatMessage(Role.TOOL, content, null, toolCallId, null, null);
    }

    /**
     * Creates a tool execution result message with tool name.
     *
     * @param toolCallId the id of the tool call being answered
     * @param toolName the name of the tool
     * @param content structured JSON result string
     * @return new tool ChatMessage
     */
    public static ChatMessage tool(final String toolCallId, final String toolName, final String content)
    {
        return new ChatMessage(Role.TOOL, content, null, toolCallId, toolName, null);
    }

    /**
     * Creates a tool execution result message with visual attachments.
     *
     * @param toolCallId the id of the tool call being answered
     * @param content structured JSON result string
     * @param attachments visual attachments
     * @return new tool ChatMessage
     */
    public static ChatMessage tool(final String toolCallId, final String content, final List<SutAttachment> attachments)
    {
        return new ChatMessage(Role.TOOL, content, null, toolCallId, null, attachments);
    }

    /**
     * Checks if this message contains tool calls.
     *
     * @return true if toolCalls is non-empty, false otherwise
     */
    public boolean hasToolCalls()
    {
        return this.toolCalls != null && !this.toolCalls.isEmpty();
    }

    /**
     * Formats this chat message into a clean, human-readable representation that explicitly
     * identifies its role and structure, avoiding confusion with unlabelled plain data while
     * eliminating raw record boilerplate.
     *
     * @return formatted string representation of this message
     */
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ChatMessage[").append(this.role);
        if (this.role == Role.TOOL && (this.toolName != null || this.toolCallId != null))
        {
            sb.append(" (tool=").append(this.toolName != null ? this.toolName : "?")
              .append(", id=").append(this.toolCallId != null ? this.toolCallId : "?")
              .append(")");
        }
        sb.append("]:\n");
        if (this.content != null && !this.content.isBlank())
        {
            sb.append(this.content.stripTrailing());
        }
        if (this.toolCalls != null && !this.toolCalls.isEmpty())
        {
            if (this.content != null && !this.content.isBlank())
            {
                sb.append("\n");
            }
            sb.append("Tool Calls: ").append(this.toolCalls);
        }
        if (this.attachments != null && !this.attachments.isEmpty())
        {
            if ((this.content != null && !this.content.isBlank()) || (this.toolCalls != null && !this.toolCalls.isEmpty()))
            {
                sb.append("\n");
            }
            sb.append("Attachments: ").append(this.attachments.size()).append(" item(s)");
        }
        return sb.toString();
    }

    /**
     * Formats a list of chat messages into a structured multi-turn conversation string.
     *
     * @param conversation the list of chat messages
     * @return formatted multi-turn conversation string
     */
    public static String formatConversation(final List<ChatMessage> conversation)
    {
        if (conversation == null || conversation.isEmpty())
        {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < conversation.size(); i++)
        {
            if (i > 0)
            {
                sb.append("\n\n");
            }
            sb.append(conversation.get(i).toString());
        }
        return sb.toString();
    }
}
