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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
}

