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
