/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
// AI-generated: Gemini 3.5 Flash
package com.xceptance.neodymium.ai.integration;

import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying Neodymium AI ability to resolve positional challenges,
 * such as clicking elements by index and removing the last item in a list.
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class PositionalChallengesTest extends BaseAiTest
{
    private String url;

    /**
     * Set up the local test page URL before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        this.url = String.format("http://localhost:%d/PositionalChallengesTest/testPositionalChallenges.html", server.getPort());
        Neodymium.getData().put("positional.test.url", this.url);
    }

    /**
     * Verifies positional click interactions ("Click the third element") and list modifications
     * ("remove the last item in the list") in both live LLM and replay modes.
     */
    @NeodymiumTest
    public final void testPositionalChallenges()
    {
        final String steps = """
                OPEN ${positional.test.url}
                Click the first element and the text "Clicked: 1" is shown
                Remove the last item in the list by clicking the "Delete" button and the text "Item D" is no longer shown
                Click the second "Click Me" button and the text "Clicked: 2" is shown
            """;

            final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

            assertThat(r1)
                .hasLlmCalls(3)
                .hasPesapCalls(3)
                .hasNoEscalations()
                .hasDirectParses(1)
                .hasReplays(0)
                .hasActionsCount(7);

            final StepDetails stepDetails0 = r1.getSteps().get(0);
            assertTrue(stepDetails0.isDirectParse());
            assertFalse(stepDetails0.isReplayed());
            assertTrue(stepDetails0.getLlmCalls().isEmpty());

            final StepDetails stepDetails1 = r1.getSteps().get(1);
            assertFalse(stepDetails1.isDirectParse());
            assertFalse(stepDetails1.isReplayed());
            assertEquals(1, stepDetails1.getLlmCalls().size());

            final StepDetails stepDetails2 = r1.getSteps().get(2);
            assertFalse(stepDetails2.isDirectParse());
            assertFalse(stepDetails2.isReplayed());
            assertEquals(1, stepDetails2.getLlmCalls().size());

            final StepDetails stepDetails3 = r1.getSteps().get(3);
            assertFalse(stepDetails3.isDirectParse());
            assertFalse(stepDetails3.isReplayed());
            assertEquals(1, stepDetails3.getLlmCalls().size());

            // Verify the result of the clicks in the SUT
            assertEquals("Clicked: 2", Selenide.$("#click-result").text());
            assertEquals(3, Selenide.$$(".item").size());
            assertFalse(Selenide.$("#item-list").text().contains("Item D"));

            // Reset the browser and verify the steps playback successfully using the saved playbook
            this.resetBrowser();

            final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

            assertThat(r2)
                .hasLlmCalls(0)
                .hasNoPesapCalls()
                .hasNoEscalations()
                .hasDirectParses(0)
                .hasReplays(4)
                .hasActionsCount(7);

            final StepDetails replayStep0 = r2.getSteps().get(0);
            assertFalse(replayStep0.isDirectParse());
            assertTrue(replayStep0.isReplayed());
            assertTrue(replayStep0.getLlmCalls().isEmpty());

            final StepDetails replayStep1 = r2.getSteps().get(1);
            assertFalse(replayStep1.isDirectParse());
            assertTrue(replayStep1.isReplayed());
            assertTrue(replayStep1.getLlmCalls().isEmpty());

            final StepDetails replayStep2 = r2.getSteps().get(2);
            assertFalse(replayStep2.isDirectParse());
            assertTrue(replayStep2.isReplayed());
            assertTrue(replayStep2.getLlmCalls().isEmpty());

            final StepDetails replayStep3 = r2.getSteps().get(3);
            assertFalse(replayStep3.isDirectParse());
            assertTrue(replayStep3.isReplayed());
            assertTrue(replayStep3.getLlmCalls().isEmpty());

            assertEquals("Clicked: 2", Selenide.$("#click-result").text());
            assertEquals(3, Selenide.$$(".item").size());
            assertFalse(Selenide.$("#item-list").text().contains("Item D"));

    }
}
