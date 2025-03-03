package com.xceptance.neodymium.junit4.tests;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.testclasses.repeat.classlevel.ClassRepeatOnFailureBrowserCombinationTest;
import com.xceptance.neodymium.testclasses.repeat.classlevel.ClassRepeatOnFailureTest;
import com.xceptance.neodymium.testclasses.repeat.classlevel.ClassRepeatOnFailureTestdataCombinationTest;
import com.xceptance.neodymium.testclasses.repeat.methodlevel.MethodRepeatOnFailureBrowserCombinationTest;
import com.xceptance.neodymium.testclasses.repeat.methodlevel.MethodRepeatOnFailureTest;
import com.xceptance.neodymium.testclasses.repeat.methodlevel.MethodRepeatOnFailureTestdataCombinationTest;
import com.xceptance.neodymium.testclasses.repeat.mix.OverridingRepeatOnFailureTest;

public class RepeatOnFailureAnnotationTest extends NeodymiumTest
{
    @Test
    public void testClassRepeatOnFailure()
    {
        Result result = JUnitCore.runClasses(ClassRepeatOnFailureTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }

    @Test
    public void testClassRepeatOnFailureBrowserCombination()
    {
        Result result = JUnitCore.runClasses(ClassRepeatOnFailureBrowserCombinationTest.class);
        checkAssumptionFailure(result, false, 20, 0, 10, 8, null);
    }

    @Test
    public void testClassRepeatOnFailureTestdataCombination()
    {
        Result result = JUnitCore.runClasses(ClassRepeatOnFailureTestdataCombinationTest.class);
        checkAssumptionFailure(result, false, 30, 0, 15, 12, null);
    }

    @Test
    public void testMethodRepeatOnFailure()
    {
        Result result = JUnitCore.runClasses(MethodRepeatOnFailureTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }

    @Test
    public void testMethodRepeatOnFailureBrowserCombination()
    {
        Result result = JUnitCore.runClasses(MethodRepeatOnFailureBrowserCombinationTest.class);
        checkAssumptionFailure(result, false, 20, 0, 10, 8, null);
    }

    @Test
    public void testMethodRepeatOnFailureTestdataCombination()
    {
        Result result = JUnitCore.runClasses(MethodRepeatOnFailureTestdataCombinationTest.class);
        checkAssumptionFailure(result, false, 30, 0, 15, 12, null);
    }

    @Test
    public void testOverridingRepeatOnFailure()
    {
        Result result = JUnitCore.runClasses(OverridingRepeatOnFailureTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }
}
