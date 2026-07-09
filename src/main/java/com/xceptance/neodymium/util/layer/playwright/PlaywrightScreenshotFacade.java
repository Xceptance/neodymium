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
package com.xceptance.neodymium.util.layer.playwright;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import com.microsoft.playwright.Page;
import com.xceptance.neodymium.util.layer.ScreenshotFacade;

/**
 * Playwright-backed implementation of {@link ScreenshotFacade}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaywrightScreenshotFacade implements ScreenshotFacade
{
    public PlaywrightScreenshotFacade()
    {
    }

    @Override
    public Optional<BufferedImage> takeViewportScreenshot() throws IOException
    {
        final Page page = PlaywrightBrowserFacade.getPage();
        if (page == null)
        {
            return Optional.empty();
        }
        final byte[] screenshotBytes = page.screenshot();
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(screenshotBytes))
        {
            return Optional.ofNullable(ImageIO.read(bais));
        }
    }

    @Override
    public Optional<BufferedImage> takeFullPageScreenshot() throws IOException
    {
        final Page page = PlaywrightBrowserFacade.getPage();
        if (page == null)
        {
            return Optional.empty();
        }
        final byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(screenshotBytes))
        {
            return Optional.ofNullable(ImageIO.read(bais));
        }
    }

    @Override
    public boolean supportsFullPageScreenshot()
    {
        return true;
    }
}
