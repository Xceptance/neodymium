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
import static com.codeborne.selenide.Selenide.open;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiJudge;
import org.neodymium.ai.junit.AiLinter;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for the HOVER action plugin verifying CSS :hover reveals,
 * delayed hover-intent previews, hover dismissal, synonym phrasings, submenu interactions,
 * and missing element handling on a neutral DOM fixture without lexical hints.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
@AiLinter(false)
@AiJudge({false, true})
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
     * Resets mouse position after each test to prevent lingering hover states.
     */
    @AfterEach
    public void resetHoverState()
    {
        if (WebDriverRunner.hasWebDriverStarted())
        {
            $("body").hover();
        }
    }

    /**
     * Verifies that hovering over a dropdown button reveals the nested submenu content via CSS :hover.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/HoverIntegrationTest_testCssHoverDropdown.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testCssHoverDropdown(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${hover.test.url} in the browser
              Hover over the 'Categories' button
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#categories-dropdown").shouldBe(visible);
    }

    /**
     * Verifies hovering using natural language synonym phrasing over a pure CSS element.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/HoverIntegrationTest_testCssHoverSynonyms.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testCssHoverSynonyms(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${hover.test.url} in the browser
              Move mouse over 'Categories'
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#categories-dropdown").shouldBe(visible);
    }

    /**
     * Verifies that moving the mouse cursor away from the dropdown closes the submenu.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/HoverIntegrationTest_testCssHoverAndHoverAway.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testCssHoverAndHoverAway(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${hover.test.url} in the browser
              Hover over the 'Categories' button
              Hover over the 'Store Catalog' heading
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#categories-dropdown").shouldNotBe(visible);
    }

    /**
     * Verifies hovering over a parent menu and clicking a revealed submenu item.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/HoverIntegrationTest_testCssHoverAndClickSubmenuItem.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testCssHoverAndClickSubmenuItem(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${hover.test.url} in the browser
              Hover over the 'Categories' button
              Click 'Electronics'
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#selection-result").shouldHave(exactText("Selected: Electronics"));
    }

    /**
     * Verifies hovering over an element with delayed hover-intent activity.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/HoverIntegrationTest_testDelayedHoverActivity.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDelayedHoverActivity(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${hover.test.url} in the browser
              Hover over 'Quick Preview'
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#preview-card").shouldBe(visible);
        $("#preview-status").shouldHave(exactText("Preview active"));
    }

    /**
     * Verifies that attempting to hover over a non-existent element raises an exception and leaves page state intact.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/HoverIntegrationTest_testHoverNonExistentElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testHoverNonExistentElementFailure(final AiSession session)
    {
        open((String) session.data().get("hover.test.url"));

        // Verify page element state
        $("#categories-dropdown").shouldNotBe(visible);
        $("#preview-card").shouldNotBe(visible);

        assertThrows(ConclusiveFailureException.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${hover.test.url} in the browser
                  Hover over 'Non-Existent Action'
                """);
        });

        // Verify page elements remain unaffected
        $("#categories-dropdown").shouldNotBe(visible);
        $("#preview-card").shouldNotBe(visible);
    }
}
