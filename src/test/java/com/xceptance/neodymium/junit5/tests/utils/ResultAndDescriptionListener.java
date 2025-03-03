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
