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
package com.xceptance.neodymium.junit4.tests;

import java.util.HashMap;

import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryBrowserCombinationTest;
import com.xceptance.neodymium.junit4.testclasses.repeat.classlevel.ClassRetryEmptyErrorMessageTest;
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
        Result result = run(ClassRetryEndWithSuccessTest.class);
        checkPass(result, 3, 0);
    }

    @Test
    public void testClassRetryOnEveryErrorTest()
    {
        Result result = run(ClassRetryOnEveryErrorTest.class);
        checkFail(result, 3, 0, 1, "Fail 3");
    }
    
    @Test
    public void testClassRetryEmptyErrorMessageTest()
    {
        Result result = run(ClassRetryEmptyErrorMessageTest.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testClassRetryOwnBrowserForSetupTest()
    {
        Result result = run(ClassRetryOwnBrowserForSetupTest.class);
        checkFail(result, 3, 0, 1, "Fail 3");
    }

    @Test
    public void testClassNoRetryOnUnexpectedErrorTest()
    {
        Result result = run(ClassNoRetryOnUnexpectedErrorTest.class);
        checkFail(result, 3, 0, 1, "Shoul not be retried");
    }

    @Test
    public void testClassRetryStopOnSuccessTest()
    {
        Result result = run(ClassRetryStopOnSuccessTest.class);
        checkPass(result, 3, 0);
    }

    @Test
    public void testClassRetryWithoutSuccessInAfterTest()
    {
        Result result = run(ClassRetryWithoutSuccessInAfterTest.class);
        checkFail(result, 3, 0, 1, "Fail 3");
    }

    @Test
    public void testClassRetryWithoutSuccessInBeforeTest()
    {
        Result result = run(ClassRetryWithoutSuccessInBeforeTest.class);
        checkFail(result, 3, 0, 1, "Fail 3");
    }

    @Test
    public void testClassRetryWithoutSuccessMultiplicationTest()
    {
        Result result = run(ClassRetryWithoutSuccessMultiplicationTest.class);
        checkFail(result, 12, 0, 1, "Fail 3");
    }

    @Test
    public void testClassRetryWithoutSuccessTest()
    {
        Result result = run(ClassRetryWithoutSuccessTest.class);
        checkFail(result, 3, 0, 1, "Fail 3");
    }

    @Test
    public void testClassRetryStopOnUnexpectedFailureTest()
    {
        Result result = run(ClassRetryStopOnUnexpectedFailureTest.class);
        checkFail(result, 3, 0, 1, "Fail 2");
    }

    @Test
    public void testClassRepeatOnFailureBrowserCombination()
    {
        Result result = run(ClassRetryBrowserCombinationTest.class);
        checkFail(result, 6, 0, 1, "Fail 3");
    }

    @Test
    public void testClassRepeatOnFailureTestdataCombination()
    {
        Result result = run(ClassRetryTestdataCombinationTest.class);
        checkFail(result, 6, 0, 1, "Fail 3");
    }

    @Test
    public void testMethodRepeatOnFailureBrowserCombination()
    {
        Result result = run(MethodRepeatOnFailureBrowserCombinationTest.class);
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
        Result result = run(MethodRepeatOnFailureTestdataCombinationTest.class);
        checkFail(result, 6, 0, 1, "Fail 3");
    }

    @Test
    public void testMethodRepeatOnFailureTest()
    {
        Result result = run(MethodRepeatOnFailureTest.class);
        checkFail(result, 4, 0, 1, "Parent fail");
    }

    @Test
    public void testOverwriteRetryTest()
    {
        Result result = run(OverwriteRetryTest.class);
        checkFail(result, 6, 0, 2, new HashMap<String, String>()
        {
            {
                put("childTest :: Browser Chrome_headless :: ", "Fail Child 3");
                put("parentTest :: Browser Chrome_headless :: ", "Fail Parent 3");
            }
        });
    }
}
