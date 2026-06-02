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
package com.xceptance.neodymium.ai.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;

/**
 * A mock implementation of {@link ActionExecutor} designed for browserless, offline execution testing.
 * <p>
 * Instead of performing live browser actions via Selenium/WebDriver, this executor intercepts, 
 * validates, and logs proposed {@link Action} objects into an in-memory thread-safe list.
 * It also supports simulating specific execution failures (such as missing target elements 
 * or obstructed buttons) to verify the AI agent's self-healing and context escalation behaviors.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class MockActionExecutor extends ActionExecutor
{
    /**
     * Thread-safe list containing all {@link Action} objects intercepted during execution.
     */
    private final List<Action> executedActions = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Simulated exception to throw upon action invocation, mimicking Selenide execution errors.
     */
    private ActionExecutionException exceptionToThrow;

    /**
     * Constructs a new MockActionExecutor with a null test class context to prevent any 
     * Selenide or browser initialization.
     */
    public MockActionExecutor()
    {
        super(null);
    }

    /**
     * Configures a simulated execution exception to throw during action processing.
     * Use this to test self-healing retry loops and visual context escalations.
     *
     * @param exceptionToThrow the simulated exception to throw
     */
    public final void setExceptionToThrow(final ActionExecutionException exceptionToThrow)
    {
        this.exceptionToThrow = exceptionToThrow;
    }

    /**
     * Intercepts a single proposed browser action. Logs the action in memory or throws a 
     * pre-configured exception if set.
     *
     * @param action the proposed action to execute
     * @throws ActionExecutionException if a simulated failure is configured
     */
    @Override
    public final void execute(final Action action)
    {
        if (this.exceptionToThrow != null)
        {
            throw this.exceptionToThrow;
        }
        if (action != null)
        {
            this.executedActions.add(action);
        }
    }

    /**
     * Intercepts a list of proposed browser actions. Logs all actions in memory or throws a 
     * pre-configured exception if set.
     *
     * @param actions the list of actions to execute
     * @throws ActionExecutionException if a simulated failure is configured
     */
    @Override
    public final void executeAll(final List<Action> actions)
    {
        if (this.exceptionToThrow != null)
        {
            throw this.exceptionToThrow;
        }
        if (actions != null)
        {
            this.executedActions.addAll(actions);
        }
    }

    /**
     * Retrieves the complete list of intercepted browser actions for test assertions.
     *
     * @return the list of executed actions
     */
    public final List<Action> getExecutedActions()
    {
        return this.executedActions;
    }
}
