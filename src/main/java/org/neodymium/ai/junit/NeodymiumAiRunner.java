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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
     * Constructs a default NeodymiumAiRunner.
     */
    public NeodymiumAiRunner()
    {
    }

    @Override
    public boolean supportsTestTemplate(final ExtensionContext context)
    {
        return context.getTestMethod().isPresent()
            && context.getTestMethod().get().isAnnotationPresent(AiPlaybook.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(final ExtensionContext context)
    {
        final Method method = context.getRequiredTestMethod();
        final Class<?> testClass = context.getRequiredTestClass();

        // 1. Resolve playbook paths
        final List<String> playbookPaths = new ArrayList<>();
        final AiPlaybook methodPlaybook = method.getAnnotation(AiPlaybook.class);
        if (methodPlaybook != null && !methodPlaybook.value().isEmpty())
        {
            playbookPaths.add(methodPlaybook.value());
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

        final List<TestTemplateInvocationContext> invocationContexts = new ArrayList<>();
        final PlaybookParser parser = new YamlPlaybookParser();
        final PlaybookResourceManager manager = new ClasspathResourceManager();

        for (final String playbookPath : playbookPaths)
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
                com.xceptance.neodymium.util.Neodymium.setTestName(
                    context.getRequiredTestClass().getSimpleName() + "." + context.getRequiredTestMethod().getName()
                );
            }

            // Automatically reset/clean the browser state at the start of a new AI session
            final String profileName = com.xceptance.neodymium.util.Neodymium.getBrowserProfileName();
            if (profileName != null)
            {
                final com.xceptance.neodymium.common.browser.BrowserRunner runner = new com.xceptance.neodymium.common.browser.BrowserRunner();
                runner.teardown(false, true,
                    new com.xceptance.neodymium.common.browser.BrowserMethodData(profileName, false, false, true, true, java.util.Collections.emptyList()),
                    com.xceptance.neodymium.util.Neodymium.getWebDriverStateContainer());
                try
                {
                    Thread.sleep(500);
                }
                catch (final InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                runner.setUpTest(
                    new com.xceptance.neodymium.common.browser.BrowserMethodData(profileName, false, false, true, true, java.util.Collections.emptyList()),
                    com.xceptance.neodymium.util.Neodymium.getTestName());
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
            final PlaybookResourceManager manager = new ClasspathResourceManager();
            
            String resolvedPlaybookPath = playbookPath;
            if (this.mode.isReplay())
            {
                // Replay modes: automatically prefer companion JSON file if present
                String companionJsonPath = null;
                if (this.datasetId != null && !this.datasetId.isEmpty())
                {
                    String suffixed = playbookPath;
                    if (suffixed.endsWith(".yaml"))
                    {
                        suffixed = suffixed.substring(0, suffixed.length() - 5) + "_" + this.datasetId + ".json";
                    }
                    else if (suffixed.endsWith(".yml"))
                    {
                        suffixed = suffixed.substring(0, suffixed.length() - 4) + "_" + this.datasetId + ".json";
                    }
                    try (final java.io.InputStream in = manager.read(suffixed))
                    {
                        if (in != null)
                        {
                            companionJsonPath = suffixed;
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }

                if (companionJsonPath == null)
                {
                    String standard = playbookPath;
                    if (standard.endsWith(".yaml"))
                    {
                        standard = standard.substring(0, standard.length() - 5) + ".json";
                    }
                    else if (standard.endsWith(".yml"))
                    {
                        standard = standard.substring(0, standard.length() - 4) + ".json";
                    }
                    companionJsonPath = standard;
                }

                try (final java.io.InputStream in = manager.read(companionJsonPath))
                {
                    if (in != null)
                    {
                        resolvedPlaybookPath = companionJsonPath;
                    }
                }
                catch (final Exception e)
                {
                    // Fall back to original path
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
            final List<PlaybookStep> playbookSteps = playbook.getSteps();
            final List<PlaybookStep> flatSteps = new ArrayList<>();
            flattenSteps(playbookSteps, flatSteps);
            executionContext.getTransientData().put("playbook.flatSteps", flatSteps);

            executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_PROMPT, new ActionExtractionPrompt());
            executionContext.getTransientData().put(ExecutionContext.KEY_RESOURCE_MANAGER, manager);
            executionContext.getTransientData().put(ExecutionContext.KEY_PLAYBOOK_PARSER, parser);
            executionContext.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, mode);
            executionContext.getTransientData().put(ExecutionContext.KEY_ACTIVE_DATASET_LABEL, this.datasetId != null ? this.datasetId : "default");
            executionContext.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, org.neodymium.ai.executor.selenide.ContextLevel.LEAN);
            executionContext.getTransientData().put("junit.testInstance", context.getRequiredTestInstance());

            if (this.mode.isRecording())
            {
                String recordingPath = playbookPath;
                if (this.datasetId != null && !this.datasetId.isEmpty())
                {
                    if (recordingPath.endsWith(".yaml"))
                    {
                        recordingPath = recordingPath.substring(0, recordingPath.length() - 5) + "_" + this.datasetId + ".json";
                    }
                    else if (recordingPath.endsWith(".yml"))
                    {
                        recordingPath = recordingPath.substring(0, recordingPath.length() - 4) + "_" + this.datasetId + ".json";
                    }
                }
                else
                {
                    if (recordingPath.endsWith(".yaml"))
                    {
                        recordingPath = recordingPath.substring(0, recordingPath.length() - 5) + ".json";
                    }
                    else if (recordingPath.endsWith(".yml"))
                    {
                        recordingPath = recordingPath.substring(0, recordingPath.length() - 4) + ".json";
                    }
                }

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

            for (int i = playbookSteps.size() - 1; i >= 0; i--)
            {
                executionContext.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(playbookSteps.get(i), this.session, executionContext));
            }

            final ExtensionContext.Store store = context.getStore(ExtensionContext.Namespace.create(context.getRequiredTestInstance()));
            store.put(AiSession.class, this.session);
            store.put(ExecutionContext.class, executionContext);
        }

        @Override
        public void interceptTestTemplateMethod(
            final Invocation<Void> invocation,
            final ReflectiveInvocationContext<Method> invocationContext,
            final ExtensionContext extensionContext
        ) throws Throwable
        {
            if (this.session != null)
            {
                final StateMachineRunner runner = new StateMachineRunner(this.session);
                runner.run();
            }
            invocation.proceed();
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
}
