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
package org.neodymium.ai.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.DomFeatureVector;

/**
 * Unit tests for {@link PlaybookToolReplayer#resolveSelectorForCandidate(DomFeatureVector)}.
 * <p>
 * The selector built here is written back into the replayed tool call, so it must either match the
 * candidate element or not be produced at all.
 *
 * @author AI-generated: Claude Sonnet 5.5
 * @author Xceptance GmbH 2026
 */
public class HealedSelectorResolutionTest
{
    private static DomFeatureVector candidate(
        final String tag,
        final String text,
        final Set<String> classes,
        final Map<String, String> attributes,
        final String accessibleName,
        final int x,
        final int y,
        final int width,
        final int height)
    {
        return new DomFeatureVector(tag, text, classes, attributes, "", accessibleName, "div", 0, x, y, width, height);
    }

    /**
     * An accessible name usually comes from the element's text or a label, not from an {@code aria-label}
     * attribute. Turning it into {@code [aria-label="..."]} produced a selector that could never match.
     */
    @Test
    public void testAccessibleNameIsNeverTurnedIntoAriaLabelAttributeSelector()
    {
        final DomFeatureVector button = candidate("button", "", Collections.emptySet(), Collections.emptyMap(), "Checkout", 0, 0, 0, 0);

        final String selector = PlaybookToolReplayer.resolveSelectorForCandidate(button);

        assertTrue(selector == null || !selector.contains("aria-label"),
            "Accessible name must not be rewritten into an aria-label attribute selector, got: " + selector);
    }

    /**
     * A genuine {@code aria-label} attribute is still a valid, matching selector.
     */
    @Test
    public void testRealAriaLabelAttributeStillUsed()
    {
        final DomFeatureVector button = candidate("button", "", Collections.emptySet(), Map.of("aria-label", "Close"), "Close", 0, 0, 0, 0);

        assertEquals("button[aria-label=\"Close\"]", PlaybookToolReplayer.resolveSelectorForCandidate(button));
    }

    /**
     * Raw viewport coordinates are not an element identity and must never be written into a recording by healing.
     */
    @Test
    public void testHealingNeverProducesCoordinateSelector()
    {
        final DomFeatureVector icon = candidate("div", "", Collections.emptySet(), Collections.emptyMap(), "", 120, 340, 24, 24);

        final String selector = PlaybookToolReplayer.resolveSelectorForCandidate(icon);

        assertTrue(selector == null || !selector.toLowerCase().contains("coord"),
            "Healing must not persist coordinates, got: " + selector);
    }

    /**
     * Attribute values containing a double quote must be escaped, otherwise the CSS selector is malformed.
     */
    @Test
    public void testAttributeValuesAreEscaped()
    {
        final DomFeatureVector testId = candidate("button", "", Collections.emptySet(), Map.of("data-testid", "say \"hi\""), "", 0, 0, 0, 0);
        final DomFeatureVector named = candidate("input", "", Collections.emptySet(), Map.of("name", "a\\b"), "", 0, 0, 0, 0);
        final DomFeatureVector labelled = candidate("button", "", Collections.emptySet(), Map.of("aria-label", "5\" pipe"), "", 0, 0, 0, 0);

        assertEquals("button[data-testid=\"say \\\"hi\\\"\"]", PlaybookToolReplayer.resolveSelectorForCandidate(testId));
        assertEquals("input[name=\"a\\\\b\"]", PlaybookToolReplayer.resolveSelectorForCandidate(named));
        assertEquals("button[aria-label=\"5\\\" pipe\"]", PlaybookToolReplayer.resolveSelectorForCandidate(labelled));
    }

    /**
     * Identity-bearing selectors keep their existing priority: id first.
     */
    @Test
    public void testIdStillPreferred()
    {
        final DomFeatureVector withId = candidate("button", "Buy", Set.of("btn"), Map.of("id", "buy", "name", "n"), "Buy", 0, 0, 0, 0);

        assertEquals("#buy", PlaybookToolReplayer.resolveSelectorForCandidate(withId));
        assertFalse(PlaybookToolReplayer.resolveSelectorForCandidate(withId).contains("aria-label"));
    }

    /**
     * Non-standard CSS IDs (e.g. starting with digits, containing colons, dots, slashes)
     * cannot use raw `#id` syntax without CSS escaping; they must use `[id="..."]`.
     */
    @Test
    public void testNonStandardIdUsesAttributeSelector()
    {
        final DomFeatureVector digitId = candidate("button", "", Collections.emptySet(), Map.of("id", "123startWithDigit"), "", 0, 0, 0, 0);
        final DomFeatureVector colonId = candidate("input", "", Collections.emptySet(), Map.of("id", "form:email"), "", 0, 0, 0, 0);
        final DomFeatureVector standardId = candidate("div", "", Collections.emptySet(), Map.of("id", "valid-id_1"), "", 0, 0, 0, 0);

        assertEquals("button[id=\"123startWithDigit\"]", PlaybookToolReplayer.resolveSelectorForCandidate(digitId));
        assertEquals("input[id=\"form:email\"]", PlaybookToolReplayer.resolveSelectorForCandidate(colonId));
        assertEquals("#valid-id_1", PlaybookToolReplayer.resolveSelectorForCandidate(standardId));
    }

    /**
     * CSS utility classes with colons or slashes (e.g. Tailwind `md:flex`, `w-1/2`) break
     * standard CSS class selectors unless escaped. Invalid CSS class tokens must be filtered out.
     */
    @Test
    public void testTailwindAndComplexClassesFiltered()
    {
        final DomFeatureVector element = candidate("button", "", Set.of("btn", "md:flex", "w-1/2"), Collections.emptyMap(), "", 0, 0, 0, 0);
        final String selector = PlaybookToolReplayer.resolveSelectorForCandidate(element);

        assertEquals("button.btn", selector);
    }

    /**
     * Backslashes and quotes inside text must be escaped for `:has-text(...)`.
     */
    @Test
    public void testHasTextEscaping()
    {
        final DomFeatureVector element = candidate("span", "Path: C:\\docs and \"quote\"", Collections.emptySet(), Collections.emptyMap(), "", 0, 0, 0, 0);
        final String selector = PlaybookToolReplayer.resolveSelectorForCandidate(element);

        assertEquals("span:has-text(\"Path: C:\\\\docs and \\\"quote\\\"\")", selector);
    }
}
