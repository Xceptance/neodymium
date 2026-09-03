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

/**
 * Checked exception indicating that Pre-Execution Step Analysis (PESAP)
 * failed to classify or resolve the semantic intent of an instruction.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class PesapClassificationException extends PipelineException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a PesapClassificationException.
     *
     * @param message the failure details message
     */
    public PesapClassificationException(final String message)
    {
        super(message);
    }

    /**
     * Constructs a PesapClassificationException with a cause.
     *
     * @param message the failure details message
     * @param cause the underlying cause exception
     */
    public PesapClassificationException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
