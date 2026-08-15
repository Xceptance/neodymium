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
package org.neodymium.ai.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying CSS selector sanitization in {@link CssSelectorSanitizer}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class CssSelectorSanitizerTest
{
    @Test
    public void testSanitizeDecimalDots()
    {
        final String rawDecimal = "span.cart-badge.bg-indigo-600.text-white.rounded-full.px-2.py-0.5.text-xs.font-bold";
        final String expectedDecimal = "span.cart-badge.bg-indigo-600.text-white.rounded-full.px-2.py-0\\.5.text-xs.font-bold";
        Assertions.assertEquals(expectedDecimal, CssSelectorSanitizer.sanitize(rawDecimal));
    }

    @Test
    public void testSanitizeFractionalSlashes()
    {
        final String rawSlashes = "div.w-1/2.h-3/4.p-2.5";
        final String expectedSlashes = "div.w-1\\/2.h-3\\/4.p-2\\.5";
        Assertions.assertEquals(expectedSlashes, CssSelectorSanitizer.sanitize(rawSlashes));
    }

    @Test
    public void testSanitizeTailwindVariants()
    {
        final String rawVariants = "div.sm:flex.md:hidden.hover:bg-blue-500";
        final String expectedVariants = "div.sm\\:flex.md\\:hidden.hover\\:bg-blue-500";
        Assertions.assertEquals(expectedVariants, CssSelectorSanitizer.sanitize(rawVariants));
    }

    @Test
    public void testSanitizePseudoClasses()
    {
        final String rawPseudo = "div.size-btn.cursor-pointer:nth-of-type(4)";
        final String expectedPseudo = "div.size-btn.cursor-pointer:nth-of-type(4)";
        Assertions.assertEquals(expectedPseudo, CssSelectorSanitizer.sanitize(rawPseudo));

        final String rawNotPseudo = "ul > li:first-child:not(.active)";
        final String expectedNotPseudo = "ul > li:first-child:not(.active)";
        Assertions.assertEquals(expectedNotPseudo, CssSelectorSanitizer.sanitize(rawNotPseudo));
    }

    @Test
    public void testSanitizeCombinedVariantsAndPseudo()
    {
        final String rawCombined = "button.size-btn.hover:bg-slate-200:nth-of-type(2)";
        final String expectedCombined = "button.size-btn.hover\\:bg-slate-200:nth-of-type(2)";
        Assertions.assertEquals(expectedCombined, CssSelectorSanitizer.sanitize(rawCombined));
    }

    @Test
    public void testSanitizeArbitraryBrackets()
    {
        final String rawBrackets = "div.top-[10px].bg-[#f0f0f0]";
        final String expectedBrackets = "div.top-\\[10px\\].bg-\\[\\#f0f0f0\\]";
        Assertions.assertEquals(expectedBrackets, CssSelectorSanitizer.sanitize(rawBrackets));
    }

    @Test
    public void testSanitizeFullMix()
    {
        final String rawFullMix = "a.btn-primary.px-1.5.w-1/3:hover";
        final String expectedFullMix = "a.btn-primary.px-1\\.5.w-1\\/3:hover";
        Assertions.assertEquals(expectedFullMix, CssSelectorSanitizer.sanitize(rawFullMix));
    }

    @Test
    public void testSanitizeAttributeSelectorNotEscaped()
    {
        final String rawAttr = "button.product-quick-add[aria-label=\"Add Premium Off-White shirts to shopping cart\"]";
        final String expectedAttr = "button.product-quick-add[aria-label=\"Add Premium Off-White shirts to shopping cart\"]";
        Assertions.assertEquals(expectedAttr, CssSelectorSanitizer.sanitize(rawAttr));

        final String rawDataAi = "a.nav-link[data-ai=\"xc123\"]";
        final String expectedDataAi = "a.nav-link[data-ai=\"xc123\"]";
        Assertions.assertEquals(expectedDataAi, CssSelectorSanitizer.sanitize(rawDataAi));

        // Attribute presence selectors (without equals sign)
        final String rawRequired = "input.form-control[required]";
        Assertions.assertEquals("input.form-control[required]", CssSelectorSanitizer.sanitize(rawRequired));

        final String rawDisabled = ".btn[disabled]";
        Assertions.assertEquals(".btn[disabled]", CssSelectorSanitizer.sanitize(rawDisabled));

        final String rawHref = "a.link[href]";
        Assertions.assertEquals("a.link[href]", CssSelectorSanitizer.sanitize(rawHref));
    }

    @Test
    public void testSanitizePseudoClassesOnStateClasses()
    {
        final String rawActiveHover = ".active:hover";
        Assertions.assertEquals(".active:hover", CssSelectorSanitizer.sanitize(rawActiveHover));

        final String rawDisabledFocus = ".disabled:focus";
        Assertions.assertEquals(".disabled:focus", CssSelectorSanitizer.sanitize(rawDisabledFocus));

        final String rawFocusWithin = ".form-group.focus:focus-within";
        Assertions.assertEquals(".form-group.focus:focus-within", CssSelectorSanitizer.sanitize(rawFocusWithin));
    }
}
