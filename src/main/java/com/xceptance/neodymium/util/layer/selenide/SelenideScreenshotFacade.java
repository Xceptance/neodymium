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
package com.xceptance.neodymium.util.layer.selenide;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v147.page.Page;
import org.openqa.selenium.devtools.v147.page.model.Viewport;
import org.openqa.selenium.firefox.HasFullPageScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.assertthat.selenium_shutterbug.core.Capture;
import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.codeborne.selenide.WebDriverRunner;
import com.google.common.collect.ImmutableMap;
import com.xceptance.neodymium.util.layer.ScreenshotFacade;

/**
 * Selenide-backed implementation of {@link ScreenshotFacade}.
 * <p>
 * This class is the <strong>only permitted location</strong> for screenshot-related
 * Selenium/WebDriver imports (CDP, DevTools, Firefox full-page, Shutterbug). Replace this
 * class with a Playwright implementation to swap driver backends without touching callers.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SelenideScreenshotFacade implements ScreenshotFacade
{
    private static final Logger LOG = LoggerFactory.getLogger(SelenideScreenshotFacade.class);

    /**
     * Default constructor.
     */
    public SelenideScreenshotFacade()
    {
    }

    /** {@inheritDoc} */
    @Override
    public Optional<BufferedImage> takeViewportScreenshot() throws IOException
    {
        if (!WebDriverRunner.hasWebDriverStarted())
        {
            return Optional.empty();
        }
        final WebDriver driver = WebDriverRunner.getWebDriver();
        try
        {
            final BufferedImage image = Shutterbug.shootPage(driver, Capture.VIEWPORT).getImage();
            return Optional.of(image);
        }
        catch (final Exception e)
        {
            LOG.warn("Viewport screenshot failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<BufferedImage> takeFullPageScreenshot() throws IOException
    {
        if (!WebDriverRunner.hasWebDriverStarted())
        {
            return Optional.empty();
        }
        final WebDriver driver = WebDriverRunner.getWebDriver();

        // Attempt advanced full-page capture for Firefox, CDP, and DevTools drivers
        if (driver instanceof HasFullPageScreenshot firefoxDriver)
        {
            final File file = firefoxDriver.getFullPageScreenshotAs(OutputType.FILE);
            return Optional.of(ImageIO.read(file));
        }
        if (driver instanceof HasCdp)
        {
            @SuppressWarnings("unchecked")
            final WebDriver cdpDriver = driver;
            final Optional<File> file = takeFullPageWithCdp(
                (WebDriver & HasCdp & JavascriptExecutor) cdpDriver, OutputType.FILE);
            if (file.isPresent())
            {
                return Optional.of(ImageIO.read(file.get()));
            }
        }
        if (driver instanceof HasDevTools)
        {
            @SuppressWarnings("unchecked")
            final WebDriver dtDriver = driver;
            final Optional<File> file = takeFullPageWithDevTools(
                (WebDriver & HasDevTools & JavascriptExecutor) dtDriver, OutputType.FILE);
            if (file.isPresent())
            {
                return Optional.of(ImageIO.read(file.get()));
            }
        }

        // Fallback: Shutterbug full-page scroll-stitch
        try
        {
            return Optional.of(Shutterbug.shootPage(driver, Capture.FULL).getImage());
        }
        catch (final Exception e)
        {
            LOG.warn("Full-page screenshot fallback failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsFullPageScreenshot()
    {
        if (!WebDriverRunner.hasWebDriverStarted())
        {
            return false;
        }
        final WebDriver driver = WebDriverRunner.getWebDriver();
        return driver instanceof HasFullPageScreenshot
            || driver instanceof HasCdp
            || driver instanceof HasDevTools;
    }

    /**
     * Captures a full-page screenshot via the Chrome DevTools Protocol (CDP).
     *
     * @param <WD>        a WebDriver that also implements {@link HasCdp} and {@link JavascriptExecutor}
     * @param <ResultType> the output type (e.g., {@code File}, {@code byte[]})
     * @param cdpDriver   the driver instance
     * @param outputType  the desired output format
     * @return an optional containing the result, or empty on failure
     */
    public static <WD extends WebDriver & HasCdp & JavascriptExecutor, ResultType> Optional<ResultType> takeFullPageWithCdp(
        final WD cdpDriver,
        final OutputType<ResultType> outputType)
    {
        final long fullWidth = (long) cdpDriver.executeScript(
            "return Math.max(document.body.scrollWidth, document.documentElement.scrollWidth,"
                + "document.body.offsetWidth, document.documentElement.offsetWidth,"
                + "document.body.clientWidth, document.documentElement.clientWidth)");
        final long fullHeight = (long) cdpDriver.executeScript(
            "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight,"
                + "document.body.offsetHeight, document.documentElement.offsetHeight,"
                + "document.body.clientHeight, document.documentElement.clientHeight)");

        final long viewWidth = (long) cdpDriver.executeScript("return window.innerWidth");
        final long viewHeight = (long) cdpDriver.executeScript("return window.innerHeight");
        final boolean exceedViewport = fullWidth > viewWidth || fullHeight > viewHeight;

        final Map<String, Object> options = ImmutableMap.of(
            "clip", ImmutableMap.of(
                "x", 0, "y", 0,
                "width", fullWidth, "height", fullHeight, "scale", 1),
            "captureBeyondViewport", exceedViewport);

        final Map<String, Object> result = cdpDriver.executeCdpCommand("Page.captureScreenshot", options);
        final String base64 = (String) result.get("data");
        return Optional.of(outputType.convertFromBase64Png(base64));
    }

    /**
     * Captures a full-page screenshot using the Selenium DevTools API.
     *
     * @param <WD>        a WebDriver that also implements {@link HasDevTools} and {@link JavascriptExecutor}
     * @param <ResultType> the desired output format
     * @param devtoolsDriver the driver instance
     * @param outputType   the desired output format
     * @return an optional containing the result, or empty on failure
     */
    public static <WD extends WebDriver & HasDevTools & JavascriptExecutor, ResultType> Optional<ResultType> takeFullPageWithDevTools(
        final WD devtoolsDriver,
        final OutputType<ResultType> outputType)
    {
        final DevTools devTools = devtoolsDriver.getDevTools();
        devTools.createSessionIfThereIsNotOne(devtoolsDriver.getWindowHandle());

        final long fullWidth = (long) devtoolsDriver.executeScript(
            "return Math.max(document.body.scrollWidth, document.documentElement.scrollWidth,"
                + "document.body.offsetWidth, document.documentElement.offsetWidth,"
                + "document.body.clientWidth, document.documentElement.clientWidth)");
        final long fullHeight = (long) devtoolsDriver.executeScript(
            "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight,"
                + "document.body.offsetHeight, document.documentElement.offsetHeight,"
                + "document.body.clientHeight, document.documentElement.clientHeight)");

        final long viewWidth = (long) devtoolsDriver.executeScript("return window.innerWidth");
        final long viewHeight = (long) devtoolsDriver.executeScript("return window.innerHeight");
        final boolean exceedViewport = fullWidth > viewWidth || fullHeight > viewHeight;

        final Viewport viewport = new Viewport(0, 0, fullWidth, fullHeight, 1);
        final String base64 = devTools.send(Page.captureScreenshot(
            Optional.empty(),
            Optional.empty(),
            Optional.of(viewport),
            Optional.empty(),
            Optional.of(exceedViewport),
            Optional.of(true)));

        return Optional.of(outputType.convertFromBase64Png(base64));
    }
}
