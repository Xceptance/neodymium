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
package com.xceptance.neodymium.aura;

/**
 * Legacy launcher bridge for the unified Neodymium AI Manager Spring Boot application.
 *
 * @deprecated Use {@link NeodymiumAiManager} instead.
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Deprecated
public final class NeodymiumAuraManager
{
    private NeodymiumAuraManager()
    {
    }

    /**
     * Main entry point for starting the Neodymium AI Manager.
     *
     * @param args command-line arguments forwarded to the Spring Boot application
     */
    public static void main(final String[] args)
    {
        NeodymiumAiManager.main(args);
    }

    /**
     * Attempts to start the unified Spring Boot AI Manager or prints an actionable diagnostic error.
     *
     * @param args command-line arguments forwarded to the Spring Boot application
     */
    public static void start(final String[] args)
    {
        NeodymiumAiManager.start(args);
    }

    /**
     * Strips ANSI escape sequences and console control codes from a log line.
     * Delegated to {@link NeodymiumAiManager#stripAnsi(String)}.
     *
     * @param line input text containing ANSI escape sequences
     * @return cleaned text with control sequences removed
     */
    public static String stripAnsi(final String line)
    {
        return NeodymiumAiManager.stripAnsi(line);
    }
}
