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

/**
 * Immutable record representing a captured browser console entry or unhandled JavaScript exception.
 *
 * @param level severity level ({@code "SEVERE"}, {@code "WARNING"}, {@code "INFO"})
 * @param message log message or exception description
 * @param source script URL or origin
 * @param lineNumber script line number (or 0 if unknown)
 * @param stackTrace formatted stack trace snippet or null
 * @param timestamp epoch millisecond when recorded
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public record ConsoleLogEntry(
    String level,
    String message,
    String source,
    int lineNumber,
    String stackTrace,
    long timestamp
)
{
    /**
     * Checks if this entry is a SEVERE error.
     *
     * @return true if severity is SEVERE or ERROR
     */
    public boolean isSevere()
    {
        return "SEVERE".equalsIgnoreCase(level) || "ERROR".equalsIgnoreCase(level);
    }

    /**
     * Checks if this entry is a WARNING.
     *
     * @return true if severity is WARNING or WARN
     */
    public boolean isWarning()
    {
        return "WARNING".equalsIgnoreCase(level) || "WARN".equalsIgnoreCase(level);
    }

    /**
     * Checks if this entry is an INFO or diagnostic message.
     *
     * @return true if severity is INFO, LOG, or DEBUG
     */
    public boolean isInfo()
    {
        return "INFO".equalsIgnoreCase(level) || "LOG".equalsIgnoreCase(level) || "DEBUG".equalsIgnoreCase(level);
    }

    /**
     * Checks whether this log entry meets or exceeds the specified minimum severity threshold.
     * Hierarchy: SEVERE &gt; WARNING &gt; INFO.
     *
     * @param minLevel minimum severity ({@code "SEVERE"}, {@code "WARNING"}, {@code "INFO"}, case-insensitive)
     * @return true if this entry satisfies the threshold
     */
    public boolean matchesMinLevel(final String minLevel)
    {
        if (minLevel == null || minLevel.isBlank())
        {
            return true;
        }

        final String norm = minLevel.trim().toUpperCase();
        return switch (norm)
        {
            case "SEVERE", "ERROR" -> isSevere();
            case "WARNING", "WARN" -> isSevere() || isWarning();
            case "INFO", "LOG", "DEBUG" -> true;
            default -> true;
        };
    }
}
