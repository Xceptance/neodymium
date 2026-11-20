/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.util;

import com.codeborne.selenide.Configuration;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.common.browser.WebDriverStateContainer;
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.common.recording.FilmTestExecution;
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

    @BeforeEach
    public void setUp()
    {
        originalSelenideHeadless = Configuration.headless;
        FilmTestExecution.clearThreadContexts();
        Neodymium.setWebDriverStateContainer(null);
        Neodymium.setBrowserProfileName(null);
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "false");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.headless.withoutVideo", "false");
    }

    @AfterEach
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

        Assertions.assertFalse(Neodymium.shouldHighlightElements(),
                               "Should not highlight if highlight setting is false in headful mode");

        Configuration.headless = true;
        Assertions.assertFalse(Neodymium.shouldHighlightElements(),
                               "Should not highlight if highlight setting is false in headless mode");
    }

    @Test
    public void testHeadfulAlwaysHighlightsWhenEnabled()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Configuration.headless = false;

        Assertions.assertFalse(Neodymium.isHeadless(), "Should be headful when Configuration.headless is false");
        Assertions.assertTrue(Neodymium.shouldHighlightElements(),
                              "Should highlight in headful mode without recording");
    }

    @Test
    public void testHeadlessSkipsHighlightingWithoutRecording()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Configuration.headless = true;

        Assertions.assertTrue(Neodymium.isHeadless(), "Should be headless when Configuration.headless is true");
        Assertions.assertFalse(FilmTestExecution.isRecording(), "Should not be recording initially");
        Assertions.assertFalse(Neodymium.shouldHighlightElements(),
                               "Should disable highlighting in headless mode when no video is recording");
    }

    @Test
    public void testHeadlessAllowsHighlightingWhenOverrideConfigured()
    {
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight", "true");
        Neodymium.configuration().setProperty("neodymium.debugUtils.highlight.headless.withoutVideo", "true");
        Configuration.headless = true;

        Assertions.assertTrue(Neodymium.isHeadless(), "Should be headless");
        Assertions.assertFalse(FilmTestExecution.isRecording(), "Should not be recording");
        Assertions.assertTrue(Neodymium.shouldHighlightElements(),
                              "Should highlight in headless without video when explicit override is enabled");
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

        Assertions.assertFalse(Neodymium.shouldHighlightElements(mockDriver),
                               "Should not highlight before recording starts");

        // Enable filming in context and start recording
        FilmTestExecution.getContextVideo().setProperty("video.enableFilming", "true");
        FilmTestExecution.getContextVideo().setProperty("video.format", "mp4");
        FilmTestExecution.startVideoRecording("test-headless-recording");

        try
        {
            Assertions.assertTrue(FilmTestExecution.isRecording(), "Video recording should be active");
            Assertions.assertTrue(FilmTestExecution.isRecording(mockDriver), "Recording for mockDriver should be active");
            Assertions.assertTrue(Neodymium.shouldHighlightElements(mockDriver),
                                  "Should highlight in headless mode when video recording is active");
        }
        finally
        {
            FilmTestExecution.finishVideoFilming("test-headless-recording", false);
        }

        Assertions.assertFalse(FilmTestExecution.isRecording(), "Recording should have finished");
        Assertions.assertFalse(Neodymium.shouldHighlightElements(mockDriver),
                               "Should return to disabled highlighting after recording finishes");
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

        Assertions.assertTrue(Neodymium.isHeadless(),
                              "isHeadless() should detect headless state from BrowserConfiguration in WebDriverStateContainer");
    }

    @Test
    public void testMatchesDecoratedDriverProxy()
    {
        final WebDriver rawDriver = (WebDriver) Proxy.newProxyInstance(
            HighlightHeadlessOptimizationTest.class.getClassLoader(),
            new Class<?>[] { WebDriver.class },
            (proxy, method, args) -> null);
        final WebDriver decoratedDriver = (WebDriver) Proxy.newProxyInstance(
            HighlightHeadlessOptimizationTest.class.getClassLoader(),
            new Class<?>[] { WebDriver.class },
            (proxy, method, args) -> null);

        final WebDriverStateContainer container = new WebDriverStateContainer();
        container.setWebDriver(rawDriver);
        container.setDecoratedWebDriver(decoratedDriver);

        Assertions.assertTrue(container.matches(rawDriver), "Should match raw driver");
        Assertions.assertTrue(container.matches(decoratedDriver), "Should match decorated driver");
        Assertions.assertFalse(container.matches(null), "Should not match null");

        final WebDriver foreignDriver = (WebDriver) Proxy.newProxyInstance(
            HighlightHeadlessOptimizationTest.class.getClassLoader(),
            new Class<?>[] { WebDriver.class },
            (proxy, method, args) -> null);
        Assertions.assertFalse(container.matches(foreignDriver), "Should not match foreign driver");
    }
}
