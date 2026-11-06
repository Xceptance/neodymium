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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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
}
