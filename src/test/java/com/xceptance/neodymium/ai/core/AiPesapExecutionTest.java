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
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Test cases verifying Pre-Execution Static Analysis Phase (PESAP) classification and linter logic.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiPesapExecutionTest extends BaseAiOfflineTest
{
    /**
     * Verifies the end-to-end Pre-Execution Static Analysis Phase (PESAP) flow
     * when PESAP is enabled and linter is disabled. It enqueues a mock classification response predicting
     * STANDARD for the step, followed by the mock action execution response.
     * Asserts that the context level is correctly predicted, the step's details
     * reflect the prediction, and the execution succeeds.
     */
    @Test
    public final void testEndToEndPesapFlow()
    {
        // 1. Explicitly configure linter for this test run
        Neodymium.getData().put("neodymium.ai.pesap.linter.enabled", "false");

        // 2. Queue the PESAP classification mock response (LlmMode.PESAP query)
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "c": "STANDARD",
                      "jm": false,
                      "sp": []
                    }
                    """)
                .tokens(50L, 25L, 0L)
                .build());

        // 3. Queue the AGENT execution mock response (LlmMode.AGENT query)
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#btn"}],
                      "d": true
                    }
                    """)
                .tokens(100L, 50L, 0L)
                .build());

        // 4. Run the execution
        final AiExecutionResult result = this.mockBrowser.execute("Click standard button");

        // 5. Assertions
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        
        // Assert exact PESAP call count is 1
        Assertions.assertEquals(1, this.llmClient.getAiStats().getPesapCallCount());

        // PESAP token tracking assertions
        Assertions.assertEquals(50L, result.getPesapInputTokens());
        Assertions.assertEquals(25L, result.getPesapOutputTokens());
        Assertions.assertEquals(75L, result.getPesapTotalTokens());

        // Agent execution token tracking assertions
        Assertions.assertEquals(100L, result.getInputTokens());
        Assertions.assertEquals(50L, result.getOutputTokens());

        // Steps and predicted ContextLevel assertions
        Assertions.assertEquals(1, result.getSteps().size());
        final StepDetails step = result.getSteps().get(0);
        Assertions.assertEquals("Click standard button", step.getRawInstruction());
        Assertions.assertEquals(ContextLevel.STANDARD, step.getPesapPredictedContextLevel());
        Assertions.assertFalse(step.isPesapRequiresJavaMethods());
        Assertions.assertNotNull(step.getPesapCall());
        final LlmCallDetails pesapCall = step.getPesapCall();
        Assertions.assertEquals(50L, pesapCall.getInputTokens());
        Assertions.assertEquals(25L, pesapCall.getOutputTokens());
        Assertions.assertEquals(LlmMode.PESAP, pesapCall.getCallMode());
    }

    /**
     * Verifies that when both classification and semantic linter are enabled under PESAP,
     * the classifier query runs, and the step predicted context level is updated correctly.
     */
    @Test
    public final void testPesapClassifierAndLinterFlow()
    {
        // 1. Explicitly enable linter property
        Neodymium.getData().put("neodymium.ai.pesap.linter.enabled", "true");

        // 2. Queue classification response
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "c": "AXTREE",
                      "jm": false,
                      "sp": []
                    }
                    """)
                .tokens(50L, 25L, 0L)
                .build());

        // 3. Queue standard agent execution response
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#btn"}],
                      "d": true
                    }
                    """)
                .tokens(100L, 50L, 0L)
                .build());

        // 4. Run the execution
        final AiExecutionResult result = this.mockBrowser.execute("Click button");

        // 5. Assertions
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());

        // Assert exact PESAP call count is 1
        Assertions.assertEquals(1, this.llmClient.getAiStats().getPesapCallCount());

        // Combined token checks
        Assertions.assertEquals(50L, result.getPesapInputTokens());
        Assertions.assertEquals(25L, result.getPesapOutputTokens());
        Assertions.assertEquals(75L, result.getPesapTotalTokens());

        Assertions.assertEquals(1, result.getSteps().size());
        final StepDetails step = result.getSteps().get(0);
        Assertions.assertNotNull(step.getPesapCall());
        final LlmCallDetails pesapCall = step.getPesapCall();
        Assertions.assertEquals(50L, pesapCall.getInputTokens());
        Assertions.assertEquals(25L, pesapCall.getOutputTokens());
        Assertions.assertEquals(LlmMode.PESAP, pesapCall.getCallMode());
    }
}
