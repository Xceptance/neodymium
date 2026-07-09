package com.xceptance.neodymium.util.layer.desktop;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import com.xceptance.neodymium.util.layer.ScreenshotFacade;

/**
 * Desktop-backed implementation of {@link ScreenshotFacade}.
 * <p>
 * Captures the entire screen using java.awt.Robot.
 * </p>
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class DesktopScreenshotFacade implements ScreenshotFacade
{
    private Robot robot;

    public DesktopScreenshotFacade()
    {
        try
        {
            this.robot = new Robot();
        }
        catch (final AWTException e)
        {
            throw new RuntimeException("Failed to initialize java.awt.Robot for DesktopScreenshotFacade", e);
        }
    }

    @Override
    public Optional<BufferedImage> takeViewportScreenshot() throws IOException
    {
        final Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        return Optional.of(robot.createScreenCapture(screenRect));
    }

    @Override
    public Optional<BufferedImage> takeFullPageScreenshot() throws IOException
    {
        return takeViewportScreenshot();
    }

    @Override
    public boolean supportsFullPageScreenshot()
    {
        return false;
    }
}
