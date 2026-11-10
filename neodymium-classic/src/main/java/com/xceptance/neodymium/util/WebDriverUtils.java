package com.xceptance.neodymium.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.common.browser.BrowserRunner;
import com.xceptance.neodymium.common.browser.WebDriverCache;

import io.cucumber.java.Scenario;

public class WebDriverUtils
{
    private static ThreadLocal<BrowserRunner> browserHelper = ThreadLocal.withInitial(new Supplier<BrowserRunner>()
    {
        @Override
        public BrowserRunner get()
        {
            return new BrowserRunner();
        }
    });

    /**
     * @param browserProfileName
     *            the browser profile name that is configured in the browser.properties file
     * 
     *            <pre>
     *            public void setup(String browserProfileName)
     *            {
     *                WebDriverUtils.setUp(browserProfileName);
     *            }
     *            </pre>
     **/
    public static void setUp(final BrowserMethodData browserProfileName, String testName)
    {
        if (browserHelper.get().getBrowserTags().contains(browserProfileName.getBrowserTag()))
        {
            browserHelper.get().setUpTest(browserProfileName, testName);
        }
        else
        {
            throw new RuntimeException("Could not find any tag that matches a browser tag");
        }
    }

    /**
     * @param scenario
     *            Scenario is a Cucumber API class that can be gathered in hooks via dependency injection
     * 
     *            <pre>
     *            public void setup(Scenario scenario)
     *            {
     *                WebDriverUtils.setUpWithBrowserTag(scenario);
     *            }
     *            </pre>
     **/
    public static void setUpWithBrowserTag(Scenario scenario)
    {
        String browserProfileName = getFirstMatchingBrowserTag(scenario);
        browserHelper.get()
                     .setUpTest(new BrowserMethodData(browserProfileName, //
                                                      Neodymium.configuration().keepBrowserOpen(), //
                                                      Neodymium.configuration().keepBrowserOpenOnFailure(), false, false, new ArrayList<Method>()), //
                                scenario.getName());
    }

    /**
     * This function can be used within a function of a JUnit test case that is annotated with @After to prevent the
     * reuse of the current WebDriver
     * 
     * <pre>
     * &#64;After
     * public void after()
     * {
     *     if (someConditionIsFulfilled)
     *     {
     *         WebDriverUtils.preventReuseAndTearDown();
     *     }
     * }
     * </pre>
     **/
    public static void preventReuseAndTearDown()
    {
        browserHelper.get().teardown(false, true, new BrowserMethodData(Neodymium.getBrowserProfileName(), //
                                                                        Neodymium.configuration().keepBrowserOpen(), //
                                                                        Neodymium.configuration()
                                                                                 .keepBrowserOpenOnFailure(), false, false, new ArrayList<Method>()),
                                     Neodymium.getWebDriverStateContainer());
    }

    /**
     * @param scenario
     *            Scenario is a Cucumber API class that can be gathered in hooks via dependency injection
     * 
     *            <pre>
     *            &#64;cucumber.api.java.After(order = 100)
     *            public void tearDown(Scenario scenario)
     *            {
     *                WebDriverUtils.tearDown(scenario);
     *            }
     *            </pre>
     **/
    public static void tearDown(Scenario scenario)
    {
        tearDown(scenario.isFailed());
    }

    /**
     * @param isFailed
     *            boolean to pass if the test failed in order to use Neodymium's keepBrowserOpenOnFailure feature
     **/
    public static void tearDown(boolean isFailed)
    {
        browserHelper.get().teardown(isFailed);
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
     *     WebDriverUtils.quitReusableCachedBrowsers();
     * }
     * </pre>
     **/
    public static void quitReusableCachedBrowsers()
    {
        WebDriverCache.quitCachedBrowsers();
    }

    static String getFirstMatchingBrowserTag(Scenario scenario)
    {
        Collection<String> scenarioTags = scenario.getSourceTagNames();
        List<String> browserTags = browserHelper.get().getBrowserTags();

        String firstBrowserTagFound = "";
        for (String tag : scenarioTags)
        {
            String compareString = tag.substring(1);// substring to cut off leading '@'
            if (browserTags.contains(compareString))
            {
                if (StringUtils.isEmpty(firstBrowserTagFound))
                {
                    firstBrowserTagFound = compareString;
                }
                else
                {
                    throw new RuntimeException("Found more than one matching browser tag");
                }
            }
        }

        if (StringUtils.isEmpty(firstBrowserTagFound))
        {
            throw new RuntimeException("Could not find any tag that matches a browser tag");
        }
        else
        {
            return firstBrowserTagFound;
        }
    }
}
