/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.ai.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.core.PageAnalyzer;

/**
 * A mock implementation of {@link PageAnalyzer} designed for browserless, offline execution testing.
 * <p>
 * Instead of extracting the HTML DOM structure or capturing screenshots from a live Selenium browser session,
 * this analyzer returns pre-configured static HTML DOM context strings and Base64 screenshots.
 * It also records every context level and screenshot title requested by the AI agent to facilitate assertions 
 * on the agent's prompt context escalations (e.g. AXTREE vs LEAN vs VISUAL).
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class MockPageAnalyzer extends PageAnalyzer
{
    /**
     * Thread-safe list recording the sequence of {@link ContextLevel}s requested by the AI agent.
     */
    private final List<ContextLevel> requestedContextLevels = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Thread-safe list recording the sequence of screenshot titles requested by the AI agent.
     */
    private final List<String> requestedScreenshotTitles = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Static HTML DOM content returned to the AI agent during DOM simplification requests.
     */
    private String mockDomText;
    
    /**
     * Static Base64 screenshot returned to the AI agent during visual screenshot requests.
     */
    private String mockScreenshotBase64;

    /**
     * Constructs a MockPageAnalyzer with a default 1x1 pixel Base64 transparent PNG screenshot.
     *
     * @param mockDomText the static mock HTML DOM text to serve
     */
    public MockPageAnalyzer(final String mockDomText)
    {
        this(mockDomText, "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    }

    /**
     * Constructs a MockPageAnalyzer with a custom mock HTML DOM text and Base64 screenshot.
     *
     * @param mockDomText           the static mock HTML DOM text to serve
     * @param mockScreenshotBase64 the custom Base64 PNG image context
     */
    public MockPageAnalyzer(final String mockDomText, final String mockScreenshotBase64)
    {
        super();
        this.mockDomText = mockDomText;
        this.mockScreenshotBase64 = mockScreenshotBase64;
    }

    /**
     * Retrieves the list of requested context levels for test assertions.
     *
     * @return the list of context levels
     */
    public final List<ContextLevel> getRequestedContextLevels()
    {
        return this.requestedContextLevels;
    }

    /**
     * Retrieves the list of requested screenshot titles for test assertions.
     *
     * @return the list of screenshot titles
     */
    public final List<String> getRequestedScreenshotTitles()
    {
        return this.requestedScreenshotTitles;
    }

    /**
     * Dynamically updates the mock DOM text served during the test.
     *
     * @param mockDomText the new mock HTML DOM text
     */
    public final void setMockDomText(final String mockDomText)
    {
        this.mockDomText = mockDomText;
    }

    /**
     * Dynamically updates the mock Base64 screenshot served during the test.
     *
     * @param mockScreenshotBase64 the new mock Base64 PNG image context
     */
    public final void setMockScreenshotBase64(final String mockScreenshotBase64)
    {
        this.mockScreenshotBase64 = mockScreenshotBase64;
    }

    /**
     * Captures a simulated visual screenshot of the current page.
     * Logs the screenshot title and returns the configured Base64 image context.
     *
     * @param title the title descriptor of the screenshot
     * @return the Base64 image context string
     * @throws IOException never thrown by the mock implementation
     */
    @Override
    public final String captureScreenshot(final String title) throws IOException
    {
        this.requestedScreenshotTitles.add(title);
        return this.mockScreenshotBase64;
    }

    /**
     * Captures a simplified DOM structure corresponding to the requested {@link ContextLevel}.
     * Logs the requested level and returns the mock DOM text.
     *
     * @param level the requested detail level
     * @return the mock HTML DOM context string
     */
    @Override
    public final String captureSimplifiedDom(final ContextLevel level)
    {
        this.requestedContextLevels.add(level);
        return this.mockDomText;
    }

    /**
     * Retrieves the simplified DOM or screenshot context corresponding to the requested {@link ContextLevel}.
     *
     * @param level the requested detail level
     * @return the mock HTML DOM context string
     */
    @Override
    public final String getPageContext(final ContextLevel level)
    {
        return captureSimplifiedDom(level);
    }
}
