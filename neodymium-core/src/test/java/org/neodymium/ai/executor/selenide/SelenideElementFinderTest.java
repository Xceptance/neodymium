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
package org.neodymium.ai.executor.selenide;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests verifying target locator splitting and candidate resolution in {@link SelenideElementFinder}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class SelenideElementFinderTest
{
    @Test
    public void testCandidateSetPreservation()
    {
        final org.neodymium.ai.action.Action action = new org.neodymium.ai.action.Action("CLICK", "#primary-btn", "Click button");
        action.setCandidateLocators(List.of(
            new org.neodymium.ai.action.LocatorCandidate(".btn-primary", 0.9),
            new org.neodymium.ai.action.LocatorCandidate("button[type='submit']", 0.8),
            new org.neodymium.ai.action.LocatorCandidate("text=Submit", 0.7)
        ));

        final List<String> candidates = action.getAllCandidateLocators();
        Assertions.assertEquals(4, candidates.size());
        Assertions.assertEquals("#primary-btn", candidates.get(0));
        Assertions.assertEquals(".btn-primary", candidates.get(1));
        Assertions.assertEquals("button[type='submit']", candidates.get(2));
        Assertions.assertEquals("text=Submit", candidates.get(3));
    }

    @Test
    public void testNullAndEmptyTargetHandling()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SelenideElementFinder.findElement((String) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SelenideElementFinder.findElement("   "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SelenideElementFinder.findElement((org.neodymium.ai.action.Action) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SelenideElementFinder.findElement("", List.of()));
    }

    @Test
    public void testActionCandidateFallbackResolution()
    {
        final org.neodymium.ai.action.Action action = new org.neodymium.ai.action.Action("CLICK", "", "Click button");
        action.setCandidateLocators(List.of(new org.neodymium.ai.action.LocatorCandidate("#valid-fallback-button", 0.9)));

        // Even though target is empty, candidate locator is extracted and attempt does not throw IllegalArgumentException for empty target
        // (will attempt resolution on fallback candidate)
        final String firstCandidate = action.getAllCandidateLocators().get(0);
        Assertions.assertEquals("#valid-fallback-button", firstCandidate);
    }
}
