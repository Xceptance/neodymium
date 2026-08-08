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
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.neodymium.ai.config.ExecutionMode;
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
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        session.getExecutionContext().getSessionData().putDynamic("assert.test.url", pageUrl, false);
    }

    /**
     * Executes Assert integration test in live, strict replay, and healing replay modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssert.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("assertData")
    public void testAssert(final AiSession session)
    {
        runPlaybook(session, """
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
            """);

        Selenide.Wait().until(d -> "Assert Action Test".equals(d.getTitle()));
        $("#welcome-message").shouldHave(text("Welcome to our web store!"));
        $("#visible-btn").shouldBe(visible);
        $("#hidden-btn").shouldBe(hidden);
        $("#visible-btn").should(exist);
        $("#username").shouldHave(value("JohnDoe"));
        $("#username").shouldBe(focused);
    }

    /**
     * Verifies that incorrect page title assertion throws AssertionError in live replay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/AssertIntegrationTest_testAssertTitleFailure.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertTitleFailure(final AiSession session)
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            runPlaybook(session, """
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
    public void testAssertUrlFailure(final AiSession session)
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            runPlaybook(session, """
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
    public void testAssertTextFailure(final AiSession session)
    {
        Assertions.assertThrows(Throwable.class, () -> 
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
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertVisibilityFailure(final AiSession session)
    {
        Assertions.assertThrows(Throwable.class, () -> 
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
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertInvisibilityFailure(final AiSession session)
    {
        Assertions.assertThrows(Throwable.class, () -> 
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
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testAssertExistenceFailure(final AiSession session)
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            runPlaybook(session, """
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
    public void testAssertFocusFailure(final AiSession session)
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            runPlaybook(session, """
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
    public void testAssertAttributeFailure(final AiSession session)
    {
        Assertions.assertThrows(Throwable.class, () -> 
        {
            runPlaybook(session, """
                steps: |
                  Open ${assert.test.url} in the browser
                  Assert that the 'Username Input' placeholder is 'Invalid Placeholder'
                """);
        });
    }
}


