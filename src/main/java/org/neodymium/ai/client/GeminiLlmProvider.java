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

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Native Google Gemini AI provider backed by LangChain4j.
 * Translates Neodymium requests into standard LangChain4j Gemini interactions,
 * supporting multimodal image attachments natively.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class GeminiLlmProvider implements LlmProvider
{
    private final AiConfiguration config;
    private final String apiKey;
    private final String modelName;

    /**
     * Constructs a GeminiLlmProvider and dynamically resolves its configuration.
     */
    public GeminiLlmProvider()
    {
        this.config = new AiConfiguration();
        
        // Resolve Gemini specific properties or fallback to globals
        final String resolvedKey = this.config.getProperty("neodymium.ai.gemini.apiKey", this.config.getApiKey("gemini"));
        this.apiKey = resolvedKey != null ? resolvedKey : System.getenv("GEMINI_API_KEY");
        
        this.modelName = this.config.getProperty("neodymium.ai.gemini.model", this.config.getModel("gemini"));
    }

    @Override
    public LlmResponse chat(final LlmRequest request) throws IOException
    {
        if (this.apiKey == null || this.apiKey.isBlank())
        {
            throw new IOException("Gemini API key is missing. Please configure neodymium.ai.gemini.apiKey or set GEMINI_API_KEY.");
        }

        final ChatModel model = GoogleAiGeminiChatModel.builder()
            .apiKey(this.apiKey)
            .modelName(this.modelName != null ? this.modelName : "gemini-3.5-flash")
            .temperature(request.temperature())
            .build();

        final List<ChatMessage> messages = new ArrayList<>();
        if (request.systemMessage() != null && !request.systemMessage().isBlank())
        {
            messages.add(SystemMessage.from(request.systemMessage()));
        }

        final List<Content> contents = new ArrayList<>();
        if (request.userMessage() != null && !request.userMessage().isBlank())
        {
            contents.add(TextContent.from(request.userMessage()));
        }

        if (request.attachments() != null)
        {
            for (final SutAttachment attachment : request.attachments())
            {
                if (attachment.base64Data() != null)
                {
                    if (attachment.mediaType() != null && attachment.mediaType().startsWith("image/"))
                    {
                        contents.add(ImageContent.from(attachment.base64Data(), attachment.mediaType()));
                    }
                    else
                    {
                        // Attach non-image data as text (e.g. JSON or DOM source)
                        try
                        {
                            final String decoded = new String(java.util.Base64.getDecoder().decode(attachment.base64Data()));
                            contents.add(TextContent.from("\n--- Attachment: " + attachment.mediaType() + " ---\n" + decoded));
                        }
                        catch (final Exception e)
                        {
                            contents.add(TextContent.from("\n--- [Unreadable Attachment: " + attachment.mediaType() + "] ---\n"));
                        }
                    }
                }
            }
        }

        messages.add(UserMessage.from(contents));

        try
        {
            final ChatResponse response = model.chat(messages);
            final dev.langchain4j.model.output.TokenUsage usage = response.tokenUsage();
            
            TokenUsage mappedUsage = null;
            if (usage != null)
            {
                mappedUsage = new TokenUsage(usage.inputTokenCount(), usage.outputTokenCount(), usage.totalTokenCount());
            }

            return new LlmResponse(response.aiMessage().text(), mappedUsage, this.modelName);
        }
        catch (final Exception e)
        {
            throw new IOException("Failed to execute Gemini chat request: " + e.getMessage(), e);
        }
    }

    @Override
    public Set<LlmCapability> getCapabilities()
    {
        // Includes LlmCapability.VISION
        return EnumSet.of(
            LlmCapability.TEXT_ONLY,
            LlmCapability.VISION,
            LlmCapability.STRUCTURED_JSON,
            LlmCapability.STEP_SPLITTING,
            LlmCapability.VERIFICATION
        );
    }
}
