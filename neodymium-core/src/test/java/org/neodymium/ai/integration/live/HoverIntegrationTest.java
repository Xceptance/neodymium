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
package org.neodymium.ai.integration.live;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for the HOVER action plugin verifying dropdown reveals,
 * submenu interactions, and missing element handling.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class HoverIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URL before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/HoverActionTest/testHoverDropdown.html", server.getPort());
        session.data().putDynamic("hover.test.url", pageUrl, false);
    }

    /**
     * Verifies that hovering over a dropdown menu reveals the nested submenu content.
     *
     * @param session the thread-isolated AiSession
     */
    @Test
    @AiPlaybook("/playbooks/integration/programmatic/HoverIntegrationTest_testHoverDropdown.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testHoverDropdown(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${hover.test.url} in the browser
              Hover over the 'Hover Me' button
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $(".dropdown-content").shouldBe(visible);
    }

    /**
     * Verifies hovering over a parent menu and clicking a revealed submenu item.
     *
     * @param session the thread-isolated AiSession
     */
    @Test
    @AiPlaybook("/playbooks/integration/programmatic/HoverIntegrationTest_testHoverAndClickSubmenuItem.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testHoverAndClickSubmenuItem(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${hover.test.url} in the browser
              Hover over the 'Hover Me' button
              Click 'Sub Item 1'
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#result").shouldHave(exactText("Dropdown Item Clicked!"));
    }

    /**
     * Verifies that attempting to hover over a non-existent element raises an exception.
     *
     * @param session the thread-isolated AiSession
     */
    @Test
    @AiPlaybook("/playbooks/integration/programmatic/HoverIntegrationTest_testHoverNonExistentElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testHoverNonExistentElementFailure(final AiSession session)
    {
        assertThrows(Throwable.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${hover.test.url} in the browser
                  Hover over the 'Non-Existent Menu' button
                """);
        });
    }
}
