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
package org.neodymium.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PlaybookStep} class.
 * Ensures proper initial state, status transitions, and composite tree structure behavior.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookStepTest
{
    /**
     * Verifies that a newly created PlaybookStep initializes all fields
     * to their correct default values (pending status, empty actions, non-composite).
     */
    @Test
    public void testInitialization()
    {
        final String instruction = "Click the login button";
        final PlaybookStep step = new PlaybookStep(instruction);

        // Verify the step captures the original instruction correctly
        assertEquals(instruction, step.getInstruction());

        // Newly created steps must default to PENDING status
        assertEquals(PlaybookStepStatus.PENDING, step.getStatus());

        // Leaf actions list must be initialized and empty
        assertNotNull(step.getActions());
        assertTrue(step.getActions().isEmpty());

        // Child steps list must be initialized and empty
        assertNotNull(step.getSubSteps());
        assertTrue(step.getSubSteps().isEmpty());

        // Step should not report as composite since it has no child steps
        assertFalse(step.isComposite());
    }

    /**
     * Verifies that step status transitions behave correctly when updated.
     */
    @Test
    public void testStatusTransition()
    {
        final PlaybookStep step = new PlaybookStep("Instruction");
        
        // Initial status is PENDING
        assertEquals(PlaybookStepStatus.PENDING, step.getStatus());

        // Transition to RUNNING
        step.setStatus(PlaybookStepStatus.RUNNING);
        assertEquals(PlaybookStepStatus.RUNNING, step.getStatus());

        // Transition to SUCCESS
        step.setStatus(PlaybookStepStatus.SUCCESS);
        assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus());
    }

    /**
     * Verifies the composite pattern behavior of PlaybookStep.
     * When sub-steps are added, the step should identify as composite.
     */
    @Test
    public void testCompositeBehavior()
    {
        final PlaybookStep parent = new PlaybookStep("Parent instruction");
        
        // Initially parent should not be composite
        assertFalse(parent.isComposite());

        final PlaybookStep child = new PlaybookStep("Child instruction");
        
        // Nest the child step inside the parent step
        parent.getSubSteps().add(child);

        // Parent should now identify as composite
        assertTrue(parent.isComposite());
        assertEquals(1, parent.getSubSteps().size());
        assertEquals(child, parent.getSubSteps().get(0));
    }
}
