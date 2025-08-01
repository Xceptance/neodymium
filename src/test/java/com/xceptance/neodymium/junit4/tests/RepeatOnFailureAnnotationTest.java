package com.xceptance.neodymium.junit4.tests;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryBrowserCombinationTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryTestdataCombinationTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryWithoutSuccessInAfterTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryWithoutSuccessInBeforeTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryWithoutSuccessMultiplicationTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryWithoutSuccessTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryStopOnUnexpectedFailureTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryStopOnSuccessTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryEndWithSuccessTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryOnEveryErrorTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryOwnBrowserForSetupTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassNoRetryOnUnexpectedErrorTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.methodlevel.MethodRepeatOnFailureBrowserCombinationTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.methodlevel.MethodRepeatOnFailureTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.methodlevel.MethodRepeatOnFailureTestdataCombinationTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.mix.OverwriteRetryTest;

public class RepeatOnFailureAnnotationTest extends NeodymiumTest
{
    @Test
    public void testClassRetryEndWithSuccessTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryEndWithSuccessTest.class);
        checkPass(result, 3, 0);
    }

    @Test
    public void testClassRetryOnEveryErrorTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryOnEveryErrorTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }

    @Test
    public void testClassRetryOwnBrowserForSetupTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryOwnBrowserForSetupTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }

    @Test
    public void testClassNoRetryOnUnexpectedErrorTest()
    {
        Result result = JUnitCore.runClasses(ClassNoRetryOnUnexpectedErrorTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }

    @Test
    public void testClassRetryStopOnSuccessTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryStopOnSuccessTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }

    @Test
    public void testClassRetryWithoutSuccessInAfterTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryWithoutSuccessInAfterTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }

    @Test
    public void testClassRetryWithoutSuccessInBeforeTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryWithoutSuccessInBeforeTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }

    @Test
    public void testClassRetryWithoutSuccessMultiplicationTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryWithoutSuccessMultiplicationTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }

    @Test
    public void testClassRetryWithoutSuccessTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryWithoutSuccessTest.class);
        checkFail(result, 3, 0, 1, "Fail 3");
    }

    @Test
    public void testClassRetryStopOnUnexpectedFailureTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryStopOnUnexpectedFailureTest.class);
        checkAssumptionFailure(result, false, 10, 0, 5, 4, null);
    }

    @Test
    public void testClassRepeatOnFailureBrowserCombination()
    {
        Result result = JUnitCore.runClasses(ClassRetryBrowserCombinationTest.class);
        checkAssumptionFailure(result, false, 20, 0, 10, 8, null);
    }

    @Test
    public void testClassRepeatOnFailureTestdataCombination()
    {
        Result result = JUnitCore.runClasses(ClassRetryTestdataCombinationTest.class);
        checkAssumptionFailure(result, false, 30, 0, 15, 12, null);
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
    public void testMethodRepeatOnFailureTest()
    {
        Result result = JUnitCore.runClasses(MethodRepeatOnFailureTest.class);
        checkFail(result, 4, 0, 1, "Parent fail");
    }

    @Test
    public void testOverwriteRetryTest()
    {
        Result result = JUnitCore.runClasses(OverwriteRetryTest.class);
        checkAssumptionFailure(result, false, 30, 0, 15, 12, null);
    }
}
