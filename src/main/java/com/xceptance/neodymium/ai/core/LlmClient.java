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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.ai.config.AiConfiguration;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
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
 * @author Xceptance GmbH 2026
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
     * Protected no-arg constructor to permit browserless instantiations without live config dependencies.
     */
    protected LlmClient()
    {
        this.config = null;
        this.aiStats = null;
        this.mode = null;
    }

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
     * Masks the given API key for safe logging, keeping only the first 4 and the last 4 characters,
     * and filling the middle characters with '*' to preserve the original key length.
     * If the key length is 8 or fewer, the entire key is masked.
     *
     * @param key the API key to mask
     * @return the masked API key
     */
    static String maskKey(final String key)
    {
        if (StringUtils.isBlank(key))
        {
            return "";
        }

        final int len = key.length();
        if (len > 8)
        {
            return key.substring(0, 4) + "*".repeat(len - 8) + key.substring(len - 4);
        }

        return "*".repeat(len);
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

        final String apiKey = (mode == LlmMode.ASSERT && StringUtils.isNotBlank(config.aiAssertionApiKey()))
                ? config.aiAssertionApiKey() : config.aiApiKey();
        final String modelName = (mode == LlmMode.ASSERT && StringUtils.isNotBlank(config.aiAssertionModel()))
                ? config.aiAssertionModel() : config.aiModel();

        if (StringUtils.isBlank(apiKey))
        {
            Assertions.fail(
                    "AI API key not configured. Set in your ai.properties, neodymium.properties or as an environment variable.");
        }

        LOG.debug("   🤖 Initializing Gemini model: {} using API key: {}", modelName, maskKey(apiKey));

        final double temperature;
        if (mode == LlmMode.GENERATOR)
        {
            temperature = config.aiGenerateTemperature();
        }
        else if (mode == LlmMode.ASSERT)
        {
            final Double assertTemp = config.aiAssertionTemperature();
            temperature = assertTemp != null ? assertTemp : 0.0;
        }
        else
        {
            temperature = config.aiTemperature();
        }
        LOG.debug("   🌡️ Using temperature: {} (mode={})", temperature, mode);

        final int timeoutSeconds = (mode == LlmMode.ASSERT && config.aiAssertionTimeoutSeconds() != null)
                ? config.aiAssertionTimeoutSeconds() : config.geminiTimeoutSeconds();

        model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxOutputTokens(4096)
                .responseFormat(ResponseFormat.JSON)
                .timeout(Duration.ofSeconds(timeoutSeconds))
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
     * Converts a Base64-encoded PNG screenshot to a Base64-encoded JPEG screenshot on-the-fly.
     * Handles transparency safely by painting onto a solid white background first.
     *
     * @param base64Png the original Base64-encoded PNG image string
     * @return the compressed Base64-encoded JPEG image string, or original string on failure
     */
    private String convertPngToJpgBase64(final String base64Png)
    {
        try
        {
            final byte[] pngBytes = Base64.getDecoder().decode(base64Png);
            final BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (originalImage == null)
            {
                return base64Png;
            }

            final BufferedImage rgbCopy = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);

            final Graphics2D g = rgbCopy.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, originalImage.getWidth(), originalImage.getHeight());
            g.drawImage(originalImage, 0, 0, null);
            g.dispose();

            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(rgbCopy, "jpg", bos);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to convert PNG screenshot to JPEG on-the-fly. Falling back to PNG.", e);
            return base64Png;
        }
    }

    /**
     * Formats a ChatMessage for trace logging, substituting Base64 image data with a placeholder.
     *
     * @param msg the ChatMessage to format
     * @return a clean string representation of the message
     */
    private static String formatChatMessageForLog(final ChatMessage msg)
    {
        if (msg instanceof final UserMessage userMsg)
        {
            if (userMsg.contents() == null)
            {
                return msg.toString();
            }
            final StringBuilder sb = new StringBuilder();
            sb.append("UserMessage { contents = [");
            boolean first = true;
            for (final Content content : userMsg.contents())
            {
                if (!first)
                {
                    sb.append(", ");
                }
                first = false;
                if (content instanceof final TextContent textContent)
                {
                    sb.append("TextContent { text = \"").append(textContent.text()).append("\" }");
                }
                else if (content instanceof final ImageContent imageContent)
                {
                    sb.append("ImageContent { image = [BASE64 DATA (MIME: ")
                            .append(imageContent.image().mimeType()).append(")] }");
                }
                else
                {
                    sb.append(content.toString());
                }
            }
            sb.append("] }");
            return sb.toString();
        }
        return msg.toString();
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

        final String base64Jpg = convertPngToJpgBase64(base64Screenshot);
        final String mimeType = base64Jpg.equals(base64Screenshot) ? "image/png" : "image/jpeg";

        // Build a multimodal user message with text + image
        final UserMessage userMessage = UserMessage.from(
                TextContent.from(userPrompt),
                ImageContent.from(base64Jpg, mimeType));

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
                LOG.trace("Role: {}, Content:\n{}", msg.type(), formatChatMessageForLog(msg));
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
