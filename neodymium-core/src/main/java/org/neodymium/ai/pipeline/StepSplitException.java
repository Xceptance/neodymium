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

import java.util.Collections;
import java.util.List;
import org.neodymium.ai.model.PlaybookStep;

/**
 * Checked exception carrying dynamically split child sub-steps when the LLM
 * or executor decides a macro-step needs to be broken down.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class StepSplitException extends PipelineException
{
    private static final long serialVersionUID = 1L;

    /**
     * The list of dynamically split child sub-steps.
     */
    private final List<PlaybookStep> subSteps;

    /**
     * Constructs a StepSplitException.
     *
     * @param message the split details message
     * @param subSteps the list of child sub-steps
     */
    public StepSplitException(final String message, final List<PlaybookStep> subSteps)
    {
        super(message);
        this.subSteps = subSteps == null ? Collections.emptyList() : List.copyOf(subSteps);
    }

    /**
     * Retrieves the sub-steps to be executed.
     *
     * @return the unmodifiable list of child playbook steps
     */
    public List<PlaybookStep> getSubSteps()
    {
        return this.subSteps;
    }
}
