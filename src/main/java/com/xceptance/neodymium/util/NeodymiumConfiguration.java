package com.xceptance.neodymium.util;

import com.xceptance.neodymium.junit4.NeodymiumRunner.DescriptionMode;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.Mutable;

@LoadPolicy(LoadType.MERGE)
@Sources(
{
  "system:properties",
  "${neodymium.temporaryConfigFile}",
  "file:config/dev-neodymium.properties",
  "system:env",
  "file:config/credentials.properties",
  "file:config/neodymium.properties"
})
public interface NeodymiumConfiguration extends Mutable
{
    @Key("neodymium.debugUtils.highlight")
    @DefaultValue("false")
    public boolean debuggingHighlightSelectedElements();

    @Key("neodymium.debugUtils.highlight.duration")
    @DefaultValue("100")
    public long debuggingHighlightDuration();

    // standard timeout for selenide interaction
    @Key("neodymium.selenide.timeout")
    @DefaultValue("3000")
    public long selenideTimeout();

    @Key("neodymium.selenide.fastSetValue")
    @DefaultValue("false")
    public boolean selenideFastSetValue();

    @Key("neodymium.selenide.clickViaJs")
    @DefaultValue("false")
    public boolean selenideClickViaJs();

    @Key("neodymium.selenideAddons.staleElement.retry.count")
    @DefaultValue("3")
    public int staleElementRetryCount();

    @Key("neodymium.selenideAddons.staleElement.retry.timeout")
    @DefaultValue("500")
    public long staleElementRetryTimeout();

    @Key("neodymium.selenideAddons.optional.retry.pollingIntervall")
    @DefaultValue("3000")
    public long optionalElementRetryPollingIntervall();

    @Key("neodymium.selenideAddons.optional.retry.timeout")
    @DefaultValue("30000")
    public long optionalElementRetryTimeout();

    @Key("neodymium.javaScriptUtils.timeout")
    @DefaultValue("2000")
    public long javaScriptTimeout();

    @Key("neodymium.javaScriptUtils.pollingInterval")
    @DefaultValue("200")
    public long javaScriptPollingInterval();

    @Key("neodymium.javaScriptUtils.loading.jQueryIsRequired")
    @DefaultValue("true")
    public boolean javascriptLoadingJQueryIsRequired();

    @Key("neodymium.javaScriptUtils.loading.animationSelector")
    public String javascriptLoadingAnimationSelector();

    @Key("neodymium.url")
    public String url();

    @Key("neodymium.url.protocol")
    public String protocol();

    @Key("neodymium.url.host")
    public String host();

    @Key("neodymium.url.path")
    public String path();

    @Key("neodymium.url.path")
    @DisableFeature(
    {
      DisableableFeature.VARIABLE_EXPANSION
    })
    public String rawPath();

    @Key("neodymium.url.site")
    public String site();

    @Key("neodymium.basicauth.username")
    public String basicAuthUsername();

    @Key("neodymium.basicauth.password")
    public String basicAuthPassword();

    @Key("neodymium.locale")
    @DefaultValue("en_US")
    public String locale();

    @Key("neodymium.localization.file")
    @DefaultValue("config/localization.yaml")
    public String localizationFile();

    @Key("neodymium.allureAddons.screenshots.perstep.always")
    @DefaultValue("false")
    public boolean screenshotPerStep();

    @Key("neodymium.allureAddons.reports.path")
    @DefaultValue("/build/reports/tests/")
    public String reportsPath();

    @Key("neodymium.context.device.breakpoint.small")
    @DefaultValue("576")
    public int smallDeviceBreakpoint();

    @Key("neodymium.context.device.breakpoint.medium")
    @DefaultValue("768")
    public int mediumDeviceBreakpoint();

    @Key("neodymium.context.device.breakpoint.large")
    @DefaultValue("992")
    public int largeDeviceBreakpoint();

    @Key("neodymium.context.device.breakpoint.xlarge")
    @DefaultValue("1200")
    public int xlargeDeviceBreakpoint();

    @Key("neodymium.context.random.initialValue")
    public Long initialRandomValue();

    @Key("neodymium.dataUtils.email.domain")
    @DefaultValue("varmail.de")
    @Deprecated
    public String dataUtilsEmailDomain();

    @Key("neodymium.dataUtils.email.local.prefix")
    @DefaultValue("test")
    @Deprecated
    public String dataUtilsEmailLocalPrefix();

    @Key("neodymium.dataUtils.email.randomCharsAmount")
    @DefaultValue("12")
    @Deprecated
    public int dataUtilsEmailRandomCharsAmount();

    @Key("neodymium.dataUtils.password.uppercaseCharAmount")
    @DefaultValue("2")
    @Deprecated
    public int dataUtilsPasswordUppercaseCharAmount();

    @Key("neodymium.dataUtils.password.lowercaseCharAmount")
    @DefaultValue("5")
    @Deprecated
    public int dataUtilsPasswordLowercaseCharAmount();

    @Key("neodymium.dataUtils.password.digitAmount")
    @DefaultValue("2")
    @Deprecated
    public int dataUtilsPasswordDigitAmount();

    @Key("neodymium.dataUtils.password.specialCharAmount")
    @DefaultValue("2")
    @Deprecated
    public int dataUtilsPasswordSpecialCharAmount();

    @Key("neodymium.dataUtils.password.specialChars")
    @DefaultValue("+-#$%&.;,_")
    @Deprecated
    public String dataUtilsPasswordSpecialChars();

    @Key("neodymium.testData.email.domain")
    @DefaultValue("${neodymium.dataUtils.email.domain}")
    public String testDataEmailDomain();

    @Key("neodymium.testData.email.local.prefix")
    @DefaultValue("${neodymium.dataUtils.email.local.prefix}")
    public String testDataEmailLocalPrefix();

    @Key("neodymium.testData.email.randomCharsAmount")
    @DefaultValue("${neodymium.dataUtils.email.randomCharsAmount}")
    public int testDataEmailRandomCharsAmount();

    @Key("neodymium.testData.password.uppercaseCharAmount")
    @DefaultValue("${neodymium.dataUtils.password.uppercaseCharAmount}")
    public int testDataPasswordUppercaseCharAmount();

    @Key("neodymium.testData.password.lowercaseCharAmount")
    @DefaultValue("${neodymium.dataUtils.password.lowercaseCharAmount}")
    public int testDataPasswordLowercaseCharAmount();

    @Key("neodymium.testData.password.digitAmount")
    @DefaultValue("${neodymium.dataUtils.password.digitAmount}")
    public int testDataPasswordDigitAmount();

    @Key("neodymium.testData.password.specialCharAmount")
    @DefaultValue("${neodymium.dataUtils.password.specialCharAmount}")
    public int testDataPasswordSpecialCharAmount();

    @Key("neodymium.testData.password.specialChars")
    @DefaultValue("${neodymium.dataUtils.password.specialChars}")
    public String testDataPasswordSpecialChars();

    @Key("neodymium.junit.viewmode")
    @DefaultValue("tree")
    public DescriptionMode junitViewMode();

    @Key("neodymium.proxy")
    @DefaultValue("false")
    public boolean useProxy();

    @Key("neodymium.proxy.host")
    public String getProxyHost();

    @Key("neodymium.proxy.port")
    public String getProxyPort();

    @Key("neodymium.proxy.bypassForHosts")
    public String getProxyBypass();

    @Key("neodymium.proxy.socket.version")
    public Integer getProxySocketVersion();

    @Key("neodymium.proxy.socket.userName")
    public String getProxySocketUsername();

    @Key("neodymium.proxy.socket.password")
    public String getProxySocketPassword();

    @Key("neodymium.selenideProxy")
    @DefaultValue("false")
    public boolean enableSelenideProxy();

    @Key("neodymium.localproxy")
    @DefaultValue("false")
    public boolean useLocalProxy();

    @Key("neodymium.localproxy.certificate")
    @DefaultValue("false")
    public boolean useLocalProxyWithSelfSignedCertificate();

    @Key("neodymium.localproxy.certificate.generate")
    @DefaultValue("true")
    public boolean localProxyGenerateSelfSignedCertificate();

    @Key("neodymium.localproxy.certificate.archiveFile")
    @DefaultValue("./config/Certificates.p12")
    public String localProxyCertificateArchiveFile();

    @Key("neodymium.localproxy.certificate.archivetype")
    @DefaultValue("PKCS12")
    public String localProxyCertificateArchiveType();

    @Key("neodymium.localproxy.certificate.name")
    public String localProxyCertificateName();

    @Key("neodymium.localproxy.certificate.password")
    public String localProxyCertificatePassword();

    @Key("neodymium.webDriver.window.width")
    @DefaultValue("-1")
    public Integer getWindowWidth();

    @Key("neodymium.webDriver.window.height")
    @DefaultValue("-1")
    public Integer getWindowHeight();

    @Key("neodymium.webDriver.reuseDriver")
    @DefaultValue("false")
    public boolean reuseWebDriver();

    @Key("neodymium.webDriver.maxReuse")
    @DefaultValue("-1")
    public int maxWebDriverReuse();

    @Key("neodymium.webDriver.browserFilter")
    @DefaultValue("")
    public String getBrowserFilter();

    @Key("neodymium.webDriver.startNewBrowserForSetUp")
    @DefaultValue("true")
    public boolean startNewBrowserForSetUp();

    @Key("neodymium.webDriver.startNewBrowserForCleanUp")
    @DefaultValue("true")
    public boolean startNewBrowserForCleanUp();

    @Key("neodymium.webDriver.keepBrowserOpen")
    @DefaultValue("false")
    public boolean keepBrowserOpen();

    @Key("neodymium.webDriver.keepBrowserOpenOnFailure")
    @DefaultValue("false")
    public boolean keepBrowserOpenOnFailure();

    @Key("neodymium.webDriver.chrome.pathToDriverServer")
    public String getChromeDriverPath();

    @Key("neodymium.webDriver.chrome.driverArguments")
    @DefaultValue("")
    public String getChromeDriverArguments();

    @Key("neodymium.webDriver.firefox.pathToDriverServer")
    public String getFirefoxDriverPath();

    @Key("neodymium.webDriver.firefox.driverArguments")
    @DefaultValue("")
    public String getFirefoxDriverArguments();

    @Key("neodymium.webDriver.ie.pathToDriverServer")
    public String getIeDriverPath();

    @Key("neodymium.webDriver.ie.driverArguments")
    @DefaultValue("")
    public String getIeDriverArguments();

    @Key("neodymium.webDriver.edge.pathToDriverServer")
    public String getEdgeDriverPath();

    @Key("neodymium.webDriver.edge.driverArguments")
    @DefaultValue("")
    public String getEdgeDriverArguments();

    @Key("neodymium.webDriver.safari.driverArguments")
    @DefaultValue("")
    public String getSafariDriverArguments();

    @Key("neodymium.webDriver.chrome.pathToBrowser")
    public String getChromeBrowserPath();

    @Key("neodymium.webDriver.firefox.pathToBrowser")
    public String getFirefoxBrowserPath();

    @Key("neodymium.testNameFilter")
    public String getTestNameFilter();

    @Key("neodymium.workInProgress")
    @DefaultValue("false")
    public boolean workInProgress();

    @Key("neodymium.screenshots.enableOnSuccess")
    @DefaultValue("false")
    public boolean enableOnSuccess();

    @Key("neodymium.screenshots.viewport.enable")
    @DefaultValue("true")
    public boolean enableViewportScreenshot();

    @Key("neodymium.screenshots.fullpagecapture.enable")
    @DefaultValue("false")
    public boolean enableFullPageCapture();

    @Key("neodymium.screenshots.fullpagecapture.highlightViewport")
    @DefaultValue("false")
    public boolean enableHighlightViewport();

    @Key("neodymium.screenshots.enableTreeDirectoryStructure")
    @DefaultValue("false")
    public boolean enableTreeDirectoryStructure();

    @Key("neodymium.screenshots.fullpagecapture.highlightColor")
    @DefaultValue("#FF0000")
    public String fullScreenHighlightColor();

    @Key("neodymium.screenshots.highlightLastElement")
    @DefaultValue("false")
    public boolean enableHighlightLastElement();

    @Key("neodymium.screenshots.enableAdvancedScreenshots")
    @DefaultValue("false")
    public boolean enableAdvancedScreenShots();

    @Key("neodymium.screenshots.element.highlightColor")
    @DefaultValue("#FF00FF")
    public String screenshotElementHighlightColor();

    @Key("neodymium.screenshots.highlightLineThickness")
    @DefaultValue("4")
    public int screenshotHighlightLineThickness();

    @Key("neodymium.screenshots.blurFullPageScreenshot")
    @DefaultValue("false")
    public boolean blurFullPageScreenshot();

    @Key("neodymium.logNeoVersion")
    @DefaultValue("true")
    public boolean logNeoVersion();

    @Key("neodymium.lighthouse.binaryPath")
    @DefaultValue("lighthouse")
    public String lighthouseBinaryPath();

    @Key("neodymium.lighthouse.assert.thresholdScore.performance")
    @DefaultValue("0.5")
    public double lighthouseAssertPerformance();

    @Key("neodymium.lighthouse.assert.thresholdScore.accessibility")
    @DefaultValue("0.5")
    public double lighthouseAssertAccessibility();

    @Key("neodymium.lighthouse.assert.thresholdScore.bestPractices")
    @DefaultValue("0.5")
    public double lighthouseAssertBestPractices();

    @Key("neodymium.lighthouse.assert.thresholdScore.seo")
    @DefaultValue("0.5")
    public double lighthouseAssertSeo();

    @Key("neodymium.lighthouse.assert.audits")
    public String lighthouseAssertAudits();

    @Key("neodymium.report.showSelenideErrorDetails")
    @DefaultValue("false")
    public boolean showSelenideErrorDetails();

    @Key("neodymium.report.enableTestDataInReport")
    @DefaultValue("true")
    public boolean addTestDataToReport();

    @Key("neodymium.report.environment.enableCustomData")
    @DefaultValue("true")
    public boolean enableCustomEnvironmentData();

    @Key("neodymium.report.environment.enableBrowserData")
    @DefaultValue("true")
    public boolean enableBrowserEnvironmentData();

    @Key("neodymium.report.enableStepLinks")
    @DefaultValue("true")
    public boolean enableStepLinks();

    @Key("neodymium.url.excludeList")
    @DefaultValue("")
    public String getExcludeList();

    @Key("neodymium.url.includeList")
    @DefaultValue("")
    public String getIncludeList();

    @Key("neodymium.popupInterval")
    @DefaultValue("1000")
    public int getPopupBlockerInterval();

    @Key("neodymium.seleniumLogLevel")
    @DefaultValue("SEVERE")
    public String seleniumLogLevel();
}
