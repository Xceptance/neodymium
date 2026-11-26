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

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.wip.WIPAnnotationChildTest;
import com.xceptance.neodymium.junit4.testclasses.wip.WIPAnnotationTest;
import com.xceptance.neodymium.junit4.testclasses.wip.WIPMultiplicationTest;
import com.xceptance.neodymium.util.Neodymium;

public class WIPTest extends NeodymiumTest
{
    @Before
    public void setJUnitViewModeFlat()
    {
        Neodymium.configuration().setProperty("neodymium.junit.viewmode", "flat");
        Neodymium.configuration().setProperty("neodymium.workInProgress", "true");
    }

    @Test
    public void testWIPAnnotation() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: "
        };
        checkDescription(WIPAnnotationTest.class, expected);

        Result result = run(WIPAnnotationTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testWIPAnnotationChild() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: ",
          "third :: "
        };
        checkDescription(WIPAnnotationChildTest.class, expected);

        Result result = run(WIPAnnotationChildTest.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testWIPMultiplication() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: 1 1 / 2 :: Browser Chrome_headless :: ",
          "first :: 2 2 / 2 :: Browser Chrome_headless :: ",
          "first :: 1 1 / 2 :: Browser Chrome_1500x1000_headless :: ",
          "first :: 2 2 / 2 :: Browser Chrome_1500x1000_headless :: ",
        };
        checkDescription(WIPMultiplicationTest.class, expected);
        Result result = run(WIPMultiplicationTest.class);
        checkPass(result, 4, 0);
    }
}
