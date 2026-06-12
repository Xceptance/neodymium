package com.xceptance.neodymium.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.browserup.bup.BrowserUpProxy;
import com.codeborne.selenide.AssertionMode;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.generator.InteractiveHud;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookManager;
import com.xceptance.neodymium.common.TestStepListener;
import com.xceptance.neodymium.common.browser.WebDriverStateContainer;
import com.xceptance.neodymium.common.testdata.TestData;

/**
 * See our Github wiki: <a href="https://github.com/Xceptance/neodymium/wiki/Neodymium-context">Neodymium context</a>
 * 
 * @author m.kaufmann
 * @author m.pfotenhauer
 */
public class Neodymium
{
    private static final Map<Thread, Neodymium> CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    // keep our current WebDriver state
    private WebDriverStateContainer webDriverStateContainer;

    // keep our current AiBrowser instance
    private AiBrowser aiBrowser;

    // keep our active AI Playbook instance
    private Playbook activeAiPlaybook;

    // keep our interactive HUD instance
    private InteractiveHud interactiveHud;

    // keep our current browser profile name
    private String browserProfileName;

    // keep our active test data source file path/name
    private String testdataSourceFile;

    // keep our current browser name
    private String browserName;

    private String testName;

    private static List<String> browserFilter = generateBrowserFilter();

    // keep our current remote debugging port
    private int remoteDebuggingPort;

    // keep last used locator
    private By lastLocator;

    private WebElement lastUsedElement;

    // our global configuration
    private NeodymiumConfiguration configuration;

    // our AI configuration
    private AiConfiguration aiConfiguration;

    // localization
    private final NeodymiumLocalization localization;

    // our data for anywhere access
    private final TestData data = new TestData();

    public final static String TEMPORARY_CONFIG_FILE_PROPERTY_NAME = "neodymium.temporaryConfigFile";

    /**
     * Constructor
     */
    private Neodymium()
    {
        // the property needs to be a valid URI in order to satisfy the Owner framework
        if (null == ConfigFactory.getProperty(TEMPORARY_CONFIG_FILE_PROPERTY_NAME))
        {
            ConfigFactory.setProperty(TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:this/path/should/never/exist/noOneShouldCreateMe.properties");
        }
        configuration = ConfigFactory.create(NeodymiumConfiguration.class, System.getProperties(), System.getenv());
        aiConfiguration = ConfigFactory.create(AiConfiguration.class, System.getProperties(), System.getenv());
        localization = NeodymiumLocalization.build(configuration.localizationFile());
    }

    private static List<String> generateBrowserFilter()
    {
        // get test specific browser definitions (aka browser tag see browser.properties)
        // could be one value or comma separated list of values
        String filter = Neodymium.configuration().getBrowserFilter();
        filter = filter.replaceAll("\\s", "");

        // parse test specific browser definitions
        if (!StringUtils.isEmpty(filter))
        {
            return Arrays.asList(filter.split(","));
        }
        return new ArrayList<String>();
    }

    public static List<String> getBrowserFilter()
    {
        return browserFilter;
    }

    /**
     * Retrieves the context instance for the current Thread.
     * 
     * @return the context instance for the current Thread
     */
    static Neodymium getContext()
    {
        return CONTEXTS.computeIfAbsent(Thread.currentThread(), key -> {
            return new Neodymium();
        });
    }

    /**
     * Clears the context instance for the current Thread. <br>
     * Attention: clearing the context leads to a loss of dynamic test parameter: browserProfileName, data and driver.
     */
    public static void clearThreadContext()
    {
        CONTEXTS.remove(Thread.currentThread());
        TestStepListener.clearLastUrl();
    }

    /**
     * Shortcut for localized text access. Will fail with an assertion if the key cannot be found.<br>
     * Looks up the key in the localization setup starting the configured full locale e.g. 'en_US', falls back to the
     * language 'en' if not found, fallback to default, and finally break with an assertion if the key can't be found.
     *
     * @param key
     *            key to lookup
     * @return localized text
     */
    public static String localizedText(final String key)
    {
        return getContext().localization.getText(key);
    }

    /**
     * Shortcut for localized text access. Will fail with an assertion if the key cannot be found.<br>
     * Looks up the key in the localization setup starting the configured full locale e.g. 'en_US', falls back to the
     * language 'en' if not found, fallback to default, and finally break with an assertion if the key can't be found.
     *
     * @param key
     *            key to lookup
     * @return localized text or the key if no localization is found
     */
    public static String tryLocalizedText(final String key)
    {
        return getContext().localization.tryGetText(key);
    }

    /**
     * Shortcut for localized text access. Will fail with an assertion if the key cannot be found.<br>
     * Looks up the key in the localization setup starting the configured full locale e.g. 'en_US', falls back to the
     * language 'en' if not found, fallback to default, and finally break with an assertion if the key can't be found.
     *
     * @param key
     *            key to lookup
     * @param locale
     *            locale to lookup the key with
     * @return localized text
     */
    public static String localizedText(final String key, final String locale)
    {
        return getContext().localization.getText(key, locale);
    }

    /**
     * Shortcut for localized text access. Will fail with an assertion if the key cannot be found.<br>
     * Looks up the key in the localization setup starting the configured full locale e.g. 'en_US', falls back to the
     * language 'en' if not found, fallback to default, and finally break with an assertion if the key can't be found.
     *
     * @param key
     *            key to lookup
     * @param locale
     *            locale to lookup the key with
     * @return localized text or key of no localization is found
     */
    public static String tryLocalizedText(final String key, final String locale)
    {
        return getContext().localization.tryGetText(key, locale);
    }

    /**
     * Get the complete test data set.<br>
     * ATTENTION: does NOT set the flag to add the test data to the report. Use with care.
     * 
     * @return dataMap
     */
    public static TestData getData()
    {
        return getContext().data;
    }

    /**
     * Get the complete test data set and add set the flag to attach the test data to the report after the test
     * finished.
     *
     * @return dataMap
     */
    public static TestData getDataAndAddToReport()
    {
        TestData testData = getData();
        testData.setTestDataUsed();
        return testData;
    }

    /**
     * Shortcut for data access. Will fail with an assertion if the key cannot be found
     *
     * @param key
     *            key to lookup
     * @return value of the data map
     */
    public static String dataValue(final String key)
    {
        return getDataAndAddToReport().get(key);
    }

    /**
     * Get the current NeodymiumConfiguration instance
     * 
     * @return neodymiumConfiguration
     */
    public static NeodymiumConfiguration configuration()
    {
        return getContext().configuration;
    }

    /**
     * Get the current AiConfiguration instance
     * 
     * @return aiConfiguration
     */
    public static AiConfiguration aiConfiguration()
    {
        return getContext().aiConfiguration;
    }

    /**
     * Get access to the current Random instance of Neodymium. This can be used to have a fixed random setup to repeat
     * runs from CI executions.
     * 
     * @return random
     */
    public static Random getRandom()
    {
        return NeodymiumRandom.getNeodymiumRandom();
    }

    /**
     * Get the current state container for the WebDriver state objects.
     * 
     * @return webDriverStateContainer
     */
    public static WebDriverStateContainer getWebDriverStateContainer()
    {
        return getContext().webDriverStateContainer;
    }

    /**
     * Set the current state container for the WebDriver state objects.<br>
     * <b>Attention:</b> This function is mainly used to set information within the context internally.
     * 
     * @param webDriverStateContainer
     *            contains the state objects belonging to the current WebDriver
     */
    public static void setWebDriverStateContainer(WebDriverStateContainer webDriverStateContainer)
    {
        getContext().webDriverStateContainer = webDriverStateContainer;
    }

    /**
     * Get the current WebDriver
     * 
     * @return webDriver
     */
    public static WebDriver getDriver()
    {
        final WebDriverStateContainer wDSC = getContext().webDriverStateContainer;
        return wDSC == null ? null : wDSC.getWebDriver();
    }

    /**
     * Check if a WebDriver is currently set in the context.
     *
     * @return true if a WebDriver is set, false otherwise
     */
    public static boolean hasDriver()
    {
        final WebDriverStateContainer wDSC = getContext().webDriverStateContainer;
        return wDSC != null && wDSC.getWebDriver() != null;
    }

    /**
     * Get the current WebDriver as RemoteWebDriver
     *
     * @return remoteWebDriver
     */
    public static RemoteWebDriver getRemoteWebDriver()
    {
        final WebDriverStateContainer wDSC = getContext().webDriverStateContainer;
        return wDSC == null ? null : (RemoteWebDriver) wDSC.getWebDriver();
    }

    /**
     * Get the embedded local proxy if configured else null.<br>
     * Can be used to manipulate headers or retrieve har archives.
     * 
     * @return browserUpProxy
     */
    public static BrowserUpProxy getLocalProxy()
    {
        final WebDriverStateContainer wDSC = getContext().webDriverStateContainer;
        return wDSC == null ? null : wDSC.getProxy();
    }

    /**
     * Name of the current browser profile
     * 
     * @return browser profile name
     */
    public static String getBrowserProfileName()
    {
        return getContext().browserProfileName;
    }

    /**
     * Set the name of the current browser profile.<br>
     * <b>Attention:</b> This function is mainly used to set information within the context internally.
     * 
     * @param browserProfileName
     *            the name of the current browser profile
     */
    public static void setBrowserProfileName(String browserProfileName)
    {
        getContext().browserProfileName = browserProfileName;
    }

    /**
     * Get the active test data source file path/name.
     * 
     * @return test data source file path/name
     */
    public static String getTestdataSourceFile()
    {
        return getContext().testdataSourceFile;
    }

    /**
     * Set the active test data source file path/name.
     * 
     * @param testdataSourceFile
     *            the test data source file path/name
     */
    public static void setTestdataSourceFile(final String testdataSourceFile)
    {
        getContext().testdataSourceFile = testdataSourceFile;
    }

    /**
     * Get the current AiBrowser instance
     * 
     * @return aiBrowser
     */
    public static AiBrowser ai()
    {
        return getContext().aiBrowser;
    }

    /**
     * Set the current AiBrowser instance.<br>
     * <b>Attention:</b> This function is mainly used to set information within the context internally.
     * 
     * @param aiBrowser
     *            the AiBrowser to set
     */
    public static void setAiBrowser(AiBrowser aiBrowser)
    {
        getContext().aiBrowser = aiBrowser;
    }

    /**
     * Get the current active AI Playbook instance
     * 
     * @return playbook
     */
    public static Playbook getAiPlaybook()
    {
        return getContext().activeAiPlaybook;
    }

    /**
     * Set the current active AI Playbook instance
     * 
     * @param playbook
     *            the Playbook to set
     */
    public static void setAiPlaybook(Playbook playbook)
    {
        getContext().activeAiPlaybook = playbook;
    }

    /**
     * Get the current InteractiveHud instance
     * 
     * @return interactiveHud
     */
    public static InteractiveHud getInteractiveHud()
    {
        return getContext().interactiveHud;
    }

    /**
     * Get or create the InteractiveHud instance
     * 
     * @return interactiveHud
     */
    public static InteractiveHud getOrCreateInteractiveHud()
    {
        if (getContext().interactiveHud == null) {
            getContext().interactiveHud = new InteractiveHud();
        }
        return getContext().interactiveHud;
    }

    /**
     * Set the current InteractiveHud instance
     * 
     * @param interactiveHud
     *            the InteractiveHud to set
     */
    public static void setInteractiveHud(InteractiveHud interactiveHud)
    {
        getContext().interactiveHud = interactiveHud;
    }

    /**
     * Name of the current browser
     * 
     * @return browser name
     */
    public static String getBrowserName()
    {
        return getContext().browserName;
    }

    public static String getTestName()
    {
        return getContext().testName;
    }

    public static void setTestName(String testName)
    {
        getContext().testName = testName;
    }

    /**
     * Set the name of the current browser.<br>
     * <b>Attention:</b> This function is mainly used to set information within the context internally.
     * 
     * @param browserName
     *            the name of the current browser
     */
    public static void setBrowserName(String browserName)
    {
        getContext().browserName = browserName;
    }

    /**
     * Remote debugging port of the current bowser
     * 
     * @return remote debugging port
     */
    public static int getRemoteDebuggingPort()
    {
        return getContext().remoteDebuggingPort;
    }

    /**
     * Set the remote debugging port of the current browser.<br>
     * <b>Attention:</b> This function is mainly used to set information within the context internally.
     * 
     * @param remoteDebuggingPort
     *            the current browser port
     */
    public static void setRemoteDebuggingPort(int remoteDebuggingPort)
    {
        getContext().remoteDebuggingPort = remoteDebuggingPort;
    }

    /**
     * Current window width and height
     * 
     * @return {@link Dimension} object containing width and height of current window
     */
    public static Dimension getWindowSize()
    {
        if (hasDriver())
        {
            return getDriver().manage().window().getSize();
        }
        return new Dimension(0, 0);
    }

    /**
     * Current viewport width and height
     * 
     * @return {@link Dimension} object containing width and height of current viewport
     */
    public static Dimension getViewportSize()
    {
        Long width = Selenide.executeJavaScript("return window.innerWidth");
        Long height = Selenide.executeJavaScript("return window.innerHeight");

        return new Dimension(width.intValue(), height.intValue());
    }

    /**
     * Current page width and height
     * 
     * @return {@link Dimension} object containing width and height of current page
     */
    public static Dimension getPageSize()
    {
        Long width = Selenide.executeJavaScript("return document.documentElement.clientWidth");
        Long height = Selenide.executeJavaScript("return document.documentElement.clientHeight");

        return new Dimension(width.intValue(), height.intValue());
    }

    /**
     * Extra small devices (portrait phones or smaller)
     * 
     * @return boolean value indicating whether it is a mobile device or not
     */
    public static boolean isExtraSmallDevice()
    {
        return getViewportSize().getWidth() < configuration().smallDeviceBreakpoint();
    }

    /**
     * Small devices (landscape phones)
     * 
     * @return boolean value indicating whether it is a tablet device/large phone or not
     * @see Neodymium
     */
    public static boolean isSmallDevice()
    {
        int width = getViewportSize().getWidth();
        NeodymiumConfiguration cfg = configuration();

        return width >= cfg.smallDeviceBreakpoint() && width < cfg.mediumDeviceBreakpoint();
    }

    /**
     * Medium devices (Tablets and small desktop aka half window or stuff)
     * 
     * @return boolean value indicating whether it is a device with small desktop or not
     * @see Neodymium
     */
    public static boolean isMediumDevice()
    {
        int width = getViewportSize().getWidth();
        NeodymiumConfiguration cfg = configuration();

        return width >= cfg.mediumDeviceBreakpoint() && width < cfg.largeDeviceBreakpoint();
    }

    /**
     * Large devices (Desktop)
     * 
     * @return boolean value indicating whether it is a device with large desktop or not
     * @see Neodymium
     */
    public static boolean isLargeDevice()
    {
        int width = getViewportSize().getWidth();
        NeodymiumConfiguration cfg = configuration();

        return width >= cfg.largeDeviceBreakpoint() && width < cfg.xlargeDeviceBreakpoint();
    }

    /**
     * Extra large devices (Large desktop)
     * 
     * @return boolean value indicating whether it is a device with extra large resolution or not
     * @see Neodymium
     */
    public static boolean isExtraLargeDevice()
    {
        return getViewportSize().getWidth() >= configuration().xlargeDeviceBreakpoint();
    }

    /**
     * Mobile of any kind?
     * 
     * @return boolean indicating whether it is a mobile device or not
     * @see Neodymium
     */
    public static boolean isMobile()
    {
        return getViewportSize().getWidth() < configuration().mediumDeviceBreakpoint();
    }

    /**
     * Tablet of any kind?
     * 
     * @return boolean value indicating whether it is a tablet device/large phone or not
     * @see Neodymium
     */
    public static boolean isTablet()
    {
        return isMediumDevice();
    }

    /**
     * Desktop of any kind?
     * 
     * @return boolean value indicating whether it is a device desktop (isLargeDesktop() or isExtraLargeDesktop()) or
     *         not
     * @see Neodymium
     */
    public static boolean isDesktop()
    {
        return getViewportSize().getWidth() >= configuration().largeDeviceBreakpoint();
    }

    /**
     * Shortcut to turn on/off Selenide SoftAssertions <br>
     * You need to add the following JUnit rule to the test class to enable the feature
     * 
     * <pre>
     * &#64;Rule
     * public SoftAsserts softAsserts = new com.codeborne.selenide.junit.SoftAsserts();
     * </pre>
     * 
     * @param useSoftAssertions
     *            boolean if the Selenide soft assertion feature is activated
     */
    public static void softAssertions(boolean useSoftAssertions)
    {
        if (useSoftAssertions)
        {
            Configuration.assertionMode = AssertionMode.SOFT;
        }
        else
        {
            Configuration.assertionMode = AssertionMode.STRICT;
        }
    }

    /**
     * Shortcut to turn on/off Selenide clickViaJs
     * 
     * @param clickViaJs
     *            boolean that decides if a click is executed via JavaScript
     */
    public static void clickViaJs(boolean clickViaJs)
    {
        Configuration.clickViaJs = clickViaJs;
    }

    /**
     * Shortcut to turn on/off Selenide fastSetValue
     * 
     * @param fastSetValue
     *            boolean that decides if a value is set JavaScript
     */
    public static void fastSetValue(boolean fastSetValue)
    {
        Configuration.fastSetValue = fastSetValue;
    }

    /**
     * Shortcut to turn on/off Selenide timeout
     * 
     * @param timeout
     *            the time that a Selenide command waits implicitly before it raises an error if it can't be executed
     */
    public static void timeout(long timeout)
    {
        Configuration.timeout = timeout;
    }

    /**
     * Shortcut to set download folder
     * 
     * @param downloadFolder
     *            the directory where Selenide should store downloaded files
     */
    public static void downloadFolder(String downloadFolder)
    {
        Configuration.downloadsFolder = downloadFolder;
    }

    /**
     * Shortcut to enable/disable Selenides inbuild Screenshots
     * 
     * @param enable
     *            enable/disable Screenshots
     */
    public static void enableSelenideScreenshots(boolean enable)
    {
        Configuration.screenshots = enable;
    }

    /**
     * Validates if the currently configured site is equal to one or more Strings.
     * 
     * @param sites
     *            Names of the sites
     * @return boolean value indicating whether the configured site is matching one of the given Strings
     * @see Neodymium
     */
    public static boolean isSite(String... sites)
    {
        if (Neodymium.configuration().site() == null)
        {
            return false;
        }
        for (int i = 0; i < sites.length; i++)
        {
            if (Neodymium.configuration().site().equals(sites[i]))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates if the currently configured locale is equal to one or more Strings.
     * 
     * @param locales
     *            Names of the locales
     * @return boolean value indicating whether the configured locale is matching one of the given Strings
     * @see Neodymium
     */
    public static boolean isLocale(String... locales)
    {
        if (Neodymium.configuration().locale() == null)
        {
            return false;
        }
        for (int i = 0; i < locales.length; i++)
        {
            if (Neodymium.configuration().locale().equals(locales[i]))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the version of the currently used Neodymium library.
     * 
     * @return build version of Neodymium library
     */
    public static String getNeodymiumVersion()
    {
        final String buildVersion = getContext().getClass().getPackage().getImplementationVersion();
        return buildVersion == null ? "?.?.?" : buildVersion;
    }

    /**
     * Saves the last used locator
     * 
     * @param by
     *            CSS Selector
     */
    public static void setLastUsedLocator(By by)
    {
        getContext().lastLocator = by;
        getContext().lastUsedElement = null;
    }

    /**
     * Saves the last used locator and the element under which to search for
     * 
     * @param element
     *            Webelement
     * @param by
     *            CSS Selector
     */
    public static void setLastUsedLocator(WebElement element, By by)
    {
        getContext().lastLocator = by;
        getContext().lastUsedElement = element;
    }

    /**
     * Returns the last used Element
     * 
     * @return last used Element or null if nothing was set
     */
    public static WebElement getLastUsedElement()
    {
        try
        {
            if (getContext().lastUsedElement != null && getContext().lastLocator != null)
            {
                return getContext().lastUsedElement.findElement(getContext().lastLocator);
            }
            else if (getContext().lastLocator != null && hasDriver())
            {
                return getDriver().findElement(getContext().lastLocator);
            }
            else
            {
                return null;
            }
        }
        catch (Throwable t)
        {
            // if this breaks something upfront was already broken so we want the original error instead of this one
            // covring up
            return null;
        }
    }

    /**
     * Checks if the test already looked up any element. return whether there is a last element stored
     */
    public static boolean hasLastUsedElement()
    {
        if (getContext().lastUsedElement != null && getContext().lastLocator != null)
        {
            return true;
        }
        else if (getContext().lastLocator != null)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static void initializePlaybook()
    {
        Playbook playbook = Neodymium.getAiPlaybook();
        if (playbook == null)
        {
            final String playbookId = getTestName();

            final boolean skipReplay = Neodymium.getData().exists("skipReplay") && Neodymium.getData().asBoolean("skipReplay", false);

            if (!skipReplay)
            {
                playbook = PlaybookManager.loadPlaybook(playbookId);
            }

            if (playbook != null)
            {
                playbook.setRecording(false);
                Neodymium.setAiPlaybook(playbook);
            }
            else
            {
                playbook = new Playbook(playbookId);
                Neodymium.setAiPlaybook(playbook);
            }
        }
    }

    public static void expectFailure(final String bugId, final Runnable runnable)
    {
        new ExpectedFailureBuilder(bugId).run(runnable);
    }

    public static ExpectedFailureBuilder expectFailure(final String bugId)
    {
        return new ExpectedFailureBuilder(bugId);
    }

    public static class ExpectedFailureBuilder
    {
        private final String bugId;
        private final List<Class<? extends Throwable>> exceptionTypes = new ArrayList<>();
        private String messagePattern;

        public ExpectedFailureBuilder(final String bugId)
        {
            this.bugId = bugId;
        }

        public ExpectedFailureBuilder ofType(final Class<? extends Throwable> exceptionType)
        {
            this.exceptionTypes.add(exceptionType);
            return this;
        }

        public ExpectedFailureBuilder withMessage(final String messagePattern)
        {
            this.messagePattern = messagePattern;
            return this;
        }

        public void run(final Runnable runnable)
        {
            try
            {
                runnable.run();
            }
            catch (final Throwable t)
            {
                if (!exceptionTypes.isEmpty())
                {
                    boolean matched = false;
                    for (final Class<? extends Throwable> type : exceptionTypes)
                    {
                        if (type.isInstance(t))
                        {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched)
                    {
                        final String msg = "Expected failure of type(s) " + exceptionTypes + " but got " + t.getClass().getName() + " for bug: " + bugId;
                        throw new AssertionError(msg, t);
                    }
                }

                if (messagePattern != null && !messagePattern.isEmpty())
                {
                    final String actualMsg = t.getMessage() != null ? t.getMessage() : "";
                    if (!actualMsg.contains(messagePattern) && !messagePattern.contains(actualMsg))
                    {
                        final String msg = "Expected failure message pattern mismatch.\nExpected to contain or be contained in: \"" + messagePattern + "\"\nActual message: \"" + actualMsg + "\" for bug: " + bugId;
                        throw new AssertionError(msg, t);
                    }
                }

                return;
            }

            final String msg = "Expected code block to fail with " + (bugId != null ? "bug: " + bugId : "expected failure") + ", but it completed successfully.";
            throw new AssertionError(msg);
        }
    }

    /**
     * Re-creates the thread-local AI configuration instance from the latest system properties and env.
     */
    public static void reloadAiConfiguration()
    {
        getContext().aiConfiguration = ConfigFactory.create(AiConfiguration.class, System.getProperties(), System.getenv());
    }

    /**
     * Re-creates the thread-local main configuration instance from the latest system properties and env.
     */
    public static void reloadConfiguration()
    {
        getContext().configuration = ConfigFactory.create(NeodymiumConfiguration.class, System.getProperties(), System.getenv());
    }

}
