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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for conditional assertion blocks.
 * 
 * @author AI-generated: Gemini 3.1 Pro
 * @author Xceptance GmbH 2026
 */
public final class AssertionUtils
{
    /**
     * Prevents instantiation.
     */
    private AssertionUtils()
    {
    }

    /**
     * A functional interface for assertion blocks that might throw exceptions 
     * (e.g., WebDriver timeouts, parsing errors) or AssertionErrors.
     */
    @FunctionalInterface
    public interface AssertionBlock
    {
        void execute() throws Exception;
    }

    /**
     * Executes assertion blocks and passes if at least ONE block succeeds.
     * If all blocks fail, a combined AssertionError is thrown containing all failures.
     *
     * @param options The assertion sets to evaluate.
     */
    public static void assertAnyOf(final AssertionBlock... options)
    {
        // Fail fast if no options are provided
        if (options == null || options.length == 0)
        {
            throw new IllegalArgumentException("At least one assertion block must be provided.");
        }

        final List<Throwable> failures = new ArrayList<>();

        for (final AssertionBlock option : options)
        {
            try
            {
                option.execute();
                
                // If we reach here, the current block passed successfully.
                // We can immediately return (Short-circuiting OR logic).
                return;
            }
            // Explicitly catching Exception and AssertionError to prevent 
            // swallowing fatal JVM errors like OutOfMemoryError.
            catch (final Exception | AssertionError e)
            {
                failures.add(e);
            }
        }

        // If we exit the loop, ALL assertion blocks failed. We aggregate the 
        // exceptions so the test report shows exactly why each option failed.
        final AssertionError totalFailure = new AssertionError(
            String.format("Total failure: All %d assertion options failed. See suppressed exceptions for details.", options.length)
        );
        
        failures.forEach(totalFailure::addSuppressed);
        
        throw totalFailure;
    }
}
