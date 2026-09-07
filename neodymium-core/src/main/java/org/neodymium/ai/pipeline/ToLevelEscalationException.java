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
 * Concrete escalation exception targeting a specific depth level or identifier
 * in the execution context pipeline hierarchy.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 * @deprecated Replaced by iterative {@link org.neodymium.ai.pipeline.steps.AgentToolLoopStep} active discovery tools.
 */
@Deprecated(since = "5.6.0", forRemoval = true)
public final class ToLevelEscalationException extends EscalationException
{
    private static final long serialVersionUID = 1L;

    /**
     * The target depth level or identifier to escalate to.
     */
    private final String targetLevel;

    /**
     * Constructs a ToLevelEscalationException.
     *
     * @param message the escalation details message
     * @param targetLevel the target depth level or identifier
     */
    public ToLevelEscalationException(final String message, final String targetLevel)
    {
        super(message);
        this.targetLevel = targetLevel;
    }

    /**
     * Gets the target level.
     *
     * @return the target level string
     */
    public String getTargetLevel()
    {
        return this.targetLevel;
    }
}
