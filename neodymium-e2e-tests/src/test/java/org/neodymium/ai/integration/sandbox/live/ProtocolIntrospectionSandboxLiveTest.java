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
 * Live LLM integration test for the Protocol & Console Introspection sandbox challenge.
 * Verifies that live autonomous agents accurately identify backend failures and client runtime errors
 * using protocol-level introspection without entering repetitive locator thrashing loops.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@AiLinter(false)
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_protocol_introspection_playbook")
public class ProtocolIntrospectionSandboxLiveTest extends BaseAiTest
{
    /**
     * Constructs a default ProtocolIntrospectionSandboxLiveTest.
     */
    public ProtocolIntrospectionSandboxLiveTest()
    {
    }

    /**
     * Sets up test page URL dynamically before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        final String pageUrl = getSandboxUrl("/shop/sandbox/protocol-introspection.html");
        session.data().putDynamic("protocol.test.url", pageUrl, false);
    }

    /**
     * Tests protocol introspection challenge using live LLM across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testProtocolIntrospectionLive(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${protocol.test.url} in the browser
              Click the trigger HTTP 500 button #trigger500Btn
              Verify that the introspection event log #eventStatus shows "HTTP 500 received"
            """);

        $("#eventStatus").shouldHave(text("HTTP 500 received"));
    }
}
