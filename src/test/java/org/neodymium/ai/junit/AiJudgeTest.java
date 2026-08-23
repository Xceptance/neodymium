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

import java.lang.reflect.Method;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiJudge} annotation parsing and reflection handling.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class AiJudgeTest
{
    @AiJudge
    private static class ClassWithDefaultJudge
    {
    }

    @AiJudge({false, true})
    private static class ClassWithJudgeMatrix
    {
        @AiJudge({true})
        public void methodOverride()
        {
        }

        public void inheritClass()
        {
        }
    }

    @Test
    @DisplayName("AiJudge default value is true")
    public void testAiJudgeDefaultValue()
    {
        final AiJudge annot = ClassWithDefaultJudge.class.getAnnotation(AiJudge.class);
        Assertions.assertNotNull(annot);
        Assertions.assertArrayEquals(new boolean[]{true}, annot.value());
    }

    @Test
    @DisplayName("AiJudge supports multi-boolean matrix array")
    public void testAiJudgeMatrix()
    {
        final AiJudge annot = ClassWithJudgeMatrix.class.getAnnotation(AiJudge.class);
        Assertions.assertNotNull(annot);
        Assertions.assertArrayEquals(new boolean[]{false, true}, annot.value());
    }

    @Test
    @DisplayName("AiJudge method-level annotation overrides class-level annotation")
    public void testAiJudgeMethodOverride() throws NoSuchMethodException
    {
        final Method overrideMethod = ClassWithJudgeMatrix.class.getMethod("methodOverride");
        final AiJudge annot = overrideMethod.getAnnotation(AiJudge.class);
        Assertions.assertNotNull(annot);
        Assertions.assertArrayEquals(new boolean[]{true}, annot.value());

        final Method inheritMethod = ClassWithJudgeMatrix.class.getMethod("inheritClass");
        Assertions.assertNull(inheritMethod.getAnnotation(AiJudge.class));
    }
}
