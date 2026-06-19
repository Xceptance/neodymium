/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.ai.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xceptance.neodymium.ai.action.Action;

/**
 * Fluent assertion helper for validating {@link Action} instances in tests.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiActionAssert
{
    private final Action action;

    /**
     * Constructs a new AiActionAssert instance.
     *
     * @param action the action to validate
     */
    public AiActionAssert(final Action action)
    {
        this.action = action;
    }

    /**
     * Entry point for fluent assertions on {@link Action}.
     *
     * @param action the action to validate
     * @return the assertion helper
     */
    public static AiActionAssert assertThat(final Action action)
    {
        return new AiActionAssert(action);
    }

    /**
     * Asserts the type of the action.
     *
     * @param expectedType the expected type
     * @return this assertion helper
     */
    public final AiActionAssert hasType(final String expectedType)
    {
        assertEquals(expectedType, action.getType(), "Action type mismatch");
        return this;
    }

    /**
     * Asserts the target of the action.
     *
     * @param expectedTarget the expected target
     * @return this assertion helper
     */
    public final AiActionAssert hasTarget(final String expectedTarget)
    {
        assertEquals(expectedTarget, action.getTarget(), "Action target mismatch");
        return this;
    }

    /**
     * Asserts that the target of the action contains the specified substring.
     *
     * @param targetContains the substring to check
     * @return this assertion helper
     */
    public final AiActionAssert hasTargetContaining(final String targetContains)
    {
        final String target = action.getTarget();
        assertTrue(target != null && target.contains(targetContains),
                "Action target ('" + target + "') does not contain: '" + targetContains + "'");
        return this;
    }

    /**
     * Asserts the value of the action.
     *
     * @param expectedValue the expected value
     * @return this assertion helper
     */
    public final AiActionAssert hasValue(final String expectedValue)
    {
        assertEquals(expectedValue, action.getValue(), "Action value mismatch");
        return this;
    }

    /**
     * Asserts the description of the action.
     *
     * @param expectedDescription the expected description
     * @return this assertion helper
     */
    public final AiActionAssert hasDescription(final String expectedDescription)
    {
        assertEquals(expectedDescription, action.getDescription(), "Action description mismatch");
        return this;
    }
}
