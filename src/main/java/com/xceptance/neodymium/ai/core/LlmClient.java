package com.xceptance.neodymium.ai.core;

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
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.TokenUsage;

/**
 * Wraps LangChain4j to communicate with Google Gemini.
 * Supports sending text prompts with optional screenshots (vision).
 * Tracks token usage via {@link TokenStats}.
 */
public class LlmClient {
    private static final Logger LOG = LoggerFactory.getLogger(LlmClient.class);

    private ChatLanguageModel model;
    private final AiConfiguration config;
    private final TokenStats tokenStats;
    private final LlmMode mode;

    /**
     * Creates a new LLM client in {@link LlmMode#AGENT} mode.
     * Uses {@code neodymium.ai.temperature} (deterministic, for {@code @NeodymiumTest}).
     *
     * @param config     application configuration
     * @param tokenStats token usage tracker
     */
    public LlmClient(final AiConfiguration config, final TokenStats tokenStats) {
        this(config, tokenStats, LlmMode.AGENT);
    }

    /**
     * Creates a new LLM client with an explicit {@link LlmMode}.
     *
     * <ul>
     *   <li>{@link LlmMode#AGENT} — reads {@code neodymium.ai.temperature}</li>
     *   <li>{@link LlmMode#GENERATOR} — reads {@code neodymium.ai.generate.temperature}</li>
     * </ul>
     *
     * @param config     application configuration
     * @param tokenStats token usage tracker
     * @param mode       the operational mode controlling temperature selection
     */
    public LlmClient(final AiConfiguration config, final TokenStats tokenStats, final LlmMode mode) {
        this.config = config;
        this.tokenStats = tokenStats;
        this.mode = mode;
    }

    public TokenStats getTokenStats() {
        return this.tokenStats;
    }

    private ChatLanguageModel getChatModel() {
        if (model != null) {
            return model;
        }

        final String apiKey = config.aiApiKey();
        final String modelName = config.aiModel();

        if (StringUtils.isBlank(Neodymium.aiConfiguration().aiApiKey())) {
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
                .timeout(Duration.ofSeconds(config.geminiTimeoutSeconds()))
                .build();
        return model;
    }

    /**
     * Sends a text-only chat message.
     *
     * @param systemPrompt instructions for the LLM
     * @param userPrompt   the user's request
     * @return the LLM's text response
     */
    public String chat(final String systemPrompt, final String userPrompt) {
        LOG.debug("   💬 Sending text-only prompt ({} chars)", userPrompt.length());
        UserMessage userMessage = UserMessage.from(userPrompt);

        return chat(systemPrompt, userMessage);
    }

    /**
     * Sends a multimodal chat message with a screenshot.
     *
     * @param systemPrompt     instructions for the LLM
     * @param userPrompt       the user's request
     * @param base64Screenshot Base64-encoded PNG screenshot
     * @return the LLM's text response
     */
    public String chatWithScreenshot(final String systemPrompt, final String userPrompt,
            final String base64Screenshot) {
        LOG.debug("   💬 Sending multimodal prompt ({} chars + screenshot)", userPrompt.length());
        // Build a multimodal user message with text + image
        final UserMessage userMessage = UserMessage.from(
                TextContent.from(userPrompt),
                ImageContent.from(base64Screenshot, "image/png"));

        return chat(systemPrompt, userMessage);
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
    private String chat(final String systemPrompt, UserMessage userMessage) {
        final List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(userMessage);

        final ChatResponse response = getChatModel().chat(messages);
        recordTokenUsage(response);

        final String text = response.aiMessage().text();
        LOG.debug("   💬 LLM response ({} chars)", text.length());
        return text;
    }

    /**
     * Records token usage from the response into the stats tracker.
     */
    private void recordTokenUsage(final ChatResponse response) {
        final TokenUsage usage = response.tokenUsage();
        if (usage != null) {
            final long input = usage.inputTokenCount() != null ? usage.inputTokenCount() : 0;
            final long output = usage.outputTokenCount() != null ? usage.outputTokenCount() : 0;
            tokenStats.record(input, output);
        }
    }
}
