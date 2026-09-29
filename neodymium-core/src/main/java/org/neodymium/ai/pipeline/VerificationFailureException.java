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
package org.neodymium.ai.pipeline;

import org.neodymium.ai.prompt.VerificationResult;

/**
 * Assertion error indicating that independent post-execution semantic outcome verification
 * has evaluated the step outcome and concluded that the goal was not achieved.
 * Extends {@link AssertionError} so test runners report verification failures as test assertion failures.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerificationFailureException extends AssertionError
{
    private static final long serialVersionUID = 1L;

    private final VerificationResult verificationResult;

    /**
     * Constructs a VerificationFailureException with a detail message.
     *
     * @param message the failure detail message
     */
    public VerificationFailureException(final String message)
    {
        this(message, null, null);
    }

    /**
     * Constructs a VerificationFailureException with a detail message and verification result.
     *
     * @param message the failure detail message
     * @param verificationResult the outcome verification result details
     */
    public VerificationFailureException(final String message, final VerificationResult verificationResult)
    {
        this(message, verificationResult, null);
    }

    /**
     * Constructs a VerificationFailureException with a verification result and detail message.
     *
     * @param verificationResult the outcome verification result details
     * @param message the failure detail message
     */
    public VerificationFailureException(final VerificationResult verificationResult, final String message)
    {
        this(message, verificationResult, null);
    }

    /**
     * Constructs a VerificationFailureException with a detail message, verification result, and underlying cause.
     *
     * @param message the failure detail message
     * @param verificationResult the outcome verification result details
     * @param cause the underlying exception cause
     */
    public VerificationFailureException(final String message, final VerificationResult verificationResult, final Throwable cause)
    {
        super(message, cause);
        this.verificationResult = verificationResult;
    }

    /**
     * Returns the verification result details associated with this failure.
     *
     * @return the verification result, or {@code null}
     */
    public VerificationResult getVerificationResult()
    {
        return this.verificationResult;
    }
}
