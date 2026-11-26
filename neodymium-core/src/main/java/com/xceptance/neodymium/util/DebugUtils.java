/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.codeborne.selenide.Selenide;

/**
 * Class with util methods for debugging
 * 
 * @author olha
 */
public class DebugUtils
{
    private static final String injectJS;
    static
    {
        try (InputStream inputStream = DebugUtils.class.getResourceAsStream("inject.js"))
        {
            injectJS = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not load inject.js", e);
        }
    }

    public static void injectHighlightingJs()
    {
        if (Neodymium.shouldHighlightElements())
        {
            injectJavaScript();
        }
    }

    public static void highlightAllElements(By by, WebDriver driver)
    {
        highlightAllElements(() -> driver.findElements(by), driver);
    }

    public static void highlightAllElements(List<WebElement> elements, WebDriver driver)
    {
        highlightAllElements(() -> elements, driver);
    }

    private static void highlightAllElements(final Supplier<List<WebElement>> getElements, final WebDriver driver)
    {
        if (Neodymium.shouldHighlightElements(driver))
        {
            final List<WebElement> elements = getElements.get();
            
            // Get single-flash duration, defaulting to 100ms if not configured
            long duration = Neodymium.configuration().debuggingHighlightDuration();
            if (duration <= 0)
            {
                duration = 100;
            }
            
            // Get number of blinks, defaulting to 3 if not configured
            int blinkCount = Neodymium.configuration().debuggingHighlightBlinkCount();
            if (blinkCount <= 0)
            {
                blinkCount = 3;
            }
            
            // Total highlight display duration calculated dynamically and reliably (each full cycle contains an ON and an OFF state)
            final long totalDuration = 2 * duration * blinkCount;
            
            highlightElements(elements, driver, duration, blinkCount);
            if (totalDuration > 0)
            {
                Selenide.sleep(totalDuration);
            }
            resetAllHighlight();
        }
    }

    public static void resetHighlights()
    {
        if (Neodymium.shouldHighlightElements())
        {
            resetAllHighlight();
        }
    }

    static void injectJavaScript()
    {
        Selenide.executeJavaScript(injectJS);
    }

    /**
     * Highlights elements with the configured duration for backward compatibility.
     */
    static void highlightElements(final List<WebElement> elements, final WebDriver driver)
    {
        long highlightTime = Neodymium.configuration().debuggingHighlightDuration();
        if (highlightTime <= 0)
        {
            highlightTime = 100;
        }
        int blinkCount = Neodymium.configuration().debuggingHighlightBlinkCount();
        if (blinkCount <= 0)
        {
            blinkCount = 3;
        }
        highlightElements(elements, driver, highlightTime, blinkCount);
    }

    /**
     * Highlights elements for a specific single-flash duration.
     */
    static void highlightElements(final List<WebElement> elements, final WebDriver driver, final long duration)
    {
        int blinkCount = Neodymium.configuration().debuggingHighlightBlinkCount();
        if (blinkCount <= 0)
        {
            blinkCount = 3;
        }
        highlightElements(elements, driver, duration, blinkCount);
    }

    static void highlightElements(final List<WebElement> elements, final WebDriver driver, final long duration, final int blinkCount)
    {
        final Object result = Selenide.executeJavaScript(
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
            elements);
        if (result != null && result.toString().startsWith("JS_ERROR"))
        {
            System.err.println("Neodymium Element Highlighting Javascript Error:\n" + result);
        }
    }

    static void resetAllHighlight()
    {
        Selenide.executeJavaScript("if(window.NEODYMIUM){window.NEODYMIUM.resetHighlightElements(document);}");
    }
}
