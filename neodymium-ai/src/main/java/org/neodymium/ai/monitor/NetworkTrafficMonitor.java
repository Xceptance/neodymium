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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.neodymium.ai.config.AiConfiguration;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.latest.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe, bounded in-memory circular monitor capturing protocol-level network failures
 * (HTTP 4xx, 5xx, network aborts, and CORS errors) via Chrome DevTools Protocol (CDP)
 * and JavaScript Resource Timing API fallback.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class NetworkTrafficMonitor
{
    private static final Logger LOG = LoggerFactory.getLogger(NetworkTrafficMonitor.class);

    public static final int DEFAULT_CAPACITY = 100;

    private final int capacity;
    private final Pattern compiledIgnorePattern;
    private final ArrayDeque<NetworkFailure> buffer;
    private final boolean enabled;

    /**
     * Creates a new monitor with default capacity and pattern from {@link AiConfiguration}.
     */
    public NetworkTrafficMonitor()
    {
        this(DEFAULT_CAPACITY, AiConfiguration.getInstance().getNetworkIgnorePattern(), AiConfiguration.getInstance().isNetworkMonitorEnabled());
    }

    /**
     * Creates a new monitor with custom capacity and ignore pattern.
     *
     * @param capacity the maximum number of network failures to store
     * @param ignorePattern regular expression matching URLs to exclude, or null/blank for none
     */
    public NetworkTrafficMonitor(final int capacity, final String ignorePattern)
    {
        this(capacity, ignorePattern, true);
    }

    /**
     * Creates a new monitor with custom capacity, ignore pattern, and enabled flag.
     *
     * @param capacity the maximum number of network failures to store
     * @param ignorePattern regular expression matching URLs to exclude, or null/blank for none
     * @param enabled whether monitoring is active
     */
    public NetworkTrafficMonitor(final int capacity, final String ignorePattern, final boolean enabled)
    {
        this.capacity = Math.max(1, capacity);
        this.enabled = enabled;
        this.compiledIgnorePattern = (ignorePattern != null && !ignorePattern.isBlank())
            ? Pattern.compile(ignorePattern.trim(), Pattern.CASE_INSENSITIVE)
            : null;
        this.buffer = new ArrayDeque<>(this.capacity);
    }

    /**
     * Records a network failure into the bounded circular buffer.
     * Oldest entries are evicted once capacity is exceeded.
     *
     * @param url the requested URL
     * @param statusCode HTTP response status code (or 0 for aborted/blocked)
     * @param method HTTP method (GET, POST, etc.)
     * @param errorDescription description or status text
     */
    public synchronized void recordFailure(final String url, final int statusCode, final String method, final String errorDescription)
    {
        if (!enabled || url == null)
        {
            return;
        }

        if (compiledIgnorePattern != null && compiledIgnorePattern.matcher(url).matches())
        {
            LOG.debug("Network failure ignored by pattern: {}", url);
            return;
        }

        final long now = System.currentTimeMillis();
        for (final NetworkFailure existing : buffer)
        {
            if (existing.url().equals(url) && existing.statusCode() == statusCode && (now - existing.timestamp() < 2000))
            {
                return;
            }
        }

        final NetworkFailure failure = new NetworkFailure(
            url,
            statusCode,
            method != null ? method : "UNKNOWN",
            errorDescription != null ? errorDescription : "",
            now
        );

        while (buffer.size() >= capacity)
        {
            buffer.pollFirst();
        }

        buffer.addLast(failure);
    }

    /**
     * Queries captured network failures filtered by status criteria, up to the specified limit.
     *
     * @param statusFilter filter criteria ({@code "all_failures"}, {@code "4xx"}, {@code "5xx"}, {@code "blocked"})
     * @param limit maximum number of entries to return (default 20 if &lt;= 0)
     * @param clearBuffer whether to remove the returned matching entries from the buffer atomically
     * @return unmodifiable list of matching network failures
     */
    public synchronized List<NetworkFailure> getFailures(final String statusFilter, final int limit, final boolean clearBuffer)
    {
        final int max = limit <= 0 ? 20 : limit;
        final List<NetworkFailure> results = new ArrayList<>();

        final Iterator<NetworkFailure> iterator = buffer.iterator();
        while (iterator.hasNext() && results.size() < max)
        {
            final NetworkFailure failure = iterator.next();
            if (failure.matchesFilter(statusFilter))
            {
                results.add(failure);
                if (clearBuffer)
                {
                    iterator.remove();
                }
            }
        }

        return Collections.unmodifiableList(results);
    }

    /**
     * Attaches CDP network event listeners to the browser session if supported by the driver.
     *
     * @param driver the active WebDriver instance
     */
    public void attach(final WebDriver driver)
    {
        if (!enabled || driver == null)
        {
            return;
        }

        if (driver instanceof HasDevTools hasDevTools)
        {
            try
            {
                final DevTools devTools = hasDevTools.getDevTools();
                devTools.createSessionIfThereIsNotOne(driver.getWindowHandle());
                devTools.send(Network.enable(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
                ));

                devTools.addListener(Network.responseReceived(), event ->
                {
                    final int status = event.getResponse().getStatus();
                    if (status >= 400)
                    {
                        recordFailure(
                            event.getResponse().getUrl(),
                            status,
                            event.getType().toString(),
                            event.getResponse().getStatusText()
                        );
                    }
                });

                devTools.addListener(Network.loadingFailed(), event ->
                {
                    recordFailure(
                        event.getRequestId().toString(),
                        0,
                        "NETWORK",
                        event.getErrorText()
                    );
                });

                LOG.info("Attached CDP Network listeners for protocol introspection.");
            }
            catch (final Throwable t)
            {
                LOG.debug("CDP Network listener attachment not available: {}", t.getMessage());
            }
        }
    }

    /**
     * Synchronizes failed HTTP requests from the browser Resource Timing API as a fallback.
     *
     * @param driver the active WebDriver instance
     */
    public void syncFromPerformanceApi(final WebDriver driver)
    {
        if (!enabled || !(driver instanceof JavascriptExecutor js))
        {
            return;
        }

        try
        {
            final Object result = js.executeScript(
                "return (window.performance && window.performance.getEntriesByType) ? " +
                "window.performance.getEntriesByType('resource')" +
                ".filter(function(r) { return r.responseStatus >= 400; })" +
                ".map(function(r) { return { url: r.name, status: r.responseStatus }; }) : [];"
            );

            if (result instanceof List<?> list)
            {
                for (final Object item : list)
                {
                    if (item instanceof Map<?, ?> map)
                    {
                        final String url = String.valueOf(map.get("url"));
                        final Object statusObj = map.get("status");
                        final int status = statusObj instanceof Number n ? n.intValue() : 0;
                        if (status >= 400)
                        {
                            recordFailure(url, status, "GET", "HTTP " + status);
                        }
                    }
                }
            }
        }
        catch (final Throwable t)
        {
            LOG.debug("Performance Resource Timing sync failed: {}", t.getMessage());
        }
    }

    /**
     * Returns the current number of network failures in the buffer.
     *
     * @return current buffer size
     */
    public synchronized int size()
    {
        return buffer.size();
    }

    /**
     * Clears all network failures from the buffer.
     */
    public synchronized void clear()
    {
        buffer.clear();
    }

    /**
     * Checks whether this network monitor is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled()
    {
        return enabled;
    }
}
