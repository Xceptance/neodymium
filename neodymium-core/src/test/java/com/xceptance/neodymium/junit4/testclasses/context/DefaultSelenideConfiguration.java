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
package com.xceptance.neodymium.junit4.testclasses.context;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import com.codeborne.selenide.Configuration;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultSelenideConfiguration
{
    @Test
    public void test1() throws Exception
    {
        Neodymium.timeout(1234);
        Assert.assertEquals(1234, Configuration.timeout);

        Neodymium.fastSetValue(true);
        Assert.assertEquals(true, Configuration.fastSetValue);

        Neodymium.clickViaJs(true);
        Assert.assertEquals(true, Configuration.clickViaJs);
    }

    @Test
    public void test2() throws Exception
    {
        Assert.assertEquals(3000, Configuration.timeout);
        Assert.assertEquals(false, Configuration.fastSetValue);
        Assert.assertEquals(false, Configuration.clickViaJs);
    }
}
