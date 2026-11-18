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
package com.xceptance.neodymium.junit4.testclasses.context.cucumbercontextclear;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Assert;

import org.neodymium.common.browser.BrowserMethodData;
import org.neodymium.util.Neodymium;
import org.neodymium.util.NeodymiumConfiguration;
import org.neodymium.util.WebDriverUtils;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;

public class CucumberContextSteps
{
    @Before
    public void beforeTest()
    {
        // setup browser since we set our selenide defaults only if a browser is involved
        WebDriverUtils.setUp(new BrowserMethodData("Chrome_headless", //
                                                   Neodymium.configuration().keepBrowserOpen(), //
                                                   Neodymium.configuration().keepBrowserOpenOnFailure(), false, false, new ArrayList<Method>()), //
                             "CucumberContextSteps");
    }

    @After(order = 100)
    public void teardown(Scenario scenario)
    {
        WebDriverUtils.tearDown(scenario);
    }

    @Given("Change timeout to 4000")
    public void testAndChangeDefaultTimeout()
    {
        NeodymiumConfiguration configuration = Neodymium.configuration();
        Assert.assertEquals(3000, configuration.selenideTimeout());

        configuration.setProperty("neodymium.selenide.timeout", "4000");
        Assert.assertEquals(4000, configuration.selenideTimeout());
    }

    @Given("Assert timeout of 3000")
    public void testDefaultTimeout()
    {
        NeodymiumConfiguration configuration = Neodymium.configuration();
        Assert.assertEquals(3000, configuration.selenideTimeout());
    }
}
