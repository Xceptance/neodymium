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

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.session.AiSession;

/**
 * Live integration test for the ASSERT action plugin.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
public class AssertIntegrationTest extends BaseAiTest
{

    /**
     * Set up test page URL before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        session.getExecutionContext().getSessionData().putDynamic("assert.test.url", pageUrl, false);
    }

    /**
     * Executes Assert integration test in both live and strict replay modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssert.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    @AiDataSet("assertData")
    public void testAssert(final AiSession session)
    {
        runPlaybook(session, """
            data:
              - testId: assertData
            steps: |
              Open ${assert.test.url} in the browser
              Assert that the page title is 'Assert Action Test'
              Assert that the welcome text 'Welcome to our web store!' is visible
              Assert that the 'Clickable Button' button is visible
              Assert that the hidden 'Secret Button' is invisible
              Assert that the 'Clickable Button' button exists
            """);

        Selenide.Wait().until(d -> "Assert Action Test".equals(d.getTitle()));
        $("#welcome-message").shouldHave(text("Welcome to our web store!"));
        $("#visible-btn").shouldBe(visible);
        $("#hidden-btn").shouldBe(hidden);
        $("#visible-btn").should(exist);
    }

    /**
     * Verifies that incorrect page title assertion throws AssertionError in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertTitleFailure.yaml")
    @AiMode({ExecutionMode.REPLAY_STRICT})
    public void testAssertTitleFailure(final AiSession session)
    {
        org.junit.jupiter.api.Assertions.assertThrows(Throwable.class, () -> 
        {
            runPlaybook(session, """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that the page title is 'Incorrect Title'
                """);
        });
    }

    /**
     * Verifies that incorrect element text assertion throws error in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertTextFailure.yaml")
    @AiMode({ExecutionMode.REPLAY_STRICT})
    public void testAssertTextFailure(final AiSession session)
    {
        org.junit.jupiter.api.Assertions.assertThrows(Throwable.class, () -> 
        {
            runPlaybook(session, """
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
    @AiMode({ExecutionMode.REPLAY_STRICT})
    public void testAssertVisibilityFailure(final AiSession session)
    {
        org.junit.jupiter.api.Assertions.assertThrows(Throwable.class, () -> 
        {
            runPlaybook(session, """
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
    @AiMode({ExecutionMode.REPLAY_STRICT})
    public void testAssertInvisibilityFailure(final AiSession session)
    {
        org.junit.jupiter.api.Assertions.assertThrows(Throwable.class, () -> 
        {
            runPlaybook(session, """
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
    @AiMode({ExecutionMode.REPLAY_STRICT})
    public void testAssertExistenceFailure(final AiSession session)
    {
        org.junit.jupiter.api.Assertions.assertThrows(Throwable.class, () -> 
        {
            runPlaybook(session, """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that the non-existent button exists
                """);
        });
    }
}
