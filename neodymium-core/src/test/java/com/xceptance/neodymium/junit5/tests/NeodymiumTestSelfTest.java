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
package com.xceptance.neodymium.junit5.tests;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;

import com.xceptance.neodymium.junit5.tests.utils.MockedFailue;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class NeodymiumTestSelfTest extends AbstractNeodymiumTest
{

    @Test
    public void testCheckFailedOneFromOne(TestInfo testInfo) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Throwable error1 = new RuntimeException("This is RuntimeException 1");

        final NeodymiumTestExecutionSummary result = createResult(1, 0, List.of(testInfo.getDisplayName().concat("1")),
                                                                  Map.of(testInfo.getDisplayName().concat("1"), error1));
        checkFail(result, 1, 0, 1, error1.toString());
    }

    @Test
    public void testCheckFailedOneFromTwo(TestInfo testInfo) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Throwable error1 = new RuntimeException("This is RuntimeException 1");

        final NeodymiumTestExecutionSummary result = createResult(2, 0, List.of(testInfo.getDisplayName().concat("1")),
                                                                  Map.of(testInfo.getDisplayName().concat("1"), error1));

        checkFail(result, 2, 0, 1, error1.toString());
    }

    @Test
    public void testCheckFailedTwoFromTwoNumber(TestInfo testInfo)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Throwable error1 = new RuntimeException("This is RuntimeException 1");
        final Throwable error2 = new RuntimeException("This is RuntimeException 2");

        final NeodymiumTestExecutionSummary result = createResult(2, 0, List.of(testInfo.getDisplayName()),
                                                                  Map.of(testInfo.getDisplayName().concat("1"), error1,
                                                                         testInfo.getDisplayName().concat("2"), error2));

        // no assertion for failure message, as one is only possible for single fail
        checkFail(result, 2, 0, 2);
    }

    @Test
    public void testCheckFailedTwoFromTwoOneFailureMessage(TestInfo testInfo)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Throwable error1 = new RuntimeException("This is RuntimeException 1");

        final NeodymiumTestExecutionSummary result = createResult(2, 0, List.of(testInfo.getDisplayName().concat("1"), testInfo.getDisplayName().concat("2")),
                                                                  Map.of(testInfo.getDisplayName().concat("1"), error1,
                                                                         testInfo.getDisplayName().concat("2"), error1));

        // no assertion for failure message, as one is only possible for single fail
        checkFail(result, 2, 0, 2, error1.toString());
    }

    @Test
    public void testCheckFailedTwoFromTwoTwoFailureMessages(TestInfo testInfo)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Throwable error1 = new RuntimeException("This is RuntimeException 1");
        final Throwable error2 = new RuntimeException("This is RuntimeException 2");

        final NeodymiumTestExecutionSummary result = createResult(2, 0, List.of(testInfo.getDisplayName().concat("1"), testInfo.getDisplayName().concat("2")),
                                                                  Map.of(testInfo.getDisplayName().concat("1"), error1,
                                                                         testInfo.getDisplayName().concat("2"), error2));
        final Map<String, String> expectedFailureMessages = Map.of(testInfo.getDisplayName().concat("1"), error1.toString(),
                                                                   testInfo.getDisplayName().concat("2"), error2.toString());

        // no assertion for failure message, as one is only possible for single fail
        checkFail(result, 2, 0, 2, expectedFailureMessages);
    }

    @Test
    public void testCheckOneFailedOneIgnoredFromTwo(TestInfo testInfo)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Throwable error = new RuntimeException("This is RuntimeException 1");
        final NeodymiumTestExecutionSummary result = createResult(2, 1, List.of(testInfo.getDisplayName().concat("1")),
                                                                  Map.of(testInfo.getDisplayName().concat("1"), error));
        checkFail(result, 2, 1, 1, error.toString());
    }

    // ------ checkPass-----------
    @Test
    public void testCheckPassedOneFromOne(TestInfo testInfo) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final NeodymiumTestExecutionSummary result = createResult(1, 0, List.of(testInfo.getDisplayName()), null);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCheckPassedTwoFromTwo(TestInfo testInfo) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final NeodymiumTestExecutionSummary result = createResult(2, 0, List.of(testInfo.getDisplayName()), null);
        checkPass(result, 2, 0);
    }

    @Test
    public void testCheckOnePassedOneIgnoredFromTwo(TestInfo testInfo)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final NeodymiumTestExecutionSummary result = createResult(2, 1, List.of(testInfo.getDisplayName()), null);
        checkPass(result, 2, 1);
    }

    private NeodymiumTestExecutionSummary createResult(int runCount, int ignoreCount, List<String> descriptions, Map<String, Throwable> failureCauses)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final CopyOnWriteArrayList<Failure> failures = new CopyOnWriteArrayList<>();

        if (failureCauses != null)
        {
            for (final String testMethodName : failureCauses.keySet())
            {
                final Failure failure = new MockedFailue(testMethodName, failureCauses.get(testMethodName));
                failures.add(failure);
            }
        }

        return new NeodymiumTestExecutionSummary(ignoreCount, failures, descriptions, runCount);
    }
}
