package com.xceptance.neodymium.junit4.tests;

import java.util.HashMap;

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
        checkFail(result, 3, 0, 1, "Fail 3");
    }

    @Test
    public void testClassRetryOwnBrowserForSetupTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryOwnBrowserForSetupTest.class);
        checkFail(result, 3, 0, 1, "Fail 3");
    }

    @Test
    public void testClassNoRetryOnUnexpectedErrorTest()
    {
        Result result = JUnitCore.runClasses(ClassNoRetryOnUnexpectedErrorTest.class);
        checkFail(result, 3, 0, 1, "Shoul not be retried");
    }

    @Test
    public void testClassRetryStopOnSuccessTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryStopOnSuccessTest.class);
        checkPass(result, 3, 0);
    }

    @Test
    public void testClassRetryWithoutSuccessInAfterTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryWithoutSuccessInAfterTest.class);
        checkFail(result, 3, 0, 1, "Fail 3");
    }

    @Test
    public void testClassRetryWithoutSuccessInBeforeTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryWithoutSuccessInBeforeTest.class);
        checkFail(result, 3, 0, 1, "Fail 3");
    }

    @Test
    public void testClassRetryWithoutSuccessMultiplicationTest()
    {
        Result result = JUnitCore.runClasses(ClassRetryWithoutSuccessMultiplicationTest.class);
        checkFail(result, 12, 0, 1, "Fail 3");
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
        checkFail(result, 3, 0, 1, "Fail 2");
    }

    @Test
    public void testClassRepeatOnFailureBrowserCombination()
    {
        Result result = JUnitCore.runClasses(ClassRetryBrowserCombinationTest.class);
        checkFail(result, 6, 0, 1, "Fail 3");
    }

    @Test
    public void testClassRepeatOnFailureTestdataCombination()
    {
        Result result = JUnitCore.runClasses(ClassRetryTestdataCombinationTest.class);
        checkFail(result, 6, 0, 1, "Fail 3");
    }

    @Test
    public void testMethodRepeatOnFailureBrowserCombination()
    {
        Result result = JUnitCore.runClasses(MethodRepeatOnFailureBrowserCombinationTest.class);
        checkFail(result, 8, 0, 2, new HashMap<String, String>()
        {
            {
                put("testWithoutRetry :: Browser Chrome_1500x1000_headless :: ", "Fail 1");
                put("testWithRetry :: Browser Chrome_1500x1000_headless :: ", "Fail 3");
            }
        });
    }

    @Test
    public void testMethodRepeatOnFailureTestdataCombination()
    {
        Result result = JUnitCore.runClasses(MethodRepeatOnFailureTestdataCombinationTest.class);
        checkFail(result, 6, 0, 1, "Fail 3");
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
        checkFail(result, 6, 0, 2, new HashMap<String, String>()
        {
            {
                put("childTest :: Browser Chrome_headless :: ", "Fail Child 3");
                put("parentTest :: Browser Chrome_headless :: ", "Fail Parent 3");
            }
        });
    }
}
