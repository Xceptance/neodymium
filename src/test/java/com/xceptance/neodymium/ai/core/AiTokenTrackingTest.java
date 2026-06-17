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
package com.xceptance.neodymium.ai.core;
import com.xceptance.neodymium.ai.BaseAiOfflineTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.AiTestRunResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.core.LlmCallDetails;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.common.testdata.TestData;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Test cases validating LLM token metrics tracking and composite runs results.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiTokenTrackingTest extends BaseAiOfflineTest
{
    /**
     * Validates browserless mock execution under standard success conditions.
     * Uses a mock LLM client to return a mock JSON response containing click actions
     * and specified token counts. Asserts that the final result aggregates these tokens,
     * captures the step-level details (raw vs expanded instructions), tracks the executed actions, 
     * records the HTTP response code (200), and registers lookup statistics.
     */
    @Test
    public final void testOfflineMockSequenceAndTokenDeltas()
    {
        this.pageAnalyzer.setMockDomText("<html><body>Mock SUT</body></html>");

        final AiMockResponse mockResponse = AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Entering text",
                      "a": [{"t": "CLICK", "tg": "#login"}],
                      "d": true
                    }
                    """)
                .delayMs(10L)
                .tokens(100L, 50L, 20L)
                .build();

        this.llmClient.addResponse(mockResponse);

        Neodymium.getData().put("loginLabel", "login button");

        final AiExecutionResult result = this.mockBrowser.execute("Click ${loginLabel}");
        
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(100L, result.getInputTokens());
        Assertions.assertEquals(50L, result.getOutputTokens());
        Assertions.assertEquals(20L, result.getCachedTokens());
        Assertions.assertEquals(150L, result.getTotalTokens());
        Assertions.assertEquals(0, result.getRetryCount());
        Assertions.assertEquals(0, result.getEscalationCount());
        
        // Assert lookup details gathered during execute
        Assertions.assertEquals(1, result.getLookups().size());
        Assertions.assertEquals("loginLabel", result.getLookups().get(0).getKey());
        Assertions.assertEquals("login button", result.getLookups().get(0).getResolvedValue());

        Assertions.assertEquals(1, result.getSteps().size());
        final StepDetails step = result.getSteps().get(0);
        Assertions.assertEquals("Click ${loginLabel}", step.getRawInstruction());
        Assertions.assertEquals("Click login button", step.getExpandedInstruction());
        Assertions.assertEquals(1, step.getActions().size());
        Assertions.assertEquals("CLICK", step.getActions().get(0).getType());

        Assertions.assertEquals(1, step.getLlmCalls().size());
        final LlmCallDetails call = step.getLlmCalls().get(0);
        Assertions.assertEquals(ContextLevel.AXTREE, call.getContextLevel());
        Assertions.assertEquals(100L, call.getInputTokens());
        Assertions.assertEquals(50L, call.getOutputTokens());
        Assertions.assertEquals(20L, call.getCachedTokens());
        Assertions.assertEquals(200, call.getResponseCode().intValue());

        // Static getter lookup validation
        Assertions.assertSame(result, Neodymium.getLastAiExecutionResult());
    }

    /**
     * Verifies the composition of before, steps, and after lifecycle execution stages
     * inside a data-driven test run. Enqueues mock responses for all three phases and
     * asserts that {@link AiTestRunResult} contains individual non-null results and
     * maps the cumulative token usage across all lifecycle stages.
     */
    @Test
    public final void testCompositeRunResult() throws Throwable
    {
        // Mock response for "before"
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "SetupSut",
                      "a": [{"t": "CLICK", "tg": "#setup"}],
                      "d": true
                    }
                    """)
                .delayMs(1L)
                .tokens(10L, 5L, 0L)
                .build());

        // Mock response for "steps"
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "RunSut",
                      "a": [{"t": "CLICK", "tg": "#run"}],
                      "d": true
                    }
                    """)
                .delayMs(1L)
                .tokens(20L, 10L, 0L)
                .build());

        // Mock response for "after"
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "TeardownSut",
                      "a": [{"t": "CLICK", "tg": "#teardown"}],
                      "d": true
                    }
                    """)
                .delayMs(1L)
                .tokens(30L, 15L, 0L)
                .build());

        final TestData data = Neodymium.getData();
        data.put("before", "Setup steps");
        data.put("steps", "Run steps");
        data.put("after", "Teardown steps");

        final AiTestRunResult runResult = this.mockBrowser.execute();

        Assertions.assertNotNull(runResult);
        Assertions.assertNotNull(runResult.getBeforeResult());
        Assertions.assertNotNull(runResult.getStepsResult());
        Assertions.assertEquals(1, runResult.getAfterResults().size());

        Assertions.assertEquals(10L, runResult.getBeforeResult().getInputTokens());
        Assertions.assertEquals(20L, runResult.getStepsResult().getInputTokens());
        Assertions.assertEquals(30L, runResult.getAfterResults().get(0).getInputTokens());

        Assertions.assertSame(runResult, Neodymium.getLastAiTestRunResult());
    }
}
