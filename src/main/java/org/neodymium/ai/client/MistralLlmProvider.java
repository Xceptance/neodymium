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
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.neodymium.ai.config.AiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Native Mistral AI provider backed by LangChain4j.
 * Translates Neodymium requests into standard LangChain4j Mistral interactions.
 * 
 * Note: This provider does not declare the VISION capability as standard Mistral endpoints
 * are primarily text-based in this implementation layer.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class MistralLlmProvider implements LlmProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MistralLlmProvider.class);

    private final AiConfiguration config;
    private final String apiKey;
    private final String modelName;

    /**
     * Constructs a MistralLlmProvider and dynamically resolves its configuration.
     */
    public MistralLlmProvider()
    {
        this.config = new AiConfiguration();
        
        // Resolve Mistral specific properties or fallback to globals
        final String resolvedKey = this.config.getProperty("neodymium.ai.mistral.apiKey", this.config.getApiKey("mistral"));
        this.apiKey = resolvedKey != null ? resolvedKey : System.getenv("MISTRAL_API_KEY");
        
        this.modelName = this.config.getProperty("neodymium.ai.mistral.model", this.config.getModel("mistral"));
    }

    @Override
    public LlmResponse chat(final LlmRequest request) throws IOException
    {
        if (this.apiKey == null || this.apiKey.isBlank())
        {
            throw new IOException("Mistral API key is missing. Please configure neodymium.ai.mistral.apiKey or set MISTRAL_API_KEY.");
        }

        final ChatModel model = MistralAiChatModel.builder()
            .apiKey(this.apiKey)
            .modelName(this.modelName != null ? this.modelName : "mistral-large-latest")
            .temperature(request.temperature())
            .build();

        final List<ChatMessage> messages = new ArrayList<>();
        if (request.systemMessage() != null && !request.systemMessage().isBlank())
        {
            messages.add(SystemMessage.from(request.systemMessage()));
        }

        if (request.attachments() != null && !request.attachments().isEmpty())
        {
            LOGGER.warn("MistralLlmProvider does not support image attachments. Attachments will be ignored.");
        }

        messages.add(UserMessage.from(request.userMessage()));

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
            throw new IOException("Failed to execute Mistral chat request: " + e.getMessage(), e);
        }
    }

    @Override
    public Set<LlmCapability> getCapabilities()
    {
        // Excludes LlmCapability.VISION
        return EnumSet.of(
            LlmCapability.TEXT_ONLY,
            LlmCapability.STRUCTURED_JSON,
            LlmCapability.STEP_SPLITTING,
            LlmCapability.VERIFICATION
        );
    }
}
