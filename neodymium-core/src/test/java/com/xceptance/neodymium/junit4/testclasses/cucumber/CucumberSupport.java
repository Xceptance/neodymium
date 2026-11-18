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
package com.xceptance.neodymium.junit4.testclasses.cucumber;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Assert;

import com.codeborne.selenide.logevents.SelenideLogger;
import org.neodymium.common.browser.BrowserMethodData;
import org.neodymium.junit4.NeodymiumCucumberRunListener;
import org.neodymium.util.Neodymium;
import org.neodymium.util.WebDriverUtils;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

public class CucumberSupport
{
    // setup hooks
    @Given("^\"([^\"]*)\" is open$")
    public static void setUp(final String browserProfileName)
    {
        WebDriverUtils.setUp(new BrowserMethodData(browserProfileName, //
                                                   Neodymium.configuration().keepBrowserOpen(), //
                                                   Neodymium.configuration().keepBrowserOpenOnFailure(), false, false, new ArrayList<Method>()), //
                             "CucumberSupport");
    }

    @Before("@SetUpWithBrowserTag")
    public static void setUp(Scenario scenario)
    {
        WebDriverUtils.setUpWithBrowserTag(scenario);
    }

    // have a lower order number than default in order to shut down the driver after
    // the test case specific after hooks
    @After(order = 100)
    public void tearDown(Scenario scenario)
    {
        WebDriverUtils.tearDown(scenario);
    }

    // test hooks
    @Given("^the browser \"([^\"]*)\" is setup$")
    public void validateBrowser(String browserProfileName)
    {
        Assert.assertEquals(browserProfileName, Neodymium.getBrowserProfileName());
    }

    @Then("^validate the AllureSelenide listener is active$")
    public void validateAllureSelenideListenerIsActive()
    {
        Assert.assertTrue(" AllureSelenide listener is not attached", SelenideLogger.hasListener(NeodymiumCucumberRunListener.LISTENER_NAME));
    }
}
