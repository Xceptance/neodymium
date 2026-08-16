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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

/**
 * Unit tests for {@link VisualBaselineGateStep}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class VisualBaselineGateStepTest
{
    @Test
    public void testNonReplayModeBypassesCheck()
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, null, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);

        final PlaybookStep step = new PlaybookStep("Verify logo is visible");
        step.setScreenshotHash("a1b2c3d4e5f6");

        final VisualBaselineGateStep gateStep = new VisualBaselineGateStep(step, session);
        assertDoesNotThrow(() -> gateStep.execute(context));
    }

    @Test
    public void testReplayWithoutRecordedHashBypassesCheck()
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, null, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

        final PlaybookStep step = new PlaybookStep("Click button");
        step.setScreenshotHash(null);

        final VisualBaselineGateStep gateStep = new VisualBaselineGateStep(step, session);
        assertDoesNotThrow(() -> gateStep.execute(context));
    }
}
