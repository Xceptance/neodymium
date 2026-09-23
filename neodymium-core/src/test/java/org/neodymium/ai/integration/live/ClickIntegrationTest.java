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
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
 * Live integration test for the CLICK action plugin verifying buttons, anchor links,
 * SVG icons, and error handling for missing and disabled elements.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class ClickIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URLs before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String standardUrl = String.format("http://localhost:%d/ClickActionTest/testClickStandardButton.html", server.getPort());
        final String anchorUrl = String.format("http://localhost:%d/ClickActionTest/testClickAnchorLink.html", server.getPort());
        final String svgUrl = String.format("http://localhost:%d/ClickActionTest/testClickSvgIcon.html", server.getPort());
        final String disabledUrl = String.format("http://localhost:%d/ClickActionTest/testClickDisabledButton.html", server.getPort());

        session.data().putDynamic("click.test.standard.url", standardUrl, false);
        session.data().putDynamic("click.test.anchor.url", anchorUrl, false);
        session.data().putDynamic("click.test.svg.url", svgUrl, false);
        session.data().putDynamic("click.test.disabled.url", disabledUrl, false);
    }

    /**
     * Verifies clicking a standard button targeted by natural language visible text.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClickIntegrationTest_testClickStandardButton.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickStandardButton(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${click.test.standard.url} in the browser
              Click the 'Submit Order' button
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#result").shouldHave(exactText("Order Submitted!"));
    }

    /**
     * Verifies clicking an anchor navigation link targeted by natural language link text.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClickIntegrationTest_testClickAnchorLink.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickAnchorLink(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${click.test.anchor.url} in the browser
              Click the 'Go to Target Destination' link
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#result").shouldHave(exactText("Link Clicked!"));
    }

    /**
     * Verifies clicking a button that contains an embedded SVG icon.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClickIntegrationTest_testClickSvgButton.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClickSvgButton(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${click.test.svg.url} in the browser
              Click the 'Add Item' button
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#result").shouldHave(exactText("SVG Button Clicked!"));
    }

    /**
     * Verifies that attempting to click a non-existent element raises an exception.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClickIntegrationTest_testClickNonExistentElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testClickNonExistentElementFailure(final AiSession session)
    {
        assertThrows(Throwable.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${click.test.standard.url} in the browser
                  Click the button #missing-button
                """);
        });
    }

    /**
     * Verifies that attempting to click a disabled button raises an exception.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClickIntegrationTest_testClickDisabledButtonFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testClickDisabledButtonFailure(final AiSession session)
    {
        assertThrows(Throwable.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${click.test.disabled.url} in the browser
                  Click the 'Disabled Order Button' button
                """);
        });
    }
}
