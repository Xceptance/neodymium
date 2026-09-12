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
package org.neodymium.ai.executor;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.probe.LocatorProbeResult;
import org.neodymium.ai.model.ContextLevel;

/**
 * Interface representing the target execution environment driving operations
 * against a SUT (such as a Selenium web browser, a REST API, or a CLI process).
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public interface TargetExecutor extends AutoCloseable
{
    /**
     * Closes any resources associated with this executor.
     *
     * @throws Exception if closing fails
     */
    @Override
    default void close() throws Exception
    {
    }

    /**
     * Captures the current state of the SUT.
     *
     * @param level the active context level to capture
     * @return the captured SUT state
     * @throws IOException if state capture fails
     */
    SutState captureState(final ContextLevel level) throws IOException;

    /**
     * Captures the current state of the SUT with optional full-page screenshot override.
     *
     * @param level the active context level to capture
     * @param isFullPage true to force full-page screenshot capture
     * @return the captured SUT state
     * @throws IOException if state capture fails
     */
    default SutState captureState(final ContextLevel level, final boolean isFullPage) throws IOException
    {
        return captureState(level);
    }

    /**
     * Captures the current state of the SUT using the default LEAN context level.
     *
     * @return the captured SUT state
     * @throws IOException if state capture fails
     */
    default SutState captureState() throws IOException
    {
        return captureState(ContextLevel.LEAN);
    }

    /**
     * Executes a specific action against the SUT.
     *
     * @param action the executable action instance
     * @throws IOException if execution fails
     */
    void execute(final Action action) throws IOException;

    /**
     * Retrieves the set of actions supported by this executor.
     *
     * @return the set of supported ActionDefinitions
     */
    Set<ActionDefinition> getSupportedActions();

    /**
     * Indicates whether this target executor supports automated locator improvement.
     *
     * @return true if locator improvement is supported, false otherwise
     */
    default boolean supportsLocatorImprovement()
    {
        return false;
    }

    /**
     * Indicates whether this target executor supports read-only live locator probing.
     *
     * @return true if locator probing is supported, false otherwise
     */
    default boolean supportsLocatorProbing()
    {
        return false;
    }

    /**
     * Probes a list of candidate locators on the live SUT without performing any state-changing actions.
     *
     * @param candidateLocators list of candidate locators to probe
     * @param maxDepth maximum number of element matches to summarize per candidate
     * @return list of probe results corresponding to each candidate locator
     */
    default List<LocatorProbeResult> probeLocators(final List<String> candidateLocators, final int maxDepth)
    {
        if (candidateLocators == null || candidateLocators.isEmpty())
        {
            return List.of();
        }
        return candidateLocators.stream()
                .map(LocatorProbeResult::unsupported)
                .toList();
    }

    /**
     * Retrieves the framework name of this executor (e.g. "SELENIUM_SELENIDE", "REST", "CLI").
     *
     * @return the framework name string
     */
    default String getFrameworkName()
    {
        return "SELENIUM_SELENIDE";
    }

    /**
     * Registers username/password credentials for dynamic HTTP Basic Authentication
     * injection on the active target driver.
     *
     * @param username the authentication username
     * @param password the authentication password
     */
    default void registerBasicAuth(final String username, final String password)
    {
    }
}
