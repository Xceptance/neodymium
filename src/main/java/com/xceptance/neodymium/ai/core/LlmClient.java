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
package com.xceptance.neodymium.ai.core;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.util.Neodymium;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.TokenUsage;

/**
 * Wraps LangChain4j to communicate with Google Gemini.
 * Supports sending text prompts with optional screenshots (vision).
 * Tracks token usage via {@link AiStats}.
  *
 * @author AI-generated: Gemini 2.5 Flash
*/
public class LlmClient
{
    private static final Logger LOG = LoggerFactory.getLogger(LlmClient.class);

    private ChatModel model;
    private final AiConfiguration config;
    private final AiStats aiStats;
    private final LlmMode mode;

    private final ThreadLocal<LlmMode> currentCallMode = ThreadLocal.withInitial(() -> null);

    /**
     * Creates a new LLM client in {@link LlmMode#AGENT} mode.
     * Uses {@code neodymium.ai.temperature} (deterministic, for {@code @NeodymiumTest}).
     *
     * @param config     the application's AI configuration properties
     * @param aiStats    the execution statistics and token tracker
     */
    public LlmClient(final AiConfiguration config, final AiStats aiStats)
    {
        this(config, aiStats, LlmMode.AGENT);
    }

    /**
     * Creates a new LLM client with an explicit {@link LlmMode}.
     *
     * <ul>
     *   <li>{@link LlmMode#AGENT} — reads {@code neodymium.ai.temperature} for execution mode</li>
     *   <li>{@link LlmMode#GENERATOR} — reads {@code neodymium.ai.generate.temperature} for page generator mode</li>
     * </ul>
     *
     * @param config     the application's AI configuration properties
     * @param aiStats    the execution statistics and token tracker
     * @param mode       the operational mode controlling temperature selection
     */
    public LlmClient(final AiConfiguration config, final AiStats aiStats, final LlmMode mode)
    {
        this.config = config;
        this.aiStats = aiStats;
        this.mode = mode;
    }

    /**
     * Retrieves the AI statistics tracker associated with this client.
     *
     * @return the active {@link AiStats} tracker
     */
    public AiStats getAiStats()
    {
        return this.aiStats;
    }

    /**
     * Lazy-initializes and returns the LangChain4j ChatModel.
     * Selects configuration properties dynamically based on the execution mode (Agent vs Generator).
     *
     * @return the initialized {@link ChatModel}
     */
    private ChatModel getChatModel()
    {
        if (model != null)
        {
            return model;
        }

        final String apiKey = config.aiApiKey();
        final String modelName = config.aiModel();

        if (StringUtils.isBlank(Neodymium.aiConfiguration().aiApiKey()))
        {
            Assertions.fail(
                    "AI API key not configured. Set in your ai.properties, neodymium.properties or as an evironment variable.");
        }

        LOG.debug("   🤖 Initializing Gemini model: {}", modelName);

        final double temperature = mode == LlmMode.GENERATOR ? config.aiGenerateTemperature() : config.aiTemperature();
        LOG.debug("   🌡️ Using temperature: {} (mode={})", temperature, mode);

        model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxOutputTokens(4096)
                .responseFormat(ResponseFormat.JSON)
                .timeout(Duration.ofSeconds(config.geminiTimeoutSeconds()))
                .build();
        return model;
    }

    /**
     * Sends a text-only chat message using the default LLM mode.
     *
     * @param systemPrompt instructions for the LLM
     * @param userPrompt   the user's request
     * @return the LLM's text response
     */
    public String chat(final String systemPrompt, final String userPrompt)
    {
        LOG.debug("   💬 Sending text-only prompt ({} chars)", userPrompt.length());
        final UserMessage userMessage = UserMessage.from(userPrompt);

        return chat(systemPrompt, userMessage);
    }

    /**
     * Sends a text-only chat message with an explicit operational mode.
     *
     * @param callMode     the operational mode to record the usage under
     * @param systemPrompt instructions for the LLM
     * @param userPrompt   the user's request
     * @return the LLM's text response
     */
    public String chat(final LlmMode callMode, final String systemPrompt, final String userPrompt)
    {
        currentCallMode.set(callMode);
        try
        {
            return chat(systemPrompt, userPrompt);
        }
        finally
        {
            currentCallMode.remove();
        }
    }

    /**
     * Sends a multimodal chat message with a screenshot using the default LLM mode.
     *
     * @param systemPrompt     instructions for the LLM
     * @param userPrompt       the user's request
     * @param base64Screenshot Base64-encoded PNG screenshot
     * @return the LLM's text response
     */
    public String chatWithScreenshot(final String systemPrompt, final String userPrompt,
            final String base64Screenshot)
    {
        LOG.debug("   💬 Sending multimodal prompt ({} chars + screenshot)", userPrompt.length());
        // Build a multimodal user message with text + image
        final UserMessage userMessage = UserMessage.from(
                TextContent.from(userPrompt),
                ImageContent.from(base64Screenshot, "image/png"));

        return chat(systemPrompt, userMessage);
    }

    /**
     * Sends a multimodal chat message with a screenshot with an explicit operational mode.
     *
     * @param callMode         the operational mode to record the usage under
     * @param systemPrompt     instructions for the LLM
     * @param userPrompt       the user's request
     * @param base64Screenshot Base64-encoded PNG screenshot
     * @return the LLM's text response
     */
    public String chatWithScreenshot(final LlmMode callMode, final String systemPrompt, final String userPrompt,
            final String base64Screenshot)
    {
        currentCallMode.set(callMode);
        try
        {
            return chatWithScreenshot(systemPrompt, userPrompt, base64Screenshot);
        }
        finally
        {
            currentCallMode.remove();
        }
    }

    /**
     * Sends a list of chat messages to the LLM.
     *
     * @param messages the list of chat messages forming the conversation
     * @return the LLM's text response
     */
    public String chat(final List<ChatMessage> messages)
    {
        LOG.debug("   💬 Sending chat message list ({} messages)", messages.size());
        if (LOG.isTraceEnabled())
        {
            LOG.trace("=== LLM REQUEST MESSAGES START ===");
            for (final ChatMessage msg : messages)
            {
                LOG.trace("Role: {}, Content:\n{}", msg.type(), msg);
            }
            LOG.trace("=== LLM REQUEST MESSAGES END ===");
        }

        final ChatResponse response = getChatModel().chat(messages);
        recordTokenUsage(response);

        final String text = response.aiMessage().text();
        LOG.debug("   💬 LLM response ({} chars)", text.length());
        if (LOG.isTraceEnabled())
        {
            LOG.trace("=== LLM RESPONSE START ===\n{}\n=== LLM RESPONSE END ===", text);
        }
        return text;
    }

    /**
     * Sends a list of chat messages to the LLM with an explicit operational mode.
     *
     * @param callMode the operational mode to record the usage under
     * @param messages the list of chat messages forming the conversation
     * @return the LLM's text response
     */
    public String chat(final LlmMode callMode, final List<ChatMessage> messages)
    {
        currentCallMode.set(callMode);
        try
        {
            return chat(messages);
        }
        finally
        {
            currentCallMode.remove();
        }
    }

    /**
     * Sends a chat message to the LLM.
     *
     * @param systemPrompt
     *                     instructions for the LLM
     * @param userMessage
     *                     the user's message
     * @return the LLM's text response
     */
    private String chat(final String systemPrompt, final UserMessage userMessage)
    {
        final List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(userMessage);
        return chat(messages);
    }

    /**
     * Records token usage from the response into the stats tracker.
     */
    private void recordTokenUsage(final ChatResponse response)
    {
        final TokenUsage usage = response.tokenUsage();
        if (usage != null)
        {
            final long input = usage.inputTokenCount() != null ? usage.inputTokenCount() : 0;
            final long output = usage.outputTokenCount() != null ? usage.outputTokenCount() : 0;
            final long cached = extractCachedTokens(response);
            final LlmMode activeMode = currentCallMode.get() != null ? currentCallMode.get() : this.mode;
            aiStats.record(activeMode, input, output, cached);
        }
    }

    /**
     * Safely extracts cached input token counts from vendor-specific TokenUsage structures.
     * Uses reflection to ensure zero hard dependencies or ClassCastException risks.
     *
     * @param response the ChatResponse from the LLM
     * @return the number of cached input tokens, or 0 if not present or unsupported
     */
    private long extractCachedTokens(final ChatResponse response)
    {
        if (response == null || response.tokenUsage() == null)
        {
            return 0;
        }

        final TokenUsage usage = response.tokenUsage();

        try
        {
            // 1. Check Google Gemini-specific token usage
            if (usage.getClass().getSimpleName().equals("GoogleAiGeminiTokenUsage"))
            {
                final Method method = usage.getClass().getMethod("cachedContentTokenCount");
                final Object result = method.invoke(usage);
                if (result instanceof Integer)
                {
                    return ((Integer) result).longValue();
                }
            }

            // 2. Check OpenAI-specific token usage (promptTokensDetails -> cachedTokens)
            try
            {
                final Method detailsMethod = usage.getClass().getMethod("promptTokensDetails");
                final Object details = detailsMethod.invoke(usage);
                if (details != null)
                {
                    final Method cachedMethod = details.getClass().getMethod("cachedTokens");
                    final Object cached = cachedMethod.invoke(details);
                    if (cached instanceof Integer)
                    {
                        return ((Integer) cached).longValue();
                    }
                }
            }
            catch (final Exception e)
            {
                // Fall through for non-OpenAI models
            }
        }
        catch (final Exception e)
        {
            LOG.trace("Failed to dynamically extract cached tokens for provider class: {}",
                    usage.getClass().getName(), e);
        }

        return 0;
    }
}
