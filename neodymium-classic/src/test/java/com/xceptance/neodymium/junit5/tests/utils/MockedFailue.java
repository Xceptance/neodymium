package com.xceptance.neodymium.junit5.tests.utils;

import java.util.Random;
import java.util.UUID;

import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;

import com.xceptance.neodymium.util.Neodymium;

public class MockedFailue implements Failure
{
    private static final long serialVersionUID = 1L;

    private String testMethodName;

    private Throwable failureCauses;

    public MockedFailue(String testMethodName, Throwable failureCauses)
    {
        this.testMethodName = testMethodName;
        this.failureCauses = failureCauses;
    }

    @Override
    public TestIdentifier getTestIdentifier()
    {
        return TestIdentifier.from(new MockedTestDescriptor("[engine:junit-jupiter]/[class:com.xceptance.neodymium.tests.NeodymiumTestSelfTest]/[method:"+testMethodName+"(org.junit.jupiter.api.TestInfo)]", testMethodName));
    }

    @Override
    public Throwable getException()
    {
        return failureCauses;
    }

}
