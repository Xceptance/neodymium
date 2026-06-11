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
package com.xceptance.neodymium.ai.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;

/**
 * Fluent assertion helper for validating {@link AiExecutionResult} instances in tests.
 */
public final class AiExecutionAssert
{
    private final AiExecutionResult result;

    private AiExecutionAssert(final AiExecutionResult result)
    {
        this.result = result;
    }

    /**
     * Entry point for fluent assertions on {@link AiExecutionResult}.
     *
     * @param result the execution result to validate
     * @return the assertion helper
     */
    public static AiExecutionAssert assertThat(final AiExecutionResult result)
    {
        return new AiExecutionAssert(result);
    }

    /**
     * Asserts the number of LLM calls made during the execution.
     *
     * @param expected the expected number of calls
     * @return this assertion helper
     */
    public final AiExecutionAssert hasLlmCalls(final int expected)
    {
        assertEquals(expected, result.getLlmCalls().size(), "LLM call count mismatch");
        return this;
    }

    /**
     * Asserts that no escalations occurred during the execution.
     *
     * @return this assertion helper
     */
    public final AiExecutionAssert hasNoEscalations()
    {
        return hasEscalations(0);
    }

    /**
     * Asserts the number of escalations that occurred during the execution.
     *
     * @param expected the expected number of escalations
     * @return this assertion helper
     */
    public final AiExecutionAssert hasEscalations(final int expected)
    {
        assertEquals(expected, result.getEscalationCount(), "Escalation count mismatch");
        return this;
    }

    /**
     * Asserts the ContextLevel used for the first LLM call.
     *
     * @param expected the expected context level
     * @return this assertion helper
     */
    public final AiExecutionAssert hasContextLevel(final ContextLevel expected)
    {
        return hasContextLevel(0, expected);
    }

    /**
     * Asserts the ContextLevel used for a specific LLM call.
     *
     * @param callIndex the index of the LLM call
     * @param expected  the expected context level
     * @return this assertion helper
     */
    public final AiExecutionAssert hasContextLevel(final int callIndex, final ContextLevel expected)
    {
        assertTrue(result.getLlmCalls().size() > callIndex, "No LLM call found at index " + callIndex);
        assertEquals(expected, result.getLlmCalls().get(callIndex).getContextLevel(), 
                "Context level mismatch on LLM call " + callIndex);
        return this;
    }

    /**
     * Asserts the total number of actions returned.
     *
     * @param expected the expected number of actions
     * @return this assertion helper
     */
    public final AiExecutionAssert hasActionsCount(final int expected)
    {
        assertEquals(expected, result.getActions().size(), "Action count mismatch");
        return this;
    }

    /**
     * Asserts the type of an action at a specific index.
     *
     * @param index the index of the action
     * @param type  the expected type (e.g. CLICK, TYPE, NAVIGATE)
     * @return this assertion helper
     */
    public final AiExecutionAssert hasAction(final int index, final String type)
    {
        assertTrue(result.getActions().size() > index, "No action found at index " + index);
        assertEquals(type, result.getActions().get(index).getType(), "Action type mismatch at index " + index);
        return this;
    }

    /**
     * Asserts the type and target containment of an action at a specific index.
     *
     * @param index          the index of the action
     * @param type           the expected type
     * @param targetContains a substring expected to be contained within the action target
     * @return this assertion helper
     */
    public final AiExecutionAssert hasAction(final int index, final String type, final String targetContains)
    {
        hasAction(index, type);
        final String target = result.getActions().get(index).getTarget();
        if (targetContains != null && !targetContains.isEmpty())
        {
            assertTrue(target != null && target.contains(targetContains), 
                    "Action target at index " + index + " ('" + target + "') does not contain: '" + targetContains + "'");
        }
        return this;
    }

    /**
     * Asserts the type, target containment, and value of an action at a specific index.
     *
     * @param index          the index of the action
     * @param type           the expected type
     * @param targetContains a substring expected to be contained within the action target
     * @param value          the expected action value
     * @return this assertion helper
     */
    public final AiExecutionAssert hasAction(final int index, final String type, final String targetContains, final String value)
    {
        hasAction(index, type, targetContains);
        assertEquals(value, result.getActions().get(index).getValue(), "Action value mismatch at index " + index);
        return this;
    }

    /**
     * Asserts the number of steps that were replayed from the playbook.
     *
     * @param expected the expected number of replayed steps
     * @return this assertion helper
     */
    public final AiExecutionAssert hasReplays(final int expected)
    {
        assertEquals(expected, result.getReplayCount(), "Replayed steps count mismatch");
        return this;
    }

    /**
     * Asserts the number of steps resolved directly via plugins/regex without LLM calls.
     *
     * @param expected the expected number of directly parsed steps
     * @return this assertion helper
     */
    public final AiExecutionAssert hasDirectParses(final int expected)
    {
        assertEquals(expected, result.getDirectParseCount(), "Directly parsed steps count mismatch");
        return this;
    }

    /**
     * Asserts if a specific step index was replayed or not.
     *
     * @param stepIndex the 0-based index of the step
     * @param expected  the expected replay status
     * @return this assertion helper
     */
    public final AiExecutionAssert hasStepReplayed(final int stepIndex, final boolean expected)
    {
        assertTrue(result.getSteps().size() > stepIndex, "No step found at index " + stepIndex);
        assertEquals(expected, result.getSteps().get(stepIndex).isReplayed(),
                "Step at index " + stepIndex + " replay status mismatch");
        return this;
    }

    /**
     * Asserts if a specific step index was resolved via direct parse or not.
     *
     * @param stepIndex the 0-based index of the step
     * @param expected  the expected direct parse status
     * @return this assertion helper
     */
    public final AiExecutionAssert hasStepDirectParsed(final int stepIndex, final boolean expected)
    {
        assertTrue(result.getSteps().size() > stepIndex, "No step found at index " + stepIndex);
        assertEquals(expected, result.getSteps().get(stepIndex).isDirectParse(),
                "Step at index " + stepIndex + " direct parse status mismatch");
        return this;
    }

    /**
     * Asserts the number of PESAP calls made during the execution.
     *
     * @param expected the expected number of PESAP calls
     * @return this assertion helper
     */
    public final AiExecutionAssert hasPesapCalls(final int expected)
    {
        assertEquals(expected, result.getPesapCallCount(), "PESAP call count mismatch");
        return this;
    }

    /**
     * Asserts that no PESAP calls occurred during the execution.
     *
     * @return this assertion helper
     */
    public final AiExecutionAssert hasNoPesapCalls()
    {
        return hasPesapCalls(0);
    }
}
