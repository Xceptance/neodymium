/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.executor.selenide.plugins;

import org.neodymium.ai.action.Action;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.UnpopulatedBranchAssertionError;
import org.neodymium.ai.pipeline.HealingRequiredException;

/**
 * Action plugin that implements conditional branching (if-then-else) for AI execution.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class BranchAction implements BrowserActionPlugin
{
    private static final ThreadLocal<Boolean> lastConditionResult = new ThreadLocal<>();

    private final ExecutionContext context;

    /**
     * Constructs a BranchAction with the given execution context.
     *
     * @param context the execution context
     */
    public BranchAction(final ExecutionContext context)
    {
        this.context = context;
    }

    /**
     * Gets the last condition execution result of a BranchAction on this thread.
     *
     * @return the last condition evaluation result, or null if none
     */
    public static Boolean getLastConditionResult()
    {
        return lastConditionResult.get();
    }

    /**
     * Clears the last condition execution result on this thread.
     */
    public static void clearLastConditionResult()
    {
        lastConditionResult.remove();
    }

    /**
     * Evaluates the branch condition and runs either the then-branch or else-branch actions.
     *
     * @param action the branch action
     * @throws Exception if branch execution fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null || this.context == null)
        {
            return;
        }

        final TargetExecutor executor = (TargetExecutor) this.context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
        if (executor == null)
        {
            throw new IllegalStateException("BranchAction requires a TargetExecutor in the execution context transient data");
        }

        boolean conditionMet = true;
        if (action.getCondition() != null && !action.getCondition().isEmpty())
        {
            try
            {
                for (final Action condAction : action.getCondition())
                {
                    executor.execute(condAction);
                }
            }
            catch (final Exception | AssertionError e)
            {
                conditionMet = false;
            }
        }
        else
        {
            // Empty condition is considered met by default
            conditionMet = true;
        }

        lastConditionResult.set(conditionMet);

        final ExecutionMode mode = (ExecutionMode) this.context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE);
        if (mode != null && mode.isReplay())
        {
            final boolean pathUnpopulated = conditionMet
                ? (action.getThen() == null || action.getThen().isEmpty())
                : (action.hasElse() && (action.getElseActions() == null || action.getElseActions().isEmpty()));

            if (pathUnpopulated)
            {
                final String errorMsg = "Branch path is unpopulated. Condition met: " + conditionMet 
                    + ", but nested '" + (conditionMet ? "then" : "else") + "' actions are missing/empty.";
                if (mode == ExecutionMode.REPLAY_STRICT)
                {
                    throw new UnpopulatedBranchAssertionError(errorMsg);
                }
                else if (mode.supportsHealing())
                {
                    throw new HealingRequiredException(errorMsg);
                }
            }
        }

        if (conditionMet)
        {
            if (action.getThen() != null)
            {
                for (final Action thenAction : action.getThen())
                {
                    executor.execute(thenAction);
                }
            }
        }
        else
        {
            if (action.getElseActions() != null)
            {
                for (final Action elseAction : action.getElseActions())
                {
                    executor.execute(elseAction);
                }
            }
        }
    }
}
