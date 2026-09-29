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
 * Live LLM integration test for the Dynamic Virtualized Lists &amp; Infinite Feeds sandbox challenge.
 * Verifies that the live AI agent iteratively discovers unmounted virtual list items, scrolls containers,
 * and interacts with dynamically mounted elements.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_virtualized_list_playbook")
public class VirtualizedListSandboxLiveTest extends BaseAiTest
{
    /**
     * Constructs a default VirtualizedListSandboxLiveTest.
     */
    public VirtualizedListSandboxLiveTest()
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
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/virtualized-list.html", server.getPort());
        session.data().putDynamic("virtual.list.test.url", pageUrl, false);
    }

    /**
     * Tests virtual list interaction using live LLM across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testVirtualizedListLive(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${virtual.list.test.url} in the browser
              Scroll down the virtual list #virtual-list-container
              Click the Select button for Product #42
              Verify that the selected product status shows "Selected: Product #42: Quantum Sensor"
              Click the Confirm Selection button
              Verify that the confirmation result shows "Order confirmed for Product #42: Quantum Sensor!"
            """);

        $("#confirmation-result").shouldHave(text("Order confirmed for Product #42: Quantum Sensor!"));
    }
}
