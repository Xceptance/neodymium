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

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.event.ExecutionListener;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.recorder.PlaybookRecorder;
import org.neodymium.ai.report.PreliminaryReportListener;
import org.neodymium.ai.report.TestExecutionReport;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.resources.InMemoryResourceManager;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.telemetry.TokenBudgetGuard;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.WebDriverCache;
import org.neodymium.common.browser.WebDriverStateContainer;
import org.neodymium.common.recording.FilmTestExecution;
import org.neodymium.util.Neodymium;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WrapsDriver;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit test suite for {@link NeodymiumAiRunner} and associated JUnit 5 annotations.
 * Verifies annotation processing and metadata extraction for {@link NeodymiumAiTest} and {@link AiPlaybook}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class NeodymiumAiRunnerTest
{
    /**
     * Sample test class decorated with {@link NeodymiumAiTest} and {@link AiPlaybook} for reflection verification.
     */
    @NeodymiumAiTest("inline:name: sample\nsteps:\n  - step: Click search button\n")
    public static class SampleTestClass
    {
        /**
         * Sample test method annotated with {@link AiPlaybook}.
         */
        @Test
        @AiPlaybook("inline:name: sample\nsteps:\n  - step: Click search button\n")
        public void sampleTestMethod()
        {
        }
    }

    /**
     * Goal: Verifies that reflection correctly detects the presence of {@link NeodymiumAiTest} on the test class
     * and {@link AiPlaybook} on individual test methods, extracting inline YAML playbook definitions.
     */
    @Test
    public void testAnnotationPresenceOnSampleTestClass() throws Exception
    {
        // 1. Verify class-level @NeodymiumAiTest annotation presence and value
        final NeodymiumAiTest aiTest = SampleTestClass.class.getAnnotation(NeodymiumAiTest.class);
        Assertions.assertNotNull(aiTest);
        Assertions.assertEquals(1, aiTest.value().length);

        // 2. Verify method-level @AiPlaybook annotation presence and inline prefix
        final AiPlaybook playbook = SampleTestClass.class.getMethod("sampleTestMethod").getAnnotation(AiPlaybook.class);
        Assertions.assertNotNull(playbook);
        Assertions.assertTrue(playbook.value().startsWith("inline:"));
    }

    /**
     * Sample test class with explicit recordingMethod and recordingFileName configuration.
     */
    public static class RecordingAnnotationTestClass
    {
        @Test
        @AiPlaybook(value = "/playbooks/sample.yaml", recordingMethod = "testSourceLive")
        public void sampleReplayMethod()
        {
        }

        @Test
        @AiPlaybook(value = "/playbooks/sample.yaml", recordingFileName = "custom-baseline-file")
        public void sampleCustomFileNameMethod()
        {
        }
    }

    /**
     * Goal: Verifies that recordingMethod and recordingFileName attributes on @AiPlaybook are correctly read.
     */
    @Test
    public void testRecordingMethodAndFileNameAnnotationAttributes() throws Exception
    {
        final AiPlaybook replayPb = RecordingAnnotationTestClass.class.getMethod("sampleReplayMethod").getAnnotation(AiPlaybook.class);
        Assertions.assertNotNull(replayPb);
        Assertions.assertEquals("testSourceLive", replayPb.recordingMethod());
        Assertions.assertEquals("", replayPb.recordingFileName());

        final AiPlaybook filePb = RecordingAnnotationTestClass.class.getMethod("sampleCustomFileNameMethod").getAnnotation(AiPlaybook.class);
        Assertions.assertNotNull(filePb);
        Assertions.assertEquals("", filePb.recordingMethod());
        Assertions.assertEquals("custom-baseline-file", filePb.recordingFileName());
    }

    /**
     * Sample test class decorated with dataset annotations to test exact vs prefix dataset matching.
     */
    public static class DataSetMatchingTestClass
    {
        @Test
        @AiDataSet("modern-bad")
        public void testExactMatch()
        {
        }
    }

    /**
     * Goal: Verifies that @AiDataSet("modern-bad") correctly extracts the dataset annotation and does not match via partial substring.
     */
    @Test
    public void testDataSetAnnotationExtraction() throws Exception
    {
        final AiDataSet ds = DataSetMatchingTestClass.class.getMethod("testExactMatch").getAnnotation(AiDataSet.class);
        Assertions.assertNotNull(ds);
        Assertions.assertEquals(1, ds.value().length);
        Assertions.assertEquals("modern-bad", ds.value()[0]);
    }

    /**
     * Sample test class decorated with multiple @Browser annotations to verify multiplication.
     */
    @Browser("Chrome_1024x768")
    @Browser("Firefox_1024x768")
    public static class MultiBrowserTestClass
    {
        @Test
        @AiInlinePlaybook("name: multi-browser\nsteps:\n  - instruction: Open homepage\n")
        public void testMultiBrowser()
        {
        }
    }

    private static ExtensionContext createMockExtensionContext(final Class<?> testClass, final Method method)
    {
        return createMockExtensionContext(testClass, method, null);
    }

    private static ExtensionContext createMockExtensionContext(final Class<?> testClass, final Method method, final Throwable executionException)
    {
        final Map<Object, Object> storeMap = new HashMap<>();
        final ExtensionContext.Store mockStore = (ExtensionContext.Store) Proxy.newProxyInstance(
            ExtensionContext.Store.class.getClassLoader(),
            new Class<?>[]{ExtensionContext.Store.class},
            (sp, sm, sargs) -> {
                if ("put".equals(sm.getName()))
                {
                    storeMap.put(sargs[0], sargs[1]);
                    return null;
                }
                if ("get".equals(sm.getName()))
                {
                    return storeMap.get(sargs[0]);
                }
                return null;
            }
        );

        return (ExtensionContext) Proxy.newProxyInstance(
            ExtensionContext.class.getClassLoader(),
            new Class<?>[]{ExtensionContext.class},
            (proxy, m, args) -> {
                if ("getExecutionException".equals(m.getName()))
                {
                    return Optional.ofNullable(executionException);
                }
                if ("getTestClass".equals(m.getName()))
                {
                    return Optional.ofNullable(testClass);
                }
                if ("getRequiredTestClass".equals(m.getName()))
                {
                    return testClass;
                }
                if ("getTestMethod".equals(m.getName()))
                {
                    return Optional.ofNullable(method);
                }
                if ("getRequiredTestMethod".equals(m.getName()))
                {
                    return method;
                }
                if ("getRequiredTestInstance".equals(m.getName()))
                {
                    return testClass.getDeclaredConstructor().newInstance();
                }
                if ("getStore".equals(m.getName()))
                {
                    return mockStore;
                }
                if (m.getReturnType().equals(Optional.class))
                {
                    return Optional.empty();
                }
                return null;
            }
        );
    }

    /**
     * Goal: Verifies that NeodymiumAiRunner multiplies test template invocation contexts across all specified browser profiles.
     */
    @Test
    public void testProvideTestTemplateInvocationContextsWithMultipleBrowsers() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = MultiBrowserTestClass.class.getMethod("testMultiBrowser");
        final ExtensionContext extensionContext = createMockExtensionContext(MultiBrowserTestClass.class, method);

        final List<TestTemplateInvocationContext> contexts =
            runner.provideTestTemplateInvocationContexts(extensionContext).collect(Collectors.toList());

        Assertions.assertEquals(2, contexts.size());
        Assertions.assertTrue(contexts.get(0).getDisplayName(1).contains("Chrome_1024x768"));
        Assertions.assertTrue(contexts.get(1).getDisplayName(2).contains("Firefox_1024x768"));
    }

    /**
     * Sample test class decorated with REPLAY_STRICT mode and nonexistent companion file.
     */
    public static class SampleReplayMissingCompanionClass
    {
        @Test
        @AiMode(ExecutionMode.REPLAY_STRICT)
        @AiInlinePlaybook("name: replay_sample\nsteps:\n  - step: Click search button\n")
        @AiPlaybook(recordingMethod = "testNonExistentLive")
        public void testMissingReplay()
        {
        }
    }

    /**
     * Goal: Verifies that when a replay test fails early in beforeEach (e.g. missing companion JSON file),
     * a failure report is still generated and registered in the test reports and index dashboard.
     */
    @Test
    public void testEarlyFailureInReplayModeGeneratesReport(@TempDir final Path tempDir) throws Exception
    {
        final Path reportDir = tempDir.resolve("ai-reports-early-fail");
        Neodymium.getData().put("neodymium.ai.report.disk.enabled", "true");
        Neodymium.getData().put("neodymium.ai.report.disk.directory", reportDir.toString());
        Neodymium.getData().put("neodymium.ai.report.disk.formats", "ALL");
        AiConfiguration.resetInstance();

        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final java.lang.reflect.Method method = SampleReplayMissingCompanionClass.class.getMethod("testMissingReplay");
        final ExtensionContext extensionContext = createMockExtensionContext(SampleReplayMissingCompanionClass.class, method);

        final List<TestTemplateInvocationContext> contexts =
            runner.provideTestTemplateInvocationContexts(extensionContext).toList();
        Assertions.assertFalse(contexts.isEmpty());

        final List<Extension> extensions = contexts.get(0).getAdditionalExtensions();
        final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
            .filter(e -> e instanceof BeforeEachCallback)
            .findFirst()
            .orElseThrow();

        Assertions.assertThrows(FileNotFoundException.class, () -> {
            beforeEach.beforeEach(extensionContext);
        });

        // Verify report files were generated despite the early failure
        final Path targetResultsDir = Path.of("target/ai-results");
        final Path searchDir;
        if (Files.exists(reportDir))
        {
            try (var stream = Files.list(reportDir))
            {
                searchDir = stream.anyMatch(p -> p.getFileName().toString().endsWith(".html")) ? reportDir : targetResultsDir;
            }
        }
        else
        {
            searchDir = targetResultsDir;
        }

        try (var stream = Files.list(searchDir))
        {
            final List<Path> files = searchDir.equals(targetResultsDir)
                ? stream.filter(p -> p.getFileName().toString().contains("SampleReplayMissingCompanionClass_testMissingReplay")).toList()
                : stream.toList();
            Assertions.assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().endsWith(".html")), "HTML report must be generated");
            Assertions.assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().endsWith(".md")), "Markdown report must be generated");
            Assertions.assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().endsWith(".json")), "JSON report must be generated");
            if (searchDir.equals(reportDir))
            {
                Assertions.assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("index.html")), "index.html must be updated");
            }

            final Path jsonPath = files.stream()
                .filter(p -> p.getFileName().toString().endsWith(".json") && !p.getFileName().toString().equals("index-data.json"))
                .findFirst()
                .orElseThrow();
            final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
            Assertions.assertEquals("FAILED", root.get("status").asText());
            Assertions.assertEquals("REPLAY_STRICT", root.get("executionMode").asText());
            Assertions.assertTrue(root.get("failureReason").asText().contains("No recorded companion JSON file found"));
        }
    }

    /**
     * Sample test class decorated with inline playbook containing datasets and unmatched @AiDataSet.
     */
    public static class UnmatchedDataSetTestClass
    {
        /**
         * Test method requesting non-existent dataset.
         */
        @Test
        @AiDataSet("nonexistent")
        @AiInlinePlaybook("name: dataset_sample\ndata:\n  - id: US\n    query: jeans\n  - id: DE\n    query: hemd\nsteps:\n  - step: Search\n")
        public void testUnmatchedDataSet()
        {
        }
    }

    /**
     * Goal: Verifies that NeodymiumAiRunner throws IllegalArgumentException when @AiDataSet does not match any dataset.
     */
    @Test
    public void testUnmatchedDataSetThrowsException() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = UnmatchedDataSetTestClass.class.getMethod("testUnmatchedDataSet");
        final ExtensionContext extensionContext = createMockExtensionContext(UnmatchedDataSetTestClass.class, method);

        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            runner.provideTestTemplateInvocationContexts(extensionContext);
        });

        Assertions.assertTrue(ex.getMessage().contains("matched @AiDataSet filter [nonexistent]"));
        Assertions.assertTrue(ex.getMessage().contains("Available dataset IDs: [US, DE]"));
    }

    /**
     * Sample test class decorated with class-level and method-level @AiVisual annotations.
     */
    @AiVisual(0.95)
    @AiMode(ExecutionMode.LLM_ONLY)
    public static class VisualAnnotationTestClass
    {
        @Test
        @AiInlinePlaybook("name: visual_sample\nsteps:\n  - step: Check logo\n")
        public void testClassLevelVisual()
        {
        }

        @Test
        @AiVisual(threshold = 0.92)
        @AiInlinePlaybook("name: visual_sample\nsteps:\n  - step: Check logo\n")
        public void testMethodLevelVisual()
        {
        }
    }

    /**
     * Goal: Verifies that @AiVisual on class and method levels configures neodymium.ai.ssim.minScore.
     */
    @Test
    public void testAiVisualAnnotationHandling() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();

        // 1. Class-level annotation (0.95)
        final Method classMethod = VisualAnnotationTestClass.class.getMethod("testClassLevelVisual");
        final ExtensionContext classContext = createMockExtensionContext(VisualAnnotationTestClass.class, classMethod);
        final List<TestTemplateInvocationContext> classInvocations =
            runner.provideTestTemplateInvocationContexts(classContext).toList();
        Assertions.assertFalse(classInvocations.isEmpty());

        final List<Extension> classExtensions = classInvocations.get(0).getAdditionalExtensions();
        final BeforeEachCallback classBeforeEach = (BeforeEachCallback) classExtensions.stream()
            .filter(e -> e instanceof BeforeEachCallback)
            .findFirst()
            .orElseThrow();
        classBeforeEach.beforeEach(classContext);
        Assertions.assertEquals("0.95", Neodymium.getData().get("neodymium.ai.ssim.minScore"));

        // 2. Method-level override (0.92)
        final Method methodMethod = VisualAnnotationTestClass.class.getMethod("testMethodLevelVisual");
        final ExtensionContext methodContext = createMockExtensionContext(VisualAnnotationTestClass.class, methodMethod);
        final List<TestTemplateInvocationContext> methodInvocations =
            runner.provideTestTemplateInvocationContexts(methodContext).toList();
        Assertions.assertFalse(methodInvocations.isEmpty());

        final List<Extension> methodExtensions = methodInvocations.get(0).getAdditionalExtensions();
        final BeforeEachCallback methodBeforeEach = (BeforeEachCallback) methodExtensions.stream()
            .filter(e -> e instanceof BeforeEachCallback)
            .findFirst()
            .orElseThrow();
        methodBeforeEach.beforeEach(methodContext);
        Assertions.assertEquals("0.92", Neodymium.getData().get("neodymium.ai.ssim.minScore"));

        // Cleanup
        Neodymium.getData().remove("neodymium.ai.ssim.minScore");
    }

    /**
     * Sample test class decorated with FORCE_RECORDING mode and specific recordingFileName.
     */
    public static class ForceRecordingUpfrontDeletionTestClass
    {
        @Test
        @AiMode(ExecutionMode.FORCE_RECORDING)
        @AiInlinePlaybook("name: force_recording_sample\nsteps:\n  - step: Click search button\n")
        @AiPlaybook(recordingFileName = "force_recording_file")
        public void testForceRecording()
        {
        }
    }

    /**
     * Sample test class decorated with LLM_RECORDING mode and specific recordingFileName.
     */
    public static class LlmRecordingPreservesExistingFileTestClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_RECORDING)
        @AiInlinePlaybook("name: llm_recording_sample\nsteps:\n  - step: Click search button\n")
        @AiPlaybook(recordingFileName = "llm_recording_file")
        public void testLlmRecording()
        {
        }
    }

    /**
     * Goal: Verifies that FORCE_RECORDING deletes pre-existing recordings upfront in beforeEach,
     * while LLM_RECORDING preserves pre-existing recordings upfront.
     */
    @Test
    public void testForceRecordingDeletesUpfrontWhileLlmRecordingPreserves(@TempDir final Path tempDir) throws Exception
    {
        final Path forceFile = tempDir.resolve("force_recording_file.json");
        final Path llmFile = tempDir.resolve("llm_recording_file.json");
        Files.writeString(forceFile, "{\"existing\":\"data\"}");
        Files.writeString(llmFile, "{\"existing\":\"data\"}");

        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toString());
        AiConfiguration.resetInstance();

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();

            // 1. Verify FORCE_RECORDING deletes upfront in beforeEach
            final Method forceMethod = ForceRecordingUpfrontDeletionTestClass.class.getMethod("testForceRecording");
            final ExtensionContext forceContext = createMockExtensionContext(ForceRecordingUpfrontDeletionTestClass.class, forceMethod);
            final List<TestTemplateInvocationContext> forceInvocations =
                runner.provideTestTemplateInvocationContexts(forceContext).toList();
            Assertions.assertFalse(forceInvocations.isEmpty());

            final BeforeEachCallback forceBeforeEach = (BeforeEachCallback) forceInvocations.get(0).getAdditionalExtensions().stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();

            forceBeforeEach.beforeEach(forceContext);
            Assertions.assertFalse(Files.exists(forceFile), "FORCE_RECORDING must delete pre-existing recording upfront");

            // 2. Verify LLM_RECORDING does NOT delete upfront in beforeEach
            final Method llmMethod = LlmRecordingPreservesExistingFileTestClass.class.getMethod("testLlmRecording");
            final ExtensionContext llmContext = createMockExtensionContext(LlmRecordingPreservesExistingFileTestClass.class, llmMethod);
            final List<TestTemplateInvocationContext> llmInvocations =
                runner.provideTestTemplateInvocationContexts(llmContext).toList();
            Assertions.assertFalse(llmInvocations.isEmpty());

            final BeforeEachCallback llmBeforeEach = (BeforeEachCallback) llmInvocations.get(0).getAdditionalExtensions().stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();

            llmBeforeEach.beforeEach(llmContext);
            Assertions.assertTrue(Files.exists(llmFile), "LLM_RECORDING must NOT delete pre-existing recording upfront");
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
        }
    }

    /**
     * Sample test class decorated with AUTO mode without existing recording file.
     */
    public static class AutoModeNoRecordingTestClass
    {
        @Test
        @AiMode(ExecutionMode.AUTO)
        @AiInlinePlaybook("name: auto_mode_sample\nsteps:\n  - step: Click search button\n")
        @AiPlaybook(recordingFileName = "nonexistent_auto_recording")
        public void testAutoWithoutRecording()
        {
        }
    }

    /**
     * Sample test class decorated with AUTO mode with pre-existing recording file.
     */
    public static class AutoModeWithExistingRecordingTestClass
    {
        @Test
        @AiMode(ExecutionMode.AUTO)
        @AiInlinePlaybook("name: auto_mode_sample\nsteps:\n  - step: Click search button\n")
        @AiPlaybook(recordingFileName = "existing_auto_recording")
        public void testAutoWithRecording()
        {
        }
    }

    /**
     * Sample test class decorated with AUTO mode where companion recording has a mismatched source YAML hash.
     */
    public static class AutoModeWithMismatchedHashTestClass
    {
        @Test
        @AiMode(ExecutionMode.AUTO)
        @AiInlinePlaybook("name: auto_mode_sample\nsteps:\n  - step: Click search button\n")
        @AiPlaybook(recordingFileName = "stale_hash_recording")
        public void testAutoWithMismatchedHash()
        {
        }
    }

    /**
     * Sample test class decorated with REPLAY_STRICT where companion recording has a mismatched source YAML hash.
     */
    public static class ReplayStrictWithMismatchedHashTestClass
    {
        @Test
        @AiMode(ExecutionMode.REPLAY_STRICT)
        @AiInlinePlaybook("name: strict_mode_sample\nsteps:\n  - step: Click search button\n")
        @AiPlaybook(recordingFileName = "strict_stale_hash_recording")
        public void testStrictWithMismatchedHash()
        {
        }
    }

    /**
     * Goal: Verifies that AUTO mode dynamically resolves to LLM_RECORDING when no companion
     * JSON recording exists on disk, without throwing FileNotFoundException.
     */
    @Test
    public void testAutoModeResolvesToLlmRecordingWhenRecordingMissing(@TempDir final Path tempDir) throws Exception
    {
        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toString());
        AiConfiguration.resetInstance();

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = AutoModeNoRecordingTestClass.class.getMethod("testAutoWithoutRecording");
            final ExtensionContext extensionContext = createMockExtensionContext(AutoModeNoRecordingTestClass.class, method);
            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final BeforeEachCallback beforeEach = (BeforeEachCallback) invocations.get(0).getAdditionalExtensions().stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();

            // Must NOT throw FileNotFoundException
            beforeEach.beforeEach(extensionContext);

            final ExecutionContext execCtx = (ExecutionContext) extensionContext.getStore(null).get(ExecutionContext.class);
            Assertions.assertNotNull(execCtx);
            Assertions.assertEquals(ExecutionMode.LLM_RECORDING, execCtx.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE));
            Assertions.assertEquals(ExecutionMode.AUTO, execCtx.getTransientData().get(ExecutionContext.KEY_CONFIGURED_EXECUTION_MODE));

            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            Assertions.assertEquals(ExecutionMode.LLM_RECORDING, session.getExecutionMode());
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
        }
    }

    /**
     * Goal: Verifies that AUTO mode dynamically resolves to REPLAY_WITH_HEALING when a companion
     * JSON recording exists on disk, setting playbookRecordingFile accordingly.
     */
    @Test
    public void testAutoModeResolvesToReplayWithHealingWhenRecordingExists(@TempDir final Path tempDir) throws Exception
    {
        final Path recordingFile = tempDir.resolve("existing_auto_recording.json");
        Files.writeString(recordingFile, "[{\"step\":\"Click search button\",\"actions\":[]}]");

        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toString());
        AiConfiguration.resetInstance();

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = AutoModeWithExistingRecordingTestClass.class.getMethod("testAutoWithRecording");
            final ExtensionContext extensionContext = createMockExtensionContext(AutoModeWithExistingRecordingTestClass.class, method);
            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final BeforeEachCallback beforeEach = (BeforeEachCallback) invocations.get(0).getAdditionalExtensions().stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);

            final ExecutionContext execCtx = (ExecutionContext) extensionContext.getStore(null).get(ExecutionContext.class);
            Assertions.assertNotNull(execCtx);
            Assertions.assertEquals(ExecutionMode.REPLAY_WITH_HEALING, execCtx.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE));
            Assertions.assertEquals(ExecutionMode.AUTO, execCtx.getTransientData().get(ExecutionContext.KEY_CONFIGURED_EXECUTION_MODE));
            final Object recPath = execCtx.getTransientData().get("playbookRecordingFile");
            Assertions.assertNotNull(recPath);
            Assertions.assertTrue(recPath.toString().endsWith("existing_auto_recording.json"));

            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            Assertions.assertEquals(ExecutionMode.REPLAY_WITH_HEALING, session.getExecutionMode());
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
        }
    }

    /**
     * Goal: Verifies that computeResourceSha256 produces identical hashes for CRLF and LF content.
     */
    @Test
    public void testComputeResourceSha256LineEndingInvariance() throws Exception
    {
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("lf.yaml", "name: test\r\nsteps:\r\n  - step: hello\r\n");
        manager.write("crlf.yaml", "name: test\nsteps:\n  - step: hello\n");

        final String hashLf = NeodymiumAiRunner.computeResourceSha256(manager, "lf.yaml");
        final String hashCrlf = NeodymiumAiRunner.computeResourceSha256(manager, "crlf.yaml");

        Assertions.assertNotNull(hashLf);
        Assertions.assertNotNull(hashCrlf);
        Assertions.assertEquals(hashLf, hashCrlf, "SHA-256 computation must normalize line endings and produce identical hashes");
    }

    /**
     * Goal: Verifies that AUTO mode discards a stale companion recording when source YAML hash differs,
     * surfaces an execution warning, and falls back to live LLM_RECORDING mode.
     */
    @Test
    public void testAutoModeDiscardsStaleRecordingWhenSourceYamlHashDiffers(@TempDir final Path tempDir) throws Exception
    {
        final Path recordingFile = tempDir.resolve("stale_hash_recording.json");
        Files.writeString(recordingFile, "[{\"step\":\"Click search button\",\"sourceYamlHash\":\"stale_deadbeef1234\",\"actions\":[]}]");

        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toString());
        AiConfiguration.resetInstance();

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = AutoModeWithMismatchedHashTestClass.class.getMethod("testAutoWithMismatchedHash");
            final ExtensionContext extensionContext = createMockExtensionContext(AutoModeWithMismatchedHashTestClass.class, method);
            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final BeforeEachCallback beforeEach = (BeforeEachCallback) invocations.get(0).getAdditionalExtensions().stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);

            final ExecutionContext execCtx = (ExecutionContext) extensionContext.getStore(null).get(ExecutionContext.class);
            Assertions.assertNotNull(execCtx);
            Assertions.assertEquals(ExecutionMode.LLM_RECORDING, execCtx.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE));
            Assertions.assertEquals(ExecutionMode.AUTO, execCtx.getTransientData().get(ExecutionContext.KEY_CONFIGURED_EXECUTION_MODE));
            Assertions.assertNull(execCtx.getTransientData().get("playbookRecordingFile"), "Stale recording path must not be bound");

            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            Assertions.assertEquals(ExecutionMode.LLM_RECORDING, session.getExecutionMode());

            @SuppressWarnings("unchecked")
            final List<String> warnings = (List<String>) execCtx.getTransientData().get(ExecutionContext.KEY_EXECUTION_WARNINGS);
            Assertions.assertNotNull(warnings);
            Assertions.assertTrue(warnings.stream().anyMatch(w -> w.contains("discarded") && w.contains("stale_hash_recording.json")));
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
        }
    }

    /**
     * Goal: Verifies that REPLAY_STRICT fails immediately with IllegalStateException when source YAML hash differs.
     */
    @Test
    public void testReplayStrictThrowsWhenSourceYamlHashDiffers(@TempDir final Path tempDir) throws Exception
    {
        final Path recordingFile = tempDir.resolve("strict_stale_hash_recording.json");
        Files.writeString(recordingFile, "[{\"step\":\"Click search button\",\"sourceYamlHash\":\"stale_deadbeef1234\",\"actions\":[]}]");

        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toString());
        AiConfiguration.resetInstance();

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = ReplayStrictWithMismatchedHashTestClass.class.getMethod("testStrictWithMismatchedHash");
            final ExtensionContext extensionContext = createMockExtensionContext(ReplayStrictWithMismatchedHashTestClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final BeforeEachCallback beforeEach = (BeforeEachCallback) invocations.get(0).getAdditionalExtensions().stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();

            final IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () ->
            {
                beforeEach.beforeEach(extensionContext);
            });

            Assertions.assertTrue(ex.getMessage().contains("has been modified since recording"),
                "Exception message must specify source YAML modification");
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
        }
    }

    /**
     * Sample test class decorated with @Browser to verify WebDriver lifecycle preservation.
     */
    @Browser("Chrome_1024x768")
    public static class SampleBrowserAiTestClass
    {
        @Test
        @AiInlinePlaybook("name: sample-browser\nsteps:\n  - step: Click search button\n")
        public void sampleMethod()
        {
        }
    }

    /**
     * Goal: Verifies that provideTestTemplateInvocationContexts and getAdditionalExtensions clear Neodymium thread context.
     */
    @Test
    public void testGetAdditionalExtensionsClearsThreadContext() throws Exception
    {
        try
        {
            Neodymium.getData().put("dirtyKey1", "dirtyValue1");

            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBrowserAiTestClass.class.getMethod("sampleMethod");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBrowserAiTestClass.class, method);

            // Phase 1: Verify provideTestTemplateInvocationContexts clears thread context
            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());
            Assertions.assertNull(Neodymium.getData().get("dirtyKey1"), "provideTestTemplateInvocationContexts must clear pre-existing thread context");

            // Phase 2: Re-populate dirty data to verify getAdditionalExtensions clears independently
            Neodymium.getData().put("dirtyKey2", "dirtyValue2");
            invocations.get(0).getAdditionalExtensions();
            Assertions.assertNull(Neodymium.getData().get("dirtyKey2"), "getAdditionalExtensions must clear thread-local context before building extensions");
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that AiInvocationExtension does not wipe the WebDriverStateContainer registered by BrowserExecutionCallback.
     * Prevents regression of issue #485 where Neodymium.getDriver() returned null in AI tests.
     */
    @Test
    public void testAiInvocationExtensionPreservesWebDriver() throws Exception
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBrowserAiTestClass.class.getMethod("sampleMethod");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBrowserAiTestClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final List<BeforeEachCallback> beforeCallbacks = extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .map(e -> (BeforeEachCallback) e)
                .toList();
            Assertions.assertEquals(2, beforeCallbacks.size());

            // Simulate driver registration by BrowserExecutionCallback (first extension)
            final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, m, args) -> null
            );
            final WebDriverStateContainer container = new WebDriverStateContainer();
            container.setWebDriver(mockDriver);
            Neodymium.setWebDriverStateContainer(container);

            // Execute AiInvocationExtension.beforeEach (second extension)
            beforeCallbacks.get(1).beforeEach(extensionContext);

            Assertions.assertTrue(Neodymium.hasDriver(), "Neodymium.hasDriver() must be true after AiInvocationExtension.beforeEach");
            Assertions.assertSame(mockDriver, Neodymium.getDriver(), "Neodymium.getDriver() must return the registered WebDriver instance");
        }
        finally
        {
            WebDriverRunner.closeWebDriver();
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that AiInvocationExtension does not initiate duplicate video recording
     * when a browser is present and automatic filming is enabled, delegating to BrowserRunner instead.
     */
    @Test
    public void testAiInvocationExtensionDoesNotDuplicateBrowserRunnerFilming() throws Exception
    {
        try
        {
            FilmTestExecution.getContextVideo().setProperty("video.enableFilming", "true");
            FilmTestExecution.getContextVideo().setProperty("video.filmAutomatically", "true");

            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBrowserAiTestClass.class.getMethod("sampleMethod");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBrowserAiTestClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final List<BeforeEachCallback> beforeCallbacks = extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .map(e -> (BeforeEachCallback) e)
                .toList();
            Assertions.assertEquals(2, beforeCallbacks.size());

            // Execute AiInvocationExtension.beforeEach
            beforeCallbacks.get(1).beforeEach(extensionContext);

            final ExecutionContext execCtx = (ExecutionContext) extensionContext.getStore(null).get(ExecutionContext.class);
            Assertions.assertNotNull(execCtx);
            Assertions.assertNull(execCtx.getTransientData().get("recordingID"),
                "recordingID must NOT be registered by AiInvocationExtension when browser is active and filmAutomatically is true");
        }
        finally
        {
            FilmTestExecution.getContextVideo().setProperty("video.enableFilming", "false");
            FilmTestExecution.getContextVideo().setProperty("video.filmAutomatically", "false");
            WebDriverRunner.closeWebDriver();
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that AiInvocationExtension.afterEach does not wipe the browser profile name before BrowserRunner teardown.
     */
    @Test
    public void testAiInvocationExtensionAfterEachPreservesBrowserProfileName() throws Exception
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBrowserAiTestClass.class.getMethod("sampleMethod");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBrowserAiTestClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();

            Neodymium.setBrowserProfileName("Chrome_1024x768");
            afterEach.afterEach(extensionContext);

            Assertions.assertEquals("Chrome_1024x768", Neodymium.getBrowserProfileName(), "Browser profile name must remain intact for BrowserRunner teardown");
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that AiInvocationExtension.afterEach does NOT close or quit a WebDriver that has been
     * returned to WebDriverCache (e.g. by BrowserExecutionCallback before an @AfterEach method when
     * startNewBrowserForCleanUp = true and reuseDriver = true).
     * Prevents regression of issue #479 (NoSuchSessionException on reused driver).
     */
    @Test
    public void testAiInvocationExtensionPreservesCachedWebDriverAcrossAfterEach() throws Exception
    {
        final AtomicBoolean quitCalled = new AtomicBoolean(false);
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            WebDriver.class.getClassLoader(),
            new Class<?>[]{WebDriver.class},
            (proxy, m, args) -> {
                if ("quit".equals(m.getName()))
                {
                    quitCalled.set(true);
                }
                return null;
            }
        );

        final WebDriverStateContainer container = new WebDriverStateContainer();
        container.setWebDriver(mockDriver);

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBrowserAiTestClass.class.getMethod("sampleMethod");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBrowserAiTestClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();

            // Simulate driver registered in Selenide WebDriverRunner, but already reclaimed into WebDriverCache
            // and detached from Neodymium thread context (exact state during @AfterEach execution)
            WebDriverRunner.setWebDriver(mockDriver);
            WebDriverCache.instance.putWebDriverStateContainer("Chrome_1024x768", container);
            Neodymium.setWebDriverStateContainer(null);

            afterEach.afterEach(extensionContext);

            Assertions.assertFalse(quitCalled.get(), "Cached WebDriver must not be quit by AiInvocationExtension.afterEach");
            Assertions.assertTrue(WebDriverCache.instance.containsWebDriver(mockDriver), "Cached WebDriver must remain in WebDriverCache");
            Assertions.assertFalse(WebDriverRunner.hasWebDriverStarted(), "WebDriverRunner must be reset after caching so ThreadLocal does not leak");
        }
        finally
        {
            WebDriverCache.instance.removeWebDriverStateContainer("Chrome_1024x768");
            WebDriverRunner.closeWebDriver();
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Sample test class without @Browser annotation to verify unmanaged WebDriver cleanup.
     */
    public static class SampleStandaloneAiTestClass
    {
        @Test
        @AiInlinePlaybook("name: sample-standalone\nsteps:\n  - step: Click search button\n")
        public void sampleMethod()
        {
        }
    }

    /**
     * Goal: Verifies that AiInvocationExtension.afterEach properly closes and quits an unmanaged WebDriver
     * that is NOT present in WebDriverCache and NOT managed by Neodymium's WebDriverStateContainer.
     */
    @Test
    public void testAiInvocationExtensionCleansUpWhenNotCached() throws Exception
    {
        final AtomicBoolean quitCalled = new AtomicBoolean(false);
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            WebDriver.class.getClassLoader(),
            new Class<?>[]{WebDriver.class},
            (proxy, m, args) -> {
                if ("quit".equals(m.getName()))
                {
                    quitCalled.set(true);
                }
                return null;
            }
        );

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleStandaloneAiTestClass.class.getMethod("sampleMethod");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleStandaloneAiTestClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();

            // Register standalone driver in Selenide WebDriverRunner without caching it in Neodymium
            WebDriverRunner.setWebDriver(mockDriver);
            Neodymium.setWebDriverStateContainer(null);

            afterEach.afterEach(extensionContext);

            Assertions.assertTrue(quitCalled.get(), "Unmanaged WebDriver must be quit by AiInvocationExtension.afterEach");
            Assertions.assertFalse(WebDriverRunner.hasWebDriverStarted(), "WebDriverRunner must report no started driver");
        }
        finally
        {
            WebDriverRunner.closeWebDriver();
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that WebDriverCache.containsWebDriver correctly identifies cached WebDrivers
     * regardless of whether raw, wrapped, or decorated instances are supplied.
     */
    @Test
    public void testWebDriverCacheContainsWebDriver()
    {
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            WebDriver.class.getClassLoader(),
            new Class<?>[]{WebDriver.class},
            (proxy, m, args) -> null
        );

        final WebDriver wrappedDriver = (WebDriver) Proxy.newProxyInstance(
            NeodymiumAiRunnerTest.class.getClassLoader(),
            new Class<?>[]{WebDriver.class, WrapsDriver.class},
            (proxy, m, args) -> {
                if ("getWrappedDriver".equals(m.getName()))
                {
                    return mockDriver;
                }
                return null;
            }
        );

        final WebDriverStateContainer container = new WebDriverStateContainer();
        container.setWebDriver(mockDriver);

        try
        {
            Assertions.assertFalse(WebDriverCache.instance.containsWebDriver(mockDriver), "Cache must not contain driver before insertion");

            WebDriverCache.instance.putWebDriverStateContainer("testTag", container);

            // 1. Direct raw equality
            Assertions.assertTrue(WebDriverCache.instance.containsWebDriver(mockDriver), "Cache must contain registered raw driver");

            // 2. Wrapped candidate matching raw cached driver
            Assertions.assertTrue(WebDriverCache.instance.containsWebDriver(wrappedDriver), "Cache must recognize wrapped driver pointing to cached driver");

            // 3. Raw candidate matching wrapped cached driver
            final WebDriverStateContainer wrappedContainer = new WebDriverStateContainer();
            wrappedContainer.setWebDriver(wrappedDriver);
            WebDriverCache.instance.putWebDriverStateContainer("testWrappedTag", wrappedContainer);

            Assertions.assertTrue(WebDriverCache.instance.containsWebDriver(mockDriver), "Cache with wrapped driver must recognize raw driver candidate");

            // 4. Null and unknown drivers
            Assertions.assertFalse(WebDriverCache.instance.containsWebDriver(null), "Null driver must return false");

            final WebDriver unknownDriver = (WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, m, args) -> null
            );
            Assertions.assertFalse(WebDriverCache.instance.containsWebDriver(unknownDriver), "Unrelated driver must return false");
        }
        finally
        {
            WebDriverCache.instance.removeWebDriverStateContainer("testTag");
            WebDriverCache.instance.removeWebDriverStateContainer("testWrappedTag");
        }
    }

    /**
     * Sample test class with programmatic setup and test method.
     */
    public static class SampleBeforeEachAndTestMethodProgrammaticClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_ONLY)
        @AiPlaybook(AiPlaybook.PROGRAMMATIC)
        public void testProgrammatic()
        {
        }
    }

    /**
     * Sample test class with programmatic setup and YAML inline playbook test method.
     */
    public static class SampleBeforeEachProgrammaticAndYamlPlaybookClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_ONLY)
        @AiInlinePlaybook("name: yaml-playbook\nsteps:\n  - step: Step in YAML\n")
        public void testYamlPlaybook()
        {
        }
    }

    /**
     * Sample test class with multiple programmatic executions in test method.
     */
    public static class SampleMultiExecuteTestMethodClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_ONLY)
        @AiPlaybook(AiPlaybook.PROGRAMMATIC)
        public void testMultiExecute()
        {
        }
    }

    /**
     * Sample test class with YAML inline playbook containing an after block.
     */
    public static class SampleAfterBlockFailureClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_ONLY)
        @AiInlinePlaybook("name: after-fail-playbook\nsteps:\n  - step: Main Step\nafter:\n  - step: Failing Teardown Step\n")
        public void testAfterPlaybook()
        {
        }
    }

    @SuppressWarnings("unchecked")
    private static ReflectiveInvocationContext<Method> mockReflectiveInvocationContext(final Method method)
    {
        return (ReflectiveInvocationContext<Method>) Proxy.newProxyInstance(
            ReflectiveInvocationContext.class.getClassLoader(),
            new Class<?>[]{ReflectiveInvocationContext.class},
            (proxy, m, args) -> {
                if ("getExecutable".equals(m.getName()))
                {
                    return method;
                }
                if ("getTarget".equals(m.getName()))
                {
                    return Optional.empty();
                }
                if ("getArguments".equals(m.getName()))
                {
                    return Collections.emptyList();
                }
                return null;
            }
        );
    }

    private static void setupMockLlm(final AiSession session)
    {
        final LlmProvider mockLlm = AiSession.createMockLlmProvider();
        session.getLlmRegistry().setDefaultProvider(mockLlm);
        for (final LlmCapability cap : LlmCapability.values())
        {
            session.getLlmRegistry().registerProvider(cap, mockLlm);
        }
    }

    /**
     * Goal: Verifies that when a test executes programmatic steps in @BeforeEach and in the @Test method,
     * both step sequences are accumulated in the final report and metrics without premature session completion (GH #474, #476).
     */
    @Test
    public void testBeforeEachProgrammaticAndTestMethodProgrammaticBothReported() throws Throwable
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBeforeEachAndTestMethodProgrammaticClass.class.getMethod("testProgrammatic");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBeforeEachAndTestMethodProgrammaticClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();

            // 1. Run beforeEach setup
            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            setupMockLlm(session);

            // 2. Simulate @BeforeEach programmatic execution
            session.execute("Step in BeforeEach");

            // 3. Simulate @Test method programmatic execution via InvocationInterceptor
            interceptor.interceptTestMethod(
                () -> {
                    session.execute("Step in TestMethod");
                    return null;
                },
                mockReflectiveInvocationContext(method),
                extensionContext
            );

            // 4. Run afterEach teardown
            afterEach.afterEach(extensionContext);

            // 5. Verify that PreliminaryReportListener contains BOTH steps
            final PreliminaryReportListener reportListener = (PreliminaryReportListener) session.getEventBus().getListeners().stream()
                .filter(l -> l instanceof PreliminaryReportListener)
                .findFirst()
                .orElseThrow();

            final TestExecutionReport report = reportListener.getReport();
            Assertions.assertNotNull(report);
            Assertions.assertEquals(2, report.getSteps().size(), "Report must contain both @BeforeEach and @Test steps");
            Assertions.assertEquals("Step in BeforeEach", report.getSteps().get(0).getInstruction());
            Assertions.assertEquals("Step in TestMethod", report.getSteps().get(1).getInstruction());
            Assertions.assertTrue(report.isSuccess(), "Overall test status must be success");
            Assertions.assertEquals("PASSED", report.getStatus());

            // 6. Verify cumulative metrics
            Assertions.assertEquals(2, session.getMetrics().getStepCount(), "Session metrics must report 2 total steps");
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that when a test executes programmatic steps in @BeforeEach followed by a YAML playbook
     * in the test method, both step sequences are accumulated in the final report without premature completion (GH #475).
     */
    @Test
    public void testBeforeEachProgrammaticAndTestMethodYamlPlaybookBothReported() throws Throwable
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBeforeEachProgrammaticAndYamlPlaybookClass.class.getMethod("testYamlPlaybook");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBeforeEachProgrammaticAndYamlPlaybookClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();

            // 1. Run beforeEach setup
            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            setupMockLlm(session);

            // 2. Simulate @BeforeEach programmatic execution
            session.execute("Step in BeforeEach");

            // 3. Simulate @Test method execution (which executes the YAML playbook steps)
            interceptor.interceptTestMethod(
                () -> null,
                mockReflectiveInvocationContext(method),
                extensionContext
            );

            // 4. Run afterEach teardown
            afterEach.afterEach(extensionContext);

            // 5. Verify that PreliminaryReportListener contains both the BeforeEach step and the YAML playbook step
            final PreliminaryReportListener reportListener = (PreliminaryReportListener) session.getEventBus().getListeners().stream()
                .filter(l -> l instanceof PreliminaryReportListener)
                .findFirst()
                .orElseThrow();

            final TestExecutionReport report = reportListener.getReport();
            Assertions.assertNotNull(report);
            Assertions.assertEquals(2, report.getSteps().size(), "Report must contain both @BeforeEach and YAML playbook steps");
            Assertions.assertEquals("Step in BeforeEach", report.getSteps().get(0).getInstruction());
            Assertions.assertEquals("Step in YAML", report.getSteps().get(1).getInstruction());
            Assertions.assertTrue(report.isSuccess(), "Overall test status must be success");
            Assertions.assertEquals("PASSED", report.getStatus());
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that multiple sequential session.execute() calls within a single test method accumulate properly.
     */
    @Test
    public void testMultipleProgrammaticExecuteCallsInTestMethodBothReported() throws Throwable
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleMultiExecuteTestMethodClass.class.getMethod("testMultiExecute");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleMultiExecuteTestMethodClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            setupMockLlm(session);

            interceptor.interceptTestMethod(
                () -> {
                    session.execute("Action 1");
                    session.execute("Action 2");
                    session.execute("Action 3");
                    return null;
                },
                mockReflectiveInvocationContext(method),
                extensionContext
            );

            afterEach.afterEach(extensionContext);

            final PreliminaryReportListener reportListener = (PreliminaryReportListener) session.getEventBus().getListeners().stream()
                .filter(l -> l instanceof PreliminaryReportListener)
                .findFirst()
                .orElseThrow();

            final TestExecutionReport report = reportListener.getReport();
            Assertions.assertNotNull(report);
            Assertions.assertEquals(3, report.getSteps().size(), "Report must contain all 3 steps");
            Assertions.assertEquals("Action 1", report.getSteps().get(0).getInstruction());
            Assertions.assertEquals("Action 2", report.getSteps().get(1).getInstruction());
            Assertions.assertEquals("Action 3", report.getSteps().get(2).getInstruction());
            Assertions.assertTrue(report.isSuccess());
            Assertions.assertEquals(3, session.getMetrics().getStepCount());
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that multiple sequential session.execute() calls within @BeforeEach accumulate properly
     * in chronological order ahead of test method steps.
     */
    @Test
    public void testMultipleProgrammaticExecuteCallsInBeforeEachBothReported() throws Throwable
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBeforeEachAndTestMethodProgrammaticClass.class.getMethod("testProgrammatic");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBeforeEachAndTestMethodProgrammaticClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            setupMockLlm(session);

            session.execute("Before Action 1");
            session.execute("Before Action 2");

            interceptor.interceptTestMethod(
                () -> {
                    session.execute("Test Action");
                    return null;
                },
                mockReflectiveInvocationContext(method),
                extensionContext
            );

            afterEach.afterEach(extensionContext);

            final PreliminaryReportListener reportListener = (PreliminaryReportListener) session.getEventBus().getListeners().stream()
                .filter(l -> l instanceof PreliminaryReportListener)
                .findFirst()
                .orElseThrow();

            final TestExecutionReport report = reportListener.getReport();
            Assertions.assertNotNull(report);
            Assertions.assertEquals(3, report.getSteps().size(), "Report must contain all 3 steps");
            Assertions.assertEquals("Before Action 1", report.getSteps().get(0).getInstruction());
            Assertions.assertEquals("Before Action 2", report.getSteps().get(1).getInstruction());
            Assertions.assertEquals("Test Action", report.getSteps().get(2).getInstruction());
            Assertions.assertTrue(report.isSuccess());
            Assertions.assertEquals("PASSED", report.getStatus());
            Assertions.assertEquals(3, session.getMetrics().getStepCount());
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that multiple sequential session.execute() calls within @AfterEach accumulate properly
     * in chronological order following test method steps.
     */
    @Test
    public void testMultipleProgrammaticExecuteCallsInAfterEachBothReported() throws Throwable
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBeforeEachAndTestMethodProgrammaticClass.class.getMethod("testProgrammatic");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBeforeEachAndTestMethodProgrammaticClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            setupMockLlm(session);

            session.execute("Before Action");

            interceptor.interceptTestMethod(
                () -> {
                    session.execute("Test Action");
                    return null;
                },
                mockReflectiveInvocationContext(method),
                extensionContext
            );

            session.execute("After Action 1");
            session.execute("After Action 2");

            afterEach.afterEach(extensionContext);

            final PreliminaryReportListener reportListener = (PreliminaryReportListener) session.getEventBus().getListeners().stream()
                .filter(l -> l instanceof PreliminaryReportListener)
                .findFirst()
                .orElseThrow();

            final TestExecutionReport report = reportListener.getReport();
            Assertions.assertNotNull(report);
            Assertions.assertEquals(4, report.getSteps().size(), "Report must contain all 4 steps");
            Assertions.assertEquals("Before Action", report.getSteps().get(0).getInstruction());
            Assertions.assertEquals("Test Action", report.getSteps().get(1).getInstruction());
            Assertions.assertEquals("After Action 1", report.getSteps().get(2).getInstruction());
            Assertions.assertEquals("After Action 2", report.getSteps().get(3).getInstruction());
            Assertions.assertTrue(report.isSuccess());
            Assertions.assertEquals("PASSED", report.getStatus());
            Assertions.assertEquals(4, session.getMetrics().getStepCount());
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies strict chronological ordering across multiple programmatic executions in @BeforeEach,
     * YAML playbook steps in @Test, and multiple programmatic executions in @AfterEach.
     */
    @Test
    public void testHybridBeforeEachAndTestYamlAndAfterEachChronologicalOrder() throws Throwable
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBeforeEachProgrammaticAndYamlPlaybookClass.class.getMethod("testYamlPlaybook");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBeforeEachProgrammaticAndYamlPlaybookClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            setupMockLlm(session);

            session.execute("Before Action 1");
            session.execute("Before Action 2");

            interceptor.interceptTestMethod(
                () -> null,
                mockReflectiveInvocationContext(method),
                extensionContext
            );

            session.execute("After Action 1");
            session.execute("After Action 2");

            afterEach.afterEach(extensionContext);

            final PreliminaryReportListener reportListener = (PreliminaryReportListener) session.getEventBus().getListeners().stream()
                .filter(l -> l instanceof PreliminaryReportListener)
                .findFirst()
                .orElseThrow();

            final TestExecutionReport report = reportListener.getReport();
            Assertions.assertNotNull(report);
            Assertions.assertEquals(5, report.getSteps().size(), "Report must contain all 5 hybrid steps in chronological order");
            Assertions.assertEquals("Before Action 1", report.getSteps().get(0).getInstruction());
            Assertions.assertEquals("Before Action 2", report.getSteps().get(1).getInstruction());
            Assertions.assertEquals("Step in YAML", report.getSteps().get(2).getInstruction());
            Assertions.assertEquals("After Action 1", report.getSteps().get(3).getInstruction());
            Assertions.assertEquals("After Action 2", report.getSteps().get(4).getInstruction());
            Assertions.assertTrue(report.isSuccess());
            Assertions.assertEquals("PASSED", report.getStatus());
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that when a step fails inside a test method but is caught and handled via try/catch,
     * subsequent successful steps clear the last error and the overall test is marked as PASSED.
     */
    @Test
    public void testTryCatchRecoveredStepDoesNotFailOverallTest() throws Throwable
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBeforeEachAndTestMethodProgrammaticClass.class.getMethod("testProgrammatic");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBeforeEachAndTestMethodProgrammaticClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);

            final AtomicBoolean failStep = new AtomicBoolean(true);
            final LlmProvider failingThenSucceedingLlm = new LlmProvider()
            {
                @Override
                public LlmResponse chat(final LlmRequest request)
                {
                    if (failStep.get())
                    {
                        throw new RuntimeException("Simulated permanent failure for step 1");
                    }
                    return new LlmResponse("[]", new TokenUsage(10, 0, 10), "mock-model", List.of(new ToolCall("mock_call", "complete_step", JsonNodeFactory.instance.objectNode())), null);
                }

                @Override
                public Set<LlmCapability> getCapabilities()
                {
                    return Set.of(LlmCapability.values());
                }
            };

            session.getLlmRegistry().setDefaultProvider(failingThenSucceedingLlm);
            for (final LlmCapability cap : LlmCapability.values())
            {
                session.getLlmRegistry().registerProvider(cap, failingThenSucceedingLlm);
            }

            final AtomicBoolean sessionFinishedSuccess = new AtomicBoolean(false);
            session.getEventBus().registerListener(new ExecutionListener()
            {
                @Override
                public void onEvent(final ExecutionEvent event)
                {
                    if (event instanceof SessionFinishedEvent sfe)
                    {
                        sessionFinishedSuccess.set(sfe.isSuccess());
                    }
                }
            });

            interceptor.interceptTestMethod(
                () -> {
                    boolean caught = false;
                    try
                    {
                        session.execute("Failing Step");
                    }
                    catch (final Exception expected)
                    {
                        caught = true;
                    }
                    Assertions.assertTrue(caught, "Step 1 failure must be caught");

                    failStep.set(false);
                    session.execute("Recovery Step");
                    return null;
                },
                mockReflectiveInvocationContext(method),
                extensionContext
            );

            afterEach.afterEach(extensionContext);

            Assertions.assertTrue(sessionFinishedSuccess.get(), "SessionFinishedEvent must report overallSuccess = true when step failure is handled");
            Assertions.assertNull(session.getExecutionContext().getTransientData().get(ExecutionContext.KEY_LAST_EXECUTION_ERROR), "KEY_LAST_EXECUTION_ERROR must be cleared after recovery step succeeds");
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that when an after-playbook teardown step fails during afterEach(),
     * the teardown error is rethrown so JUnit reports the failure, and SessionFinishedEvent
     * reports overallSuccess = false.
     */
    @Test
    public void testAfterPlaybookTeardownFailureInAfterEachIsRethrown() throws Throwable
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleAfterBlockFailureClass.class.getMethod("testAfterPlaybook");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleAfterBlockFailureClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);

            final LlmProvider failingAfterLlm = new LlmProvider()
            {
                @Override
                public LlmResponse chat(final LlmRequest request)
                {
                    throw new RuntimeException("Simulated after-playbook teardown failure");
                }

                @Override
                public Set<LlmCapability> getCapabilities()
                {
                    return Set.of(LlmCapability.values());
                }
            };

            session.getLlmRegistry().setDefaultProvider(failingAfterLlm);
            for (final LlmCapability cap : LlmCapability.values())
            {
                session.getLlmRegistry().registerProvider(cap, failingAfterLlm);
            }

            final AtomicBoolean sessionFinishedSuccess = new AtomicBoolean(true);
            session.getEventBus().registerListener(new ExecutionListener()
            {
                @Override
                public void onEvent(final ExecutionEvent event)
                {
                    if (event instanceof SessionFinishedEvent sfe)
                    {
                        sessionFinishedSuccess.set(sfe.isSuccess());
                    }
                }
            });

            // Simulate unexecuted after steps executing in afterEach callback
            final Throwable thrown = Assertions.assertThrows(
                Throwable.class,
                () -> afterEach.afterEach(extensionContext),
                "afterEach must rethrow afterPlaybookError when teardown fails"
            );

            Assertions.assertTrue(
                thrown.getMessage().contains("Simulated after-playbook teardown failure")
                    || (thrown.getCause() != null && thrown.getCause().getMessage().contains("Simulated after-playbook teardown failure")),
                "Rethrown exception must cite teardown failure"
            );
            Assertions.assertFalse(sessionFinishedSuccess.get(), "SessionFinishedEvent must report overallSuccess = false on teardown failure");
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that when both the test body fails AND deferred after-playbook teardown fails,
     * the teardown error is attached as a suppressed exception to the primary test exception.
     */
    @Test
    public void testAfterPlaybookTeardownFailureAttachedAsSuppressedWhenTestBodyFails() throws Throwable
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleAfterBlockFailureClass.class.getMethod("testAfterPlaybook");
            final RuntimeException primaryTestException = new RuntimeException("Primary test body failure");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleAfterBlockFailureClass.class, method, primaryTestException);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);

            final LlmProvider failingAfterLlm = new LlmProvider()
            {
                @Override
                public LlmResponse chat(final LlmRequest request)
                {
                    throw new RuntimeException("Simulated after-playbook teardown failure");
                }

                @Override
                public Set<LlmCapability> getCapabilities()
                {
                    return Set.of(LlmCapability.values());
                }
            };

            session.getLlmRegistry().setDefaultProvider(failingAfterLlm);
            for (final LlmCapability cap : LlmCapability.values())
            {
                session.getLlmRegistry().registerProvider(cap, failingAfterLlm);
            }

            // afterEach should NOT throw because primary test exception is already present in context
            afterEach.afterEach(extensionContext);

            // Assert teardown error was added as a suppressed exception on the primary test exception
            final Throwable[] suppressed = primaryTestException.getSuppressed();
            Assertions.assertEquals(1, suppressed.length, "Teardown error must be attached as a suppressed exception");
            Assertions.assertTrue(
                suppressed[0].getMessage().contains("Simulated after-playbook teardown failure"),
                "Suppressed exception must cite teardown failure"
            );
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that when context == null and an error is recorded in KEY_LAST_EXECUTION_ERROR,
     * FORCE_RECORDING mode correctly voids the recording file upon afterEach(null).
     */
    @Test
    public void testForceRecordingVoidedWhenFailureRecordedWithNullContext(@TempDir final Path tempDir) throws Exception
    {
        final Path forceFile = tempDir.resolve("force_recording_file.json");
        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toString());
        AiConfiguration.resetInstance();

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method forceMethod = ForceRecordingUpfrontDeletionTestClass.class.getMethod("testForceRecording");
            final ExtensionContext forceContext = createMockExtensionContext(ForceRecordingUpfrontDeletionTestClass.class, forceMethod);
            final List<TestTemplateInvocationContext> forceInvocations =
                runner.provideTestTemplateInvocationContexts(forceContext).toList();
            Assertions.assertFalse(forceInvocations.isEmpty());

            final List<Extension> extensions = forceInvocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback forceBeforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback forceAfterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();

            forceBeforeEach.beforeEach(forceContext);

            // Re-create recording file to simulate partial recording written before failure
            Files.writeString(forceFile, "{\"steps\":[{\"step\":\"initial\"}]}");
            Assertions.assertTrue(Files.exists(forceFile), "Recording file should exist prior to failure cleanup");

            final AiSession session = (AiSession) forceContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            session.getExecutionContext().getTransientData().put(
                ExecutionContext.KEY_LAST_EXECUTION_ERROR,
                new RuntimeException("Simulated failure in headless / null-context execution")
            );

            // Execute afterEach with null context
            forceAfterEach.afterEach(null);

            Assertions.assertFalse(Files.exists(forceFile), "FORCE_RECORDING must delete recording file when failure occurred with null context");
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that NeodymiumAiRunner gracefully handles unmodifiable lists seeded
     * under playbook.steps and playbook.flatSteps without throwing UnsupportedOperationException.
     */
    @Test
    public void testAppendExecutedStepsHandlesUnmodifiableStepsList() throws Throwable
    {
        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleBeforeEachProgrammaticAndYamlPlaybookClass.class.getMethod("testYamlPlaybook");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleBeforeEachProgrammaticAndYamlPlaybookClass.class, method);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);

            // Intentionally replace playbook.steps and flatSteps with unmodifiable lists
            final PlaybookStep seededStep = new PlaybookStep("Seeded Step");
            session.getExecutionContext().getTransientData().put("playbook.steps", List.of(seededStep));
            session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(seededStep));

            final LlmProvider succeedingLlm = new LlmProvider()
            {
                @Override
                public LlmResponse chat(final LlmRequest request)
                {
                    return new LlmResponse("[]", new TokenUsage(10, 0, 10), "mock-model", List.of(new ToolCall("mock_call", "complete_step", JsonNodeFactory.instance.objectNode())), null);
                }

                @Override
                public Set<LlmCapability> getCapabilities()
                {
                    return Set.of(LlmCapability.values());
                }
            };

            session.getLlmRegistry().setDefaultProvider(succeedingLlm);
            for (final LlmCapability cap : LlmCapability.values())
            {
                session.getLlmRegistry().registerProvider(cap, succeedingLlm);
            }

            // Running test method triggers appendExecutedSteps onto unmodifiable list
            interceptor.interceptTestMethod(
                () -> null,
                mockReflectiveInvocationContext(method),
                extensionContext
            );

            afterEach.afterEach(extensionContext);

            @SuppressWarnings("unchecked")
            final List<PlaybookStep> finalSteps = (List<PlaybookStep>) session.getExecutionContext().getTransientData().get("playbook.steps");
            Assertions.assertNotNull(finalSteps);
            Assertions.assertTrue(finalSteps.size() >= 2, "Unmodifiable steps list must be converted and accumulated into");
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that when a step succeeds and the test body subsequently fails with an assertion error,
     * the final preliminary execution report retains the failure reason and stack trace.
     */
    @Test
    public void testTestBodyAssertionFailureSetsFailureReasonInReport() throws Throwable
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = SampleBeforeEachProgrammaticAndYamlPlaybookClass.class.getMethod("testYamlPlaybook");
        final AssertionError bodyAssertionFailure = new AssertionError("Expected elements count [5] but was [3]");
        final ExtensionContext extensionContext = createMockExtensionContext(SampleBeforeEachProgrammaticAndYamlPlaybookClass.class, method, bodyAssertionFailure);

        try
        {
            final List<TestTemplateInvocationContext> contexts = runner.provideTestTemplateInvocationContexts(extensionContext)
                .collect(Collectors.toList());
            Assertions.assertEquals(1, contexts.size());

            final TestTemplateInvocationContext invContext = contexts.get(0);
            final List<Extension> extensions = invContext.getAdditionalExtensions();

            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            setupMockLlm(session);

            interceptor.interceptTestMethod(
                () -> null,
                mockReflectiveInvocationContext(method),
                extensionContext
            );

            afterEach.afterEach(extensionContext);

            final PreliminaryReportListener reportListener = (PreliminaryReportListener) session.getEventBus().getListeners().stream()
                .filter(l -> l instanceof PreliminaryReportListener)
                .findFirst()
                .orElseThrow();

            final TestExecutionReport report = reportListener.getReport();
            Assertions.assertNotNull(report);
            Assertions.assertFalse(report.isSuccess());
            Assertions.assertEquals("FAILED", report.getStatus());
            Assertions.assertNotNull(report.getFailureReason(), "Report failure reason must be populated from test body failure");
            Assertions.assertTrue(report.getFailureReason().contains("Expected elements count [5] but was [3]"));
            Assertions.assertNotNull(report.getFailureStackTrace(), "Report failure stack trace must be populated from test body failure");
            Assertions.assertTrue(report.getFailureStackTrace().contains("AssertionError"));
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Sample test class with multiple @BeforeEach methods annotated with @AiPlaybook.
     */
    public static class SampleMultipleBeforeEachClass
    {
        @BeforeEach
        @AiPlaybook("inline-setup-1.yaml")
        public void setupOne()
        {
        }

        @BeforeEach
        @AiPlaybook("inline-setup-2.yaml")
        public void setupTwo()
        {
        }

        @Test
        @AiMode(ExecutionMode.LLM_ONLY)
        @AiInlinePlaybook("name: main_test\nsteps:\n  - step: Main test step\n")
        public void testMultipleSetup()
        {
        }
    }

    /**
     * Goal: Verifies that multiple @BeforeEach methods annotated with @AiPlaybook
     * accumulate their steps in playbook.beforeSteps without overwriting earlier ones.
     */
    @Test
    public void testMultipleBeforeEachPlaybookAnnotationsAccumulateSteps() throws Throwable
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method testMethod = SampleMultipleBeforeEachClass.class.getMethod("testMultipleSetup");
        final Method setupMethod1 = SampleMultipleBeforeEachClass.class.getMethod("setupOne");
        final Method setupMethod2 = SampleMultipleBeforeEachClass.class.getMethod("setupTwo");
        final ExtensionContext extensionContext = createMockExtensionContext(SampleMultipleBeforeEachClass.class, testMethod);

        try
        {
            final List<TestTemplateInvocationContext> contexts = runner.provideTestTemplateInvocationContexts(extensionContext)
                .collect(Collectors.toList());
            Assertions.assertEquals(1, contexts.size());

            final TestTemplateInvocationContext invContext = contexts.get(0);
            final List<Extension> extensions = invContext.getAdditionalExtensions();

            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            setupMockLlm(session);

            final PlaybookResourceManager resourceManager =
                (PlaybookResourceManager) session.getExecutionContext().getTransientData().get(ExecutionContext.KEY_RESOURCE_MANAGER);
            resourceManager.write("inline-setup-1.yaml", "name: setup1\nsteps:\n  - step: Setup step one\n");
            resourceManager.write("inline-setup-2.yaml", "name: setup2\nsteps:\n  - step: Setup step two\n");

            interceptor.interceptBeforeEachMethod(
                () -> null,
                mockReflectiveInvocationContext(setupMethod1),
                extensionContext
            );

            interceptor.interceptBeforeEachMethod(
                () -> null,
                mockReflectiveInvocationContext(setupMethod2),
                extensionContext
            );

            @SuppressWarnings("unchecked")
            final List<PlaybookStep> beforeSteps = (List<PlaybookStep>) session.getExecutionContext().getTransientData().get("playbook.beforeSteps");
            Assertions.assertNotNull(beforeSteps);
            Assertions.assertEquals(2, beforeSteps.size(), "playbook.beforeSteps must contain steps from both @BeforeEach methods");
            Assertions.assertEquals("Setup step one", beforeSteps.get(0).getInstruction());
            Assertions.assertEquals("Setup step two", beforeSteps.get(1).getInstruction());
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that when lifecycle YAML playbooks exist on the filesystem/resource manager upfront
     * before test instance post-processing, steps are not registered twice into playbook.beforeSteps.
     */
    @Test
    public void testLifecyclePlaybookAnnotationsDoNotDuplicateStepsWhenFilesExistUpfront() throws Throwable
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method testMethod = SampleMultipleBeforeEachClass.class.getMethod("testMultipleSetup");
        final Method setupMethod1 = SampleMultipleBeforeEachClass.class.getMethod("setupOne");
        final Method setupMethod2 = SampleMultipleBeforeEachClass.class.getMethod("setupTwo");
        final ExtensionContext extensionContext = createMockExtensionContext(SampleMultipleBeforeEachClass.class, testMethod);

        try
        {
            final List<TestTemplateInvocationContext> contexts = runner.provideTestTemplateInvocationContexts(extensionContext)
                .collect(Collectors.toList());
            Assertions.assertEquals(1, contexts.size());

            final TestTemplateInvocationContext invContext = contexts.get(0);
            final List<Extension> extensions = invContext.getAdditionalExtensions();

            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();

            // Run beforeEach callback which sets up session
            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            setupMockLlm(session);

            final PlaybookResourceManager resourceManager =
                (PlaybookResourceManager) session.getExecutionContext().getTransientData().get(ExecutionContext.KEY_RESOURCE_MANAGER);
            resourceManager.write("inline-setup-1.yaml", "name: setup1\nsteps:\n  - step: Setup step one\n");
            resourceManager.write("inline-setup-2.yaml", "name: setup2\nsteps:\n  - step: Setup step two\n");

            // Execute the @BeforeEach methods via the interceptor
            interceptor.interceptBeforeEachMethod(
                () -> null,
                mockReflectiveInvocationContext(setupMethod1),
                extensionContext
            );

            interceptor.interceptBeforeEachMethod(
                () -> null,
                mockReflectiveInvocationContext(setupMethod2),
                extensionContext
            );

            @SuppressWarnings("unchecked")
            final List<PlaybookStep> beforeSteps = (List<PlaybookStep>) session.getExecutionContext().getTransientData().get("playbook.beforeSteps");
            Assertions.assertNotNull(beforeSteps);
            Assertions.assertEquals(2, beforeSteps.size(), "playbook.beforeSteps must have exactly 2 steps, without duplicate entries");
            Assertions.assertEquals("Setup step one", beforeSteps.get(0).getInstruction());
            Assertions.assertEquals("Setup step two", beforeSteps.get(1).getInstruction());
        }
        finally
        {
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Sample test class decorated with programmatic mode and recording-enabled execution mode.
     */
    public static class SampleProgrammaticRecordingListenerTestClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_RECORDING)
        @AiPlaybook(AiPlaybook.PROGRAMMATIC)
        public void testRecordingMode(final AiSession session)
        {
        }
    }

    /**
     * Sample test class decorated with programmatic mode and non-recording execution mode.
     */
    public static class SampleProgrammaticNonRecordingListenerTestClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_ONLY)
        @AiPlaybook(AiPlaybook.PROGRAMMATIC)
        public void testNonRecordingMode(final AiSession session)
        {
        }
    }

    /**
     * Sample test class failing during programmatic step execution.
     */
    public static class SampleProgrammaticFailureTestClass
    {
        @Test
        @AiMode(ExecutionMode.FORCE_RECORDING)
        @AiPlaybook(AiPlaybook.PROGRAMMATIC)
        public void testFailure(final AiSession session) throws Exception
        {
            session.execute("Step 1");
            throw new IllegalStateException("Intentional test step failure");
        }
    }

    /**
     * Goal: Verifies that NeodymiumAiRunner registers PlaybookRecorder on eventBus in recording mode for programmatic tests.
     */
    @Test
    public void testProgrammaticListenerRegistrationInRecordingMode() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = SampleProgrammaticRecordingListenerTestClass.class.getMethod("testRecordingMode", AiSession.class);
        final ExtensionContext extensionContext = createMockExtensionContext(SampleProgrammaticRecordingListenerTestClass.class, method);

        final List<TestTemplateInvocationContext> invocations =
            runner.provideTestTemplateInvocationContexts(extensionContext).toList();
        Assertions.assertFalse(invocations.isEmpty());

        final BeforeEachCallback beforeEach = (BeforeEachCallback) invocations.get(0).getAdditionalExtensions().stream()
            .filter(e -> e instanceof BeforeEachCallback)
            .findFirst()
            .orElseThrow();

        beforeEach.beforeEach(extensionContext);

        final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
        Assertions.assertNotNull(session, "AiSession must be created and stored");
        Assertions.assertTrue(
            session.getEventBus().getListeners().stream().anyMatch(l -> l instanceof PlaybookRecorder),
            "PlaybookRecorder must be registered on eventBus when mode persists recordings"
        );
        Assertions.assertTrue(
            session.getEventBus().getListeners().stream().anyMatch(l -> l instanceof TokenBudgetGuard),
            "TokenBudgetGuard must be registered on eventBus"
        );
    }

    /**
     * Goal: Verifies that NeodymiumAiRunner does NOT register PlaybookRecorder in non-recording mode (LLM_ONLY).
     */
    @Test
    public void testProgrammaticListenerOmittedInNonRecordingMode() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = SampleProgrammaticNonRecordingListenerTestClass.class.getMethod("testNonRecordingMode", AiSession.class);
        final ExtensionContext extensionContext = createMockExtensionContext(SampleProgrammaticNonRecordingListenerTestClass.class, method);

        final List<TestTemplateInvocationContext> invocations =
            runner.provideTestTemplateInvocationContexts(extensionContext).toList();
        Assertions.assertFalse(invocations.isEmpty());

        final BeforeEachCallback beforeEach = (BeforeEachCallback) invocations.get(0).getAdditionalExtensions().stream()
            .filter(e -> e instanceof BeforeEachCallback)
            .findFirst()
            .orElseThrow();

        beforeEach.beforeEach(extensionContext);

        final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
        Assertions.assertNotNull(session, "AiSession must be created and stored");
        Assertions.assertFalse(
            session.getEventBus().getListeners().stream().anyMatch(l -> l instanceof PlaybookRecorder),
            "PlaybookRecorder must not be registered on eventBus when mode does not persist recordings"
        );
    }

    /**
     * Goal: Verifies that if a programmatic test fails, PlaybookRecorder does not persist a corrupt recording.
     */
    @Test
    public void testProgrammaticExecutionOnFailureDoesNotPersistCorruptRecording(@TempDir final Path tempDir) throws Throwable
    {
        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toAbsolutePath().toString());
        AiConfiguration.resetInstance();

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleProgrammaticFailureTestClass.class.getMethod("testFailure", AiSession.class);
            final Throwable intentionalFailure = new IllegalStateException("Intentional test step failure");
            final ExtensionContext extensionContext = createMockExtensionContext(SampleProgrammaticFailureTestClass.class, method, intentionalFailure);

            final List<TestTemplateInvocationContext> invocations =
                runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertFalse(invocations.isEmpty());

            final List<Extension> extensions = invocations.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream()
                .filter(e -> e instanceof BeforeEachCallback)
                .findFirst()
                .orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream()
                .filter(e -> e instanceof InvocationInterceptor)
                .findFirst()
                .orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream()
                .filter(e -> e instanceof AfterEachCallback)
                .findFirst()
                .orElseThrow();

            beforeEach.beforeEach(extensionContext);

            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session, "AiSession must be present in extensionContext store");
            setupMockLlm(session);

            Assertions.assertThrows(IllegalStateException.class, () -> {
                interceptor.interceptTestMethod(
                    () -> {
                        session.execute("Step 1");
                        throw intentionalFailure;
                    },
                    null,
                    extensionContext
                );
            });

            afterEach.afterEach(extensionContext);

            final Path recordingFile = tempDir.resolve("SampleProgrammaticFailureTestClass_testFailure.json");
            Assertions.assertFalse(Files.exists(recordingFile), "Recording file must not be written when test execution fails");
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that when an intentional negative test catches an expected session failure
     * via assertThrows, JUnit passes the test method, and NeodymiumAiRunner reconciles the preliminary
     * execution report and interactive console status from FAILED to PASSED with expectedFailure=true.
     */
    @Test
    public void testExpectedFailureCaughtByAssertThrowsReconcilesReportToPassed(@TempDir final Path tempDir) throws Throwable
    {
        final Path reportDir = tempDir.resolve("ai-reports-reconciled");
        System.setProperty("neodymium.ai.report.disk.directory", reportDir.toString());
        System.setProperty("neodymium.ai.report.disk.enabled", "true");
        System.setProperty("neodymium.ai.console.executionLogs.enabled", "true");
        System.setProperty("neodymium.ai.console.executionLogs.directory", reportDir.toString());
        AiConfiguration.resetInstance();

        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = SampleProgrammaticFailureTestClass.class.getMethod("testFailure", AiSession.class);
        final ExtensionContext extensionContext = createMockExtensionContext(SampleProgrammaticFailureTestClass.class, method, null);

        try
        {
            final List<TestTemplateInvocationContext> contexts = runner.provideTestTemplateInvocationContexts(extensionContext).toList();
            Assertions.assertEquals(1, contexts.size());

            final List<Extension> extensions = contexts.get(0).getAdditionalExtensions();
            final BeforeEachCallback beforeEach = (BeforeEachCallback) extensions.stream().filter(e -> e instanceof BeforeEachCallback).findFirst().orElseThrow();
            final InvocationInterceptor interceptor = (InvocationInterceptor) extensions.stream().filter(e -> e instanceof InvocationInterceptor).findFirst().orElseThrow();
            final AfterEachCallback afterEach = (AfterEachCallback) extensions.stream().filter(e -> e instanceof AfterEachCallback).findFirst().orElseThrow();

            beforeEach.beforeEach(extensionContext);
            final AiSession session = (AiSession) extensionContext.getStore(null).get(AiSession.class);
            Assertions.assertNotNull(session);
            setupMockLlm(session);

            final PreliminaryReportListener reportListener = (PreliminaryReportListener) session.getEventBus().getListeners().stream()
                .filter(l -> l instanceof PreliminaryReportListener)
                .findFirst()
                .orElseThrow();

            interceptor.interceptTestMethod(
                () -> {
                    final TestExecutionReport.ReportStepEntry failingStep = new TestExecutionReport.ReportStepEntry(1, "Assert invalid condition");
                    failingStep.setStatus("FAILED");
                    failingStep.setFailureReason("Assertion failed as expected");
                    reportListener.getReport().addStep(failingStep);
                    reportListener.getReport().setStatus("FAILED");
                    reportListener.getReport().setSuccess(false);
                    reportListener.getReport().setFailureReason("Assertion failed as expected");
                    return null;
                },
                null,
                extensionContext
            );

            Assertions.assertEquals("FAILED", reportListener.getReport().getStatus());

            afterEach.afterEach(extensionContext);

            Assertions.assertEquals("PASSED", reportListener.getReport().getStatus(), "Report status must be reconciled to PASSED");
            Assertions.assertTrue(reportListener.getReport().isSuccess(), "Report success must be reconciled to true");
            Assertions.assertTrue(reportListener.getReport().isExpectedFailure(), "Report must be flagged with expectedFailure=true");
            Assertions.assertEquals("Assertion failed as expected", reportListener.getReport().getFailureReason(), "Failure reason must be preserved for diagnostics");
        }
        finally
        {
            System.clearProperty("neodymium.ai.report.disk.directory");
            System.clearProperty("neodymium.ai.report.disk.enabled");
            System.clearProperty("neodymium.ai.console.executionLogs.enabled");
            System.clearProperty("neodymium.ai.console.executionLogs.directory");
            AiConfiguration.resetInstance();
            Neodymium.clearThreadContext();
        }
    }
}
