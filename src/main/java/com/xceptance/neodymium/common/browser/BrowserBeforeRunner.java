package com.xceptance.neodymium.common.browser;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.common.Data;
import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserBeforeRunner
{
    public void run(Supplier<Throwable> beforeMethodInvocation, Method before, boolean junit5) throws Throwable
    {
        WebDriverStateContainer oldWDsCont = Neodymium.getWebDriverStateContainer();
        BrowserConfiguration oldBrowserConfiguration = MultibrowserConfiguration.getInstance().getBrowserProfiles()
                                                                                .get(Neodymium.getBrowserProfileName());

        boolean startNewBrowserForSetUp = shouldStartNewBrowser(before);

        BrowserConfiguration browserConfiguration = oldBrowserConfiguration;

        List<Browser> methodBrowserAnnotations = Data.getAnnotations(before, Browser.class);

        // if @Before method is annotated with @Browser tag, it might need to be executed with another
        // browser
        if (!methodBrowserAnnotations.isEmpty())
        {
            browserConfiguration = MultibrowserConfiguration.getInstance().getBrowserProfiles()
                                                            .get(methodBrowserAnnotations.get(0).value());
        }

        // if browserConfiguration is null, the browser should not be started for this method and browserTag and
        // browserRunner are therefore not required
        BrowserMethodData browserTag = browserConfiguration != null ? BrowserData.addKeepBrowserOpenInformation(browserConfiguration.getBrowserTag(), before)
                                                                    : null;
        BrowserRunner browserRunner = browserTag != null ? new BrowserRunner(browserTag, before.getName()) : null;

        // if we don't need to start new browser for the setup and the browser for the test was not suppressed
        // it means that we should use the same browser for setup
        // as the might have been other @Before methods with new browser running previously, let's explicitly set
        // the driver to original
        if (!startNewBrowserForSetUp && (Neodymium.getDriver() == null || !Neodymium.getDriver().equals(oldWDsCont.getWebDriver()))
            && oldWDsCont != null)
        {
            WebDriverRunner.setWebDriver(oldWDsCont.getWebDriver());
            Neodymium.setWebDriverStateContainer(oldWDsCont);
            Neodymium.setBrowserProfileName(oldBrowserConfiguration.getConfigTag());
            Neodymium.setBrowserName(oldBrowserConfiguration.getCapabilities().getBrowserName());
        }

        // if we need to start new browser for the set up and any browser configuration for the @Before method
        // was found, create a new driver
        else if (startNewBrowserForSetUp && browserConfiguration != null)
        {
            browserRunner.setUpTest();

        }
        else if (startNewBrowserForSetUp)
        {
            throw new RuntimeException("No browser setting for " + (junit5 ? "@BeforeEach" : "@Before") + " method '" + before.getName()
                                       + "' was found. "
                                       + "If browser is suppressed for the test and is also not required for the set up,"
                                       + " please mark the " + (junit5 ? "@BeforeEach" : "@Before") + " method with @DontStartNewBrowserForSetUp annotation."
                                       + " If you need to start a browser for the set up,"
                                       + " please, use @Browser"
                                       + " annotaion to mention what browser should be used exactly for this " + (junit5 ? "@BeforeEach" : "@Before") + ".");
        }
        boolean beforeFailed = false;
        try
        {
            Throwable e = beforeMethodInvocation.get();
            if (e != null)
            {
                beforeFailed = true;
                throw e;
            }
        }
        finally
        {
            // if we did a set up of new driver before the @Before method, we need to close it
            if (startNewBrowserForSetUp && browserConfiguration != null)
            {
                browserRunner.teardown(beforeFailed);
            }

            // set driver back to the original to execute the test or clean up (in case of failure)
            // Neodymium.setWebDriverStateContainer(oldWDsCont);
            if (oldWDsCont != null)
            {
                WebDriverRunner.setWebDriver(oldWDsCont.getWebDriver());
                Neodymium.setWebDriverStateContainer(oldWDsCont);
            }
            if (oldBrowserConfiguration != null)
            {
                Neodymium.setBrowserProfileName(oldBrowserConfiguration.getConfigTag());
                Neodymium.setBrowserName(oldBrowserConfiguration.getCapabilities().getBrowserName());
            }
        }
    }

    public static boolean shouldStartNewBrowser(Method each)
    {
        List<StartNewBrowserForSetUp> methodStartNewBrowserForSetUp = Data.getAnnotations(each,
                                                                                          StartNewBrowserForSetUp.class);
        List<StartNewBrowserForSetUp> classStartNewBrowserForSetUp = Data.getAnnotations(each.getDeclaringClass(),
                                                                                         StartNewBrowserForSetUp.class);

        // if global config for dontStartNewBrowserForSetUp is set to false, we should not reach this point
        boolean startNewBrowserForSetUp = false;
        if (!classStartNewBrowserForSetUp.isEmpty())
        {
            startNewBrowserForSetUp = true;
        }

        if (!methodStartNewBrowserForSetUp.isEmpty())
        {
            startNewBrowserForSetUp = true;
        }

        // if @Before method is annotated with @SuppressBrowser annotation, no new browser should be started
        return startNewBrowserForSetUp;
    }

    public static boolean isSuppressed(Method each)
    {
        List<SuppressBrowsers> methodSuppressBrowserAnnotations = Data.getAnnotations(each, SuppressBrowsers.class);

        return !methodSuppressBrowserAnnotations.isEmpty();
    }
}