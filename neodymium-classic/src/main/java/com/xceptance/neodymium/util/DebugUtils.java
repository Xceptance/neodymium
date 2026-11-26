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

    private static void highlightAllElements(Supplier<List<WebElement>> getElements, WebDriver driver)
    {
        if (Neodymium.shouldHighlightElements(driver))
        {
            highlightElements(getElements.get(), driver);
            if (Neodymium.configuration().debuggingHighlightDuration() > 0)
            {
                Selenide.sleep(Neodymium.configuration().debuggingHighlightDuration());
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

    static void highlightElements(List<WebElement> elements, WebDriver driver)
    {
        long highlightTime = Neodymium.configuration().debuggingHighlightDuration();
        if (highlightTime <= 0)
        {
            highlightTime = 75;
        }
        Selenide.executeJavaScript("if(window.NEODYMIUM){"
                                   + "window.NEODYMIUM.highlightAllElements(arguments[0], document, "
                                   + highlightTime + ");"
                                   + "}",
                                   elements, driver.getWindowHandle());
    }

    static void resetAllHighlight()
    {
        Selenide.executeJavaScript("if(window.NEODYMIUM){window.NEODYMIUM.resetHighlightElements(document);}");
    }
}
