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
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiLinter;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for the TYPE action plugin verifying single-line text input,
 * multiline textarea input, sequential form filling and submission, natural language
 * semantic locators, value overwriting, and negative failure handling on disabled,
 * readonly, and non-existent inputs.
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
public class TypeIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URL before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/TypeActionTest/testTypeHappyPath.html", server.getPort());
        session.data().putDynamic("type.test.url", pageUrl, false);
    }

    /**
     * Verifies typing into a standard text input field targeted by element ID.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/TypeIntegrationTest_testType.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testType(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${type.test.url} in the browser
              Type 'John' into #first-name
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#first-name").shouldHave(value("John"));
    }

    /**
     * Verifies typing multiline text into a textarea element.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/TypeIntegrationTest_testTypeTextarea.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testTypeTextarea(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${type.test.url} in the browser
              Type 'Detailed automated feedback notes.' into #comments
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#comments").shouldHave(value("Detailed automated feedback notes."));
    }

    /**
     * Verifies sequential typing into multiple input elements followed by form submission.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/TypeIntegrationTest_testTypeSequentialFormFillAndSubmit.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testTypeSequentialFormFillAndSubmit(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${type.test.url} in the browser
              Type 'Alice' into #first-name
              Type 'Great experience' into #comments
              Click #btn-submit
            """)
            .verifyMetrics()
            .hasStepCount(4)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#first-name").shouldHave(value("Alice"));
        $("#comments").shouldHave(value("Great experience"));
        $("#result").shouldHave(exactText("Submitted: Alice - Great experience"));
    }

    /**
     * Verifies resolving and typing into fields using natural language labels and placeholders.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/TypeIntegrationTest_testTypeNaturalLanguageLocators.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testTypeNaturalLanguageLocators(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${type.test.url} in the browser
              Type 'Bob' into the 'First Name' field
              Type 'Feedback via placeholder' into the 'Enter comments' field
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#first-name").shouldHave(value("Bob"));
        $("#comments").shouldHave(value("Feedback via placeholder"));
    }

    /**
     * Verifies that subsequent typing into a field replaces the previous value.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/TypeIntegrationTest_testTypeOverwriteExistingValue.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testTypeOverwriteExistingValue(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${type.test.url} in the browser
              Type 'Initial' into #first-name
              Type 'Replacement' into #first-name
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#first-name").shouldHave(value("Replacement"));
    }

    /**
     * Verifies that attempting to type into a disabled input field raises an exception and leaves value intact.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/TypeIntegrationTest_testTypeDisabledElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testTypeDisabledElementFailure(final AiSession session)
    {
        assertThrows(ConclusiveFailureException.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${type.test.url} in the browser
                  Type 'Blocked' into #disabled-input
                """);
        });

        $("#disabled-input").shouldHave(value("Cannot clear"));
    }

    /**
     * Verifies that attempting to type into a readonly input field raises an exception and leaves value intact.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/TypeIntegrationTest_testTypeReadonlyElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testTypeReadonlyElementFailure(final AiSession session)
    {
        assertThrows(ConclusiveFailureException.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${type.test.url} in the browser
                  Type 'Readonly update' into #readonly-input
                """);
        });

        $("#readonly-input").shouldHave(value("Cannot clear readonly"));
    }

    /**
     * Verifies that attempting to type into a non-existent element raises an exception.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/TypeIntegrationTest_testTypeNonExistentElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testTypeNonExistentElementFailure(final AiSession session)
    {
        assertThrows(ConclusiveFailureException.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${type.test.url} in the browser
                  Type 'Ghost' into #missing-input-field
                """);
        });
    }
}
