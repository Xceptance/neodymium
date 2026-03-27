package com.xceptance.neodymium.ai.core;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Main entry point for AI-powered browser test automation.
 * Manages the WebDriver lifecycle and the AI agent.
 *
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * try (AiBrowser ai = new AiBrowser()) {
 *     ai.execute("""
 *                 Open https://example.com
 *                 Click on the login button.
 *                 Type 'user@test.com' into the email field.
 *             """);
 * }
 * }</pre>
 */
public class AiBrowser implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AiBrowser.class);

    private final AiAgent agent;

    private final AiConfiguration config;
    private final TokenStats tokenStats;

    private Object test;

    /**
     * Creates a new AiBrowser with default configuration.
     */
    public AiBrowser(Object test)
    {
        this(Neodymium.aiConfiguration(), test);
    }

    /**
     * Creates a new AiBrowser with custom configuration.
     *
     * @param config
     *            the configuration to use
     */
    public AiBrowser(final AiConfiguration config, Object test)
    {
        this.test = test;
        this.config = config;
        this.tokenStats = new TokenStats();
        this.agent = createAgent(config);

        LOG.debug("AiBrowser initialized — model: {}", config.aiModel());
    }

    /**
     * Executes natural language test instructions.
     * This is the core AI method — it sends your instructions to the LLM,
     * which analyzes the page and performs the corresponding browser actions.
     *
     * @param naturalLanguageInstructions test steps written in plain English
     */
    public void execute(final String naturalLanguageInstructions) {
        agent.execute(resolveTestDataToPrompt(naturalLanguageInstructions));
    }

    /**
     * Closes the browser and releases resources.
     */
    @Override
    public void close() {
        // Log cumulative token usage before shutdown
        tokenStats.logSummary();
    }

    private AiAgent createAgent(final AiConfiguration config) {
        final LlmClient llmClient = new LlmClient(config, tokenStats);
        final PageAnalyzer pageAnalyzer = new PageAnalyzer();
        final ActionExecutor actionExecutor = new ActionExecutor(test);
        return new AiAgent(llmClient, pageAnalyzer, actionExecutor, config);
    }


    public static String resolveTestDataToPrompt(String promptTemplate)
    {
        Map<String, String> testData = Neodymium.getData();

        // Matcher for ${any_variable_name}
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(promptTemplate);

        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find())
        {
            sb.append(promptTemplate, lastEnd, matcher.start());
            String placeholderKey = matcher.group(1);

            // Find the value by ignoring case in the map keys
            String value = testData.entrySet().stream()
                                   .filter(entry -> entry.getKey().equalsIgnoreCase(placeholderKey))
                                   .map(Map.Entry::getValue)
                                   .findFirst()
                                   .orElse(matcher.group(0)); // Fallback to original ${KEY} if not found

            sb.append(value);
            lastEnd = matcher.end();
        }
        sb.append(promptTemplate.substring(lastEnd));

        return sb.toString();
    }
}
