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

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.selected;
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
 * Live integration test for the CLEAR action plugin.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class ClearIntegrationTest extends BaseAiTest
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
        session.data().putDynamic("clear.test.url", pageUrl, false);
    }

    /**
     * Executes Clear integration test in both live and strict replay modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testClear(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${clear.test.url} in the browser
              Type 'initial text' into #first-name
              Clear #first-name
              # clear something already cleared works
              Clear #first-name
            """);

        $("#first-name").shouldBe(empty);
    }

    /**
     * Verifies that attempting to clear a disabled input field raises an exception.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClearIntegrationTest_testClearDisabledElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testClearDisabledElementFailure(final AiSession session)
    {
        assertThrows(Throwable.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${clear.test.url} in the browser
                  Clear #disabled-input
                """);
        });
    }

    /**
     * Verifies that attempting to clear a non-existent element raises an exception.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClearIntegrationTest_testClearNonExistentElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testClearNonExistentElementFailure(final AiSession session)
    {
        assertThrows(Throwable.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${clear.test.url} in the browser
                  Clear #missing-input-field
                """);
        });
    }

    /**
     * Verifies that attempting to clear a readonly input field raises an exception.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClearIntegrationTest_testClearReadonlyElementFailure.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testClearReadonlyElementFailure(final AiSession session)
    {
        assertThrows(Throwable.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${clear.test.url} in the browser
                  Clear #readonly-input
                """);
        });
    }

    /**
     * Verifies that clearing a checked checkbox unchecks it.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClearIntegrationTest_testClearCheckbox.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testClearCheckbox(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${clear.test.url} in the browser
              Clear #sample-checkbox
            """);

        $("#sample-checkbox").shouldNotBe(selected);
    }
}
