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
import com.xceptance.neodymium.ai.core.StepDetails;

/**
 * Fluent assertion helper for validating {@link StepDetails} instances in tests.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class StepDetailsAssert
{
    private final StepDetails step;

    /**
     * Constructs a new StepDetailsAssert instance.
     *
     * @param step the step details to validate
     */
    public StepDetailsAssert(final StepDetails step)
    {
        this.step = step;
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
     * Asserts that the step is direct parsed (directParse == true, replayed == false, and no LLM calls).
     *
     * @return this assertion helper
     */
    public final StepDetailsAssert isDirectParse()
    {
        assertTrue(step.isDirectParse(), "Expected step to be direct parsed");
        assertFalse(step.isReplayed(), "Expected step not to be replayed");
        assertTrue(step.getLlmCalls().isEmpty(), "Expected step to have no LLM calls");
        return this;
    }

    /**
     * Asserts that the step is not direct parsed.
     *
     * @return this assertion helper
     */
    public final StepDetailsAssert isNotDirectParse()
    {
        assertFalse(step.isDirectParse(), "Expected step not to be direct parsed");
        return this;
    }

    /**
     * Asserts that the step is replayed (directParse == false, replayed == true, and no LLM calls).
     *
     * @return this assertion helper
     */
    public final StepDetailsAssert isReplayed()
    {
        assertFalse(step.isDirectParse(), "Expected step not to be direct parsed");
        assertTrue(step.isReplayed(), "Expected step to be replayed");
        assertTrue(step.getLlmCalls().isEmpty(), "Expected step to have no LLM calls");
        return this;
    }

    /**
     * Asserts that the step is not replayed.
     *
     * @return this assertion helper
     */
    public final StepDetailsAssert isNotReplayed()
    {
        assertFalse(step.isReplayed(), "Expected step not to be replayed");
        return this;
    }

    /**
     * Asserts that the step has no LLM calls.
     *
     * @return this assertion helper
     */
    public final StepDetailsAssert hasNoLlmCalls()
    {
        assertTrue(step.getLlmCalls().isEmpty(), "Expected step to have no LLM calls");
        return this;
    }

    /**
     * Asserts that the step has a specific number of LLM calls, and is neither direct parsed nor replayed.
     *
     * @param expectedLlmCalls the expected number of LLM calls
     * @return this assertion helper
     */
    public final StepDetailsAssert hasLlmCalls(final int expectedLlmCalls)
    {
        assertFalse(step.isDirectParse(), "Expected step not to be direct parsed");
        assertFalse(step.isReplayed(), "Expected step not to be replayed");
        assertEquals(expectedLlmCalls, step.getLlmCalls().size(), "Step LLM call count mismatch");
        return this;
    }

    /**
     * Asserts that JIT PESAP was called for this step.
     *
     * @return this assertion helper
     */
    public final StepDetailsAssert hasPesapCall()
    {
        assertTrue(step.isPesapCalled(), "Expected JIT PESAP to be called for this step");
        return this;
    }

    /**
     * Asserts that JIT PESAP was not called for this step.
     *
     * @return this assertion helper
     */
    public final StepDetailsAssert hasNoPesapCall()
    {
        assertFalse(step.isPesapCalled(), "Expected JIT PESAP not to be called for this step");
        return this;
    }

    /**
     * Asserts the type of an action at a specific index in this step.
     *
     * @param index the index of the action
     * @param type  the expected type
     * @return this assertion helper
     */
    public final StepDetailsAssert hasAction(final int index, final String type)
    {
        assertTrue(step.getActions().size() > index, "No action found at index " + index);
        assertEquals(type, step.getActions().get(index).getType(), "Action type mismatch at index " + index);
        return this;
    }

    /**
     * Asserts the count of actions
     *
     * @param expectedCount the count of actions
     * @return this assertion helper
     */
    public final StepDetailsAssert hasActionsCount(final int expectedCount)
    {
        assertEquals(expectedCount, step.getActions().size(), "Action count mismatch");
        return this;
    }

    /**
     * Asserts the type and target containment of an action at a specific index in this step.
     *
     * @param index          the index of the action
     * @param type           the expected type
     * @param targetContains a substring expected to be contained within the action target
     * @return this assertion helper
     */
    public final StepDetailsAssert hasAction(final int index, final String type, final String targetContains)
    {
        hasAction(index, type);
        final String target = step.getActions().get(index).getTarget();
        if (targetContains != null && !targetContains.isEmpty())
        {
            assertTrue(target != null && target.contains(targetContains), 
                    "Action target at index " + index + " ('" + target + "') does not contain: '" + targetContains + "'");
        }
        return this;
    }

    /**
     * Asserts the type, target containment, and value of an action at a specific index in this step.
     *
     * @param index          the index of the action
     * @param type           the expected type
     * @param targetContains a substring expected to be contained within the action target
     * @param value          the expected action value
     * @return this assertion helper
     */
    public final StepDetailsAssert hasAction(final int index, final String type, final String targetContains, final String value)
    {
        hasAction(index, type, targetContains);
        assertEquals(value, step.getActions().get(index).getValue(), "Action value mismatch at index " + index);
        return this;
    }

    /**
     * Performs assertions on a specific action in this step.
     *
     * @param actionIndex the 0-based index of the action
     * @param assertion   the action assertions to run
     * @return this assertion helper
     */
    public final StepDetailsAssert action(final int actionIndex, final Consumer<AiActionAssert> assertion)
    {
        assertTrue(step.getActions().size() > actionIndex, "No action found at index " + actionIndex);
        assertion.accept(new AiActionAssert(step.getActions().get(actionIndex)));
        return this;
    }

    /**
     * Asserts the expanded instruction of the step.
     *
     * @param expected the expected expanded instruction
     * @return this assertion helper
     */
    public final StepDetailsAssert hasExpandedInstruction(final String expected)
    {
        assertEquals(expected, step.getExpandedInstruction(), "Step expanded instruction mismatch");
        return this;
    }

    /**
     * Asserts the original unsplit instruction of the step.
     *
     * @param expected the expected original unsplit instruction
     * @return this assertion helper
     */
    public final StepDetailsAssert hasOriginalUnsplitInstruction(final String expected)
    {
        assertEquals(expected, step.getOriginalUnsplitInstruction(), "Step original unsplit instruction mismatch");
        return this;
    }

    /**
     * Asserts the number of escalations that occurred during this step.
     *
     * @param expected the expected number of escalations
     * @return this assertion helper
     */
    public final StepDetailsAssert hasEscalations(final int expected)
    {
        assertEquals(expected, step.getEscalations().size(), "Step escalation count mismatch");
        return this;
    }

    /**
     * Asserts that no escalations occurred during this step.
     *
     * @return this assertion helper
     */
    public final StepDetailsAssert hasNoEscalations()
    {
        return hasEscalations(0);
    }
}
