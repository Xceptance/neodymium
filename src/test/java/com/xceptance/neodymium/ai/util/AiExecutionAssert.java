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
package com.xceptance.neodymium.ai.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.core.StepDetails;

/**
 * Fluent assertion helper for validating {@link AiExecutionResult} instances in tests.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
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
     * Entry point for fluent assertions on {@link StepDetails}.
     *
     * @param step the step details to validate
     * @return the assertion helper
     */
    public static StepDetailsAssert assertThat(final StepDetails step)
    {
        return new StepDetailsAssert(step);
    }

    /**
     * Entry point for fluent assertions on {@link Action}.
     *
     * @param action the action to validate
     * @return the assertion helper
     */
    public static AiActionAssert assertThat(final Action action)
    {
        return new AiActionAssert(action);
    }

    /**
     * Performs assertions on a specific step using a step assertion helper.
     *
     * @param stepIndex the 0-based index of the step
     * @param assertion the step assertions to run
     * @return this assertion helper
     */
    public final AiExecutionAssert step(final int stepIndex, final Consumer<StepDetailsAssert> assertion)
    {
        assertTrue(result.getSteps().size() > stepIndex, "No step found at index " + stepIndex);
        assertion.accept(new StepDetailsAssert(result.getSteps().get(stepIndex)));
        return this;
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
     * Asserts the ContextLevel used for the first LLM call of a specific step.
     *
     * @param stepIndex the index of the step
     * @param expected  the expected context level
     * @return this assertion helper
     */
    public final AiExecutionAssert hasContextLevel(final int stepIndex, final ContextLevel expected)
    {
        assertTrue(result.getSteps().size() > stepIndex, "No step found at index " + stepIndex);
        final var step = result.getSteps().get(stepIndex);
        assertFalse(step.getLlmCalls().isEmpty(), "Expected step at index " + stepIndex + " to have at least one LLM call");
        assertEquals(expected, step.getLlmCalls().get(0).getContextLevel(), 
                "Context level mismatch on first LLM call of step " + stepIndex);
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
     * Asserts the type of the first action of a specific step index.
     *
     * @param stepIndex the index of the step
     * @param type      the expected type (e.g. CLICK, TYPE, NAVIGATE)
     * @return this assertion helper
     */
    public final AiExecutionAssert hasAction(final int stepIndex, final String type)
    {
        assertTrue(result.getSteps().size() > stepIndex, "No step found at index " + stepIndex);
        final var step = result.getSteps().get(stepIndex);
        assertFalse(step.getActions().isEmpty(), "No action found in step " + stepIndex);
        assertEquals(type, step.getActions().get(0).getType(), "Action type mismatch at step " + stepIndex);
        return this;
    }

    /**
     * Asserts the type and target containment of the first action of a specific step index.
     *
     * @param stepIndex      the index of the step
     * @param type           the expected type
     * @param targetContains a substring expected to be contained within the action target
     * @return this assertion helper
     */
    public final AiExecutionAssert hasAction(final int stepIndex, final String type, final String targetContains)
    {
        hasAction(stepIndex, type);
        final var step = result.getSteps().get(stepIndex);
        final String target = step.getActions().get(0).getTarget();
        if (targetContains != null && !targetContains.isEmpty())
        {
            assertTrue(target != null && target.contains(targetContains), 
                    "Action target at step " + stepIndex + " ('" + target + "') does not contain: '" + targetContains + "'");
        }
        return this;
    }

    /**
     * Asserts the type, target containment, and value of the first action of a specific step index.
     *
     * @param stepIndex      the index of the step
     * @param type           the expected type
     * @param targetContains a substring expected to be contained within the action target
     * @param value          the expected action value
     * @return this assertion helper
     */
    public final AiExecutionAssert hasAction(final int stepIndex, final String type, final String targetContains, final String value)
    {
        hasAction(stepIndex, type, targetContains);
        final var step = result.getSteps().get(stepIndex);
        assertEquals(value, step.getActions().get(0).getValue(), "Action value mismatch at step " + stepIndex);
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

    /**
     * Asserts the total number of steps in the execution result.
     *
     * @param expected the expected number of steps
     * @return this assertion helper
     */
    public final AiExecutionAssert hasStepsCount(final int expected)
    {
        assertEquals(expected, result.getSteps().size(), "Step count mismatch");
        return this;
    }
}
