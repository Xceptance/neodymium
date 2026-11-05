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

    private static void highlightAllElements(Supplier<List<WebElement>> getElements, WebDriver driver)
    {
        if (Neodymium.configuration().debuggingHighlightSelectedElements())
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
        if (Neodymium.configuration().debuggingHighlightSelectedElements())
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
