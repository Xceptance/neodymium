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
    }

    @Test
    public void testSanitizeCssSelector()
    {
        final String raw1 = "span.cart-badge.bg-indigo-600.text-white.rounded-full.px-2.py-0.5.text-xs.font-bold";
        final String expected1 = "span.cart-badge.bg-indigo-600.text-white.rounded-full.px-2.py-0\\.5.text-xs.font-bold";
        Assertions.assertEquals(expected1, LocatorResolver.sanitizeCssSelector(raw1));

        final String raw2 = "div.size-btn.cursor-pointer:nth-of-type(4)";
        final String expected2 = "div.size-btn.cursor-pointer:nth-of-type(4)";
        Assertions.assertEquals(expected2, LocatorResolver.sanitizeCssSelector(raw2));
    }
}
