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
package org.neodymium.ai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TokenUsage} record and its null-safe factory methods.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class TokenUsageTest
{
    @Test
    public void testCanonicalConstructor()
    {
        final TokenUsage usage = new TokenUsage(100, 50, 150, 25);
        assertEquals(100, usage.inputTokenCount());
        assertEquals(50, usage.outputTokenCount());
        assertEquals(150, usage.totalTokenCount());
        assertEquals(25, usage.cachedTokenCount());
    }

    @Test
    public void testThreeArgConstructorDefaultsCachedToZero()
    {
        final TokenUsage usage = new TokenUsage(80, 20, 100);
        assertEquals(80, usage.inputTokenCount());
        assertEquals(20, usage.outputTokenCount());
        assertEquals(100, usage.totalTokenCount());
        assertEquals(0, usage.cachedTokenCount());
    }

    @Test
    public void testOfAllNulls()
    {
        final TokenUsage usage = TokenUsage.of(null, null, null, null);
        assertNotNull(usage);
        assertEquals(0, usage.inputTokenCount());
        assertEquals(0, usage.outputTokenCount());
        assertEquals(0, usage.totalTokenCount());
        assertEquals(0, usage.cachedTokenCount());
    }

    @Test
    public void testOfThreeArgAllNulls()
    {
        final TokenUsage usage = TokenUsage.of(null, null, null);
        assertNotNull(usage);
        assertEquals(0, usage.inputTokenCount());
        assertEquals(0, usage.outputTokenCount());
        assertEquals(0, usage.totalTokenCount());
        assertEquals(0, usage.cachedTokenCount());
    }

    @Test
    public void testOfDerivesTotalWhenNull()
    {
        final TokenUsage usage = TokenUsage.of(120, 80, null, 15);
        assertNotNull(usage);
        assertEquals(120, usage.inputTokenCount());
        assertEquals(80, usage.outputTokenCount());
        assertEquals(200, usage.totalTokenCount());
        assertEquals(15, usage.cachedTokenCount());
    }

    @Test
    public void testOfPartialNullsNullOutput()
    {
        final TokenUsage usage = TokenUsage.of(150, null, null, null);
        assertNotNull(usage);
        assertEquals(150, usage.inputTokenCount());
        assertEquals(0, usage.outputTokenCount());
        assertEquals(150, usage.totalTokenCount());
        assertEquals(0, usage.cachedTokenCount());
    }

    @Test
    public void testOfPartialNullsNullInput()
    {
        final TokenUsage usage = TokenUsage.of(null, 65, null, 10);
        assertNotNull(usage);
        assertEquals(0, usage.inputTokenCount());
        assertEquals(65, usage.outputTokenCount());
        assertEquals(65, usage.totalTokenCount());
        assertEquals(10, usage.cachedTokenCount());
    }

    @Test
    public void testOfExplicitValuesPreserved()
    {
        final TokenUsage usage = TokenUsage.of(500, 200, 700, 100);
        assertNotNull(usage);
        assertEquals(500, usage.inputTokenCount());
        assertEquals(200, usage.outputTokenCount());
        assertEquals(700, usage.totalTokenCount());
        assertEquals(100, usage.cachedTokenCount());
    }

    @Test
    public void testOfNegativeValuesClampedToZero()
    {
        final TokenUsage usage = TokenUsage.of(-10, -5, null, -1);
        assertNotNull(usage);
        assertEquals(0, usage.inputTokenCount());
        assertEquals(0, usage.outputTokenCount());
        assertEquals(0, usage.totalTokenCount());
        assertEquals(0, usage.cachedTokenCount());
    }
}
