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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.recorder.PlaybookRecorder;
import org.neodymium.ai.resources.InMemoryResourceManager;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.telemetry.TokenBudgetGuard;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.WebDriverStateContainer;
import org.neodymium.util.Neodymium;
import org.openqa.selenium.WebDriver;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

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

    private static org.junit.jupiter.api.extension.ExtensionContext createMockExtensionContext(final Class<?> testClass, final java.lang.reflect.Method method)
    {
        final java.util.Map<Object, Object> storeMap = new java.util.HashMap<>();
        final org.junit.jupiter.api.extension.ExtensionContext.Store mockStore = (org.junit.jupiter.api.extension.ExtensionContext.Store) java.lang.reflect.Proxy.newProxyInstance(
            org.junit.jupiter.api.extension.ExtensionContext.Store.class.getClassLoader(),
            new Class<?>[]{org.junit.jupiter.api.extension.ExtensionContext.Store.class},
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

        return (org.junit.jupiter.api.extension.ExtensionContext) java.lang.reflect.Proxy.newProxyInstance(
            org.junit.jupiter.api.extension.ExtensionContext.class.getClassLoader(),
            new Class<?>[]{org.junit.jupiter.api.extension.ExtensionContext.class},
            (proxy, m, args) -> {
                if ("getTestClass".equals(m.getName()))
                {
                    return java.util.Optional.ofNullable(testClass);
                }
                if ("getRequiredTestClass".equals(m.getName()))
                {
                    return testClass;
                }
                if ("getTestMethod".equals(m.getName()))
                {
                    return java.util.Optional.ofNullable(method);
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
                if (m.getReturnType().equals(java.util.Optional.class))
                {
                    return java.util.Optional.empty();
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
        final java.lang.reflect.Method method = MultiBrowserTestClass.class.getMethod("testMultiBrowser");
        final org.junit.jupiter.api.extension.ExtensionContext extensionContext = createMockExtensionContext(MultiBrowserTestClass.class, method);

        final java.util.List<org.junit.jupiter.api.extension.TestTemplateInvocationContext> contexts =
            runner.provideTestTemplateInvocationContexts(extensionContext).collect(java.util.stream.Collectors.toList());

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
     * Sample test class decorated with programmatic playbook mode.
     */
    public static class SampleProgrammaticSetupTestClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_RECORDING)
        @AiPlaybook(AiPlaybook.PROGRAMMATIC)
        public void testProgrammatic(final AiSession session)
        {
        }
    }

    /**
     * Goal: Verifies that NeodymiumAiRunner registers 'playbook.allSteps' in transient data for programmatic tests.
     */
    @Test
    public void testProgrammaticSetupInitializesPlaybookAllSteps() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = SampleProgrammaticSetupTestClass.class.getMethod("testProgrammatic", AiSession.class);
        final ExtensionContext extensionContext = createMockExtensionContext(SampleProgrammaticSetupTestClass.class, method);

        final List<TestTemplateInvocationContext> invocations =
            runner.provideTestTemplateInvocationContexts(extensionContext).toList();
        Assertions.assertFalse(invocations.isEmpty());

        final BeforeEachCallback beforeEach = (BeforeEachCallback) invocations.get(0).getAdditionalExtensions().stream()
            .filter(e -> e instanceof BeforeEachCallback)
            .findFirst()
            .orElseThrow();

        beforeEach.beforeEach(extensionContext);

        final ExecutionContext execCtx = (ExecutionContext) extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).get(ExecutionContext.class);
        Assertions.assertNotNull(execCtx, "ExecutionContext must be stored in ExtensionContext store");

        @SuppressWarnings("unchecked")
        final List<PlaybookStep> allSteps = (List<PlaybookStep>) execCtx.getTransientData().get("playbook.allSteps");
        Assertions.assertNotNull(allSteps, "playbook.allSteps must be registered in transient data for programmatic tests");
        Assertions.assertTrue(allSteps.isEmpty(), "playbook.allSteps should initially be empty in programmatic mode");
    }

    private static LlmProvider createDefaultMockLlmProvider()
    {
        return new LlmProvider()
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
     * Sample test class executing a programmatic step sequence.
     */
    public static class SampleProgrammaticExecutionTestClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_RECORDING)
        @AiPlaybook(AiPlaybook.PROGRAMMATIC)
        public void testProgrammaticExecution(final AiSession session) throws Exception
        {
            session.execute("Open https://example.org\nClick login");
        }
    }

    /**
     * Sample test class executing multiple programmatic step blocks.
     */
    public static class SampleProgrammaticMultiExecuteTestClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_RECORDING)
        @AiPlaybook(AiPlaybook.PROGRAMMATIC)
        public void testMultiExecute(final AiSession session) throws Exception
        {
            session.execute("Step 1: Open website");
            session.execute("Step 2: Log in to account");
        }
    }

    /**
     * Sample test class failing during programmatic step execution.
     */
    public static class SampleProgrammaticFailureTestClass
    {
        @Test
        @AiMode(ExecutionMode.LLM_RECORDING)
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
     * Goal: Verifies that executing a programmatic test writes a companion recording JSON file containing actual step content.
     */
    @Test
    public void testProgrammaticExecutionWritesNonEmptyCompanionRecording(@TempDir final Path tempDir) throws Throwable
    {
        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toAbsolutePath().toString());
        AiConfiguration.resetInstance();

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleProgrammaticExecutionTestClass.class.getMethod("testProgrammaticExecution", AiSession.class);
            final ExtensionContext extensionContext = createMockExtensionContext(SampleProgrammaticExecutionTestClass.class, method);

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
            session.getLlmRegistry().setDefaultProvider(createDefaultMockLlmProvider());

            interceptor.interceptTestMethod(
                () -> {
                    session.execute("Open https://example.org\nClick login");
                    return null;
                },
                null,
                extensionContext
            );

            afterEach.afterEach(extensionContext);

            final Path recordingFile = tempDir.resolve("SampleProgrammaticExecutionTestClass_testProgrammaticExecution.json");
            Assertions.assertTrue(Files.exists(recordingFile), "Companion recording file must be created: " + recordingFile);

            final String json = Files.readString(recordingFile);
            Assertions.assertFalse(json.trim().equals("[]") || json.trim().equals("[ ]"), "Recording must not be an empty array");

            final ObjectMapper mapper = new ObjectMapper();
            final List<PlaybookStep> recordedSteps = mapper.readValue(json, new TypeReference<List<PlaybookStep>>() {});
            Assertions.assertFalse(recordedSteps.isEmpty(), "Recorded steps must not be empty");
            Assertions.assertTrue(recordedSteps.get(0).getInstruction().contains("Open https://example.org"), "Recorded step instruction must match executed instruction");
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
            Neodymium.clearThreadContext();
        }
    }

    /**
     * Goal: Verifies that multiple programmatic session.execute() calls accumulate steps sequentially into the single recording JSON file.
     */
    @Test
    public void testProgrammaticMultiExecuteAccumulatesAllStepsInRecordingFile(@TempDir final Path tempDir) throws Throwable
    {
        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toAbsolutePath().toString());
        AiConfiguration.resetInstance();

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleProgrammaticMultiExecuteTestClass.class.getMethod("testMultiExecute", AiSession.class);
            final ExtensionContext extensionContext = createMockExtensionContext(SampleProgrammaticMultiExecuteTestClass.class, method);

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
            session.getLlmRegistry().setDefaultProvider(createDefaultMockLlmProvider());

            interceptor.interceptTestMethod(
                () -> {
                    session.execute("Step 1: Open website");
                    session.execute("Step 2: Log in to account");
                    return null;
                },
                null,
                extensionContext
            );

            afterEach.afterEach(extensionContext);

            final Path recordingFile = tempDir.resolve("SampleProgrammaticMultiExecuteTestClass_testMultiExecute.json");
            Assertions.assertTrue(Files.exists(recordingFile), "Companion recording file must be created: " + recordingFile);

            final String json = Files.readString(recordingFile);
            final ObjectMapper mapper = new ObjectMapper();
            final List<PlaybookStep> recordedSteps = mapper.readValue(json, new TypeReference<List<PlaybookStep>>() {});
            Assertions.assertEquals(2, recordedSteps.size(), "Both programmatic execution steps must be captured in the recording");
            Assertions.assertEquals("Step 1: Open website", recordedSteps.get(0).getInstruction());
            Assertions.assertEquals("Step 2: Log in to account", recordedSteps.get(1).getInstruction());
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
            Neodymium.clearThreadContext();
        }
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
            final ExtensionContext extensionContext = createMockExtensionContext(SampleProgrammaticFailureTestClass.class, method);

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
            session.getLlmRegistry().setDefaultProvider(createDefaultMockLlmProvider());

            Assertions.assertThrows(IllegalStateException.class, () -> {
                interceptor.interceptTestMethod(
                    () -> {
                        session.execute("Step 1");
                        throw new IllegalStateException("Intentional test step failure");
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
     * Goal: Verifies that if an execution error occurs in transient data (e.g. step failure)
     * but the test method catches the exception, NeodymiumAiRunner still marks overallSuccess as false
     * and suppresses recording persistence.
     */
    @Test
    public void testProgrammaticExecutionErrorSuppressesRecordingEvenWhenExceptionSwallowed(@TempDir final Path tempDir) throws Throwable
    {
        System.setProperty("neodymium.ai.playbook.recordingDirectory", tempDir.toAbsolutePath().toString());
        AiConfiguration.resetInstance();

        try
        {
            final NeodymiumAiRunner runner = new NeodymiumAiRunner();
            final Method method = SampleProgrammaticFailureTestClass.class.getMethod("testFailure", AiSession.class);
            final ExtensionContext extensionContext = createMockExtensionContext(SampleProgrammaticFailureTestClass.class, method);

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
            session.getLlmRegistry().setDefaultProvider(createDefaultMockLlmProvider());

            interceptor.interceptTestMethod(
                () -> {
                    // Simulate a step execution failure recorded into transient data, swallowed by test
                    session.getExecutionContext().getTransientData().put(
                        ExecutionContext.KEY_LAST_EXECUTION_ERROR,
                        new RuntimeException("Simulated internal step error")
                    );
                    return null;
                },
                null,
                extensionContext
            );

            afterEach.afterEach(extensionContext);

            final Path recordingFile = tempDir.resolve("SampleProgrammaticFailureTestClass_testFailure.json");
            Assertions.assertFalse(Files.exists(recordingFile), "Recording file must not be written when execution error is present in transient data");
        }
        finally
        {
            System.clearProperty("neodymium.ai.playbook.recordingDirectory");
            AiConfiguration.resetInstance();
            Neodymium.clearThreadContext();
        }
    }
}

