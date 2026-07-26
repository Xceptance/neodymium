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
import org.neodymium.ai.model.PlaybookStepStatus;

/**
 * Event indicating that a playbook step finished execution.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class StepFinishedEvent extends ExecutionEvent
{
    /**
     * The playbook step that finished.
     */
    private final PlaybookStep step;

    /**
     * The final outcome status of the step.
     */
    private final PlaybookStepStatus status;

    /**
     * Constructs a StepFinishedEvent.
     *
     * @param step the playbook step
     * @param status the final outcome status
     */
    public StepFinishedEvent(final PlaybookStep step, final PlaybookStepStatus status)
    {
        super();
        this.step = step;
        this.status = status;
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
     * Gets the step outcome status.
     *
     * @return the step status
     */
    public PlaybookStepStatus getStatus()
    {
        return this.status;
    }

    @Override
    public String getEventType()
    {
        return "step.finished";
    }

    @Override
    public org.neodymium.ai.event.EventCategory getCategory()
    {
        return org.neodymium.ai.event.EventCategory.STRUCTURAL;
    }
}
