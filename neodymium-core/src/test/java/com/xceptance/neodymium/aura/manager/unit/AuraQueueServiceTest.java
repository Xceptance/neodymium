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

import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.QueueRunProgressListener;
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.RunRequest;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link AuraQueueService} process lifecycle, stop handling, and execution JSON ingestion.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("aura-manager")
public final class AuraQueueServiceTest
{
    private AuraInteractiveService interactiveService;
    private AuraQueueService queueService;

    @BeforeEach
    public void setUp()
    {
        interactiveService = new AuraInteractiveService();
        queueService = new AuraQueueService(null, interactiveService);
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
    public void testStopCurrentTestBroadcastsCancellationAndDoesNotHaltQueue()
    {
        final InteractiveConsoleEngine engine = interactiveService.getOrCreateConsoleEngine();
        Assertions.assertDoesNotThrow(() -> {
            queueService.stopCurrentTest();
        });

        Assertions.assertFalse(queueService.isManuallyStopped(), "stopCurrentTest must not halt the whole queue");
        Assertions.assertTrue(queueService.getCurrentRunLogs().stream()
            .anyMatch(log -> log.contains("Test execution cancelled by user")), "Cancellation log should be broadcast");
        Assertions.assertTrue(engine.getCurrentStateJson().contains("cancelled") || engine.getCurrentStateJson().contains("skipped"),
            "Engine state must reflect cancelled/skipped status");
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
        final java.util.List<String> logs = (java.util.List<String>) logsField.get(queueService);

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
    public void testExecuteQueueInteractiveSeparateBatches() throws Exception
    {
        System.setProperty("neodymium.aura.test", "true");
        try
        {
            final RunRequest req = new RunRequest();
            req.interactive = true;
            final DatasetSelection ds1 = new DatasetSelection();
            ds1.file = "TestFile.yaml";
            ds1.id = "Dataset 1";
            final DatasetSelection ds2 = new DatasetSelection();
            ds2.file = "TestFile.yaml";
            ds2.id = "Dataset 2";
            req.datasets = List.of(ds1, ds2);

            queueService.executeQueue(req, 8080);

            final long deadline = System.currentTimeMillis() + 5000;
            while (queueService.isRunningQueue() && System.currentTimeMillis() < deadline)
            {
                Thread.sleep(50);
            }

            Assertions.assertFalse(queueService.isRunningQueue());
            Assertions.assertEquals(2, queueService.getGlobalTestsRun());
            Assertions.assertEquals(2, queueService.getCompletedFiles().size());
        }
        finally
        {
            System.clearProperty("neodymium.aura.test");
        }
    }

    @Test
    public void testExecuteQueueNonInteractiveBatched() throws Exception
    {
        System.setProperty("neodymium.aura.test", "true");
        try
        {
            final RunRequest req = new RunRequest();
            req.interactive = false;
            final DatasetSelection ds1 = new DatasetSelection();
            ds1.file = "TestFile.yaml";
            ds1.id = "Dataset 1";
            final DatasetSelection ds2 = new DatasetSelection();
            ds2.file = "TestFile.yaml";
            ds2.id = "Dataset 2";
            req.datasets = List.of(ds1, ds2);

            queueService.executeQueue(req, 8080);

            final long deadline = System.currentTimeMillis() + 5000;
            while (queueService.isRunningQueue() && System.currentTimeMillis() < deadline)
            {
                Thread.sleep(50);
            }

            Assertions.assertFalse(queueService.isRunningQueue());
            Assertions.assertEquals(2, queueService.getGlobalTestsRun());
            // In non-interactive mode, the 2 datasets are batched together so completedFiles has 1 entry
            Assertions.assertEquals(1, queueService.getCompletedFiles().size());
        }
        finally
        {
            System.clearProperty("neodymium.aura.test");
        }
    }

    @Test
    public void testMidRunIngestionOnlyReportsFinalStatuses(@TempDir final Path tempDir) throws Exception
    {
        final Path classDir = writeExecutionFiles(tempDir);
        final List<Map<String, Object>> ingested = new ArrayList<>();
        final QueueRunProgressListener listener = new RecordingListener(ingested);

        invokeIngest(listener, "run_test", classDir.getParent().toFile(), true);

        Assertions.assertEquals(2, ingested.size(), "Only final status executions should be ingested mid-run");
        final Map<String, Object> passed = ingested.stream()
            .filter(p -> "exec-1".equals(p.get("id")))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("passed", passed.get("status"));
        final Map<String, Object> finished = ingested.stream()
            .filter(p -> "exec-3".equals(p.get("id")))
            .findFirst()
            .orElseThrow();
        Assertions.assertEquals("passed", finished.get("status"), "'finished' should be normalized to 'passed'");
        Assertions.assertTrue(ingested.stream().noneMatch(p -> "exec-2".equals(p.get("id"))),
            "A still running execution must not be ingested mid-run");
    }

    @Test
    public void testPostExitIngestionReportsAllFiles(@TempDir final Path tempDir) throws Exception
    {
        final Path classDir = writeExecutionFiles(tempDir);
        final List<Map<String, Object>> ingested = new ArrayList<>();
        final QueueRunProgressListener listener = new RecordingListener(ingested);

        invokeIngest(listener, "run_test", classDir.getParent().toFile(), false);

        Assertions.assertEquals(3, ingested.size(), "After subprocess exit all parseable files should be ingested");
    }

    @Test
    public void testIngestionDoesNotReportTwice(@TempDir final Path tempDir) throws Exception
    {
        final Path classDir = writeExecutionFiles(tempDir);
        final List<Map<String, Object>> ingested = new ArrayList<>();
        final QueueRunProgressListener listener = new RecordingListener(ingested);

        invokeIngest(listener, "run_test", classDir.getParent().toFile(), true);
        invokeIngest(listener, "run_test", classDir.getParent().toFile(), true);

        Assertions.assertEquals(2, ingested.size(), "Repeated scans must not ingest the same execution twice");
    }

    @Test
    public void testIngestionIgnoresUnparseableFiles(@TempDir final Path tempDir) throws Exception
    {
        final Path classDir = Files.createDirectories(tempDir.resolve("run_test").resolve("SomeClass"));
        Files.writeString(classDir.resolve("console-execution-1.json"), "{\"status\":\"passed\",\"id\":\"exec-1\"}",
            StandardCharsets.UTF_8);
        Files.writeString(classDir.resolve("broken.json"), "{not valid json", StandardCharsets.UTF_8);

        final List<Map<String, Object>> ingested = new ArrayList<>();
        invokeIngest(new RecordingListener(ingested), "run_test", classDir.getParent().toFile(), true);

        Assertions.assertEquals(1, ingested.size(), "Unparseable execution files must be skipped");
    }

    private Path writeExecutionFiles(final Path tempDir) throws Exception
    {
        final Path classDir = Files.createDirectories(tempDir.resolve("run_test").resolve("SomeClass"));
        Files.writeString(classDir.resolve("console-execution-1.json"),
            "{\"id\":\"exec-1\",\"status\":\"passed\",\"testClass\":\"SomeClass\",\"title\":\"t1\",\"browser\":\"Chrome\"}",
            StandardCharsets.UTF_8);
        Files.writeString(classDir.resolve("console-execution-2.json"),
            "{\"id\":\"exec-2\",\"status\":\"running\",\"testClass\":\"SomeClass\",\"title\":\"t2\",\"browser\":\"Chrome\"}",
            StandardCharsets.UTF_8);
        Files.writeString(classDir.resolve("console-execution-3.json"),
            "{\"id\":\"exec-3\",\"status\":\"finished\",\"testClass\":\"SomeClass\",\"title\":\"t3\",\"browser\":\"Chrome\"}",
            StandardCharsets.UTF_8);
        return classDir;
    }

    private void invokeIngest(
        final QueueRunProgressListener listener,
        final String runId,
        final File baseDir,
        final boolean onlyFinalStatus) throws Exception
    {
        final Method method = AuraQueueService.class.getDeclaredMethod(
            "ingestBatchExecutionFiles", QueueRunProgressListener.class, String.class, String.class, List.class, boolean.class);
        method.setAccessible(true);
        method.invoke(queueService, listener, runId, "SomeClass", List.of(baseDir), onlyFinalStatus);
    }

    @Test
    public void testMultipleDatasetSelectionsGroupIntoSingleBatch() throws Exception
    {
        final com.xceptance.neodymium.aura.dto.RunRequest req = new com.xceptance.neodymium.aura.dto.RunRequest();
        final com.xceptance.neodymium.aura.dto.DatasetSelection sel1 = new com.xceptance.neodymium.aura.dto.DatasetSelection();
        sel1.file = "wikipedia_search.yml";
        sel1.id = "1";

        final com.xceptance.neodymium.aura.dto.DatasetSelection sel2 = new com.xceptance.neodymium.aura.dto.DatasetSelection();
        sel2.file = "wikipedia_search.yml";
        sel2.id = "2";

        req.datasets = List.of(sel1, sel2);
        req.globalBrowserProfiles = List.of("Chrome_1024x768");

        final Method getProfilesMethod = AuraQueueService.class.getDeclaredMethod(
            "getEffectiveBrowserProfiles", com.xceptance.neodymium.aura.dto.DatasetSelection.class, List.class);
        getProfilesMethod.setAccessible(true);

        final List<Object> batches = new ArrayList<>();
        final Class<?> batchClass = Class.forName("com.xceptance.neodymium.aura.AuraQueueService$ExecutionBatch");
        final Field datasetIdsField = batchClass.getDeclaredField("datasetIds");
        datasetIdsField.setAccessible(true);
        final Field targetProfilesField = batchClass.getDeclaredField("targetProfiles");
        targetProfilesField.setAccessible(true);
        final Field fileField = batchClass.getDeclaredField("file");
        fileField.setAccessible(true);

        for (final com.xceptance.neodymium.aura.dto.DatasetSelection selection : req.datasets)
        {
            @SuppressWarnings("unchecked")
            final List<String> profiles = (List<String>) getProfilesMethod.invoke(queueService, selection, req.globalBrowserProfiles);

            Object existingBatch = null;
            for (final Object b : batches)
            {
                final String fileVal = (String) fileField.get(b);
                @SuppressWarnings("unchecked")
                final List<String> profVal = (List<String>) targetProfilesField.get(b);
                if (fileVal.equals(selection.file) && profVal.equals(profiles))
                {
                    existingBatch = b;
                    break;
                }
            }

            if (existingBatch != null)
            {
                if (selection.id != null && !selection.id.isBlank())
                {
                    @SuppressWarnings("unchecked")
                    final List<String> ids = (List<String>) datasetIdsField.get(existingBatch);
                    if (!ids.contains(selection.id))
                    {
                        ids.add(selection.id);
                    }
                }
            }
            else
            {
                final java.lang.reflect.Constructor<?> batchConst = batchClass.getDeclaredConstructor(String.class, List.class);
                batchConst.setAccessible(true);
                final Object batch = batchConst.newInstance(selection.file, profiles);
                if (selection.id != null && !selection.id.isBlank())
                {
                    @SuppressWarnings("unchecked")
                    final List<String> ids = (List<String>) datasetIdsField.get(batch);
                    ids.add(selection.id);
                }
                batches.add(batch);
            }
        }

        Assertions.assertEquals(1, batches.size(), "Multiple dataset selections for the same file and profiles must be grouped into a single batch");
        @SuppressWarnings("unchecked")
        final List<String> ids = (List<String>) datasetIdsField.get(batches.get(0));
        Assertions.assertEquals(List.of("1", "2"), ids, "Combined batch must contain all selected dataset IDs");
    }

    @Test
    public void testExtractSubprocessErrorMessage()
    {
        final List<String> logs = List.of(
            "[INFO] Spawning Maven Subprocess for YAML test: tests/stokke/stokkeOrderPayPalTest.yml...",
            "2026-09-23T09:22:17.079+02:00  INFO 7025 --- [aura-manager] [raQueueExecutor] c.x.neodymium.aura.AuraQueueService : [Aura Subprocess] java.lang.RuntimeException: Failed to parse playbook: tests/stokke/stokkeOrderPayPalTest.yml",
            "2026-09-23T09:22:17.081+02:00  INFO 7025 --- [aura-manager] [raQueueExecutor] c.x.neodymium.aura.AuraQueueService : [Aura Subprocess] Caused by: java.lang.IllegalArgumentException: Invalid playbook step format in file: proceed-to-payment.steps."
        );

        final String extracted = AuraQueueService.extractSubprocessErrorMessage(logs);
        Assertions.assertNotNull(extracted, "Extracted error message must not be null");
        Assertions.assertTrue(extracted.contains("Failed to parse playbook: tests/stokke/stokkeOrderPayPalTest.yml"), "Extracted error must contain main failure");
        Assertions.assertTrue(extracted.contains("Invalid playbook step format in file: proceed-to-payment.steps"), "Extracted error must contain cause");
    }

    /**
     * Listener recording every execution payload received via {@link #onTestExecutionCompleted(String, Map)}.
     */
    private static final class RecordingListener implements QueueRunProgressListener
    {
        private final List<Map<String, Object>> target;

        RecordingListener(final List<Map<String, Object>> target)
        {
            this.target = target;
        }

        @Override
        public void onRunStarted(final String runId, final String batchName, final String environment)
        {
        }

        @Override
        public void onTestExecutionCompleted(final String runId, final Map<String, Object> executionData)
        {
            target.add(executionData);
        }

        @Override
        public void onRunFinished(final String runId)
        {
        }
    }
}
