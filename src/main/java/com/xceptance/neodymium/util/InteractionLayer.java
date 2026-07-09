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
package com.xceptance.neodymium.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.proxy.SelenideProxyServer;
import com.xceptance.neodymium.util.layer.AssertionFacade;
import com.xceptance.neodymium.util.layer.BrowserFacade;
import com.xceptance.neodymium.util.layer.AiPromptFacade;
import com.xceptance.neodymium.util.layer.FoundElement;
import com.xceptance.neodymium.util.layer.ScreenshotFacade;
import com.xceptance.neodymium.util.layer.selenide.SelenideBrowserFacade;
import com.xceptance.neodymium.util.layer.selenide.SelenideScreenshotFacade;
import com.xceptance.neodymium.ai.core.ContextLevel;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Public entry point for all browser and element interaction within the Neodymium framework.
 * <p>
 * This class delegates to swappable {@link BrowserFacade}, {@link ScreenshotFacade}, and
 * {@link AssertionFacade} implementations. To switch from Selenide to Playwright (or any
 * other driver), replace the facade implementations — no callers need to change.
 * </p>
 * <p>
 * Obtain the current instance via {@link Neodymium#interaction()}.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class InteractionLayer
{
    /** Browser-level operation facade (navigation, window management, JS, logging). */
    private final BrowserFacade browser;

    /** Assertion facade (assertTrue, assertEquals, should). */
    private final AssertionFacade assertions;

    /** Screenshot capture facade (viewport, full-page). */
    private final ScreenshotFacade screenshots;

    /**
     * Creates an InteractionLayer with the configured driver backend.
     */
    public InteractionLayer()
    {
        this(Neodymium.configuration().driverBackend());
    }

    /**
     * Creates an InteractionLayer with the facades for the given backend.
     *
     * @param backend the name of the backend to use
     */
    public InteractionLayer(final String backend)
    {
        this.browser = com.xceptance.neodymium.util.layer.DriverBackendFactory.createBrowserFacade(backend);
        this.assertions = com.xceptance.neodymium.util.layer.DriverBackendFactory.createAssertionFacade(backend);
        this.screenshots = com.xceptance.neodymium.util.layer.DriverBackendFactory.createScreenshotFacade(backend);
    }

    /**
     * Creates an InteractionLayer with custom facade implementations.
     * Use this constructor to inject alternative backends (e.g. Playwright).
     *
     * @param browser     the browser facade implementation to use
     * @param screenshots the screenshot facade implementation to use
     * @param assertions  the assertion facade implementation to use
     */
    public InteractionLayer(final BrowserFacade browser, final ScreenshotFacade screenshots, final AssertionFacade assertions)
    {
        this.browser = browser;
        this.screenshots = screenshots;
        this.assertions = assertions;
    }

    /**
     * Returns the assertion facade for the current interaction layer.
     *
     * @return assertion facade
     */
    public AssertionFacade assertions()
    {
        return assertions;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the given URL in the current browser tab.
     *
     * @param url the URL to open
     */
    public void open(final String url)
    {
        final String username = Neodymium.configuration().basicAuthUsername();
        final String password = Neodymium.configuration().basicAuthPassword();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(username))
        {
            browser.openWithBasicAuth(url, username, password);
        }
        else
        {
            browser.open(url);
        }
    }

    /**
     * Opens the given URL using HTTP Basic Authentication.
     *
     * @param url      the URL to navigate to
     * @param username the basic-auth username
     * @param password the basic-auth password
     */
    public void openWithBasicAuth(final String url, final String username, final String password)
    {
        browser.openWithBasicAuth(url, username, password);
    }

    /**
     * Navigates back in the browser history.
     */
    public void back()
    {
        browser.back();
    }

    /**
     * Navigates forward in the browser history.
     */
    public void forward()
    {
        browser.forward();
    }

    /**
     * Reloads the current page.
     */
    public void refresh()
    {
        browser.refresh();
    }

    /**
     * Blocks until the browser reports {@code document.readyState === "complete"}.
     */
    public void waitUntilDocumentReady()
    {
        browser.waitUntilDocumentReady();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Window / Tab management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the window handle of the currently focused browser window.
     *
     * @return current window handle
     */
    public String getWindowHandle()
    {
        return browser.getWindowHandle();
    }

    /**
     * Returns all open window handles.
     *
     * @return set of window handle strings
     */
    public Set<String> getWindowHandles()
    {
        return browser.getWindowHandles();
    }

    /**
     * Switches focus to the browser window identified by the given handle.
     *
     * @param handle the window handle to switch to
     * @return the WebDriver instance (for backwards compatibility with callers expecting a return value)
     */
    public void switchToWindow(final String handle)
    {
        browser.switchToWindow(handle);
    }

    /**
     * Resets focus to the top-level document, exiting any iframe or frame context.
     */
    public void switchToDefaultContent()
    {
        browser.switchToDefaultContent();
    }

    /**
     * Switches focus to the frame identified by the given CSS selector.
     *
     * @param selector CSS selector of the frame
     */
    public void switchToFrame(final String selector)
    {
        browser.switchToFrame(selector);
    }

    /**
     * Switches focus to the frame identified by the given index.
     *
     * @param index zero-based index of the frame
     */
    public void switchToFrame(final int index)
    {
        browser.switchToFrame(index);
    }

    /**
     * Switches focus to the parent frame of the current frame.
     */
    public void switchToParentFrame()
    {
        browser.switchToParentFrame();
    }

    /**
     * Opens a new blank browser tab and switches focus to it.
     */
    public void openNewTab()
    {
        browser.openNewTab();
    }

    /**
     * Opens a new browser window and switches focus to it.
     */
    public void openNewWindow()
    {
        browser.openNewWindow();
    }

    /**
     * Closes the currently focused browser window or tab.
     */
    public void closeCurrentWindow()
    {
        browser.closeCurrentWindow();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page information
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the URL of the current page.
     *
     * @return current page URL string
     */
    public String getCurrentUrl()
    {
        return browser.getCurrentUrl();
    }

    /**
     * Returns the title of the current page.
     *
     * @return current page title
     */
    public String title()
    {
        return browser.getPageTitle();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Driver state
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if a browser session is currently active.
     *
     * @return {@code true} if a WebDriver has been started
     */
    public boolean hasWebDriverStarted()
    {
        return browser.hasWebDriverStarted();
    }

    /**
     * Retrieves the current WebDriver instance, or {@code null} if no Selenide session is active.
     *
     * <p>
     * When the driver backend is set to {@code desktop}, no Selenide/WebDriver session exists.
     * Calling {@link com.codeborne.selenide.WebDriverRunner#getWebDriver()} in that case throws
     * {@code IllegalStateException}. This method guards against that by returning {@code null}
     * whenever no driver has been started, allowing callers such as
     * {@link com.xceptance.neodymium.common.AllureTestStepListener} to null-check safely.
     * </p>
     *
     * @return the active WebDriver, or {@code null} if no WebDriver session is running
     */
    public WebDriver getDriver()
    {
        if (!hasWebDriverStarted())
        {
            return null;
        }
        try
        {
            return WebDriverRunner.getWebDriver();
        }
        catch (final IllegalStateException e)
        {
            // No Selenide session is bound to this thread — treat as no driver
            return null;
        }
    }

    /**
     * Retrieves the current Selenide Driver instance.
     * <p>
     * This provides access to Selenide internals (e.g., for custom conditions).
     * Prefer higher-level methods where possible.
     * </p>
     *
     * @return the current Selenide {@link Driver}
     */
    public Driver driver()
    {
        return WebDriverRunner.driver();
    }

    /**
     * Sets the current WebDriver (e.g., when a driver is created externally).
     *
     * @param driver the WebDriver to activate
     */
    public void setWebDriver(final WebDriver driver)
    {
        WebDriverRunner.setWebDriver(driver);
    }

    /**
     * Sets the current WebDriver together with a Selenide proxy server.
     *
     * @param driver      the WebDriver to activate
     * @param proxyServer the proxy server to associate with this driver
     */
    public void setWebDriver(final WebDriver driver, final SelenideProxyServer proxyServer)
    {
        WebDriverRunner.setWebDriver(driver, proxyServer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cookies
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Clears all browser cookies for the current session.
     */
    public void clearBrowserCookies()
    {
        browser.clearBrowserCookies();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JavaScript
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Executes synchronous JavaScript in the context of the current page.
     *
     * @param jsCode the JavaScript snippet to execute
     * @param args   optional arguments passed to the script
     * @param <T>    the expected return type
     * @return the result of the JavaScript execution, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T executeJavaScript(final String jsCode, final Object... args)
    {
        return browser.executeJavaScript(jsCode, args);
    }

    /**
     * Executes asynchronous JavaScript. The script must call the last argument
     * (a callback) to signal completion.
     *
     * @param jsCode the JavaScript snippet to execute
     * @param args   optional arguments
     * @param <T>    the expected return type
     * @return the result passed to the callback, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T executeAsyncJavaScript(final String jsCode, final Object... args)
    {
        return browser.executeAsyncJavaScript(jsCode, args);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Keyboard
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sends a key sequence to the currently focused (active) element in the browser.
     * The key name is mapped to the appropriate platform key code internally.
     *
     * @param keyName the key name (e.g., "ENTER", "TAB", "ESCAPE") or a single character
     */
    public void sendKeysToActiveElement(final String keyName)
    {
        browser.sendKeysToActiveElement(mapKeyName(keyName));
    }

    /**
     * Sends a raw key sequence (already resolved to a {@link CharSequence}) to the active element.
     *
     * @param key the key or chord to send
     */
    public void sendKeysToActiveElement(final CharSequence key)
    {
        browser.sendKeysToActiveElement(key);
    }

    /**
     * Maps a human-readable key name (e.g., "ENTER", "TAB", "Ctrl+A") to the
     * driver-specific key sequence.
     * <p>
     * This keeps all {@code org.openqa.selenium.Keys} references inside the layer.
     * </p>
     *
     * @param keyName the key name to map
     * @return the driver key sequence
     */
    public CharSequence mapKeyName(final String keyName)
    {
        if (browser instanceof com.xceptance.neodymium.util.layer.desktop.DesktopBrowserFacade)
        {
            return keyName;
        }
        return com.xceptance.neodymium.util.layer.selenide.KeyMapper.map(keyName);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Timing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pauses the current thread for the given amount of milliseconds.
     *
     * @param milliseconds the sleep duration in milliseconds
     */
    public void sleep(final long milliseconds)
    {
        browser.sleep(milliseconds);
    }

    /**
     * Returns the currently configured implicit wait timeout in milliseconds.
     *
     * @return timeout in milliseconds
     */
    public long getTimeout()
    {
        return browser.getTimeout();
    }

    /**
     * Returns whether Selenide is configured to capture screenshots automatically.
     * This mirrors {@code Configuration.config().screenshots()} without requiring callers
     * to import Selenide's {@link Configuration} class.
     *
     * @return {@code true} if Selenide's automatic screenshot capture is enabled
     */
    public boolean isSelenideScreenshotsEnabled()
    {
        return Configuration.config().screenshots();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Error handling
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Wraps a runnable such that any assertion error thrown is re-thrown
     * in a framework-appropriate manner (e.g. Selenide UIAssertionError → JUnit failure).
     *
     * @param runnable the assertion block to execute
     */
    public void wrapAssertionError(final Runnable runnable)
    {
        browser.wrapAssertionError(runnable);
    }

    /**
     * Safely executes a runnable, swallowing any exception without propagating it.
     * Useful for best-effort operations (e.g. debug highlighting) that must never
     * fail the test.
     *
     * @param runnable the block to attempt
     */
    public void safeExecute(final Runnable runnable)
    {
        browser.safeExecute(runnable);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Driver event logging
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds a driver event listener using the underlying logging infrastructure.
     * For Selenide this delegates to {@code SelenideLogger.addListener}.
     *
     * @param listenerId unique string identifier for the listener
     * @param listener   the listener (must be compatible with the active backend)
     */
    public void addDriverLogger(final String listenerId, final Object listener)
    {
        browser.addDriverLogger(listenerId, listener);
    }

    /**
     * Removes a previously registered driver event listener.
     *
     * @param listenerId the unique listener identifier
     */
    public void removeDriverLogger(final String listenerId)
    {
        browser.removeDriverLogger(listenerId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Element finding
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Finds a {@link SelenideElement} using the given {@link By} locator.
     *
     * @param locator the element locator
     * @return the found element (lazy — not yet resolved in the DOM)
     */
    public SelenideElement find(final By locator)
    {
        return Selenide.$(locator);
    }

    /**
     * Finds a {@link SelenideElement} using a CSS selector string.
     *
     * @param cssSelector the CSS selector
     * @return the found element (lazy)
     */
    public SelenideElement find(final String cssSelector)
    {
        return Selenide.$(cssSelector);
    }

    /**
     * Finds all elements matching the given {@link By} locator.
     *
     * @param locator the locator
     * @return an {@link ElementsCollection} (lazy)
     */
    public ElementsCollection findAll(final By locator)
    {
        return Selenide.$$(locator);
    }

    /**
     * Finds all elements matching the given CSS selector.
     *
     * @param cssSelector the CSS selector
     * @return an {@link ElementsCollection} (lazy)
     */
    public ElementsCollection findAll(final String cssSelector)
    {
        return Selenide.$$(cssSelector);
    }

    /**
     * Wraps a list of existing {@link WebElement} instances as an {@link ElementsCollection}.
     *
     * @param elements the raw WebElements to wrap
     * @return an {@link ElementsCollection}
     */
    public ElementsCollection findAll(final List<WebElement> elements)
    {
        return Selenide.$$(elements);
    }

    /**
     * Finds all elements matching the given XPath expression.
     *
     * @param xpath the XPath expression
     * @return an {@link ElementsCollection} (lazy)
     */
    public ElementsCollection findAllXPath(final String xpath)
    {
        return Selenide.$$x(xpath);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Backend-agnostic element finding (AI action layer)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Finds the first element matching the given locator using the active backend.
     * <p>
     * Returns a backend-agnostic {@link FoundElement} — use this in the AI action layer
     * instead of {@link #find(By)} which returns a Selenide-specific type.
     * </p>
     *
     * @param locator the Selenium {@link By} locator
     * @return the first matched element wrapped in a {@link FoundElement}
     */
    public FoundElement findFirstElement(final By locator)
    {
        return browser.findFirst(locator);
    }

    /**
     * Finds all elements matching the given locator using the active backend.
     * <p>
     * Returns a backend-agnostic list — use this in the AI action layer
     * instead of {@link #findAll(By)} which returns a Selenide-specific type.
     * </p>
     *
     * @param locator the Selenium {@link By} locator
     * @return a possibly-empty list of matched elements wrapped in {@link FoundElement}
     */
    public List<FoundElement> findAllElements(final By locator)
    {
        return browser.findElements(locator);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Screenshot access (delegated to ScreenshotFacade)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Captures a viewport screenshot as a {@link BufferedImage}.
     *
     * @return the screenshot image, or empty if the driver is unavailable
     * @throws IOException if capture fails
     */
    public Optional<BufferedImage> takeViewportScreenshot() throws IOException
    {
        return screenshots.takeViewportScreenshot();
    }

    /**
     * Captures a full-page screenshot as a {@link BufferedImage}.
     * Uses the most capable method available (CDP, DevTools, Firefox native, Shutterbug fallback).
     *
     * @return the screenshot image, or empty if unavailable
     * @throws IOException if capture fails
     */
    public Optional<BufferedImage> takeFullPageScreenshot() throws IOException
    {
        return screenshots.takeFullPageScreenshot();
    }

    /**
     * Returns {@code true} if the active driver supports full-page screenshots.
     *
     * @return {@code true} if full-page capture is available
     */
    public boolean supportsFullPageScreenshot()
    {
        return screenshots.supportsFullPageScreenshot();
    }

    /**
     * Closes the active browser session.
     */
    public void close()
    {
        browser.close();
    }
    public java.util.Map<String, Object> getAXTree()
    {
        return browser.getAXTree();
    }
    public java.util.Map<String, Object> executeCdpCommand(final String command, final java.util.Map<String, Object> params)
    {
        return browser.executeCdpCommand(command, params);
    }

    /**
     * Returns {@code true} if a session is currently active.
     *
     * @return {@code true} if active
     */
    public boolean hasActiveSession()
    {
        return browser.hasActiveSession();
    }

    /**
     * Returns the AI prompt facade associated with this interaction layer.
     *
     * @return AI prompt facade
     */
    public AiPromptFacade prompts()
    {
        return browser.getPrompts();
    }
}

