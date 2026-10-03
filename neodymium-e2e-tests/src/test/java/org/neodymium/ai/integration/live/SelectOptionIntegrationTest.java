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

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$;

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
 * Live integration test for the SELECT action plugin and option element assertions,
 * option selections, multi-select options, and optgroup selections.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
@AiLinter(false)
public class SelectOptionIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URL before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/SelectOptionTest.html", server.getPort());
        session.data().putDynamic("select.test.url", pageUrl, false);
    }

    /**
     * Verifies asserting selected option state by option element ID.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/SelectOptionIntegrationTest_testAssertOptionById.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testAssertOptionById(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${select.test.url} in the browser
              Assert that the 'opt-de' option is selected
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#opt-de").shouldBe(selected);
    }

    /**
     * Verifies selecting an option by value and asserting the dropdown value.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/SelectOptionIntegrationTest_testSelectOptionByValue.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testSelectOptionByValue(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${select.test.url} in the browser
              Select option 'France' in the 'country-select' dropdown
              Assert that the 'country-select' dropdown value is 'FR'
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#country-select").shouldHave(value("FR"));
        $("#opt-fr").shouldBe(selected);
    }

    /**
     * Verifies asserting a disabled option state.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/SelectOptionIntegrationTest_testAssertDisabledOption.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testAssertDisabledOption(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${select.test.url} in the browser
              Assert that the 'opt-dis' option is disabled
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#opt-dis").shouldBe(disabled);
    }

    /**
     * Verifies multi-select dropdown option assertions.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/SelectOptionIntegrationTest_testMultiSelectOptions.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testMultiSelectOptions(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${select.test.url} in the browser
              Assert that the 'opt-tech' option is selected
              Assert that the 'opt-music' option is selected
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#opt-tech").shouldBe(selected);
        $("#opt-music").shouldBe(selected);
    }

    /**
     * Verifies selecting a dropdown option by visible text.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/SelectOptionIntegrationTest_testSelectOptionByVisibleText.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testSelectOptionByVisibleText(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${select.test.url} in the browser
              Select option 'United States' in the 'country-select' dropdown
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#country-select").shouldHave(value("US"));
        $("#opt-us").shouldBe(selected);
    }

    /**
     * Verifies selecting an option nested inside an optgroup.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/SelectOptionIntegrationTest_testSelectOptionInOptgroup.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testSelectOptionInOptgroup(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${select.test.url} in the browser
              Select option 'Tesla' in the 'car-select' dropdown
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#car-select").shouldHave(value("tesla"));
        $("#opt-tesla").shouldBe(selected);
    }
}
