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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Playbook} class.
 * Validates root model initialization, steps registration, and deep unmodifiable immutability guarantees.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookTest
{
    /**
     * Verifies that the Playbook successfully initializes with provided steps and datasets.
     */
    @Test
    public void testInitialization()
    {
        final List<PlaybookStep> steps = new ArrayList<>();
        steps.add(new PlaybookStep("Step 1"));
        
        final List<Map<String, SessionData.DataEntry>> datasets = new ArrayList<>();
        final Map<String, SessionData.DataEntry> dataset = new HashMap<>();
        dataset.put("username", new SessionData.DataEntry("user1", false));
        datasets.add(dataset);

        final Playbook playbook = new Playbook(steps, datasets);

        // Verify that steps list is correctly populated
        assertEquals(1, playbook.getSteps().size());
        assertEquals("Step 1", playbook.getSteps().get(0).getInstruction());

        // Verify that dataset maps are correctly populated
        assertEquals(1, playbook.getDataSets().size());
        assertEquals("user1", playbook.getDataSets().get(0).get("username").value());
    }

    /**
     * Verifies the strict immutability behavior of the Playbook wrapper class.
     * Accessing steps, datasets lists, or nested maps should throw UnsupportedOperationException if mutated.
     */
    @Test
    public void testImmutability()
    {
        final List<PlaybookStep> steps = new ArrayList<>();
        steps.add(new PlaybookStep("Step 1"));

        final List<Map<String, SessionData.DataEntry>> datasets = new ArrayList<>();
        final Map<String, SessionData.DataEntry> dataset = new HashMap<>();
        dataset.put("username", new SessionData.DataEntry("user1", false));
        datasets.add(dataset);

        final Playbook playbook = new Playbook(steps, datasets);

        // Verify that steps list cannot be modified after construction
        assertThrows(UnsupportedOperationException.class, () -> {
            playbook.getSteps().add(new PlaybookStep("Step 2"));
        });

        // Verify that the datasets wrapper list itself cannot be modified
        assertThrows(UnsupportedOperationException.class, () -> {
            playbook.getDataSets().add(new HashMap<>());
        });

        // Verify that individual inner dataset parameter maps cannot be modified
        assertThrows(UnsupportedOperationException.class, () -> {
            playbook.getDataSets().get(0).put("password", new SessionData.DataEntry("pass", true));
        });
    }

    /**
     * Verifies that {@link Playbook#builder()} programmatically constructs a valid Playbook instance.
     */
    @Test
    public void testPlaybookBuilder()
    {
        final Playbook playbook = Playbook.builder()
            .step("Navigate to homepage")
            .step("Click login button")
            .build();

        assertNotNull(playbook);
        assertEquals(2, playbook.getSteps().size());
        assertEquals("Navigate to homepage", playbook.getSteps().get(0).getInstruction());
        assertEquals("Click login button", playbook.getSteps().get(1).getInstruction());
    }
}
