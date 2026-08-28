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
