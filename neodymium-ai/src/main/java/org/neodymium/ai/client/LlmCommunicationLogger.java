/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.client;

import java.lang.reflect.Method;
import org.neodymium.ai.config.AiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated logger and runtime controller for LLM communication wire logging.
 * Captures raw HTTP request and response payloads exchanged with LLM providers.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmCommunicationLogger
{
    /**
     * Logger category name for LLM communication.
     */
    public static final String LOGGER_NAME = "org.neodymium.ai.client.communication";

    /**
     * Dedicated SLF4J logger instance for LLM communication.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LOGGER_NAME);

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private LlmCommunicationLogger()
    {
    }

    /**
     * Gets the dedicated communication SLF4J logger.
     *
     * @return the SLF4J logger instance
     */
    public static Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * Checks whether LLM communication wire logging is active.
     * If enabled via {@link AiConfiguration}, dynamically ensures the Log4j2 logger level
     * is set to INFO so logs are dispatched to the AI_COMMUNICATION_FILE appender.
     *
     * @return true if communication logging is active, false otherwise
     */
    public static boolean isLoggingActive()
    {
        final boolean configEnabled = AiConfiguration.getInstance().isCommunicationLogEnabled();
        if (configEnabled)
        {
            if (!LOGGER.isInfoEnabled())
            {
                enableLog4jLevel();
            }
            return true;
        }

        return LOGGER.isInfoEnabled();
    }

    /**
     * Dynamically sets the Log4j2 logger level for the communication logger to INFO via reflection
     * if Log4j2 core is available on the runtime classpath.
     */
    private static void enableLog4jLevel()
    {
        try
        {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null)
            {
                cl = LlmCommunicationLogger.class.getClassLoader();
            }
            final Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level", true, cl);
            final Object infoLevel = levelClass.getField("INFO").get(null);
            final Class<?> configuratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator", true, cl);
            final Method setLevelMethod = configuratorClass.getMethod("setLevel", String.class, levelClass);
            setLevelMethod.invoke(null, LOGGER_NAME, infoLevel);
        }
        catch (final Throwable ignored)
        {
            // Ignore if Log4j2 core is not present on runtime classpath
        }
    }
}
