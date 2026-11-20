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
package org.neodymium.ai.pipeline;

/**
 * Thrown when a test step marked with an expected bug succeeds unexpectedly.
 * Extends {@link AssertionError} so test runners (e.g. JUnit, Surefire, Allure) report
 * this unexpected pass as an assertion failure.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExpectedBugNotReproducedException extends AssertionError
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an ExpectedBugNotReproducedException with a detail message.
     *
     * @param message the detail message describing the step and expected bug
     */
    public ExpectedBugNotReproducedException(final String message)
    {
        super(message);
    }

    /**
     * Constructs an ExpectedBugNotReproducedException with a detail message and cause.
     *
     * @param message the detail message describing the step and expected bug
     * @param cause the underlying cause
     */
    public ExpectedBugNotReproducedException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
