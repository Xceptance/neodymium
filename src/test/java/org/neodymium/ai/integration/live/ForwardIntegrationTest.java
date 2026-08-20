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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codeborne.selenide.WebDriverRunner;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.session.AiSession;

/**
 * Live integration test for the FORWARD action plugin.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class ForwardIntegrationTest extends BaseAiTest
{

    /**
     * Executes Forward integration test in both live and strict replay modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    @AiDataSet("forwardData")
    public void testForward(final AiSession session) throws Exception
    {
        final String pageUrl1 = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        final String pageUrl2 = String.format("http://localhost:%d/TypeActionTest/testTypeHappyPath.html", server.getPort());
        session.data().putDynamic("forward.test.url1", pageUrl1, false);
        session.data().putDynamic("forward.test.url2", pageUrl2, false);

        session.execute( """
            data:
              - testId: forwardData
            steps: |
              Open ${forward.test.url1} in the browser
              Open ${forward.test.url2} in the browser
              Go back
              Go forward
            """);

        assertTrue(WebDriverRunner.url().contains("testTypeHappyPath.html"));
    }
}
