/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
