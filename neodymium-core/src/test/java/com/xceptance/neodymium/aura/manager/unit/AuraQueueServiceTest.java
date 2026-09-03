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

import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.AuraReportingService;
import com.xceptance.neodymium.aura.QueueRunProgressListener;
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
