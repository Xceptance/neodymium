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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

/**
 * Utility class providing debug-mode element highlighting support.
 * <p>
 * All browser/driver access is routed through {@link Neodymium#interaction()} so this class
 * contains no direct Selenium/Selenide imports.
 * </p>
 *
 * @author olha
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class DebugUtils
{
    /** The injected JavaScript for element highlighting, loaded once at class initialization. */
    private static final String injectJS;

    static
    {
        try (InputStream inputStream = DebugUtils.class.getResourceAsStream("inject.js"))
        {
            injectJS = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Could not load inject.js", e);
        }
    }

    /**
     * Injects the element highlighting JavaScript into the current page if highlighting is enabled.
     */
    public static void injectHighlightingJs()
    {
        if (Neodymium.configuration().debuggingHighlightSelectedElements())
        {
            injectJavaScript();
        }
    }

    /**
     * Highlights all elements matching the given locator using the default CSS strategy.
     *
     * @param locator the locator string identifying elements to highlight
     */
    public static void highlightAllElements(final String locator)
    {
        highlightAllElements(locator, LocatorType.CSS);
    }

    /**
     * Highlights all elements matching the given locator of the specified type.
     *
     * @param locator the locator string identifying elements to highlight
     * @param type the type of the locator strategy to use
     */
    public static void highlightAllElements(final String locator, final LocatorType type)
    {
        if (locator != null && Neodymium.configuration().debuggingHighlightSelectedElements())
        {
            highlightElementsAsync(locator, type);
            final long duration = buildDuration();
            final int blinkCount = buildBlinkCount();
            final long totalDuration = 2L * duration * blinkCount;

            if (totalDuration > 0)
            {
                Neodymium.interaction().sleep(totalDuration);
            }
            resetAllHighlight();
        }
    }

    /**
     * Highlights all elements matching the given locator of the specified type asynchronously.
     * This triggers the blink animation in the browser without blocking the current thread
     * or resetting the highlight afterwards.
     *
     * @param locator the locator string identifying elements to highlight
     * @param type the type of the locator strategy to use
     */
    static void highlightElementsAsync(final String locator, final LocatorType type)
    {
        if (locator != null && Neodymium.configuration().debuggingHighlightSelectedElements())
        {
            final String selector = locatorToSelectorString(locator, type);
            final long duration = buildDuration();
            final int blinkCount = buildBlinkCount();

            final Object result = Neodymium.interaction().executeJavaScript(
                "if (window.NEODYMIUM)\n"
                + "{\n"
                + "    try\n"
                + "    {\n"
                + "        var selector = arguments[0];\n"
                + "        var elements = [];\n"
                + "        if (selector.startsWith('xpath=')) {\n"
                + "            var xpath = selector.substring(6);\n"
                + "            var result = document.evaluate(xpath, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);\n"
                + "            for (var i = 0; i < result.snapshotLength; i++) {\n"
                + "                elements.push(result.snapshotItem(i));\n"
                + "            }\n"
                + "        } else {\n"
                + "            elements = Array.from(document.querySelectorAll(selector));\n"
                + "        }\n"
                + "        window.NEODYMIUM.highlightAllElements(elements, document, " + duration + ", null, null, " + blinkCount + ");\n"
                + "    }\n"
                + "    catch (e)\n"
                + "    {\n"
                + "        return 'JS_ERROR: ' + e.message + '\\n' + e.stack;\n"
                + "    }\n"
                + "}",
                selector
            );

            if (result != null && result.toString().startsWith("JS_ERROR"))
            {
                // Silencing the error to avoid cluttering logs with invalid selectors from best-effort highlighting.
                // System.err.println("Neodymium Element Highlighting Javascript Error:\n" + result);
            }
        }
    }

    private static String locatorToSelectorString(final String locator, final LocatorType type)
    {
        if (type == null)
        {
            return locator;
        }
        switch (type)
        {
            case XPATH:
            {
                return "xpath=" + locator;
            }
            case CSS:
            {
                return locator;
            }
            case ID:
            {
                return "#" + locator;
            }
            case CLASS_NAME:
            {
                return "." + locator;
            }
            case TAG_NAME:
            {
                return locator;
            }
            case NAME:
            {
                return "[name=\"" + locator + "\"]";
            }
            default:
            {
                return locator;
            }
        }
    }

    /**
     * Resets all active element highlights in the page.
     */
    public static void resetHighlights()
    {
        if (Neodymium.configuration().debuggingHighlightSelectedElements())
        {
            resetAllHighlight();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers (package-private for tests)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Injects the highlight JavaScript into the current page.
     */
    static void injectJavaScript()
    {
        Neodymium.interaction().executeJavaScript(injectJS);
    }

    /**
     * Resets all element highlights via JavaScript.
     */
    static void resetAllHighlight()
    {
        Neodymium.interaction().executeJavaScript("if(window.NEODYMIUM){window.NEODYMIUM.resetHighlightElements(document);}");
    }

    /** Returns the configured highlight duration, defaulting to 100 ms. */
    private static long buildDuration()
    {
        final long d = Neodymium.configuration().debuggingHighlightDuration();
        return d > 0 ? d : 100L;
    }

    /** Returns the configured blink count, defaulting to 3. */
    private static int buildBlinkCount()
    {
        final int c = Neodymium.configuration().debuggingHighlightBlinkCount();
        return c > 0 ? c : 3;
    }
}
