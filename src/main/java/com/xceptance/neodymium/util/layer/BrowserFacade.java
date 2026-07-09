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
package com.xceptance.neodymium.util.layer;

import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.HasCdp;
import com.xceptance.neodymium.ai.core.ContextLevel;

/**
 * Facade abstracting all browser-level operations from framework consumers.
 * <p>
 * Implement this interface for each driver backend (Selenide/Selenium, Playwright, etc.)
 * so that callers never import any driver-specific types.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public interface BrowserFacade
{
    /**
     * Opens the given URL in the current browser tab.
     *
     * @param url the URL to open
     */
    void open(String url);

    /**
     * Opens the given URL using HTTP Basic Authentication.
     *
     * @param url      the URL to open
     * @param username the basic-auth username
     * @param password the basic-auth password
     */
    void openWithBasicAuth(String url, String username, String password);

    /**
     * Navigates back in the browser history.
     */
    void back();

    /**
     * Navigates forward in the browser history.
     */
    void forward();

    /**
     * Reloads the current page.
     */
    void refresh();

    /**
     * Pauses the current thread for the given amount of milliseconds.
     *
     * @param milliseconds the sleep duration
     */
    void sleep(long milliseconds);

    /**
     * Returns the URL of the current page.
     *
     * @return current page URL
     */
    String getCurrentUrl();

    /**
     * Returns the title of the current page.
     *
     * @return current page title
     */
    String getPageTitle();

    /**
     * Returns the window handle of the currently focused browser window.
     *
     * @return current window handle
     */
    String getWindowHandle();

    /**
     * Returns all window handles currently open in the browser.
     *
     * @return set of window handles
     */
    Set<String> getWindowHandles();

    /**
     * Switches focus to the browser window identified by the given handle.
     *
     * @param handle the window handle to switch to
     */
    void switchToWindow(String handle);

    /**
     * Switches focus to the frame identified by the given CSS selector.
     *
     * @param selector CSS selector of the frame
     */
    void switchToFrame(String selector);

    /**
     * Switches focus to the frame identified by the given index.
     *
     * @param index zero-based index of the frame
     */
    void switchToFrame(int index);

    /**
     * Switches focus to the parent frame of the current frame.
     */
    void switchToParentFrame();

    /**
     * Resets focus to the top-level document (exits any iframe/frame context).
     */
    void switchToDefaultContent();

    /**
     * Sends a key sequence to the currently focused (active) element in the browser.
     *
     * @param key the key or chord to send
     */
    void sendKeysToActiveElement(CharSequence key);

    /**
     * Opens a new blank browser tab and switches focus to it.
     */
    void openNewTab();

    /**
     * Opens a new browser window and switches focus to it.
     */
    void openNewWindow();

    /**
     * Closes the currently focused browser window or tab.
     */
    void closeCurrentWindow();

    /**
     * Returns {@code true} if a browser session is currently active.
     *
     * @return {@code true} if started
     */
    boolean hasWebDriverStarted();

    /**
     * Clears all browser cookies for the current session.
     */
    void clearBrowserCookies();

    /**
     * Executes synchronous JavaScript in the context of the current page.
     *
     * @param script the JavaScript snippet to execute
     * @param args   optional script arguments
     * @param <T>    expected return type
     * @return result of the script, or {@code null}
     */
    <T> T executeJavaScript(String script, Object... args);

    /**
     * Executes asynchronous JavaScript in the context of the current page.
     * The script must call the last argument (a callback) to signal completion.
     *
     * @param script the JavaScript snippet to execute
     * @param args   optional script arguments
     * @param <T>    expected return type
     * @return result passed to the callback, or {@code null}
     */
    <T> T executeAsyncJavaScript(String script, Object... args);

    /**
     * Blocks until the browser reports {@code document.readyState === "complete"}.
     */
    void waitUntilDocumentReady();

    /**
     * Returns the current Selenide/driver timeout in milliseconds.
     * This is used by callers that need to know the configured implicit wait.
     *
     * @return timeout in milliseconds
     */
    long getTimeout();

    /**
     * Adds a driver event listener using the implementation-specific logging infrastructure.
     * For Selenide this delegates to {@code SelenideLogger.addListener}.
     *
     * @param listenerId a unique identifier for the listener
     * @param listener   the listener to add (implementation-typed)
     */
    void addDriverLogger(String listenerId, Object listener);

    /**
     * Removes a previously registered driver event listener.
     *
     * @param listenerId the unique identifier of the listener to remove
     */
    void removeDriverLogger(String listenerId);

    /**
     * Wraps a runnable such that any assertion error thrown is re-thrown
     * in a framework-appropriate manner (e.g. Selenide UIAssertionError → JUnit failure).
     *
     * @param runnable the assertion block to execute
     */
    void wrapAssertionError(Runnable runnable);

    /**
     * Safely executes a runnable, swallowing any exception without propagating it.
     * Useful for best-effort operations (e.g. debug highlighting) that must never
     * fail the test.
     *
     * @param runnable the block to attempt
     */
    void safeExecute(Runnable runnable);

    /**
     * Captures the full accessibility tree of the current page.
     *
     * @return a map representing the AXTree, or {@code null} if unavailable
     */
    java.util.Map<String, Object> getAXTree();

    /**
     * Executes a low-level Chrome DevTools Protocol (CDP) command.
     *
     * @param command the CDP command name
     * @param params  the command parameters
     * @return the result of the command, or {@code null} if unsupported or failed
     */
    java.util.Map<String, Object> executeCdpCommand(String command, java.util.Map<String, Object> params);

    /**
     * Closes the entire browser session.
     */
    void close();

    /**
     * Finds the first element matching the given locator.
     *
     * @param locator the Selenium {@link By} locator
     * @return the first matched element, never {@code null}
     * @throws RuntimeException if no element is found
     */
    FoundElement findFirst(By locator);

    /**
     * Finds all elements matching the given locator.
     *
     * @param locator the Selenium {@link By} locator
     * @return a possibly-empty list of matched elements
     */
    List<FoundElement> findElements(By locator);

    /**
     * Returns {@code true} if a session (browser or desktop) is currently active.
     *
     * @return {@code true} if started
     */
    default boolean hasActiveSession()
    {
        return hasWebDriverStarted();
    }

    /**
     * Returns the AI prompt facade associated with this browser facade.
     *
     * @return AI prompt facade
     */
    default AiPromptFacade getPrompts()
    {
        return new AiPromptFacade() {};
    }
}

