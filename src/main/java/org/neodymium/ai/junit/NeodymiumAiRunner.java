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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
import org.neodymium.ai.prompt.ActionExtractionPrompt;
import org.neodymium.ai.resources.ClasspathResourceManager;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.neodymium.ai.playbook.PlaybookParser;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.ai.session.AiSession;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.Browsers;
import org.neodymium.common.browser.BrowserMethodData;
import org.neodymium.common.browser.BrowserRunner;
import org.neodymium.util.Neodymium;

/**
 * JUnit 5 {@link TestTemplateInvocationContextProvider} implementation for Neodymium AI tests.
 * Resolves playbooks, filters datasets, maps sequential executions, and manages lifecycle context.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class NeodymiumAiRunner implements TestTemplateInvocationContextProvider
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
            || method.isAnnotationPresent(AiPlaybook.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(final ExtensionContext context)
    {
        final Method method = context.getRequiredTestMethod();
        final Class<?> testClass = context.getRequiredTestClass();

        // 1. Resolve playbook paths
        final List<String> playbookPaths = new ArrayList<>();
        final AiPlaybook methodPlaybook = method.getAnnotation(AiPlaybook.class);
        final AiPlaybook classPlaybook = testClass.getAnnotation(AiPlaybook.class);
        if (methodPlaybook != null && !methodPlaybook.value().isEmpty())
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
            modes.add(new AiConfiguration().getExecutionMode());
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

        final List<String> resolvedPaths = new ArrayList<>();
        for (final String path : playbookPaths)
        {
            if (path != null && !path.isEmpty())
            {
                if ("programmatic".equalsIgnoreCase(path))
                {
                    final String name = (methodPlaybook != null && !methodPlaybook.name().isEmpty()) ? methodPlaybook.name()
                                      : (classPlaybook != null && !classPlaybook.name().isEmpty()) ? classPlaybook.name()
                                      : testClass.getSimpleName() + "_" + method.getName();
                    final String virtualPath = "playbooks/integration/programmatic/" + name + ".yaml";
                    resolvedPaths.add(virtualPath);
                }
                else if (path.startsWith("inline:"))
                {
                    final String yamlContent = path.substring("inline:".length()).trim();
                    final String key = "inline-" + Math.abs(yamlContent.hashCode()) + ".yaml";
                    INLINE_PLAYBOOKS.put(key, yamlContent);
                    resolvedPaths.add(key);
                }
                else if (path.startsWith("/"))
                {
                    // Absolute path from classpath root (leading slash stripped)
                    resolvedPaths.add(path.substring(1));
                }
                else
                {
                    // Relative path from test class package
                    final String packagePath = testClass.getPackageName().replace('.', '/');
                    if (path.startsWith(packagePath + "/"))
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

        final List<TestTemplateInvocationContext> invocationContexts = new ArrayList<>();
        final PlaybookParser parser = new YamlPlaybookParser();
        final PlaybookResourceManager manager = new HybridResourceManager(new ClasspathResourceManager());

        for (final String playbookPath : resolvedPaths)
        {
            Playbook playbook;
            try
            {
                playbook = parser.parse(playbookPath, manager);
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
                for (final Map<String, SessionData.DataEntry> ds : allDataSets)
                {
                    final String dsId = getDataSetId(ds);
                    if (shouldIncludeDataSet(dsId, datasetFilters))
                    {
                        filteredDataSets.add(ds);
                    }
                }
            }

            // Create template invocation context for Cartesian product of datasets and execution modes
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
                            return String.format("[%d] playbook=%s, dataset=%s, mode=%s", invocationIndex, name, datasetLabel, mode);
                        }

                        @Override
                        public List<Extension> getAdditionalExtensions()
                        {
                            return Collections.singletonList(new AiInvocationExtension(playbookPath, dataset, mode, dsId));
                        }
                    });
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
                if (val.matches(pattern) || java.util.regex.Pattern.compile(pattern).matcher(val).find())
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
        private AiSession session;

        public AiInvocationExtension(
            final String playbookPath,
            final Map<String, SessionData.DataEntry> dataset,
            final ExecutionMode mode,
            final String datasetId
        )
        {
            this.playbookPath = playbookPath;
            this.dataset = dataset;
            this.mode = mode;
            this.datasetId = datasetId;
        }

        @Override
        public void beforeEach(final ExtensionContext context) throws Exception
        {
            // Set test name dynamically in the Neodymium context
            if (context.getRequiredTestMethod() != null && context.getRequiredTestClass() != null)
            {
                Neodymium.setTestName(
                    context.getRequiredTestClass().getSimpleName() + "." + context.getRequiredTestMethod().getName()
                );
            }

            // Resolve browser profile name from annotations if not already set
            if (Neodymium.getBrowserProfileName() == null)
            {
                final String annotBrowser;
                final Method method = context.getRequiredTestMethod();
                if (method.isAnnotationPresent(Browser.class))
                {
                    annotBrowser = method.getAnnotation(Browser.class).value();
                }
                else if (method.isAnnotationPresent(Browsers.class))
                {
                    final Browser[] bs = method.getAnnotation(Browsers.class).value();
                    annotBrowser = bs.length > 0 ? bs[0].value() : null;
                }
                else
                {
                    final Class<?> testClass = context.getRequiredTestClass();
                    if (testClass.isAnnotationPresent(Browser.class))
                    {
                        annotBrowser = testClass.getAnnotation(Browser.class).value();
                    }
                    else if (testClass.isAnnotationPresent(Browsers.class))
                    {
                        final Browser[] bs = testClass.getAnnotation(Browsers.class).value();
                        annotBrowser = bs.length > 0 ? bs[0].value() : null;
                    }
                    else
                    {
                        annotBrowser = null;
                    }
                }
                if (annotBrowser != null)
                {
                    Neodymium.setBrowserProfileName(annotBrowser);
                }
            }

            // Automatically reset/clean the browser state at the start of a new AI session
            try
            {
                com.codeborne.selenide.Selenide.closeWebDriver();
            }
            catch (final Exception e)
            {
                // Ignore driver closure errors
            }

            final String profileName = Neodymium.getBrowserProfileName();
            if (profileName != null)
            {
                final BrowserRunner runner = new BrowserRunner();
                runner.setUpTest(
                    new BrowserMethodData(profileName, false, false, true, true, Collections.emptyList()),
                    Neodymium.getTestName());
                if (Neodymium.getWebDriverStateContainer() != null && Neodymium.getWebDriverStateContainer().getWebDriver() != null)
                {
                    com.codeborne.selenide.WebDriverRunner.setWebDriver(Neodymium.getWebDriverStateContainer().getWebDriver());
                }
            }

            // Automatically detect mock integration test package and apply thread-local overrides
            if (context.getRequiredTestClass() != null)
            {
                final String fqcn = context.getRequiredTestClass().getName();
                if (fqcn.contains(".integration.mock."))
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
            final AiConfiguration config = new AiConfiguration();
            LlmRegistry.bootstrap(registry, config);

            final ExecutionEventBus eventBus = new ExecutionEventBus();
            final SelenideTargetExecutor executor = new SelenideTargetExecutor();

            this.session = AiSession.mock(sessionData, registry, eventBus, executor);
            final ExecutionContext executionContext = this.session.getExecutionContext();
            executor.setExecutionContext(executionContext);
            
            final PlaybookParser parser = new YamlPlaybookParser();
            final PlaybookResourceManager manager = new HybridResourceManager(new ClasspathResourceManager());
            
            final Class<?> testClass = context.getRequiredTestClass();
            final Method method = context.getRequiredTestMethod();
            final String browserProfile = Neodymium.getBrowserProfileName();

            String recMethod = null;
            String recFileName = null;
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
            else if (classPb != null && !classPb.recordingFileName().isEmpty())
            {
                recFileName = classPb.recordingFileName();
            }

            String resolvedPlaybookPath = playbookPath;
            if (this.mode.isReplay())
            {
                // Replay modes: try candidate paths using recordingMethod / recordingFileName, then method defaults
                final List<String> candidatePaths = new ArrayList<>();
                candidatePaths.add(computeRecordingPath(playbookPath, testClass, method, this.datasetId, browserProfile, recMethod, recFileName));
                candidatePaths.add(computeRecordingPath(playbookPath, testClass, method, this.datasetId, null, recMethod, recFileName));
                candidatePaths.add(computeRecordingPath(playbookPath, testClass, null, this.datasetId, browserProfile, null, recFileName));
                candidatePaths.add(computeRecordingPath(playbookPath, testClass, null, this.datasetId, null, null, recFileName));
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
                    resolvedPlaybookPath = companionJsonPath;
                }
                else
                {
                    final String msg = String.format(
                        "Replay mode '%s' failed for test '%s.%s': No recorded companion JSON file found. Candidate paths searched:\n  - %s",
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

            final Playbook playbook = parser.parse(resolvedPlaybookPath, manager);
            final List<PlaybookStep> playbookSteps = new ArrayList<>(playbook.getSteps());
            final List<PlaybookStep> flatSteps = new ArrayList<>();
            flattenSteps(playbookSteps, flatSteps);
            executionContext.getTransientData().put("playbook.flatSteps", flatSteps);
            executionContext.getTransientData().put("playbook.steps", playbookSteps);
            if (playbook.getSystemPromptAddons() != null)
            {
                executionContext.getTransientData().put("playbook.systemPromptAddons", playbook.getSystemPromptAddons());
            }

            executionContext.getTransientData().put("playbook.resolvedPath", resolvedPlaybookPath);
            executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_PROMPT, new ActionExtractionPrompt());
            executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_MODEL, Neodymium.aiConfiguration().aiModel());
            executionContext.getTransientData().put(ExecutionContext.KEY_RESOURCE_MANAGER, manager);
            executionContext.getTransientData().put(ExecutionContext.KEY_PLAYBOOK_PARSER, parser);
            executionContext.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, mode);
            executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_DATASET_LABEL, this.datasetId != null ? this.datasetId : "default");
            executionContext.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, org.neodymium.ai.executor.selenide.ContextLevel.LEAN);
            executionContext.getTransientData().put("junit.testInstance", context.getRequiredTestInstance());

            if (resolvedPlaybookPath != null && resolvedPlaybookPath.contains("/programmatic/"))
            {
                executionContext.getTransientData().put("playbook.programmatic", true);
            }

            if (this.mode.isRecording())
            {
                final String recordingPath = computeRecordingPath(playbookPath, testClass, method, this.datasetId, browserProfile, recMethod, recFileName);

                if (this.mode == org.neodymium.ai.config.ExecutionMode.FORCE_RECORDING)
                {
                    try
                    {
                        manager.delete(recordingPath);
                    }
                    catch (final Exception e)
                    {
                        // Ignore deletion failures if the file doesn't exist
                    }
                }

                final org.neodymium.ai.recorder.PlaybookRecorder recorder = new org.neodymium.ai.recorder.PlaybookRecorder(manager, recordingPath, playbookSteps);
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
            runPlaybookForMethod(invocationContext.getExecutable());
            invocation.proceed();
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
                final AiPlaybook playbookAnnot = method.getAnnotation(AiPlaybook.class);
                final String path = playbookAnnot.value();

                final ExecutionContext execCtx = this.session.getExecutionContext();
                final PlaybookParser parser = (PlaybookParser) execCtx.getTransientData().get(ExecutionContext.KEY_PLAYBOOK_PARSER);
                final PlaybookResourceManager manager = (PlaybookResourceManager) execCtx.getTransientData().get(ExecutionContext.KEY_RESOURCE_MANAGER);

                if (parser != null && manager != null)
                {
                    final Playbook playbook = parser.parse(path, manager);
                    final List<PlaybookStep> playbookSteps = new ArrayList<>(playbook.getSteps());

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
            if (this.session != null)
            {
                this.session.close();
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
        return computeRecordingPath(playbookPath, testClass, method, datasetId, browserProfile, null, null);
    }

    private static String computeRecordingPath(
        final String playbookPath,
        final Class<?> testClass,
        final Method method,
        final String datasetId,
        final String browserProfile,
        final String recordingMethod,
        final String recordingFileName
    )
    {
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
                final String parentDir = extractParentDir(playbookPath);
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

        final String parentDir = extractParentDir(playbookPath);
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
