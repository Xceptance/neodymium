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
package org.neodymium.ai.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ConsoleLogMonitor} covering severity thresholds, exception stack
 * formatting, circular buffer capacity, draining, and thread safety.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class ConsoleLogMonitorTest
{
    @Test
    public void testBufferCapacityAndEviction()
    {
        final ConsoleLogMonitor monitor = new ConsoleLogMonitor(5);

        for (int i = 1; i <= 8; i++)
        {
            monitor.recordEntry("INFO", "Log message " + i, "app.js", i, null);
        }

        assertEquals(5, monitor.size(), "Buffer size must not exceed capacity of 5");

        final List<ConsoleLogEntry> logs = monitor.getLogs("INFO", 10, false);
        assertEquals(5, logs.size());
        assertEquals("Log message 4", logs.get(0).message(), "Oldest entries 1..3 should have been evicted");
        assertEquals("Log message 8", logs.get(4).message());
    }

    @Test
    public void testSeverityFilteringSevere()
    {
        final ConsoleLogMonitor monitor = new ConsoleLogMonitor(10);
        monitor.recordEntry("INFO", "Informational startup", "main.js", 1, null);
        monitor.recordEntry("WARNING", "Deprecated feature used", "main.js", 10, null);
        monitor.recordEntry("SEVERE", "Uncaught TypeError", "main.js", 25, "TypeError: null");

        final List<ConsoleLogEntry> severeOnly = monitor.getLogs("SEVERE", 10, false);
        assertEquals(1, severeOnly.size());
        assertEquals("SEVERE", severeOnly.get(0).level());
        assertEquals("Uncaught TypeError", severeOnly.get(0).message());
    }

    @Test
    public void testSeverityFilteringWarning()
    {
        final ConsoleLogMonitor monitor = new ConsoleLogMonitor(10);
        monitor.recordEntry("INFO", "Informational startup", "main.js", 1, null);
        monitor.recordEntry("WARNING", "Deprecated feature used", "main.js", 10, null);
        monitor.recordEntry("SEVERE", "Uncaught TypeError", "main.js", 25, "TypeError: null");

        final List<ConsoleLogEntry> warnAndAbove = monitor.getLogs("WARNING", 10, false);
        assertEquals(2, warnAndAbove.size());
        assertTrue(warnAndAbove.stream().allMatch(l -> l.isSevere() || l.isWarning()));
    }

    @Test
    public void testSeverityFilteringInfo()
    {
        final ConsoleLogMonitor monitor = new ConsoleLogMonitor(10);
        monitor.recordEntry("INFO", "Informational startup", "main.js", 1, null);
        monitor.recordEntry("WARNING", "Deprecated feature used", "main.js", 10, null);
        monitor.recordEntry("SEVERE", "Uncaught TypeError", "main.js", 25, "TypeError: null");

        final List<ConsoleLogEntry> allLogs = monitor.getLogs("INFO", 10, false);
        assertEquals(3, allLogs.size());
    }

    @Test
    public void testUnhandledRuntimeExceptionStackFormatting()
    {
        final ConsoleLogMonitor monitor = new ConsoleLogMonitor(10);
        final String stack = "TypeError: Cannot read properties of undefined (reading 'item')\n" +
            "    at renderCart (cart.js:42:15)\n" +
            "    at init (app.js:10:5)";

        monitor.recordException("Uncaught TypeError: Cannot read properties of undefined", "cart.js", 42, stack);

        final List<ConsoleLogEntry> logs = monitor.getLogs("SEVERE", 10, false);
        assertEquals(1, logs.size());

        final ConsoleLogEntry entry = logs.get(0);
        assertEquals("SEVERE", entry.level());
        assertEquals("cart.js", entry.source());
        assertEquals(42, entry.lineNumber());
        assertNotNull(entry.stackTrace());
        assertTrue(entry.stackTrace().contains("at renderCart (cart.js:42:15)"));
    }

    @Test
    public void testBufferDrainingOnDemand()
    {
        final ConsoleLogMonitor monitor = new ConsoleLogMonitor(10);
        monitor.recordEntry("SEVERE", "Error 1", "script.js", 1, null);
        monitor.recordEntry("SEVERE", "Error 2", "script.js", 2, null);

        assertEquals(2, monitor.size());

        final List<ConsoleLogEntry> drained = monitor.getLogs("SEVERE", 10, true);
        assertEquals(2, drained.size());
        assertEquals(0, monitor.size(), "Buffer should be drained when clearBuffer=true");

        final List<ConsoleLogEntry> subsequent = monitor.getLogs("SEVERE", 10, false);
        assertTrue(subsequent.isEmpty(), "Subsequent fetch should be empty");
    }

    @Test
    public void testLimitEnforcement()
    {
        final ConsoleLogMonitor monitor = new ConsoleLogMonitor(20);
        for (int i = 1; i <= 10; i++)
        {
            monitor.recordEntry("SEVERE", "Error " + i, "script.js", i, null);
        }

        final List<ConsoleLogEntry> limited = monitor.getLogs("SEVERE", 4, false);
        assertEquals(4, limited.size());
        assertEquals("Error 1", limited.get(0).message());
    }

    @Test
    public void testMultiThreadedConcurrencySafety() throws InterruptedException
    {
        final int capacity = 50;
        final ConsoleLogMonitor monitor = new ConsoleLogMonitor(capacity);
        final int writerThreads = 8;
        final int recordsPerThread = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(writerThreads + 4);
        final CountDownLatch latch = new CountDownLatch(writerThreads + 4);
        final AtomicInteger errors = new AtomicInteger(0);

        for (int t = 0; t < writerThreads; t++)
        {
            final int threadId = t;
            executor.submit(() ->
            {
                try
                {
                    for (int i = 0; i < recordsPerThread; i++)
                    {
                        monitor.recordEntry("WARNING", "Thread " + threadId + " warning " + i, "app.js", i, null);
                    }
                }
                catch (final Exception e)
                {
                    errors.incrementAndGet();
                }
                finally
                {
                    latch.countDown();
                }
            });
        }

        for (int r = 0; r < 4; r++)
        {
            executor.submit(() ->
            {
                try
                {
                    for (int i = 0; i < 50; i++)
                    {
                        final List<ConsoleLogEntry> list = monitor.getLogs("INFO", 10, i % 2 == 0);
                        assertFalse(list.size() > 10);
                        Thread.sleep(1);
                    }
                }
                catch (final Exception e)
                {
                    errors.incrementAndGet();
                }
                finally
                {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent test must complete within timeout");
        executor.shutdown();
        assertEquals(0, errors.get(), "Zero concurrency exceptions expected");
        assertTrue(monitor.size() <= capacity, "Buffer size must not exceed capacity under high concurrency");
    }
}
