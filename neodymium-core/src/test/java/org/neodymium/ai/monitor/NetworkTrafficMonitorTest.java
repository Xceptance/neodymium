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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NetworkTrafficMonitor} covering bounded circular buffering,
 * multi-threaded concurrency safety, status filtering, URL regex exclusion, and buffer draining.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class NetworkTrafficMonitorTest
{
    @Test
    public void testCircularBufferCapacityAndEviction()
    {
        final NetworkTrafficMonitor monitor = new NetworkTrafficMonitor(5, ".*(analytics).*");

        for (int i = 1; i <= 8; i++)
        {
            monitor.recordFailure("https://example.com/api/" + i, 500, "GET", "Error " + i);
        }

        assertEquals(5, monitor.size(), "Buffer size must not exceed capacity of 5");

        final List<NetworkFailure> failures = monitor.getFailures("all_failures", 10, false);
        assertEquals(5, failures.size());
        assertEquals("https://example.com/api/4", failures.get(0).url(), "Oldest entries 1..3 should have been evicted");
        assertEquals("https://example.com/api/8", failures.get(4).url());
    }

    @Test
    public void testStatusFilteringAllFailures()
    {
        final NetworkTrafficMonitor monitor = new NetworkTrafficMonitor(10, null);
        monitor.recordFailure("https://example.com/api/1", 404, "GET", "Not Found");
        monitor.recordFailure("https://example.com/api/2", 500, "POST", "Server Error");
        monitor.recordFailure("https://example.com/api/3", 0, "GET", "Blocked/Aborted");

        final List<NetworkFailure> failures = monitor.getFailures("all_failures", 10, false);
        assertEquals(3, failures.size());
    }

    @Test
    public void testStatusFiltering4xx()
    {
        final NetworkTrafficMonitor monitor = new NetworkTrafficMonitor(10, null);
        monitor.recordFailure("https://example.com/api/400", 400, "POST", "Bad Request");
        monitor.recordFailure("https://example.com/api/404", 404, "GET", "Not Found");
        monitor.recordFailure("https://example.com/api/500", 500, "GET", "Internal Error");
        monitor.recordFailure("https://example.com/api/blocked", 0, "GET", "Aborted");

        final List<NetworkFailure> failures = monitor.getFailures("4xx", 10, false);
        assertEquals(2, failures.size());
        assertTrue(failures.stream().allMatch(NetworkFailure::is4xx));
    }

    @Test
    public void testStatusFiltering5xx()
    {
        final NetworkTrafficMonitor monitor = new NetworkTrafficMonitor(10, null);
        monitor.recordFailure("https://example.com/api/404", 404, "GET", "Not Found");
        monitor.recordFailure("https://example.com/api/500", 500, "GET", "Internal Error");
        monitor.recordFailure("https://example.com/api/503", 503, "POST", "Service Unavailable");
        monitor.recordFailure("https://example.com/api/blocked", 0, "GET", "Aborted");

        final List<NetworkFailure> failures = monitor.getFailures("5xx", 10, false);
        assertEquals(2, failures.size());
        assertTrue(failures.stream().allMatch(NetworkFailure::is5xx));
    }

    @Test
    public void testStatusFilteringBlocked()
    {
        final NetworkTrafficMonitor monitor = new NetworkTrafficMonitor(10, null);
        monitor.recordFailure("https://example.com/api/404", 404, "GET", "Not Found");
        monitor.recordFailure("https://example.com/api/500", 500, "GET", "Internal Error");
        monitor.recordFailure("https://example.com/api/cors", 0, "GET", "CORS Rejection");
        monitor.recordFailure("https://example.com/api/net-abort", -1, "POST", "Connection Aborted");

        final List<NetworkFailure> failures = monitor.getFailures("blocked", 10, false);
        assertEquals(2, failures.size());
        assertTrue(failures.stream().allMatch(NetworkFailure::isBlocked));
    }

    @Test
    public void testUrlRegexExclusion()
    {
        final NetworkTrafficMonitor monitor = new NetworkTrafficMonitor(10, ".*(analytics|telemetry|favicon\\.ico).*");
        monitor.recordFailure("https://example.com/telemetry/event", 500, "POST", "Failed Telemetry");
        monitor.recordFailure("https://example.com/google-analytics.js", 404, "GET", "Missing script");
        monitor.recordFailure("https://example.com/favicon.ico", 404, "GET", "Missing icon");
        monitor.recordFailure("https://example.com/api/checkout", 500, "POST", "Payment Failed");

        assertEquals(1, monitor.size(), "Ignored URLs must not be stored in the failure buffer");
        final List<NetworkFailure> failures = monitor.getFailures("all_failures", 10, false);
        assertEquals(1, failures.size());
        assertEquals("https://example.com/api/checkout", failures.get(0).url());
    }

    @Test
    public void testBufferClearing()
    {
        final NetworkTrafficMonitor monitor = new NetworkTrafficMonitor(10, null);
        monitor.recordFailure("https://example.com/api/1", 500, "GET", "Error 1");
        monitor.recordFailure("https://example.com/api/2", 500, "GET", "Error 2");

        assertEquals(2, monitor.size());

        final List<NetworkFailure> firstFetch = monitor.getFailures("all_failures", 10, true);
        assertEquals(2, firstFetch.size());
        assertEquals(0, monitor.size(), "Buffer should be drained when clearBuffer=true");

        final List<NetworkFailure> secondFetch = monitor.getFailures("all_failures", 10, false);
        assertTrue(secondFetch.isEmpty(), "Subsequent call should return empty list");
    }

    @Test
    public void testLimitEnforcement()
    {
        final NetworkTrafficMonitor monitor = new NetworkTrafficMonitor(20, null);
        for (int i = 1; i <= 10; i++)
        {
            monitor.recordFailure("https://example.com/api/" + i, 500, "GET", "Error " + i);
        }

        final List<NetworkFailure> limited = monitor.getFailures("all_failures", 3, false);
        assertEquals(3, limited.size());
        assertEquals("https://example.com/api/1", limited.get(0).url());
    }

    @Test
    public void testMultiThreadedConcurrencySafety() throws InterruptedException
    {
        final int capacity = 50;
        final NetworkTrafficMonitor monitor = new NetworkTrafficMonitor(capacity, ".*(analytics).*");
        final int writerThreads = 8;
        final int recordsPerThread = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(writerThreads + 4);
        final CountDownLatch latch = new CountDownLatch(writerThreads + 4);
        final AtomicInteger errors = new AtomicInteger(0);

        // Writers
        for (int t = 0; t < writerThreads; t++)
        {
            final int threadId = t;
            executor.submit(() ->
            {
                try
                {
                    for (int i = 0; i < recordsPerThread; i++)
                    {
                        monitor.recordFailure("https://example.com/t" + threadId + "/api/" + i, 500, "GET", "Err");
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

        // Concurrent Readers / Drainers
        for (int r = 0; r < 4; r++)
        {
            executor.submit(() ->
            {
                try
                {
                    for (int i = 0; i < 50; i++)
                    {
                        final List<NetworkFailure> list = monitor.getFailures("all_failures", 10, i % 2 == 0);
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
