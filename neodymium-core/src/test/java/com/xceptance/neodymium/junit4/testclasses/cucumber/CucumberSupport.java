/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
