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
 *
 * // AI-generated: Gemini 3.5 Flash
 */
package com.xceptance.neodymium.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.util.Neodymium;

/**
 * Tests for the unified expected failure (bug tags) feature in both standard Neodymium
 * programmatic tests and the AI Agent playbook engine.
 */
public final class AiAgentExpectedFailureTest
{
    /**
     * Tests case-insensitive matching and stripping regex for expected failures.
     *
     * @throws Exception if reflection fails
     */
    @Test
    public void testBugTagRegexPattern() throws Exception
    {
        final Field patternField = AiAgent.class.getDeclaredField("BUG_TAG_PATTERN");
        patternField.setAccessible(true);
        final Pattern pattern = (Pattern) patternField.get(null);
        assertNotNull(pattern);

        // Test matching variations
        final String input1 = "Click the button (bug)";
        final Matcher matcher1 = pattern.matcher(input1);
        assertTrue(matcher1.find());
        assertNull(matcher1.group(1));

        final String stripped1 = matcher1.replaceAll("").replaceAll("\\s+", " ").trim();
        assertEquals("Click the button", stripped1);

        final String input2 = "Type text in input (bug: APP-1234)";
        final Matcher matcher2 = pattern.matcher(input2);
        assertTrue(matcher2.find());
        assertEquals("APP-1234", matcher2.group(1));

        final String stripped2 = matcher2.replaceAll("").replaceAll("\\s+", " ").trim();
        assertEquals("Type text in input", stripped2);

        final String input3 = "Check visual layout (BUG: APP-9171) (visual)";
        final Matcher matcher3 = pattern.matcher(input3);
        assertTrue(matcher3.find());
        assertEquals("APP-9171", matcher3.group(1));

        final String stripped3 = matcher3.replaceAll("").replaceAll("\\s+", " ").trim();
        assertEquals("Check visual layout (visual)", stripped3);

        final String input4 = "No bug tags present here";
        final Matcher matcher4 = pattern.matcher(input4);
        assertFalse(matcher4.find());
    }

    /**
     * Tests that the programmatic expectFailure API correctly catches and swallows
     * the expected exception.
     */
    @Test
    public void testProgrammaticExpectFailureSuccess()
    {
        // Simple expectFailure
        Neodymium.expectFailure("BUG-1", () -> {
            throw new IllegalArgumentException("Expected failure");
        });

        // Fluent expectFailure with type matching
        Neodymium.expectFailure("BUG-2")
                 .ofType(NullPointerException.class)
                 .run(() -> {
                     throw new NullPointerException("NPE");
                 });

        // Fluent expectFailure with message pattern matching
        Neodymium.expectFailure("BUG-3")
                 .withMessage("failed to load")
                 .run(() -> {
                     throw new RuntimeException("Database failed to load details");
                 });

        // Fluent expectFailure with type and message pattern matching
        Neodymium.expectFailure("BUG-4")
                 .ofType(IllegalStateException.class)
                 .withMessage("invalid state")
                 .run(() -> {
                     throw new IllegalStateException("System in invalid state.");
                 });
    }

    /**
     * Tests that the programmatic expectFailure API fails the test (throws AssertionError)
     * if the code block under test completes successfully.
     */
    @Test
    public void testProgrammaticExpectFailureFailsOnSuccess()
    {
        final AssertionError error = assertThrows(AssertionError.class, () -> {
            Neodymium.expectFailure("BUG-5", () -> {
                // Succeeded! That is bad because we expected a failure.
                final int a = 1 + 1;
            });
        });

        assertTrue(error.getMessage().contains("BUG-5"));
        assertTrue(error.getMessage().contains("completed successfully"));
    }

    /**
     * Tests that the programmatic expectFailure API fails the test (throws AssertionError)
     * if a wrong exception type is thrown.
     */
    @Test
    public void testProgrammaticExpectFailureFailsOnWrongExceptionType()
    {
        final AssertionError error = assertThrows(AssertionError.class, () -> {
            Neodymium.expectFailure("BUG-6")
                     .ofType(NullPointerException.class)
                     .run(() -> {
                         throw new IllegalArgumentException("Different exception type");
                     });
        });

        assertTrue(error.getMessage().contains("BUG-6"));
        assertTrue(error.getMessage().contains("Expected failure of type(s)"));
        assertTrue(error.getCause() instanceof IllegalArgumentException);
    }

    /**
     * Tests that the programmatic expectFailure API fails the test (throws AssertionError)
     * if the exception message does not match.
     */
    @Test
    public void testProgrammaticExpectFailureFailsOnWrongMessage()
    {
        final AssertionError error = assertThrows(AssertionError.class, () -> {
            Neodymium.expectFailure("BUG-7")
                     .withMessage("connection timed out")
                     .run(() -> {
                         throw new RuntimeException("unauthorized access");
                     });
        });

        assertTrue(error.getMessage().contains("BUG-7"));
        assertTrue(error.getMessage().contains("Expected failure message pattern mismatch"));
    }
}
