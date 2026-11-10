package com.xceptance.neodymium.junit4.testclasses.cucumber;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Assert;

import com.codeborne.selenide.logevents.SelenideLogger;
import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.junit4.NeodymiumCucumberRunListener;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.WebDriverUtils;

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
