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
package org.neodymium.ai.pipeline.steps;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.session.AiSession;

/**
 * Unit tests for {@link CaptureStateStep}.
 * Validates SUT state capture and context transient storage.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class CaptureStateStepTest
{
    @Test
    public void testCaptureStateExecution() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider provider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);

        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        final CaptureStateStep captureStep = new CaptureStateStep();
        captureStep.execute(context);

        assertNotNull(context.getTransientData().get(ExecutionContext.KEY_LAST_STATE), "Last captured state should be populated in transient data.");
    }
}
