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
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiLinter;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for the CHECK action plugin verifying checkbox and radio button
 * state management, idempotency, form aggregation, and error handling.
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
public class CheckIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URL before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/CheckActionTest/testCheckHappyPath.html", server.getPort());
        session.data().putDynamic("check.test.url", pageUrl, false);
    }

    /**
     * Verifies checking an unchecked checkbox by its accessible label.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/CheckIntegrationTest_testCheckCheckbox.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testCheckCheckbox(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${check.test.url} in the browser
              Check the 'Subscribe to newsletter' checkbox
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#newsletter").shouldBe(selected);
    }

    /**
     * Verifies checking a radio button in a radio button group by accessible label.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/CheckIntegrationTest_testCheckRadioButton.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testCheckRadioButton(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${check.test.url} in the browser
              Check the 'Email' radio button
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#contact-email").shouldBe(selected);
        $("#contact-phone").shouldNotBe(selected);
    }

    /**
     * Verifies that checking an already-checked checkbox is idempotent and does not toggle the checkbox off.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/CheckIntegrationTest_testCheckIdempotency.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testCheckIdempotency(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${check.test.url} in the browser
              Check the 'Subscribe to newsletter' checkbox
              Check the 'Subscribe to newsletter' checkbox
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#newsletter").shouldBe(selected);
    }

    /**
     * Verifies checking both a checkbox and a radio button, submitting the form, and asserting combined result state.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/CheckIntegrationTest_testCheckAndSubmit.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testCheckAndSubmit(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${check.test.url} in the browser
              Check the 'Subscribe to newsletter' checkbox
              Check the 'Phone' radio button
              Click #btn-submit
            """)
            .verifyMetrics()
            .hasStepCount(4)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#newsletter").shouldBe(selected);
        $("#contact-phone").shouldBe(selected);
        $("#result").shouldHave(exactText("Newsletter: true, Contact: phone"));
    }

    /**
     * Verifies that attempting to check a non-existent element raises an exception.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/CheckIntegrationTest_testCheckNonExistentElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testCheckNonExistentElementFailure(final AiSession session)
    {
        assertThrows(Throwable.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${check.test.url} in the browser
                  Check the checkbox #missing-checkbox-target
                """);
        });
    }

    /**
     * Verifies that attempting to check a disabled element raises an exception.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/CheckIntegrationTest_testCheckDisabledElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testCheckDisabledElementFailure(final AiSession session)
    {
        assertThrows(Throwable.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${check.test.url} in the browser
                  Check the checkbox #disabled-box
                """);
        });
    }
}
