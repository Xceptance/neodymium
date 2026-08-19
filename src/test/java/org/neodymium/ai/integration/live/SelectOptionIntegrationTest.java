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
import org.junit.jupiter.api.Test;
import org.neodymium.ai.executor.selenide.ContextLevel;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;

import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;

/**
 * Integration test verifying structured select and option element assertions,
 * option selections, and option state validations.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("integration")
@NeodymiumAiTest
public class SelectOptionIntegrationTest extends BaseAiTest
{

    /**
     * Set up test page URL before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/SelectOptionTest.html", server.getPort());
        session.data().putDynamic("select.test.url", pageUrl, false);
    }

    /**
     * Verifies asserting selected option state by option element ID.
     *
     * @param session the thread-isolated AiSession
     */
    @Test
    @AiPlaybook("/playbooks/integration/programmatic/SelectOptionIntegrationTest_testAssertOptionById.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("selectData")
    public void testAssertOptionById(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: selectData
            steps: |
              Open ${select.test.url} in the browser
              Assert that the 'opt-de' option is selected
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onLive(m -> m.hasStandardCalls(2).hasPesapCalls(2).hasContextLevelCount(ContextLevel.MINIMAL, 2))
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#opt-de").shouldBe(selected);
    }

    /**
     * Verifies selecting an option by value and asserting the dropdown value.
     *
     * @param session the thread-isolated AiSession
     */
    @Test
    @AiPlaybook("/playbooks/integration/programmatic/SelectOptionIntegrationTest_testSelectOptionByValue.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("selectData")
    public void testSelectOptionByValue(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: selectData
            steps: |
              Open ${select.test.url} in the browser
              Select option 'France' in the 'country-select' dropdown
              Assert that the 'country-select' dropdown value is 'FR'
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onLive(m -> m.hasStandardCalls(3).hasPesapCalls(3).hasContextLevelCount(ContextLevel.MINIMAL, 3))
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#country-select").shouldHave(value("FR"));
        $("#opt-fr").shouldBe(selected);
    }

    /**
     * Verifies asserting a disabled option state.
     *
     * @param session the thread-isolated AiSession
     */
    @Test
    @AiPlaybook("/playbooks/integration/programmatic/SelectOptionIntegrationTest_testAssertDisabledOption.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("selectData")
    public void testAssertDisabledOption(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: selectData
            steps: |
              Open ${select.test.url} in the browser
              Assert that the 'opt-dis' option is disabled
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onLive(m -> m.hasStandardCalls(2).hasPesapCalls(2).hasContextLevelCount(ContextLevel.MINIMAL, 2))
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#opt-dis").shouldBe(disabled);
    }

    /**
     * Verifies multi-select dropdown option assertions.
     *
     * @param session the thread-isolated AiSession
     */
    @Test
    @AiPlaybook("/playbooks/integration/programmatic/SelectOptionIntegrationTest_testMultiSelectOptions.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("selectData")
    public void testMultiSelectOptions(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: selectData
            steps: |
              Open ${select.test.url} in the browser
              Assert that the 'opt-tech' option is selected
              Assert that the 'opt-music' option is selected
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .hasNoEscalations()
            .onLive(m -> m.hasStandardCalls(3).hasPesapCalls(3).hasContextLevelCount(ContextLevel.MINIMAL, 3))
            .onStrictReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#opt-tech").shouldBe(selected);
        $("#opt-music").shouldBe(selected);
    }
}
