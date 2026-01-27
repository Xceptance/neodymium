package com.xceptance.neodymium.junit5.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.xceptance.neodymium.junit5.testclasses.webDriver.LocalProxyTrustAllServers;
import com.xceptance.neodymium.junit5.testclasses.webDriver.LocalProxyUsingProvidedCertificates;
import com.xceptance.neodymium.junit5.testclasses.webDriver.LocalProxyUsingProvidedCertificatesRuntimeException;
import com.xceptance.neodymium.junit5.testclasses.webDriver.LocalProxyUsingSelfCreatedCertificates;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateClearReuseWebDriverCache;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepBrowserOpenAnnotationClassDoesntInterfereWithConfig;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepBrowserOpenAnnotationClassOverridesConfig;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepBrowserOpenAnnotationMethodDoesntInterfereWithConfig;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepBrowserOpenAnnotationMethodOverridesClass;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepBrowserOpenAnnotationMethodOverridesConfig;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepBrowserOpenOnFailureAnnotationClassDoesntInterfereWithConfig;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepBrowserOpenOnFailureAnnotationClassOverridesConfig;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepBrowserOpenOnFailureAnnotationMethodDoesntInterfereWithConfig;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepBrowserOpenOnFailureAnnotationMethodOverridesConfig;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepWebDriverOpen;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateKeepWebDriverOpenOnFailure;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidatePreventReuseWebDriver;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateReuseWebDriver;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateWebDriverClosed;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateWebDriverMaxReuse;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateWebDriverMaxReuseWithTwoWebDrivers;
import com.xceptance.neodymium.junit5.testclasses.webDriver.ValidateWebDriverReuseCounter;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class NeodymiumWebDriverTest extends AbstractNeodymiumTest
{
    @Test
    public void testValidateWebDriverClosed()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "false");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateWebDriverClosed-neodymium.properties", properties);

        NeodymiumTestExecutionSummary summary = run(ValidateWebDriverClosed.class);
        checkPass(summary, 6, 0);
    }

    @Test
    public void testValidateReuseWebDriver()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateReuseWebDriver-neodymium.properties", properties);

        NeodymiumTestExecutionSummary summary = run(ValidateReuseWebDriver.class);
        checkPass(summary, 6, 0);
    }

    @Test
    public void testValidateWebDriverReuseCounter()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.webDriver.maxReuse", "0");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateWebDriverReuseCounter-neodymium.properties", properties);

        NeodymiumTestExecutionSummary summary = run(ValidateWebDriverReuseCounter.class);
        checkPass(summary, 18, 0);
    }

    @Test
    public void testValidateWebDriverMaxReuse()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.webDriver.maxReuse", "1");

        addPropertiesForTest("temp-ValidateWebDriverMaxReuse-neodymium.properties", properties);

        NeodymiumTestExecutionSummary summary = run(ValidateWebDriverMaxReuse.class);
        checkPass(summary, 15, 0);
    }

    @Test
    public void testValidateWebDriverMaxReuseWithTwoWebDrivers()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.webDriver.maxReuse", "2");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateWebDriverMaxReuseWithTwoWebDrivers-neodymium.properties", properties);

        NeodymiumTestExecutionSummary summary = run(ValidateWebDriverMaxReuseWithTwoWebDrivers.class);
        checkPass(summary, 27, 0);
    }

    @Test
    public void testValidateClearReuseWebDriverCache()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidateClearReuseWebDriverCache-neodymium.properties", properties);

        NeodymiumTestExecutionSummary summary = run(ValidateClearReuseWebDriverCache.class);
        checkPass(summary, 9, 0);
    }

    @Test
    public void testValidatePreventReuseWebDriver()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.localproxy", "true");

        addPropertiesForTest("temp-ValidatePreventReuseWebDriver-neodymium.properties", properties);

        NeodymiumTestExecutionSummary summary = run(ValidatePreventReuseWebDriver.class);
        checkPass(summary, 9, 0);
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
        NeodymiumTestExecutionSummary summary = run(ValidateKeepWebDriverOpen.class);
        checkFail(summary, 9, 0, 1);
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
        NeodymiumTestExecutionSummary summary = run(ValidateKeepWebDriverOpenOnFailure.class);
        checkFail(summary, 9, 0, 1);
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
        NeodymiumTestExecutionSummary result = run(ValidateKeepBrowserOpenAnnotationClassDoesntInterfereWithConfig.class);
        checkFail(result, 9, 0, 1);
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
        NeodymiumTestExecutionSummary result = run(ValidateKeepBrowserOpenOnFailureAnnotationClassDoesntInterfereWithConfig.class);
        checkFail(result, 9, 0, 1);
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
        NeodymiumTestExecutionSummary result = run(ValidateKeepBrowserOpenAnnotationMethodDoesntInterfereWithConfig.class);
        checkFail(result, 9, 0, 1);
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
        NeodymiumTestExecutionSummary result = run(ValidateKeepBrowserOpenOnFailureAnnotationMethodDoesntInterfereWithConfig.class);
        checkFail(result, 9, 0, 1);
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
        NeodymiumTestExecutionSummary result = run(ValidateKeepBrowserOpenAnnotationClassOverridesConfig.class);
        checkFail(result, 9, 0, 1);
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
        NeodymiumTestExecutionSummary result = run(ValidateKeepBrowserOpenOnFailureAnnotationClassOverridesConfig.class);
        checkFail(result, 9, 0, 1);
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
        NeodymiumTestExecutionSummary result = run(ValidateKeepBrowserOpenAnnotationMethodOverridesConfig.class);
        checkFail(result, 9, 0, 1);
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
        NeodymiumTestExecutionSummary result = run(ValidateKeepBrowserOpenOnFailureAnnotationMethodOverridesConfig.class);
        checkFail(result, 9, 0, 1);
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
        NeodymiumTestExecutionSummary result = run(ValidateKeepBrowserOpenAnnotationMethodOverridesClass.class);
        checkFail(result, 9, 0, 1);
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

        NeodymiumTestExecutionSummary summary = run(LocalProxyTrustAllServers.class);
        checkPass(summary, 3, 0);
    }

    @Test
    public void testLocalProxyUsingSelfCreatedCertificates()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.localproxy", "true");
        properties.put("neodymium.localproxy.certificate", "true");

        addPropertiesForTest("temp-LocalProxyUsingSelfCreatedCertificates-neodymium.properties", properties);

        NeodymiumTestExecutionSummary summary = run(LocalProxyUsingSelfCreatedCertificates.class);
        checkPass(summary, 3, 0);
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

        NeodymiumTestExecutionSummary summary = run(LocalProxyUsingProvidedCertificates.class);
        checkPass(summary, 3, 0);
    }

    @Test
    public void testLocalProxyUsingProvidedCertificatesRuntimeException()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.localproxy", "true");
        properties.put("neodymium.localproxy.certificate", "true");
        properties.put("neodymium.localproxy.certificate.generate ", "false");

        addPropertiesForTest("temp-LocalProxyUsingProvidedCertificatesRuntimeException-neodymium.properties", properties);

        NeodymiumTestExecutionSummary summary = run(LocalProxyUsingProvidedCertificatesRuntimeException.class);
        checkFail(summary, 3, 0, 1,
                  "java.lang.RuntimeException: The local proxy certificate isn't fully configured. Please check: certificate archive type, certificate archive file, certificate name and certificate password.");
    }

    public static void assertWebDriverClosed(WebDriver webDriver)
    {
        Assertions.assertNotNull(webDriver);
        RemoteWebDriver driver = (RemoteWebDriver) webDriver;
        Assertions.assertNull(driver.getSessionId());
    }

    public static void assertWebDriverAlive(WebDriver webDriver)
    {
        Assertions.assertNotNull(webDriver);
        RemoteWebDriver driver = (RemoteWebDriver) webDriver;
        Assertions.assertNotNull(driver.getSessionId());
    }

    public static void assertProxyStopped(BrowserUpProxy proxy)
    {
        Assertions.assertNotNull(proxy);
        Assertions.assertTrue(proxy.isStarted());
        Assertions.assertTrue(((BrowserUpProxyServer) proxy).isStopped());
    }

    public static void assertProxyAlive(BrowserUpProxy proxy)
    {
        Assertions.assertNotNull(proxy);
        Assertions.assertTrue(proxy.isStarted());
        Assertions.assertFalse(((BrowserUpProxyServer) proxy).isStopped());
    }
}
