/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.Selenide;

/**
 * Utility class for highlighting web elements in browser automation frameworks (Selenide, Selenium, Playwright, etc.).
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class ElementHighlightUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ElementHighlightUtils.class);

    private static final String HIGHLIGHT_SCRIPT;

    static
    {
        HIGHLIGHT_SCRIPT = loadHighlightScript();
    }

    /**
     * Functional interface abstracting JavaScript execution across automation frameworks (Selenide, Selenium, Playwright, etc.).
     */
    @FunctionalInterface
    public interface JavaScriptExecutor
    {
        /**
         * Executes the given JavaScript snippet with optional arguments.
         *
         * @param script the JavaScript source code to execute
         * @param args arguments to pass to the script execution
         * @return the result returned by the script execution
         */
        Object executeJavaScript(final String script, final Object... args);
    }

    /**
     * Loads the element highlighting JavaScript resource from classpath with fallback paths.
     *
     * @return the JavaScript content as String, or empty string if loading failed
     */
    private static String loadHighlightScript()
    {
        final String[] resourcePaths = {
            "/org/neodymium/util/element-highlighter.js",
            "element-highlighter.js",
            "/com/xceptance/neodymium/util/inject.js",
            "inject.js"
        };

        for (final String path : resourcePaths)
        {
            try (final InputStream inputStream = path.startsWith("/") 
                    ? ElementHighlightUtils.class.getResourceAsStream(path)
                    : ElementHighlightUtils.class.getResourceAsStream(path))
            {
                if (inputStream != null)
                {
                    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                }
            }
            catch (final Exception e)
            {
                LOGGER.debug("Failed to load highlighting script from resource path: {}", path, e);
            }
        }

        LOGGER.error("Could not load element highlighting script from any known resource path.");
        return "";
    }

    /**
     * Injects the highlighting JavaScript into the browser using Selenide if enabled in configuration.
     */
    public static void injectHighlightingJs()
    {
        if (Neodymium.shouldHighlightElements())
        {
            injectJavaScript(Selenide::executeJavaScript);
        }
    }

    /**
     * Injects the highlighting JavaScript into the browser using a custom JavaScriptExecutor.
     *
     * @param jsExecutor the executor used to run JavaScript in the target page
     */
    public static void injectHighlightingJs(final JavaScriptExecutor jsExecutor)
    {
        if (Neodymium.shouldHighlightElements())
        {
            injectJavaScript(jsExecutor);
        }
    }

    /**
     * Highlights all elements matching the given locator using Selenide/WebDriver.
     *
     * @param by locator for finding target elements
     * @param driver WebDriver instance
     */
    public static void highlightAllElements(final By by, final WebDriver driver)
    {
        highlightAllElements(() -> driver.findElements(by), driver);
    }

    /**
     * Highlights all elements in the given list using Selenide/WebDriver.
     *
     * @param elements list of WebElements to highlight
     * @param driver WebDriver instance
     */
    public static void highlightAllElements(final List<WebElement> elements, final WebDriver driver)
    {
        highlightAllElements(() -> elements, driver);
    }

    /**
     * Internal helper to resolve elements and highlight them using Selenide/WebDriver.
     *
     * @param getElements supplier delivering the list of elements to highlight
     * @param driver WebDriver instance
     */
    private static void highlightAllElements(final Supplier<List<WebElement>> getElements, final WebDriver driver)
    {
        if (Neodymium.shouldHighlightElements(driver))
        {
            final List<WebElement> elements = getElements.get();
            if (elements == null || elements.isEmpty())
            {
                return;
            }

            long duration = Neodymium.configuration().debuggingHighlightDuration();
            if (duration <= 0)
            {
                duration = 100;
            }

            int blinkCount = Neodymium.configuration().debuggingHighlightBlinkCount();
            if (blinkCount <= 0)
            {
                blinkCount = 3;
            }

            final long totalDuration = 2 * duration * blinkCount;

            highlightElements(elements, Selenide::executeJavaScript, duration, blinkCount);
            if (totalDuration > 0)
            {
                Selenide.sleep(totalDuration);
            }
            resetAllHighlight(Selenide::executeJavaScript);
        }
    }

    /**
     * Framework-agnostic method to highlight target elements using any JavaScript executor (e.g. Playwright, Selenide, Selenium).
     *
     * @param targetElements target elements or locator references to highlight
     * @param jsExecutor functional executor for running JS in the browser context
     * @param duration single blink duration in milliseconds
     * @param blinkCount number of blinking cycles
     */
    public static void highlightElements(final Object targetElements, final JavaScriptExecutor jsExecutor, final long duration, final int blinkCount)
    {
        if (HIGHLIGHT_SCRIPT.isEmpty())
        {
            LOGGER.warn("Highlight script is empty; skipping element highlighting.");
            return;
        }

        injectJavaScript(jsExecutor);

        final Object result = jsExecutor.executeJavaScript(
            "if (window.NEODYMIUM)\n"
            + "{\n"
            + "    try\n"
            + "    {\n"
            + "        window.NEODYMIUM.highlightAllElements(arguments[0], document, " + duration + ", null, null, " + blinkCount + ");\n"
            + "    }\n"
            + "    catch (e)\n"
            + "    {\n"
            + "        return 'JS_ERROR: ' + e.message + '\\n' + e.stack;\n"
            + "    }\n"
            + "}",
            targetElements);

        if (result != null && result.toString().startsWith("JS_ERROR"))
        {
            LOGGER.error("Neodymium Element Highlighting JavaScript Error:\n{}", result);
        }
    }

    /**
     * Resets active highlights using default Selenide JavaScript execution.
     */
    public static void resetHighlights()
    {
        if (Neodymium.shouldHighlightElements())
        {
            resetAllHighlight(Selenide::executeJavaScript);
        }
    }

    /**
     * Resets active highlights using a custom JavaScriptExecutor.
     *
     * @param jsExecutor functional executor for running JS in the browser context
     */
    public static void resetHighlights(final JavaScriptExecutor jsExecutor)
    {
        if (Neodymium.shouldHighlightElements())
        {
            resetAllHighlight(jsExecutor);
        }
    }

    /**
     * Injects the highlighting JavaScript into the browser session via the given executor.
     *
     * @param jsExecutor functional executor for running JS in the browser context
     */
    public static void injectJavaScript(final JavaScriptExecutor jsExecutor)
    {
        if (!HIGHLIGHT_SCRIPT.isEmpty())
        {
            jsExecutor.executeJavaScript(HIGHLIGHT_SCRIPT);
        }
    }

    /**
     * Clears all highlight boxes from the current document using the given JavaScript executor.
     *
     * @param jsExecutor functional executor for running JS in the browser context
     */
    public static void resetAllHighlight(final JavaScriptExecutor jsExecutor)
    {
        jsExecutor.executeJavaScript("if(window.NEODYMIUM){window.NEODYMIUM.resetHighlightElements(document);}");
    }
}
