package com.xceptance.neodymium.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.HasCdp;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v137.page.Page;
import org.openqa.selenium.devtools.v137.page.model.Viewport;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.HasFullPageScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.assertthat.selenium_shutterbug.core.Capture;
import com.assertthat.selenium_shutterbug.core.PageSnapshot;
import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.assertthat.selenium_shutterbug.utils.image.ImageProcessor;
import com.assertthat.selenium_shutterbug.utils.web.Coordinates;
import com.google.common.collect.ImmutableMap;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.Neodymium;

import io.qameta.allure.Allure;

public class ScreenshotWriter
{
    private static final Logger log = LoggerFactory.getLogger(ScreenshotWriter.class);

    private static boolean highlightViewPort()
    {
        return Neodymium.configuration().enableFullPageCapture() ? Neodymium.configuration().enableHighlightViewport() : false;
    }

    private static boolean blurFullPageScreenshot()
    {
        return Neodymium.configuration().enableFullPageCapture() ? Neodymium.configuration().blurFullPageScreenshot() : false;
    }

    private static Capture getCaptureMode()
    {
        return Neodymium.configuration().enableFullPageCapture() ? Capture.FULL : Capture.VIEWPORT;
    }

    public static String getFormatedReportsPath()
    {
        return Path.of(System.getProperty("java.io.tmpdir") + Neodymium.configuration().reportsPath()).normalize().toString();
    }

    public static boolean doScreenshot(String filename) throws IOException
    {
        return doScreenshot(filename, getFormatedReportsPath());
    }

    public static boolean doScreenshot(String filename, String pathname) throws IOException
    {
        // If no driver is available, we cannot take a screenshot
        if (!Neodymium.hasDriver())
        {
            return false;
        }

        WebDriver driver = Neodymium.getDriver();

        Capture captureMode = getCaptureMode();
        WebDriver webDriver = Neodymium.getDriver();
        Optional<BufferedImage> imageOptional = Optional.empty();
        if (captureMode.equals(Capture.FULL))
        {
            Optional<File> imageFile = Optional.empty();
            if (webDriver instanceof HasFullPageScreenshot firefoxDriver)
            {
                imageFile = Optional.of(firefoxDriver.getFullPageScreenshotAs(OutputType.FILE));
            }
            else if (webDriver instanceof HasCdp)
            {
                imageFile = takeScreenshotWithCDP((WebDriver & HasCdp & JavascriptExecutor) webDriver, OutputType.FILE);
            }
            else if (webDriver instanceof HasDevTools)
            {
                imageFile = takeScreenshot((WebDriver & HasDevTools & JavascriptExecutor) webDriver, OutputType.FILE);
            }
            if (imageFile.isPresent())
            {
                imageOptional = Optional.of(ImageIO.read(imageFile.get()));
            }
        }
        else
        {
            PageSnapshot snapshot = Shutterbug.shootPage(driver, captureMode);
            imageOptional = Optional.of(snapshot.getImage());
        }
        if (imageOptional.isPresent())
        {
            BufferedImage image = imageOptional.get();
            Files.createDirectories(Paths.get(pathname));
            String imagePath = pathname + File.separator + filename + ".png";
            File outputfile = new File(imagePath);

            if (highlightViewPort() || blurFullPageScreenshot())
            {
                double devicePixelRatio = Double.parseDouble(((JavascriptExecutor) driver).executeScript("return window.devicePixelRatio") + "");
                int offsetY = (int) (Double.parseDouble(((JavascriptExecutor) driver)
                                                                                     .executeScript("return Math.round(Math.max(document.documentElement.scrollTop, document.body.scrollTop))")
                                                                                     .toString()));
                int offsetX = (int) (Double.parseDouble(((JavascriptExecutor) driver)
                                                                                     .executeScript("return Math.round(Math.max(document.documentElement.scrollLeft, document.body.scrollLeft))")
                                                                                     .toString()));

                Dimension size = Neodymium.getViewportSize();
                if (driver instanceof FirefoxDriver)
                {
                    size = new Dimension(size.width - (int) (15 * devicePixelRatio), size.height - (int) (15 * devicePixelRatio));
                }
                Point currentLocation = new Point(offsetX, offsetY);
                Coordinates coords = new Coordinates(currentLocation, currentLocation, size, new Dimension(0, 0), devicePixelRatio);

                if (highlightViewPort())
                {
                    image = highlightScreenShot(image, coords, Color.decode(Neodymium.configuration().fullScreenHighlightColor()));
                }
                if (blurFullPageScreenshot())
                {
                    image = ImageProcessor.blurExceptArea(image, coords);
                }
            }
            if (Neodymium.configuration().enableHighlightLastElement() && Neodymium.hasLastUsedElement())
            {
                try
                {
                    double devicePixelRatio = Double.parseDouble("" + ((JavascriptExecutor) driver).executeScript("return window.devicePixelRatio"));
                    image = highlightScreenShot(image, new Coordinates(Neodymium.getLastUsedElement(), devicePixelRatio),
                                                Color.decode(Neodymium.configuration().screenshotElementHighlightColor()));
                }
                catch (NoSuchElementException e)
                {
                    // If the test is breaking because we can't find an element, we also can't highlight this element...
                    // so
                    // a NoSuchElementException is expected and can be ignored.
                }
            }
            log.info("captured Screenshot to: " + imagePath);

            boolean result = ImageIO.write(image, "png", outputfile);
            if (result)
            {
                // The idea is to put the screenshot to the best place in the report,
                // but for before methods, this is not possible due to allure limitations
                // so we just add it normally when the allure lifecycle does not allow to be altered
                boolean screenshotAdded;
                Allure.getLifecycle().addAttachment("Screenshot", "image/png", ".png", new FileInputStream(imagePath));
                screenshotAdded = true;

                // to spare disk space, remove the file if we already used it inside the report
                if (screenshotAdded)
                {
                    outputfile.delete();
                }
            }
            return result;
        }
        return false;
    }

    public static <WD extends WebDriver & HasDevTools & JavascriptExecutor, ResultType> Optional<ResultType> takeScreenshot(
                                                                                                                            WD devtoolsDriver,
                                                                                                                            OutputType<ResultType> outputType)
    {
        DevTools devTools = devtoolsDriver.getDevTools();
        devTools.createSessionIfThereIsNotOne(devtoolsDriver.getWindowHandle());

        long fullWidth = (long) devtoolsDriver.executeScript("return Math.max(document.body.scrollWidth, document.documentElement.scrollWidth, document.body.offsetWidth, document.documentElement.offsetWidth, document.body.clientWidth, document.documentElement.clientWidth)");
        long fullHeight = (long) devtoolsDriver.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, document.body.offsetHeight, document.documentElement.offsetHeight, document.body.clientHeight, document.documentElement.clientHeight)");

        long viewWidth = (long) devtoolsDriver.executeScript("return window.innerWidth");
        long viewHeight = (long) devtoolsDriver.executeScript("return window.innerHeight");
        boolean exceedViewport = fullWidth > viewWidth || fullHeight > viewHeight;
        Viewport viewport = new Viewport(0, 0, fullWidth, fullHeight, 1);

        String base64 = devTools.send(Page.captureScreenshot(
                                                             Optional.empty(),
                                                             Optional.empty(),
                                                             Optional.of(viewport),
                                                             Optional.empty(),
                                                             Optional.of(exceedViewport),
                                                             Optional.of(true)));

        ResultType screenshot = outputType.convertFromBase64Png(base64);
        return Optional.of(screenshot);
    }

    public static <WD extends WebDriver & HasCdp & JavascriptExecutor, ResultType> Optional<ResultType> takeScreenshotWithCDP(
                                                                                                                              WD cdpDriver,
                                                                                                                              OutputType<ResultType> outputType)
    {
        long fullWidth = (long) cdpDriver.executeScript("return Math.max(document.body.scrollWidth, document.documentElement.scrollWidth, document.body.offsetWidth, document.documentElement.offsetWidth, document.body.clientWidth, document.documentElement.clientWidth)");
        long fullHeight = (long) cdpDriver.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, document.body.offsetHeight, document.documentElement.offsetHeight, document.body.clientHeight, document.documentElement.clientHeight)");

        long viewWidth = (long) cdpDriver.executeScript("return window.innerWidth");
        long viewHeight = (long) cdpDriver.executeScript("return window.innerHeight");
        boolean exceedViewport = fullWidth > viewWidth || fullHeight > viewHeight;
        Map<String, Object> captureScreenshotOptions = ImmutableMap.of(
                                                                       "clip", ImmutableMap.of(
                                                                                               "x", 0,
                                                                                               "y", 0,
                                                                                               "width", fullWidth,
                                                                                               "height", fullHeight,
                                                                                               "scale", 1),
                                                                       "captureBeyondViewport", exceedViewport);

        Map<String, Object> result = cdpDriver.executeCdpCommand("Page.captureScreenshot", captureScreenshotOptions);

        String base64 = (String) result.get("data");
        ResultType screenshot = outputType.convertFromBase64Png(base64);
        return Optional.of(screenshot);
    }

    public static BufferedImage highlightScreenShot(BufferedImage sourceImage, Coordinates coords, Color color)
    {
        int lineWith = Neodymium.configuration().screenshotHighlightLineThickness();
        Graphics2D g = sourceImage.createGraphics();

        int maxHeigt = sourceImage.getHeight();
        int maxWidth = sourceImage.getWidth();

        g.setPaint(color);
        g.setStroke(new BasicStroke(lineWith));
        g.drawRoundRect(
                        Math.max(coords.getX() + lineWith / 2, 0),
                        Math.max(coords.getY() + lineWith / 2, 0),
                        Math.min(coords.getWidth() - lineWith / 2, maxWidth),
                        Math.min(coords.getHeight() - lineWith / 2, maxHeigt),
                        5, 5);
        g.dispose();
        return sourceImage;
    }

}
