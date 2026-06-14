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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.LlmCallDetails;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Test cases verifying HTTP exception handling, propagation, and retry rules.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiErrorHandlingTest extends BaseAiOfflineTest
{
    /**
     * Verifies that when the LLM service returns HTTP communication failures (e.g., 429 Too Many Requests),
     * the framework propagates the exception, preserves the HTTP status code, and captures the details
     * in the execution result. Asserts that the step's overall outcome is marked as failed.
     */
    @Test
    public final void testHttpExceptionPreservation()
    {
        // Simulate 429 Too Many Requests - queue enough responses for retries
        final AiMockResponse mockRes = AiMockResponse.builder()
                .httpStatusCode(429)
                .build();
        for (int i = 0; i < 10; i++)
        {
            this.llmClient.addResponse(mockRes);
        }

        final Throwable t = Assertions.assertThrows(Throwable.class, () -> {
            this.mockBrowser.execute("Click button");
        });

        Assertions.assertTrue(t.getMessage().contains("Simulated HTTP error code: 429") || 
                (t.getCause() != null && t.getCause().getMessage().contains("Simulated HTTP error code: 429")));

        // Verify cached result on failure
        final AiExecutionResult result = Neodymium.getLastAiExecutionResult();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(1, result.getSteps().size());
        
        final StepDetails step = result.getSteps().get(0);
        Assertions.assertTrue(step.getLlmCalls().size() >= 1);
        
        final LlmCallDetails call = step.getLlmCalls().get(0);
        Assertions.assertEquals(429, call.getResponseCode().intValue());
        Assertions.assertTrue(call.getErrorMessage().contains("Simulated HTTP error code: 429"));
    }

    /**
     * Recipe 1: Asserting Retry & Self-Healing on HTTP 503 Errors.
     * Validate that Neodymium correctly logs communication errors, handles retry rules,
     * and succeeds once the service returns.
     */
    @Test
    public final void testSelfHealingOnHttp503Error()
    {
        // Set up the virtual LLM queue: a 503 failure, followed by a clean action success response
        this.llmClient.addResponse(AiMockResponse.builder()
                .httpStatusCode(503)
                .delayMs(10L)
                .build());
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
                .tokens(150L, 45L)
                .build());

        // Execute
        final AiExecutionResult result = this.mockBrowser.execute("Click on the blue button");

        // Verify self-healing occurred
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(1, result.getEscalationCount());
        Assertions.assertEquals(0, result.getRetryCount());
        Assertions.assertEquals(2, result.getLlmCalls().size());
        
        // Assert that the first call captured the 503 error details
        Assertions.assertNotNull(result.getLlmCalls().get(0).getErrorMessage());
        Assertions.assertTrue(result.getLlmCalls().get(0).getErrorMessage().contains("503"));
    }

    /**
     * Verifies that when execution continually fails, the agent retries exactly maxRetries times
     * and throws AssertionError wrapping the failure.
     */
    @Test
    public final void testMaxRetriesExceededThrowsAssertionError()
    {
        // Set max retries to 2
        Neodymium.getData().put("neodymium.ai.agent.maxRetries", "2");

        // Queue standard LLM click action response
        final AiMockResponse mockRes = AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#btn"}],
                      "d": true
                    }
                    """)
                .build();
        this.llmClient.addResponse(mockRes);
        this.llmClient.addResponse(mockRes);
        this.llmClient.addResponse(mockRes);
        this.llmClient.addResponse(mockRes);

        // Configure ActionExecutor to always fail (so it exhausts all retries)
        final ActionExecutor failingExecutor = new ActionExecutor(this)
        {
            @Override
            public final void executeAll(final List<Action> actions)
            {
                throw new ActionExecutionException("Action execution simulated failure", null);
            }
        };

        // Build browser with the custom executor
        try (final AiBrowser browser = new AiBrowser(Neodymium.aiConfiguration(), this, this.llmClient, this.pageAnalyzer, failingExecutor))
        {
            Neodymium.setAiBrowser(browser);

            final AssertionError ex = Assertions.assertThrows(AssertionError.class, () -> {
                browser.execute("Click button");
            });

            Assertions.assertTrue(ex.getMessage().contains("failed (3 tries)")); // 1 initial + 2 retries
        }
    }
}
