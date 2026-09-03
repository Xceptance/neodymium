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
package org.neodymium.ai.pipeline;

/**
 * Exception thrown when LLM token consumption exceeds configured input or output token budgets during a test run.
 * Bypasses step healing and retry loops to trigger an immediate test abort.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class TokenBudgetExceededException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /** The token budget category (INPUT or OUTPUT) */
    public enum BudgetType
    {
        INPUT,
        OUTPUT
    }

    private final BudgetType budgetType;
    private final int consumedTokens;
    private final int budgetLimit;

    /**
     * Constructs a TokenBudgetExceededException with specific budget violation metrics.
     *
     * @param budgetType the direction of token budget exceeded (INPUT or OUTPUT)
     * @param consumedTokens cumulative tokens consumed in the active session
     * @param budgetLimit configured maximum token budget limit
     */
    public TokenBudgetExceededException(
        final BudgetType budgetType,
        final int consumedTokens,
        final int budgetLimit
    )
    {
        super(formatMessage(budgetType, consumedTokens, budgetLimit));
        this.budgetType = budgetType;
        this.consumedTokens = consumedTokens;
        this.budgetLimit = budgetLimit;
    }

    /**
     * Helper to format a clear abort error message for reports.
     */
    private static String formatMessage(final BudgetType type, final int consumed, final int limit)
    {
        final String direction = type == BudgetType.INPUT ? "Input tokens consumed" : "Output tokens generated";
        final String target = type == BudgetType.INPUT ? "input token budget" : "output token budget";
        return String.format("Token budget exceeded for test run: %s (%d) exceeded configured %s (%d). Test run aborted.",
            direction, consumed, target, limit);
    }

    /**
     * Gets the exceeded budget category type.
     *
     * @return the budget type enum
     */
    public BudgetType getBudgetType()
    {
        return this.budgetType;
    }

    /**
     * Checks if the exceeded budget was an input token budget.
     *
     * @return true if input token budget exceeded, false otherwise
     */
    public boolean isInputBudget()
    {
        return this.budgetType == BudgetType.INPUT;
    }

    /**
     * Checks if the exceeded budget was an output token budget.
     *
     * @return true if output token budget exceeded, false otherwise
     */
    public boolean isOutputBudget()
    {
        return this.budgetType == BudgetType.OUTPUT;
    }

    /**
     * Gets total tokens consumed when budget limit was breached.
     *
     * @return consumed token count
     */
    public int getConsumedTokens()
    {
        return this.consumedTokens;
    }

    /**
     * Gets the configured token budget limit threshold.
     *
     * @return budget limit threshold
     */
    public int getBudgetLimit()
    {
        return this.budgetLimit;
    }
}
