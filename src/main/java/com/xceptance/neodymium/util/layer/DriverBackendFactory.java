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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.xceptance.neodymium.util.layer.selenide.SelenideBrowserFacade;
import com.xceptance.neodymium.util.layer.selenide.SelenideScreenshotFacade;

/**
 * Factory class that constructs browser and screenshot facades depending on configuration.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class DriverBackendFactory
{
    private static final Map<String, Supplier<BrowserFacade>> BROWSER_FACADES = new ConcurrentHashMap<>();
    private static final Map<String, Supplier<ScreenshotFacade>> SCREENSHOT_FACADES = new ConcurrentHashMap<>();
    private static final Map<String, Supplier<AssertionFacade>> ASSERTION_FACADES = new ConcurrentHashMap<>();

    static
    {
        register("selenium", SelenideBrowserFacade::new, SelenideScreenshotFacade::new, com.xceptance.neodymium.util.layer.selenide.SelenideAssertionFacade::new);
        register("desktop", 
            com.xceptance.neodymium.util.layer.desktop.DesktopBrowserFacade::new, 
            com.xceptance.neodymium.util.layer.desktop.DesktopScreenshotFacade::new, 
            com.xceptance.neodymium.util.layer.desktop.DesktopAssertionFacade::new);
        register("playwright", 
            () -> {
                try
                {
                    final Class<?> clazz = Class.forName("com.xceptance.neodymium.util.layer.playwright.PlaywrightBrowserFacade");
                    return (BrowserFacade) clazz.getDeclaredConstructor().newInstance();
                }
                catch (final Exception e)
                {
                    throw new RuntimeException("Failed to instantiate PlaywrightBrowserFacade", e);
                }
            }, 
            () -> {
                try
                {
                    final Class<?> clazz = Class.forName("com.xceptance.neodymium.util.layer.playwright.PlaywrightScreenshotFacade");
                    return (ScreenshotFacade) clazz.getDeclaredConstructor().newInstance();
                }
                catch (final Exception e)
                {
                    throw new RuntimeException("Failed to instantiate PlaywrightScreenshotFacade", e);
                }
            },
            () -> {
                try
                {
                    final Class<?> clazz = Class.forName("com.xceptance.neodymium.util.layer.playwright.PlaywrightAssertionFacade");
                    return (AssertionFacade) clazz.getDeclaredConstructor().newInstance();
                }
                catch (final Exception e)
                {
                    // Fallback to Selenide if Playwright one is not yet implemented or available
                    return new com.xceptance.neodymium.util.layer.selenide.SelenideAssertionFacade();
                }
            }
        );
    }

    private DriverBackendFactory()
    {
    }

    /**
     * Registers a driver backend implementation dynamically.
     *
     * @param name               the backend name
     * @param browserSupplier     supplier for BrowserFacade
     * @param screenshotSupplier  supplier for ScreenshotFacade
     * @param assertionSupplier   supplier for AssertionFacade
     */
    public static void register(final String name, final Supplier<BrowserFacade> browserSupplier, 
            final Supplier<ScreenshotFacade> screenshotSupplier, final Supplier<AssertionFacade> assertionSupplier)
    {
        BROWSER_FACADES.put(name.toLowerCase(), browserSupplier);
        SCREENSHOT_FACADES.put(name.toLowerCase(), screenshotSupplier);
        ASSERTION_FACADES.put(name.toLowerCase(), assertionSupplier);
    }

    /**
     * Creates a {@link BrowserFacade} for the configured backend.
     *
     * @param backend configured backend name
     * @return BrowserFacade instance
     */
    public static BrowserFacade createBrowserFacade(final String backend)
    {
        final Supplier<BrowserFacade> supplier = BROWSER_FACADES.get(backend.toLowerCase());
        if (supplier == null)
        {
            throw new IllegalArgumentException("Unknown driver backend: " + backend);
        }
        return supplier.get();
    }

    /**
     * Creates a {@link ScreenshotFacade} for the configured backend.
     *
     * @param backend configured backend name
     * @return ScreenshotFacade instance
     */
    public static ScreenshotFacade createScreenshotFacade(final String backend)
    {
        final Supplier<ScreenshotFacade> supplier = SCREENSHOT_FACADES.get(backend.toLowerCase());
        if (supplier == null)
        {
            throw new IllegalArgumentException("Unknown driver backend: " + backend);
        }
        return supplier.get();
    }

    /**
     * Creates an {@link AssertionFacade} for the configured backend.
     *
     * @param backend configured backend name
     * @return AssertionFacade instance
     */
    public static AssertionFacade createAssertionFacade(final String backend)
    {
        final Supplier<AssertionFacade> supplier = ASSERTION_FACADES.get(backend.toLowerCase());
        if (supplier == null)
        {
            throw new IllegalArgumentException("Unknown driver backend: " + backend);
        }
        return supplier.get();
    }
}
