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

import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.cucumber.CucumberSetBrowserViaTag;
import com.xceptance.neodymium.junit4.testclasses.cucumber.CucumberSetBrowserViaTagFail;
import com.xceptance.neodymium.junit4.testclasses.cucumber.CucumberSetBrowserViaTestData;
import com.xceptance.neodymium.junit4.testclasses.cucumber.CucumberSetBrowserViaTestDataFail;

public class CucumberTest extends NeodymiumTest
{
    @Test
    public void testSetBrowserViaTestData() throws Exception
    {
        Result result = run(CucumberSetBrowserViaTestData.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testSetBrowserViaTestDataFail() throws Exception
    {
        Result result = run(CucumberSetBrowserViaTestDataFail.class);
        checkFail(result, 1, 0, 1);
    }

    @Test
    public void testSetBrowserViaTag() throws Exception
    {
        Result result = run(CucumberSetBrowserViaTag.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testSetBrowserViaTagFail() throws Exception
    {
        Result result = run(CucumberSetBrowserViaTagFail.class);
        checkFail(result, 1, 0, 1);
    }
}
