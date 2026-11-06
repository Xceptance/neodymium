package com.xceptance.neodymium.junit4.testclasses.context.cucumbercontextclear;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.NeodymiumConfiguration;
import com.xceptance.neodymium.util.WebDriverUtils;

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
