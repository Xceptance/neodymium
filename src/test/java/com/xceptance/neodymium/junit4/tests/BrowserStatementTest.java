package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.Result;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.CapabilityType;

import com.xceptance.neodymium.common.Data;
import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit4.testclasses.browser.DisableRandomBrowserAnnotation;
import com.xceptance.neodymium.junit4.testclasses.browser.RandomBrowsersClassInitialisationException;
import com.xceptance.neodymium.junit4.testclasses.browser.RandomBrowsersMethodInitialisationException;
import com.xceptance.neodymium.junit4.testclasses.browser.SystemPropertyBrowserFilter;
import com.xceptance.neodymium.junit4.testclasses.browser.classonly.ClassBrowserSuppressed;
import com.xceptance.neodymium.junit4.testclasses.browser.classonly.ClassBrowserSuppressedNoBrowserAnnotation;
import com.xceptance.neodymium.junit4.testclasses.browser.classonly.ClassBrowserSuppressedWithBefore;
import com.xceptance.neodymium.junit4.testclasses.browser.classonly.MethodBrowserSuppressedWithAfter;
import com.xceptance.neodymium.junit4.testclasses.browser.classonly.NewBrowserIsNotStartedForCleanUp;
import com.xceptance.neodymium.junit4.testclasses.browser.classonly.NewBrowserIsNotStartedForSetUp;
import com.xceptance.neodymium.junit4.testclasses.browser.classonly.OneClassBrowserOneMethod;
import com.xceptance.neodymium.junit4.testclasses.browser.classonly.RandomBrowserClassLevel;
import com.xceptance.neodymium.junit4.testclasses.browser.classonly.TwoClassBrowserOneMethod;
import com.xceptance.neodymium.junit4.testclasses.browser.classonly.TwoSameClassBrowserOneMethod;
import com.xceptance.neodymium.junit4.testclasses.browser.inheritance.BrowserOverwrittingChild;
import com.xceptance.neodymium.junit4.testclasses.browser.inheritance.RandomBrowsersChild;
import com.xceptance.neodymium.junit4.testclasses.browser.inheritance.RandomBrowsersOverwritingChild;
import com.xceptance.neodymium.junit4.testclasses.browser.methodonly.MethodBrowserSuppressNoBrowserAnnotation;
import com.xceptance.neodymium.junit4.testclasses.browser.methodonly.OneBrowserOneMethodBrowserSuppressed;
import com.xceptance.neodymium.junit4.testclasses.browser.methodonly.RandomBrowserMethodLevel;
import com.xceptance.neodymium.junit4.testclasses.browser.methodonly.StartNewBrowserForOneOfTheAfters;
import com.xceptance.neodymium.junit4.testclasses.browser.methodonly.StartNewBrowserForOneOfTheBefores;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.ClassAndMethodSameBrowserOneMethod;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.ClassBrowserSuppressedAfterWithBrowser;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.ClassBrowserSuppressedBeforeWithBrowser;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.MethodBrowserAnnotationOverwritesClassRandomBrowser;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.NewBrowserIsNotStartedForOneOfCleanUps;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.NewBrowserIsNotStartedForOneOfSetUps;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.OverwriteBrowserForCleanUp;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.OverwriteBrowserForSetUp;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.RandomBrowserMixed;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.StartBrowserForCleanUp;
import com.xceptance.neodymium.junit4.testclasses.browser.mixed.StartBrowserForSetUp;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserStatementTest extends NeodymiumTest
{
    private static MultibrowserConfiguration browserConfig;

    @BeforeClass
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

    @Before
    public void setJUnitViewModeFlat()
    {
        Neodymium.configuration().setProperty("neodymium.junit.viewmode", "flat");
    }

    @Test
    public void testRandomBrowserClassLevel()
    {
        // an empty browser tag (@Browser({""})) should raise an error
        Result result = run(RandomBrowserClassLevel.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testRandomBrowserMethodLevel()
    {
        // an empty browser tag (@Browser({""})) should raise an error
        Result result = run(RandomBrowserMethodLevel.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testMethodBrowserAnnotationOverwritesClassRandomBrowser()
    {
        Result result = run(MethodBrowserAnnotationOverwritesClassRandomBrowser.class);
        checkPass(result, 3, 0);
    }

    @Test
    public void testRandomBrowserMixed()
    {
        Result result = run(RandomBrowserMixed.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testRandomBrowsersInheritance()
    {
        // the test from RandomBrowserChild should be run 2 times, as the corresponding annotations should be inherited
        // from the RandomBrowserParent class
        Result result = run(RandomBrowsersChild.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testRandomBrowsersOverwriting()
    {
        // the test from RandomBrowsersOverwritingChild should be run 3 times, as the corresponding annotations from
        // RandomBrowserParent class should be overwritten
        Result result = run(RandomBrowsersOverwritingChild.class);
        checkPass(result, 3, 0);
    }

    @Test
    public void testFindBrowserRelatedClassAnnotationMethod()
    {
        List<RandomBrowsers> foundAnnotations = Data.getAnnotations(RandomBrowsersOverwritingChild.class,
                                                                    RandomBrowsers.class);

        Assert.assertTrue("There should be only one annotaion found", foundAnnotations.size() == 1);
        Assert.assertEquals("BrowserStatement.findBrowserRelatedClassAnnotation works not as expected", 3, foundAnnotations.get(0).value());
    }

    @Test
    public void testRandomBrowsersMethodInitialisationException()
    {
        // test method, marked to be run with more random browsers, that it's annotated with @Browser annotations,
        // should throw exception with the corresponding error message
        Result result = run(RandomBrowsersMethodInitialisationException.class);
        checkFail(result, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with 9 random browsers, but there are only 4 available");
    }

    @Test
    public void testRandomBrowsersClassInitialisationException()
    {
        // test class, marked to be run with more random browsers, that it's annotated with @Browser annotations,
        // should throw exception with the corresponding error message
        Result result = run(RandomBrowsersClassInitialisationException.class);
        checkFail(result, 1, 0, 1,
                  "java.lang.IllegalArgumentException: Method 'test1' is marked to be run with 9 random browsers, but there are only 4 available");
    }

    @Test
    public void testStartBrowserForSetUp()
    {
        // by default, new browser is started for each @Before
        Result result = run(StartBrowserForSetUp.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testNewBrowserIsNotStartedForOneOfSetUps()
    {
        // by default, new browser is started for each @After
        Result result = run(NewBrowserIsNotStartedForOneOfSetUps.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testNewBrowserIsNotStartedForSetUp()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.startNewBrowserForSetUp", "false");

        addPropertiesForTest("temp-testNewBrowserIsNotStartedForSetUp-neodymium.properties", properties);
        // if test class is annotated with @@StartNewBrowserForCleanUp(false), no new browser is started for cleanup
        Result result = run(NewBrowserIsNotStartedForSetUp.class);
        checkPass(result, 1, 0);
        ConfigFactory.clearProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME);
    }

    @Test
    public void testClassBrowserSuppressedWithBefore()
    {
        // if test class, marked to run without browser but it's not marked that no new browser should be started for
        // @After method, Runtime Exception should be thrown
        Result result = run(ClassBrowserSuppressedWithBefore.class);
        checkFail(result, 1, 0, 1,
                  "No browser setting for @Before method 'before' was found."
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
        Result result = run(StartNewBrowserForOneOfTheBefores.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testClassBrowserSuppressedBeforeWithBrowser()
    {
        // although test class is marked to be run without browser, if @After method is annotated with @Browser, the
        // browser should be started for clean up
        Result result = run(ClassBrowserSuppressedBeforeWithBrowser.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testOverwriteBrowserForSetUp()
    {
        // it should be possible to use different browser profle for clean up (using @Browser annotation)
        Result result = run(OverwriteBrowserForSetUp.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testStartBrowserForCleanUp()
    {
        // by default, new browser is started for each @After
        Result result = run(StartBrowserForCleanUp.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testNewBrowserIsNotStartedForOneOfCleanUps()
    {
        // by default, new browser is started for each @After
        Result result = run(NewBrowserIsNotStartedForOneOfCleanUps.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testNewBrowserIsNotStartedForCleanUp()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.startNewBrowserForCleanUp", "false");
        addPropertiesForTest("temp-NewBrowserIsNotStartedForCleanUp-neodymium.properties", properties);
        // if test class is annotated with @@StartNewBrowserForCleanUp(false), no new browser is started for cleanup
        Result result = run(NewBrowserIsNotStartedForCleanUp.class);
        checkPass(result, 1, 0);
        ConfigFactory.clearProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME);
    }

    @Test
    public void testSupressBrowserWithAfter()
    {
        // if test class, marked to run without browser but it's not marked that no new browser should be started for
        // @After method, Runtime Exception should be thrown
        Result result = run(MethodBrowserSuppressedWithAfter.class);
        checkFail(result, 1, 0, 1, "No browser setting for @After method 'after' was found. "
                                   + "If browser was suppressed for the test but is annotated with @StartNewBrowserForCleanUp because browser isrequired for the clean up,"
                                   + " please, use @Browser annotation to specify what browser is required for this clean up.");
    }

    @Test
    public void testDontStartNewBrowserForOneOfTheAfters()
    {
        // if test class, marked to run without browser but it's not marked that no new browser should be started for
        // @After method, Runtime Exception should be thrown
        Result result = run(StartNewBrowserForOneOfTheAfters.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testClassBrowserSuppressedAfterWithBrowser()
    {
        // although test class is marked to be run without browser, if @After method is annotated with @Browser, the
        // browser should be started for clean up
        Result result = run(ClassBrowserSuppressedAfterWithBrowser.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testOverwriteBrowserForCleanUp()
    {
        // it should be possible to use different browser profle for clean up (using @Browser annotation)
        Result result = run(OverwriteBrowserForCleanUp.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testSystemPropertyBrowserFilter() throws Throwable
    {
        System.setProperty("browserdefinition", "Chrome_headless");
        Result result = run(SystemPropertyBrowserFilter.class);
        System.setProperty("browserdefinition", "");
        checkPass(result, 1, 0);
    }

    @Test
    public void testSystemPropertyBrowserFilterWithNoResults() throws Throwable
    {
        System.setProperty("browserdefinition", "Chrome_notExisting");
        Result result = run(SystemPropertyBrowserFilter.class);
        System.setProperty("browserdefinition", "");
        checkPass(result, 0, 0);
    }

    @Test
    public void testNeodymiumPropertyBrowserFilter()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.browserFilter", "Chrome_headless");

        addPropertiesForTest("temp-NeodymiumPropertyBrowserFilter-neodymium.properties", properties);
        // if test class is annotated with @@StartNewBrowserForCleanUp(false), no new browser is started for cleanup
        Result result = run(SystemPropertyBrowserFilter.class);
        ConfigFactory.clearProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME);
        checkPass(result, 1, 0);
    }

    @Test
    public void testNeodymiumPropertyBrowserFilterNoResults()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.browserFilter", "Chrome_notExisting");

        addPropertiesForTest("temp-NeodymiumPropertyBrowserFilterNoResults-neodymium.properties", properties);
        // if test class is annotated with @@StartNewBrowserForCleanUp(false), no new browser is started for cleanup
        Result result = run(SystemPropertyBrowserFilter.class);
        ConfigFactory.clearProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME);
        checkPass(result, 0, 0);
    }

    @Test
    public void testTestBrowser() throws Throwable
    {
        // one test method and one browser annotated on class
        String[] expected = new String[]
        {
          "first :: Browser chrome :: "
        };
        checkDescription(OneClassBrowserOneMethod.class, expected);
    }

    @Test
    public void testClassBrowserSuppressed() throws Throwable
    {
        //
        String[] expected = new String[]
        {
          "first :: "
        };
        checkDescription(ClassBrowserSuppressed.class, expected);
    }

    @Test
    public void testDisableRandomBrowserAnnotation() throws Throwable
    {
        //
        String[] expected = new String[]
        {
          "test1 :: Browser Chrome_1024x768 :: ",
          "test1 :: Browser Chrome_1500x1000 :: "
        };
        checkDescription(DisableRandomBrowserAnnotation.class, expected);
    }

    @Test
    public void testTwoClassBrowserOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Browser chrome :: ", "first :: Browser firefox :: "
        };
        checkDescription(TwoClassBrowserOneMethod.class, expected);
    }

    @Test
    public void testTwoSameClassBrowserOneMethod() throws Throwable
    {
        // two browser annotated on class, both have same value
        String[] expected = new String[]
        {
          "first :: Browser chrome :: "
        };
        checkDescription(TwoSameClassBrowserOneMethod.class, expected);
    }

    @Test
    public void testClassAndMethodSameBrowserOneMethod() throws Throwable
    {
        // same browser annotated on class and method
        String[] expected = new String[]
        {
          "first :: Browser chrome :: "
        };
        checkDescription(ClassAndMethodSameBrowserOneMethod.class, expected);
    }

    @Test
    public void testClassBrowserSuppressedNoBrowserAnnotation() throws Throwable
    {
        // no browser definition but browser suppressed on class
        String[] expected = new String[]
        {
          "first :: "
        };
        checkDescription(ClassBrowserSuppressedNoBrowserAnnotation.class, expected);
    }

    @Test
    public void testName() throws Throwable
    {
        // no browser definition but browser suppressed on method
        String[] expected = new String[]
        {
          "first :: "
        };
        checkDescription(MethodBrowserSuppressNoBrowserAnnotation.class, expected);
    }

    @Test
    public void testOneBrowserOneMethodBrowserSuppressed() throws Throwable
    {
        // a browser definition on a method and a suppress browser
        String[] expected = new String[]
        {
          "first :: ",
          "second :: "
        };
        checkDescription(OneBrowserOneMethodBrowserSuppressed.class, expected);
    }

    @Test
    public void testAnnotationsCorrect() throws Throwable
    {
        Map<String, List<String>> expectedAnnotations = new HashMap<String, List<String>>();
        List<String> anno1 = new ArrayList<String>();
        anno1.add("@org.junit.Test");
        anno1.add("@com.xceptance.neodymium.common.browser.Browser.*chrome");
        anno1.add("@com.xceptance.neodymium.common.browser.SuppressBrowsers()");
        expectedAnnotations.put("first :: ", anno1);

        List<String> anno2 = new ArrayList<String>();
        anno2.add("@org.junit.Test");
        anno2.add("@org.junit.Ignore.*This should be visible");
        expectedAnnotations.put("second :: ", anno2);
        checkAnnotations(OneBrowserOneMethodBrowserSuppressed.class, expectedAnnotations);
    }

    @Test
    public void testMultibrowserConfiguration() throws Throwable
    {
        // define one chrome browser then validate the parsed multi-browser configuration
        // TODO: we need more of this tests...
        Map<String, BrowserConfiguration> browserProfiles = browserConfig.getBrowserProfiles();
        // Assert.assertEquals(1, browserProfiles.size());

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
          "test :: Browser Chrome_1024x768 :: ",
          "testParent :: Browser Chrome_1024x768 :: "
        };
        checkDescription(BrowserOverwrittingChild.class, expected);
        Result result = run(BrowserOverwrittingChild.class);
        checkPass(result, 2, 0);
    }

    private void checkChrome(BrowserConfiguration config)
    {
        Assert.assertNotNull(config);
        Assert.assertEquals("chrome", config.getConfigTag());
        Assert.assertEquals("Google Chrome", config.getName());
        Assert.assertEquals("local", config.getTestEnvironment());
        MutableCapabilities testCapabilities = config.getCapabilities();
        Assert.assertEquals("chrome", testCapabilities.getBrowserName());
        Assert.assertEquals(true, testCapabilities.getCapability(CapabilityType.ACCEPT_INSECURE_CERTS));
        LinkedList<String> list = new LinkedList<>();
        list.add("headless");
        Assert.assertEquals(list, config.getArguments());

        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("geolocation.enabled", true);
        prefs.put("renderer.memory_cache.size", 120000);
        prefs.put("homepage", "https://www.xceptance.com");
        Assert.assertEquals(prefs, config.getPreferences());

        Assert.assertEquals(new File("target").getAbsolutePath(), config.getDownloadDirectory());
    }

    private void checkMultiChrome(BrowserConfiguration config)
    {
        Assert.assertNotNull(config);
        Assert.assertEquals("multiChrome", config.getConfigTag());
        Assert.assertEquals("Multi Argument Chrome", config.getName());
        MutableCapabilities testCapabilities = config.getCapabilities();
        Assert.assertEquals("chrome", testCapabilities.getBrowserName());
        LinkedList<String> list = new LinkedList<>();
        list.add("-crash-test");
        list.add("-window-position=0,0");
        list.add("-window-size=1024,768");
        Assert.assertEquals(list, config.getArguments());
    }

    private void checkFirefox(BrowserConfiguration config)
    {
        Assert.assertNotNull(config);
        Assert.assertEquals("firefox", config.getConfigTag());
        Assert.assertEquals("Mozilla Firefox", config.getName());
        Assert.assertEquals(null, config.getTestEnvironment());
        MutableCapabilities testCapabilities = config.getCapabilities();
        Assert.assertEquals("firefox", testCapabilities.getBrowserName());
        LinkedList<String> list = new LinkedList<>();
        list.add("headless");
        Assert.assertEquals(list, config.getArguments());

        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("media.navigator.permission.disabled", true);
        prefs.put("app.update.backgroundMaxErrors", 1);
        prefs.put("browser.startup.homepage", "https://www.xceptance.com");
        Assert.assertEquals(prefs, config.getPreferences());
        Assert.assertEquals(new File("target").getAbsolutePath(), config.getDownloadDirectory());
    }

    private void checkMultiFirefox(BrowserConfiguration config)
    {
        Assert.assertNotNull(config);
        Assert.assertEquals("multiFirefox", config.getConfigTag());
        Assert.assertEquals("Multi Argument Firefox", config.getName());
        MutableCapabilities testCapabilities = config.getCapabilities();
        Assert.assertEquals("firefox", testCapabilities.getBrowserName());
        LinkedList<String> list = new LinkedList<>();
        list.add("-headless");
        list.add("-width=1024");
        list.add("-height=768");
        Assert.assertEquals(list, config.getArguments());
    }

    private void checkTestEnvironment(BrowserConfiguration config)
    {
        Assert.assertNotNull(config);
        Assert.assertEquals("testEnvironmentFlags", config.getConfigTag());
        Assert.assertEquals("Test Environment Browser", config.getName());
        MutableCapabilities testCapabilities = config.getCapabilities();
        HashMap<String, Object> testGridProperties = config.getGridProperties();
        Assert.assertEquals("chrome", testCapabilities.getBrowserName());
        Assert.assertEquals(1234, testGridProperties.get("idleTimeout"));
        Assert.assertEquals(1234, testGridProperties.get("idletimeout"));
        Assert.assertEquals(5678, testGridProperties.get("maxDuration"));
        Assert.assertEquals(5678, testGridProperties.get("maxduration"));
        Assert.assertEquals("3.1234", testGridProperties.get("seleniumVersion"));
        Assert.assertEquals("3.1234", testGridProperties.get("selenium-version"));
        Assert.assertEquals("800x600", testGridProperties.get("screenResolution"));
        Assert.assertEquals("800x600", testGridProperties.get("screen-resolution"));
        Assert.assertEquals("800x600", testGridProperties.get("resolution"));
        Assert.assertEquals("Windows 10", testGridProperties.get("os"));
        Assert.assertEquals("MyDevice", testGridProperties.get("deviceName"));
        Assert.assertEquals("landscape", testGridProperties.get("deviceOrientation"));
        Assert.assertEquals("landscape", testGridProperties.get("orientation"));
    }

    private void checkTestEnvironment2(BrowserConfiguration config)
    {
        Assert.assertNotNull(config);
        Assert.assertEquals("testEnvironmentFlags2", config.getConfigTag());
        Assert.assertEquals("Test Environment Browser", config.getName());
        MutableCapabilities testCapabilities = config.getCapabilities();
        HashMap<String, Object> testGridProperties = config.getGridProperties();
        Assert.assertEquals("chrome", testCapabilities.getBrowserName());
        Assert.assertEquals(1234, testGridProperties.get("idleTimeout"));
        Assert.assertEquals(1234, testGridProperties.get("idletimeout"));
        Assert.assertEquals(5678, testGridProperties.get("maxDuration"));
        Assert.assertEquals(5678, testGridProperties.get("maxduration"));
        Assert.assertEquals("3.1234", testGridProperties.get("seleniumVersion"));
        Assert.assertEquals("3.1234", testGridProperties.get("selenium-version"));
        Assert.assertEquals("800x600", testGridProperties.get("screenResolution"));
        Assert.assertEquals("800x600", testGridProperties.get("screen-resolution"));
        Assert.assertEquals("800x600", testGridProperties.get("resolution"));
        Assert.assertEquals("Windows 10", testGridProperties.get("os"));
        Assert.assertEquals("MyDevice", testGridProperties.get("deviceName"));
        Assert.assertEquals("portrait", testGridProperties.get("orientation"));
    }
}