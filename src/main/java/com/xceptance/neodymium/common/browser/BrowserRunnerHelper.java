package com.xceptance.neodymium.common.browser;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.GeckoDriverService.Builder;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.os.ExecutableFinder;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.support.events.EventFiringDecorator;

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
import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.BrowserConfigurationMapper;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.TestEnvironment;
import com.xceptance.neodymium.common.browser.wrappers.ChromeBuilder;
import com.xceptance.neodymium.common.browser.wrappers.EdgeBuilder;
import com.xceptance.neodymium.common.browser.wrappers.GeckoBuilder;
import com.xceptance.neodymium.common.browser.wrappers.IEBuilder;
import com.xceptance.neodymium.common.browser.wrappers.SafariBuilder;
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

    private static List<String> edgeBrowsers = new LinkedList<String>();

    private static List<String> safariBrowsers = new LinkedList<String>();

    private final static Object mutex = new Object();

    static
    {
        chromeBrowsers.add(CHROME.browserName());

        firefoxBrowsers.add(FIREFOX.browserName());

        internetExplorerBrowsers.add(IE.browserName());
        edgeBrowsers.add(EDGE.browserName());

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
     * Instantiate the {@link WebDriver} according to the configuration read from {@link Browser} annotations.
     * 
     * @param config
     *            {@link BrowserConfiguration} that describes the desired browser instance
     * @param testClassInstance
     *            {@link String} name of the test to display on test environment
     * @return {@link WebDriverStateContainer} the instance of the browser described in {@link BrowserConfiguration} and
     *         in {@link NeodymiumConfiguration}
     * @throws MalformedURLException
     *             if <a href="https://github.com/Xceptance/neodymium/wiki/Selenium-grid">Selenium grid</a> is used and
     *             the given grid URL is invalid
     */
    public static WebDriverStateContainer createWebDriverStateContainer(final BrowserConfiguration config, final String testClassInstance)
        throws MalformedURLException
    {
        Logger.getLogger("org.openqa.selenium").setLevel(Level.parse(Neodymium.configuration().seleniumLogLevel()));

        final MutableCapabilities capabilities = config.getCapabilities();
        final WebDriverStateContainer wDSC = new WebDriverStateContainer();

        // set up proxy
        SelenideProxyServer selenideProxyServer = null;
        if (Neodymium.configuration().useLocalProxy())
        {
            final BrowserUpProxy proxy = setupEmbeddedProxy();

            // set the Proxy for later usage
            wDSC.setProxy(proxy);
            // configure the proxy via capabilities
            capabilities.setCapability(CapabilityType.PROXY, ClientUtil.createSeleniumProxy(proxy));
        }
        else if (Neodymium.configuration().useProxy())
        {
            capabilities.setCapability(CapabilityType.PROXY, createProxyCapabilities());
        }
        else if (Neodymium.configuration().enableSelenideProxy())
        {
            final SelenideProxyServerFactory selenideProxyServerFactory = Plugins.inject(SelenideProxyServerFactory.class);
            selenideProxyServer = selenideProxyServerFactory.create(new SelenideConfig(),
                                                                    (Proxy) capabilities.getCapability(CapabilityType.PROXY));
            final Proxy proxy = selenideProxyServer.getSeleniumProxy();
            capabilities.setCapability(CapabilityType.PROXY, proxy);
        }
        else
        {
            // no proxy should be used, so set the proxy to null
            // makes it possible to run some test with temp proxy and some without during a single test run
            capabilities.setCapability(CapabilityType.PROXY, (Object) null);
        }

        final String testEnvironment = config.getTestEnvironment();
        if (StringUtils.isEmpty(testEnvironment) || "local".equalsIgnoreCase(testEnvironment))
        {
            final String browserName = capabilities.getBrowserName();
            if (chromeBrowsers.contains(browserName))
            {
                final ChromeOptions options = new ChromeOptions();
                final String driverInPathPath = new ExecutableFinder().find("chromedriver");

                // do we have a custom path?
                String pathToBrowser = Neodymium.configuration().getChromeBrowserPath();
                if (StringUtils.isNotBlank(pathToBrowser))
                {
                    options.setBinary(pathToBrowser);
                }
                if (config.isHeadless())
                {
                    options.addArguments("--headless");
                }

                // find a free port for each chrome session (important for lighthouse)
                int remoteDebuggingPort = PortProber.findFreePort();
                Neodymium.setRemoteDebuggingPort(remoteDebuggingPort);
                options.addArguments("--remote-debugging-port=" + remoteDebuggingPort);

                if (config.getArguments() != null && config.getArguments().size() > 0)
                {
                    options.addArguments(config.getArguments());
                }

                if (config.getPreferences() != null && !config.getPreferences().isEmpty())
                {
                    options.setExperimentalOption("prefs", config.getPreferences());
                }

                if ((config.getPreferences() != null && !config.getPreferences().isEmpty()) ||
                    StringUtils.isNotBlank(config.getDownloadDirectory()))
                {
                    final HashMap<String, Object> prefs = new HashMap<>();

                    // if we have configured prefs, we need to add all to the experimental options
                    if (config.getPreferences() != null && !config.getPreferences().isEmpty())
                    {
                        prefs.putAll(config.getPreferences());
                    }
                    // if we have configured a download folder separately, it'll override the general config
                    if (StringUtils.isNotBlank(config.getDownloadDirectory()))
                    {
                        prefs.put("download.default_directory", config.getDownloadDirectory());
                        prefs.put("plugins.always_open_pdf_externally", true);
                    }

                    options.setExperimentalOption("prefs", prefs);
                }

                ChromeBuilder chromeBuilder = new ChromeBuilder(config.getDriverArguments());
                if (StringUtils.isNotBlank(driverInPathPath))
                {
                    chromeBuilder.usingDriverExecutable(new File(driverInPathPath));
                }

                wDSC.setWebDriver(new ChromeDriver(chromeBuilder.build(), options.merge(capabilities)));
            }
            else if (firefoxBrowsers.contains(browserName))
            {
                final FirefoxOptions options = new FirefoxOptions();
                new ExecutableFinder().find("firefox");
                final String driverInPathPath = new ExecutableFinder().find("geckodriver");

                if (StringUtils.isNotBlank(Neodymium.configuration().getFirefoxBrowserPath()))
                {
                    options.setBinary(new File(Neodymium.configuration().getFirefoxBrowserPath()).toPath());
                }
                else
                {
                    options.setBinary(new ExecutableFinder().find("firefox"));
                }
                if (config.isHeadless())
                {
                    options.addArguments("--headless");
                }
                if (config.getArguments() != null && config.getArguments().size() > 0)
                {
                    options.addArguments(config.getArguments());
                }
                if ((config.getPreferences() != null && !config.getPreferences().isEmpty()) ||
                    StringUtils.isNotBlank(config.getDownloadDirectory()))
                {
                    final FirefoxProfile profile = new FirefoxProfile();

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

				Builder geckoBuilder = null;
				final List<String> driverArguments = config.getDriverArguments();
				if (driverArguments != null) {
					int webSocketPort;
					final List<String> wsPorts = driverArguments.stream()
							.filter(arg -> arg.contains("--websocket-port=")).collect(Collectors.toList());
					if (!wsPorts.isEmpty()) {
						webSocketPort = Integer
								.parseInt(wsPorts.get(wsPorts.size() - 1).replace("--websocket-port=", ""));
						driverArguments.remove(driverArguments.stream().filter(arg -> arg.contains("--websocket-port="))
								.findFirst().get());
						geckoBuilder = new GeckoBuilder(driverArguments).withAllowHosts("localhost")
								.withWebSocketPort(webSocketPort);
					} else {
						geckoBuilder = new GeckoBuilder(driverArguments).withAllowHosts("localhost");
					}
				} else {
					geckoBuilder = new GeckoBuilder(null).withAllowHosts("localhost");
				}

				if (StringUtils.isNotBlank(driverInPathPath)) {
					geckoBuilder.usingDriverExecutable(new File(driverInPathPath));
				}

                wDSC.setWebDriver(new FirefoxDriver(geckoBuilder.build(), options.merge(capabilities)));
            }
            else if (internetExplorerBrowsers.contains(browserName))
            {
                final InternetExplorerOptions options = new InternetExplorerOptions();
                final String driverInPathPath = new ExecutableFinder().find("IEDriverServer");
                if (config.getArguments() != null && config.getArguments().size() > 0)
                {
                    for (final String argument : config.getArguments())
                    {
                        options.addCommandSwitches(argument);
                    }
                }
                IEBuilder ieBuilder = new IEBuilder(config.getDriverArguments());
                if (StringUtils.isNotBlank(driverInPathPath))
                {
                    ieBuilder.usingDriverExecutable(new File(driverInPathPath));
                }

                wDSC.setWebDriver(new InternetExplorerDriver(ieBuilder.build(), options.merge(capabilities)));
            }
            else if (edgeBrowsers.contains(browserName))
            {

                final String driverInPathPath = new ExecutableFinder().find("msedgedriver");
                final EdgeOptions options = new EdgeOptions().merge(capabilities);

                if (config.getArguments() != null && config.getArguments().size() > 0)
                {
                    options.addArguments(config.getArguments());
                }

                EdgeBuilder edgeBuilder = new EdgeBuilder(config.getDriverArguments());
                if (StringUtils.isNotBlank(driverInPathPath))
                {
                    edgeBuilder.usingDriverExecutable(new File(driverInPathPath));
                }

                wDSC.setWebDriver(new EdgeDriver(edgeBuilder.build(), options));
            }
            else if (safariBrowsers.contains(browserName))
            {
                // safari driver is not expected to be in PATH, it will be looked in
                // /usr/bin/safaridriver and /Applications/Safari Technology Preview.app/Contents/MacOS/safaridriver
                final SafariOptions options = new SafariOptions();
                wDSC.setWebDriver(new SafariDriver(new SafariBuilder(config.getDriverArguments())
                                                                                                 .build(), options));
            }
            else
            {
                wDSC.setWebDriver(new RemoteWebDriver(capabilities.merge(capabilities)));
            }

        }
        else
        {

            // establish connection to target website
            final TestEnvironment testEnvironmentProperties = MultibrowserConfiguration.getInstance().getTestEnvironment(testEnvironment);
            if (testEnvironmentProperties == null)
            {
                throw new IllegalArgumentException("No properties found for test environment: \"" + testEnvironment + "\"");
            }
            final String testEnvironmentUrl = testEnvironmentProperties.getUrl();
            ClientConfig configClient = ClientConfig.defaultConfig();
            configClient = configClient.baseUrl(new URL(testEnvironmentUrl));
            config.getGridProperties().put("userName", testEnvironmentProperties.getUsername());
            config.getGridProperties().put("accessKey", testEnvironmentProperties.getPassword());
            final String buildId = StringUtils.isBlank(System.getenv("BUILD_NUMBER")) ? "local run" : System.getenv("BUILD_NUMBER");
            config.getGridProperties().put("sessionName", testClassInstance);
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
                final String optionsTag = testEnvironmentProperties.getOptionsTag();
                if (StringUtils.isBlank(optionsTag))
                {
                    for (final String key : config.getGridProperties().keySet())
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
        final WebDriver decoratedDriver = new EventFiringDecorator<WebDriver>(new NeodymiumWebDriverListener()).decorate(wDSC.getWebDriver());
        wDSC.setDecoratedWebDriver(decoratedDriver);
        WebDriverRunner.setWebDriver(decoratedDriver, selenideProxyServer);
        return wDSC;
    }

    private static BrowserUpProxy setupEmbeddedProxy()
    {
        // instantiate the proxy
        final BrowserUpProxy proxy = new BrowserUpProxyServer();

        if (Neodymium.configuration().useLocalProxyWithSelfSignedCertificate())
        {
            final CertificateAndKeySource rootCertificateSource = createLocalProxyRootCertSource();
            final ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder().rootCertificateSource(rootCertificateSource)
                                                                                 .build();
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
        if (!StringUtils.isAllEmpty(Neodymium.configuration().getProxySocketUsername(),
                                    Neodymium.configuration().getProxySocketPassword())
            ||
            Neodymium.configuration().getProxySocketVersion() != null)
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
            final List<String> popularContentTypes = IOUtils.readLines(BrowserConfigurationMapper.class.getResourceAsStream("/content-types.properties"),
                                                                       UTF_8);
            popularContentTypes.add("application/x-download");
            return String.join(";", popularContentTypes);
        }
        catch (final Exception e)
        {
            return "text/plain;text/csv;application/zip;application/pdf;application/octet-stream;" +
                   "application/msword;application/vnd.ms-excel;text/css;text/html";
        }
    }
}
