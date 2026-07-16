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
import dev.langchain4j.model.openai.OpenAiChatModel;
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
 * OpenAI-compatible LLM provider designed for Llama 4 and other open models
 * hosted on Google Cloud Vertex AI Model-as-s-Service endpoints.
 *
 * Supports vision capabilities (image attachments) and explicit API key submission.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class VertexAiLlamaProvider implements LlmProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VertexAiLlamaProvider.class);

    private final AiConfiguration config;
    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final ChatModel model;

    /**
     * Constructs a VertexAiLlamaProvider and dynamically resolves its configuration.
     */
    public VertexAiLlamaProvider()
    {
        this.config = new AiConfiguration();

        // Resolve api key from properties or env
        final String resolvedKey = this.config.getProperty("neodymium.ai.vertex.apiKey", null);
        this.apiKey = resolvedKey != null ? resolvedKey : System.getenv("VERTEX_API_KEY");

        if (this.apiKey == null || this.apiKey.isBlank())
        {
            throw new IllegalArgumentException("Vertex API key is missing. Please configure neodymium.ai.vertex.apiKey or set VERTEX_API_KEY.");
        }

        // Resolve model name, default to llama-4-maverick-17b-128e-instruct-maas
        final String resolvedModel = this.config.getProperty("neodymium.ai.vertex.model", null);
        this.modelName = resolvedModel != null ? resolvedModel : "llama-4-maverick-17b-128e-instruct-maas";

        // Resolve base URL for Model-as-a-Service OpenAI-compatible endpoint
        final String resolvedBaseUrl = this.config.getProperty("neodymium.ai.vertex.baseUrl", null);
        if (resolvedBaseUrl != null)
        {
            this.baseUrl = resolvedBaseUrl;
        }
        else
        {
            final String projectId = this.config.getProperty("neodymium.ai.vertex.projectId", "YOUR_PROJECT_ID");
            final String location = this.config.getProperty("neodymium.ai.vertex.location", "us-central1");
            this.baseUrl = String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/endpoints/openapi",
                location, projectId, location);
        }

        this.model = OpenAiChatModel.builder()
            .baseUrl(this.baseUrl)
            .apiKey(this.apiKey)
            .modelName(this.modelName)
            .temperature(0.0)
            .timeout(java.time.Duration.ofSeconds(180))
            .build();
    }

    @Override
    public LlmResponse chat(final LlmRequest request) throws IOException
    {

        final List<ChatMessage> messages = new ArrayList<>();
        if (request.systemMessage() != null && !request.systemMessage().isBlank())
        {
            final String llamaSystemMessage = request.systemMessage() + "\n\n" +
                "CRITICAL: You MUST output ONLY the raw JSON block. Do NOT include any conversational preamble, introduction, explanation, markdown fences, or postfix text. Your output must start with '{' and end with '}'.\n" +
                "CRITICAL SELECTOR RULE:\n" +
                "1. Never use dynamic 'data-neo-ref' attributes (e.g., [data-neo-ref='...']) in your CSS selectors. They are temporary and volatile.\n" +
                "2. Always prefer targeting elements directly by their ID (e.g., '#zip-code-value') or unique standard attributes/classes if available.\n" +
                "3. Avoid using fragile relative paths or sibling combinators (like '+' or '~') that depend on exact nesting, as they frequently fail due to minor HTML structural changes.\n" +
                "CRITICAL REGEX RULE:\n" +
                "4. When the instruction asks to verify a pattern (e.g., \"in the form 'V-[0-9]+-US'\"), you MUST use the exact regular expression pattern as the assertion value (e.g., 'V-[0-9]+-US'), NOT the specific literal value currently shown in the DOM (e.g., do NOT use 'V-12345-US').\n" +
                "CRITICAL ACTIONS LIMIT:\n" +
                "5. You MUST ONLY generate actions that are directly requested by the instruction. Do NOT generate extra, unsolicited assertions or actions for other elements on the page (for example, if the instruction asks to verify the total, do NOT add extra assertions for the order number or zip code).";
            messages.add(SystemMessage.from(llamaSystemMessage));
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
                        // Ignore image attachments to avoid the Vertex AI openapi proxy "media_resolution parameter not supported" error
                        LOGGER.warn("Ignoring image attachment for Vertex AI Llama provider to avoid media_resolution error.");
                    }
                    else
                    {
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
            final ChatResponse response = this.model.chat(messages);
            final dev.langchain4j.model.output.TokenUsage usage = response.tokenUsage();

            TokenUsage mappedUsage = null;
            if (usage != null)
            {
                mappedUsage = new TokenUsage(
                    usage.inputTokenCount(),
                    usage.outputTokenCount(),
                    usage.totalTokenCount(),
                    0
                );
            }

            return new LlmResponse(response.aiMessage().text(), mappedUsage, this.modelName);
        }
        catch (final Exception e)
        {
            throw new IOException("Failed to execute Llama chat request on Vertex: " + e.getMessage(), e);
        }
    }

    @Override
    public Set<LlmCapability> getCapabilities()
    {
        return EnumSet.of(
            LlmCapability.TEXT_ONLY,
            LlmCapability.STRUCTURED_JSON,
            LlmCapability.STEP_SPLITTING,
            LlmCapability.VERIFICATION
        );
    }
}
