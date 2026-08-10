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

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.readonly;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.executor.selenide.ContextLevel;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for the ASSERT action plugin.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@NeodymiumAiTest
public class AssertIntegrationTest extends BaseAiTest
{

    /**
     * Set up test page URL before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        session.data().putDynamic("assert.test.url", pageUrl, false);
    }

    /**
     * Executes all Assert integration steps sequentially in a single test case to check for cross-step dependencies.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertAll.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertAll(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that currentUrl contains 'testAssertHappyPath.html'
              Assert that the pageTitle is 'Assert Action Test'
              Assert that the welcome text matches '/Welcome.*store!/'
              Assert that the welcome text 'Welcome to our web store!' is visible
              Assert that the 'Clickable Button' button is present
              Assert that the hidden 'Secret Button' is absent
              Assert that the 'Clickable Button' button exists
              Assert that the 'Username Input' value is 'JohnDoe'
              Assert that the 'Username Input' placeholder is 'Enter username'
              Click the 'Username Input' field
              Assert that the 'Username Input' field is focused
              Assert that the 'newsletter-opt' checkbox is checked
              Assert that the 'terms-opt' checkbox is unchecked
              Assert that the 'plan-monthly' radio button is checked
              Assert that the 'plan-yearly' radio button is unchecked
              Assert that the 'disabled-input' field is disabled
              Assert that the 'enabled-input' field is enabled
              Assert that the 'opt-user' option is selected
              Assert that the 'readonly-input' field is readonly
            """);

        Selenide.Wait().until(d -> "Assert Action Test".equals(d.getTitle()));
        $("#welcome-message").shouldHave(text("Welcome to our web store!"));
        $("#visible-btn").shouldBe(visible);
        $("#hidden-btn").shouldBe(hidden);
        $("#visible-btn").should(exist);
        $("#username").shouldHave(value("JohnDoe"));
        $("#username").shouldBe(focused);
        $("#newsletter-opt").shouldBe(checked);
        $("#terms-opt").shouldNotBe(checked);
        $("#plan-monthly").shouldBe(checked);
        $("#plan-yearly").shouldNotBe(checked);
        $("#disabled-input").shouldBe(disabled);
        $("#enabled-input").shouldBe(enabled);
        $("#opt-user").shouldBe(selected);
        $("#readonly-input").shouldBe(readonly);
    }

    /**
     * Sliced test case verifying URL assertion.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertUrl.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertUrl(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that currentUrl contains 'testAssertHappyPath.html'
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onLive(m -> m.hasStandardCalls(2).hasPesapCalls(2).hasContextLevelCount(ContextLevel.MINIMAL, 2))
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());
    }

    /**
     * Sliced test case verifying page title assertion.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertTitle.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertTitle(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the pageTitle is 'Assert Action Test'
              Assert that the page title is 'Assert Action Test'
              Assert that the title of the page is 'Assert Action Test'
              Page title == 'Assert Action Test'
            """)
            .verifyMetrics()
            .hasStepCount(5)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        Selenide.Wait().until(d -> "Assert Action Test".equals(d.getTitle()));
    }

    /**
     * Sliced test case verifying regex match assertion.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertRegex.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertRegex(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the welcome text matches '/Welcome.*store!/'
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#welcome-message").shouldHave(text("Welcome to our web store!"));
    }

    /**
     * Sliced test case verifying element visibility and absence assertions.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertVisibility.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertVisibility(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the welcome text 'Welcome to our web store!' is visible
              Assert that the hidden 'Secret Button' is absent
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#visible-btn").shouldBe(visible);
        $("#hidden-btn").shouldBe(hidden);
    }

    /**
     * Sliced test case verifying element presence and existence assertions.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertExistence.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertExistence(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the 'Clickable Button' button is present
              Assert that the 'Clickable Button' button exists
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#visible-btn").should(exist);
    }

    /**
     * Sliced test case verifying element value and placeholder attribute assertions.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertAttributes.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertAttributes(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the 'Username Input' value is 'JohnDoe'
              Assert that the 'Username Input' placeholder is 'Enter username'
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onLive(m -> m.hasStandardCalls(3).hasPesapCalls(3).hasContextLevelCount(ContextLevel.MINIMAL, 3))
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#username").shouldHave(value("JohnDoe"));
    }

    /**
     * Sliced test case verifying element click interaction and focus assertion.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertFocus.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertFocus(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Click the 'Username Input' field
              Assert that the 'Username Input' field is focused
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onLive(m -> m.hasStandardCalls(3).hasPesapCalls(3).hasContextLevelCount(ContextLevel.MINIMAL, 3))
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#username").shouldBe(focused);
    }

    /**
     * Sliced test case verifying checkbox checked and unchecked state assertions.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertCheckboxState.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertCheckboxState(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the 'newsletter-opt' checkbox is checked
              Assert that the 'terms-opt' checkbox is unchecked
              Assert that the 'newsletter-opt' checkbox is true
              Assert that the 'terms-opt' checkbox is false
            """)
            .verifyMetrics()
            .hasStepCount(5)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#newsletter-opt").shouldBe(checked);
        $("#terms-opt").shouldNotBe(checked);
    }

    /**
     * Sliced test case verifying radio button checked and unchecked state assertions.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertRadioButtonState.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertRadioButtonState(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the 'plan-monthly' radio button is checked
              Assert that the 'plan-yearly' radio button is unchecked
              Assert that the 'plan-monthly' radio button is true
              Assert that the 'plan-yearly' radio button is false
            """)
            .verifyMetrics()
            .hasStepCount(5)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#plan-monthly").shouldBe(checked);
        $("#plan-yearly").shouldNotBe(checked);
    }

    /**
     * Sliced test case verifying element disabled and enabled state assertions.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertDisabledState.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertDisabledState(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the 'disabled-input' field is disabled
              Assert that the 'enabled-input' field is enabled
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onLive(m -> m.hasStandardCalls(3).hasPesapCalls(3).hasContextLevelCount(ContextLevel.MINIMAL, 3))
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#disabled-input").shouldBe(disabled);
        $("#enabled-input").shouldBe(enabled);
    }

    /**
     * Sliced test case verifying select option state assertion.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertSelectedState.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertSelectedState(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the 'opt-user' option is selected
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onLive(m -> m.hasStandardCalls(2).hasPesapCalls(2).hasContextLevelCount(ContextLevel.MINIMAL, 2))
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#opt-user").shouldBe(selected);
    }

    /**
     * Sliced test case verifying input readonly state assertion.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertReadonlyState.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssertReadonlyState(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the 'readonly-input' field is readonly
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onLive(m -> m.hasStandardCalls(2).hasPesapCalls(2).hasContextLevelCount(ContextLevel.MINIMAL, 2))
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#readonly-input").shouldBe(readonly);
    }

    /**
     * Verifies that incorrect page title assertion throws AssertionError in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertTitleFailure.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertTitleFailure(final AiSession session) throws Exception
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            session.execute( """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that the page title is 'Incorrect Title'
                """);
        });
    }

    /**
     * Verifies that incorrect URL assertion throws AssertionError in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertUrlFailure.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertUrlFailure(final AiSession session) throws Exception
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            session.execute( """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that currentUrl contains 'nonexistent-page.html'
                """);
        });
    }

    /**
     * Verifies that incorrect element text assertion throws error in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertTextFailure.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertTextFailure(final AiSession session) throws Exception
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            session.execute( """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that the welcome text 'Goodbye!' is visible
                """);
        });
    }

    /**
     * Verifies that visible hidden element assertion throws error in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertVisibilityFailure.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertVisibilityFailure(final AiSession session) throws Exception
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            session.execute( """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that the hidden 'Secret Button' is visible
                """);
        });
    }

    /**
     * Verifies that hidden visible element assertion throws error in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertInvisibilityFailure.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertInvisibilityFailure(final AiSession session) throws Exception
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            session.execute( """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that the 'Clickable Button' button is hidden
                """);
        });
    }

    /**
     * Verifies that existing non-existent element assertion throws error in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertExistenceFailure.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertExistenceFailure(final AiSession session) throws Exception
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            session.execute( """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that the non-existent button exists
                """);
        });
    }

    /**
     * Verifies that focused unfocused element assertion throws error in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertFocusFailure.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertFocusFailure(final AiSession session) throws Exception
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            session.execute( """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that the 'Clickable Button' button is focused
                """);
        });
    }

    /**
     * Verifies that incorrect element attribute assertion throws error in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertAttributeFailure.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertAttributeFailure(final AiSession session) throws Exception
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            session.execute( """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that the 'Username Input' placeholder is 'Invalid Placeholder'
                """);
        });
    }
}


