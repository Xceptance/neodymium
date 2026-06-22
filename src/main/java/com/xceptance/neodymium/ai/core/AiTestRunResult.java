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
package com.xceptance.neodymium.ai.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite result representing the before, steps, and after execution results of an AI test run.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiTestRunResult
{
    private AiExecutionResult beforeResult;
    private AiExecutionResult stepsResult;
    private final List<AiExecutionResult> afterResults;

    public AiTestRunResult()
    {
        this.afterResults = Collections.synchronizedList(new ArrayList<>());
    }

    public final AiExecutionResult getBeforeResult()
    {
        return this.beforeResult;
    }

    public final void setBeforeResult(final AiExecutionResult beforeResult)
    {
        this.beforeResult = beforeResult;
    }

    public final AiExecutionResult getStepsResult()
    {
        return this.stepsResult;
    }

    public final void setStepsResult(final AiExecutionResult stepsResult)
    {
        this.stepsResult = stepsResult;
    }

    public final List<AiExecutionResult> getAfterResults()
    {
        return this.afterResults;
    }

    public final void addAfterResult(final AiExecutionResult afterResult)
    {
        if (afterResult != null)
        {
            this.afterResults.add(afterResult);
        }
    }
}
