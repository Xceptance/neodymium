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

import java.util.concurrent.atomic.AtomicInteger;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.event.ExecutionListener;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.TokenBudgetExceededException;

/**
 * Event listener that accumulates real-time token usage (input and output tokens) and enforces
 * configured token budget limits during an AI test run.
 * Throws a {@link TokenBudgetExceededException} immediately upon budget breach to abort execution.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class TokenBudgetGuard implements ExecutionListener
{
    private final int inputBudget;
    private final int outputBudget;

    private final AtomicInteger tokenUsageInput = new AtomicInteger(0);
    private final AtomicInteger tokenUsageOutput = new AtomicInteger(0);

    /**
     * Constructs a default TokenBudgetGuard that dynamically resolves budget limits from
     * active ExecutionContext or global AiConfiguration.
     */
    public TokenBudgetGuard()
    {
        this(-1, -1);
    }

    /**
     * Constructs a TokenBudgetGuard with specific input and output token budget limits.
     *
     * @param inputBudget maximum allowed input/prompt tokens (<= 0 means resolve dynamically)
     * @param outputBudget maximum allowed output/completion tokens (<= 0 means resolve dynamically)
     */
    public TokenBudgetGuard(final int inputBudget, final int outputBudget)
    {
        this.inputBudget = inputBudget;
        this.outputBudget = outputBudget;
    }

    /**
     * Consumes execution events to track token usage and enforce real-time budget boundaries.
     *
     * @param event the dispatched execution event
     * @throws TokenBudgetExceededException if input or output token limits are breached
     */
    @Override
    public void onEvent(final ExecutionEvent event)
    {
        if (event instanceof LlmResponseReceivedEvent llmEvent)
        {
            if (llmEvent.getResponse() != null && llmEvent.getResponse().tokenUsage() != null)
            {
                final TokenUsage usage = llmEvent.getResponse().tokenUsage();
                final int currentInput = this.tokenUsageInput.addAndGet(usage.inputTokenCount());
                final int currentOutput = this.tokenUsageOutput.addAndGet(usage.outputTokenCount());

                final int effectiveInputBudget = resolveInputBudget();
                if (effectiveInputBudget > 0 && currentInput > effectiveInputBudget)
                {
                    throw new TokenBudgetExceededException(
                        TokenBudgetExceededException.BudgetType.INPUT,
                        currentInput,
                        effectiveInputBudget
                    );
                }

                final int effectiveOutputBudget = resolveOutputBudget();
                if (effectiveOutputBudget > 0 && currentOutput > effectiveOutputBudget)
                {
                    throw new TokenBudgetExceededException(
                        TokenBudgetExceededException.BudgetType.OUTPUT,
                        currentOutput,
                        effectiveOutputBudget
                    );
                }
            }
        }
    }

    private int resolveInputBudget()
    {
        if (this.inputBudget > 0)
        {
            return this.inputBudget;
        }
        final ExecutionContext activeCtx = ExecutionContext.getActiveContext();
        if (activeCtx != null)
        {
            final Object ctxBudget = activeCtx.getTransientData().get(ExecutionContext.KEY_TOKEN_BUDGET_INPUT);
            if (ctxBudget instanceof Integer i && i > 0)
            {
                return i;
            }
        }
        return org.neodymium.ai.config.AiConfiguration.getInstance().getTokenBudgetInput();
    }

    private int resolveOutputBudget()
    {
        if (this.outputBudget > 0)
        {
            return this.outputBudget;
        }
        final ExecutionContext activeCtx = ExecutionContext.getActiveContext();
        if (activeCtx != null)
        {
            final Object ctxBudget = activeCtx.getTransientData().get(ExecutionContext.KEY_TOKEN_BUDGET_OUTPUT);
            if (ctxBudget instanceof Integer i && i > 0)
            {
                return i;
            }
        }
        return org.neodymium.ai.config.AiConfiguration.getInstance().getTokenBudgetOutput();
    }

    /**
     * Gets the configured input token budget limit.
     *
     * @return input token budget limit
     */
    public int getInputBudget()
    {
        return this.inputBudget;
    }

    /**
     * Gets the configured output token budget limit.
     *
     * @return output token budget limit
     */
    public int getOutputBudget()
    {
        return this.outputBudget;
    }

    /**
     * Gets total input tokens consumed so far in the active session.
     *
     * @return current input token count
     */
    public int getCurrentInputTokens()
    {
        return this.tokenUsageInput.get();
    }

    /**
     * Gets total output tokens generated so far in the active session.
     *
     * @return current output token count
     */
    public int getCurrentOutputTokens()
    {
        return this.tokenUsageOutput.get();
    }
}
