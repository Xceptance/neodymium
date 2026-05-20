/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
  *
 * // AI-generated: Gemini 2.0 Flash
*/
public class AiBrowser implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AiBrowser.class);

    private final AiAgent agent;

    private final AiConfiguration config;
    private final AiStats aiStats;

    private Object test;

    /**
     * Creates a new AiBrowser with default configuration.
     */
    public AiBrowser(Object test) {
        this(Neodymium.aiConfiguration(), test);
    }

    /**
     * Creates a new AiBrowser with custom configuration.
     *
     * @param config
     *               the configuration to use
     */
    public AiBrowser(final AiConfiguration config, Object test) {
        this.test = test;
        this.config = config;
        this.aiStats = new AiStats();
        this.agent = createAgent(config);

        LOG.debug("AiBrowser initialized — model: {}", config.aiModel());
    }

    /**
     * Set the main prompt to be executed.
     * 
     * @param instructions the main prompt instructions
     * @return the current AiBrowser instance
     */
    public AiBrowser prompt(String instructions) {
        Neodymium.getData().put("prompt", instructions);
        return this;
    }

    /**
     * Set instructions to be executed before the main prompt.
     * 
     * @param instructions the before instructions
     * @return the current AiBrowser instance
     */
    public AiBrowser before(String instructions) {
        Neodymium.getData().put("before", instructions);
        return this;
    }

    /**
     * Set instructions to be executed after the main prompt.
     * 
     * @param instructions the after instructions
     * @return the current AiBrowser instance
     */
    public AiBrowser after(String instructions) {
        Neodymium.getData().put("after", instructions);
        return this;
    }

    /**
     * Set the system context for the LLM.
     * 
     * @param context the system context instructions
     * @return the current AiBrowser instance
     */
    public AiBrowser systemContext(String context) {
        Neodymium.getData().put("context", context);
        return this;
    }

    /**
     * Executes natural language test instructions.
     * This is the core AI method — it sends your instructions to the LLM,
     * which analyzes the page and performs the corresponding browser actions.
     *
     * @param naturalLanguageInstructions test steps written in plain English
     */
    public void execute(final String naturalLanguageInstructions) {
        if (config.aiInteractive()) {
            try {
                com.xceptance.neodymium.ai.generator.InteractiveHud hud = Neodymium.getOrCreateInteractiveHud();
                if (Neodymium.getData() != null) {
                    hud.setDataBindings(new java.util.HashMap<>(Neodymium.getData()));
                }
            } catch (Exception e) {
            }
        }

        if (Neodymium.getData() != null && Neodymium.getData().exists("context")) {
            agent.setSutContext(resolveTestDataToPrompt(Neodymium.getData().asString("context")));
        }


        agent.execute(naturalLanguageInstructions);
    }

    /**
     * Executes natural language test instructions derived implicitly from the
     * active test dataset. Expects a `prompt`
     * variable to be defined within the currently injected dataset (e.g. via YAML).
     * 
     * @throws Throwable
     */
    public void execute() throws Throwable {
        if (!Neodymium.getData().exists("prompt")) {
            throw new IllegalArgumentException(
                    "Cannot execute AI instruction implicitly: 'prompt' property is missing from the test dataset.");
        }
        // retrieve all data once, to only have ONE test data attachment
        Neodymium.getDataAndAddToReport();

        try {
            if (!Neodymium.getData().containsKey("random")) {
                Neodymium.getData().put("random", java.util.UUID.randomUUID().toString().substring(0, 8));
            }
        } catch (Exception e) {
            // safely ignore if data is missing
        }

        if (Neodymium.getData().exists("context")) {
            agent.setSutContext(resolveTestDataToPrompt(Neodymium.getData().asString("context")));
        }


        Throwable testError = null;

        try {
            if (Neodymium.getData().exists("before")) {
                executeList(Neodymium.getData().asString("before"));
            }

            execute(Neodymium.getData().asString("prompt"));

        } catch (Throwable t) {
            testError = t;
            throw t;
        } finally {
            if (Neodymium.getData().exists("after")) {
                Throwable lastAfterError = null;
                // If it is a list, executeList will handle the individual elements
                // Wait, if an element fails in after, we want the OTHER after elements to still
                // run.
                // So executeList must catch and accumulate errors for 'after'.
                try {
                    executeListAfterMode(Neodymium.getData().asString("after"));
                } catch (Throwable afterError) {
                    if (testError != null) {
                        testError.addSuppressed(afterError);
                    } else {
                        throw afterError;
                    }
                }
            }
        }
    }

    private void executeList(String jsonOrString) {
        java.util.List<String> list = null;
        try {
            list = new com.google.gson.Gson().fromJson(jsonOrString,
                    new com.google.gson.reflect.TypeToken<java.util.List<String>>() {
                    }.getType());
        } catch (com.google.gson.JsonSyntaxException e) {
            // ignore, treat as a single string
        }

        if (list != null) {
            for (String item : list) {
                execute(item);
            }
        } else {
            execute(jsonOrString);
        }
    }

    private void executeListAfterMode(String jsonOrString) throws Throwable {
        java.util.List<String> list = null;
        try {
            list = new com.google.gson.Gson().fromJson(jsonOrString,
                    new com.google.gson.reflect.TypeToken<java.util.List<String>>() {
                    }.getType());
        } catch (com.google.gson.JsonSyntaxException e) {
            // ignore
        }

        if (list != null) {
            Throwable accumulatedError = null;
            for (String item : list) {
                try {
                    execute(item);
                } catch (Throwable t) {
                    if (accumulatedError == null) {
                        accumulatedError = t;
                    } else {
                        accumulatedError.addSuppressed(t);
                    }
                }
            }
            if (accumulatedError != null) {
                throw accumulatedError;
            }
        } else {
            // treat as a single string
            execute(jsonOrString);
        }
    }

    /**
     * Closes the browser and releases resources.
     */
    @Override
    public void close() {
        // Log cumulative token usage before shutdown
        if (aiStats.getCallCount() > 0) {
            aiStats.logSummary();
        }
    }

    private AiAgent createAgent(final AiConfiguration config) {
        final LlmClient llmClient = new LlmClient(config, aiStats);
        final PageAnalyzer pageAnalyzer = new PageAnalyzer();
        final ActionExecutor actionExecutor = new ActionExecutor(test);
        return new AiAgent(llmClient, pageAnalyzer, actionExecutor, config);
    }

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}", Pattern.CASE_INSENSITIVE);

    public static String resolveTestDataToPrompt(String promptTemplate) {
        if (promptTemplate == null || promptTemplate.isEmpty()) {
            return promptTemplate;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(promptTemplate);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(promptTemplate, lastEnd, matcher.start());
            String placeholderKey = matcher.group(1);

            String value = extractValue(placeholderKey, 0);
            if (value == null) {
                value = matcher.group(0);
            }

            sb.append(value);
            lastEnd = matcher.end();
        }
        sb.append(promptTemplate.substring(lastEnd));

        return sb.toString();
    }

    private static String extractValue(String placeholderKey, int depth) {
        // emergency stop
        if (depth > 10) {
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
        if (value == null && (placeholderKey.contains(".") || placeholderKey.contains("["))) {
            try {
                // Convert custom bracket notation like product[name] to jsonPath standard
                // product['name']
                String jsonPathQuery = placeholderKey.replaceAll("\\[([a-zA-Z0-9_\\-]+)\\]", "['$1']");

                // Add strict jsonPath prefix if needed
                if (!jsonPathQuery.startsWith("$")) {
                    jsonPathQuery = "$." + jsonPathQuery;
                }

                Object resolvedValue = testData.get(jsonPathQuery, Object.class);
                if (resolvedValue != null) {
                    value = resolvedValue.toString();
                }
            } catch (Exception e) {
                // Ignore and fall back to next resolution step
            }
        }

        // 3. Try to get from neodymium configuration
        if (value == null) {
            if (Neodymium.configuration() instanceof org.aeonbits.owner.Accessible) {
                String configValue = ((org.aeonbits.owner.Accessible) Neodymium.configuration())
                        .getProperty(placeholderKey);
                if (configValue != null) {
                    value = configValue;
                }
            }
        }

        // Recursively resolve nested placeholders within the found value
        if (value != null && value.contains("${")) {
            Matcher nestedMatcher = VARIABLE_PATTERN.matcher(value);
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            while (nestedMatcher.find()) {
                sb.append(value, lastEnd, nestedMatcher.start());
                String nestedKey = nestedMatcher.group(1);
                String nestedValue = extractValue(nestedKey, depth + 1);

                if (nestedValue == null) {
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
     * Executes the generative AI agent to create a natural language playbook.
     * 
     * @param intent The high-level intent or goal for the AI to achieve.
     */
    public void generatePrompt(final String intent) {
        generatePrompt(intent, null);
    }

    /**
     * Executes the generative AI agent to create a natural language playbook.
     * 
     * @param intent        The high-level intent or goal for the AI to achieve.
     * @param systemContext System-specific context to guide the AI's behavior.
     */
    public void generatePrompt(final String intent, final String systemContext) {
        // 1. Enforce @NeodymiumTestGenerator
        boolean isGenerator = false;
        try {
            if (test != null) {
                Class<?> testClass = test.getClass();
                if (testClass.isAnnotationPresent(com.xceptance.neodymium.junit5.NeodymiumTestGenerator.class)) {
                    isGenerator = true;
                } else {
                    String testName = Neodymium.getTestName();
                    if (testName != null && testName.contains(" :: ")) {
                        String methodName = testName.split(" :: ")[1];
                        try {
                            java.lang.reflect.Method method = testClass.getMethod(methodName);
                            if (method
                                    .isAnnotationPresent(com.xceptance.neodymium.junit5.NeodymiumTestGenerator.class)) {
                                isGenerator = true;
                            }
                        } catch (NoSuchMethodException e) {
                            // ignore
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        org.junit.jupiter.api.Assertions.assertTrue(isGenerator,
                "Method must be annotated with @NeodymiumTestGenerator to use AiBrowser.generatePrompt()");

        // 2. Build URL
        String url = Neodymium.configuration().url();
        if (url == null || url.trim().isEmpty()) {
            try {
                url = com.codeborne.selenide.WebDriverRunner.url();
            } catch (Exception e) {
            }
        }
        if (url == null || url.trim().isEmpty() || url.equals("about:blank")) {
            throw new IllegalArgumentException("No URL provided in configuration. Please provide a neodymium.url");
        }

        // 3. Build Output Path
        String outputPath = "src/test/resources/ai-playbooks/generated.yml";
        String testName = Neodymium.getTestName();
        if (testName != null && testName.contains(" :: ")) {
            String[] parts = testName.split(" :: ");
            String className = parts[0];
            String methodName = parts[1];
            outputPath = "src/test/resources/" + className.replace('.', '/') + "/" + methodName + ".yml";

            // Support @DataFolder overrides
            try {
                if (test != null) {
                    Class<?> testClass = test.getClass();
                    com.xceptance.neodymium.common.testdata.DataFolder[] folders = testClass
                            .getAnnotationsByType(com.xceptance.neodymium.common.testdata.DataFolder.class);
                    if (folders != null && folders.length > 0) {
                        outputPath = "src/test/resources/" + folders[0].value() + "/" + methodName + ".yml";
                    }
                }
            } catch (Exception e) {
            }
        }

        String resolvedSutContext = systemContext;
        try {
            if (resolvedSutContext == null && Neodymium.getData() != null && Neodymium.getData().exists("context")) {
                resolvedSutContext = resolveTestDataToPrompt(Neodymium.getData().asString("context"));
            }
        } catch (Exception e) {
            // ignore if no data is available
        }

        // 4. Trigger logic utilizing a generator-mode LLM Client
        // (uses neodymium.ai.generate.temperature, not the agent temperature)
        final LlmClient generatorClient = new LlmClient(config, aiStats, LlmMode.GENERATOR);
        com.xceptance.neodymium.ai.generator.AiPromptGenerator generator = new com.xceptance.neodymium.ai.generator.AiPromptGenerator();
        generator.generate(generatorClient, url, intent, resolvedSutContext, outputPath);
    }

    /**
     * Returns the current test case instance
     * 
     * @return
     */
    public Object getTest() {
        return test;
    }
}
