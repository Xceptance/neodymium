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
package org.neodymium.ai.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;

/**
 * Unit tests verifying execution mode query helpers and telemetry metrics calculations in {@link ExecutionMetrics}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class ExecutionMetricsTest
{
    @Test
    @DisplayName("ExecutionMetrics mode helpers return correct boolean flags for REPLAY_STRICT")
    public void testStrictReplayModeHelpers()
    {
        final ExecutionMetrics metrics = new ExecutionMetrics(
            ExecutionMode.REPLAY_STRICT, 0, 0, 0, 0, 0, 5, 0, 0, 5, 0, new TokenUsage(0, 0, 0)
        );

        Assertions.assertEquals(ExecutionMode.REPLAY_STRICT, metrics.getExecutionMode());
        Assertions.assertTrue(metrics.isStrictReplay());
        Assertions.assertTrue(metrics.isReplay());
        Assertions.assertFalse(metrics.isLive());
        Assertions.assertFalse(metrics.isRecording());
        Assertions.assertFalse(metrics.supportsHealing());
        Assertions.assertFalse(metrics.wasHealed());
        Assertions.assertEquals(0, metrics.getLlmCallCount());
        Assertions.assertEquals(5, metrics.getStepCount());
        Assertions.assertEquals(5, metrics.getReplayedStepCount());
    }

    @Test
    @DisplayName("ExecutionMetrics mode helpers return correct boolean flags for REPLAY_WITH_HEALING")
    public void testReplayWithHealingModeHelpers()
    {
        final ExecutionMetrics metrics = new ExecutionMetrics(
            ExecutionMode.REPLAY_WITH_HEALING, 2, 1, 1, 0, 0, 4, 1, 0, 3, 0, new TokenUsage(100, 20, 0)
        );

        Assertions.assertEquals(ExecutionMode.REPLAY_WITH_HEALING, metrics.getExecutionMode());
        Assertions.assertTrue(metrics.supportsHealing());
        Assertions.assertTrue(metrics.isReplay());
        Assertions.assertTrue(metrics.wasHealed());
        Assertions.assertEquals(1, metrics.getHealedStepCount());
        Assertions.assertEquals(2, metrics.getLlmCallCount());
        Assertions.assertFalse(metrics.isStrictReplay());
    }

    @Test
    @DisplayName("ExecutionMetrics mode helpers return correct boolean flags for FORCE_RECORDING")
    public void testForceRecordingModeHelpers()
    {
        final ExecutionMetrics metrics = new ExecutionMetrics(
            ExecutionMode.FORCE_RECORDING, 3, 3, 0, 0, 0, 3, 0, 0, 0, 1, new TokenUsage(500, 100, 50)
        );

        Assertions.assertEquals(ExecutionMode.FORCE_RECORDING, metrics.getExecutionMode());
        Assertions.assertTrue(metrics.isLive());
        Assertions.assertTrue(metrics.isRecording());
        Assertions.assertFalse(metrics.isReplay());
        Assertions.assertFalse(metrics.isStrictReplay());
        Assertions.assertEquals(3, metrics.getLlmCallCount());
        Assertions.assertEquals(1, metrics.getInternalCacheHits());
        Assertions.assertEquals(0, metrics.getRcaCallCount());
    }

    @Test
    @DisplayName("ExecutionMetrics correctly retains and returns rcaCallCount")
    public void testRcaCallMetrics()
    {
        final ExecutionMetrics metrics = new ExecutionMetrics(
            ExecutionMode.LLM_ONLY, 4, 1, 1, 1, 0, 1, 3, 0, 0, 0, 0, 0, null, new TokenUsage(300, 100, 0)
        );

        Assertions.assertEquals(1, metrics.getRcaCallCount());
        Assertions.assertEquals(4, metrics.getLlmCallCount());
        Assertions.assertEquals(1, metrics.getStandardCallCount());
        Assertions.assertEquals(1, metrics.getVerificationCallCount());
        Assertions.assertEquals(1, metrics.getPesapCallCount());
        Assertions.assertEquals(0, metrics.getJudgeCallCount());
    }
}
