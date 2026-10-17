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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.latest.runtime.Runtime;
import org.openqa.selenium.devtools.latest.runtime.model.CallFrame;
import org.openqa.selenium.devtools.latest.runtime.model.ExceptionDetails;
import org.openqa.selenium.devtools.latest.runtime.model.RemoteObject;
import org.openqa.selenium.devtools.latest.runtime.model.StackTrace;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe, bounded in-memory circular monitor collecting browser console messages,
 * warnings, and unhandled client-side runtime exceptions via Chrome DevTools Protocol (CDP)
 * and W3C browser log fallback.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class ConsoleLogMonitor
{
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleLogMonitor.class);

    public static final int DEFAULT_CAPACITY = 100;

    private final int capacity;
    private final ArrayDeque<ConsoleLogEntry> buffer;

    /**
     * Creates a new monitor with default capacity (100).
     */
    public ConsoleLogMonitor()
    {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a new monitor with specified capacity.
     *
     * @param capacity maximum number of console log entries to store
     */
    public ConsoleLogMonitor(final int capacity)
    {
        this.capacity = Math.max(1, capacity);
        this.buffer = new ArrayDeque<>(this.capacity);
    }

    /**
     * Records a console log entry into the circular buffer.
     *
     * @param level severity level ({@code "SEVERE"}, {@code "WARNING"}, {@code "INFO"})
     * @param message log message or error description
     * @param source source script URL or identifier
     * @param lineNumber script line number
     * @param stackTrace formatted stack trace or null
     */
    public synchronized void recordEntry(
        final String level,
        final String message,
        final String source,
        final int lineNumber,
        final String stackTrace)
    {
        if (message == null || message.isBlank())
        {
            return;
        }

        final long now = System.currentTimeMillis();
        for (final ConsoleLogEntry existing : buffer)
        {
            if (existing.message().equals(message) && existing.level().equals(level != null ? level : "INFO") && (now - existing.timestamp() < 2000))
            {
                return;
            }
        }

        final ConsoleLogEntry entry = new ConsoleLogEntry(
            level != null ? level : "INFO",
            message,
            source != null ? source : "unknown",
            Math.max(0, lineNumber),
            stackTrace,
            now
        );

        while (buffer.size() >= capacity)
        {
            buffer.pollFirst();
        }

        buffer.addLast(entry);
    }

    /**
     * Records an unhandled runtime exception with {@code SEVERE} level.
     *
     * @param message exception message
     * @param source script URL
     * @param lineNumber script line number
     * @param stackTrace formatted stack trace
     */
    public synchronized void recordException(
        final String message,
        final String source,
        final int lineNumber,
        final String stackTrace)
    {
        recordEntry("SEVERE", message, source, lineNumber, stackTrace);
    }

    /**
     * Queries captured console log entries matching the minimum severity threshold.
     *
     * @param minLevel minimum severity ({@code "SEVERE"}, {@code "WARNING"}, {@code "INFO"})
     * @param limit maximum number of entries to return (default 20 if &lt;= 0)
     * @param clearBuffer whether to drain returned entries from the buffer atomically
     * @return unmodifiable list of matching log entries
     */
    public synchronized List<ConsoleLogEntry> getLogs(final String minLevel, final int limit, final boolean clearBuffer)
    {
        final int max = limit <= 0 ? 20 : limit;
        final List<ConsoleLogEntry> results = new ArrayList<>();

        final Iterator<ConsoleLogEntry> iterator = buffer.iterator();
        while (iterator.hasNext() && results.size() < max)
        {
            final ConsoleLogEntry entry = iterator.next();
            if (entry.matchesMinLevel(minLevel))
            {
                results.add(entry);
                if (clearBuffer)
                {
                    iterator.remove();
                }
            }
        }

        return Collections.unmodifiableList(results);
    }

    /**
     * Attaches CDP Runtime event listeners to the browser session if supported.
     *
     * @param driver the active WebDriver instance
     */
    public void attach(final WebDriver driver)
    {
        if (driver == null)
        {
            return;
        }

        if (driver instanceof HasDevTools hasDevTools)
        {
            try
            {
                final DevTools devTools = hasDevTools.getDevTools();
                devTools.createSessionIfThereIsNotOne(driver.getWindowHandle());
                devTools.send(Runtime.enable());

                devTools.addListener(Runtime.exceptionThrown(), event ->
                {
                    final ExceptionDetails details = event.getExceptionDetails();
                    final String msg = details.getText() != null ? details.getText() : "Unhandled JavaScript Exception";
                    final String url = details.getUrl().orElse("unknown");
                    final int line = details.getLineNumber();

                    final StringBuilder stack = new StringBuilder();
                    if (details.getStackTrace().isPresent())
                    {
                        final StackTrace st = details.getStackTrace().get();
                        for (final CallFrame frame : st.getCallFrames())
                        {
                            stack.append("    at ").append(frame.getFunctionName())
                                 .append(" (").append(frame.getUrl())
                                 .append(":").append(frame.getLineNumber())
                                 .append(":").append(frame.getColumnNumber())
                                 .append(")\n");
                        }
                    }

                    recordException(msg, url, line, stack.length() > 0 ? stack.toString().trim() : null);
                });

                devTools.addListener(Runtime.consoleAPICalled(), event ->
                {
                    final String typeStr = event.getType().toString();
                    final String level = switch (typeStr.toLowerCase())
                    {
                        case "error", "assert" -> "SEVERE";
                        case "warning", "warn" -> "WARNING";
                        default -> "INFO";
                    };

                    final StringBuilder msg = new StringBuilder();
                    for (final RemoteObject arg : event.getArgs())
                    {
                        if (msg.length() > 0)
                        {
                            msg.append(" ");
                        }
                        if (arg.getValue().isPresent())
                        {
                            msg.append(arg.getValue().get());
                        }
                        else if (arg.getDescription().isPresent())
                        {
                            msg.append(arg.getDescription().get());
                        }
                    }

                    recordEntry(level, msg.length() > 0 ? msg.toString() : "console." + typeStr, "console", 0, null);
                });

                LOG.info("Attached CDP Runtime listeners for console and exception monitoring.");
            }
            catch (final Throwable t)
            {
                LOG.debug("CDP Runtime listener attachment not available: {}", t.getMessage());
            }
        }
    }

    /**
     * Synchronizes browser logs via standard W3C log endpoint as a fallback.
     *
     * @param driver the active WebDriver instance
     */
    public void syncFromW3cLogs(final WebDriver driver)
    {
        if (driver == null)
        {
            return;
        }

        try
        {
            final LogEntries entries = driver.manage().logs().get(LogType.BROWSER);
            for (final LogEntry entry : entries)
            {
                final String lvlName = entry.getLevel().getName();
                final String level = switch (lvlName.toUpperCase())
                {
                    case "SEVERE", "ERROR" -> "SEVERE";
                    case "WARNING", "WARN" -> "WARNING";
                    default -> "INFO";
                };
                recordEntry(level, entry.getMessage(), "w3c_browser_log", 0, null);
            }
        }
        catch (final Throwable t)
        {
            LOG.debug("W3C browser log retrieval not available: {}", t.getMessage());
        }
    }

    /**
     * Returns the current number of console log entries in the buffer.
     *
     * @return current buffer size
     */
    public synchronized int size()
    {
        return buffer.size();
    }

    /**
     * Clears all log entries from the buffer.
     */
    public synchronized void clear()
    {
        buffer.clear();
    }
}
