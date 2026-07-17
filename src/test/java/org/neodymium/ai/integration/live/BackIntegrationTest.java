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

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;

import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.session.AiSession;

/**
 * Live integration test for the BACK action plugin.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class BackIntegrationTest extends BaseAiTest
{

    /**
     * Executes Back integration test in both live (recording) and strict replay modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    @AiDataSet("backData")
    public void testBack(final AiSession session)
    {
        final String pageUrl1 = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        final String pageUrl2 = String.format("http://localhost:%d/TypeActionTest/testTypeHappyPath.html", server.getPort());
        session.getExecutionContext().getSessionData().putDynamic("back.test.url1", pageUrl1, false);
        session.getExecutionContext().getSessionData().putDynamic("back.test.url2", pageUrl2, false);

        runPlaybook(session, """
            data:
              - testId: backData
            steps: |
              Open ${back.test.url1} in the browser
              Open ${back.test.url2} in the browser
              Go back to the previous page
            """);

        // Assert we are back on the first page
        $("h1").shouldHave(text("Assert Action Test"));
        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));
    }
}
