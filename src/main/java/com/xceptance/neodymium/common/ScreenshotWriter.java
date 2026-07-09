package com.xceptance.neodymium.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.assertthat.selenium_shutterbug.core.Capture;
import com.assertthat.selenium_shutterbug.utils.image.ImageProcessor;
import com.assertthat.selenium_shutterbug.utils.web.Coordinates;
import com.xceptance.neodymium.util.LocatorType;
import com.xceptance.neodymium.util.Neodymium;

import io.qameta.allure.Allure;

/**
 * Utility for capturing and writing screenshots, with optional element highlighting.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class ScreenshotWriter {
    private static final Logger log = LoggerFactory.getLogger(ScreenshotWriter.class);

    private static boolean highlightViewPort() {
        return Neodymium.configuration().enableFullPageCapture() ? Neodymium.configuration().enableHighlightViewport()
                : false;
    }

    private static boolean blurFullPageScreenshot() {
        return Neodymium.configuration().enableFullPageCapture() ? Neodymium.configuration().blurFullPageScreenshot()
                : false;
    }

    private static Capture getCaptureMode() {
        return Neodymium.configuration().enableFullPageCapture() ? Capture.FULL : Capture.VIEWPORT;
    }

    public static String getFormatedReportsPath() {
        return Path.of(System.getProperty("java.io.tmpdir") + Neodymium.configuration().reportsPath()).normalize()
                .toString();
    }

    public static String doScreenshot(String filename) throws IOException {
        return doScreenshot(filename, getFormatedReportsPath());
    }

    public static String doScreenshot(String filename, String pathname) throws IOException {
        return doScreenshot(filename, pathname, false, true);
    }

    public static String doScreenshot(String filename, boolean didSelenideScreenshot) throws IOException {
        return doScreenshot(filename, getFormatedReportsPath(), didSelenideScreenshot, true);
    }

    public static String doScreenshot(String filename, String pathname, boolean didSelenideScreenshot, boolean attach)
            throws IOException {
        String base64Image = null;

        // do viewport first otherwise the screen may be moved
        // viewport: !didSelenideScreenshot && enableViewportScreenshot
        if (!didSelenideScreenshot && Neodymium.configuration().enableViewportScreenshot()) {
            String vpBase64 = takeScreenshot(filename, pathname, Capture.VIEWPORT, attach);
            if (vpBase64 != null) {
                base64Image = vpBase64;
            }
        }

        // full page logic block
        if (Neodymium.configuration().enableAdvancedScreenShots()
                && Neodymium.configuration().enableFullPageCapture()) {
            String fpBase64 = takeScreenshot(filename, pathname, Capture.FULL, attach);
            if (fpBase64 != null) {
                base64Image = fpBase64;
            }
        }

        return base64Image;
    }

    private static String takeScreenshot(final String filename, final String pathname, final Capture captureMode, final boolean attach)
            throws IOException {
        // If no driver is available, we cannot take a screenshot
        if (!Neodymium.hasActiveBrowser()) {
            return null;
        }

        Optional<BufferedImage> imageOptional = Optional.empty();

        if (Capture.FULL.equals(captureMode)) {
            imageOptional = Neodymium.interaction().takeFullPageScreenshot();
        } else {
            imageOptional = Neodymium.interaction().takeViewportScreenshot();
        }

        if (imageOptional.isPresent()) {
            BufferedImage image = imageOptional.get();
            Files.createDirectories(Paths.get(pathname));
            final String imagePath = pathname + File.separator + filename + ".png";
            final File outputfile = new File(imagePath);

            // Logic for highlighting/blurring
            if (Capture.FULL.equals(captureMode) && (highlightViewPort() || blurFullPageScreenshot())) {
                final double devicePixelRatio = Double.parseDouble(
                        Neodymium.interaction().executeJavaScript("return window.devicePixelRatio") + "");
                final int offsetY = (int) (Double.parseDouble(Neodymium.interaction()
                        .executeJavaScript(
                                "return Math.round(Math.max(document.documentElement.scrollTop, document.body.scrollTop))")
                        .toString()));
                final int offsetX = (int) (Double.parseDouble(Neodymium.interaction()
                        .executeJavaScript(
                                "return Math.round(Math.max(document.documentElement.scrollLeft, document.body.scrollLeft))")
                        .toString()));

                Dimension size = Neodymium.getViewportSize();
                // Use Develop branch math here:
                size = new Dimension(Math.min(size.width - (int) (15 * devicePixelRatio),
                        image.getWidth()), Math.min(size.height - (int) (15 * devicePixelRatio), image.getHeight()));

                final Point currentLocation = new Point(offsetX, offsetY);
                final Coordinates coords = new Coordinates(currentLocation, currentLocation, size, new Dimension(0, 0),
                        devicePixelRatio);

                if (highlightViewPort()) {
                    image = highlightScreenShot(image, coords,
                            Color.decode(Neodymium.configuration().fullScreenHighlightColor()));
                }
                if (blurFullPageScreenshot()) {
                    image = ImageProcessor.blurExceptArea(image, coords);
                }
            }

            if (Neodymium.configuration().enableHighlightLastElement() && Neodymium.hasLastUsedLocator()) {
                final String locator = Neodymium.getLastUsedLocatorString();
                final LocatorType type = Neodymium.getLastUsedLocatorType();
                String selector = locator;
                boolean isXpath = false;

                if (type != null) {
                    switch (type) {
                        case XPATH:
                            isXpath = true;
                            break;
                        case CSS:
                            selector = locator;
                            break;
                        case ID:
                            selector = "#" + locator;
                            break;
                        case CLASS_NAME:
                            selector = "." + locator;
                            break;
                        case TAG_NAME:
                            selector = locator;
                            break;
                        case NAME:
                            selector = "[name=\"" + locator + "\"]";
                            break;
                    }
                }

                final String jsQuery;
                if (isXpath) {
                    jsQuery = "var el = document.evaluate(arguments[0], document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;\n" +
                              "if (el) {\n" +
                              "  var r = el.getBoundingClientRect();\n" +
                              "  return [r.left + window.scrollX, r.top + window.scrollY, r.width, r.height];\n" +
                              "}\n" +
                              "return null;";
                } else {
                    jsQuery = "var el = document.querySelector(arguments[0]);\n" +
                              "if (el) {\n" +
                              "  var r = el.getBoundingClientRect();\n" +
                              "  return [r.left + window.scrollX, r.top + window.scrollY, r.width, r.height];\n" +
                              "}\n" +
                              "return null;";
                }

                try {
                    final Object rectObj = Neodymium.interaction().executeJavaScript(jsQuery, selector);
                    if (rectObj instanceof List<?> list && list.size() == 4) {
                        final double x = Double.parseDouble(list.get(0).toString());
                        final double y = Double.parseDouble(list.get(1).toString());
                        final double w = Double.parseDouble(list.get(2).toString());
                        final double h = Double.parseDouble(list.get(3).toString());

                        final double devicePixelRatio = Double.parseDouble(
                                "" + Neodymium.interaction().executeJavaScript("return window.devicePixelRatio"));

                        final Point elementLocation = new Point((int) x, (int) y);
                        final Dimension elementSize = new Dimension((int) w, (int) h);
                        final Coordinates coords = new Coordinates(elementLocation, elementLocation, elementSize, new Dimension(0, 0), devicePixelRatio);

                        image = highlightScreenShot(image, coords,
                                Color.decode(Neodymium.configuration().screenshotElementHighlightColor()));
                    }
                } catch (final Throwable e) {
                    // ignore if element cannot be found or JS fails
                }
            }
            log.debug("captured Screenshot to: " + imagePath);

            final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            final boolean result = ImageIO.write(image, "png", baos);
            if (result) {
                final byte[] imageBytes = baos.toByteArray();
                java.nio.file.Files.write(outputfile.toPath(), imageBytes);

                // The idea is to put the screenshot to the best place in the report,
                // but for before methods, this is not possible due to allure limitations
                // so we just add it normally when the allure lifecycle does not allow to be
                // altered
                boolean screenshotAdded;
                if (attach) {
                    Allure.getLifecycle().addAttachment(
                            captureMode == Capture.FULL ? "Screenshot" : "View Port Screenshot", "image/png", ".png",
                            new java.io.ByteArrayInputStream(imageBytes));
                }
                // This will still be set to true, since the attach mode just wants to return a
                // screenshot
                screenshotAdded = true;

                // to spare disk space, remove the file if we already used it inside the report
                if (screenshotAdded) {
                    outputfile.delete();
                }

                return java.util.Base64.getEncoder().encodeToString(imageBytes);
            }
            return null;
        }

        return null;
    }

    public static BufferedImage highlightScreenShot(final BufferedImage sourceImage, final Coordinates coords, final Color color) {
        final int lineWith = Neodymium.configuration().screenshotHighlightLineThickness();
        final Graphics2D g = sourceImage.createGraphics();

        final int maxHeigt = sourceImage.getHeight();
        final int maxWidth = sourceImage.getWidth();

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

