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
