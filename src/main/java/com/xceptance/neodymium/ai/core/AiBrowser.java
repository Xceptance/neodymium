package com.xceptance.neodymium.ai.core;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.common.testdata.TestData;
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
     * Executes natural language test instructions derived implicitly from the active test dataset.
     * Expects a `prompt` variable to be defined within the currently injected dataset (e.g. via YAML).
     */
    public void execute() {
        if (!Neodymium.getData().exists("prompt")) {
            throw new IllegalArgumentException("Cannot execute AI instruction implicitly: 'prompt' property is missing from the test dataset.");
        }
        // retrieve all data once, to only have ONE test data attachment
        Neodymium.getDataAndAddToReport();

        execute(Neodymium.getData().asString("prompt"));
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


    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}", Pattern.CASE_INSENSITIVE);

    public static String resolveTestDataToPrompt(String promptTemplate)
    {
        if (promptTemplate == null || promptTemplate.isEmpty())
        {
            return promptTemplate;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(promptTemplate);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find())
        {
            sb.append(promptTemplate, lastEnd, matcher.start());
            String placeholderKey = matcher.group(1);
            
            String value = extractValue(placeholderKey, 0);
            if (value == null)
            {
                value = matcher.group(0);
            }

            sb.append(value);
            lastEnd = matcher.end();
        }
        sb.append(promptTemplate.substring(lastEnd));

        return sb.toString();
    }

    private static String extractValue(String placeholderKey, int depth)
    {
        // emergency stop
        if (depth > 10)
        {
            return null;
        }

        TestData testData = Neodymium.getData();
        String value = null;

        // 1. Try to find the exact value (ignoring case)
        value = testData.entrySet().stream()
                        .filter(entry -> entry.getKey().equalsIgnoreCase(placeholderKey))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);

        // 2. Try nested resolution via JSONPath if applicable
        if (value == null && (placeholderKey.contains(".") || placeholderKey.contains("[")))
        {
            try
            {
                // Convert custom bracket notation like product[name] to jsonPath standard product['name']
                String jsonPathQuery = placeholderKey.replaceAll("\\[([a-zA-Z0-9_\\-]+)\\]", "['$1']");
                
                // Add strict jsonPath prefix if needed
                if (!jsonPathQuery.startsWith("$"))
                {
                    jsonPathQuery = "$." + jsonPathQuery;
                }

                Object resolvedValue = testData.get(jsonPathQuery, Object.class);
                if (resolvedValue != null)
                {
                    value = resolvedValue.toString();
                }
            }
            catch (Exception e)
            {
                // Ignore and fall back to next resolution step
            }
        }

        // 3. Try to get from neodymium configuration
        if (value == null && placeholderKey.toLowerCase().startsWith("neodymium."))
        {
            if (Neodymium.configuration() instanceof org.aeonbits.owner.Accessible)
            {
                String configValue = ((org.aeonbits.owner.Accessible) Neodymium.configuration()).getProperty(placeholderKey);
                if (configValue != null)
                {
                    value = configValue;
                }
            }
        }

        // Recursively resolve nested placeholders within the found value
        if (value != null && value.contains("${"))
        {
            Matcher nestedMatcher = VARIABLE_PATTERN.matcher(value);
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            while (nestedMatcher.find())
            {
                sb.append(value, lastEnd, nestedMatcher.start());
                String nestedKey = nestedMatcher.group(1);
                String nestedValue = extractValue(nestedKey, depth + 1);
                
                if (nestedValue == null)
                {
                    nestedValue = nestedMatcher.group(0);
                }
                
                sb.append(nestedValue);
                lastEnd = nestedMatcher.end();
            }
            sb.append(value.substring(lastEnd));
            value = sb.toString();
        }

        return Neodymium.tryLocalizedText(value);
    }

    /**
     * Returns the current test case instance
     * 
     * @return
     */
    public Object getTest()
    {
        return test;
    }
}
