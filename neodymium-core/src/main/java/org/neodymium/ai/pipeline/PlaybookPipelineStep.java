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
package org.neodymium.ai.pipeline;

import org.neodymium.ai.model.PlaybookStep;

/**
 * Extension of {@link PipelineStep} that is explicitly bound to a {@link PlaybookStep}.
 * Allows the pipeline execution context to distinguish top-level playbook execution steps
 * from intra-step boundary hooks or error recovery wrappers during live step manipulation.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public interface PlaybookPipelineStep extends PipelineStep
{
    /**
     * Returns the underlying playbook step associated with this pipeline step.
     *
     * @return the associated playbook step
     */
    PlaybookStep getPlaybookStep();
}
