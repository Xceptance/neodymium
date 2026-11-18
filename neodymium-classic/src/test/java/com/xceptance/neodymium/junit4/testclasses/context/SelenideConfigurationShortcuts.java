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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.AssertionMode;
import com.codeborne.selenide.Configuration;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class SelenideConfigurationShortcuts
{
    @Test
    public void validateSoftAssertion()
    {
        Assert.assertEquals(Configuration.assertionMode, AssertionMode.STRICT);

        Neodymium.softAssertions(true);
        Assert.assertEquals(Configuration.assertionMode, AssertionMode.SOFT);

        Neodymium.softAssertions(false);
        Assert.assertEquals(Configuration.assertionMode, AssertionMode.STRICT);
    }

    @Test
    public void validateClickViaJs()
    {
        Assert.assertEquals(Configuration.clickViaJs, false);

        Neodymium.clickViaJs(true);
        Assert.assertEquals(Configuration.clickViaJs, true);

        Neodymium.clickViaJs(false);
        Assert.assertEquals(Configuration.clickViaJs, false);
    }

    @Test
    public void validateFastSetValue()
    {
        Assert.assertEquals(Configuration.fastSetValue, false);

        Neodymium.fastSetValue(true);
        Assert.assertEquals(Configuration.fastSetValue, true);

        Neodymium.fastSetValue(false);
        Assert.assertEquals(Configuration.fastSetValue, false);
    }

    @Test
    public void validateTimeout()
    {
        Assert.assertEquals(Configuration.timeout, 3000);

        Neodymium.timeout(1000);
        Assert.assertEquals(Configuration.timeout, 1000);

        Neodymium.timeout(2000);
        Assert.assertEquals(Configuration.timeout, 2000);
    }
}
