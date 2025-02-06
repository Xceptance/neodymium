package com.xceptance.neodymium.common.browser;

import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.browserup.bup.BrowserUpProxy;
import com.google.common.collect.ImmutableMap;
import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.common.recording.FilmTestExecution;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.AllureAddons.EnvironmentInfoMode;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserRunner
{
    public static Logger LOGGER = LoggerFactory.getLogger(BrowserRunner.class);

    private final boolean shouldLogBrowsers;

    private BrowserMethodData browserMethodData;

    private WebDriverStateContainer wDSCont;

    private MultibrowserConfiguration multibrowserConfiguration = MultibrowserConfiguration.getInstance();

    private String testName;

    private String recordingID;

    /**
     * Create browser runner for test
     * 
     * @param browserTag
     *            {@link BrowserMethodData} browser tag
     * @param testName
     *            {@link String} test name
     */
    public BrowserRunner(BrowserMethodData browserTag, String testName)
    {
        this.browserMethodData = browserTag;
        this.testName = testName;
        shouldLogBrowsers = Neodymium.configuration().enableBrowserEnvironmentData();
    }

    public BrowserRunner()
    {
        shouldLogBrowsers = Neodymium.configuration().enableBrowserEnvironmentData();
    }

    public void setUpTest()
    {
        setUpTest(browserMethodData, testName);
        recordingID = UUID.randomUUID().toString();
        if (FilmTestExecution.getContextGif().filmAutomatically())
        {
            FilmTestExecution.startGifRecording(recordingID);
        }
        if (FilmTestExecution.getContextVideo().filmAutomatically())
        {
            FilmTestExecution.startVideoRecording(recordingID);
        }
    }

    public void teardown(boolean testFailed)
    {
        teardown(testFailed, browserMethodData, wDSCont);
    }

    public void teardown(boolean testFailed, BrowserMethodData browserMethodData, WebDriverStateContainer wDSCont)
    {
        try
        {
            if (FilmTestExecution.getContextGif().filmAutomatically())
            {
                FilmTestExecution.finishGifFilming(recordingID, testFailed);
            }
            if (FilmTestExecution.getContextVideo().filmAutomatically())
            {
                FilmTestExecution.finishVideoFilming(recordingID, testFailed);
            }
            if (shouldLogBrowsers && Neodymium.getBrowserProfileName() != null)
            {
                AllureAddons.addEnvironmentInformation(ImmutableMap.<String, String> builder()
                                                                   .put("Used Browserconfigurations",
                                                                        Neodymium.getBrowserProfileName())
                                                                   .build(),
                                                       EnvironmentInfoMode.APPEND_VALUE);
            }
        }
        finally
        {
            teardown(testFailed, false, browserMethodData, wDSCont);
        }
    }

    /**
     * Sets the test instance up.
     * 
     * @param browserTag
     *            name of the browser corresponding to the browser.properties
     * @param testName
     *            name of the test to set up
     */
    public void setUpTest(BrowserMethodData browserTag, String testName)
    {
        validateBrowserTagIsKnown(browserTag.getBrowserTag());
        wDSCont = null;
        this.browserMethodData = browserTag;
        BrowserConfiguration browserConfiguration = multibrowserConfiguration.getBrowserProfiles().get(browserTag.getBrowserTag());
        try
        {
            // try to find appropriate web driver in cache before create a new instance
            if (Neodymium.configuration().reuseWebDriver())
            {
                wDSCont = WebDriverCache.instance.removeWebDriverStateContainerByBrowserTag(browserConfiguration.getConfigTag());
                if (wDSCont != null && wDSCont.getWebDriver() != null)
                {
                    wDSCont.getWebDriver().manage().deleteAllCookies();
                }
            }

            if (wDSCont == null)
            {
                wDSCont = BrowserRunnerHelper.createWebDriverStateContainer(browserConfiguration, testName);
            }
            else
            {
                LOGGER.debug("Browser instance served from cache");
            }
        }
        catch (final MalformedURLException e)
        {
            throw new RuntimeException("An error occurred during URL creation. See nested exception.", e);
        }
        if (wDSCont != null)
        {
            // set browser window size
            BrowserRunnerHelper.setBrowserWindowSize(browserConfiguration, wDSCont.getWebDriver());
            Neodymium.setWebDriverStateContainer(wDSCont);
            Neodymium.setBrowserProfileName(browserConfiguration.getConfigTag());
            Neodymium.setBrowserName(browserConfiguration.getCapabilities().getBrowserName());

            initSelenideConfiguration();
        }
        else
        {
            throw new RuntimeException("Could not create driver for browsertag: " + browserConfiguration.getConfigTag() +
                                       ". Please check your browserconfigurations.");
        }
    }

    private void initSelenideConfiguration()
    {
        // set our default timeout
        Neodymium.timeout(Neodymium.configuration().selenideTimeout());

        Neodymium.enableSelenideScreenshots(!Neodymium.configuration().enableAdvancedScreenShots());

        Neodymium.fastSetValue(Neodymium.configuration().selenideFastSetValue());
        Neodymium.clickViaJs(Neodymium.configuration().selenideClickViaJs());
    }

    public void teardown(boolean testFailed, boolean preventReuse, BrowserMethodData browserMethodData, WebDriverStateContainer webDriverStateContainer)
    {
        BrowserConfiguration browserConfiguration = multibrowserConfiguration.getBrowserProfiles().get(Neodymium.getBrowserProfileName());
        // keep browser open
        if (keepOpen(testFailed, browserMethodData, browserConfiguration))
        {
            LOGGER.debug("Keep browser open");
            // nothing to do
        }
        // reuse
        else if (canReuse(preventReuse, webDriverStateContainer))
        {
            LOGGER.debug("Put browser into cache");
            webDriverStateContainer.incrementUsedCount();
            WebDriverCache.instance.putWebDriverStateContainer(browserMethodData.getBrowserTag(), webDriverStateContainer);
        }
        // close the WebDriver
        else if (webDriverStateContainer != null)
        {
            LOGGER.debug("Teardown browser");
            WebDriver webDriver = webDriverStateContainer.getWebDriver();
            if (webDriver != null)
            {
                webDriver.quit();
            }

            BrowserUpProxy proxy = webDriverStateContainer.getProxy() != null ? webDriverStateContainer.getProxy() : null;
            if (proxy != null)
            {
                try
                {
                    proxy.stop();
                }
                catch (IllegalStateException e)
                {
                    // nothing to do here except for catching error of a second stop of the proxy
                }
            }
        }

        Neodymium.setWebDriverStateContainer(null);
        Neodymium.setBrowserProfileName(null);
        Neodymium.setBrowserName(null);
    }

    private boolean keepOpen(boolean testFailed, BrowserMethodData browserMethodData, BrowserConfiguration browserConfiguration)
    {
        return (browserConfiguration != null && !browserConfiguration.isHeadless())
               && (((browserMethodData.isKeepBrowserOpenOnFailure()) && testFailed) ||
                   (browserMethodData.isKeepBrowserOpen() && !browserMethodData.isKeepBrowserOpenOnFailure()));
    }

    private boolean canReuse(boolean preventReuse, WebDriverStateContainer webDriverStateContainer)
    {
        boolean maxReuseReached = (Neodymium.configuration().maxWebDriverReuse() > 0)
                                  && (webDriverStateContainer.getUsedCount() >= Neodymium.configuration().maxWebDriverReuse());
        return Neodymium.configuration().reuseWebDriver() && !preventReuse && !maxReuseReached && isWebDriverStillOpen(webDriverStateContainer.getWebDriver());
    }

    private boolean isWebDriverStillOpen(final WebDriver webDriver)
    {
        if (webDriver == null)
        {
            return false;
        }
        try
        {
            RemoteWebDriver driver = (RemoteWebDriver) webDriver;
            return driver.getSessionId() != null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LOGGER.debug("Couldn't detect if the WebDriver is still open!");
            return true;
        }
    }

    public List<String> getBrowserTags()
    {
        // make a copy of all available browser tags
        List<String> tags = new LinkedList<>();
        tags.addAll(multibrowserConfiguration.getBrowserProfiles().keySet());
        return tags;
    }

    public void validateBrowserTagIsKnown(String browserTag)
    {
        if (!getBrowserTags().contains(browserTag))
        {
            throw new IllegalArgumentException("Can not find browser configuration with tag: " + browserTag);
        }
    }
}
