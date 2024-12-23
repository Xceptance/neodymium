package com.xceptance.neodymium.common.browser;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.common.Data;
import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserAfterRunner
{
    public void run(Supplier<Throwable> afterMethodInvocation, Method after, boolean junit5) throws Throwable
    {
        WebDriverStateContainer oldDriver = Neodymium.getWebDriverStateContainer();
        BrowserConfiguration oldBrowserConfiguration = MultibrowserConfiguration.getInstance().getBrowserProfiles()
                                                                                .get(Neodymium.getBrowserProfileName());
        boolean isSuppressed = isSuppressed(after);
        boolean startNewBrowserForCleanUp = shouldStartNewBrowser(after) && !isSuppressed;

        BrowserConfiguration browserConfiguration = oldBrowserConfiguration;
        List<Browser> methodBrowserAnnotations = Data.getAnnotations(after, Browser.class);

        // if @After method is annotated with @Browser tag, it might need to be executed with another
        // browser
        if (!methodBrowserAnnotations.isEmpty())
        {
            browserConfiguration = MultibrowserConfiguration.getInstance().getBrowserProfiles()
                                                            .get(methodBrowserAnnotations.get(0).value());
        }

        // if browserConfiguration is null, the browser should not be started for this method and browserTag and
        // browserRunner are therefore not required
        BrowserMethodData browserTag = browserConfiguration != null ? BrowserData.addKeepBrowserOpenInformation(browserConfiguration.getBrowserTag(), after)
                                                                    : null;
        BrowserRunner browserRunner = browserTag != null ? new BrowserRunner(browserTag, after.getName()) : null;

        // if we don't need to start new browser for the cleanup and the browser for the test was not suppressed
        // it means that we should use the same browser for cleanup
        // as the might have been other @After methods with new browser running previously, let's explicitly set
        // the driver to original
        if (!startNewBrowserForCleanUp && !isSuppressed && (Neodymium.getDriver() == null || !Neodymium.getDriver().equals(oldDriver.getWebDriver()))
            && oldDriver != null)
        {
            WebDriverRunner.setWebDriver(oldDriver.getWebDriver());
            Neodymium.setWebDriverStateContainer(oldDriver);
            Neodymium.setBrowserProfileName(oldBrowserConfiguration.getConfigTag());
            Neodymium.setBrowserName(oldBrowserConfiguration.getCapabilities().getBrowserName());
        }
        // if we need to start new browser for the clean up and any browser configuration for the @After method
        // was found, create a new driver
        else if (startNewBrowserForCleanUp && browserConfiguration != null)
        {
            browserRunner.setUpTest();
        }
        else if (!isSuppressed && browserConfiguration == null)
        {
            throw new RuntimeException("No browser setting for " + (junit5 ? "@AfterEach" : "@After") + " method '" + (after.getName()) + "' was found. "
                                       + "If browser was suppressed for the test but is annotated with @StartNewBrowserForCleanUp because browser isrequired for the clean up,"
                                       + " please, use @Browser annotation to specify what browser is required for this clean up.");
        }
        boolean beforeFailed = false;
        try
        {
            Throwable e = afterMethodInvocation.get();
            if (e != null)
            {
                beforeFailed = true;
                throw e;
            }
        }
        finally
        {
            // if we did a set up of new driver before the @After method, we need to close it
            if (startNewBrowserForCleanUp && browserConfiguration != null)
            {
                browserRunner.teardown(beforeFailed);
                if (oldDriver != null)
                {
                    WebDriverRunner.setWebDriver(oldDriver.getWebDriver());
                    Neodymium.setWebDriverStateContainer(oldDriver);
                }
                if (oldBrowserConfiguration != null)
                {
                    Neodymium.setBrowserProfileName(oldBrowserConfiguration.getConfigTag());
                    Neodymium.setBrowserName(oldBrowserConfiguration.getCapabilities().getBrowserName());
                }
            }
        }
    }

    private boolean shouldStartNewBrowser(Method each)
    {
        List<StartNewBrowserForCleanUp> methodStartNewBrowserForCleanUp = Data.getAnnotations(each,
                                                                                              StartNewBrowserForCleanUp.class);
        List<StartNewBrowserForCleanUp> classStartNewBrowserForCleanUp = Data.getAnnotations(each.getDeclaringClass(),
                                                                                             StartNewBrowserForCleanUp.class);

        // if global config for dontStartNewBrowserForCleanUp is set to false, we should not reach this point
        boolean startNewBrowserForCleanUp = false;
        if (!classStartNewBrowserForCleanUp.isEmpty())
        {
            startNewBrowserForCleanUp = true;
        }

        if (!methodStartNewBrowserForCleanUp.isEmpty())
        {
            startNewBrowserForCleanUp = true;
        }

        return startNewBrowserForCleanUp;
    }

    private boolean isSuppressed(Method each)
    {
        List<SuppressBrowsers> methodSuppressBrowserAnnotations = Data.getAnnotations(each, SuppressBrowsers.class);

        return !methodSuppressBrowserAnnotations.isEmpty();
    }
}
