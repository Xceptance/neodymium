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
package org.neodymium.ai.integration.sandbox.live;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live LLM integration test for the Dual-Thumb Range Sliders &amp; Drag Gestures sandbox challenge.
 * Verifies that the live AI agent perceives custom slider handles and draggable lists, executes coordinate
 * drags and drop gestures, and continues autonomous test execution.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_range_slider_playbook")
public class RangeSliderSandboxLiveTest extends BaseAiTest
{
    /**
     * Constructs a default RangeSliderSandboxLiveTest.
     */
    public RangeSliderSandboxLiveTest()
    {
    }

    /**
     * Set up test page URL dynamically before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/range-slider.html", server.getPort());
        session.data().putDynamic("range.slider.test.url", pageUrl, false);
    }

    /**
     * Tests range slider coordinate dragging and priority list reordering using live LLM across execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testRangeSliderLive(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${range.slider.test.url} in the browser
              Drag the minimum price slider thumb right by 60 pixels
              Verify that the min price displays "$150"
              Drag the maximum price slider thumb left by 60 pixels
              Verify that the max price displays "$300"
              Verify that the price range displays "$150 - $300"
              Drag the Requirements Analysis item and drop it onto the Automated Testing item
              Verify that the priority order status contains "Requirements Analysis"
            """);

        $("#price-range-display").shouldHave(text("$150 - $300"));
        $("#reorder-status").shouldHave(text("Requirements Analysis"));
    }
}
