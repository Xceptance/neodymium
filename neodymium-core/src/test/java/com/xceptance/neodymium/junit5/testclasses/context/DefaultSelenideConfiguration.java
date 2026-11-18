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
package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.Configuration;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

@Browser("Chrome_headless")
public class DefaultSelenideConfiguration
{
    @NeodymiumTest
    public void test1() throws Exception
    {
        Neodymium.timeout(1234);
        Assertions.assertEquals(1234, Configuration.timeout);

        Neodymium.fastSetValue(true);
        Assertions.assertEquals(true, Configuration.fastSetValue);

        Neodymium.clickViaJs(true);
        Assertions.assertEquals(true, Configuration.clickViaJs);
    }

    @NeodymiumTest
    public void test2() throws Exception
    {
        Assertions.assertEquals(3000, Configuration.timeout);
        Assertions.assertEquals(false, Configuration.fastSetValue);
        Assertions.assertEquals(false, Configuration.clickViaJs);
    }
}
