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
 * A mock action executor running offline by recording actions without executing them via Selenium/WebDriver.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class MockActionExecutor extends ActionExecutor
{
    private final List<Action> executedActions = Collections.synchronizedList(new ArrayList<>());
    private ActionExecutionException exceptionToThrow;

    public MockActionExecutor()
    {
        super(null);
    }

    public final void setExceptionToThrow(final ActionExecutionException exceptionToThrow)
    {
        this.exceptionToThrow = exceptionToThrow;
    }

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

    public final List<Action> getExecutedActions()
    {
        return this.executedActions;
    }
}
