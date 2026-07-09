package com.xceptance.neodymium.util.layer.desktop;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;

import com.xceptance.neodymium.util.layer.BrowserFacade;
import com.xceptance.neodymium.util.layer.FoundElement;
import com.xceptance.neodymium.util.layer.AiPromptFacade;
import com.xceptance.neodymium.ai.core.ContextLevel;

/**
 * Desktop-backed implementation of {@link BrowserFacade}.
 * <p>
 * This facade interacts with the host OS directly using commands and coordinate-based
 * input via java.awt.Robot.
 * </p>
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class DesktopBrowserFacade implements BrowserFacade
{
    private static final Pattern COORD_PATTERN = Pattern.compile("\\[(\\d+)\\s*,\\s*(\\d+)\\]");
    
    public DesktopBrowserFacade()
    {
    }

    @Override
    public void open(final String urlOrCommand)
    {
        try
        {
            // For desktop, 'open' executes the CLI command.
            Runtime.getRuntime().exec(urlOrCommand);
            // Add a small sleep to allow the app to launch visually
            sleep(2000);
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Failed to execute desktop command: " + urlOrCommand, e);
        }
    }

    @Override
    public void openWithBasicAuth(final String url, final String username, final String password)
    {
        open(url);
    }

    @Override
    public void back()
    {
    }

    @Override
    public void forward()
    {
    }

    @Override
    public void refresh()
    {
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
        return "desktop://os";
    }

    @Override
    public String getPageTitle()
    {
        return "Desktop";
    }

    @Override
    public String getWindowHandle()
    {
        return "desktop";
    }

    @Override
    public Set<String> getWindowHandles()
    {
        return Collections.singleton("desktop");
    }

    @Override
    public void switchToWindow(final String handle)
    {
    }

    @Override
    public void switchToFrame(final String selector)
    {
    }

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
    }

    @Override
    public void sendKeysToActiveElement(final CharSequence key)
    {
        // For sending keys directly to the active window without coordinates.
        // We can just create a DesktopFoundElement at 0,0 and sendKeys, 
        // since Robot doesn't need to move mouse if it's already focused.
        new DesktopFoundElement(0, 0).sendKeys(key);
    }

    @Override
    public void openNewTab()
    {
    }

    @Override
    public void openNewWindow()
    {
    }

    @Override
    public void closeCurrentWindow()
    {
    }

    @Override
    public boolean hasWebDriverStarted()
    {
        // The desktop backend never starts a Selenide/WebDriver session.
        // Returning false ensures that callers (e.g. InteractionLayer.getDriver(),
        // BrowserAfterRunner) correctly treat this as a no-browser context.
        return false;
    }

    @Override
    public void clearBrowserCookies()
    {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T executeJavaScript(final String script, final Object... args)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T executeAsyncJavaScript(final String script, final Object... args)
    {
        return null;
    }

    @Override
    public void waitUntilDocumentReady()
    {
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
        return null;
    }

    @Override
    public java.util.Map<String, Object> executeCdpCommand(final String command, final java.util.Map<String, Object> params)
    {
        return null;
    }

    @Override
    public void close()
    {
    }

    @Override
    public FoundElement findFirst(final By locator)
    {
        return parseCoordinateLocator(locator);
    }

    @Override
    public List<FoundElement> findElements(final By locator)
    {
        return Collections.singletonList(parseCoordinateLocator(locator));
    }
    
    private DesktopFoundElement parseCoordinateLocator(final By locator)
    {
        final String desc = locator.toString();
        final Matcher m = COORD_PATTERN.matcher(desc);
        if (m.find())
        {
            try
            {
                final int x = Integer.parseInt(m.group(1));
                final int y = Integer.parseInt(m.group(2));
                return new DesktopFoundElement(x, y);
            }
            catch (final NumberFormatException e)
            {
                // Fallthrough to exception
            }
        }
        
        // Extract raw selector/locator string (e.g. By.cssSelector: /tmp/file.txt -> /tmp/file.txt)
        final String path;
        final int colonIdx = desc.indexOf(':');
        if (colonIdx != -1)
        {
            path = desc.substring(colonIdx + 1).trim();
        }
        else
        {
            path = desc.trim();
        }
        
        if (path.startsWith("/") || path.contains("/") || path.contains("\\"))
        {
            return new DesktopFileFoundElement(path);
        }
        
        throw new IllegalArgumentException("Desktop backend requires element locators to be [x, y] coordinates or file paths. Got: " + desc);
    }

    @Override
    public boolean hasActiveSession()
    {
        return true;
    }

    @Override
    public AiPromptFacade getPrompts()
    {
        return new DesktopAiPromptFacade();
    }
}

