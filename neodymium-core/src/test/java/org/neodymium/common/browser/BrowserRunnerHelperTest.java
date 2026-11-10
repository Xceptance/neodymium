package org.neodymium.common.browser;

import java.io.File;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.common.browser.BrowserRunnerHelper.Sleeper;
import org.neodymium.common.browser.BrowserRunnerHelper.WebDriverStateContainerSupplier;
import org.neodymium.util.Neodymium;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import com.browserup.bup.BrowserUpProxy;
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.common.browser.wrappers.ChromeBuilder;
import org.neodymium.common.browser.wrappers.EdgeBuilder;
import org.neodymium.common.browser.wrappers.IEBuilder;
import org.openqa.selenium.firefox.GeckoDriverService.Builder;

/**
 * Tests for {@link BrowserRunnerHelper} retry mechanism on {@link SessionNotCreatedException} and cleanup.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class BrowserRunnerHelperTest
{
    private String originalMaxRetries;

    private String originalPauseMin;

    private String originalPauseMax;

    private String originalChromeDriverPath;

    private String originalFirefoxDriverPath;

    private String originalEdgeDriverPath;

    private String originalIeDriverPath;

    @BeforeEach
    public void setUp()
    {
        originalMaxRetries = Neodymium.configuration().getProperty("neodymium.webDriver.sessionRetry.maxRetries");
        originalPauseMin = Neodymium.configuration().getProperty("neodymium.webDriver.sessionRetry.pause.min");
        originalPauseMax = Neodymium.configuration().getProperty("neodymium.webDriver.sessionRetry.pause.max");
        originalChromeDriverPath = Neodymium.configuration().getProperty("neodymium.webDriver.chrome.pathToDriverServer");
        originalFirefoxDriverPath = Neodymium.configuration().getProperty("neodymium.webDriver.firefox.pathToDriverServer");
        originalEdgeDriverPath = Neodymium.configuration().getProperty("neodymium.webDriver.edge.pathToDriverServer");
        originalIeDriverPath = Neodymium.configuration().getProperty("neodymium.webDriver.ie.pathToDriverServer");
    }

    @AfterEach
    public void tearDown()
    {
        restoreProperty("neodymium.webDriver.sessionRetry.maxRetries", originalMaxRetries);
        restoreProperty("neodymium.webDriver.sessionRetry.pause.min", originalPauseMin);
        restoreProperty("neodymium.webDriver.sessionRetry.pause.max", originalPauseMax);
        restoreProperty("neodymium.webDriver.chrome.pathToDriverServer", originalChromeDriverPath);
        restoreProperty("neodymium.webDriver.firefox.pathToDriverServer", originalFirefoxDriverPath);
        restoreProperty("neodymium.webDriver.edge.pathToDriverServer", originalEdgeDriverPath);
        restoreProperty("neodymium.webDriver.ie.pathToDriverServer", originalIeDriverPath);
    }

    private void restoreProperty(final String key, final String value)
    {
        if (value != null)
        {
            Neodymium.configuration().setProperty(key, value);
        }
        else
        {
            Neodymium.configuration().removeProperty(key);
        }
    }

    @Test
    public void testSuccessfulCreationWithoutRetry() throws MalformedURLException
    {
        final WebDriverStateContainer expectedContainer = new WebDriverStateContainer();
        final AtomicInteger callCount = new AtomicInteger(0);
        final List<Long> sleepDurations = new ArrayList<>();

        final WebDriverStateContainerSupplier supplier = () ->
        {
            callCount.incrementAndGet();
            return expectedContainer;
        };

        final Sleeper sleeper = sleepDurations::add;

        final WebDriverStateContainer result = BrowserRunnerHelper.createWebDriverStateContainerWithRetry(supplier, sleeper);

        Assertions.assertSame(expectedContainer, result);
        Assertions.assertEquals(1, callCount.get());
        Assertions.assertTrue(sleepDurations.isEmpty());
    }

    @Test
    public void testTransientFailureRetriesAndSucceeds() throws MalformedURLException
    {
        final WebDriverStateContainer expectedContainer = new WebDriverStateContainer();
        final AtomicInteger callCount = new AtomicInteger(0);
        final List<Long> sleepDurations = new ArrayList<>();

        final WebDriverStateContainerSupplier supplier = () ->
        {
            if (callCount.incrementAndGet() == 1)
            {
                throw new SessionNotCreatedException("session not created from chrome not reachable");
            }
            return expectedContainer;
        };

        final Sleeper sleeper = sleepDurations::add;

        final WebDriverStateContainer result = BrowserRunnerHelper.createWebDriverStateContainerWithRetry(supplier, sleeper);

        Assertions.assertSame(expectedContainer, result);
        Assertions.assertEquals(2, callCount.get());
        Assertions.assertEquals(1, sleepDurations.size());
        final long slept = sleepDurations.get(0);
        Assertions.assertTrue(slept >= 2000L && slept <= 10000L,
                            "Expected pause between 2000 and 10000 ms, but was: " + slept);
    }

    @Test
    public void testPersistentFailureAbortsAfterSingleRetry()
    {
        final AtomicInteger callCount = new AtomicInteger(0);
        final List<Long> sleepDurations = new ArrayList<>();

        final WebDriverStateContainerSupplier supplier = () ->
        {
            callCount.incrementAndGet();
            throw new SessionNotCreatedException("session not created from chrome not reachable");
        };

        final Sleeper sleeper = sleepDurations::add;

        final SessionNotCreatedException thrown = Assertions.assertThrows(SessionNotCreatedException.class, () ->
        {
            BrowserRunnerHelper.createWebDriverStateContainerWithRetry(supplier, sleeper);
        });

        Assertions.assertTrue(thrown.getMessage().contains("chrome not reachable"));
        Assertions.assertEquals(2, callCount.get());
        Assertions.assertEquals(1, sleepDurations.size());
    }

    @Test
    public void testUnrelatedExceptionPropagatesImmediatelyWithoutRetry()
    {
        final AtomicInteger callCount = new AtomicInteger(0);
        final List<Long> sleepDurations = new ArrayList<>();

        final WebDriverStateContainerSupplier supplier = () ->
        {
            callCount.incrementAndGet();
            throw new IllegalArgumentException("Invalid argument");
        };

        final Sleeper sleeper = sleepDurations::add;

        Assertions.assertThrows(IllegalArgumentException.class, () ->
        {
            BrowserRunnerHelper.createWebDriverStateContainerWithRetry(supplier, sleeper);
        });

        Assertions.assertEquals(1, callCount.get());
        Assertions.assertTrue(sleepDurations.isEmpty());
    }

    @Test
    public void testZeroRetriesConfiguredFailsImmediately()
    {
        Neodymium.configuration().setProperty("neodymium.webDriver.sessionRetry.maxRetries", "0");

        final AtomicInteger callCount = new AtomicInteger(0);
        final List<Long> sleepDurations = new ArrayList<>();

        final WebDriverStateContainerSupplier supplier = () ->
        {
            callCount.incrementAndGet();
            throw new SessionNotCreatedException("Immediate failure");
        };

        final Sleeper sleeper = sleepDurations::add;

        Assertions.assertThrows(SessionNotCreatedException.class, () ->
        {
            BrowserRunnerHelper.createWebDriverStateContainerWithRetry(supplier, sleeper);
        });

        Assertions.assertEquals(1, callCount.get());
        Assertions.assertTrue(sleepDurations.isEmpty());
    }

    @Test
    public void testThreadInterruptedDuringSleepRestoresFlagAndThrows()
    {
        final AtomicInteger callCount = new AtomicInteger(0);

        final WebDriverStateContainerSupplier supplier = () ->
        {
            callCount.incrementAndGet();
            throw new SessionNotCreatedException("Failure triggering sleep");
        };

        final Sleeper sleeper = millis ->
        {
            throw new InterruptedException("Interrupted during test");
        };

        final SessionNotCreatedException thrown = Assertions.assertThrows(SessionNotCreatedException.class, () ->
        {
            BrowserRunnerHelper.createWebDriverStateContainerWithRetry(supplier, sleeper);
        });

        Assertions.assertTrue(thrown.getMessage().contains("Failure triggering sleep"));
        Assertions.assertTrue(Thread.currentThread().isInterrupted());
        // Clear interrupted status for subsequent tests
        Assertions.assertTrue(Thread.interrupted());
        Assertions.assertEquals(1, callCount.get());
    }

    @Test
    public void testCalculateRetryPauseMsHonorsFixedBounds()
    {
        Neodymium.configuration().setProperty("neodymium.webDriver.sessionRetry.pause.min", "5000");
        Neodymium.configuration().setProperty("neodymium.webDriver.sessionRetry.pause.max", "5000");

        final long pause = BrowserRunnerHelper.calculateRetryPauseMs();
        Assertions.assertEquals(5000L, pause);
    }

    @Test
    public void testCalculateRetryPauseMsHonorsInvertedBounds()
    {
        Neodymium.configuration().setProperty("neodymium.webDriver.sessionRetry.pause.min", "6000");
        Neodymium.configuration().setProperty("neodymium.webDriver.sessionRetry.pause.max", "3000");

        final long pause = BrowserRunnerHelper.calculateRetryPauseMs();
        Assertions.assertTrue(pause >= 3000L && pause <= 6000L, "Expected between 3000 and 6000, got: " + pause);
    }

    @Test
    public void testCleanupWebDriverStateContainerQuitsDriverAndStopsProxy()
    {
        final AtomicBoolean quitCalled = new AtomicBoolean(false);
        final AtomicBoolean stopCalled = new AtomicBoolean(false);

        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            WebDriver.class.getClassLoader(),
            new Class<?>[]{ WebDriver.class },
            (proxy, method, args) ->
            {
                if ("quit".equals(method.getName()))
                {
                    quitCalled.set(true);
                }
                return null;
            });

        final BrowserUpProxy mockProxy = (BrowserUpProxy) Proxy.newProxyInstance(
            BrowserUpProxy.class.getClassLoader(),
            new Class<?>[]{ BrowserUpProxy.class },
            (proxy, method, args) ->
            {
                if ("stop".equals(method.getName()))
                {
                    stopCalled.set(true);
                }
                return null;
            });

        final WebDriverStateContainer wDSC = new WebDriverStateContainer();
        wDSC.setWebDriver(mockDriver);
        wDSC.setProxy(mockProxy);

        BrowserRunnerHelper.cleanupWebDriverStateContainer(wDSC);

        Assertions.assertTrue(quitCalled.get(), "Expected webDriver.quit() to be called");
        Assertions.assertTrue(stopCalled.get(), "Expected proxy.stop() to be called");
    }

    @Test
    public void testCleanupHandlesNullAndExceptionsGracefully()
    {
        // Must not throw for null
        Assertions.assertDoesNotThrow(() -> BrowserRunnerHelper.cleanupWebDriverStateContainer(null));

        final WebDriver throwingDriver = (WebDriver) Proxy.newProxyInstance(
            WebDriver.class.getClassLoader(),
            new Class<?>[]{ WebDriver.class },
            (proxy, method, args) ->
            {
                if ("quit".equals(method.getName()))
                {
                    throw new WebDriverException("Driver quit error");
                }
                return null;
            });

        final BrowserUpProxy throwingProxy = (BrowserUpProxy) Proxy.newProxyInstance(
            BrowserUpProxy.class.getClassLoader(),
            new Class<?>[]{ BrowserUpProxy.class },
            (proxy, method, args) ->
            {
                if ("stop".equals(method.getName()))
                {
                    throw new IllegalStateException("Proxy already stopped");
                }
                return null;
            });

        final WebDriverStateContainer wDSC = new WebDriverStateContainer();
        wDSC.setWebDriver(throwingDriver);
        wDSC.setProxy(throwingProxy);

        Assertions.assertDoesNotThrow(() -> BrowserRunnerHelper.cleanupWebDriverStateContainer(wDSC));
    }

    @Test
    public void testCreateChromeBuilderWithoutConfiguredDriverPathLeavesExecutableNullForSeleniumManager()
    {
        Neodymium.configuration().removeProperty("neodymium.webDriver.chrome.pathToDriverServer");
        final BrowserConfiguration config = new BrowserConfiguration();

        final ChromeBuilder builder = BrowserRunnerHelper.createChromeBuilder(config);

        Assertions.assertNull(builder.exe, "Driver executable should remain null when not explicitly configured");
    }

    @Test
    public void testCreateChromeBuilderWithConfiguredDriverPathSetsExecutable()
    {
        final String customPath = "/custom/path/to/chromedriver";
        Neodymium.configuration().setProperty("neodymium.webDriver.chrome.pathToDriverServer", customPath);
        final BrowserConfiguration config = new BrowserConfiguration();

        final ChromeBuilder builder = BrowserRunnerHelper.createChromeBuilder(config);

        Assertions.assertEquals(new File(customPath), builder.exe);
    }

    @Test
    public void testCreateGeckoBuilderWithoutConfiguredDriverPathLeavesExecutableNullForSeleniumManager()
    {
        Neodymium.configuration().removeProperty("neodymium.webDriver.firefox.pathToDriverServer");
        final BrowserConfiguration config = new BrowserConfiguration();

        final Builder builder = BrowserRunnerHelper.createGeckoBuilder(config);

        Assertions.assertNull(builder.exe, "GeckoDriver executable should remain null when not explicitly configured");
    }

    @Test
    public void testCreateGeckoBuilderWithConfiguredDriverPathSetsExecutable()
    {
        final String customPath = "/custom/path/to/geckodriver";
        Neodymium.configuration().setProperty("neodymium.webDriver.firefox.pathToDriverServer", customPath);
        final BrowserConfiguration config = new BrowserConfiguration();

        final Builder builder = BrowserRunnerHelper.createGeckoBuilder(config);

        Assertions.assertEquals(new File(customPath), builder.exe);
    }

    @Test
    public void testCreateEdgeBuilderWithoutConfiguredDriverPathLeavesExecutableNullForSeleniumManager()
    {
        Neodymium.configuration().removeProperty("neodymium.webDriver.edge.pathToDriverServer");
        final BrowserConfiguration config = new BrowserConfiguration();

        final EdgeBuilder builder = BrowserRunnerHelper.createEdgeBuilder(config);

        Assertions.assertNull(builder.exe, "EdgeDriver executable should remain null when not explicitly configured");
    }

    @Test
    public void testCreateEdgeBuilderWithConfiguredDriverPathSetsExecutable()
    {
        final String customPath = "/custom/path/to/msedgedriver";
        Neodymium.configuration().setProperty("neodymium.webDriver.edge.pathToDriverServer", customPath);
        final BrowserConfiguration config = new BrowserConfiguration();

        final EdgeBuilder builder = BrowserRunnerHelper.createEdgeBuilder(config);

        Assertions.assertEquals(new File(customPath), builder.exe);
    }

    @Test
    public void testCreateIEBuilderWithoutConfiguredDriverPathLeavesExecutableNullForSeleniumManager()
    {
        Neodymium.configuration().removeProperty("neodymium.webDriver.ie.pathToDriverServer");
        final BrowserConfiguration config = new BrowserConfiguration();

        final IEBuilder builder = BrowserRunnerHelper.createIEBuilder(config);

        Assertions.assertNull(builder.exe, "IEDriver executable should remain null when not explicitly configured");
    }

    @Test
    public void testCreateIEBuilderWithConfiguredDriverPathSetsExecutable()
    {
        final String customPath = "C:\\custom\\path\\to\\IEDriverServer.exe";
        Neodymium.configuration().setProperty("neodymium.webDriver.ie.pathToDriverServer", customPath);
        final BrowserConfiguration config = new BrowserConfiguration();

        final IEBuilder builder = BrowserRunnerHelper.createIEBuilder(config);

        Assertions.assertEquals(new File(customPath), builder.exe);
    }
}
