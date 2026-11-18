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
