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
 * A mock page analyzer running offline returning canned DOM contents and screenshots.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class MockPageAnalyzer extends PageAnalyzer
{
    private final List<ContextLevel> requestedContextLevels = Collections.synchronizedList(new ArrayList<>());
    private final List<String> requestedScreenshotTitles = Collections.synchronizedList(new ArrayList<>());
    private String mockDomText;
    private String mockScreenshotBase64;

    public MockPageAnalyzer(final String mockDomText)
    {
        this(mockDomText, "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    }

    public MockPageAnalyzer(final String mockDomText, final String mockScreenshotBase64)
    {
        super();
        this.mockDomText = mockDomText;
        this.mockScreenshotBase64 = mockScreenshotBase64;
    }

    public final List<ContextLevel> getRequestedContextLevels()
    {
        return this.requestedContextLevels;
    }

    public final List<String> getRequestedScreenshotTitles()
    {
        return this.requestedScreenshotTitles;
    }

    public final void setMockDomText(final String mockDomText)
    {
        this.mockDomText = mockDomText;
    }

    public final void setMockScreenshotBase64(final String mockScreenshotBase64)
    {
        this.mockScreenshotBase64 = mockScreenshotBase64;
    }

    @Override
    public final String captureScreenshot(final String title) throws IOException
    {
        this.requestedScreenshotTitles.add(title);
        return this.mockScreenshotBase64;
    }

    @Override
    public final String captureSimplifiedDom(final ContextLevel level)
    {
        this.requestedContextLevels.add(level);
        return this.mockDomText;
    }

    @Override
    public final String getPageContext(final ContextLevel level)
    {
        return captureSimplifiedDom(level);
    }
}
