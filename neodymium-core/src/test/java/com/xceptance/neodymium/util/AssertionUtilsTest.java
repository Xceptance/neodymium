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
package com.xceptance.neodymium.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AssertionUtils}.
 * 
 * @author AI-generated: Gemini 3.1 Pro
 * @author Xceptance GmbH 2026
 */
public class AssertionUtilsTest
{
    @Test
    public void testFirstOptionPasses()
    {
        Assertions.assertDoesNotThrow(() -> 
        {
            AssertionUtils.assertAnyOf(
                () -> Assertions.assertTrue(true),
                () -> Assertions.fail("Should not execute")
            );
        });
    }

    @Test
    public void testSecondOptionPasses()
    {
        Assertions.assertDoesNotThrow(() -> 
        {
            AssertionUtils.assertAnyOf(
                () -> Assertions.fail("First failed"),
                () -> Assertions.assertTrue(true)
            );
        });
    }

    @Test
    public void testAllOptionsFail()
    {
        final AssertionError error = Assertions.assertThrows(AssertionError.class, () -> 
        {
            AssertionUtils.assertAnyOf(
                () -> Assertions.fail("First failed"),
                () -> Assertions.fail("Second failed")
            );
        });
        
        Assertions.assertTrue(error.getMessage().contains("Total failure"));
        Assertions.assertEquals(2, error.getSuppressed().length);
        Assertions.assertEquals("First failed", error.getSuppressed()[0].getMessage());
        Assertions.assertEquals("Second failed", error.getSuppressed()[1].getMessage());
    }

    @Test
    public void testHandlesExceptions()
    {
        final AssertionError error = Assertions.assertThrows(AssertionError.class, () -> 
        {
            AssertionUtils.assertAnyOf(
                () -> { throw new RuntimeException("Runtime error"); }
            );
        });
        
        Assertions.assertEquals(1, error.getSuppressed().length);
        Assertions.assertEquals("Runtime error", error.getSuppressed()[0].getMessage());
        Assertions.assertTrue(error.getSuppressed()[0] instanceof RuntimeException);
    }
    
    @Test
    public void testDoesNotSwallowFatalErrors()
    {
        Assertions.assertThrows(OutOfMemoryError.class, () -> 
        {
            AssertionUtils.assertAnyOf(
                () -> { throw new OutOfMemoryError("Fatal JVM error"); }
            );
        });
    }

    @Test
    public void testEmptyOptionsThrowsIllegalArgumentException()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> 
        {
            AssertionUtils.assertAnyOf();
        });
    }
}
