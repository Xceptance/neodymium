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
package org.neodymium.ai.telemetry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.junit.AiContext;
import org.neodymium.ai.model.ExecutionMetrics;
import org.neodymium.ai.model.MetricsAsserter;
import org.neodymium.ai.model.PlaybookRecording;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.TokenBudgetExceededException;

/**
 * Unit tests verifying real-time token budget enforcement (input and output tokens) and post-execution verifyMetrics token asserter methods.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class TokenBudgetGuardTest
{
    private ExecutionContext context;
    private ExecutionEventBus eventBus;

    @BeforeEach
    public void setup()
    {
        System.clearProperty("neodymium.ai.tokenBudget.input");
        System.clearProperty("neodymium.ai.tokenBudget.output");
        AiConfiguration.resetInstance();

        this.context = new ExecutionContext(null);
        this.eventBus = new ExecutionEventBus();
    }

    @Test
    @DisplayName("Input token budget breach triggers TokenBudgetExceededException")
    public void testInputTokenBudgetBreachThrowsException()
    {
        final TokenBudgetGuard guard = new TokenBudgetGuard(100, -1);
        this.eventBus.registerListener(guard);

        final LlmRequest request = new LlmRequest("System prompt", "User prompt", null, null, 0.0, 30);
        final LlmResponse response = new LlmResponse("Result", new TokenUsage(150, 10, 160), "mock-model");
        final LlmResponseReceivedEvent event = new LlmResponseReceivedEvent(request, response, 100, "EXECUTION");

        final TokenBudgetExceededException exception = Assertions.assertThrows(
            TokenBudgetExceededException.class,
            () -> this.eventBus.dispatch(event)
        );

        Assertions.assertTrue(exception.isInputBudget());
        Assertions.assertFalse(exception.isOutputBudget());
        Assertions.assertEquals(150, exception.getConsumedTokens());
        Assertions.assertEquals(100, exception.getBudgetLimit());
        Assertions.assertTrue(exception.getMessage().contains("Input tokens consumed (150) exceeded configured input token budget (100)"));
    }

    @Test
    @DisplayName("Output token budget breach triggers TokenBudgetExceededException")
    public void testOutputTokenBudgetBreachThrowsException()
    {
        final TokenBudgetGuard guard = new TokenBudgetGuard(-1, 50);
        this.eventBus.registerListener(guard);

        final LlmRequest request = new LlmRequest("System prompt", "User prompt", null, null, 0.0, 30);
        final LlmResponse response = new LlmResponse("Result", new TokenUsage(20, 80, 100), "mock-model");
        final LlmResponseReceivedEvent event = new LlmResponseReceivedEvent(request, response, 100, "EXECUTION");

        final TokenBudgetExceededException exception = Assertions.assertThrows(
            TokenBudgetExceededException.class,
            () -> this.eventBus.dispatch(event)
        );

        Assertions.assertFalse(exception.isInputBudget());
        Assertions.assertTrue(exception.isOutputBudget());
        Assertions.assertEquals(80, exception.getConsumedTokens());
        Assertions.assertEquals(50, exception.getBudgetLimit());
        Assertions.assertTrue(exception.getMessage().contains("Output tokens generated (80) exceeded configured output token budget (50)"));
    }

    @Test
    @DisplayName("Token usage within budget does not throw exception")
    public void testTokenUsageWithinBudgetPasses()
    {
        final TokenBudgetGuard guard = new TokenBudgetGuard(200, 100);
        this.eventBus.registerListener(guard);

        final LlmRequest request = new LlmRequest("System prompt", "User prompt", null, null, 0.0, 30);
        final LlmResponse response = new LlmResponse("Result", new TokenUsage(100, 40, 140), "mock-model");
        final LlmResponseReceivedEvent event = new LlmResponseReceivedEvent(request, response, 100, "EXECUTION");

        Assertions.assertDoesNotThrow(() -> this.eventBus.dispatch(event));
    }

    @Test
    @DisplayName("Disabled token budgets (-1) allow arbitrary token counts without throwing exception")
    public void testDisabledBudgetsAllowArbitraryTokens()
    {
        final TokenBudgetGuard guard = new TokenBudgetGuard(-1, -1);
        this.eventBus.registerListener(guard);

        final LlmRequest request = new LlmRequest("System prompt", "User prompt", null, null, 0.0, 30);
        final LlmResponse response = new LlmResponse("Result", new TokenUsage(1_000_000, 500_000, 1_500_000), "mock-model");
        final LlmResponseReceivedEvent event = new LlmResponseReceivedEvent(request, response, 100, "EXECUTION");

        Assertions.assertDoesNotThrow(() -> this.eventBus.dispatch(event));
    }

    @Test
    @DisplayName("verifyMetrics fluent asserters correctly validate input, output, and total token usage")
    public void testVerifyMetricsTokenAsserters()
    {
        final TokenUsage usage = new TokenUsage(1000, 200, 1200, 300);
        final ExecutionMetrics metrics = new ExecutionMetrics(
            ExecutionMode.LLM_ONLY,
            1, 1, 0, 0, 0,
            1, 0, 0, 0, 0,
            0, null, usage
        );

        final MetricsAsserter asserter = new MetricsAsserter(metrics);

        Assertions.assertDoesNotThrow(() -> asserter.hasInputTokens(1000));
        Assertions.assertDoesNotThrow(() -> asserter.hasInputTokens(500, 1500));
        Assertions.assertDoesNotThrow(() -> asserter.hasOutputTokens(200));
        Assertions.assertDoesNotThrow(() -> asserter.hasOutputTokens(100, 300));
        Assertions.assertDoesNotThrow(() -> asserter.hasTotalTokens(1200));
        Assertions.assertDoesNotThrow(() -> asserter.hasTotalTokens(1000, 1500));

        final AssertionError inputErr = Assertions.assertThrows(AssertionError.class, () -> asserter.hasInputTokens(500));
        Assertions.assertTrue(inputErr.getMessage().contains("input tokens"));

        final AssertionError outputErr = Assertions.assertThrows(AssertionError.class, () -> asserter.hasOutputTokens(50, 100));
        Assertions.assertTrue(outputErr.getMessage().contains("output tokens"));
    }
}
