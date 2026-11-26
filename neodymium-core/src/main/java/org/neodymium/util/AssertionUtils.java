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
package org.neodymium.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for conditional assertion blocks.
 * 
 * @author AI-generated: Gemini 3.1 Pro
 * @author Xceptance GmbH 2026
 */
public class AssertionUtils
{
    /**
     * Prevents instantiation.
     */
    protected AssertionUtils()
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
