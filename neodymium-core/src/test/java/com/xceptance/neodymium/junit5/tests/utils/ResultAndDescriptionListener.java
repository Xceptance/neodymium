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
package com.xceptance.neodymium.junit5.tests.utils;

import java.util.LinkedList;
import java.util.List;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

public class ResultAndDescriptionListener extends SummaryGeneratingListener
{
    private List<String> description = new LinkedList<>();

    private List<String> containerDescription = new LinkedList<>();

    @Override
    public void dynamicTestRegistered(TestIdentifier testIdentifier)
    {
        super.dynamicTestRegistered(testIdentifier);
        description.add(testIdentifier.getDisplayName());
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult)
    {
        super.executionFinished(testIdentifier, testExecutionResult);
        if (testIdentifier.isContainer())
        {
            containerDescription.add(testIdentifier.getDisplayName());
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason)
    {
        super.executionSkipped(testIdentifier, reason);
        description.add(testIdentifier.getDisplayName());
    }

    @Override
    public NeodymiumTestExecutionSummary getSummary()
    {
        return new NeodymiumTestExecutionSummary(super.getSummary(), getDescription());
    }

    private List<String> getDescription()
    {
        if (description.isEmpty() && !containerDescription.isEmpty())
        {
            return List.of(containerDescription.get(0));
        }
        return description;
    }
}
