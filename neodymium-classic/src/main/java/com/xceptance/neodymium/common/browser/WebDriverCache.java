package com.xceptance.neodymium.common.browser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.browserup.bup.BrowserUpProxy;

/**
 * A cache to hold different instances of {@link WebDriver}. Instances are kept in a synchronized {@link HashMap} which
 * are indexed by an "browserTag" {@link String}. That browserTag is a unique character sequence. All access to the
 * cache will be done in a synchronized way. The constructor adds an VM shutdown hook to clean up the cache and close
 * the cached {@link WebDriver} gracefully.
 * 
 * @author m.kaufmann
 */
public class WebDriverCache
{
    public static Logger LOGGER = LoggerFactory.getLogger(WebDriverCache.class);

    public static final WebDriverCache instance = new WebDriverCache();

    private final Map<String, WebDriverStateContainer> cache = Collections.synchronizedMap(new HashMap<>());

    /**
     * The private constructor of the {@link WebDriverCache}. Creates the synchronized {@link HashMap} instance and
     * add's a VM shutdown hook to clean up the cache and close the cached {@link WebDriver} gracefully if the
     * neodymium.webDriver.keepBrowserOpen property is set to <code>false</code> in property file "browser.properties".
     * See config folder
     */
    private WebDriverCache()
    {
        Runtime.getRuntime().addShutdownHook(new WebDriverCacheCleanupHook());
    }

    /**
     * Look's up the cache for a {@link WebDriver} that is referenced with the argument browserTag
     * 
     * @param browserTag
     *            a {@link String} that will be used find a referenced {@link WebDriver} instance in the cache
     * @return the {@link WebDriver} if one was found in the cache, else <code>null</code>
     */
    public WebDriver getWebDriverByBrowserTag(String browserTag)
    {
        WebDriverStateContainer container = cache.get(browserTag);
        return container != null ? container.getWebDriver() : null;
    }

    /**
     * Put's the instance of a {@link WebDriver} into the cache and uses browserTag to reference it. If there is already
     * an {@link WebDriver} stored in the cache with the same browserTag {@link String} then the instance will be
     * overwritten.
     * 
     * @param browserTag
     *            a {@link String} that will be used to reference the cached {@link WebDriver}
     * @param webDriverStateContainer
     *            an instance of {@link WebDriverStateContainer} that should be stored in the cache this contains the
     *            WebDriver and belonging objects and information like the proxy and the used count
     */
    public void putWebDriverStateContainer(String browserTag, WebDriverStateContainer webDriverStateContainer)
    {
        cache.put(browserTag, webDriverStateContainer);
    }

    /**
     * Look's up the cache for the browserTag argument and removes {@link WebDriver} instance from cache and returns a
     * boolean indicating whether it was found and removed or not
     * 
     * @param browserTag
     *            a {@link String} that will be used to find the referenced {@link WebDriver} in the cache
     * @return {@link Boolean} indicating whether it was found and removed or not
     */
    public boolean removeWebDriverStateContainer(String browserTag)
    {
        return (removeWebDriverStateContainerByBrowserTag(browserTag) != null);
    }

    /**
     * Look's up the cache for the browserTag argument and removes {@link WebDriverStateContainer} instance from cache
     * and returns the stored {@link WebDriverStateContainer} instance if found.
     * 
     * @param browserTag
     *            The String used in {@link Browser} to reference a browser configuration
     * @return {@link WebDriverStateContainer} if found, else <code>null</code>
     */
    public WebDriverStateContainer removeWebDriverStateContainerByBrowserTag(String browserTag)
    {
        return cache.remove(browserTag);
    }

    /**
     * Look's up the cache for the browserTag argument and retrieves {@link WebDriverStateContainer} instance from cache
     * and returns the stored {@link WebDriverStateContainer} instance if found.
     * 
     * @param browserTag
     *            the String used in {@link Browser} to reference a browser configuration
     * @return {@link WebDriverStateContainer} if found, else <code>null</code>
     */
    public WebDriverStateContainer getWebDriverStateContainerByBrowserTag(String browserTag)
    {
        return cache.get(browserTag);
    }

    /**
     * Looks up cache for argument {@link WebDriver} and removes it from cache.
     * 
     * @param driver
     *            an instance of {@link WebDriver}
     * @return {@link Boolean} which indicates if the {@link WebDriver} was found and removed from cache.
     */
    public boolean removeWebDriverStateContainerByWebDriver(WebDriver driver)
    {
        synchronized (cache)
        {
            boolean removed = false;
            for (Entry<String, WebDriverStateContainer> entry : cache.entrySet())
            {
                if (entry.getValue().getWebDriver() == driver)
                {
                    cache.remove(entry.getKey());
                    removed = true;
                }
            }
            return removed;
        }
    }

    /**
     * Retrieves the number of all cached {@link WebDriverStateContainer}
     * 
     * @return size
     */
    public int getWebDriverStateContainerCacheSize()
    {
        return cache.size();
    }

    /**
     * This function can be used within a function of a JUnit test case that is annotated with @AfterClass to clear the
     * WebDriverCache of the WebDrivers ready for reuse.
     * <p>
     * <b>Attention:</b> It is save to run this function during a sequential test execution. It can have repercussions
     * (e.g. longer test duration) in a parallel execution environment.
     *
     * <pre>
     * &#64;AfterClass
     * public void afterClass()
     * {
     *     WebDriverCache.quitCachedBrowsers();
     * }
     * </pre>
     **/
    public static void quitCachedBrowsers()
    {
        instance.cache.values().forEach((wDSC) -> {
            try
            {
                WebDriver webDriver = wDSC.getWebDriver();
                LOGGER.debug("Quit web driver: " + webDriver.toString());
                webDriver.quit();
                BrowserUpProxy proxy = wDSC.getProxy();
                if (proxy != null)
                {
                    proxy.stop();
                }
            }
            catch (Exception e)
            {
                LOGGER.debug("Error on quitting web driver", e);
            }
        });
        instance.cache.clear();
    }
}
