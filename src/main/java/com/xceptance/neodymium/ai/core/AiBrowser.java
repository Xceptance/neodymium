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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.ai.action.Action;
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
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public class AiBrowser implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AiBrowser.class);

    private final AiAgent agent;

    private final AiConfiguration config;
    private final AiStats aiStats;

    private Object test;

    private AiExecutionResult lastExecutionResult;
    private AiTestRunResult lastTestRunResult;
    private final List<AiExecutionResult> executionResults = new CopyOnWriteArrayList<>();
    private boolean globalStatsLogged = false;
    private boolean stepStatsLogged = false;

    private final List<Class<?>> dynamicallyRegisteredClasses = new CopyOnWriteArrayList<>();

    public final void registerMethodClass(final Class<?> clazz)
    {
        if (clazz != null && !dynamicallyRegisteredClasses.contains(clazz))
        {
            dynamicallyRegisteredClasses.add(clazz);
        }
    }

    public final List<Class<?>> getDynamicallyRegisteredClasses()
    {
        return this.dynamicallyRegisteredClasses;
    }

    /**
     * Creates a new AiBrowser with default configuration.
     */
    public AiBrowser(final Object test) {
        this(Neodymium.aiConfiguration(), test);
    }

    /**
     * Creates a new AiBrowser with custom configuration.
     *
     * @param config
     *               the configuration to use
     */
    public AiBrowser(final AiConfiguration config, final Object test) {
        this(config, test, new LlmClient(config, new AiStats()), new PageAnalyzer(), new ActionExecutor(test));
    }

    /**
     * Creates a new AiBrowser with custom configuration and injected dependencies.
     *
     * @param config          the configuration to use
     * @param test            the test class instance
     * @param llmClient       the LLM client instance
     * @param pageAnalyzer    the page analyzer instance
     * @param actionExecutor  the action executor instance
     */
    public AiBrowser(final AiConfiguration config, final Object test,
            final LlmClient llmClient, final PageAnalyzer pageAnalyzer,
            final ActionExecutor actionExecutor) {
        this.test = test;
        this.config = config;
        this.aiStats = llmClient.getAiStats();
        this.agent = new AiAgent(llmClient, pageAnalyzer, actionExecutor, config);

        LOG.debug("╔════════════════════════════════════════════════════════════════════════════════════");
        LOG.debug("║ 🎬 STARTING TEST CASE: {}", Neodymium.getTestName());
        LOG.debug("║ 🤖 AI Model:           {}", config.aiModel());
        LOG.debug("╚════════════════════════════════════════════════════════════════════════════════════");
    }

    public final AiExecutionResult getLastExecutionResult()
    {
        return this.lastExecutionResult;
    }

    public final void setLastExecutionResult(final AiExecutionResult result)
    {
        this.lastExecutionResult = result;
    }

    public final AiTestRunResult getLastTestRunResult()
    {
        return this.lastTestRunResult;
    }

    public final void setLastTestRunResult(final AiTestRunResult result)
    {
        this.lastTestRunResult = result;
    }

    /**
     * Set the main steps to be executed.
     * 
     * @param instructions the main steps instructions
     * @return the current AiBrowser instance
     */
    public AiBrowser steps(final String instructions)
    {
        Neodymium.getData().put("steps", instructions);
        return this;
    }

    /**
     * Set instructions to be executed before the main prompt.
     * 
     * @param instructions the before instructions
     * @return the current AiBrowser instance
     */
    public AiBrowser before(final String instructions) {
        Neodymium.getData().put("before", instructions);
        return this;
    }

    /**
     * Set instructions to be executed after the main prompt.
     * 
     * @param instructions the after instructions
     * @return the current AiBrowser instance
     */
    public AiBrowser after(final String instructions) {
        Neodymium.getData().put("after", instructions);
        return this;
    }

    /**
     * Set the system context for the LLM.
     * 
     * @param context the system context instructions
     * @return the current AiBrowser instance
     */
    public AiBrowser systemContext(final String context) {
        Neodymium.getData().put("context", context);
        return this;
    }

    /**
     * Executes natural language test instructions.
     * This is the core AI method — it sends your instructions to the LLM,
     * which analyzes the page and performs the corresponding browser actions.
     *
     * @param naturalLanguageInstructions test steps written in plain English
     * @return the execution result details
     */
    public final AiExecutionResult execute(final String naturalLanguageInstructions) {
        if (config.aiInteractive()) {
            try {
                com.xceptance.neodymium.ai.generator.InteractiveHud hud = Neodymium.getOrCreateInteractiveHud();
                if (Neodymium.getData() != null) {
                    hud.setDataBindings(new java.util.HashMap<>(Neodymium.getData()));
                }
            } catch (Exception e) {
            }
        }

        final AiExecutionResult result = new AiExecutionResult(Neodymium.getData(), this);
        this.lastExecutionResult = result;
        this.executionResults.add(result);

        if (Neodymium.getData() != null && Neodymium.getData().exists("context")) {
            agent.setSutContext(resolveTestDataToPrompt(Neodymium.getData().asString("context"), result.getLookups()));
        }

        try {
            agent.execute(naturalLanguageInstructions, result);
        } catch (final Throwable t) {
            throw t;
        }

        return result;
    }

    /**
     * Executes natural language test instructions derived implicitly from the
     * active test dataset. Expects a `steps`
     * variable to be defined within the currently injected dataset (e.g. via YAML).
     * 
     * @return the composite execution result for this test run
     * @throws Throwable
     */
    public final AiTestRunResult execute() throws Throwable
    {
        if (!Neodymium.getData().exists("steps"))
        {
            throw new IllegalArgumentException(
                    "Cannot execute AI instruction implicitly: 'steps' property is missing from the test dataset.");
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

        final AiTestRunResult runResult = new AiTestRunResult();
        this.lastTestRunResult = runResult;
        Throwable testError = null;

        try {
            if (Neodymium.getData().exists("before")) {
                runResult.setBeforeResult(executeAndGetResult(Neodymium.getData().asString("before")));
            }

            runResult.setStepsResult(execute(Neodymium.getData().asString("steps")));

        } catch (final Throwable t)
        {
            testError = t;
            throw t;
        } finally {
            if (Neodymium.getData().exists("after")) {
                try {
                    executeListAfterMode(Neodymium.getData().asString("after"), runResult);
                } catch (Throwable afterError) {
                    if (testError != null) {
                        testError.addSuppressed(afterError);
                    } else {
                        throw afterError;
                    }
                }
            }
        }

        return runResult;
    }

    private AiExecutionResult executeAndGetResult(final String jsonOrString) {
        java.util.List<String> list = null;
        try {
            list = new com.google.gson.Gson().fromJson(jsonOrString,
                    new com.google.gson.reflect.TypeToken<java.util.List<String>>() {
                    }.getType());
        } catch (com.google.gson.JsonSyntaxException e) {
            // ignore, treat as a single string
        }

        if (list != null) {
            AiExecutionResult lastRes = null;
            for (final String item : list) {
                lastRes = execute(item);
            }
            return lastRes;
        } else {
            return execute(jsonOrString);
        }
    }

    private void executeListAfterMode(final String jsonOrString, final AiTestRunResult runResult) throws Throwable {
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
            for (final String item : list) {
                try {
                    runResult.addAfterResult(execute(item));
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
            runResult.addAfterResult(execute(jsonOrString));
        }
    }

    /**
     * Closes the browser and releases resources.
     */
    @Override
    public void close()
    {
        if (!globalStatsLogged && aiStats.getOverallCallCount() > 0)
        {
            aiStats.logSummary();
            globalStatsLogged = true;
        }
        if (!stepStatsLogged && (aiStats.getReplayCount() > 0 || aiStats.getDirectParseCount() > 0 || aiStats.getOverallCallCount() > 0))
        {
            logStepSummary();
            stepStatsLogged = true;
        }
    }

    /**
     * Logs both the cumulative AI execution statistics and the step-by-step trace statistics.
     */
    public void logStatsAndStepSummary()
    {
        if (!globalStatsLogged && aiStats.getOverallCallCount() > 0)
        {
            aiStats.logSummary();
            globalStatsLogged = true;
        }
        if (!stepStatsLogged && (aiStats.getReplayCount() > 0 || aiStats.getDirectParseCount() > 0 || aiStats.getOverallCallCount() > 0))
        {
            logStepSummary();
            stepStatsLogged = true;
        }
    }

    /**
     * Logs the cumulative AI execution statistics.
     */
    public void logStats()
    {
        if (aiStats.getOverallCallCount() > 0)
        {
            aiStats.logSummary();
            globalStatsLogged = true;
        }
    }

    private void logStepSummary()
    {
        LOG.trace("======== 📊 AI Step Execution Statistics ========");
        int stepIndex = 1;
        for (final AiExecutionResult result : executionResults)
        {
            synchronized (result.getSteps())
            {
                for (final StepDetails step : result.getSteps())
                {
                    logSingleStep(step, stepIndex++);
                }
            }
        }
        LOG.trace("=================================================");
    }

    /**
     * Logs the step-by-step trace statistics for a specific execution result.
     *
     * @param result the execution result to log
     */
    public final void logStepSummary(final AiExecutionResult result)
    {
        LOG.trace("======== 📊 AI Step Execution Statistics ========");
        int stepIndex = 1;
        synchronized (result.getSteps())
        {
            for (final StepDetails step : result.getSteps())
            {
                logSingleStep(step, stepIndex++);
            }
        }
        LOG.trace("=================================================");
        this.stepStatsLogged = true;
    }

    /**
     * Clears the accumulated execution results list.
     */
    public final void clearExecutionResults()
    {
        this.executionResults.clear();
    }

    private void logSingleStep(final StepDetails step, final int stepIndex)
    {
        LOG.trace("  Step {}: {}", stepIndex, step.getRawInstruction());
        final String mode;
        if (step.isReplayed())
        {
            mode = "REPLAY";
        }
        else if (step.isDirectParse())
        {
            mode = "DIRECT_PARSE";
        }
        else
        {
            mode = "LLM";
        }
        LOG.trace("    Mode:           {}", mode);
        LOG.trace("    Duration:       {} ms", step.getDurationMs());

        final List<Action> actions = step.getActions();
        if (actions != null && !actions.isEmpty())
        {
            final String actionTypes = actions.stream()
                    .map(Action::getType)
                    .collect(Collectors.joining(", "));
            LOG.trace("    Actions:        {} ({})", actions.size(), actionTypes);
        }
        else
        {
            LOG.trace("    Actions:        0");
        }

        long standardIn = 0;
        long standardOut = 0;
        long standardCached = 0;
        int standardCalls = 0;

        long pesapIn = 0;
        long pesapOut = 0;
        long pesapCached = 0;
        int pesapCalls = 0;

        if (step.getPesapCall() != null)
        {
            final LlmCallDetails pc = step.getPesapCall();
            pesapIn += pc.getInputTokens();
            pesapOut += pc.getOutputTokens();
            pesapCached += pc.getCachedTokens();
            pesapCalls = 1;
        }

        for (final LlmCallDetails call : step.getLlmCalls())
        {
            if (call.getCallMode() == LlmMode.PESAP)
            {
                pesapIn += call.getInputTokens();
                pesapOut += call.getOutputTokens();
                pesapCached += call.getCachedTokens();
                pesapCalls++;
            }
            else
            {
                standardIn += call.getInputTokens();
                standardOut += call.getOutputTokens();
                standardCached += call.getCachedTokens();
                standardCalls++;
            }
        }

        if (standardCalls > 0)
        {
            LOG.trace("    Standard Calls: {} (Tokens: {} in ({} cached) → {} out)",
                      standardCalls, standardIn, standardCached, standardOut);
        }
        if (pesapCalls > 0)
        {
            LOG.trace("    PESAP Calls:    {} (Tokens: {} in ({} cached) → {} out)",
                      pesapCalls, pesapIn, pesapCached, pesapOut);
        }

        if (step.getFailureReason() != null)
        {
            LOG.trace("    Failure:        {}", step.getFailureReason());
        }
    }

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}", Pattern.CASE_INSENSITIVE);

    public static String resolveTestDataToPrompt(final String promptTemplate) {
        return resolveTestDataToPrompt(promptTemplate, null);
    }

    public static String resolveTestDataToPrompt(final String promptTemplate, final List<LookupDetails> lookupsCollector) {
        if (promptTemplate == null || promptTemplate.isEmpty()) {
            return promptTemplate;
        }

        final Matcher matcher = VARIABLE_PATTERN.matcher(promptTemplate);
        final StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(promptTemplate, lastEnd, matcher.start());
            final String placeholderKey = matcher.group(1);

            final String value = extractValue(placeholderKey, 0, lookupsCollector);
            if (value == null) {
                sb.append(matcher.group(0));
            } else {
                sb.append(value);
            }
            lastEnd = matcher.end();
        }
        sb.append(promptTemplate.substring(lastEnd));

        return sb.toString();
    }

    private static String extractValue(final String placeholderKey, final int depth, final List<LookupDetails> lookupsCollector) {
        // emergency stop
        if (depth > 10) {
            return null;
        }

        final TestData testData = Neodymium.getData();
        String value = null;
        String source = "Not Found";

        // 1. Try to find the exact value (case-sensitive)
        if (testData.containsKey(placeholderKey)) {
            value = testData.get(placeholderKey);
            source = "TestData Map";
        }

        // 1b. Try to find the value case-insensitively if not found exactly
        if (value == null) {
            final java.util.Optional<Map.Entry<String, String>> entryOpt = testData.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(placeholderKey))
                    .findFirst();
            if (entryOpt.isPresent()) {
                value = entryOpt.get().getValue();
                source = "TestData Map";
            }
        }

        // 2. Try nested resolution via JSONPath if applicable
        if (value == null && (placeholderKey.contains(".") || placeholderKey.contains("["))) {
            try {
                String jsonPathQuery = placeholderKey.replaceAll("\\[([a-zA-Z0-9_\\-]+)\\]", "['$1']");
                if (!jsonPathQuery.startsWith("$")) {
                    jsonPathQuery = "$." + jsonPathQuery;
                }

                final Object resolvedValue = testData.get(jsonPathQuery, Object.class);
                if (resolvedValue != null) {
                    value = resolvedValue.toString();
                    source = "JSONPath Query";
                }
            } catch (final Exception e) {
                // Ignore and fall back to next resolution step
            }
        }

        // 3. Try to get from neodymium configuration
        if (value == null) {
            if (Neodymium.configuration() instanceof org.aeonbits.owner.Accessible) {
                final String configValue = ((org.aeonbits.owner.Accessible) Neodymium.configuration())
                        .getProperty(placeholderKey);
                if (configValue != null) {
                    value = configValue;
                    source = "Neodymium Configuration";
                }
            }
        }

        // Recursively resolve nested placeholders within the found value
        if (value != null && value.contains("${")) {
            final Matcher nestedMatcher = VARIABLE_PATTERN.matcher(value);
            final StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            while (nestedMatcher.find()) {
                sb.append(value, lastEnd, nestedMatcher.start());
                final String nestedKey = nestedMatcher.group(1);
                final String nestedValue = extractValue(nestedKey, depth + 1, lookupsCollector);

                if (nestedValue == null) {
                    sb.append(nestedMatcher.group(0));
                } else {
                    sb.append(nestedValue);
                }
                lastEnd = nestedMatcher.end();
            }
            sb.append(value.substring(lastEnd));
            value = sb.toString();
        }

        // Translate / Localize
        boolean localized = false;
        if (value != null) {
            final String localizedValue = Neodymium.tryLocalizedText(value);
            if (localizedValue != null && !localizedValue.equals(value)) {
                value = localizedValue;
                localized = true;
                if ("Not Found".equals(source) || "TestData Map".equals(source) || "JSONPath Query".equals(source) || "Neodymium Configuration".equals(source)) {
                    source = "Localization File";
                }
            }
        }

        if (lookupsCollector != null) {
            lookupsCollector.add(new LookupDetails(placeholderKey, value, localized, source));
        }

        return value;
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

    /**
     * Returns the cumulative AI execution statistics for the current browser session.
     * 
     * @return the AiStats instance
     */
    public AiStats getStats()
    {
        return aiStats;
    }
}
