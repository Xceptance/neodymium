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
package org.neodymium.ai.junit;

import com.codeborne.selenide.WebDriverRunner;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.neodymium.ai.client.InMemoryLlmCache;
import org.neodymium.ai.client.LlmCacheHelper;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.InteractiveConsoleListener;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
import org.neodymium.ai.playbook.PlaybookParser;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.prompt.ActionExtractionPrompt;
import org.neodymium.ai.resources.ClasspathResourceManager;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.ai.session.AiSession;
import org.neodymium.common.browser.BrowserData;
import org.neodymium.common.browser.BrowserMethodData;
import org.neodymium.junit5.browser.BrowserExecutionCallback;
import org.neodymium.util.Neodymium;

import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.ai.console.InteractiveConsoleServer;

/**
 * JUnit 5 {@link TestTemplateInvocationContextProvider} implementation for Neodymium AI tests.
 * Resolves playbooks, filters datasets, maps sequential executions, and manages lifecycle context.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class NeodymiumAiRunner implements TestTemplateInvocationContextProvider, BeforeAllCallback, AfterAllCallback
{
    /**
     * In-memory storage for inline playbooks registered at test runtime.
     */
    private static final Map<String, String> INLINE_PLAYBOOKS = new ConcurrentHashMap<>();

    /**
     * Constructs a default NeodymiumAiRunner.
     */
    public NeodymiumAiRunner()
    {
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception
    {
        InMemoryLlmCache.clear();
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception
    {
        InMemoryLlmCache.clear();
    }

    @Override
    public boolean supportsTestTemplate(final ExtensionContext context)
    {
        if (context.getTestMethod().isEmpty())
        {
            return false;
        }
        final Class<?> testClass = context.getRequiredTestClass();
        final Method method = context.getRequiredTestMethod();
        return testClass.isAnnotationPresent(NeodymiumAiTest.class)
            || testClass.isAnnotationPresent(AiPlaybook.class)
            || method.isAnnotationPresent(AiPlaybook.class)
            || method.isAnnotationPresent(AiInlinePlaybook.class)
            || testClass.isAnnotationPresent(org.neodymium.common.testdata.DataFile.class)
            || method.isAnnotationPresent(org.neodymium.common.testdata.DataFile.class)
            || testClass.isAnnotationPresent(com.xceptance.neodymium.common.testdata.DataFile.class)
            || method.isAnnotationPresent(com.xceptance.neodymium.common.testdata.DataFile.class);
    }

    @Override
    public boolean mayReturnZeroTestTemplateInvocationContexts(final ExtensionContext context)
    {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(final ExtensionContext context)
    {
        final Method method = context.getRequiredTestMethod();
        final Class<?> testClass = context.getRequiredTestClass();

        if (method.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)
            || method.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)
            || method.isAnnotationPresent(org.junit.jupiter.api.BeforeAll.class)
            || method.isAnnotationPresent(org.junit.jupiter.api.AfterAll.class))
        {
            return Stream.empty();
        }

        // 1. Resolve playbook paths or inline playbooks
        final List<String> playbookPaths = new ArrayList<>();
        final AiInlinePlaybook methodInlinePlaybook = method.getAnnotation(AiInlinePlaybook.class);
        final AiPlaybook methodPlaybook = method.getAnnotation(AiPlaybook.class);
        final AiPlaybook classPlaybook = testClass.getAnnotation(AiPlaybook.class);

        if (methodInlinePlaybook != null && !methodInlinePlaybook.value().isEmpty())
        {
            final String yamlContent = methodInlinePlaybook.value();
            final String key = "inline-" + Math.abs(yamlContent.hashCode()) + ".yaml";
            INLINE_PLAYBOOKS.put(key, yamlContent);
            playbookPaths.add(key);
        }
        else
        {
            final String testFileFilter = com.xceptance.neodymium.util.Neodymium.configuration().getTestFileFilter();
            if (org.apache.commons.lang3.StringUtils.isNotBlank(testFileFilter))
            {
                String cleanFilter = testFileFilter.replace("\\.", ".").replaceAll("^\\^\\(|\\)\\$$", "");
                if (!cleanFilter.endsWith(".yaml") && !cleanFilter.endsWith(".yml"))
                {
                    cleanFilter = cleanFilter + ".yaml";
                }
                playbookPaths.add(cleanFilter);
            }
            else if (methodPlaybook != null && !methodPlaybook.value().isEmpty())
            {
                playbookPaths.add(methodPlaybook.value());
            }
            else
            {
                if (classPlaybook != null && !classPlaybook.value().isEmpty())
                {
                    playbookPaths.add(classPlaybook.value());
                }
                else
                {
                    final NeodymiumAiTest classAiTest = testClass.getAnnotation(NeodymiumAiTest.class);
                    if (classAiTest != null && classAiTest.value().length > 0)
                    {
                        for (final String path : classAiTest.value())
                        {
                            if (path != null && !path.isEmpty())
                            {
                                playbookPaths.add(path);
                            }
                        }
                    }
                    else
                    {
                        org.neodymium.common.testdata.DataFile dataFileClass = testClass.getAnnotation(org.neodymium.common.testdata.DataFile.class);
                        if (dataFileClass == null)
                        {
                            final com.xceptance.neodymium.common.testdata.DataFile legacyDataFileClass = testClass.getAnnotation(com.xceptance.neodymium.common.testdata.DataFile.class);
                            if (legacyDataFileClass != null)
                            {
                                playbookPaths.add(legacyDataFileClass.value());
                            }
                        }
                        else if (!dataFileClass.value().isEmpty())
                        {
                            playbookPaths.add(dataFileClass.value());
                        }

                        if (playbookPaths.isEmpty())
                        {
                            org.neodymium.common.testdata.DataFile dataFileMethod = method.getAnnotation(org.neodymium.common.testdata.DataFile.class);
                            if (dataFileMethod == null)
                            {
                                final com.xceptance.neodymium.common.testdata.DataFile legacyDataFileMethod = method.getAnnotation(com.xceptance.neodymium.common.testdata.DataFile.class);
                                if (legacyDataFileMethod != null && !legacyDataFileMethod.value().isEmpty())
                                {
                                    playbookPaths.add(legacyDataFileMethod.value());
                                }
                            }
                            else if (!dataFileMethod.value().isEmpty())
                            {
                                playbookPaths.add(dataFileMethod.value());
                            }
                        }
                    }
                }
            }
        }

        // Default to class/method/dataset convention if no explicit playbook path is configured
        if (playbookPaths.isEmpty())
        {
            final String dsLabel;
            final AiDataSet methodDS = method.getAnnotation(AiDataSet.class);
            if (methodDS != null && methodDS.value().length > 0)
            {
                dsLabel = "_" + methodDS.value()[0];
            }
            else
            {
                final AiDataSet classDS = testClass.getAnnotation(AiDataSet.class);
                if (classDS != null && classDS.value().length > 0)
                {
                    dsLabel = "_" + classDS.value()[0];
                }
                else
                {
                    dsLabel = "";
                }
            }

            final String defaultPlaybookName = testClass.getSimpleName() + "_" + method.getName() + dsLabel + ".yaml";
            final String resourcePath = testClass.getPackageName().replace('.', '/') + "/" + defaultPlaybookName;
            playbookPaths.add(resourcePath);
        }

        // 2. Resolve execution modes
        final List<ExecutionMode> modes = new ArrayList<>();
        final AiMode methodMode = method.getAnnotation(AiMode.class);
        if (methodMode != null)
        {
            for (final ExecutionMode m : methodMode.value())
            {
                if (m != null)
                {
                    modes.add(m);
                }
            }
        }
        else
        {
            final AiMode classMode = testClass.getAnnotation(AiMode.class);
            if (classMode != null)
            {
                for (final ExecutionMode m : classMode.value())
                {
                    if (m != null)
                    {
                        modes.add(m);
                    }
                }
            }
        }
        if (modes.isEmpty())
        {
            modes.add(AiConfiguration.getInstance().getExecutionMode());
        }

        // 3. Resolve dataset filters
        final List<AiDataSet> datasetFilters = new ArrayList<>();
        final AiDataSet methodDataSet = method.getAnnotation(AiDataSet.class);
        if (methodDataSet != null)
        {
            datasetFilters.add(methodDataSet);
        }
        final AiDataSets methodDataSets = method.getAnnotation(AiDataSets.class);
        if (methodDataSets != null)
        {
            Collections.addAll(datasetFilters, methodDataSets.value());
        }
        
        final AiDataSet classDataSet = testClass.getAnnotation(AiDataSet.class);
        if (classDataSet != null)
        {
            datasetFilters.add(classDataSet);
        }
        final AiDataSets classDataSets = testClass.getAnnotation(AiDataSets.class);
        if (classDataSets != null)
        {
            Collections.addAll(datasetFilters, classDataSets.value());
        }

        final org.neodymium.common.testdata.DataSet neoDataSet = method.getAnnotation(org.neodymium.common.testdata.DataSet.class);
        final com.xceptance.neodymium.common.testdata.DataSet legacyDataSet = method.getAnnotation(com.xceptance.neodymium.common.testdata.DataSet.class);
        final String legacyDsIdFilter = (neoDataSet != null && !neoDataSet.id().isEmpty()) ? neoDataSet.id()
                : (legacyDataSet != null && !legacyDataSet.id().isEmpty()) ? legacyDataSet.id() : null;

        final PlaybookResourceManager manager = new HybridResourceManager(new ClasspathResourceManager());
        final List<String> resolvedPaths = new ArrayList<>();
        for (final String path : playbookPaths)
        {
            if (path != null && !path.isEmpty())
            {
                if (INLINE_PLAYBOOKS.containsKey(path))
                {
                    resolvedPaths.add(path);
                }
                else if ("programmatic".equalsIgnoreCase(path))
                {
                    final String name = (methodPlaybook != null && !methodPlaybook.name().isEmpty()) ? methodPlaybook.name()
                                      : (classPlaybook != null && !classPlaybook.name().isEmpty()) ? classPlaybook.name()
                                      : testClass.getSimpleName() + "_" + method.getName();
                    final String virtualPath = "playbooks/integration/programmatic/" + name + ".yaml";
                    resolvedPaths.add(virtualPath);
                }
                else if (path.startsWith("/"))
                {
                    // Absolute path from classpath root (leading slash stripped)
                    resolvedPaths.add(path.substring(1));
                }
                else
                {
                    // Relative path from test class package unless it contains path separators or exists at root
                    final String packagePath = testClass.getPackageName().replace('.', '/');
                    if (path.startsWith(packagePath + "/") || path.contains("/"))
                    {
                        resolvedPaths.add(path);
                    }
                    else
                    {
                        boolean existsAtRoot = false;
                        try (final java.io.InputStream in = manager.read(path))
                        {
                            if (in != null)
                            {
                                existsAtRoot = true;
                            }
                        }
                        catch (final Exception ignored)
                        {
                        }

                        if (existsAtRoot)
                        {
                            resolvedPaths.add(path);
                        }
                        else
                        {
                            resolvedPaths.add(packagePath + "/" + path);
                        }
                    }
                }
            }
        }

        final List<TestTemplateInvocationContext> invocationContexts = new ArrayList<>();
        final PlaybookParser parser = new YamlPlaybookParser();

        final BrowserData browserData = new BrowserData(testClass);
        final List<BrowserMethodData> browsers = browserData.createIterationData(method);
        if (browsers.isEmpty())
        {
            browsers.add(null);
        }

        for (final String playbookPath : resolvedPaths)
        {
            Playbook playbook;
            try
            {
                if (playbookPath.startsWith("playbooks/integration/programmatic/"))
                {
                    try
                    {
                        playbook = parser.parse(playbookPath, manager);
                    }
                    catch (final IllegalArgumentException e)
                    {
                        playbook = new Playbook(new ArrayList<>(), new ArrayList<>());
                    }
                }
                else
                {
                    playbook = parser.parse(playbookPath, manager);
                }
            }
            catch (final Exception e)
            {
                throw new RuntimeException("Failed to parse playbook: " + playbookPath, e);
            }

            final List<Map<String, SessionData.DataEntry>> allDataSets = playbook.getDataSets();
            final List<Map<String, SessionData.DataEntry>> filteredDataSets = new ArrayList<>();

            if (allDataSets.isEmpty())
            {
                // Run once with empty dataset
                filteredDataSets.add(Collections.emptyMap());
            }
            else
            {
                final String globalTestIdFilter = com.xceptance.neodymium.util.Neodymium.configuration().getTestIdFilter();
                final java.util.regex.Pattern globalTestIdPattern = org.apache.commons.lang3.StringUtils.isNotBlank(globalTestIdFilter)
                        ? java.util.regex.Pattern.compile(globalTestIdFilter)
                        : null;

                int dsIndex = 1;
                for (final Map<String, SessionData.DataEntry> ds : allDataSets)
                {
                    String dsId = getDataSetId(ds);
                    if (dsId == null)
                    {
                        dsId = String.valueOf(dsIndex);
                    }
                    boolean legacyMatch = (legacyDsIdFilter == null) || legacyDsIdFilter.equalsIgnoreCase(dsId);
                    boolean globalMatch = (globalTestIdPattern == null) || globalTestIdPattern.matcher(dsId).find();
                    if (legacyMatch && globalMatch && shouldIncludeDataSet(dsId, datasetFilters))
                    {
                        filteredDataSets.add(ds);
                    }
                    dsIndex++;
                }
            }

            // Create template invocation context for Cartesian product of browsers, playbooks, datasets, and execution modes
            for (final BrowserMethodData browser : browsers)
            {
                for (final ExecutionMode mode : modes)
                {
                    for (final Map<String, SessionData.DataEntry> dataset : filteredDataSets)
                    {
                        final String dsId = getDataSetId(dataset);
                        invocationContexts.add(new TestTemplateInvocationContext()
                        {
                            @Override
                            public String getDisplayName(final int invocationIndex)
                            {
                                final String name = playbookPath.substring(playbookPath.lastIndexOf('/') + 1);
                                final String datasetLabel = dsId != null ? dsId : "default";
                                final String browserLabel = browser != null ? " :: Browser " + browser.getBrowserTag() : "";
                                return String.format("[%d] playbook=%s, dataset=%s, mode=%s%s", invocationIndex, name, datasetLabel, mode, browserLabel);
                            }

                            @Override
                            public List<Extension> getAdditionalExtensions()
                            {
                                final List<Extension> extensions = new ArrayList<>();
                                if (browser != null)
                                {
                                    extensions.add(new BrowserExecutionCallback(browser, method.getName()));
                                }
                                extensions.add(new AiInvocationExtension(playbookPath, dataset, mode, dsId, browser));
                                return extensions;
                            }
                        });
                    }
                }
            }
        }

        return invocationContexts.stream();
    }

    private String getDataSetId(final Map<String, SessionData.DataEntry> dataSet)
    {
        for (final String key : new String[]{"testId", "testid", "id", "testID"})
        {
            if (dataSet.containsKey(key))
            {
                final Object val = dataSet.get(key).value();
                if (val != null)
                {
                    return String.valueOf(val);
                }
            }
        }
        return null;
    }

    private boolean shouldIncludeDataSet(final String dsId, final List<AiDataSet> filters)
    {
        if (filters.isEmpty())
        {
            return true;
        }

        boolean included = false;
        boolean hasInclusions = false;

        for (final AiDataSet filter : filters)
        {
            final List<String> includesList = new ArrayList<>();
            Collections.addAll(includesList, filter.value());
            Collections.addAll(includesList, filter.include());

            if (!includesList.isEmpty())
            {
                hasInclusions = true;
                if (matchesAny(dsId, includesList))
                {
                    included = true;
                }
            }

            if (filter.exclude().length > 0 && matchesAny(dsId, Arrays.asList(filter.exclude())))
            {
                return false;
            }
        }

        return hasInclusions ? included : true;
    }

    private boolean matchesAny(final String val, final List<String> patterns)
    {
        if (val == null)
        {
            return false;
        }
        for (final String pattern : patterns)
        {
            if (val.equals(pattern))
            {
                return true;
            }
            try
            {
                if (val.matches(pattern))
                {
                    return true;
                }
            }
            catch (final Exception e)
            {
                // Fall back to literal check on error
            }
        }
        return false;
    }

    private static class AiInvocationExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver, InvocationInterceptor
    {
        private final String playbookPath;
        private final Map<String, SessionData.DataEntry> dataset;
        private final ExecutionMode mode;
        private final String datasetId;
        private final BrowserMethodData browser;
        private AiSession session;
        private String recordingPath;
        private PlaybookResourceManager resourceManager;

        public AiInvocationExtension(
            final String playbookPath,
            final Map<String, SessionData.DataEntry> dataset,
            final ExecutionMode mode,
            final String datasetId,
            final BrowserMethodData browser
        )
        {
            this.playbookPath = playbookPath;
            this.dataset = dataset;
            this.mode = mode;
            this.datasetId = datasetId;
            this.browser = browser;
        }

        @Override
        public void beforeEach(final ExtensionContext context) throws Exception
        {
            // Set test name dynamically in the Neodymium context
            if (context.getRequiredTestMethod() != null && context.getRequiredTestClass() != null)
            {
                final String testName = context.getRequiredTestClass().getSimpleName() + "." + context.getRequiredTestMethod().getName();
                Neodymium.setTestName(testName);
                com.xceptance.neodymium.util.Neodymium.setTestName(testName);
            }

            if (this.browser != null)
            {
                Neodymium.setBrowserProfileName(this.browser.getBrowserTag());
            }

            if (Neodymium.hasDriver())
            {
                final org.neodymium.common.browser.WebDriverStateContainer legacyCont = Neodymium.getWebDriverStateContainer();
                if (legacyCont != null && legacyCont.getWebDriver() != null)
                {
                    final org.openqa.selenium.WebDriver driver = legacyCont.getDecoratedWebDriver() != null
                        ? legacyCont.getDecoratedWebDriver()
                        : legacyCont.getWebDriver();
                    com.codeborne.selenide.WebDriverRunner.setWebDriver(driver);
                }
            }

            // Automatically detect mock integration test package and apply thread-local overrides
            if (context.getRequiredTestClass() != null)
            {
                final String fqcn = context.getRequiredTestClass().getName();
                if (fqcn.contains(".integration.mock.") || fqcn.contains(".sandbox.mock."))
                {
                    Neodymium.getData().put("neodymium.ai.global.provider", "mock");
                    Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
                }
            }

            // Copy playbook dataset variables to Neodymium.getData() for thread-local overrides
            if (dataset != null)
            {
                dataset.forEach((key, entry) -> {
                    if (entry != null && entry.value() != null)
                    {
                        Neodymium.getData().put(key, String.valueOf(entry.value()));
                    }
                });
            }

            final SessionData sessionData = new SessionData(new HashMap<>(dataset));
            
            final LlmRegistry registry = new LlmRegistry();
            final AiConfiguration config = AiConfiguration.getInstance();
            LlmRegistry.bootstrap(registry, config);
            LlmCacheHelper.wrapRegistryIfActive(registry);

            final ExecutionEventBus eventBus = new ExecutionEventBus();
            final SelenideTargetExecutor executor = new SelenideTargetExecutor();

            this.session = AiSession.mock(this.mode, sessionData, registry, eventBus, executor);
            final ExecutionContext executionContext = this.session.getExecutionContext();
            executor.setExecutionContext(executionContext);

            final boolean isInteractive = config.isInteractive();
            final boolean isManagerActive = config.isManagerActive();
            final boolean isConsoleExecutionLogsEnabled = config.isConsoleExecutionLogsEnabled();

            if (isInteractive || isManagerActive || isConsoleExecutionLogsEnabled)
            {
                final String runId = config.getProperty("neodymium.managerRunId", "run_" + System.currentTimeMillis());
                final InteractiveConsoleEngine consoleEngine = new InteractiveConsoleEngine(runId);

                if (isInteractive && !isManagerActive)
                {
                    try
                    {
                        final InteractiveConsoleServer consoleServer = new InteractiveConsoleServer(consoleEngine);
                        consoleServer.openBrowser();
                        executionContext.getTransientData().put("interactiveConsoleServer", consoleServer);
                    }
                    catch (final Exception e)
                    {
                        org.slf4j.LoggerFactory.getLogger(NeodymiumAiRunner.class).error("Failed to start standalone InteractiveConsoleServer", e);
                    }
                }

                final InteractiveConsoleListener interactiveListener = new InteractiveConsoleListener(consoleEngine, this.session, isInteractive);
                eventBus.registerListener(interactiveListener);
            }
            
            final PlaybookParser parser = new YamlPlaybookParser();
            final PlaybookResourceManager manager = new HybridResourceManager(new ClasspathResourceManager());
            this.resourceManager = manager;
            
            final Class<?> testClass = context.getRequiredTestClass();
            final Method method = context.getRequiredTestMethod();
            final String browserProfile = Neodymium.getBrowserProfileName();

            if (testClass != null)
            {
                executionContext.getTransientData().put("testClass", testClass.getName());
            }
            if (method != null)
            {
                executionContext.getTransientData().put("testMethod", method.getName());
            }
            if (playbookPath != null)
            {
                executionContext.getTransientData().put("playbookFile", playbookPath);
            }
            if (this.datasetId != null)
            {
                executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_DATASET_LABEL, this.datasetId);
            }
            if (browserProfile != null && !browserProfile.isEmpty())
            {
                executionContext.getTransientData().put("browser", browserProfile);
            }
            else
            {
                final String browserName = Neodymium.getBrowserName();
                if (browserName != null && !browserName.isEmpty())
                {
                    executionContext.getTransientData().put("browser", browserName);
                }
            }

            final AiContext methodContextAnnot = method != null ? method.getAnnotation(AiContext.class) : null;
            final AiContext classContextAnnot = testClass != null ? testClass.getAnnotation(AiContext.class) : null;

            int resolvedInputBudget = -1;
            int resolvedOutputBudget = -1;

            if (methodContextAnnot != null && methodContextAnnot.tokenBudgetInput() > 0)
            {
                resolvedInputBudget = methodContextAnnot.tokenBudgetInput();
            }
            else if (classContextAnnot != null && classContextAnnot.tokenBudgetInput() > 0)
            {
                resolvedInputBudget = classContextAnnot.tokenBudgetInput();
            }

            if (methodContextAnnot != null && methodContextAnnot.tokenBudgetOutput() > 0)
            {
                resolvedOutputBudget = methodContextAnnot.tokenBudgetOutput();
            }
            else if (classContextAnnot != null && classContextAnnot.tokenBudgetOutput() > 0)
            {
                resolvedOutputBudget = classContextAnnot.tokenBudgetOutput();
            }

            if (resolvedInputBudget > 0)
            {
                executionContext.getTransientData().put(ExecutionContext.KEY_TOKEN_BUDGET_INPUT, resolvedInputBudget);
            }
            if (resolvedOutputBudget > 0)
            {
                executionContext.getTransientData().put(ExecutionContext.KEY_TOKEN_BUDGET_OUTPUT, resolvedOutputBudget);
            }

            String recMethod = null;
            String recFileName = null;
            String recDir = null;
            final AiPlaybook methodPb = method.getAnnotation(AiPlaybook.class);
            final AiPlaybook classPb = testClass != null ? testClass.getAnnotation(AiPlaybook.class) : null;
            if (methodPb != null && !methodPb.recordingMethod().isEmpty())
            {
                recMethod = methodPb.recordingMethod();
            }
            else if (classPb != null && !classPb.recordingMethod().isEmpty())
            {
                recMethod = classPb.recordingMethod();
            }
            if (methodPb != null && !methodPb.recordingFileName().isEmpty())
            {
                recFileName = methodPb.recordingFileName();
            }
            else if (methodPb != null && !methodPb.name().isEmpty())
            {
                recFileName = methodPb.name();
            }
            else if (classPb != null && !classPb.recordingFileName().isEmpty())
            {
                recFileName = classPb.recordingFileName();
            }
            else if (classPb != null && !classPb.name().isEmpty())
            {
                recFileName = classPb.name();
            }

            if (methodPb != null && !methodPb.recordingDirectory().isEmpty())
            {
                recDir = methodPb.recordingDirectory();
            }
            else if (classPb != null && !classPb.recordingDirectory().isEmpty())
            {
                recDir = classPb.recordingDirectory();
            }

            String resolvedPlaybookPath = playbookPath;
            if (this.mode.isReplay())
            {
                // Replay modes: try candidate paths using recordingMethod / recordingFileName, then method defaults
                final List<String> candidatePaths = new ArrayList<>();
                candidatePaths.add(computeRecordingPath(playbookPath, testClass, method, this.datasetId, browserProfile, recMethod, recFileName, recDir));
                candidatePaths.add(computeRecordingPath(playbookPath, testClass, method, this.datasetId, null, recMethod, recFileName, recDir));
                candidatePaths.add(computeRecordingPath(playbookPath, testClass, null, this.datasetId, browserProfile, null, recFileName, recDir));
                candidatePaths.add(computeRecordingPath(playbookPath, testClass, null, this.datasetId, null, null, recFileName, recDir));
                candidatePaths.add(computeLegacyRecordingPath(playbookPath, this.datasetId));

                String companionJsonPath = null;
                for (final String candidate : candidatePaths)
                {
                    if (candidate == null || candidate.isEmpty())
                    {
                        continue;
                    }
                    try (final java.io.InputStream in = manager.read(candidate))
                    {
                        if (in != null)
                        {
                            companionJsonPath = candidate;
                            break;
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }

                if (companionJsonPath != null)
                {
                    executionContext.getTransientData().put("playbookRecordingFile", companionJsonPath);
                    resolvedPlaybookPath = companionJsonPath;
                }
                else
                {
                    final String msg = String.format(
                        "Replay mode '%s' failed for test '%s.%s': No recorded companion JSON file found. Candidate paths searched:\n  - %s\n"
                        + "Please run the live recording test first to generate the recording.",
                        this.mode,
                        testClass != null ? testClass.getSimpleName() : "UnknownClass",
                        method != null ? method.getName() : "unknownMethod",
                        String.join("\n  - ", candidatePaths.stream().filter(p -> p != null && !p.isEmpty()).distinct().toList())
                    );
                    org.slf4j.LoggerFactory.getLogger(NeodymiumAiRunner.class).error("❌ {}", msg);
                    throw new java.io.FileNotFoundException(msg);
                }
            }
            else if (this.mode.isLive())
            {
                // Live/recording modes: automatically prefer companion YAML file if present (so we start from original instructions)
                String companionYamlPath = playbookPath;
                if (companionYamlPath.endsWith(".json"))
                {
                    companionYamlPath = companionYamlPath.substring(0, companionYamlPath.length() - 5) + ".yaml";
                    boolean found = false;
                    try (final java.io.InputStream in = manager.read(companionYamlPath))
                    {
                        if (in != null)
                        {
                            resolvedPlaybookPath = companionYamlPath;
                            found = true;
                        }
                    }
                    catch (final Exception e)
                    {
                        // Fall back to .yml
                    }
                    if (!found)
                    {
                        companionYamlPath = playbookPath.substring(0, playbookPath.length() - 5) + ".yml";
                        try (final java.io.InputStream in = manager.read(companionYamlPath))
                        {
                            if (in != null)
                            {
                                resolvedPlaybookPath = companionYamlPath;
                            }
                        }
                        catch (final Exception e)
                        {
                            // Fall back to original path
                        }
                    }
                }
            }

            Playbook playbook;
            if (resolvedPlaybookPath != null && resolvedPlaybookPath.startsWith("playbooks/integration/programmatic/"))
            {
                try
                {
                    playbook = parser.parse(resolvedPlaybookPath, manager);
                }
                catch (final Exception e)
                {
                    playbook = Playbook.builder().build();
                }
            }
            else
            {
                playbook = parser.parse(resolvedPlaybookPath, manager);
            }

            final String companionYamlPath = (playbookPath != null && !playbookPath.isEmpty()) ? playbookPath : resolvedPlaybookPath;
            String yamlHash = null;
            if (this.mode.isReplay())
            {
                yamlHash = computeResourceSha256(manager, companionYamlPath);
                if (yamlHash == null && companionYamlPath.endsWith(".json"))
                {
                    final String fallbackYamlPath = companionYamlPath.substring(0, companionYamlPath.length() - 5) + ".yaml";
                    yamlHash = computeResourceSha256(manager, fallbackYamlPath);
                }
            }

            final List<PlaybookStep> playbookSteps = new ArrayList<>(playbook.getSteps());
            if (resolvedPlaybookPath != null)
            {
                executionContext.getTransientData().put("playbookFile", resolvedPlaybookPath);
            }

            if (this.mode.isRecording() && !playbookSteps.isEmpty())
            {
                final String freshYamlHash = computeResourceSha256(manager, companionYamlPath);
                if (freshYamlHash != null)
                {
                    for (final PlaybookStep step : playbookSteps)
                    {
                        step.setSourceYamlHash(freshYamlHash);
                    }
                }
            }

            if (yamlHash != null && !playbookSteps.isEmpty())
            {
                String recordedHash = null;
                for (final PlaybookStep step : playbookSteps)
                {
                    if (step.getSourceYamlHash() != null && !step.getSourceYamlHash().isEmpty())
                    {
                        recordedHash = step.getSourceYamlHash();
                        break;
                    }
                }
                if (recordedHash != null && !recordedHash.equalsIgnoreCase(yamlHash))
                {
                    final String warning = String.format(
                        "Source YAML file '%s' (SHA-256: %s...) has been modified since recording '%s' (SHA-256: %s...) was generated.",
                        companionYamlPath,
                        yamlHash.substring(0, Math.min(8, yamlHash.length())),
                        resolvedPlaybookPath,
                        recordedHash.substring(0, Math.min(8, recordedHash.length()))
                    );
                    org.slf4j.LoggerFactory.getLogger(NeodymiumAiRunner.class).warn("⚠️ [YAML Coherence Mismatch] {}", warning);
                    executionContext.getTransientData().put(ExecutionContext.KEY_YAML_MISMATCH_WARNING, warning);

                    @SuppressWarnings("unchecked")
                    List<String> warningsList = (List<String>) executionContext.getTransientData().get(ExecutionContext.KEY_EXECUTION_WARNINGS);
                    if (warningsList == null)
                    {
                        warningsList = new ArrayList<>();
                        executionContext.getTransientData().put(ExecutionContext.KEY_EXECUTION_WARNINGS, warningsList);
                    }
                    warningsList.add(warning);
                }
            }

            final List<PlaybookStep> flatSteps = new ArrayList<>();
            flattenSteps(playbookSteps, flatSteps);
            executionContext.getTransientData().put("playbook.flatSteps", flatSteps);
            executionContext.getTransientData().put("playbook.steps", playbookSteps);

            if (testClass != null)
            {
                final List<PlaybookStep> beforeFlat = new ArrayList<>();
                final List<PlaybookStep> afterFlat = new ArrayList<>();
                for (final Method m : testClass.getDeclaredMethods())
                {
                    if (m.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class) && m.isAnnotationPresent(AiPlaybook.class))
                    {
                        final String beforePbPath = m.getAnnotation(AiPlaybook.class).value();
                        try
                        {
                            final Playbook beforePb = parser.parse(beforePbPath, manager);
                            flattenSteps(beforePb.getSteps(), beforeFlat);
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                    else if (m.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class) && m.isAnnotationPresent(AiPlaybook.class))
                    {
                        final String afterPbPath = m.getAnnotation(AiPlaybook.class).value();
                        try
                        {
                            final Playbook afterPb = parser.parse(afterPbPath, manager);
                            flattenSteps(afterPb.getSteps(), afterFlat);
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                }
                if (!beforeFlat.isEmpty())
                {
                    executionContext.getTransientData().put("playbook.beforeSteps", beforeFlat);
                }
                if (!afterFlat.isEmpty())
                {
                    executionContext.getTransientData().put("playbook.afterSteps", afterFlat);
                }
            }

            if (playbook.getPromptAddons() != null)
            {
                executionContext.getTransientData().put("playbook.promptAddons", playbook.getPromptAddons());
            }

            executionContext.getTransientData().put("playbook.resolvedPath", resolvedPlaybookPath);
            executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_PROMPT, new ActionExtractionPrompt());
            executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_MODEL, Neodymium.aiConfiguration().aiModel());
            executionContext.getTransientData().put(ExecutionContext.KEY_RESOURCE_MANAGER, manager);
            executionContext.getTransientData().put(ExecutionContext.KEY_PLAYBOOK_PARSER, parser);
            executionContext.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, mode);
            executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_DATASET_LABEL, this.datasetId != null ? this.datasetId : "default");
            executionContext.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, org.neodymium.ai.model.ContextLevel.MINIMAL);
            executionContext.getTransientData().put(ExecutionContext.KEY_TOTAL_LLM_CALLS, 0);
            executionContext.getTransientData().put(ExecutionContext.KEY_TOTAL_REPLAYS, 0);
            executionContext.getTransientData().put("junit.testInstance", context.getRequiredTestInstance());

            if (resolvedPlaybookPath != null && resolvedPlaybookPath.contains("/programmatic/"))
            {
                executionContext.getTransientData().put("playbook.programmatic", true);
            }

            this.recordingPath = computeRecordingPath(playbookPath, testClass, method, this.datasetId, browserProfile, recMethod, recFileName, recDir);
            if (this.recordingPath != null)
            {
                final org.neodymium.ai.recorder.PlaybookRecorder recorder = new org.neodymium.ai.recorder.PlaybookRecorder(manager, this.recordingPath, playbookSteps, this.mode);
                eventBus.registerListener(recorder);
            }

            executionContext.getTransientData().put("playbook.mainSteps", playbookSteps);

            final ExtensionContext.Store store = context.getStore(ExtensionContext.Namespace.create(context.getRequiredTestInstance()));
            store.put(AiSession.class, this.session);
            store.put(ExecutionContext.class, executionContext);
        }

        @Override
        public void interceptBeforeEachMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext
        ) throws Throwable
        {
            invocation.proceed();
            runPlaybookForMethod(invocationContext.getExecutable());
        }

        @Override
        public void interceptTestMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext
        ) throws Throwable
        {
            runMainTestPlaybook();
            invocation.proceed();
        }

        @Override
        public void interceptTestTemplateMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext
        ) throws Throwable
        {
            runMainTestPlaybook();
            invocation.proceed();
        }

        @Override
        public void interceptAfterEachMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext
        ) throws Throwable
        {
            try
            {
                runPlaybookForMethod(invocationContext.getExecutable());
            }
            finally
            {
                invocation.proceed();
            }
        }

        private void runPlaybookForMethod(final Method method) throws Exception
        {
            if (method != null && method.isAnnotationPresent(AiPlaybook.class) && this.session != null)
            {
                final ExecutionContext execCtx = this.session.getExecutionContext();
                if (execCtx != null && Boolean.TRUE.equals(execCtx.getTransientData().get("playbook.programmatic")))
                {
                    return;
                }
                final AiPlaybook playbookAnnot = method.getAnnotation(AiPlaybook.class);
                final String path = playbookAnnot.value();

                final PlaybookParser parser = (PlaybookParser) execCtx.getTransientData().get(ExecutionContext.KEY_PLAYBOOK_PARSER);
                final PlaybookResourceManager manager = (PlaybookResourceManager) execCtx.getTransientData().get(ExecutionContext.KEY_RESOURCE_MANAGER);

                if (parser != null && manager != null)
                {
                    final Playbook playbook = parser.parse(path, manager);
                    final List<PlaybookStep> playbookSteps = new ArrayList<>(playbook.getSteps());
                    final List<PlaybookStep> flat = new ArrayList<>();
                    flattenSteps(playbookSteps, flat);

                    if (method.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class))
                    {
                        execCtx.getTransientData().put("playbook.beforeSteps", flat);
                    }
                    else if (method.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class))
                    {
                        execCtx.getTransientData().put("playbook.afterSteps", flat);
                    }

                    for (int i = playbookSteps.size() - 1; i >= 0; i--)
                    {
                        execCtx.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(playbookSteps.get(i), this.session, execCtx));
                    }

                    final StateMachineRunner runner = new StateMachineRunner(this.session);
                    runner.run();
                }
            }
        }

        private void runMainTestPlaybook() throws Exception
        {
            if (this.session != null && !this.session.getExecutionContext().getTransientData().containsKey("playbook.programmatic"))
            {
                @SuppressWarnings("unchecked")
                final List<PlaybookStep> playbookSteps = (List<PlaybookStep>) this.session.getExecutionContext().getTransientData().remove("playbook.mainSteps");
                if (playbookSteps != null && !playbookSteps.isEmpty())
                {
                    final ExecutionContext execCtx = this.session.getExecutionContext();
                    for (int i = playbookSteps.size() - 1; i >= 0; i--)
                    {
                        execCtx.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(playbookSteps.get(i), this.session, execCtx));
                    }
                }
                final StateMachineRunner runner = new StateMachineRunner(this.session);
                runner.run();
            }
        }

        @Override
        public void afterEach(final ExtensionContext context) throws Exception
        {
            try
            {
                if (this.session != null)
                {
                    final ExecutionContext execCtx = this.session.getExecutionContext();
                    if (execCtx != null && execCtx.getTransientData().get("interactiveConsoleServer") instanceof InteractiveConsoleServer consoleServer)
                    {
                        try
                        {
                            consoleServer.stop();
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                    this.session.close();
                }
            }
            finally
            {
                if (context.getExecutionException().isPresent() && this.mode.isRecording() && this.recordingPath != null && this.resourceManager != null)
                {
                    try
                    {
                        this.resourceManager.delete(this.recordingPath);
                        final Method m = context.getTestMethod().orElse(null);
                        org.slf4j.LoggerFactory.getLogger(NeodymiumAiRunner.class).warn(
                            "⚠️ Test method '{}' failed. Voided invalid recorded playbook at {}",
                            m != null ? m.getName() : "unknown",
                            this.recordingPath
                        );
                    }
                    catch (final Exception e)
                    {
                        org.slf4j.LoggerFactory.getLogger(NeodymiumAiRunner.class).warn("Failed to void recorded playbook at {}", this.recordingPath, e);
                    }
                }

                final Method method = context.getTestMethod().orElse(null);
                final Class<?> testClass = context.getTestClass().orElse(null);
                if (method != null && method.isAnnotationPresent(AiLlmCache.class) && (testClass == null || !testClass.isAnnotationPresent(AiLlmCache.class)))
                {
                    InMemoryLlmCache.clear();
                }
            }
        }

        @Override
        public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
        {
            final Class<?> type = parameterContext.getParameter().getType();
            return type == AiSession.class || type == ExecutionContext.class;
        }

        @Override
        public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
        {
            final Class<?> type = parameterContext.getParameter().getType();
            if (type == AiSession.class)
            {
                return this.session;
            }
            if (type == ExecutionContext.class && this.session != null)
            {
                return this.session.getExecutionContext();
            }
            return null;
        }
    }

    private static String computeResourceSha256(final PlaybookResourceManager manager, final String path)
    {
        if (manager == null || path == null || path.isEmpty())
        {
            return null;
        }
        try (final java.io.InputStream in = manager.read(path))
        {
            if (in == null)
            {
                return null;
            }
            final byte[] bytes = in.readAllBytes();
            final java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(bytes);
            final StringBuilder hex = new StringBuilder();
            for (final byte b : hash)
            {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
        catch (final Exception e)
        {
            return null;
        }
    }

    private static void flattenSteps(final List<PlaybookStep> source, final List<PlaybookStep> target)
    {
        for (final PlaybookStep step : source)
        {
            if (step.isComposite())
            {
                flattenSteps(step.getSubSteps(), target);
            }
            else
            {
                target.add(step);
            }
        }
    }

    /**
     * A hybrid resource manager that intercepts inline playbooks stored in memory,
     * falling back to classpath/filesystem resolution for standard file paths.
     *
     * @author AI-generated: Gemini 3.5 Flash
     * @author Xceptance GmbH 2026
     */
    public static final class HybridResourceManager implements PlaybookResourceManager
    {
        private final PlaybookResourceManager delegate;

        /**
         * Constructs a HybridResourceManager wrapping a delegate.
         *
         * @param delegate the fallback resource manager delegate
         */
        public HybridResourceManager(final PlaybookResourceManager delegate)
        {
            this.delegate = delegate;
        }

        /**
         * Reads the raw resource stream. Intercepts inline keys.
         *
         * @param identifier the resource key
         * @return the raw stream
         * @throws IOException on resolution failure
         */
        @Override
        public InputStream read(final String identifier) throws IOException
        {
            if (identifier != null)
            {
                if (INLINE_PLAYBOOKS.containsKey(identifier))
                {
                    return new ByteArrayInputStream(INLINE_PLAYBOOKS.get(identifier).getBytes(StandardCharsets.UTF_8));
                }
                if (identifier.contains("/programmatic/") && (identifier.endsWith(".yaml") || identifier.endsWith(".yml")))
                {
                    return new ByteArrayInputStream("steps: []".getBytes(StandardCharsets.UTF_8));
                }
            }
            return this.delegate.read(identifier);
        }

        /**
         * Writes raw content. Redirects inline keys to memory.
         *
         * @param identifier the resource key
         * @param content the content payload
         * @throws IOException on I/O failure
         */
        @Override
        public void write(final String identifier, final String content) throws IOException
        {
            if (identifier != null && identifier.startsWith("inline-"))
            {
                INLINE_PLAYBOOKS.put(identifier, content);
                return;
            }
            this.delegate.write(identifier, content);
        }

        /**
         * Deletes a resource. Removes from memory if inline.
         *
         * @param identifier the resource key
         * @throws IOException on I/O failure
         */
        @Override
        public void delete(final String identifier) throws IOException
        {
            if (identifier != null && INLINE_PLAYBOOKS.containsKey(identifier))
            {
                INLINE_PLAYBOOKS.remove(identifier);
                return;
            }
            this.delegate.delete(identifier);
        }

        /**
         * Resolves relative inclusions against parent resources.
         *
         * @param parentIdentifier the parent resource key
         * @param relativePath the relative path to resolve
         * @return the resolved normalized path
         */
        @Override
        public String resolveInclude(final String parentIdentifier, final String relativePath)
        {
            return this.delegate.resolveInclude(parentIdentifier, relativePath);
        }
    }

    private static String extractParentDir(final String path)
    {
        if (path == null)
        {
            return "";
        }
        final int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0)
        {
            return path.substring(0, lastSlash + 1);
        }
        return "";
    }

    private static String computeRecordingPath(
        final String playbookPath,
        final Class<?> testClass,
        final Method method,
        final String datasetId,
        final String browserProfile
    )
    {
        return computeRecordingPath(playbookPath, testClass, method, datasetId, browserProfile, null, null, null);
    }

    private static String computeRecordingPath(
        final String playbookPath,
        final Class<?> testClass,
        final Method method,
        final String datasetId,
        final String browserProfile,
        final String recordingMethod,
        final String recordingFileName,
        final String recordingDirectory
    )
    {
        final String recDir = (recordingDirectory != null && !recordingDirectory.trim().isEmpty())
            ? recordingDirectory.trim()
            : org.neodymium.ai.config.AiConfiguration.getInstance().playbookRecordingDirectory();

        if (recordingFileName != null && !recordingFileName.trim().isEmpty())
        {
            String name = recordingFileName.trim();
            if (name.toLowerCase().endsWith(".json"))
            {
                name = name.substring(0, name.length() - 5);
            }

            final StringBuilder sb = new StringBuilder();
            if (name.startsWith("/"))
            {
                sb.append(name);
            }
            else
            {
                final String parentDir = recDir != null
                    ? (recDir.endsWith("/") ? recDir : recDir + "/")
                    : extractParentDir(playbookPath);
                sb.append(parentDir).append(name);
            }

            if (datasetId != null && !datasetId.isEmpty())
            {
                sb.append("_").append(datasetId);
            }

            if (browserProfile != null && !browserProfile.isEmpty())
            {
                sb.append("_").append(browserProfile);
            }

            sb.append(".json");
            return sb.toString();
        }

        final String parentDir = recDir != null
            ? (recDir.endsWith("/") ? recDir : recDir + "/")
            : extractParentDir(playbookPath);
        final StringBuilder sb = new StringBuilder();
        sb.append(parentDir);

        if (testClass != null)
        {
            sb.append(testClass.getSimpleName());
        }

        final String targetMethodName;
        if (recordingMethod != null && !recordingMethod.trim().isEmpty())
        {
            targetMethodName = recordingMethod.trim();
        }
        else if (method != null)
        {
            targetMethodName = method.getName();
        }
        else
        {
            targetMethodName = null;
        }

        if (targetMethodName != null && !targetMethodName.isEmpty())
        {
            if (testClass != null)
            {
                sb.append("_");
            }
            sb.append(targetMethodName);
        }

        if (datasetId != null && !datasetId.isEmpty())
        {
            sb.append("_").append(datasetId);
        }

        if (browserProfile != null && !browserProfile.isEmpty())
        {
            sb.append("_").append(browserProfile);
        }

        sb.append(".json");
        return sb.toString();
    }

    private static String computeLegacyRecordingPath(final String playbookPath, final String datasetId)
    {
        if (playbookPath == null)
        {
            return null;
        }
        String path = playbookPath;
        if (datasetId != null && !datasetId.isEmpty())
        {
            if (path.endsWith(".yaml"))
            {
                return path.substring(0, path.length() - 5) + "_" + datasetId + ".json";
            }
            else if (path.endsWith(".yml"))
            {
                return path.substring(0, path.length() - 4) + "_" + datasetId + ".json";
            }
        }
        if (path.endsWith(".yaml"))
        {
            return path.substring(0, path.length() - 5) + ".json";
        }
        else if (path.endsWith(".yml"))
        {
            return path.substring(0, path.length() - 4) + ".json";
        }
        return path + ".json";
    }
}
