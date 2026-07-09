/*
 * Copyright (c) 2017-2026 Xceptance Software Technologies GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.util.layer.selenide;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chromium.HasCdp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.AuthenticationType;
import com.codeborne.selenide.BasicAuthCredentials;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.logevents.LogEventListener;
import com.codeborne.selenide.logevents.SelenideLogger;
import com.xceptance.neodymium.common.TestStepListener;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.layer.BrowserFacade;
import com.xceptance.neodymium.util.layer.FoundElement;
import io.qameta.allure.selenide.AllureSelenide;

/**
 * Selenide-backed implementation of {@link BrowserFacade}.
 * <p>
 * This class is the <strong>only permitted location</strong> for Selenide/Selenium/WebDriver
 * imports related to browser-level operations. Replace this class with a Playwright
 * implementation to swap driver backends without touching any callers.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SelenideBrowserFacade implements BrowserFacade
{
    private static final Logger LOG = LoggerFactory.getLogger(SelenideBrowserFacade.class);
    /**
     * Default constructor.
     */
    public SelenideBrowserFacade()
    {
        final AllureSelenide allureSelenide = new AllureSelenide();

        // if advanced screenshots are enabled, Selenide screenshots should be disabled
        allureSelenide.screenshots(!Neodymium.configuration().enableAdvancedScreenShots());

        SelenideLogger.addListener("allure-selenide-java", allureSelenide);
        SelenideLogger.addListener(TestStepListener.LISTENER_NAME, new TestStepListener());
    }

    /** {@inheritDoc} */
    @Override
    public void open(final String url)
    {
        Selenide.open(url);
    }

    /** {@inheritDoc} */
    @Override
    public void openWithBasicAuth(final String url, final String username, final String password)
    {
        // For Chrome/Chromium: set a preemptive Authorization header via the CDP Network domain.
        // Selenide's built-in mechanism uses the Fetch.authRequired CDP handler, which is
        // challenge-response only (it fires only when the server returns a 401). Sites that
        // enforce preemptive auth (they check the Authorization header on the very first
        // request and return 403 or redirect instead of 401) never trigger that handler.
        // The Network.setExtraHTTPHeaders command sends the header on every subsequent request
        // to any domain, covering both preemptive and challenge-response scenarios.
        if (WebDriverRunner.hasWebDriverStarted())
        {
            final WebDriver driver = WebDriverRunner.getWebDriver();
            if (driver instanceof HasCdp cdpDriver)
            {
                try
                {
                    final String encoded = Base64.getEncoder()
                        .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
                    cdpDriver.executeCdpCommand("Network.enable", Map.of());
                    final Map<String, Object> headers = new HashMap<>();
                    headers.put("Authorization", "Basic " + encoded);
                    final Map<String, Object> params = new HashMap<>();
                    params.put("headers", headers);
                    cdpDriver.executeCdpCommand("Network.setExtraHTTPHeaders", params);
                    LOG.debug("Preemptive Basic Auth header set via CDP for: {}", url);
                }
                catch (final Exception e)
                {
                    LOG.warn("Could not set preemptive Basic Auth header via CDP; "
                        + "falling back to challenge-response only: {}", e.getMessage());
                }
            }
        }

        // Selenide's built-in open with BASIC auth:
        // - Chrome/Chromium: registers a CDP Fetch.authRequired handler (challenge-response)
        // - Firefox: prepends credentials to the URL (preemptive via URL encoding)
        Selenide.open(url, AuthenticationType.BASIC, new BasicAuthCredentials(username, password));
    }

    /** {@inheritDoc} */
    @Override
    public void back()
    {
        Selenide.back();
    }

    /** {@inheritDoc} */
    @Override
    public void forward()
    {
        Selenide.forward();
    }

    /** {@inheritDoc} */
    @Override
    public void refresh()
    {
        Selenide.refresh();
    }

    /** {@inheritDoc} */
    @Override
    public void sleep(final long milliseconds)
    {
        Selenide.sleep(milliseconds);
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrentUrl()
    {
        return WebDriverRunner.url();
    }

    /** {@inheritDoc} */
    @Override
    public String getPageTitle()
    {
        return Selenide.title();
    }

    /** {@inheritDoc} */
    @Override
    public String getWindowHandle()
    {
        return WebDriverRunner.getWebDriver().getWindowHandle();
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getWindowHandles()
    {
        return WebDriverRunner.getWebDriver().getWindowHandles();
    }

    /** {@inheritDoc} */
    @Override
    public void switchToWindow(final String handle)
    {
        Selenide.switchTo().window(handle);
    }

    /** {@inheritDoc} */
    @Override
    public void switchToFrame(final String selector)
    {
        Selenide.switchTo().frame(Selenide.$(selector));
    }

    /** {@inheritDoc} */
    @Override
    public void switchToFrame(final int index)
    {
        Selenide.switchTo().frame(index);
    }

    /** {@inheritDoc} */
    @Override
    public void switchToParentFrame()
    {
        Selenide.switchTo().parentFrame();
    }

    /** {@inheritDoc} */
    @Override
    public void switchToDefaultContent()
    {
        WebDriverRunner.getWebDriver().switchTo().defaultContent();
    }

    /** {@inheritDoc} */
    @Override
    public void sendKeysToActiveElement(final CharSequence key)
    {
        WebDriverRunner.getWebDriver().switchTo().activeElement().sendKeys(key);
    }

    /** {@inheritDoc} */
    @Override
    public void openNewTab()
    {
        WebDriverRunner.getWebDriver().switchTo().newWindow(WindowType.TAB);
    }

    /** {@inheritDoc} */
    @Override
    public void openNewWindow()
    {
        WebDriverRunner.getWebDriver().switchTo().newWindow(WindowType.WINDOW);
    }

    /** {@inheritDoc} */
    @Override
    public void closeCurrentWindow()
    {
        WebDriverRunner.getWebDriver().close();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasWebDriverStarted()
    {
        return WebDriverRunner.hasWebDriverStarted();
    }

    /** {@inheritDoc} */
    @Override
    public void clearBrowserCookies()
    {
        Selenide.clearBrowserCookies();
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeJavaScript(final String script, final Object... args)
    {
        return Selenide.executeJavaScript(script, args);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T executeAsyncJavaScript(final String script, final Object... args)
    {
        return Selenide.executeAsyncJavaScript(script, args);
    }

    /** {@inheritDoc} */
    @Override
    public void waitUntilDocumentReady()
    {
        Selenide.Wait().until(d -> "complete".equals(Selenide.executeJavaScript("return document.readyState")));
    }

    /** {@inheritDoc} */
    @Override
    public long getTimeout()
    {
        return Configuration.timeout;
    }

    /** {@inheritDoc} */
    @Override
    public void addDriverLogger(final String listenerId, final Object listener)
    {
        if (listener instanceof LogEventListener logEventListener)
        {
            SelenideLogger.addListener(listenerId, logEventListener);
        }
        else
        {
            throw new IllegalArgumentException(
                "Listener must be an instance of com.codeborne.selenide.logevents.LogEventListener for the Selenide backend, got: "
                    + (listener == null ? "null" : listener.getClass().getName()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeDriverLogger(final String listenerId)
    {
        SelenideLogger.removeListener(listenerId);
    }

    /** {@inheritDoc} */
    @Override
    public void wrapAssertionError(final Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (final UIAssertionError e)
        {
            throw e;
        }
        catch (final AssertionError e)
        {
            // Wrap the assertion error so it is properly surfaced as a Selenide UI assertion failure.
            // UIAssertionError(String message, long timeout, Throwable cause) is the available constructor.
            throw new UIAssertionError(e.getMessage(), 0L, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void safeExecute(final Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (final Throwable ignored)
        {
            // Intentionally suppress — best-effort operation (e.g. debug highlights)
        }
    }

    /** {@inheritDoc} */
    @Override
    public java.util.Map<String, Object> getAXTree()
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        if (driver instanceof HasCdp cdpDriver)
        {
            return cdpDriver.executeCdpCommand("Accessibility.getFullAXTree", java.util.Map.of());
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public java.util.Map<String, Object> executeCdpCommand(final String command, final java.util.Map<String, Object> params)
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        if (driver instanceof HasCdp cdpDriver)
        {
            return cdpDriver.executeCdpCommand(command, params);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void close()
    {
        Selenide.closeWebDriver();
    }

    /** {@inheritDoc} */
    @Override
    public FoundElement findFirst(final By locator)
    {
        return new SelenideFoundElement(Selenide.$(locator));
    }

    /** {@inheritDoc} */
    @Override
    public List<FoundElement> findElements(final By locator)
    {
        final com.codeborne.selenide.ElementsCollection all = Selenide.$$(locator);
        final List<FoundElement> result = new ArrayList<>(all.size());
        for (final com.codeborne.selenide.SelenideElement el : all)
        {
            result.add(new SelenideFoundElement(el));
        }
        return result;
    }
}
