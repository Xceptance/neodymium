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
package com.xceptance.neodymium.ai.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * 
 * Custom thread-safe Log4j2 Appender that captures log lines in parallel,
 * allowing test suites to verify framework logs (such as token metrics or linter alerts).
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LogCaptureAppender extends AbstractAppender
{
    private final List<String> logs = new ArrayList<>();

    private LogCaptureAppender()
    {
        super("LogCapture", null, null, false, null);
    }

    @Override
    public void append(final LogEvent event)
    {
        synchronized (logs)
        {
            logs.add(event.getMessage().getFormattedMessage());
        }
    }

    /**
     * Gets a copy of all logs captured in parallel during the test run.
     * 
     * @return the list of log lines
     */
    public List<String> getLogs()
    {
        synchronized (logs)
        {
            return new ArrayList<>(logs);
        }
    }

    /**
     * Starts capturing Log4j2 logs in parallel.
     * 
     * @return the started appender instance
     */
    public static LogCaptureAppender startCapture()
    {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration config = context.getConfiguration();
        final LogCaptureAppender appender = new LogCaptureAppender();
        appender.start();
        config.addAppender(appender);
        updateLoggers(config, appender);
        context.updateLoggers();
        return appender;
    }

    /**
     * Stops log capturing and removes the appender from log configurations.
     */
    public void stopCapture()
    {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final Configuration config = context.getConfiguration();
        for (final LoggerConfig loggerConfig : config.getLoggers().values())
        {
            loggerConfig.removeAppender("LogCapture");
        }
        config.getRootLogger().removeAppender("LogCapture");
        this.stop();
        context.updateLoggers();
    }

    private static void updateLoggers(final Configuration config, final Appender appender)
    {
        for (final LoggerConfig loggerConfig : config.getLoggers().values())
        {
            loggerConfig.addAppender(appender, Level.DEBUG, null);
        }
        config.getRootLogger().addAppender(appender, Level.DEBUG, null);
    }
}
