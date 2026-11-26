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
package com.xceptance.neodymium.junit4.tests;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class NeodymiumTestSelfTest extends NeodymiumTest
{
    @Rule
    public TestName name = new TestName();

    @Test
    public void testCheckFailedOneFromOne() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final String errorMessage = "This is RuntimeException 1";
        final Result result = createResult(1, 0, Map.of(name.getMethodName().concat("1"), new RuntimeException(errorMessage)));
        checkFail(result, 1, 0, 1, errorMessage);
    }

    @Test
    public void testCheckFailedOneFromTwo() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final String errorMessage = "This is RuntimeException 1";
        final Result result = createResult(2, 0, Map.of(name.getMethodName().concat("1"), new RuntimeException(errorMessage)));

        checkFail(result, 2, 0, 1, errorMessage);
    }

    @Test
    public void testCheckFailedTwoFromTwoNumber() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final String errorMessage1 = "This is RuntimeException 1";
        final String errorMessage2 = "This is RuntimeException 2";
        final Result result = createResult(2, 0, Map.of(name.getMethodName().concat("1"), new RuntimeException(errorMessage1),
                                                        name.getMethodName().concat("2"), new RuntimeException(errorMessage2)));

        // no assertion for failure message, as one is only possible for single fail
        checkFail(result, 2, 0, 2);
    }

    @Test
    public void testCheckFailedTwoFromTwoOneFailureMessage() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final String errorMessage = "This is RuntimeException 1";
        final Result result = createResult(2, 0, Map.of(name.getMethodName().concat("1"), new RuntimeException(errorMessage),
                                                        name.getMethodName().concat("2"), new RuntimeException(errorMessage)));

        // no assertion for failure message, as one is only possible for single fail
        checkFail(result, 2, 0, 2, errorMessage);
    }

    @Test
    public void testCheckFailedTwoFromTwoTwoFailureMessages() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final String errorMessage1 = "This is RuntimeException 1";
        final String errorMessage2 = "This is RuntimeException 2";
        final Result result = createResult(2, 0, Map.of(name.getMethodName().concat("1"), new RuntimeException(errorMessage1),
                                                        name.getMethodName().concat("2"), new RuntimeException(errorMessage2)));
        final Map<String, String> expectedFailureMessages = Map.of(name.getMethodName().concat("1"), errorMessage1,
                                                                   name.getMethodName().concat("2"), errorMessage2);

        // no assertion for failure message, as one is only possible for single fail
        checkFail(result, 2, 0, 2, expectedFailureMessages);
    }

    @Test
    public void testCheckOneFailedOneIgnoredFromTwo() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final String errorMessage = "This is RuntimeException 1";
        final Result result = createResult(2, 1, Map.of(name.getMethodName().concat("1"), new RuntimeException(errorMessage)));
        checkFail(result, 2, 1, 1, errorMessage);
    }

    // ------ checkPass-----------
    @Test
    public void testCheckPassedOneFromOne() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Result result = createResult(1, 0, null);
        checkPass(result, 1, 0);
    }

    @Test
    public void testCheckPassedTwoFromTwo() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Result result = createResult(2, 0, null);
        checkPass(result, 2, 0);
    }

    @Test
    public void testCheckOnePassedOneIgnoredFromTwo() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Result result = createResult(2, 1, null);
        checkPass(result, 2, 1);
    }

    private Result createResult(int runCount, int ignoreCount, Map<String, Throwable> failureCauses)
        throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
    {
        final Result result = new Result();
        final Field countField = Result.class.getDeclaredField("count");
        final Field ignoreCountField = Result.class.getDeclaredField("ignoreCount");
        final Field failuresField = Result.class.getDeclaredField("failures");

        countField.setAccessible(true);
        ignoreCountField.setAccessible(true);
        failuresField.setAccessible(true);

        countField.set(result, new AtomicInteger(runCount));
        ignoreCountField.set(result, new AtomicInteger(ignoreCount));

        if (failureCauses != null)
        {
            final CopyOnWriteArrayList<Failure> failures = new CopyOnWriteArrayList<>();
            for (final String testMethodName : failureCauses.keySet())
            {
                final Failure failure = new Failure(Description.createTestDescription(getClass(), testMethodName), failureCauses.get(testMethodName));
                failures.add(failure);
            }
            failuresField.set(result, failures);
        }
        countField.setAccessible(false);
        ignoreCountField.setAccessible(false);
        failuresField.setAccessible(false);
        return result;
    }
}
