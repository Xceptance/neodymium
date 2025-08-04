package com.xceptance.neodymium.junit5.tests;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryBrowserCombinationTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryTestdataCombinationTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryWithoutSuccessInAfterTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryWithoutSuccessInBeforeTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryWithoutSuccessMultiplicationTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryWithoutSuccessTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryStopOnUnexpectedFailureTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryStopOnSuccessTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryEndWithSuccessTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryOnEveryErrorTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassRetryOwnBrowserForSetupTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.classlevel.ClassNoRetryOnUnexpectedErrorTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.methodlevel.MethodRepeatOnFailureBrowserCombinationTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.methodlevel.MethodRepeatOnFailureTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.methodlevel.MethodRepeatOnFailureTestdataCombinationTest;
import com.xceptance.neodymium.junit5.testclasses.repeat.mix.OverwriteRetryTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class RepeatOnFailureAnnotationTest extends AbstractNeodymiumTest
{
    @Test
    public void testClassRetryEndWithSuccessTest()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryEndWithSuccessTest.class);
        checkPass(summary, 3, 0);
    }

    @Test
    public void testClassRetryOnEveryErrorTest()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryOnEveryErrorTest.class);
        checkFail(summary, 3, 0, 1, "java.lang.AssertionError: Fail 3");
    }

    @Test
    public void testClassRetryOwnBrowserForSetupTest()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryOwnBrowserForSetupTest.class);
        checkFail(summary, 3, 0, 1, "java.lang.AssertionError: Fail 3");
    }

    @Test
    public void testClassNoRetryOnUnexpectedErrorTest()
    {
        NeodymiumTestExecutionSummary summary = run(ClassNoRetryOnUnexpectedErrorTest.class);
        checkFail(summary, 3, 0, 1, "java.lang.AssertionError: Shoul not be retried");
    }

    @Test
    public void testClassRetryStopOnSuccessTest()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryStopOnSuccessTest.class);
        checkPass(summary, 3, 0);
    }

    @Test
    public void testClassRetryWithoutSuccessInAfterTest()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryWithoutSuccessInAfterTest.class);
        checkFail(summary, 3, 0, 1, "java.lang.AssertionError: Fail 3");
    }

    @Test
    public void testClassRetryWithoutSuccessInBeforeTest()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryWithoutSuccessInBeforeTest.class);
        checkFail(summary, 3, 0, 1, "java.lang.AssertionError: Fail 3");
    }

    @Test
    public void testClassRetryWithoutSuccessMultiplicationTest()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryWithoutSuccessMultiplicationTest.class);
        checkFail(summary, 12, 0, 1, new HashMap<String, String>()
        {
            {
                put("test :: 2 :: Browser Chrome_1500x1000_headless", "java.lang.AssertionError: Fail 3");
            }
        });
    }

    @Test
    public void testClassRetryWithoutSuccessTest()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryWithoutSuccessTest.class);
        checkFail(summary, 3, 0, 1, "java.lang.AssertionError: Fail 3");
    }

    @Test
    public void testClassRetryStopOnUnexpectedFailureTest()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryStopOnUnexpectedFailureTest.class);
        checkFail(summary, 3, 0, 1, "java.lang.AssertionError: Fail 2");
    }

    @Test
    public void testClassRepeatOnFailureBrowserCombination()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryBrowserCombinationTest.class);
        checkFail(summary, 6, 0, 1, new HashMap<String, String>()
        {
            {
                put("testVisitingHomepage :: Browser Chrome_1500x1000_headless", "java.lang.AssertionError: Fail 3");
            }
        });
    }

    @Test
    public void testClassRepeatOnFailureTestdataCombination()
    {
        NeodymiumTestExecutionSummary summary = run(ClassRetryTestdataCombinationTest.class);
        checkFail(summary, 6, 0, 1, new HashMap<String, String>()
        {
            {
                put("testVisitingHomepage :: 2 :: Browser Chrome_headless", "java.lang.AssertionError: Fail 3");
            }
        });
    }

    @Test
    public void testMethodRepeatOnFailureBrowserCombination()
    {
        NeodymiumTestExecutionSummary summary = run(MethodRepeatOnFailureBrowserCombinationTest.class);
        checkFail(summary, 8, 0, 2, new HashMap<String, String>()
        {
            {
                put("testWithRetry :: Browser Chrome_1500x1000_headless", "java.lang.AssertionError: Fail 3");
                put("testWithoutRetry :: Browser Chrome_1500x1000_headless", "java.lang.AssertionError: Fail 1");
            }
        });
    }

    @Test
    public void testMethodRepeatOnFailureTestdataCombination()
    {
        NeodymiumTestExecutionSummary summary = run(MethodRepeatOnFailureTestdataCombinationTest.class);
        checkFail(summary, 6, 0, 1, new HashMap<String, String>()
        {
            {
                put("testVisitingHomepage :: 2 :: Browser Chrome_headless", "java.lang.AssertionError: Fail 3");
            }
        });
    }

    @Test
    public void testMethodRepeatOnFailureTest()
    {
        NeodymiumTestExecutionSummary summary = run(MethodRepeatOnFailureTest.class);
        checkFail(summary, 4, 0, 1, "java.lang.AssertionError: Parent fail");
    }

    @Test
    public void testOverwriteRetryTest()
    {
        NeodymiumTestExecutionSummary summary = run(OverwriteRetryTest.class);
        checkFail(summary, 6, 0, 2, new HashMap<String, String>()
        {
            {
                put("childTest :: Browser Chrome_headless", "java.lang.AssertionError: Fail Child 3");
                put("parentTest :: Browser Chrome_headless", "java.lang.AssertionError: Fail Parent 3");
            }
        });
    }
}
