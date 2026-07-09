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
package com.xceptance.neodymium.util.layer.playwright;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.CDPSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xceptance.neodymium.common.BrowserNavigationHook;
import com.xceptance.neodymium.util.layer.BrowserFacade;
import com.xceptance.neodymium.util.layer.FoundElement;
import org.openqa.selenium.By;

/**
 * Playwright-backed implementation of {@link BrowserFacade}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaywrightBrowserFacade implements BrowserFacade
{
    private static final ThreadLocal<PlaywrightState> STATE = ThreadLocal.withInitial(PlaywrightState::new);

    private final BrowserNavigationHook navigationHook = new BrowserNavigationHook();

    private static class PlaywrightState
    {
        Playwright playwright;
        Browser browser;
        BrowserContext context;
        Page page;
        CDPSession cdpSession;
        final Gson gson = new Gson();
    }

    public PlaywrightBrowserFacade()
    {
    }

    /**
     * Retrieves the current Playwright Page for the current thread.
     *
     * @return current Page or null if not initialized
     */
    public static Page getPage()
    {
        return STATE.get().page;
    }

    private PlaywrightState getOrCreateState()
    {
        final PlaywrightState state = STATE.get();
        if (state.playwright == null)
        {
            state.playwright = Playwright.create();

            boolean headless = true;
            String browserName = "chrome";
            int width = -1;
            int height = -1;

            final String profile = com.xceptance.neodymium.util.Neodymium.getBrowserProfileName();
            if (profile != null)
            {
                final com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration browserConfiguration =
                    com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration.getInstance()
                                                                                                  .getBrowserProfiles()
                                                                                                  .get(profile);
                if (browserConfiguration != null)
                {
                    headless = browserConfiguration.isHeadless();
                    if (browserConfiguration.getCapabilities() != null)
                    {
                        browserName = browserConfiguration.getCapabilities().getBrowserName().toLowerCase();
                    }
                    width = browserConfiguration.getBrowserWidth();
                    height = browserConfiguration.getBrowserHeight();
                }
            }

            final String sysHeadless = System.getProperty("playwright.headless");
            if (sysHeadless != null)
            {
                headless = !"false".equalsIgnoreCase(sysHeadless);
            }

            final BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(headless);
            if (browserName.contains("firefox"))
            {
                state.browser = state.playwright.firefox().launch(options);
            }
            else if (browserName.contains("safari") || browserName.contains("webkit"))
            {
                state.browser = state.playwright.webkit().launch(options);
            }
            else
            {
                state.browser = state.playwright.chromium().launch(options);
            }

            final Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();
            if (width > 0 && height > 0)
            {
                contextOptions.setViewportSize(width, height);
            }

            final String username = com.xceptance.neodymium.util.Neodymium.configuration().basicAuthUsername();
            final String password = com.xceptance.neodymium.util.Neodymium.configuration().basicAuthPassword();
            if (org.apache.commons.lang3.StringUtils.isNotBlank(username))
            {
                contextOptions.setHttpCredentials(username, password);
            }

            state.context = state.browser.newContext(contextOptions);
            state.context.onPage(p -> setupPageListeners(p));
            state.page = state.context.newPage();
            setupPageListeners(state.page);
        }
        return state;
    }

    @Override
    public void open(final String url)
    {
        getOrCreateState().page.navigate(url);
    }

    @Override
    public void openWithBasicAuth(final String url, final String username, final String password)
    {
        final PlaywrightState state = STATE.get();
        if (state.playwright == null)
        {
            getOrCreateState();
        }
        if (state.context != null)
        {
            state.context.close();
        }
        state.context = state.browser.newContext(new Browser.NewContextOptions()
            .setHttpCredentials(username, password));
        state.context.onPage(p -> setupPageListeners(p));
        state.page = state.context.newPage();
        setupPageListeners(state.page);
        state.page.navigate(url);
    }

    @Override
    public void back()
    {
        getOrCreateState().page.goBack();
    }

    @Override
    public void forward()
    {
        getOrCreateState().page.goForward();
    }

    @Override
    public void refresh()
    {
        getOrCreateState().page.reload();
    }

    @Override
    public void sleep(final long milliseconds)
    {
        try
        {
            Thread.sleep(milliseconds);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getCurrentUrl()
    {
        return getOrCreateState().page.url();
    }

    @Override
    public String getPageTitle()
    {
        return getOrCreateState().page.title();
    }

    @Override
    public String getWindowHandle()
    {
        return String.valueOf(getOrCreateState().page.hashCode());
    }

    @Override
    public Set<String> getWindowHandles()
    {
        final Set<String> handles = new HashSet<>();
        for (final Page p : getOrCreateState().context.pages())
        {
            handles.add(String.valueOf(p.hashCode()));
        }
        return handles;
    }

    /** {@inheritDoc} */
    @Override
    public void switchToWindow(final String handle)
    {
        final PlaywrightState state = STATE.get();
        for (final Page p : state.context.pages())
        {
            if (String.valueOf(p.hashCode()).equals(handle))
            {
                state.page = p;
                p.bringToFront();
                return;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void switchToFrame(final String selector)
    {
        // In Playwright, we don't "switch" the page, but we can track the current frame
        // For simplicity, we just use the selector in future interactions if we implement a frame-aware InteractionLayer
        // But for now, we'll just store the current frame locator in the state if needed.
    }

    /** {@inheritDoc} */
    @Override
    public void switchToFrame(final int index)
    {
    }

    @Override
    public void switchToParentFrame()
    {
    }

    @Override
    public void switchToDefaultContent()
    {
        // No-op in Playwright as it is locator/frame-scoped
    }

    @Override
    public void sendKeysToActiveElement(final CharSequence key)
    {
        getOrCreateState().page.keyboard().press(key.toString());
    }

    @Override
    public void openNewTab()
    {
        final PlaywrightState state = getOrCreateState();
        state.page = state.context.newPage();
        setupPageListeners(state.page);
    }

    @Override
    public void openNewWindow()
    {
        final PlaywrightState state = getOrCreateState();
        state.context = state.browser.newContext();
        state.context.onPage(p -> setupPageListeners(p));
        state.page = state.context.newPage();
        setupPageListeners(state.page);
    }

    @Override
    public void closeCurrentWindow()
    {
        final PlaywrightState state = getOrCreateState();
        state.page.close();
        if (!state.context.pages().isEmpty())
        {
            state.page = state.context.pages().get(0);
        }
    }

    @Override
    public boolean hasWebDriverStarted()
    {
        return STATE.get().playwright != null;
    }

    @Override
    public void clearBrowserCookies()
    {
        if (hasWebDriverStarted())
        {
            STATE.get().context.clearCookies();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T executeJavaScript(final String script, final Object... args)
    {
        final String finalScript;
        if (script.contains("return"))
        {
            // Playwright's page.evaluate() does not run in a function scope that provides
            // `arguments`, because the outermost expression passed to evaluate() is treated
            // as an arrow function body by Playwright's internal adapter. Arrow functions
            // intentionally do not have an `arguments` binding, so scripts that use the
            // Selenium-style `arguments[0]` pattern would throw "ReferenceError: arguments
            // is not defined".
            //
            // Fix: wrap the script in a REGULAR (non-arrow) function expression so that
            // `arguments` IS defined. The single parameter `args` receives the serialised
            // argument array from Playwright; `.apply(null, args)` then spreads that array
            // into `arguments[0]`, `arguments[1]`, … inside the inner function body,
            // exactly replicating Selenium's executeScript contract.
            finalScript = "(function(args) { return (function() { " + script + " }).apply(null, args instanceof Array ? args : [args]); })";
        }
        else
        {
            finalScript = script;
        }
        // Pass args as a List so Playwright serialises it as a JS array for the wrapper parameter
        return (T) getOrCreateState().page.evaluate(finalScript, java.util.Arrays.asList(args));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T executeAsyncJavaScript(final String script, final Object... args)
    {
        final String finalScript;
        if (script.contains("return"))
        {
            // Same rationale as executeJavaScript — use a regular function expression so that
            // `arguments` is available inside the inner function body.
            finalScript = "(function(args) { return (function() { " + script + " }).apply(null, args instanceof Array ? args : [args]); })";
        }
        else
        {
            finalScript = script;
        }
        return (T) getOrCreateState().page.evaluate(finalScript, java.util.Arrays.asList(args));
    }

    @Override
    public void waitUntilDocumentReady()
    {
        getOrCreateState().page.waitForLoadState();
    }

    @Override
    public long getTimeout()
    {
        return 30000L;
    }

    @Override
    public void addDriverLogger(final String listenerId, final Object listener)
    {
    }

    @Override
    public void removeDriverLogger(final String listenerId)
    {
    }

    @Override
    public void wrapAssertionError(final Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (final AssertionError e)
        {
            throw e;
        }
        catch (final Exception e)
        {
            throw new AssertionError(e);
        }
    }

    @Override
    public void safeExecute(final Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (final Exception e)
        {
        }
    }

    @Override
    public java.util.Map<String, Object> getAXTree()
    {
        return executeCdpCommand("Accessibility.getFullAXTree", java.util.Map.of());
    }

    @SuppressWarnings("unchecked")
    @Override
    public java.util.Map<String, Object> executeCdpCommand(final String command, final java.util.Map<String, Object> params)
    {
        final PlaywrightState state = getOrCreateState();
        if (state.cdpSession == null)
        {
            state.cdpSession = state.page.context().newCDPSession(state.page);
        }

        final JsonObject jsonParams = state.gson.toJsonTree(params).getAsJsonObject();
        final JsonObject result = state.cdpSession.send(command, jsonParams);
        
        if (result == null)
        {
            return null;
        }
        
        return state.gson.fromJson(result, java.util.Map.class);
    }

    @Override
    public void close()
    {
        final PlaywrightState state = STATE.get();
        if (state != null && state.playwright != null)
        {
            try
            {
                if (state.cdpSession != null)
                {
                    state.cdpSession.detach();
                }
                if (state.context != null)
                {
                    state.context.close();
                }
                if (state.browser != null)
                {
                    state.browser.close();
                }
                state.playwright.close();
            }
            finally
            {
                STATE.remove();
            }
        }
    }
    private void setupPageListeners(final Page page)
    {
        if (page == null)
        {
            return;
        }
        page.onFrameNavigated(frame ->
        {
            if (frame == page.mainFrame())
            {
                navigationHook.onNavigation(page.url());
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public FoundElement findFirst(final By locator)
    {
        final Page page = getOrCreateState().page;
        final String selector = byToPlaywrightSelector(locator);
        return new PlaywrightFoundElement(page.locator(selector).first());
    }

    /** {@inheritDoc} */
    @Override
    public List<FoundElement> findElements(final By locator)
    {
        final Page page = getOrCreateState().page;
        final String selector = byToPlaywrightSelector(locator);
        final Locator all = page.locator(selector);
        final int count = all.count();
        final List<FoundElement> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            result.add(new PlaywrightFoundElement(all.nth(i)));
        }
        return result;
    }

    /**
     * Converts a Selenium {@link By} locator to a Playwright-compatible CSS/XPath selector string.
     *
     * @param locator the Selenium By locator
     * @return the equivalent Playwright selector string
     */
    private static String byToPlaywrightSelector(final By locator)
    {
        final String desc = locator.toString();
        // Selenium toString format: "By.cssSelector: .foo" or "By.xpath: //div"
        if (desc.startsWith("By.xpath: "))
        {
            return "xpath=" + desc.substring("By.xpath: ".length());
        }
        if (desc.startsWith("By.cssSelector: "))
        {
            return desc.substring("By.cssSelector: ".length());
        }
        if (desc.startsWith("By.linkText: "))
        {
            // Playwright text selector
            return "text=" + desc.substring("By.linkText: ".length());
        }
        // Fallback: strip "By.*: " prefix
        final int colon = desc.indexOf(": ");
        return colon >= 0 ? desc.substring(colon + 2) : desc;
    }
}
