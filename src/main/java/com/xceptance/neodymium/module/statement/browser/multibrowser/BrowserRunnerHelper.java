package com.xceptance.neodymium.module.statement.browser.multibrowser;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openqa.selenium.remote.Browser.CHROME;
import static org.openqa.selenium.remote.Browser.EDGE;
import static org.openqa.selenium.remote.Browser.FIREFOX;
import static org.openqa.selenium.remote.Browser.IE;
import static org.openqa.selenium.remote.Browser.SAFARI;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.support.events.EventFiringWebDriver;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.client.ClientUtil;
import com.browserup.bup.mitm.CertificateAndKeySource;
import com.browserup.bup.mitm.KeyStoreFileCertificateSource;
import com.browserup.bup.mitm.RootCertificateGenerator;
import com.browserup.bup.mitm.manager.ImpersonatingMitmManager;
import com.browserup.bup.proxy.auth.AuthType;
import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.impl.Plugins;
import com.codeborne.selenide.proxy.SelenideProxyServer;
import com.codeborne.selenide.proxy.SelenideProxyServerFactory;
import com.xceptance.neodymium.NeodymiumWebDriverListener;
import com.xceptance.neodymium.module.statement.browser.multibrowser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.module.statement.browser.multibrowser.configuration.BrowserConfigurationMapper;
import com.xceptance.neodymium.module.statement.browser.multibrowser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.module.statement.browser.multibrowser.configuration.TestEnvironment;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.NeodymiumConfiguration;

/**
 * Helper class to create webdriver for the test
 * 
 * @author olha
 */
public final class BrowserRunnerHelper
{
    private static final String CERT_PASSWORD = "xceptance";

    private static final String CERT_NAME = "MITMProxy";

    private static final String CERT_FORMAT = "PKCS12";

    private static List<String> chromeBrowsers = new LinkedList<String>();

    private static List<String> firefoxBrowsers = new LinkedList<String>();

    private static List<String> internetExplorerBrowsers = new LinkedList<String>();

    private static List<String> safariBrowsers = new LinkedList<String>();

    private final static Object mutex = new Object();

    static
    {
        chromeBrowsers.add(CHROME.browserName());

        firefoxBrowsers.add(FIREFOX.browserName());

        internetExplorerBrowsers.add(IE.browserName());
        internetExplorerBrowsers.add(EDGE.browserName());

        safariBrowsers.add(SAFARI.browserName());
    }

    /**
     * Sets the browser window size
     * <p>
     * Reads the default size from browser properties and applies them to the browser window as long as its no
     * device-emulation test. In case of device-emulation the emulated device specifies the size of the browser window.
     *
     * @param config
     *            {@link BrowserConfiguration} that describes the requested browser properties
     * @param driver
     *            {@link WebDriver} instance of the configured {@link BrowserConfiguration}
     */
    public static void setBrowserWindowSize(final BrowserConfiguration config, final WebDriver driver)
    {
        // get the configured window size and set it if defined
        final int windowWidth = Neodymium.configuration().getWindowWidth();
        final int windowHeight = Neodymium.configuration().getWindowHeight();

        final int configuredBrowserWidth = config.getBrowserWidth();
        final int configuredBrowserHeight = config.getBrowserHeight();

        Dimension browserSize = null;
        // first check if the configured browser profile has a defined size, else use the default browser size
        if (configuredBrowserWidth > 0 && configuredBrowserHeight > 0)
        {
            browserSize = new Dimension(configuredBrowserWidth, configuredBrowserHeight);
        }
        else if (windowWidth > 0 && windowHeight > 0)
        {
            browserSize = new Dimension(windowWidth, windowHeight);
        }

        try
        {
            if (browserSize != null)
                driver.manage().window().setSize(browserSize);
        }
        catch (final UnsupportedCommandException e)
        {
            // same as the exception handling below
            if (!e.getMessage().contains("not yet supported"))
                throw e;
        }
        catch (final WebDriverException e)
        {
            // on saucelabs in some cases like iphone emulation you can't resize the browser.
            // they throw an unchecked WebDriverException with the message "Not yet implemented"
            // if we catch an exception we check the message. if another message is set we throw the exception else
            // we suppress it
            if (!e.getMessage().contains("Not yet implemented"))
                throw e;
        }
    }

    /**
     * Creates a {@link FirefoxBinary} object and sets the path, but only if the path is not blank.
     * 
     * @param pathToBrowser
     *            the path to the browser binary
     * @return the Firefox binary
     */
    private static FirefoxBinary createFirefoxBinary(final String pathToBrowser)
    {
        if (StringUtils.isNotBlank(pathToBrowser))
        {
            return new FirefoxBinary(new File(pathToBrowser));
        }
        else
        {
            return new FirefoxBinary();
        }
    }

    /**
     * Instantiate the {@link WebDriver} according to the configuration read from {@link Browser} annotations.
     * 
     * @param config
     *            {@link BrowserConfiguration} that describes the desired browser instance
     * @return {@link WebDriverStateContainer} the instance of the browser described in {@link BrowserConfiguration} and
     *         in {@link NeodymiumConfiguration}
     * @throws MalformedURLException
     *             if <a href="https://github.com/Xceptance/neodymium-library/wiki/Selenium-grid">Selenium grid</a> is
     *             used and the given grid URL is invalid
     */
    public static WebDriverStateContainer createWebDriverStateContainer(final BrowserConfiguration config, Object testClassInstance)
        throws MalformedURLException
    {
        final MutableCapabilities capabilities = config.getCapabilities();
        final WebDriverStateContainer wDSC = new WebDriverStateContainer();
        SelenideProxyServer selenideProxyServer = null;
        if (Neodymium.configuration().useLocalProxy())
        {
            BrowserUpProxy proxy = setupEmbeddedProxy();

            // set the Proxy for later usage
            wDSC.setProxy(proxy);
            // configure the proxy via capabilities
            capabilities.setCapability(CapabilityType.PROXY, ClientUtil.createSeleniumProxy(proxy));
        }
        else if (Neodymium.configuration().useProxy())
        {
            capabilities.setCapability(CapabilityType.PROXY, createProxyCapabilities());
        }

        final String testEnvironment = config.getTestEnvironment();
        if (StringUtils.isEmpty(testEnvironment) || "local".equalsIgnoreCase(testEnvironment))
        {
            if (Neodymium.configuration().enableSelenideProxy())
            {
                SelenideProxyServerFactory selenideProxyServerFactory = Plugins.inject(SelenideProxyServerFactory.class);
                selenideProxyServer = selenideProxyServerFactory.create(new SelenideConfig(),
                                                                        (Proxy) capabilities.getCapability(CapabilityType.PROXY));
                var proxy = selenideProxyServer.getSeleniumProxy();
                capabilities.setCapability(CapabilityType.PROXY, proxy);
            }
            final String browserName = capabilities.getBrowserName();
            if (chromeBrowsers.contains(browserName))
            {
                final ChromeOptions options = (ChromeOptions) capabilities;

                // do we have a custom path?
                final String pathToBrowser = Neodymium.configuration().getChromeBrowserPath();
                if (StringUtils.isNotBlank(pathToBrowser))
                {
                    options.setBinary(pathToBrowser);
                }
                if (config.isHeadless())
                {
                    options.addArguments("--headless");
                }
                if (config.getArguments() != null && config.getArguments().size() > 0)
                {
                    options.addArguments(config.getArguments());
                }


                if (config.getPreferences() != null && !config.getPreferences().isEmpty())
                {
                    options.setExperimentalOption("prefs", config.getPreferences());
                }

                if ((config.getPreferences() != null && !config.getPreferences().isEmpty()) || StringUtils.isNotBlank(config.getDownloadDirectory()))
                {
                    HashMap<String, Object> prefs = new HashMap<>();

                    // if we have configured prefs, we need to add all to the experimental options
                    if(config.getPreferences() != null && !config.getPreferences().isEmpty())
                    {
                        prefs.putAll(config.getPreferences());
                    }
                    // if we have configured a download folder separately, it'll override the general config
                    if(StringUtils.isNotBlank(config.getDownloadDirectory()))
                    {
                        prefs.put("download.default_directory", config.getDownloadDirectory());
                        prefs.put("plugins.always_open_pdf_externally", true);                        
                    }

                    options.setExperimentalOption("prefs", prefs);
                }

                wDSC.setWebDriver(new ChromeDriver(options));
            }
            else if (firefoxBrowsers.contains(browserName))
            {
                final FirefoxOptions options = new FirefoxOptions().merge(capabilities);
                options.setBinary(createFirefoxBinary(Neodymium.configuration().getFirefoxBrowserPath()));
                if (config.isHeadless())
                {
                    options.addArguments("--headless");
                }
                if (config.getArguments() != null && config.getArguments().size() > 0)
                {
                    options.addArguments(config.getArguments());
                }
                if (StringUtils.isNotBlank(config.getDownloadDirectory()))
                {
                    FirefoxProfile profile = new FirefoxProfile();
                }
              
                if ((config.getPreferences() != null && !config.getPreferences().isEmpty()) || StringUtils.isNotBlank(config.getDownloadDirectory()))
                {
                    FirefoxProfile profile = new FirefoxProfile();

                    // if we have configured prefs, we need to add all to the experimental options
                    if (config.getPreferences() != null && !config.getPreferences().isEmpty())
                    {
                        // differentiate types of preference values to avoid misunderstanding
                        config.getPreferences().forEach((key, val) -> {
                            if (val.equals("true") || val.equals("false"))
                            {
                                profile.setPreference(key, Boolean.parseBoolean(val.toString()));
                            }
                            else if (StringUtils.isNumeric(val.toString()))
                            {
                                profile.setPreference(key, Integer.parseInt(val.toString()));
                            }
                            else
                            {
                                profile.setPreference(key, val.toString());
                            }
                        });
                    }

                    // if we have configured a download folder separately, it'll override the general config
                    if (StringUtils.isNotBlank(config.getDownloadDirectory()))
                    {
                        profile.setPreference("browser.download.dir", config.getDownloadDirectory());
                        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", popularContentTypes());
                        profile.setPreference("pdfjs.disabled", true);
                        profile.setPreference("browser.download.folderList", 2);
                    }
                    options.setProfile(profile);
                }
                
                wDSC.setWebDriver(new FirefoxDriver(new GeckoDriverService.Builder().withAllowHosts("localhost").build(), options));
            }
            else if (internetExplorerBrowsers.contains(browserName))
            {
                final InternetExplorerOptions options = new InternetExplorerOptions().merge(capabilities);
                if (config.getArguments() != null && config.getArguments().size() > 0)
                {
                    for (String argument : config.getArguments())
                    {
                        options.addCommandSwitches(argument);
                    }
                }
                wDSC.setWebDriver(new InternetExplorerDriver(options));
            }
            else if (safariBrowsers.contains(browserName))
            {
                final SafariOptions options = (SafariOptions) capabilities;
                wDSC.setWebDriver(new SafariDriver(options));
            }
            else
            {
                wDSC.setWebDriver(new RemoteWebDriver(capabilities));
            }

        }
        else
        {
            // establish connection to target website
            TestEnvironment testEnvironmentProperties = MultibrowserConfiguration.getInstance().getTestEnvironment(testEnvironment);
            if (testEnvironmentProperties == null)
            {
                throw new IllegalArgumentException("No properties found for test environment: \"" + testEnvironment + "\"");
            }
            String testEnvironmentUrl = testEnvironmentProperties.getUrl();
            ClientConfig configClient = ClientConfig.defaultConfig();
            configClient = configClient.baseUrl(new URL(testEnvironmentUrl));
            config.getGridProperties().put("userName", testEnvironmentProperties.getUsername());
            config.getGridProperties().put("accessKey", testEnvironmentProperties.getPassword());
            String buildId = StringUtils.isBlank(System.getenv("BUILD_NUMBER")) ? "local run" : System.getenv("BUILD_NUMBER");
            config.getGridProperties().put("sessionName", testClassInstance.getClass().toString());
            config.getGridProperties().put("buildName", "Test Automation");
            config.getGridProperties().put("buildIdentifier", buildId);
            if (testEnvironmentUrl.contains("browserstack"))
            {
                capabilities.setCapability("bstack:options", config.getGridProperties());
            }
            else if (testEnvironmentUrl.contains("saucelabs"))
            {
                capabilities.setCapability("sauce:options", config.getGridProperties());
            }
            else
            {
                String optionsTag = testEnvironmentProperties.getOptionsTag();
                if (StringUtils.isBlank(optionsTag))
                {
                    for (String key : config.getGridProperties().keySet())
                    {
                        capabilities.setCapability(key, config.getGridProperties().get(key));
                    }
                }
                else
                {
                    capabilities.setCapability(optionsTag, config.getGridProperties());
                }
            }
            wDSC.setWebDriver(new RemoteWebDriver(new HttpCommandExecutor(new HashMap<>(), configClient, new NeodymiumProxyHttpClientFactory(testEnvironmentProperties)), capabilities));
        }
        EventFiringWebDriver eFWDriver = new EventFiringWebDriver(wDSC.getWebDriver());
        eFWDriver.register(new NeodymiumWebDriverListener());
        wDSC.setWebDriver(eFWDriver);
        WebDriverRunner.webdriverContainer.setWebDriver(wDSC.getWebDriver(), selenideProxyServer);
        return wDSC;
    }

    private static BrowserUpProxy setupEmbeddedProxy()
    {
        // instantiate the proxy
        final BrowserUpProxy proxy = new BrowserUpProxyServer();

        if (Neodymium.configuration().useLocalProxyWithSelfSignedCertificate())
        {
            final CertificateAndKeySource rootCertificateSource = createLocalProxyRootCertSource();
            final ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder().rootCertificateSource(rootCertificateSource).build();
            proxy.setMitmManager(mitmManager);
        }
        else
        {
            // disable proxy certificate verification
            proxy.setTrustAllServers(true);
        }

        // start the proxy
        proxy.start();

        // default basic authentication via the proxy
        final String host = Neodymium.configuration().host();
        final String bUsername = Neodymium.configuration().basicAuthUsername();
        final String bPassword = Neodymium.configuration().basicAuthPassword();
        if (StringUtils.isNoneBlank(host, bUsername, bPassword))
        {
            proxy.autoAuthorization(host, bUsername, bPassword, AuthType.BASIC);
        }

        return proxy;
    }

    private static CertificateAndKeySource createLocalProxyRootCertSource()
    {
        if (Neodymium.configuration().localProxyGenerateSelfSignedCertificate())
        {
            synchronized (mutex)
            {
                final File certFile = new File("./config/embeddedLocalProxySelfSignedRootCertificate.p12");
                certFile.deleteOnExit();
                if (certFile.canRead())
                {
                    return new KeyStoreFileCertificateSource(CERT_FORMAT, certFile, CERT_NAME, CERT_PASSWORD);
                }
                else
                {
                    // create a dynamic CA root certificate generator using default settings (2048-bit RSA keys)
                    final RootCertificateGenerator rootCertificateGenerator = RootCertificateGenerator.builder().build();
                    // save the dynamically-generated CA root certificate for installation in a browser
                    rootCertificateGenerator.saveRootCertificateAndKey(CERT_FORMAT, certFile, CERT_NAME, CERT_PASSWORD);
                    return rootCertificateGenerator;
                }
            }
        }
        else
        {
            // configure the MITM using the provided certificate
            final String type = Neodymium.configuration().localProxyCertificateArchiveType();
            final String file = Neodymium.configuration().localProxyCertificateArchiveFile();
            final String cName = Neodymium.configuration().localProxyCertificateName();
            final String cPassword = Neodymium.configuration().localProxyCertificatePassword();
            if (StringUtils.isAnyBlank(type, file, cName, cPassword))
            {
                throw new RuntimeException("The local proxy certificate isn't fully configured. Please check: certificate archive type, certificate archive file, certificate name and certificate password.");
            }
            return new KeyStoreFileCertificateSource(type, new File(file), cName, cPassword);
        }
    }

    public static Proxy createProxyCapabilities()
    {
        final String proxyHost = Neodymium.configuration().getProxyHost() + ":" + Neodymium.configuration().getProxyPort();

        final Proxy webdriverProxy = new Proxy();
        webdriverProxy.setHttpProxy(proxyHost);
        webdriverProxy.setSslProxy(proxyHost);
        if (!StringUtils.isAllEmpty(Neodymium.configuration().getProxySocketUsername(), Neodymium.configuration().getProxySocketPassword())
            || Neodymium.configuration().getProxySocketVersion() != null)
        {
            webdriverProxy.setSocksProxy(proxyHost);
            if (StringUtils.isNoneEmpty(Neodymium.configuration().getProxySocketUsername(),
                                        Neodymium.configuration().getProxySocketPassword()))
            {
                webdriverProxy.setSocksUsername(Neodymium.configuration().getProxySocketUsername());
                webdriverProxy.setSocksPassword(Neodymium.configuration().getProxySocketPassword());
            }
            if (Neodymium.configuration().getProxySocketVersion() != null)
            {
                webdriverProxy.setSocksVersion(4);
            }
        }

        webdriverProxy.setNoProxy(Neodymium.configuration().getProxyBypass());
        return webdriverProxy;
    }

    /**
     * Gets popular content types from the jdk content-types.properties file. In case the file is not found returns:
     * </br>
     * <em>text/plain;text/csv;application/zip;application/pdf;
     * application/octet-stream;application/msword;application/vnd.ms-excel;text/css;text/html</em>
     * 
     * @return popular content types
     */
    private static String popularContentTypes()
    {
        try
        {
            List<String> popularContentTypes = IOUtils.readLines(BrowserConfigurationMapper.class.getResourceAsStream("/content-types.properties"), UTF_8);
            popularContentTypes.add("application/x-download");
            return String.join(";", popularContentTypes);
        }
        catch (Exception e)
        {
            return "text/plain;text/csv;application/zip;application/pdf;application/octet-stream;" +
                   "application/msword;application/vnd.ms-excel;text/css;text/html";
        }
    }
}
