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
package org.neodymium.ai.model;

/**
 * Thrown when a recorded playbook companion JSON file targets an automation framework
 * (e.g., PLAYWRIGHT) incompatible with the current runner engine (e.g., SELENIUM_SELENIDE).
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class IncompatibleFrameworkException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an IncompatibleFrameworkException with the given detail message.
     *
     * @param message the detail message describing the framework mismatch
     */
    public IncompatibleFrameworkException(final String message)
    {
        super(message);
    }
}
