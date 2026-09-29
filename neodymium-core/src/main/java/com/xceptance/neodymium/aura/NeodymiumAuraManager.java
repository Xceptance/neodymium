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

import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launcher bridge for the unified Neodymium Aura Manager Spring Boot application.
 * <p>
 * If invoked directly (for example by legacy build scripts or external projects), this bridge dynamically
 * locates {@code com.xceptance.aura.AuraManagerApplication} on the classpath and transfers execution to it.
 * If the {@code aura-manager} dependency is missing from the classpath, it outputs a clear diagnostic
 * message detailing the missing dependency and how to resolve it before terminating.
 * </p>
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class NeodymiumAuraManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumAuraManager.class);
    private static final String SPRING_BOOT_MAIN_CLASS = "com.xceptance.aura.AuraManagerApplication";

    private NeodymiumAuraManager()
    {
    }

    /**
     * Main entry point for starting the Neodymium Aura Manager.
     *
     * @param args command-line arguments forwarded to the Spring Boot application
     */
    public static void main(final String[] args)
    {
        start(args);
    }

    /**
     * Attempts to start the unified Spring Boot Aura Manager or prints an actionable diagnostic error.
     *
     * @param args command-line arguments forwarded to the Spring Boot application
     */
    public static void start(final String[] args)
    {
        try
        {
            final Class<?> appClass = Class.forName(SPRING_BOOT_MAIN_CLASS);
            final Method mainMethod = appClass.getMethod("main", String[].class);
            LOGGER.info("[Aura Bridge] Launching Neodymium Aura Manager ({})", SPRING_BOOT_MAIN_CLASS);
            mainMethod.invoke(null, (Object) (args != null ? args : new String[0]));
        }
        catch (final ClassNotFoundException | NoClassDefFoundError e)
        {
            printMissingDependencyError();
            System.exit(1);
        }
        catch (final Exception e)
        {
            LOGGER.error("[Aura Bridge] Failed to launch Neodymium Aura Manager: {}", e.getMessage(), e);
            System.err.println("[Aura Bridge] Fatal error starting Aura Manager: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Strips ANSI escape sequences and console control codes from a log line.
     * Delegated to {@link AuraQueueService#stripAnsi(String)}.
     *
     * @param line input text containing ANSI escape sequences
     * @return cleaned text with control sequences removed
     */
    public static String stripAnsi(final String line)
    {
        return AuraQueueService.stripAnsi(line);
    }

    private static void printMissingDependencyError()
    {
        final String errorMessage =
            "\n========================================================================================\n"
            + "[ERROR] Neodymium Aura Manager could not be started!\n"
            + "========================================================================================\n"
            + "Cause:\n"
            + "  The unified Spring Boot Aura Manager artifact ('com.xceptance.aura:aura-manager')\n"
            + "  was not found on the runtime classpath.\n\n"
            + "Resolution:\n"
            + "  Add the following dependency to your project's pom.xml:\n\n"
            + "  <dependency>\n"
            + "      <groupId>com.xceptance.aura</groupId>\n"
            + "      <artifactId>aura-manager</artifactId>\n"
            + "      <version>${neodymium.version}</version>\n"
            + "      <scope>test</scope>\n"
            + "  </dependency>\n\n"
            + "Alternative:\n"
            + "  For standalone interactive debugging without the Aura Manager server, run:\n"
            + "  mvn test -Dneodymium.ai.interactive=true\n"
            + "========================================================================================\n";

        LOGGER.error(errorMessage);
        System.err.println(errorMessage);
    }
}
