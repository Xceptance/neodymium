package com.xceptance.neodymium.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.xceptance.neodymium.testclasses.webDriver.LocalProxyTrustAllServers;
import com.xceptance.neodymium.testclasses.webDriver.LocalProxyUsingProvidedCertificates;
import com.xceptance.neodymium.testclasses.webDriver.LocalProxyUsingProvidedCertificatesRuntimeException;
import com.xceptance.neodymium.testclasses.webDriver.LocalProxyUsingSelfCreatedCertificates;
import com.xceptance.neodymium.testclasses.webDriver.ValidateClearReuseWebDriverCache;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepBrowserOpenAnnotationClassDoesntInterfereWithConfig;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepBrowserOpenAnnotationClassOverridesConfig;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepBrowserOpenAnnotationMethodDoesntInterfereWithConfig;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepBrowserOpenAnnotationMethodOverridesClass;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepBrowserOpenAnnotationMethodOverridesConfig;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepBrowserOpenOnFailureAnnotationClassDoesntInterfereWithConfig;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepBrowserOpenOnFailureAnnotationClassOverridesConfig;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepBrowserOpenOnFailureAnnotationMethodDoesntInterfereWithConfig;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepBrowserOpenOnFailureAnnotationMethodOverridesConfig;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepWebDriverOpen;
import com.xceptance.neodymium.testclasses.webDriver.ValidateKeepWebDriverOpenOnFailure;
import com.xceptance.neodymium.testclasses.webDriver.ValidatePreventReuseWebDriver;
import com.xceptance.neodymium.testclasses.webDriver.ValidateReuseWebDriver;
import com.xceptance.neodymium.testclasses.webDriver.ValidateWebDriverClosed;
import com.xceptance.neodymium.testclasses.webDriver.ValidateWebDriverMaxReuse;
import com.xceptance.neodymium.testclasses.webDriver.ValidateWebDriverMaxReuseWithTwoWebDrivers;
import com.xceptance.neodymium.testclasses.webDriver.ValidateWebDriverReuseCounter;

public class NeodymiumWebDriverTest extends NeodymiumTest
{
    @Test
    public void testValidateWebDriverClosed()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "false");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateWebDriverClosed-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(ValidateWebDriverClosed.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testValidateReuseWebDriver()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateReuseWebDriver-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(ValidateReuseWebDriver.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testValidateWebDriverReuseCounter()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.webDriver.maxReuse", "0");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateWebDriverReuseCounter-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(ValidateWebDriverReuseCounter.class);
        checkPass(result, 6, 0);
    }

    @Test
    public void testValidateWebDriverMaxReuse()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.webDriver.maxReuse", "1");

        addPropertiesForTest("temp-ValidateWebDriverMaxReuse-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(ValidateWebDriverMaxReuse.class);
        checkPass(result, 5, 0);
    }

    @Test
    public void testValidateWebDriverMaxReuseWithTwoWebDrivers()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.webDriver.maxReuse", "2");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateWebDriverMaxReuseWithTwoWebDrivers-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(ValidateWebDriverMaxReuseWithTwoWebDrivers.class);
        checkPass(result, 9, 0);
    }

    @Test
    public void testValidateClearReuseWebDriverCache()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateClearReuseWebDriverCache-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(ValidateClearReuseWebDriverCache.class);
        checkPass(result, 3, 0);
    }

    @Test
    public void testValidatePreventReuseWebDriver()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidatePreventReuseWebDriver-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(ValidatePreventReuseWebDriver.class);
        checkPass(result, 3, 0);
    }

    @Test
    public void testValidateKeepWebDriverOpen()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "false");
        properties.put("neodymium.webDriver.keepBrowserOpen", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepWebDriverOpen-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepWebDriverOpen.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testValidateKeepWebDriverOpenOnFailure()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "true");
        properties.put("neodymium.webDriver.keepBrowserOpen", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepWebDriverOpenOnFailure-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepWebDriverOpenOnFailure.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testValidateKeepBrowserOpenAnnotationClassDoesntInterfereWithConfig()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "false");
        properties.put("neodymium.webDriver.keepBrowserOpen", "false");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepBrowserOpenAnnotationClassDoesntInterfereWithConfig-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepBrowserOpenAnnotationClassDoesntInterfereWithConfig.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testValidateKeepBrowserOpenOnFailureAnnotationClassDoesntInterfereWithConfig()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "true");
        properties.put("neodymium.webDriver.keepBrowserOpen", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepBrowserOpenOnFailureAnnotationClassDoesntInterfereWithConfig-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepBrowserOpenOnFailureAnnotationClassDoesntInterfereWithConfig.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testValidateKeepBrowserOpenAnnotationMethodDoesntInterfereWithConfig()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "false");
        properties.put("neodymium.webDriver.keepBrowserOpen", "false");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepBrowserOpenAnnotationMethodDoesntInterfereWithConfig-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepBrowserOpenAnnotationMethodDoesntInterfereWithConfig.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testValidateKeepBrowserOpenOnFailureAnnotationMethodDoesntInterfereWithConfig()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "true");
        properties.put("neodymium.webDriver.keepBrowserOpen", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepBrowserOpenOnFailureAnnotationMethodDoesntInterfereWithConfig-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepBrowserOpenOnFailureAnnotationMethodDoesntInterfereWithConfig.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testValidateKeepBrowserOpenAnnotationClassOverridesConfig()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "true");
        properties.put("neodymium.webDriver.keepBrowserOpen", "false");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepBrowserOpenAnnotationClassOverridesConfig-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepBrowserOpenAnnotationClassOverridesConfig.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testValidateKeepBrowserOpenOnFailureAnnotationClassOverridesConfig()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "false");
        properties.put("neodymium.webDriver.keepBrowserOpen", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepBrowserOpenOnFailureAnnotationClassOverridesConfig-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepBrowserOpenOnFailureAnnotationClassOverridesConfig.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testValidateKeepBrowserOpenAnnotationMethodOverridesConfig()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "true");
        properties.put("neodymium.webDriver.keepBrowserOpen", "false");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepBrowserOpenAnnotationMethodOverridesConfig-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepBrowserOpenAnnotationMethodOverridesConfig.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testValidateKeepBrowserOpenOnFailureAnnotationMethodOverridesConfig()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "false");
        properties.put("neodymium.webDriver.keepBrowserOpen", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepBrowserOpenAnnotationMethodOverridesConfig-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepBrowserOpenOnFailureAnnotationMethodOverridesConfig.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testValidateKeepBrowserOpenAnnotationMethodOverridesClass()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "false");
        properties.put("neodymium.webDriver.keepBrowserOpen", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateKeepBrowserOpenAnnotationMethodOverridesConfig-neodymium.properties", properties);

        // XVFB or a display needed
        Result result = JUnitCore.runClasses(ValidateKeepBrowserOpenAnnotationMethodOverridesClass.class);
        checkFail(result, 3, 0, 1);
    }

    @Test
    public void testLocalProxyTrustAllServers()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.localproxy", "true");
        properties.put("neodymium.localproxy.certificate", "false");
        properties.put("neodymium.url.host", "authenticationtest.com");
        properties.put("neodymium.basicauth.username", "User");
        properties.put("neodymium.basicauth.password", "Pass");

        addPropertiesForTest("temp-LocalProxyTrustAllServers-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(LocalProxyTrustAllServers.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testLocalProxyUsingSelfCreatedCertificates()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.localproxy", "true");
        properties.put("neodymium.localproxy.certificate", "true");

        addPropertiesForTest("temp-LocalProxyUsingSelfCreatedCertificates-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(LocalProxyUsingSelfCreatedCertificates.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testLocalProxyUsingProvidedCertificates()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.localproxy", "true");
        properties.put("neodymium.localproxy.certificate", "true");
        properties.put("neodymium.localproxy.certificate.generate ", "false");
        properties.put("neodymium.localproxy.certificate.archiveFile ", "./config/LocalProxyTestCertificate.p12");
        properties.put("neodymium.localproxy.certificate.archivetype ", "PKCS12");
        properties.put("neodymium.localproxy.certificate.name ", "MitmProxy");
        properties.put("neodymium.localproxy.certificate.password ", "xceptance");

        addPropertiesForTest("temp-LocalProxyUsingProvidedCertificates-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(LocalProxyUsingProvidedCertificates.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testLocalProxyUsingProvidedCertificatesRuntimeException()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.localproxy", "true");
        properties.put("neodymium.localproxy.certificate", "true");
        properties.put("neodymium.localproxy.certificate.generate ", "false");

        addPropertiesForTest("temp-LocalProxyUsingProvidedCertificatesRuntimeException-neodymium.properties", properties);

        Result result = JUnitCore.runClasses(LocalProxyUsingProvidedCertificatesRuntimeException.class);
        checkFail(result, 1, 0, 1,
                  "The local proxy certificate isn't fully configured. Please check: certificate archive type, certificate archive file, certificate name and certificate password.");
    }

    public static void assertWebDriverClosed(WebDriver webDriver)
    {
        Assert.assertNotNull(webDriver);
        RemoteWebDriver driver = (RemoteWebDriver) webDriver;
        Assert.assertNull(driver.getSessionId());
    }

    public static void assertWebDriverAlive(WebDriver webDriver)
    {
        Assert.assertNotNull(webDriver);
        RemoteWebDriver driver = (RemoteWebDriver) webDriver;
        Assert.assertNotNull(driver.getSessionId());
    }

    public static void assertProxyStopped(BrowserUpProxy proxy)
    {
        Assert.assertNotNull(proxy);
        Assert.assertTrue(proxy.isStarted());
        Assert.assertTrue(((BrowserUpProxyServer) proxy).isStopped());
    }

    public static void assertProxyAlive(BrowserUpProxy proxy)
    {
        Assert.assertNotNull(proxy);
        Assert.assertTrue(proxy.isStarted());
        Assert.assertFalse(((BrowserUpProxyServer) proxy).isStopped());
    }
}
