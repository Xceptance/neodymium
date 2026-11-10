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
package org.neodymium.ai.integration.sandbox.live;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

import com.codeborne.selenide.SelenideElement;

/**
 * Live LLM integration test for the multi-level Web Components and Shadow DOM sandbox challenge.
 * Executes live against the configured LLM across 3 execution modes.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_web_components_playbook")
public class WebComponentsSandboxLiveTest extends BaseAiTest
{
    /**
     * Set up test page URL dynamically before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/web-components.html", server.getPort());
        session.data().putDynamic("webcomponents.test.url", pageUrl, false);
    }

    /**
     * Tests Web Components challenge live across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testWebComponentsChallengeLive(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${webcomponents.test.url} in the browser
              Type 'alex.chen@acme-corp.com' into the email address field
              Click the continue button
              Type 'P@ssw0rd2026!' into the password field
              Check the 'Remember this device' switch
              Click the sign in button
              Verify that #portal-status shows "Welcome back, alex.chen@acme-corp.com"
            """);

        $("#portal-status").shouldHave(text("Welcome back, alex.chen@acme-corp.com"));

        final SelenideElement welcomeBanner = SelenideElementFinder.findElement("aura-account-card .welcome-banner");
        Assertions.assertNotNull(welcomeBanner);
        welcomeBanner.shouldHave(text("Welcome back, alex.chen@acme-corp.com"));
    }
}
