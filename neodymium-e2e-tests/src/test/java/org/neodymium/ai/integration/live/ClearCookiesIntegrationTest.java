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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import org.openqa.selenium.Cookie;

/**
 * Live integration test for the CLEAR_COOKIES action plugin verifying complete cookie
 * lifecycle purging and safe execution on clean sessions.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class ClearCookiesIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URL before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        session.data().putDynamic("clearCookies.test.url", pageUrl, false);
    }

    /**
     * Verifies that clearing cookies wipes existing active browser cookies.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClearCookiesIntegrationTest_testClearCookiesWithActiveSession.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClearCookiesWithActiveSession(final AiSession session) throws Exception
    {
        final String pageUrl = (String) session.data().get("clearCookies.test.url");
        WebDriverRunner.getWebDriver().get(pageUrl);
        WebDriverRunner.getWebDriver().manage().addCookie(new Cookie("neoTestCookie", "activeSessionValue123"));
        assertFalse(WebDriverRunner.getWebDriver().manage().getCookies().isEmpty(), "Active cookie should exist prior to clear");

        session.execute( """
            steps: |
              Open ${clearCookies.test.url} in the browser
              Clear all cookies
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        assertTrue(WebDriverRunner.getWebDriver().manage().getCookies().isEmpty(), "All cookies should be wiped after clear action");
    }

    /**
     * Verifies that clearing cookies when no cookies exist executes safely as a clean no-op.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ClearCookiesIntegrationTest_testClearCookiesWhenEmpty.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testClearCookiesWhenEmpty(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${clearCookies.test.url} in the browser
              Clear all cookies
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        assertTrue(WebDriverRunner.getWebDriver().manage().getCookies().isEmpty(), "Cookies should remain empty");
    }
}
