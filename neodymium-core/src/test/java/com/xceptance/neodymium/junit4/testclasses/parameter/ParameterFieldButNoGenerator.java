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
package com.xceptance.neodymium.junit4.testclasses.parameter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import org.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class ParameterFieldButNoGenerator
{
    /**
     * Neodymium emulates JUnit's parameterized runner with a few differences. Running this test with JUnits parameterized
     * runner will fail since this runner expects an generator function. Neodymium on the other site handles parameters as
     * optional. So if there is no generator function but parameterized members it just will ignore these annotation and run
     * only the test methods. So the only thing to test in absence of a generator is that the members don't change their
     * values.
     */

    @Parameter
    public Integer testInt;

    @Parameter
    public int testint;

    @Parameter
    public Integer testIntInitialized = 5;

    @Test
    public void test()
    {
        // test that no parameter annotated member is overwritten if there is no generator function
        Assert.assertNull(testInt);
        Assert.assertEquals(0, testint);
        Assert.assertEquals(5, testIntInitialized.intValue());
    }
}
