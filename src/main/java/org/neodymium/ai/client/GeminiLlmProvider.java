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
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
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
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class GeminiLlmProvider implements LlmProvider
{
    private final AiConfiguration config;
    private final String apiKey;
    private final String modelName;
    private final ChatModel defaultModel;
    private final java.util.concurrent.ConcurrentHashMap<String, ChatModel> modelCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Constructs a GeminiLlmProvider with global configuration settings.
     */
    public GeminiLlmProvider()
    {
        this.config = AiConfiguration.getInstance();
        
        // Resolve Gemini specific properties or fallback to globals
        final String resolvedKey = this.config.getProperty("neodymium.ai.gemini.apiKey", this.config.getApiKey("gemini"));
        this.apiKey = resolvedKey != null ? resolvedKey : System.getenv("GEMINI_API_KEY");
        
        if (this.apiKey == null || this.apiKey.isBlank())
        {
            throw new IllegalArgumentException("Gemini API key is missing. Please configure neodymium.ai.gemini.apiKey or set GEMINI_API_KEY.");
        }

        this.modelName = this.config.getProperty("neodymium.ai.gemini.model", this.config.getModel("gemini"));

        this.defaultModel = buildChatModel(0.0, 180, ResponseSchema.TEXT, ReasoningEffort.MEDIUM);
    }

    /**
     * Builds a GeminiThinkingConfig instance mapped from the specified ReasoningEffort tier.
     */
    private static GeminiThinkingConfig buildThinkingConfig(final ReasoningEffort effort)
    {
        final ReasoningEffort effectiveEffort = effort != null ? effort : ReasoningEffort.MEDIUM;
        return switch (effectiveEffort)
        {
            case OFF -> GeminiThinkingConfig.builder().thinkingBudget(0).includeThoughts(false).build();
            case LOW -> GeminiThinkingConfig.builder().thinkingLevel("LOW").includeThoughts(true).build();
            case MEDIUM -> GeminiThinkingConfig.builder().thinkingLevel("MEDIUM").includeThoughts(true).build();
            case HIGH -> GeminiThinkingConfig.builder().thinkingLevel("HIGH").includeThoughts(true).build();
        };
    }

    /**
     * Builds a GoogleAiGeminiChatModel with specified temperature, timeout, response schema, and reasoning effort.
     */
    private ChatModel buildChatModel(
        final double temperature,
        final int timeoutSeconds,
        final ResponseSchema responseSchema,
        final ReasoningEffort reasoningEffort
    )
    {
        final int maxTokens = ResponseSchema.resolveMaxOutputTokens(responseSchema);
        final ReasoningEffort effort = reasoningEffort != null ? reasoningEffort : ResponseSchema.resolveReasoningEffort(responseSchema);

        return GoogleAiGeminiChatModel.builder()
            .apiKey(this.apiKey)
            .modelName(this.modelName != null ? this.modelName : "gemini-3.5-flash-lite")
            .temperature(temperature)
            .maxOutputTokens(maxTokens)
            .thinkingConfig(buildThinkingConfig(effort))
            .timeout(java.time.Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 180))
            .build();
    }

    /**
     * Resolves appropriate ChatModel for the given temperature, timeout, response schema, and reasoning effort settings.
     */
    private ChatModel getChatModel(
        final double temperature,
        final int timeoutSeconds,
        final ResponseSchema responseSchema,
        final ReasoningEffort reasoningEffort
    )
    {
        final double temp = temperature >= 0.0 ? temperature : 0.0;
        final int timeout = timeoutSeconds > 0 ? timeoutSeconds : 180;
        final ResponseSchema schema = responseSchema != null ? responseSchema : ResponseSchema.TEXT;
        final ReasoningEffort effort = reasoningEffort != null ? reasoningEffort : ResponseSchema.resolveReasoningEffort(schema);

        if (temp == 0.0 && timeout == 180 && schema == ResponseSchema.TEXT && effort == ReasoningEffort.MEDIUM)
        {
            return this.defaultModel;
        }

        final String cacheKey = String.format("%s:%.2f:%d:%s:%s", this.modelName, temp, timeout, schema.name(), effort.name());
        return this.modelCache.computeIfAbsent(cacheKey, k -> buildChatModel(temp, timeout, schema, effort));
    }

    @Override
    public LlmResponse chat(final LlmRequest rawRequest) throws IOException
    {
        final org.neodymium.ai.prompt.SanitizedPayload sanitizedPayload = org.neodymium.ai.prompt.LlmSanitizerHelper.sanitizeRequest(rawRequest);
        final LlmRequest request = org.neodymium.ai.prompt.LlmSanitizerHelper.toSanitizedRequest(rawRequest, sanitizedPayload);

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
                    final boolean isImage = attachment.mediaType() != null &&
                        (attachment.mediaType().startsWith("image/") || "screenshot".equalsIgnoreCase(attachment.mediaType()));
                    if (isImage)
                    {
                        final String mimeType = (attachment.mediaType() != null && attachment.mediaType().startsWith("image/"))
                            ? attachment.mediaType() : "image/png";
                        contents.add(ImageContent.from(attachment.base64Data(), mimeType));
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

        return LlmRetryHelper.executeWithRetry(() -> {
            try
            {
                final ChatModel activeModel = getChatModel(
                    request.temperature(),
                    request.timeoutSeconds(),
                    request.responseSchema(),
                    request.reasoningEffort()
                );
                final ChatResponse response = activeModel.chat(messages);

                final dev.langchain4j.model.output.TokenUsage usage = response.tokenUsage();

                TokenUsage mappedUsage = null;
                if (usage != null)
                {
                    mappedUsage = new TokenUsage(
                        usage.inputTokenCount(),
                        usage.outputTokenCount(),
                        usage.totalTokenCount(),
                        extractCachedTokens(usage)
                    );
                }

                final LlmResponse rawResponse = new LlmResponse(response.aiMessage().text(), mappedUsage, this.modelName);
                return org.neodymium.ai.prompt.LlmSanitizerHelper.unmaskResponse(rawResponse, sanitizedPayload.maskToVariableMap());
            }
            catch (final Exception e)
            {
                throw new IOException("Failed to execute Gemini chat request: " + e.getMessage(), e);
            }
        });
    }

    private int extractCachedTokens(final dev.langchain4j.model.output.TokenUsage usage)
    {
        if (usage == null)
        {
            return 0;
        }
        try
        {
            final java.lang.reflect.Method method = usage.getClass().getMethod("cachedContentTokenCount");
            final Object result = method.invoke(usage);
            if (result instanceof Number)
            {
                return ((Number) result).intValue();
            }
        }
        catch (final Exception e)
        {
            // Silent catch
        }
        return 0;
    }

    @Override
    public Set<LlmCapability> getCapabilities()
    {
        // Includes LlmCapability.VISION
        return EnumSet.of(
            LlmCapability.TEXT_ONLY,
            LlmCapability.VISION,
            LlmCapability.EXECUTION,
            LlmCapability.PESAP,
            LlmCapability.VERIFICATION
        );
    }
}
