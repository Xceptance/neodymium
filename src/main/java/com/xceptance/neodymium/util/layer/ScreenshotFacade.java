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
package com.xceptance.neodymium.util.layer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

/**
 * Facade abstracting all screenshot/capture operations from framework consumers.
 * <p>
 * Implement this interface for each driver backend (Selenide/Selenium, Playwright, etc.)
 * so that callers never import driver-specific screenshot types.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public interface ScreenshotFacade
{
    /**
     * Captures a viewport-only screenshot and returns it as a {@link BufferedImage}.
     *
     * @return screenshot image, or empty if the driver is unavailable
     * @throws IOException if the capture fails for I/O reasons
     */
    Optional<BufferedImage> takeViewportScreenshot() throws IOException;

    /**
     * Captures a full-page screenshot (using the most appropriate method for the
     * current browser) and returns it as a {@link BufferedImage}.
     * <p>
     * Falls back to viewport capture if full-page is not supported.
     * </p>
     *
     * @return screenshot image, or empty if the driver is unavailable or the capture failed
     * @throws IOException if the capture fails for I/O reasons
     */
    Optional<BufferedImage> takeFullPageScreenshot() throws IOException;

    /**
     * Returns {@code true} if the current driver supports full-page screenshot capture
     * via any available mechanism (CDP, DevTools, Firefox native).
     *
     * @return {@code true} if full-page screenshots are supported
     */
    boolean supportsFullPageScreenshot();
}
