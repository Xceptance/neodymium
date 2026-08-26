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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.util.Neodymium;
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
    @com.xceptance.neodymium.common.browser.Browser("Chrome_1024x768")
    @com.xceptance.neodymium.common.browser.Browser("Firefox_1024x768")
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
        try (var stream = Files.list(reportDir))
        {
            final List<Path> files = stream.toList();
            Assertions.assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().endsWith(".html")), "HTML report must be generated");
            Assertions.assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().endsWith(".md")), "Markdown report must be generated");
            Assertions.assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().endsWith(".json")), "JSON report must be generated");
            Assertions.assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("index.html")), "index.html must be updated");

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
}
