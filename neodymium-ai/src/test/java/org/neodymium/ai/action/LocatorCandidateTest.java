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
package org.neodymium.ai.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LocatorCandidate} and candidate methods in {@link Action}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class LocatorCandidateTest
{
    @Test
    public void testLocatorCandidateConstruction()
    {
        final LocatorCandidate cand = new LocatorCandidate("#submit-btn", "ID", 0.95, "Primary ID selector");
        assertEquals("#submit-btn", cand.getLocator());
        assertEquals("ID", cand.getStrategy());
        assertEquals(0.95, cand.getScore(), 0.001);
        assertEquals("Primary ID selector", cand.getReasoning());
    }

    @Test
    public void testActionCandidateLocators()
    {
        final Action action = new Action("CLICK", "#primary-btn", List.of(), "Click button", "Click submit");
        final LocatorCandidate cand1 = new LocatorCandidate("#primary-btn", "ID", 0.90, "ID");
        final LocatorCandidate cand2 = new LocatorCandidate("button.submit-btn", "CLASS", 0.98, "Better class");
        action.setCandidateLocators(List.of(cand1, cand2));

        assertEquals(2, action.getCandidateLocators().size());
        assertEquals("button.submit-btn", action.getBestCandidateLocator());
    }
}
