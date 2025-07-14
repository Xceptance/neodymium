package com.xceptance.neodymium.junit5.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.CapabilityType;

import com.xceptance.neodymium.common.Data;
import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit5.testclasses.browser.DisableRandomBrowserAnnotation;
import com.xceptance.neodymium.junit5.testclasses.browser.RandomBrowsersClassInitialisationException;
import com.xceptance.neodymium.junit5.testclasses.browser.RandomBrowsersMethodInitialisationException;
import com.xceptance.neodymium.junit5.testclasses.browser.classonly.ClassBrowserSuppressed;
import com.xceptance.neodymium.junit5.testclasses.browser.classonly.ClassBrowserSuppressedNoBrowserAnnotation;
import com.xceptance.neodymium.junit5.testclasses.browser.classonly.ClassBrowserSuppressedWithBefore;
import com.xceptance.neodymium.junit5.testclasses.browser.classonly.MethodBrowserSuppressedWithAfter;
import com.xceptance.neodymium.junit5.testclasses.browser.classonly.NewBrowserIsNotStartedForCleanUp;
import com.xceptance.neodymium.junit5.testclasses.browser.classonly.NewBrowserIsNotStartedForSetUp;
import com.xceptance.neodymium.junit5.testclasses.browser.classonly.OneClassBrowserOneMethod;
import com.xceptance.neodymium.junit5.testclasses.browser.classonly.RandomBrowserClassLevel;
import com.xceptance.neodymium.junit5.testclasses.browser.classonly.TwoClassBrowserOneMethod;
import com.xceptance.neodymium.junit5.testclasses.browser.classonly.TwoSameClassBrowserOneMethod;
import com.xceptance.neodymium.junit5.testclasses.browser.inheritance.BrowserOverwritingChild;
import com.xceptance.neodymium.junit5.testclasses.browser.inheritance.RandomBrowsersChild;
import com.xceptance.neodymium.junit5.testclasses.browser.inheritance.RandomBrowsersOverwritingChild;
import com.xceptance.neodymium.junit5.testclasses.browser.methodonly.MethodBrowserSuppressNoBrowserAnnotation;
import com.xceptance.neodymium.junit5.testclasses.browser.methodonly.OneBrowserOneMethodBrowserSuppressed;
import com.xceptance.neodymium.junit5.testclasses.browser.methodonly.RandomBrowserMethodLevel;
import com.xceptance.neodymium.junit5.testclasses.browser.methodonly.StartNewBrowserForOneOfTheAfters;
import com.xceptance.neodymium.junit5.testclasses.browser.methodonly.StartNewBrowserForOneOfTheBefores;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.ClassAndMethodSameBrowserOneMethod;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.ClassBrowserSuppressedAfterWithBrowser;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.ClassBrowserSuppressedBeforeWithBrowser;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.MethodBrowserAnnotationOverwritesClassRandomBrowser;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.NewBrowserIsNotStartedForOneOfCleanUps;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.NewBrowserIsNotStartedForOneOfSetUps;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.OverwriteBrowserForCleanUp;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.OverwriteBrowserForSetUp;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.RandomBrowserMixed;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.StartBrowserForCleanUp;
import com.xceptance.neodymium.junit5.testclasses.browser.mixed.StartBrowserForSetUp;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserStatementTest extends AbstractNeodymiumTest
{
    private static MultibrowserConfiguration browserConfig;

    @BeforeAll
    public static void beforeClass() throws IOException
    {
        Map<String, String> properties = new HashMap<>();

        properties.put("browserprofile.chrome.name", "Google Chrome");
        properties.put("browserprofile.chrome.browser", "chrome");
        properties.put("browserprofile.chrome.testEnvironment", "local");
        properties.put("browserprofile.chrome.acceptInsecureCertificates", "true");
        properties.put("browserprofile.chrome.arguments", "headless");

        properties.put("browserprofile.chrome.preferences",
                       "homepage=https://www.xceptance.com ; geolocation.enabled=true ; renderer.memory_cache.size=120000");
        properties.put("browserprofile.chrome.downloadDirectory", "target");

        properties.put("browserprofile.firefox.name", "Mozilla Firefox");
        properties.put("browserprofile.firefox.browser", "firefox");
        properties.put("browserprofile.firefox.arguments", "headless");

        properties.put("browserprofile.firefox.preferences",
                       "media.navigator.permission.disabled=true ; browser.startup.homepage=https://www.xceptance.com ; app.update.backgroundMaxErrors=1");

        properties.put("browserprofile.firefox.downloadDirectory", "target");

        properties.put("browserprofile.multiFirefox.name", "Multi Argument Firefox");
        properties.put("browserprofile.multiFirefox.browser", "firefox");
        properties.put("browserprofile.multiFirefox.arguments", "-headless ; -width=1024; -height=768 ");

        properties.put("browserprofile.multiChrome.name", "Multi Argument Chrome");
        properties.put("browserprofile.multiChrome.browser", "chrome");
        properties.put("browserprofile.multiChrome.arguments", " -crash-test ; -window-position=0,0 ;-window-size=1024,768 ");

        properties.put("browserprofile.testEnvironmentFlags.name", "Test Environment Browser");
        properties.put("browserprofile.testEnvironmentFlags.browser", "chrome");
        properties.put("browserprofile.testEnvironmentFlags.idleTimeout", "1234");
        properties.put("browserprofile.testEnvironmentFlags.maxDuration", "5678");
        properties.put("browserprofile.testEnvironmentFlags.seleniumVersion", "3.1234");
        properties.put("browserprofile.testEnvironmentFlags.screenResolution", "800x600");
        properties.put("browserprofile.testEnvironmentFlags.platform", "Windows 10");
        properties.put("browserprofile.testEnvironmentFlags.deviceName", "MyDevice");
        properties.put("browserprofile.testEnvironmentFlags.deviceOrientation", "landscape");

        properties.put("browserprofile.testEnvironmentFlags2.name", "Test Environment Browser");
        properties.put("browserprofile.testEnvironmentFlags2.browser", "chrome");
        properties.put("browserprofile.testEnvironmentFlags2.idleTimeout", "1234");
        properties.put("browserprofile.testEnvironmentFlags2.maxDuration", "5678");
        properties.put("browserprofile.testEnvironmentFlags2.seleniumVersion", "3.1234");
        properties.put("browserprofile.testEnvironmentFlags2.screenResolution", "800x600");
        properties.put("browserprofile.testEnvironmentFlags2.platform", "Windows 10");
        properties.put("browserprofile.testEnvironmentFlags2.deviceName", "MyDevice");
        properties.put("browserprofile.testEnvironmentFlags2.deviceOrientation", "portrait");

        File tempConfigFile = File.createTempFile("browser", "", new File("./config/"));
        tempFiles.add(tempConfigFile);
        writeMapToPropertiesFile(properties, tempConfigFile);

        MultibrowserConfiguration.clearAllInstances();
        browserConfig = MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @BeforeEach
    public void setJUnitViewModeFlat()
    {
        Neodymium.configuration().setProperty("neodymium.junit.viewmode", "flat");
    }

    @Test
    public void testRandomBrowserClassLevel()
    {
        // an empty browser tag (@Browser({""})) should raise an error
        NeodymiumTestExecutionSummary summary = run(RandomBrowserClassLevel.class);
        checkPass(summary, 2, 0);
    }

    @Test
    public void testRandomBrowserMethodLevel()
    {
        // an empty browser tag (@Browser({""})) should raise an error
        NeodymiumTestExecutionSummary summary = run(RandomBrowserMethodLevel.class);
        checkPass(summary, 2, 0);
    }

    @Test
    public void testMethodBrowserAnnotationOverwritesClassRandomBrowser()
    {
        NeodymiumTestExecutionSummary summary = run(MethodBrowserAnnotationOverwritesClassRandomBrowser.class);
        checkPass(summary, 3, 0);
    }

    @Test
    public void testRandomBrowserMixed()
    {
        NeodymiumTestExecutionSummary summary = run(RandomBrowserMixed.class);
        checkPass(summary, 2, 0);
    }

    @Test
    public void testRandomBrowsersInheritance()
    {
        // the test from RandomBrowserChild should be run 2 times, as the corresponding annotations should be inherited
        // from the RandomBrowserParent class
        NeodymiumTestExecutionSummary summary = run(RandomBrowsersChild.class);
        checkPass(summary, 2, 0);
    }

    @Test
    public void testRandomBrowsersOverwriting()
    {
        // the test from RandomBrowsersOverwritingChild should be run 3 times, as the corresponding annotations from
        // RandomBrowserParent class should be overwritten
        NeodymiumTestExecutionSummary summary = run(RandomBrowsersOverwritingChild.class);
        checkPass(summary, 3, 0);
    }

    @Test
    public void testFindBrowserRelatedClassAnnotationMethod()
    {
        List<RandomBrowsers> foundAnnotations = Data.getAnnotations(RandomBrowsersOverwritingChild.class,
                                                                    RandomBrowsers.class);

        Assertions.assertTrue(foundAnnotations.size() == 1, "There should be only one annotaion found");
        Assertions.assertEquals(3, foundAnnotations.get(0).value(), "BrowserStatement.findBrowserRelatedClassAnnotation works not as expected");
    }

    @Test
    public void testRandomBrowsersMethodInitialisationException()
    {
        // test method, marked to be run with more random browsers, that it's annotated with @Browser annotations,
        // should throw exception with the corresponding error message
        NeodymiumTestExecutionSummary summary = run(RandomBrowsersMethodInitialisationException.class);
        checkFail(summary, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with 9 random browsers, but there are only 4 available");
    }

    @Test
    public void testRandomBrowsersClassInitialisationException()
    {
        // test class, marked to be run with more random browsers, that it's annotated with @Browser annotations,
        // should throw exception with the corresponding error message
        NeodymiumTestExecutionSummary summary = run(RandomBrowsersClassInitialisationException.class);
        checkFail(summary, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with 9 random browsers, but there are only 4 available");
    }

    @Test
    public void testStartBrowserForSetUp()
    {
        // by default, new browser is started for each @Before
        NeodymiumTestExecutionSummary summary = run(StartBrowserForSetUp.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testNewBrowserIsNotStartedForOneOfSetUps()
    {
        // by default, new browser is started for each @After
        NeodymiumTestExecutionSummary summary = run(NewBrowserIsNotStartedForOneOfSetUps.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testNewBrowserIsNotStartedForSetUp()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.startNewBrowserForSetUp", "false");

        addPropertiesForTest("temp-testNewBrowserIsNotStartedForSetUp-neodymium.properties", properties);
        // if test class is annotated with @@StartNewBrowserForCleanUp(false), no new browser is started for cleanup
        NeodymiumTestExecutionSummary summary = run(NewBrowserIsNotStartedForSetUp.class);
        checkPass(summary, 1, 0);
        ConfigFactory.clearProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME);
    }

    @Test
    public void testClassBrowserSuppressedWithBefore()
    {
        // if test class, marked to run without browser but it's not marked that no new browser should be started for
        // @After method, Runtime Exception should be thrown
        NeodymiumTestExecutionSummary summary = run(ClassBrowserSuppressedWithBefore.class);
        checkFail(summary, 1, 0, 1,
                  "java.lang.RuntimeException: No browser setting for @BeforeEach method 'before' was found."
                                    + " If browser is suppressed for the test"
                                    + " but the before method is annotated with @StartNewBrowserForSetUp"
                                    + " because it requires a browser, please,"
                                    + " use @Browser annotation to specify what browser is required for this method.");
    }

    @Test
    public void testDontStartNewBrowserForOneOfTheBefores()
    {
        // if test class, marked to run without browser but it's not marked that no new browser should be started for
        // @After method, Runtime Exception should be thrown
        NeodymiumTestExecutionSummary summary = run(StartNewBrowserForOneOfTheBefores.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testClassBrowserSuppressedBeforeWithBrowser()
    {
        // although test class is marked to be run without browser, if @After method is annotated with @Browser, the
        // browser should be started for clean up
        NeodymiumTestExecutionSummary summary = run(ClassBrowserSuppressedBeforeWithBrowser.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testOverwriteBrowserForSetUp()
    {
        // it should be possible to use different browser profle for clean up (using @Browser annotation)
        NeodymiumTestExecutionSummary summary = run(OverwriteBrowserForSetUp.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testStartBrowserForCleanUp()
    {
        // by default, new browser is started for each @After
        NeodymiumTestExecutionSummary summary = run(StartBrowserForCleanUp.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testNewBrowserIsNotStartedForOneOfCleanUps()
    {
        // by default, new browser is started for each @After
        NeodymiumTestExecutionSummary summary = run(NewBrowserIsNotStartedForOneOfCleanUps.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testNewBrowserIsNotStartedForCleanUp()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.startNewBrowserForCleanUp", "false");

        addPropertiesForTest("temp-NewBrowserIsNotStartedForCleanUp-neodymium.properties", properties);
        // if test class is annotated with @@StartNewBrowserForCleanUp(false), no new browser is started for cleanup
        NeodymiumTestExecutionSummary summary = run(NewBrowserIsNotStartedForCleanUp.class);
        checkPass(summary, 1, 0);
        ConfigFactory.clearProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME);
    }

    @Test
    public void testSupressBrowserWithAfter()
    {
        // if test class, marked to run without browser but it's not marked that no new browser should be started for
        // @After method, Runtime Exception should be thrown
        NeodymiumTestExecutionSummary summary = run(MethodBrowserSuppressedWithAfter.class);
        checkFail(summary, 1, 0, 1, "java.lang.RuntimeException: No browser setting for @AfterEach method 'after' was found. "
                                    + "If browser was suppressed for the test but is annotated with @StartNewBrowserForCleanUp because browser isrequired for the clean up,"
                                    + " please, use @Browser annotation to specify what browser is required for this clean up.");
    }

    @Test
    public void testDontStartNewBrowserForOneOfTheAfters()
    {
        // if test class, marked to run without browser but it's not marked that no new browser should be started for
        // @After method, Runtime Exception should be thrown
        NeodymiumTestExecutionSummary summary = run(StartNewBrowserForOneOfTheAfters.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testClassBrowserSuppressedAfterWithBrowser()
    {
        // although test class is marked to be run without browser, if @After method is annotated with @Browser, the
        // browser should be started for clean up
        NeodymiumTestExecutionSummary summary = run(ClassBrowserSuppressedAfterWithBrowser.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testOverwriteBrowserForCleanUp()
    {
        // it should be possible to use different browser profle for clean up (using @Browser annotation)
        NeodymiumTestExecutionSummary summary = run(OverwriteBrowserForCleanUp.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testTestBrowser() throws Throwable
    {
        // one test method and one browser annotated on class
        String[] expected = new String[]
        {
          "first :: Browser chrome"
        };
        checkDescription(OneClassBrowserOneMethod.class, expected);
    }

    @Test
    public void testClassBrowserSuppressed() throws Throwable
    {
        //
        String[] expected = new String[]
        {
          "first"
        };
        checkDescription(ClassBrowserSuppressed.class, expected);
    }

    @Test
    public void testDisableRandomBrowserAnnotation() throws Throwable
    {
        //
        String[] expected = new String[]
        {
          "test1 :: Browser Chrome_1024x768",
          "test1 :: Browser Chrome_1500x1000"
        };
        checkDescription(DisableRandomBrowserAnnotation.class, expected);
    }

    @Test
    public void testTwoClassBrowserOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Browser chrome", "first :: Browser firefox"
        };
        checkDescription(TwoClassBrowserOneMethod.class, expected);
    }

    @Test
    public void testTwoSameClassBrowserOneMethod() throws Throwable
    {
        // two browser annotated on class, both have same value
        String[] expected = new String[]
        {
          "first :: Browser chrome"
        };
        checkDescription(TwoSameClassBrowserOneMethod.class, expected);
    }

    @Test
    public void testClassAndMethodSameBrowserOneMethod() throws Throwable
    {
        // same browser annotated on class and method
        String[] expected = new String[]
        {
          "first :: Browser chrome"
        };
        checkDescription(ClassAndMethodSameBrowserOneMethod.class, expected);
    }

    @Test
    public void testClassBrowserSuppressedNoBrowserAnnotation() throws Throwable
    {
        // no browser definition but browser suppressed on class
        String[] expected = new String[]
        {
          "first"
        };
        checkDescription(ClassBrowserSuppressedNoBrowserAnnotation.class, expected);
    }

    @Test
    public void testName() throws Throwable
    {
        // no browser definition but browser suppressed on method
        String[] expected = new String[]
        {
          "first"
        };
        checkDescription(MethodBrowserSuppressNoBrowserAnnotation.class, expected);
    }

    @Test
    public void testOneBrowserOneMethodBrowserSuppressed() throws Throwable
    {
        // a browser definition on a method and a suppress browser
        String[] expected = new String[]
        {
          "first",
          "second()"
        };
        checkDescription(OneBrowserOneMethodBrowserSuppressed.class, expected);
    }

    @Test
    public void testMultibrowserConfiguration() throws Throwable
    {
        // define one chrome browser then validate the parsed multi-browser configuration
        // TODO: we need more of this tests...
        Map<String, BrowserConfiguration> browserProfiles = browserConfig.getBrowserProfiles();
        // Assertions.assertEquals(1, browserProfiles.size());

        checkChrome(browserProfiles.get("chrome"));
        checkMultiChrome(browserProfiles.get("multiChrome"));
        checkFirefox(browserProfiles.get("firefox"));
        checkMultiFirefox(browserProfiles.get("multiFirefox"));
        checkTestEnvironment(browserProfiles.get("testEnvironmentFlags"));
        checkTestEnvironment2(browserProfiles.get("testEnvironmentFlags2"));
    }

    @Test
    public void testBrowserOverwrittingInheritance() throws Throwable
    {
        String[] expected = new String[]
        {
          "test :: Browser Chrome_1024x768",
          "testParent :: Browser Chrome_1024x768"
        };
        checkDescription(BrowserOverwritingChild.class, expected);
        NeodymiumTestExecutionSummary summary = run(BrowserOverwritingChild.class);
        checkPass(summary, 2, 0);
    }

    private void checkChrome(BrowserConfiguration config)
    {
        Assertions.assertNotNull(config);
        Assertions.assertEquals("chrome", config.getConfigTag());
        Assertions.assertEquals("Google Chrome", config.getName());
        Assertions.assertEquals("local", config.getTestEnvironment());
        MutableCapabilities testCapabilities = config.getCapabilities();
        Assertions.assertEquals("chrome", testCapabilities.getBrowserName());
        Assertions.assertEquals(true, testCapabilities.getCapability(CapabilityType.ACCEPT_INSECURE_CERTS));
        LinkedList<String> list = new LinkedList<>();
        list.add("headless");
        Assertions.assertEquals(list, config.getArguments());

        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("geolocation.enabled", true);
        prefs.put("renderer.memory_cache.size", 120000);
        prefs.put("homepage", "https://www.xceptance.com");
        Assertions.assertEquals(prefs, config.getPreferences());

        Assertions.assertEquals(new File("target").getAbsolutePath(), config.getDownloadDirectory());
    }

    private void checkMultiChrome(BrowserConfiguration config)
    {
        Assertions.assertNotNull(config);
        Assertions.assertEquals("multiChrome", config.getConfigTag());
        Assertions.assertEquals("Multi Argument Chrome", config.getName());
        MutableCapabilities testCapabilities = config.getCapabilities();
        Assertions.assertEquals("chrome", testCapabilities.getBrowserName());
        LinkedList<String> list = new LinkedList<>();
        list.add("-crash-test");
        list.add("-window-position=0,0");
        list.add("-window-size=1024,768");
        Assertions.assertEquals(list, config.getArguments());
    }

    private void checkFirefox(BrowserConfiguration config)
    {
        Assertions.assertNotNull(config);
        Assertions.assertEquals("firefox", config.getConfigTag());
        Assertions.assertEquals("Mozilla Firefox", config.getName());
        Assertions.assertEquals(null, config.getTestEnvironment());
        MutableCapabilities testCapabilities = config.getCapabilities();
        Assertions.assertEquals("firefox", testCapabilities.getBrowserName());
        LinkedList<String> list = new LinkedList<>();
        list.add("headless");
        Assertions.assertEquals(list, config.getArguments());

        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("media.navigator.permission.disabled", true);
        prefs.put("app.update.backgroundMaxErrors", 1);
        prefs.put("browser.startup.homepage", "https://www.xceptance.com");
        Assertions.assertEquals(prefs, config.getPreferences());
        Assertions.assertEquals(new File("target").getAbsolutePath(), config.getDownloadDirectory());
    }

    private void checkMultiFirefox(BrowserConfiguration config)
    {
        Assertions.assertNotNull(config);
        Assertions.assertEquals("multiFirefox", config.getConfigTag());
        Assertions.assertEquals("Multi Argument Firefox", config.getName());
        MutableCapabilities testCapabilities = config.getCapabilities();
        Assertions.assertEquals("firefox", testCapabilities.getBrowserName());
        LinkedList<String> list = new LinkedList<>();
        list.add("-headless");
        list.add("-width=1024");
        list.add("-height=768");
        Assertions.assertEquals(list, config.getArguments());
    }

    private void checkTestEnvironment(BrowserConfiguration config)
    {
        Assertions.assertNotNull(config);
        Assertions.assertEquals("testEnvironmentFlags", config.getConfigTag());
        Assertions.assertEquals("Test Environment Browser", config.getName());
        MutableCapabilities testCapabilities = config.getCapabilities();
        HashMap<String, Object> testGridProperties = config.getGridProperties();
        Assertions.assertEquals("chrome", testCapabilities.getBrowserName());
        Assertions.assertEquals(1234, testGridProperties.get("idleTimeout"));
        Assertions.assertEquals(1234, testGridProperties.get("idletimeout"));
        Assertions.assertEquals(5678, testGridProperties.get("maxDuration"));
        Assertions.assertEquals(5678, testGridProperties.get("maxduration"));
        Assertions.assertEquals("3.1234", testGridProperties.get("seleniumVersion"));
        Assertions.assertEquals("3.1234", testGridProperties.get("selenium-version"));
        Assertions.assertEquals("800x600", testGridProperties.get("screenResolution"));
        Assertions.assertEquals("800x600", testGridProperties.get("screen-resolution"));
        Assertions.assertEquals("800x600", testGridProperties.get("resolution"));
        Assertions.assertEquals("Windows 10", testGridProperties.get("os"));
        Assertions.assertEquals("MyDevice", testGridProperties.get("deviceName"));
        Assertions.assertEquals("landscape", testGridProperties.get("deviceOrientation"));
        Assertions.assertEquals("landscape", testGridProperties.get("orientation"));
    }

    private void checkTestEnvironment2(BrowserConfiguration config)
    {
        Assertions.assertNotNull(config);
        Assertions.assertEquals("testEnvironmentFlags2", config.getConfigTag());
        Assertions.assertEquals("Test Environment Browser", config.getName());
        MutableCapabilities testCapabilities = config.getCapabilities();
        HashMap<String, Object> testGridProperties = config.getGridProperties();
        Assertions.assertEquals("chrome", testCapabilities.getBrowserName());
        Assertions.assertEquals(1234, testGridProperties.get("idleTimeout"));
        Assertions.assertEquals(1234, testGridProperties.get("idletimeout"));
        Assertions.assertEquals(5678, testGridProperties.get("maxDuration"));
        Assertions.assertEquals(5678, testGridProperties.get("maxduration"));
        Assertions.assertEquals("3.1234", testGridProperties.get("seleniumVersion"));
        Assertions.assertEquals("3.1234", testGridProperties.get("selenium-version"));
        Assertions.assertEquals("800x600", testGridProperties.get("screenResolution"));
        Assertions.assertEquals("800x600", testGridProperties.get("screen-resolution"));
        Assertions.assertEquals("800x600", testGridProperties.get("resolution"));
        Assertions.assertEquals("Windows 10", testGridProperties.get("os"));
        Assertions.assertEquals("MyDevice", testGridProperties.get("deviceName"));
        Assertions.assertEquals("portrait", testGridProperties.get("orientation"));
    }
}
