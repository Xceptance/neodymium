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
import java.util.Optional;

import javax.imageio.ImageIO;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.assertthat.selenium_shutterbug.core.Capture;
import com.assertthat.selenium_shutterbug.core.PageSnapshot;
import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.assertthat.selenium_shutterbug.utils.image.ImageProcessor;
import com.assertthat.selenium_shutterbug.utils.web.Coordinates;
import com.codeborne.selenide.ex.UIAssertionError;
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

    public static void doScreenshot(String displayName, String testClassName, Optional<Throwable> executionException, Annotation[] annotationList, TestStage testStage)
        throws IOException
    {
        if (Neodymium.configuration().enableAdvancedScreenShots())
        {
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
            String dataSetName = "";
            for (Annotation a : annotationList)
            {
                if (a.annotationType().equals(DataSet.class))
                {
                    DataSet set = (DataSet) a;
                    dataSetName += "_DataSet";
                    if (set.value() > 0)
                    {
                        dataSetName += "_" + set.value();
                    }
                    if (!set.id().isEmpty())
                    {
                        dataSetName += "_" + set.id();
                    }
                }
            }
            String imageName = displayName + '_' + Neodymium.getBrowserProfileName() + dataSetName + '_' + timeStamp;
            if (Neodymium.configuration().enableTreeDirectoryStructure())
            {
                testClassName = testClassName.replace('.', File.separatorChar);
            }

            String pathName = getFormatedReportsPath() + File.separator + testClassName;
            if (Neodymium.configuration().enableOnSuccess() && executionException.isEmpty())
            {
                doScreenshot(imageName, pathName, testStage);
            }
//            else
//            {
//                if (executionException.isPresent())
//                {
//                    Throwable error = executionException.get();
//                    if (error instanceof UIAssertionError)
//                    {
//                        doScreenshot(imageName, pathName, testStage);
//                    }
//                }
//            }
        }
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
        return doScreenshot(filename, getFormatedReportsPath(), TestStage.UNKNOWN);
    }

    public static boolean doScreenshot(String filename, String pathname, TestStage testStage) throws IOException
    {
        // If no driver is available, we cannot take a screenshot
        if (!Neodymium.hasDriver())
        {
            return false;
        }

        WebDriver driver = Neodymium.getDriver();

        Capture captureMode = getCaptureMode();

        PageSnapshot snapshot = Shutterbug.shootPage(driver, captureMode);
        BufferedImage image = snapshot.getImage();
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
                // If the test is breaking because we can't find an element, we also can't highlight this element... so
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
//            if (AllureAddons.canUpdateAllureTest() && testStage != TestStage.AFTER_EACH)
//            {
//                AllureAddons.removeAttachmentFromStepByName("Screenshot");
//                screenshotAdded = AllureAddons.addAttachmentToStep("Screenshot-"+testStage, "image/png", ".png", new FileInputStream(imagePath));
//            }
//            else
//            {
                Allure.getLifecycle().addAttachment("Screenshot-"+testStage, "image/png", ".png", new FileInputStream(imagePath));
                screenshotAdded = true;
//            }

            // to spare disk space, remove the file if we already used it inside the report
            if (screenshotAdded)
            {
                outputfile.delete();
            }
        }
        return result;
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
