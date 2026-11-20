/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TestStepListener} URL matching behavior.
 */
public class TestStepListenerTest
{
    @Test
    @DisplayName("matchesUrl correctly matches regex patterns, literal substrings, and query strings")
    public void testMatchesUrl() throws Exception
    {
        final java.lang.reflect.Method method = TestStepListener.class.getDeclaredMethod("matchesUrl", String.class, String.class);
        method.setAccessible(true);

        // Regex pattern matches
        Assertions.assertTrue((Boolean) method.invoke(null, ".*example\\.com/checkout", "https://example.com/checkout?id=123"));
        Assertions.assertFalse((Boolean) method.invoke(null, ".*example\\.com/admin", "https://example.com/checkout?id=123"));

        // Literal substring with query characters (? and &) that might be invalid/incomplete regex
        Assertions.assertTrue((Boolean) method.invoke(null, "https://example.com/item?id=1&ref=2", "https://example.com/item?id=1&ref=2"));
        Assertions.assertTrue((Boolean) method.invoke(null, "item?id=1", "https://example.com/item?id=1&ref=2"));

        // Null checks
        Assertions.assertFalse((Boolean) method.invoke(null, null, "https://example.com"));
        Assertions.assertFalse((Boolean) method.invoke(null, "https://example.com", null));
    }
}
