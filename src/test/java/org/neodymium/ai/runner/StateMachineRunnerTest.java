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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.session.AiSession;

/**
 * Dedicated unit tests for {@link StateMachineRunner}.
 * Validates thread context restoration, state machine execution loop,
 * and exception safety in step processing.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class StateMachineRunnerTest
{
    private ExecutionContext outerContext;

    @BeforeEach
    public void setUp()
    {
        this.outerContext = new ExecutionContext(new SessionData());
        ExecutionContext.setActiveContext(this.outerContext);
    }

    @AfterEach
    public void tearDown()
    {
        ExecutionContext.setActiveContext(null);
    }

    @Test
    public void testStateMachineRunnerRestoresPreviousThreadLocalContextOnSuccess() throws PipelineException
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), new MockTargetExecutor());
        final StateMachineRunner runner = new StateMachineRunner(session);

        final ExecutionContext innerContext = session.getExecutionContext();
        final List<String> executedOrder = new ArrayList<>();

        innerContext.pushStep(ctx -> executedOrder.add("step1"));

        runner.run();

        assertEquals(1, executedOrder.size());
        assertEquals("step1", executedOrder.get(0));
        assertSame(this.outerContext, ExecutionContext.getActiveContext(), "StateMachineRunner must restore previous ThreadLocal ExecutionContext on success");
    }

    @Test
    public void testStateMachineRunnerRestoresPreviousThreadLocalContextOnFailure()
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), new MockTargetExecutor());
        final StateMachineRunner runner = new StateMachineRunner(session);

        final ExecutionContext innerContext = session.getExecutionContext();
        innerContext.pushStep(ctx -> {
            throw new ConclusiveFailureException("Step execution failed intentionally");
        });

        assertThrows(ConclusiveFailureException.class, () -> runner.run());

        assertSame(this.outerContext, ExecutionContext.getActiveContext(), "StateMachineRunner must restore previous ThreadLocal ExecutionContext on failure");
    }
}
