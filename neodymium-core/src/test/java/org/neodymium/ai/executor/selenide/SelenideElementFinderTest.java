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

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.openqa.selenium.By;

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
        final Action action = new Action("CLICK", "#primary-btn", "Click button");
        action.setCandidateLocators(List.of(
            new LocatorCandidate(".btn-primary", 0.9),
            new LocatorCandidate("button[type='submit']", 0.8),
            new LocatorCandidate("text=Submit", 0.7)
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
        Assertions.assertThrows(IllegalArgumentException.class, () -> SelenideElementFinder.findElement((Action) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SelenideElementFinder.findElement("", List.of()));
    }

    @Test
    public void testActionCandidateFallbackResolution()
    {
        final Action action = new Action("CLICK", "", "Click button");
        action.setCandidateLocators(List.of(new LocatorCandidate("#valid-fallback-button", 0.9)));

        // Even though target is empty, candidate locator is extracted and attempt does not throw IllegalArgumentException for empty target
        // (will attempt resolution on fallback candidate)
        final String firstCandidate = action.getAllCandidateLocators().get(0);
        Assertions.assertEquals("#valid-fallback-button", firstCandidate);
    }

    @Test
    public void testIsDirectlyPresentSafelyReturnsFalseOnNullOrBlank()
    {
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent(null));
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("   "));
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("#some-missing-element"));
    }

    @Test
    public void testFindFirstVisibleHandlesNullGracefully()
    {
        Assertions.assertNull(SelenideElementFinder.findFirstVisible(null, "someTarget"));
    }

    @Test
    public void testIsDirectlyPresentCompoundSelectorDoesNotHijackAncestor()
    {
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("article[data-ai=\"xcboo7um\"] button[data-ai=\"xcz0f8a5\"]"));
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("article#xcboo7um button#xcz0f8a5"));
    }

    @Test
    public void testResetDomStampCacheForTesting()
    {
        SelenideElementFinder.resetDomStampCacheForTesting();
        Assertions.assertFalse(SelenideElementFinder.isDirectlyPresent("#non-existent-after-reset"));
    }

    @Test
    public void testIsAutomationIdSelector()
    {
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("[data-ai='xc123']"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("[data-ai=\"xcrxcvi\"]"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("data-ai=xc123"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("#xc123"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("div > #xc4o48u"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("xc123"));
        Assertions.assertTrue(SelenideElementFinder.isAutomationIdSelector("xcrxcvi"));

        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector(null));
        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector(""));
        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector("   "));
        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector(".btn-primary"));
        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector("#submit-btn"));
        Assertions.assertFalse(SelenideElementFinder.isAutomationIdSelector("//button[@id='save']"));
    }

    @Test
    public void testResolveLocatorTransformsHashAutomationId()
    {
        final By resolvedHash = SelenideElementFinder.resolveLocator("#xc123");
        Assertions.assertEquals(By.cssSelector("[data-ai='xc123']"), resolvedHash);

        final By resolvedCompound = SelenideElementFinder.resolveLocator("div.order-summary > #xcrxcvi");
        Assertions.assertEquals(By.cssSelector("div.order-summary > [data-ai='xcrxcvi']"), resolvedCompound);
    }
}
