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
 * // AI-generated: Gemini 3.5 Flash
 */
public final class AiPesapExecutionTest extends BaseAiOfflineTest
{
    /**
     * Verifies the end-to-end Pre-Execution Static Analysis Phase (PESAP) flow
     * when PESAP is enabled and linter is disabled. It enqueues a mock classification response predicting
     * LEAN for the step, followed by the mock action execution response.
     * Asserts that the context level is correctly predicted, the step's details
     * reflect the prediction, and the execution succeeds.
     */
    @Test
    public final void testEndToEndPesapFlow()
    {
        // 1. Explicitly enable PESAP for this test run
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "true");
        Neodymium.getData().put("neodymium.ai.pesap.classify.enabled", "true");
        Neodymium.getData().put("neodymium.ai.pesap.linter.enabled", "false");

        // 2. Queue the PESAP classification mock response (LlmMode.PESAP query)
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "predictions": {
                        "0": "LEAN"
                      }
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
        Assertions.assertEquals(ContextLevel.LEAN, step.getPesapPredictedContextLevel());
    }

    /**
     * Verifies that when both classification and semantic linter are enabled under PESAP,
     * both queries are sent to the LLM, warnings are collected, and their tokens are aggregated.
     */
    @Test
    public final void testPesapClassifierAndLinterFlow()
    {
        // 1. Explicitly enable both PESAP classification and linter properties
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "true");
        Neodymium.getData().put("neodymium.ai.pesap.classify.enabled", "true");
        Neodymium.getData().put("neodymium.ai.pesap.linter.enabled", "true");

        // 2. Queue classification response
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "predictions": {
                        "0": "AXTREE"
                      }
                    }
                    """)
                .tokens(50L, 25L, 0L)
                .build());

        // 3. Queue linter response with a warning
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "warnings": {
                        "0": ["Button click target is ambiguous"]
                      }
                    }
                    """)
                .tokens(60L, 30L, 0L)
                .build());

        // 4. Queue standard agent execution response
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

        // 5. Run the execution
        final AiExecutionResult result = this.mockBrowser.execute("Click button");

        // 6. Assertions
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());

        // Assert exact PESAP call count is 2 (classifier + linter)
        Assertions.assertEquals(2, this.llmClient.getAiStats().getPesapCallCount());

        // Combined token checks (classification tokens + linter tokens)
        Assertions.assertEquals(110L, result.getPesapInputTokens()); // 50 + 60
        Assertions.assertEquals(55L, result.getPesapOutputTokens());  // 25 + 30
        Assertions.assertEquals(165L, result.getPesapTotalTokens());
    }

    /**
     * Verifies that when only the semantic linter is enabled, the classifier is bypassed.
     * The linter query runs, and the step predicted context level remains null.
     */
    @Test
    public final void testPesapOnlyLinterEnabledFlow()
    {
        // 1. Enable linter but disable classification
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "true");
        Neodymium.getData().put("neodymium.ai.pesap.classify.enabled", "false");
        Neodymium.getData().put("neodymium.ai.pesap.linter.enabled", "true");

        // 2. Queue linter response (since classify is disabled, first response is for the linter)
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "warnings": {}
                    }
                    """)
                .tokens(60L, 30L, 0L)
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

        // Assert exact PESAP call count is 1 (linter only)
        Assertions.assertEquals(1, this.llmClient.getAiStats().getPesapCallCount());

        // PESAP tokens should match linter call only
        Assertions.assertEquals(60L, result.getPesapInputTokens());
        Assertions.assertEquals(30L, result.getPesapOutputTokens());

        // Step predicted context level should remain null
        Assertions.assertEquals(1, result.getSteps().size());
        final StepDetails step = result.getSteps().get(0);
        Assertions.assertNull(step.getPesapPredictedContextLevel());
    }

    /**
     * Verifies that when both classification and linter are disabled, no PESAP queries
     * are sent to the LLM, and PESAP token metrics are zero.
     */
    @Test
    public final void testPesapBothDisabledFlow()
    {
        // 1. Disable both PESAP sub-switches
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "true");
        Neodymium.getData().put("neodymium.ai.pesap.classify.enabled", "false");
        Neodymium.getData().put("neodymium.ai.pesap.linter.enabled", "false");

        // 2. Queue standard agent execution response (first and only call)
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

        // 3. Run the execution
        final AiExecutionResult result = this.mockBrowser.execute("Click button");

        // 4. Assertions
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());

        // Assert exact PESAP call count is 0
        Assertions.assertEquals(0, this.llmClient.getAiStats().getPesapCallCount());

        // PESAP tokens should be zero
        Assertions.assertEquals(0L, result.getPesapInputTokens());
        Assertions.assertEquals(0L, result.getPesapOutputTokens());
        Assertions.assertEquals(0L, result.getPesapTotalTokens());
    }
}
