/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.junit5.filtering;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.neodymium.common.WorkInProgress;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
import org.neodymium.util.NeodymiumAnnotationUtils;

public class WipTestMethodCallback implements ExecutionCondition
{
    private String testExecutionRegex = Neodymium.configuration().getTestNameFilter();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        boolean workInProgress = Neodymium.configuration().workInProgress();
        boolean wipMethod = Stream.of(context.getRequiredTestClass().getMethods())
                                  .filter(method -> NeodymiumAnnotationUtils.isAnnotationPresent(method, NeodymiumTest.class) || NeodymiumAnnotationUtils.isAnnotationPresent(method, org.neodymium.junit5.NeodymiumTestGenerator.class))
                                  .anyMatch(method -> NeodymiumAnnotationUtils.isAnnotationPresent(method, WorkInProgress.class));

        String testNameFilterMessage = testExecutionRegex != null ? "method or test matching filter: '" + testExecutionRegex + "' " : "";

        if (workInProgress && wipMethod && !NeodymiumAnnotationUtils.isAnnotationPresent(context.getRequiredTestMethod(), WorkInProgress.class))
        {
            return ConditionEvaluationResult.disabled(testNameFilterMessage + "not marked as WIP");
        }

        return ConditionEvaluationResult.enabled("");
    }
}
