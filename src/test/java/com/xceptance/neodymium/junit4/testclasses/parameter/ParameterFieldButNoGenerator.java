package com.xceptance.neodymium.junit4.testclasses.parameter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
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
