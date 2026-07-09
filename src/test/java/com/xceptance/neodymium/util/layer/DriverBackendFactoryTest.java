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

import org.junit.Assert;
import org.junit.Test;
import com.xceptance.neodymium.util.layer.selenide.SelenideBrowserFacade;
import com.xceptance.neodymium.util.layer.selenide.SelenideScreenshotFacade;
import com.xceptance.neodymium.util.layer.playwright.PlaywrightBrowserFacade;
import com.xceptance.neodymium.util.layer.playwright.PlaywrightScreenshotFacade;

/**
 * Unit tests for the {@link DriverBackendFactory}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class DriverBackendFactoryTest
{
    @Test
    public void testSeleniumBackendCreation()
    {
        final BrowserFacade browser = DriverBackendFactory.createBrowserFacade("selenium");
        final ScreenshotFacade screenshot = DriverBackendFactory.createScreenshotFacade("selenium");
        Assert.assertTrue(browser instanceof SelenideBrowserFacade);
        Assert.assertTrue(screenshot instanceof SelenideScreenshotFacade);
    }

    @Test
    public void testPlaywrightBackendCreation()
    {
        final BrowserFacade browser = DriverBackendFactory.createBrowserFacade("playwright");
        final ScreenshotFacade screenshot = DriverBackendFactory.createScreenshotFacade("playwright");
        Assert.assertTrue(browser instanceof PlaywrightBrowserFacade);
        Assert.assertTrue(screenshot instanceof PlaywrightScreenshotFacade);
    }
}
