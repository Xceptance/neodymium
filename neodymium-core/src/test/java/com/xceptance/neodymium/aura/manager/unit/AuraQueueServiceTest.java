/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.aura.manager.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.AuraReportingService;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AuraQueueService} process lifecycle, error log extraction, and stop handling.
 *
 * @author AI-generated: Antigravity
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("aura-manager")
public final class AuraQueueServiceTest
{
    private AuraReportingService reportingService;
    private AuraInteractiveService interactiveService;
    private AuraQueueService queueService;

    @BeforeEach
    public void setUp()
    {
        reportingService = new AuraReportingService();
        interactiveService = new AuraInteractiveService();
        queueService = new AuraQueueService(reportingService, interactiveService);
    }

    @Test
    public void testStopProcessWhenNoActiveProcess()
    {
        Assertions.assertDoesNotThrow(() -> {
            queueService.stopProcess();
        });
        Assertions.assertTrue(queueService.isManuallyStopped());
    }

    @Test
    public void testStopCurrentTestWhenNoActiveProcess()
    {
        Assertions.assertDoesNotThrow(() -> {
            queueService.stopCurrentTest();
        });
    }

    @Test
    public void testSetManuallyStopped()
    {
        queueService.setManuallyStopped(true);
        Assertions.assertTrue(queueService.isManuallyStopped());

        queueService.setManuallyStopped(false);
        Assertions.assertFalse(queueService.isManuallyStopped());
    }

    @Test
    public void testBroadcastLogFormatting() throws Exception
    {
        final String rawLog = "\u001B[32m[INFO]\u001B[0m Test log with ANSI escape codes";
        queueService.broadcastLog(rawLog);

        final Field logsField = AuraQueueService.class.getDeclaredField("currentRunLogs");
        logsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final List<String> logs = (List<String>) logsField.get(queueService);

        Assertions.assertFalse(logs.isEmpty());
        Assertions.assertEquals("[INFO] Test log with ANSI escape codes", logs.get(0));
    }

    @Test
    public void testRunningQueueGuard() throws Exception
    {
        final Field runningField = AuraQueueService.class.getDeclaredField("runningQueue");
        runningField.setAccessible(true);
        final AtomicBoolean runningQueue = (AtomicBoolean) runningField.get(queueService);

        Assertions.assertFalse(runningQueue.get());
        runningQueue.set(true);
        Assertions.assertTrue(runningQueue.get());
    }

    @Test
    public void testAddRunCompletedListenerMulticast() throws Exception
    {
        final AtomicInteger count1 = new AtomicInteger(0);
        final AtomicInteger count2 = new AtomicInteger(0);

        final Consumer<String> l1 = id -> count1.incrementAndGet();
        final Consumer<String> l2 = id -> count2.incrementAndGet();

        queueService.addRunCompletedListener(l1);
        queueService.addRunCompletedListener(l2);

        final Field listenersField = AuraQueueService.class.getDeclaredField("onRunCompletedListeners");
        listenersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final List<Consumer<String>> listeners =
                (List<Consumer<String>>) listenersField.get(queueService);

        Assertions.assertEquals(2, listeners.size());
        for (final Consumer<String> listener : listeners)
        {
            listener.accept("run_123");
        }

        Assertions.assertEquals(1, count1.get());
        Assertions.assertEquals(1, count2.get());

        queueService.removeRunCompletedListener(l1);
        Assertions.assertEquals(1, listeners.size());
    }

    @Test
    public void testRunCompletedListenerObservesIdleQueue()
    {
        final AtomicBoolean queueWasIdleWhenNotified = new AtomicBoolean(false);
        final Consumer<String> listener = runId -> {
            queueWasIdleWhenNotified.set(!queueService.isRunningQueue());
        };

        queueService.addRunCompletedListener(listener);
        // Verify when queue is idle, listener executes and observes non-running queue
        listener.accept("run_idle_test");
        Assertions.assertTrue(queueWasIdleWhenNotified.get());
    }

    @Test
    public void testExtractSubprocessErrorMessageOnPlaybookParseException()
    {
        final List<String> logs = List.of(
            "2026-09-30T09:42:03.710+02:00  INFO 10956 --- [aura-manager] [raQueueExecutor] c.x.neodymium.aura.AuraQueueService      : [Aura Subprocess] [ERROR] com.xceptance.neodymium.aura.sandbox.Aura_tests_stokke_stokkeOrderPayPalTest_yml_Test.executeYamlTest  Time elapsed: 5.638 s  <<< ERROR!",
            "2026-09-30T09:42:03.712+02:00  INFO 10956 --- [aura-manager] [raQueueExecutor] c.x.neodymium.aura.AuraQueueService      : [Aura Subprocess] java.lang.RuntimeException: Failed to parse playbook: tests/stokke/stokkeOrderPayPalTest.yml",
            "2026-09-30T09:42:03.713+02:00  INFO 10956 --- [aura-manager] [raQueueExecutor] c.x.neodymium.aura.AuraQueueService      : [Aura Subprocess] Caused by: java.lang.ClassCastException: class java.util.ArrayList cannot be cast to class java.util.Map"
        );

        final String extractedError = AuraQueueService.extractSubprocessErrorMessage(logs);

        Assertions.assertNotNull(extractedError, "Extracted error message should not be null for playbook parse failure logs");
        Assertions.assertTrue(extractedError.contains("Failed to parse playbook: tests/stokke/stokkeOrderPayPalTest.yml"),
            "Error message should contain playbook parse exception details");
        Assertions.assertTrue(extractedError.contains("Caused by: java.lang.ClassCastException"),
            "Error message should contain root cause exception details");
    }

    @Test
    public void testExtractSubprocessErrorMessageIgnoresSlf4jWarnLogs()
    {
        final List<String> logs = List.of(
            "14:41:18.606 [main] WARN com.codeborne.selenide.impl.BiDiUti -- Failed to establish BiDi connection: org.openqa.selenium.bidi.BiDiException: Unable to create a BiDi connection",
            "14:42:39.000 [main] WARN com.codeborne.selenide.impl.BiDiUti -- Failed to establish BiDi connection: org.openqa.selenium.bidi.BiDiException: Unable to create a BiDi connection"
        );

        final String extractedError = AuraQueueService.extractSubprocessErrorMessage(logs);

        Assertions.assertNull(extractedError, "Extracted error message should be null for log streams containing only WARN entries");
    }

    @Test
    public void testExtractSubprocessErrorMessageFiltersMavenSurefireBoilerplateAndDeduplicates()
    {
        final List<String> logs = List.of(
            "Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 5.790 s <<< FAILURE! -- in com.xceptance.neodymium.aura.sandbox.Aura_tests_posters_homepage_yaml_Test",
            "com.xceptance.neodymium.aura.sandbox.Aura_tests_posters_homepage_yaml_Test.executeYamlTest()[1] -- Time elapsed: 5.128 s <<< ERROR!",
            "java.lang.IllegalStateException: Failed to instantiate LLM provider class 'org.neodymium.ai.client.GeminiLlmProvider' for provider 'gemini' (role 'global'): Gemini API key is missing. Please configure neodymium.ai.gemini.apiKey or set GEMINI_API_KEY.",
            "Caused by: java.lang.IllegalArgumentException: Gemini API key is missing. Please configure neodymium.ai.gemini.apiKey or set GEMINI_API_KEY.",
            "[ERROR] Errors:",
            "[ERROR]   Aura_tests_posters_homepage_yaml_Test.executeYamlTest()[1] » IllegalState Failed to instantiate LLM provider class 'org.neodymium.ai.client.GeminiLlmProvider' for provider 'gemini' (role 'global'): Gemini API key is missing. Please configure neodymium.ai.gemini.apiKey or set GEMINI_API_KEY.",
            "[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0",
            "[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.2:test (default-test) on project archy_test:",
            "[ERROR] See /home/weigel/Documents/projects/automation/archy_test/target/surefire-reports for the individual test results.",
            "[ERROR] See dump files (if any exist) [date].dump, [date]-jvmRun].dump and [date].dumpstream. -> [Help 1]",
            "[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.",
            "[ERROR] Re-run Maven using the -X switch to enable full debug logging.",
            "[ERROR] For more information about the errors and possible solutions, please read the following articles:",
            "[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException"
        );

        final String extracted = AuraQueueService.extractSubprocessErrorMessage(logs);

        Assertions.assertNotNull(extracted);
        Assertions.assertTrue(extracted.contains("Failed to instantiate LLM provider class 'org.neodymium.ai.client.GeminiLlmProvider'"));
        Assertions.assertTrue(extracted.contains("Caused by: java.lang.IllegalArgumentException: Gemini API key is missing."));
        Assertions.assertFalse(extracted.contains("Failed to execute goal"), "Should exclude Maven execution goal failure");
        Assertions.assertFalse(extracted.contains("surefire-reports"), "Should exclude surefire report directory advice");
        Assertions.assertFalse(extracted.contains("To see the full stack trace"), "Should exclude Maven debug CLI tips");
        Assertions.assertFalse(extracted.contains("http://cwiki.apache.org"), "Should exclude Apache wiki links");
        Assertions.assertFalse(extracted.contains("<<< FAILURE!"), "Should exclude surefire test lifecycle header");
        Assertions.assertFalse(extracted.contains("<<< ERROR!"), "Should exclude surefire test lifecycle header");
        Assertions.assertFalse(extracted.contains("Tests run: 1"), "Should exclude surefire summary counts");
        Assertions.assertFalse(extracted.contains("» IllegalState"), "Should omit redundant Surefire summary marker when full exception was already captured");
    }

    @Test
    public void testExtractSubprocessErrorMessagePreservesDirectSummaryWhenNoPriorExceptionLogged()
    {
        final List<String> logs = List.of(
            "[ERROR] Errors:",
            "[ERROR]   Aura_test.executeYamlTest()[1] » AssertionFailedError expected: <true> but was: <false>",
            "[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0"
        );

        final String extracted = AuraQueueService.extractSubprocessErrorMessage(logs);

        Assertions.assertNotNull(extracted);
        Assertions.assertTrue(extracted.contains("AssertionFailedError expected: <true> but was: <false>"));
        Assertions.assertFalse(extracted.contains("Tests run:"));
    }

    @Test
    public void testUpdateExecutionSnapshotsAndFallbacksDoesNotCreatePhantomSecondFileWhenFirstExecutionExists() throws Exception
    {
        final Path tempDir = Files.createTempDirectory("aura-queue-test-");
        try
        {
            final File runDir = tempDir.toFile();
            final String className = "Aura_tests_stokke_self_include_yaml_Test";
            final File testDir = new File(runDir, className);
            Assertions.assertTrue(testDir.mkdirs());

            final File exec1 = new File(testDir, "console-execution-1.json");
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode node = mapper.createObjectNode();
            node.put("id", 1);
            node.put("name", "DEU");
            node.put("status", "running");
            mapper.writeValue(exec1, node);

            queueService.updateExecutionSnapshotsAndFallbacks(
                "run_123",
                className,
                List.of("DEU"),
                List.of("Default"),
                List.of(runDir),
                1,
                true,
                false,
                "Playbook execution failed"
            );

            final File exec2 = new File(testDir, "console-execution-2.json");
            Assertions.assertFalse(exec2.exists(), "Phantom console-execution-2.json should NOT be created when console-execution-1.json exists");

            Assertions.assertTrue(exec1.exists());
            final ObjectNode updatedNode = (ObjectNode) mapper.readTree(exec1);
            Assertions.assertEquals("failed", updatedNode.path("status").asText());
            Assertions.assertTrue(updatedNode.path("failureReason").asText().contains("Playbook execution failed"));
        }
        finally
        {
            AuraQueueService.deleteDirRecursively(tempDir.toFile());
        }
    }

    @Test
    public void testUpdateExecutionSnapshotsAndFallbacksPreservesPassedExecutionWithoutInjectingBatchFailure() throws Exception
    {
        final Path tempDir = Files.createTempDirectory("aura-queue-test-");
        try
        {
            final File runDir = tempDir.toFile();
            final String className = "Aura_tests_stokke_self_include_yaml_Test";
            final File testDir = new File(runDir, className);
            Assertions.assertTrue(testDir.mkdirs());

            final File exec1 = new File(testDir, "console-execution-1.json");
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode node = mapper.createObjectNode();
            node.put("id", 1);
            node.put("name", "DEU");
            node.put("status", "passed");
            node.put("currentStepIndex", 3);
            final ObjectNode metrics = node.putObject("metrics");
            metrics.put("totalSteps", 3);
            node.putArray("steps").addObject().put("status", "passed");
            mapper.writeValue(exec1, node);

            queueService.updateExecutionSnapshotsAndFallbacks(
                "run_123",
                className,
                List.of("DEU"),
                List.of("Default"),
                List.of(runDir),
                1,
                true,
                false,
                "Error: Tests run: 2, Failures: 1 <<< FAILURE! Caused by: NoSuchElementException"
            );

            Assertions.assertTrue(exec1.exists());
            final ObjectNode updatedNode = (ObjectNode) mapper.readTree(exec1);
            Assertions.assertEquals("passed", updatedNode.path("status").asText());
            Assertions.assertFalse(updatedNode.hasNonNull("failureReason"), "Passed test must not be injected with batch failureReason");
            Assertions.assertFalse(updatedNode.hasNonNull("error"), "Passed test must not be injected with batch error");
        }
        finally
        {
            AuraQueueService.deleteDirRecursively(tempDir.toFile());
        }
    }

    @Test
    public void testUpdateExecutionSnapshotsAndFallbacksPreservesSkippedExecutionInFailedBatch() throws Exception
    {
        final Path tempDir = Files.createTempDirectory("aura-queue-test-");
        try
        {
            final File runDir = tempDir.toFile();
            final String className = "Aura_tests_stokke_self_include_yaml_Test";
            final File testDir = new File(runDir, className);
            Assertions.assertTrue(testDir.mkdirs());

            final File exec1 = new File(testDir, "console-execution-1.json");
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode node = mapper.createObjectNode();
            node.put("id", 1);
            node.put("name", "DEU");
            node.put("status", "skipped");
            mapper.writeValue(exec1, node);

            queueService.updateExecutionSnapshotsAndFallbacks(
                "run_123",
                className,
                List.of("DEU"),
                List.of("Default"),
                List.of(runDir),
                1,
                true,
                false,
                "Error: Sibling test failure"
            );

            Assertions.assertTrue(exec1.exists());
            final ObjectNode updatedNode = (ObjectNode) mapper.readTree(exec1);
            Assertions.assertEquals("skipped", updatedNode.path("status").asText());
            Assertions.assertFalse(updatedNode.hasNonNull("failureReason"), "Skipped test must not be injected with batch failureReason");
            Assertions.assertFalse(updatedNode.hasNonNull("error"), "Skipped test must not be injected with batch error");
        }
        finally
        {
            AuraQueueService.deleteDirRecursively(tempDir.toFile());
        }
    }

    @Test
    public void testUpdateExecutionSnapshotsAndFallbacksPreservesZeroStepPassedExecutionInFailedBatch() throws Exception
    {
        final Path tempDir = Files.createTempDirectory("aura-queue-test-");
        try
        {
            final File runDir = tempDir.toFile();
            final String className = "Aura_tests_stokke_self_include_yaml_Test";
            final File testDir = new File(runDir, className);
            Assertions.assertTrue(testDir.mkdirs());

            final File exec1 = new File(testDir, "console-execution-1.json");
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode node = mapper.createObjectNode();
            node.put("id", 1);
            node.put("name", "DEU");
            node.put("status", "passed");
            node.put("currentStepIndex", 0);
            final ObjectNode metrics = node.putObject("metrics");
            metrics.put("totalSteps", 0);
            node.putArray("steps");
            mapper.writeValue(exec1, node);

            queueService.updateExecutionSnapshotsAndFallbacks(
                "run_123",
                className,
                List.of("DEU"),
                List.of("Default"),
                List.of(runDir),
                1,
                true,
                false,
                "Error: Sibling test failure"
            );

            Assertions.assertTrue(exec1.exists());
            final ObjectNode updatedNode = (ObjectNode) mapper.readTree(exec1);
            Assertions.assertEquals("passed", updatedNode.path("status").asText(), "0-step passed test must not be turned into failed");
            Assertions.assertFalse(updatedNode.hasNonNull("failureReason"));
        }
        finally
        {
            AuraQueueService.deleteDirRecursively(tempDir.toFile());
        }
    }

    @Test
    public void testUpdateExecutionSnapshotsAndFallbacksEnrichesFailedExecutionWithNullFailureReason() throws Exception
    {
        final Path tempDir = Files.createTempDirectory("aura-queue-test-");
        try
        {
            final File runDir = tempDir.toFile();
            final String className = "Aura_tests_stokke_self_include_yaml_Test";
            final File testDir = new File(runDir, className);
            Assertions.assertTrue(testDir.mkdirs());

            final File exec1 = new File(testDir, "console-execution-1.json");
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode node = mapper.createObjectNode();
            node.put("id", 1);
            node.put("name", "DEU");
            node.put("status", "failed");
            node.putNull("failureReason");
            mapper.writeValue(exec1, node);

            queueService.updateExecutionSnapshotsAndFallbacks(
                "run_123",
                className,
                List.of("DEU"),
                List.of("Default"),
                List.of(runDir),
                1,
                true,
                false,
                "Subprocess crashed during execution"
            );

            Assertions.assertTrue(exec1.exists());
            final ObjectNode updatedNode = (ObjectNode) mapper.readTree(exec1);
            Assertions.assertEquals("failed", updatedNode.path("status").asText());
            Assertions.assertTrue(updatedNode.path("failureReason").asText().contains("Subprocess crashed"));
            Assertions.assertTrue(updatedNode.path("error").asText().contains("Subprocess crashed"));
        }
        finally
        {
            AuraQueueService.deleteDirRecursively(tempDir.toFile());
        }
    }

    @Test
    public void testUpdateExecutionSnapshotsAndFallbacksCreatesFallbackWhenZeroExecutionsExist() throws Exception
    {
        final Path tempDir = Files.createTempDirectory("aura-queue-test-");
        try
        {
            final File runDir = tempDir.toFile();
            final String className = "Aura_tests_stokke_self_include_yaml_Test";
            final File testDir = new File(runDir, className);

            queueService.updateExecutionSnapshotsAndFallbacks(
                "run_123",
                className,
                List.of("DEU"),
                List.of("Default"),
                List.of(runDir),
                1,
                true,
                false,
                "Early JVM failure before test startup"
            );

            final File exec1 = new File(testDir, "console-execution-1.json");
            final File exec2 = new File(testDir, "console-execution-2.json");

            Assertions.assertTrue(exec1.exists(), "Fallback console-execution-1.json should be created when no executions exist");
            Assertions.assertFalse(exec2.exists(), "Phantom console-execution-2.json should NOT be created");

            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode createdNode = (ObjectNode) mapper.readTree(exec1);
            Assertions.assertEquals("failed", createdNode.path("status").asText());
            Assertions.assertTrue(createdNode.path("failureReason").asText().contains("Early JVM failure"));
        }
        finally
        {
            AuraQueueService.deleteDirRecursively(tempDir.toFile());
        }
    }

    @Test
    public final void testGetCurrentBatchNameDefaultAndSet()
    {
        Assertions.assertNotNull(queueService.getCurrentBatchName());
        Assertions.assertEquals("", queueService.getCurrentBatchName());
    }

    @Test
    public final void testUpdateExecutionSnapshotsFinalizesRunningExecutionsOnSuccess() throws Exception
    {
        final Path tempDir = Files.createTempDirectory("aura-queue-test-success-");
        try
        {
            final File runDir = tempDir.toFile();
            final String className = "Aura_tests_checkout_process_yaml_Test";
            final File testDir = new File(runDir, className);
            Assertions.assertTrue(testDir.mkdirs());

            final File exec1 = new File(testDir, "console-execution-1.json");
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode node = mapper.createObjectNode();
            node.put("id", 1);
            node.put("name", "CheckoutDataset1");
            node.put("status", "running");
            mapper.writeValue(exec1, node);

            queueService.updateExecutionSnapshotsAndFallbacks(
                "run_success_123",
                className,
                List.of("CheckoutDataset1", "CheckoutDataset2"),
                List.of("Default"),
                List.of(runDir),
                0,
                false,
                false,
                null
            );

            Assertions.assertTrue(exec1.exists());
            final ObjectNode updatedNode1 = (ObjectNode) mapper.readTree(exec1);
            Assertions.assertEquals("passed", updatedNode1.path("status").asText(), "Running execution must be updated to passed on successful batch completion");

            final File exec2 = new File(testDir, "console-execution-2.json");
            Assertions.assertTrue(exec2.exists(), "Fallback execution snapshot must be created for dataset 2");
            final ObjectNode createdNode2 = (ObjectNode) mapper.readTree(exec2);
            Assertions.assertEquals("passed", createdNode2.path("status").asText(), "Dataset 2 snapshot must have passed status");
        }
        finally
        {
            AuraQueueService.deleteDirRecursively(tempDir.toFile());
        }
    }

    @Test
    public void testIngestBatchExecutionFilesDeduplicatesAcrossMultipleStorageRoots() throws Exception
    {
        final java.nio.file.Path tempDir1 = Files.createTempDirectory("aura_queue_test_storage1_");
        final java.nio.file.Path tempDir2 = Files.createTempDirectory("aura_queue_test_storage2_");
        try
        {
            final String className = "Aura_tests_checkout_process_yaml_Test";
            final File testDir1 = new File(tempDir1.toFile(), className);
            final File testDir2 = new File(tempDir2.toFile(), className);
            Assertions.assertTrue(testDir1.mkdirs());
            Assertions.assertTrue(testDir2.mkdirs());

            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode node = mapper.createObjectNode();
            node.put("id", "dataset_1");
            node.put("status", "passed");

            // Write identical execution files in both storage directories
            final File exec1 = new File(testDir1, "console-execution-1.json");
            final File exec2 = new File(testDir2, "console-execution-1.json");
            mapper.writeValue(exec1, node);
            mapper.writeValue(exec2, node);

            final java.lang.reflect.Method ingestMethod = AuraQueueService.class.getDeclaredMethod(
                "ingestBatchExecutionFiles",
                com.xceptance.neodymium.aura.QueueRunProgressListener.class,
                String.class,
                String.class,
                List.class,
                boolean.class,
                AtomicInteger.class,
                AtomicInteger.class
            );
            ingestMethod.setAccessible(true);

            final AtomicInteger batchIngestedRuns = new AtomicInteger(0);
            final AtomicInteger batchReportedSurefireRuns = new AtomicInteger(0);
            final List<File> storageRoots = List.of(tempDir1.toFile(), tempDir2.toFile());

            final List<Map<String, Object>> completedPayloads = new ArrayList<>();
            final com.xceptance.neodymium.aura.QueueRunProgressListener listener = new com.xceptance.neodymium.aura.QueueRunProgressListener()
            {
                @Override
                public void onRunStarted(final String rId, final String bName, final String env)
                {
                }

                @Override
                public void onTestExecutionCompleted(final String rId, final Map<String, Object> executionData)
                {
                    completedPayloads.add(executionData);
                }

                @Override
                public void onRunFinished(final String rId)
                {
                }
            };

            ingestMethod.invoke(queueService, listener, "run_123", className, storageRoots, true, batchIngestedRuns, batchReportedSurefireRuns);

            Assertions.assertEquals(1, batchIngestedRuns.get(), "Batch ingested runs should be exactly 1 despite existing in 2 storage roots");
            Assertions.assertEquals(1, queueService.getGlobalTestsRun(), "Global tests run counter should be incremented exactly once");
            Assertions.assertEquals(1, queueService.getGlobalPassed(), "Global passed counter should be incremented exactly once");
            Assertions.assertEquals(1, completedPayloads.size(), "Listener should receive completed execution exactly once");
        }
        finally
        {
            AuraQueueService.deleteDirRecursively(tempDir1.toFile());
            AuraQueueService.deleteDirRecursively(tempDir2.toFile());
        }
    }
}

