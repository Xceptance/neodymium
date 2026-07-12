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
package org.neodymium.ai.runner;

import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.structural.TryCatchStep;
import org.neodymium.ai.session.AiSession;

/**
 * State machine loop that pops and executes scheduled steps from the LIFO stack
 * in the active session context. Coordinates execution hooks and Exception handling.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class StateMachineRunner
{
    /**
     * The active playbook execution session.
     */
    private final AiSession session;

    /**
     * Constructs a StateMachineRunner.
     *
     * @param session the execution session
     */
    public StateMachineRunner(final AiSession session)
    {
        this.session = session;
    }

    /**
     * Wires the pre hooks, executes the scheduled steps on the context stack,
     * handles Try/Catch exceptions routing, and triggers post hooks.
     *
     * @throws PipelineException if a step fails and is not caught by any try-catch block
     */
    public void run() throws PipelineException
    {
        this.session.runPreHooks();
        final ExecutionContext context = this.session.getExecutionContext();
        context.getTransientData().put("session", this.session);
        context.getTransientData().put("targetExecutor", this.session.getTargetExecutor());
        boolean success = false;

        try
        {
            while (context.hasSteps())
            {
                final PipelineStep step = context.popStep();
                try
                {
                    step.execute(context);
                }
                catch (PipelineException e)
                {
                    // Check if an active TryCatch scope can handle this exception
                    final PipelineStep activeScope = context.peekTryCatch();
                    if (activeScope instanceof TryCatchStep tryCatch)
                    {
                        final PipelineStep handler = tryCatch.getHandlerFor(e);
                        if (handler != null)
                        {
                            // Pop TryCatch from the exception scope stack
                            context.popTryCatch();
                            // Discard try block pending steps up to boundary marker
                            context.discardStepsUpToTryCatch(tryCatch);
                            // Schedule exception handler step
                            context.pushStep(handler);
                            continue;
                        }
                    }
                    // Bubbling up out of loop
                    throw e;
                }
            }
            success = true;
        }
        finally
        {
            this.session.runPostHooks(success);
        }
    }
}
