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
        if (Neodymium.configuration().debuggingHighlightSelectedElements())
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
        if (Neodymium.configuration().debuggingHighlightSelectedElements())
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
        if (Neodymium.configuration().debuggingHighlightSelectedElements())
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
