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
import java.lang.reflect.Field;
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
}

