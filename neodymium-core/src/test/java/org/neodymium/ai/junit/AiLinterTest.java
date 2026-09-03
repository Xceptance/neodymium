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
package org.neodymium.ai.junit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiLinter} annotation parsing and reflection handling.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiLinterTest
{
    @AiLinter
    private static class ClassWithDefaultLinter
    {
    }

    @AiLinter({false, true})
    private static class ClassWithLinterMatrix
    {
        @AiLinter({false})
        public void methodOverride()
        {
        }

        public void inheritClass()
        {
        }
    }

    @Test
    @DisplayName("AiLinter default value is true")
    public void testAiLinterDefaultValue()
    {
        final AiLinter annot = ClassWithDefaultLinter.class.getAnnotation(AiLinter.class);
        assertNotNull(annot);
        assertArrayEquals(new boolean[]{true}, annot.value());
    }

    @Test
    @DisplayName("AiLinter supports multi-boolean matrix array")
    public void testAiLinterMatrix()
    {
        final AiLinter annot = ClassWithLinterMatrix.class.getAnnotation(AiLinter.class);
        assertNotNull(annot);
        assertArrayEquals(new boolean[]{false, true}, annot.value());
    }

    @Test
    @DisplayName("AiLinter method-level annotation overrides class-level annotation")
    public void testAiLinterMethodOverride() throws NoSuchMethodException
    {
        final Method overrideMethod = ClassWithLinterMatrix.class.getMethod("methodOverride");
        final AiLinter annot = overrideMethod.getAnnotation(AiLinter.class);
        assertNotNull(annot);
        assertArrayEquals(new boolean[]{false}, annot.value());

        final Method inheritMethod = ClassWithLinterMatrix.class.getMethod("inheritClass");
        assertNull(inheritMethod.getAnnotation(AiLinter.class));
    }
}
