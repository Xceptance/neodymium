/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.util;

import com.codeborne.selenide.Configuration;
import com.xceptance.neodymium.common.browser.WebDriverStateContainer;
import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.recording.FilmTestExecution;
import java.lang.reflect.Proxy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Tests the performance optimization that disables element highlighting when running
 * in headless mode without active video or GIF recording.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class HighlightHeadlessOptimizationTest
{
    private boolean originalSelenideHeadless;

    @Before
    public void setUp()
    {
        originalSelenideHeadless = Configuration.headless;
        FilmTestExecution.clearThreadContexts();
        Neodymium.setWebDriverStateContainer(null);
        Neodymium.setBrowserProfileName(null);
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "false");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.headless.withoutVideo", "false");
    }

    @After
    public void tearDown()
    {
        Configuration.headless = originalSelenideHeadless;
        FilmTestExecution.clearThreadContexts();
        Neodymium.setWebDriverStateContainer(null);
        Neodymium.setBrowserProfileName(null);
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "false");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.headless.withoutVideo", "false");
    }

    @Test
    public void testHighlightDisabledGlobally()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "false");
        Configuration.headless = false;

        Assert.assertFalse("Should not highlight if highlight setting is false in headful mode",
                           Neodymium.shouldHighlightElements());

        Configuration.headless = true;
        Assert.assertFalse("Should not highlight if highlight setting is false in headless mode",
                           Neodymium.shouldHighlightElements());
    }

    @Test
    public void testHeadfulAlwaysHighlightsWhenEnabled()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Configuration.headless = false;

        Assert.assertFalse("Should be headful when Configuration.headless is false", Neodymium.isHeadless());
        Assert.assertTrue("Should highlight in headful mode without recording", Neodymium.shouldHighlightElements());
    }

    @Test
    public void testHeadlessSkipsHighlightingWithoutRecording()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Configuration.headless = true;

        Assert.assertTrue("Should be headless when Configuration.headless is true", Neodymium.isHeadless());
        Assert.assertFalse("Should not be recording initially", FilmTestExecution.isRecording());
        Assert.assertFalse("Should disable highlighting in headless mode when no video is recording",
                           Neodymium.shouldHighlightElements());
    }

    @Test
    public void testHeadlessAllowsHighlightingWhenOverrideConfigured()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.headless.withoutVideo", "true");
        Configuration.headless = true;

        Assert.assertTrue("Should be headless", Neodymium.isHeadless());
        Assert.assertFalse("Should not be recording", FilmTestExecution.isRecording());
        Assert.assertTrue("Should highlight in headless without video when explicit override is enabled",
                          Neodymium.shouldHighlightElements());
    }

    @Test
    public void testHeadlessEnablesHighlightingDuringActiveRecording()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Configuration.headless = true;

        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            HighlightHeadlessOptimizationTest.class.getClassLoader(),
            new Class<?>[] { WebDriver.class, TakesScreenshot.class },
            (proxy, method, args) -> {
                if ("getScreenshotAs".equals(method.getName()))
                {
                    return new byte[0];
                }
                return null;
            });
        final WebDriverStateContainer container = new WebDriverStateContainer();
        container.setWebDriver(mockDriver);
        Neodymium.setWebDriverStateContainer(container);

        Assert.assertFalse("Should not highlight before recording starts",
                           Neodymium.shouldHighlightElements(mockDriver));

        // Enable filming in context and start recording
        FilmTestExecution.getContextVideo().setProperty("video.enableFilming", "true");
        FilmTestExecution.getContextVideo().setProperty("video.format", "mp4");
        FilmTestExecution.startVideoRecording("test-headless-recording-classic");

        try
        {
            Assert.assertTrue("Video recording should be active", FilmTestExecution.isRecording());
            Assert.assertTrue("Recording for mockDriver should be active", FilmTestExecution.isRecording(mockDriver));
            Assert.assertTrue("Should highlight in headless mode when video recording is active",
                              Neodymium.shouldHighlightElements(mockDriver));
        }
        finally
        {
            FilmTestExecution.finishVideoFilming("test-headless-recording-classic", false);
        }

        Assert.assertFalse("Recording should have finished", FilmTestExecution.isRecording());
        Assert.assertFalse("Should return to disabled highlighting after recording finishes",
                           Neodymium.shouldHighlightElements(mockDriver));
    }

    @Test
    public void testIsHeadlessDetectsBrowserConfiguration()
    {
        Configuration.headless = false;

        final BrowserConfiguration browserConfig = new BrowserConfiguration();
        browserConfig.setHeadless(true);

        final WebDriverStateContainer container = new WebDriverStateContainer();
        container.setBrowserConfiguration(browserConfig);
        Neodymium.setWebDriverStateContainer(container);

        Assert.assertTrue("isHeadless() should detect headless state from BrowserConfiguration in WebDriverStateContainer",
                          Neodymium.isHeadless());
    }
}
