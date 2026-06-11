/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
// AI-generated: Gemini 3.5 Flash
package com.xceptance.neodymium.ai.core;
import com.xceptance.neodymium.ai.BaseAiTest;

import org.junit.jupiter.api.Assertions;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.core.PageAnalyzer;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Integration tests for PageAnalyzer frameId detection logic.
 * Verifies that frameId attributes/headers are omitted on pages with only one frame
 * and included on pages with multiple frames.
 *
 * @author AI-generated: Gemini 3.5 Flash
 */
@Browser("Chrome_1024x768")
public class PageAnalyzerFrameDetectionTest extends BaseAiTest
{
    /**
     * Verifies that frameId attributes and headers are omitted when only one frame exists.
     */
    @NeodymiumTest
    public final void testSingleFrameOmission()
    {
        final String targetUrl = String.format("http://localhost:%d/ClickActionTest/testClickStandardButton.html", server.getPort());
        Selenide.open(targetUrl);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String dom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);

        Assertions.assertNotNull(dom);
        Assertions.assertFalse(dom.contains("frameId="), "DOM should not contain frameId attribute for single frame");
        Assertions.assertFalse(dom.contains("Frame:"), "DOM should not contain Frame info headers for single frame");
    }

    /**
     * Verifies that frameId attributes and headers are included when multiple frames exist.
     */
    @NeodymiumTest
    public final void testMultiFrameInclusion()
    {
        final String targetUrl = String.format("http://localhost:%d/NestedFrameTest/testNestedFrames.html", server.getPort());
        Selenide.open(targetUrl);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String dom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);

        Assertions.assertNotNull(dom);
        Assertions.assertTrue(dom.contains("frameId="), "DOM should contain frameId attribute for multi-frame page");
        Assertions.assertTrue(dom.contains("Frame:"), "DOM should contain Frame info headers for multi-frame page");
    }
}
