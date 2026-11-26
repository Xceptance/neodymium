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
