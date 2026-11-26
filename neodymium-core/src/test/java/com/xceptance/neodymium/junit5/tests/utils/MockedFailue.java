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


import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;


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
