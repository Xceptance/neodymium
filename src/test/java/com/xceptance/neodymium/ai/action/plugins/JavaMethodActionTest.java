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
package com.xceptance.neodymium.ai.action.plugins;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link JavaMethodAction} plugin.
 * Specifically verifies that the prompt instructions dynamically reflect and document
 * all available static methods in registered utility classes.
 */
public final class JavaMethodActionTest
{
    /**
     * Verifies that getPromptInstructions returns instructions containing the
     * expected utility method names and parameters from AiAssertions.
     */
    @Test
    public void testGetPromptInstructionsIncludesUtilityMethods()
    {
        final JavaMethodAction action = new JavaMethodAction();
        final String instructions = action.getPromptInstructions(JavaMethodActionTest.class, null);
        
        Assertions.assertNotNull(instructions);
        
        // Assert basic capability description
        Assertions.assertTrue(instructions.contains("JAVA_METHOD: Invoke a Java method"));
        
        // Assert reflected methods from AiAssertions are present
        Assertions.assertTrue(instructions.contains("assertPriceGreaterThanZero(String)"));
        Assertions.assertTrue(instructions.contains("assertGreaterThanZero(String)"));
        Assertions.assertTrue(instructions.contains("assertCalculation(String)"));
        Assertions.assertTrue(instructions.contains("assertNumberGreaterThan(String)"));
        Assertions.assertTrue(instructions.contains("assertNumbersEqual(String)"));
    }
}
