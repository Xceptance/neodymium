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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.neodymium.ai.client.ChatMessage.Role;
import org.neodymium.ai.tool.ToolDefinition;

/**
 * Immutable record representing the request parameters and context payload sent to an LLM provider.
 * Supports both single-turn prompt calls and native multi-turn tool conversations.
 *
 * @param systemMessage the system level instruction prompt
 * @param userMessage the user query or instruction prompt
 * @param attachments the list of SUT attachments (like screenshots or console logs)
 * @param responseSchema the expected/enforced output response schema format
 * @param temperature the model generation temperature setting
 * @param timeoutSeconds the request timeout in seconds
 * @param reasoningEffort the reasoning/thinking effort tier for this request
 * @param messages the native multi-turn conversation messages
 * @param tools the list of available tools provided as structured schemas
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public record LlmRequest(
    String systemMessage,
    String userMessage,
    List<SutAttachment> attachments,
    ResponseSchema responseSchema,
    double temperature,
    int timeoutSeconds,
    ReasoningEffort reasoningEffort,
    List<ChatMessage> messages,
    List<ToolDefinition> tools
)
{
    /**
     * Canonical constructor that performs defensive copying and sets default reasoning effort.
     */
    public LlmRequest(
        final String systemMessage,
        final String userMessage,
        final List<SutAttachment> attachments,
        final ResponseSchema responseSchema,
        final double temperature,
        final int timeoutSeconds,
        final ReasoningEffort reasoningEffort,
        final List<ChatMessage> messages,
        final List<ToolDefinition> tools
    )
    {
        this.systemMessage = systemMessage;
        this.userMessage = userMessage;
        this.attachments = attachments == null ? Collections.emptyList() : List.copyOf(attachments);
        this.responseSchema = responseSchema;
        this.temperature = temperature;
        this.timeoutSeconds = timeoutSeconds;
        this.reasoningEffort = reasoningEffort != null ? reasoningEffort : ResponseSchema.resolveReasoningEffort(responseSchema);
        this.tools = tools == null ? Collections.emptyList() : List.copyOf(tools);

        if (messages != null && !messages.isEmpty())
        {
            this.messages = List.copyOf(messages);
        }
        else
        {
            final List<ChatMessage> synthesized = new ArrayList<>();
            if (systemMessage != null && !systemMessage.isBlank())
            {
                synthesized.add(ChatMessage.system(systemMessage));
            }
            if (userMessage != null && !userMessage.isBlank())
            {
                synthesized.add(ChatMessage.user(userMessage, this.attachments));
            }
            this.messages = Collections.unmodifiableList(synthesized);
        }
    }

    /**
     * Multi-turn native tool calling constructor.
     *
     * @param messages native multi-turn dialogue messages
     * @param tools available tools as structured schemas
     * @param temperature model temperature
     * @param timeoutSeconds request timeout
     * @param reasoningEffort reasoning tier
     */
    public LlmRequest(
        final List<ChatMessage> messages,
        final List<ToolDefinition> tools,
        final double temperature,
        final int timeoutSeconds,
        final ReasoningEffort reasoningEffort
    )
    {
        this(
            resolveSystemContent(messages),
            resolveLastUserContent(messages),
            Collections.emptyList(),
            ResponseSchema.TEXT,
            temperature,
            timeoutSeconds,
            reasoningEffort,
            messages,
            tools
        );
    }

    /**
     * Multi-turn native tool calling constructor with attachments and response schema.
     */
    public LlmRequest(
        final List<ChatMessage> messages,
        final List<ToolDefinition> tools,
        final List<SutAttachment> attachments,
        final ResponseSchema responseSchema,
        final double temperature,
        final int timeoutSeconds,
        final ReasoningEffort reasoningEffort
    )
    {
        this(
            resolveSystemContent(messages),
            resolveLastUserContent(messages),
            attachments,
            responseSchema,
            temperature,
            timeoutSeconds,
            reasoningEffort,
            messages,
            tools
        );
    }

    /**
     * Legacy constructor resolving default reasoning effort from responseSchema.
     */
    public LlmRequest(
        final String systemMessage,
        final String userMessage,
        final List<SutAttachment> attachments,
        final ResponseSchema responseSchema,
        final double temperature,
        final int timeoutSeconds,
        final ReasoningEffort reasoningEffort
    )
    {
        this(systemMessage, userMessage, attachments, responseSchema, temperature, timeoutSeconds, reasoningEffort, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Convenience constructor without explicit reasoning effort or native tools.
     */
    public LlmRequest(
        final String systemMessage,
        final String userMessage,
        final List<SutAttachment> attachments,
        final ResponseSchema responseSchema,
        final double temperature,
        final int timeoutSeconds
    )
    {
        this(systemMessage, userMessage, attachments, responseSchema, temperature, timeoutSeconds, ResponseSchema.resolveReasoningEffort(responseSchema), Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Single-turn structured tool calling constructor with tools and response schema.
     *
     * @param systemMessage the system prompt instructions
     * @param userMessage the user instruction prompt
     * @param attachments SUT attachments
     * @param tools the list of available structured tool definitions
     * @param responseSchema the response schema type
     * @param temperature model sampling temperature
     * @param timeoutSeconds request execution timeout
     */
    public LlmRequest(
        final String systemMessage,
        final String userMessage,
        final List<SutAttachment> attachments,
        final List<ToolDefinition> tools,
        final ResponseSchema responseSchema,
        final double temperature,
        final int timeoutSeconds
    )
    {
        this(systemMessage, userMessage, attachments, responseSchema, temperature, timeoutSeconds, ResponseSchema.resolveReasoningEffort(responseSchema), Collections.emptyList(), tools);
    }

    /**
     * Creates a single-turn request with structured tools and no attachments.
     *
     * @param systemMessage the system prompt instructions
     * @param userMessage the user instruction prompt
     * @param tools the list of available structured tool definitions
     * @param responseSchema the response schema type
     * @param temperature model sampling temperature
     * @param timeoutSeconds request execution timeout
     * @return the created LlmRequest instance
     */
    public static LlmRequest withTools(
        final String systemMessage,
        final String userMessage,
        final List<ToolDefinition> tools,
        final ResponseSchema responseSchema,
        final double temperature,
        final int timeoutSeconds
    )
    {
        return new LlmRequest(systemMessage, userMessage, Collections.emptyList(), tools, responseSchema, temperature, timeoutSeconds);
    }

    /**
     * Whether this request contains structured tool definitions.
     *
     * @return true if tools are provided
     */
    public boolean hasTools()
    {
        return this.tools != null && !this.tools.isEmpty();
    }

    /**
     * Whether this request contains a multi-turn conversation beyond a single prompt/system exchange.
     *
     * @return true if the conversation contains assistant responses, tool results, or multiple dialogue turns
     */
    public boolean isMultiTurn()
    {
        if (this.messages == null || this.messages.size() <= 1)
        {
            return false;
        }
        if (this.messages.size() == 2)
        {
            final ChatMessage first = this.messages.get(0);
            final ChatMessage second = this.messages.get(1);
            if (first.role() == Role.SYSTEM && second.role() == Role.USER)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Formats all conversation messages in this request into a structured multi-turn conversation string.
     *
     * @return formatted conversation string
     */
    public String fullConversationFormatted()
    {
        return ChatMessage.formatConversation(this.messages);
    }

    /**
     * Formats all conversation messages in this request into a structured multi-turn conversation string,
     * excluding system messages (which are typically rendered separately in reports).
     *
     * @return formatted dialogue conversation string
     */
    public String dialogueConversationFormatted()
    {
        if (this.messages == null || this.messages.isEmpty())
        {
            return this.userMessage != null ? this.userMessage : "";
        }
        final List<ChatMessage> dialogueMsgs = this.messages.stream()
            .filter(msg -> msg.role() != Role.SYSTEM)
            .toList();
        if (dialogueMsgs.isEmpty())
        {
            return this.userMessage != null ? this.userMessage : "";
        }
        return ChatMessage.formatConversation(dialogueMsgs);
    }

    private static String resolveSystemContent(final List<ChatMessage> msgs)
    {
        if (msgs == null)
        {
            return "";
        }
        for (final ChatMessage msg : msgs)
        {
            if (msg.role() == Role.SYSTEM)
            {
                return msg.content() != null ? msg.content() : "";
            }
        }
        return "";
    }

    private static String resolveLastUserContent(final List<ChatMessage> msgs)
    {
        if (msgs == null)
        {
            return "";
        }
        for (int i = msgs.size() - 1; i >= 0; i--)
        {
            final ChatMessage msg = msgs.get(i);
            if (msg.role() == Role.USER)
            {
                return msg.content() != null ? msg.content() : "";
            }
        }
        return "";
    }
}
