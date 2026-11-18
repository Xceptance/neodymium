/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
