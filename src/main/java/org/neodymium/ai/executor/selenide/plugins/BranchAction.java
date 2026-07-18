/*
 * Apache License 2.0
 *
 * Copyright (c) 2026 Xceptance
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
                : (action.getElseActions() == null || action.getElseActions().isEmpty());

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
