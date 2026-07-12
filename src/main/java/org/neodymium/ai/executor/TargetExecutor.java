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
import java.util.Set;
import org.neodymium.ai.action.Action;

/**
 * Interface representing the target execution environment driving operations
 * against a SUT (such as a Selenium web browser, a REST API, or a CLI process).
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public interface TargetExecutor
{
    /**
     * Captures the current state of the SUT.
     *
     * @return the captured SUT state
     * @throws IOException if state capture fails
     */
    SutState captureState() throws IOException;

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
}
