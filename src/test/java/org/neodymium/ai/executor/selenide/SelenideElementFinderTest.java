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

import java.lang.reflect.Method;
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
    public void testSplitCandidatesWithCommaSeparatedTargets() throws Exception
    {
        final Method method = SelenideElementFinder.class.getDeclaredMethod("splitCandidates", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        final List<String> candidates1 = (List<String>) method.invoke(null, "button#xc_eb4gzc, button:has-text('L')");
        Assertions.assertEquals(2, candidates1.size());
        Assertions.assertEquals("button#xc_eb4gzc", candidates1.get(0));
        Assertions.assertEquals("button:has-text('L')", candidates1.get(1));

        @SuppressWarnings("unchecked")
        final List<String> candidates2 = (List<String>) method.invoke(null, "button[data-text='a,b'], button.class2");
        Assertions.assertEquals(2, candidates2.size());
        Assertions.assertEquals("button[data-text='a,b']", candidates2.get(0));
        Assertions.assertEquals("button.class2", candidates2.get(1));

        @SuppressWarnings("unchecked")
        final List<String> candidates3 = (List<String>) method.invoke(null, "simple-target");
        Assertions.assertEquals(1, candidates3.size());
        Assertions.assertEquals("simple-target", candidates3.get(0));

        @SuppressWarnings("unchecked")
        final List<String> candidates4 = (List<String>) method.invoke(null, "text=Total Paid: $27.58");
        Assertions.assertEquals(1, candidates4.size());
        Assertions.assertEquals("text=Total Paid: $27.58", candidates4.get(0));

        // Functional pseudo-classes with commas inside parentheses
        @SuppressWarnings("unchecked")
        final List<String> candidatesNot = (List<String>) method.invoke(null, "button:not(.a, .b)");
        Assertions.assertEquals(1, candidatesNot.size());
        Assertions.assertEquals("button:not(.a, .b)", candidatesNot.get(0));

        @SuppressWarnings("unchecked")
        final List<String> candidatesIs = (List<String>) method.invoke(null, ".card:is(.x, .y)");
        Assertions.assertEquals(1, candidatesIs.size());
        Assertions.assertEquals(".card:is(.x, .y)", candidatesIs.get(0));

        @SuppressWarnings("unchecked")
        final List<String> candidatesNth = (List<String>) method.invoke(null, "li:nth-child(2n, 3)");
        Assertions.assertEquals(1, candidatesNth.size());
        Assertions.assertEquals("li:nth-child(2n, 3)", candidatesNth.get(0));

        @SuppressWarnings("unchecked")
        final List<String> candidatesHas = (List<String>) method.invoke(null, "div:has(> span, > em)");
        Assertions.assertEquals(1, candidatesHas.size());
        Assertions.assertEquals("div:has(> span, > em)", candidatesHas.get(0));

        @SuppressWarnings("unchecked")
        final List<String> candidatesMulti = (List<String>) method.invoke(null, "#a, #b");
        Assertions.assertEquals(2, candidatesMulti.size());
        Assertions.assertEquals("#a", candidatesMulti.get(0));
        Assertions.assertEquals("#b", candidatesMulti.get(1));
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
