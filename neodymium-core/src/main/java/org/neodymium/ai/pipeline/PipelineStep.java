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
 * Functional interface representing a single, composable execution step or phase
 * within the runner execution pipeline.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@FunctionalInterface
public interface PipelineStep
{
    /**
     * Executes this phase within the given thread-isolated execution context.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if a flow redirection, retry loop, or execution error occurs
     */
    void execute(final ExecutionContext context) throws PipelineException;
}
