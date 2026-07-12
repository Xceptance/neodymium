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
package org.neodymium.ai.event.structural;

import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.model.PlaybookStep;

/**
 * Event indicating that a playbook step has started execution.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class StepStartedEvent extends ExecutionEvent
{
    /**
     * The playbook step that started.
     */
    private final PlaybookStep step;

    /**
     * The index of the playbook step in the queue.
     */
    private final int stepIndex;

    /**
     * Constructs a StepStartedEvent.
     *
     * @param step the playbook step
     * @param stepIndex the step index
     */
    public StepStartedEvent(final PlaybookStep step, final int stepIndex)
    {
        super();
        this.step = step;
        this.stepIndex = stepIndex;
    }

    /**
     * Gets the playbook step.
     *
     * @return the playbook step
     */
    public PlaybookStep getStep()
    {
        return this.step;
    }

    /**
     * Gets the step index.
     *
     * @return the step index
     */
    public int getStepIndex()
    {
        return this.stepIndex;
    }
}
