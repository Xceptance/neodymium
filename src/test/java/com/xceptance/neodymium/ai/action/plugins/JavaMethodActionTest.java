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
        final String instructions = action.getPromptInstructions();
        
        Assertions.assertNotNull(instructions);
        
        // Assert basic capability description
        Assertions.assertTrue(instructions.contains("JAVA_METHOD: Invoke a Java method"));
        
        // Assert reflected methods from AiAssertions are present
        Assertions.assertTrue(instructions.contains("assertPriceGreaterThanZero(String)"));
        Assertions.assertTrue(instructions.contains("assertGreaterThanZero(String)"));
        Assertions.assertTrue(instructions.contains("verifyCalculation(String)"));
        Assertions.assertTrue(instructions.contains("assertNumberGreaterThan(String)"));
        Assertions.assertTrue(instructions.contains("assertNumberEqual(String)"));
    }
}
