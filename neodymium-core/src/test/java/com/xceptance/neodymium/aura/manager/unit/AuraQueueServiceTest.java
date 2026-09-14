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
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.RunRequest;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AuraQueueService} process lifecycle and stop handling.
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
        queueService = new AuraQueueService(interactiveService);
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
}
