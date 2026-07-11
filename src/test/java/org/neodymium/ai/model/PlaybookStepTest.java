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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PlaybookStep} class.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookStepTest
{
    @Test
    public void testInitialization()
    {
        final String instruction = "Click the login button";
        final PlaybookStep step = new PlaybookStep(instruction);

        assertEquals(instruction, step.getInstruction());
        assertEquals(PlaybookStepStatus.PENDING, step.getStatus());
        assertNotNull(step.getActions());
        assertTrue(step.getActions().isEmpty());
        assertNotNull(step.getSubSteps());
        assertTrue(step.getSubSteps().isEmpty());
        assertFalse(step.isComposite());
    }

    @Test
    public void testStatusTransition()
    {
        final PlaybookStep step = new PlaybookStep("Instruction");
        assertEquals(PlaybookStepStatus.PENDING, step.getStatus());

        step.setStatus(PlaybookStepStatus.RUNNING);
        assertEquals(PlaybookStepStatus.RUNNING, step.getStatus());

        step.setStatus(PlaybookStepStatus.SUCCESS);
        assertEquals(PlaybookStepStatus.SUCCESS, step.getStatus());
    }

    @Test
    public void testCompositeBehavior()
    {
        final PlaybookStep parent = new PlaybookStep("Parent instruction");
        assertFalse(parent.isComposite());

        final PlaybookStep child = new PlaybookStep("Child instruction");
        parent.getSubSteps().add(child);

        assertTrue(parent.isComposite());
        assertEquals(1, parent.getSubSteps().size());
        assertEquals(child, parent.getSubSteps().get(0));
    }
}
