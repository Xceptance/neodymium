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

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;

/**
 * Unit tests verifying Option 3 conditional mode lambdas and fluent assertions on {@link MetricsAsserter}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class MetricsAsserterTest
{
    @Test
    @DisplayName("Option 3 conditional lambdas execute only for matching mode during REPLAY_STRICT run")
    public void testOption3ConditionalLambdasStrictReplay()
    {
        final ExecutionMetrics metrics = new ExecutionMetrics(
            ExecutionMode.REPLAY_STRICT, 0, 0, 0, 0, 0, 3, 0, 0, 3, 0, new TokenUsage(0, 0, 0)
        );

        final MetricsAsserter asserter = new MetricsAsserter(metrics);
        final AtomicBoolean strictRan = new AtomicBoolean(false);
        final AtomicBoolean replayRan = new AtomicBoolean(false);
        final AtomicBoolean liveRan = new AtomicBoolean(false);

        asserter
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onStrictReplay(m -> {
                strictRan.set(true);
                m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed();
            })
            .onReplay(m -> {
                replayRan.set(true);
                m.hasReplayedStepCount(3);
            })
            .onLive(m -> liveRan.set(true));

        Assertions.assertTrue(strictRan.get(), "onStrictReplay lambda should have executed");
        Assertions.assertTrue(replayRan.get(), "onReplay lambda should have executed");
        Assertions.assertFalse(liveRan.get(), "onLive lambda should NOT have executed");
    }

    @Test
    @DisplayName("Option 3 conditional lambdas execute only for matching mode during FORCE_RECORDING run")
    public void testOption3ConditionalLambdasLiveRecording()
    {
        final ExecutionMetrics metrics = new ExecutionMetrics(
            ExecutionMode.FORCE_RECORDING, 2, 2, 0, 0, 0, 2, 0, 0, 0, 0, new TokenUsage(300, 50, 0)
        );

        final MetricsAsserter asserter = new MetricsAsserter(metrics);
        final AtomicBoolean liveRan = new AtomicBoolean(false);
        final AtomicBoolean strictRan = new AtomicBoolean(false);

        asserter
            .hasStepCount(2)
            .onLive(m -> {
                liveRan.set(true);
                m.hasLlmCalls();
            })
            .onStrictReplay(m -> strictRan.set(true))
            .onMode(ExecutionMode.FORCE_RECORDING, m -> m.hasLlmCalls(2));

        Assertions.assertTrue(liveRan.get(), "onLive lambda should have executed");
        Assertions.assertFalse(strictRan.get(), "onStrictReplay lambda should NOT have executed");
    }

    @Test
    @DisplayName("matchesModeExpectations passes cleanly for strict replay metrics")
    public void testMatchesModeExpectations()
    {
        final ExecutionMetrics metrics = new ExecutionMetrics(
            ExecutionMode.REPLAY_STRICT, 0, 0, 0, 0, 0, 4, 0, 0, 4, 0, new TokenUsage(0, 0, 0)
        );

        new MetricsAsserter(metrics).matchesModeExpectations();
    }

    @Test
    @DisplayName("MetricsAsserter boolean query helpers and overloaded range/breakdown assertions work cleanly")
    public void testBooleanHelpersAndOverloadedRangeAssertions()
    {
        final ExecutionMetrics metrics = new ExecutionMetrics(
            ExecutionMode.FORCE_RECORDING, 15, 12, 0, 3, 0, 12, 0, 0, 0, 2, new TokenUsage(1000, 200, 100)
        );

        final MetricsAsserter asserter = new MetricsAsserter(metrics);

        Assertions.assertTrue(asserter.isLive());
        Assertions.assertTrue(asserter.isRecording());
        Assertions.assertFalse(asserter.isReplay());
        Assertions.assertFalse(asserter.isStrictReplay());
        Assertions.assertFalse(asserter.isHealed());
        Assertions.assertFalse(asserter.supportsHealing());

        asserter
            .hasStepCount(10, 15)
            .hasStandardCalls(12)
            .hasStandardCalls(10, 14)
            .hasPesapCalls(3)
            .hasPesapCalls(1, 5)
            .hasLlmCalls(12, 20)
            .hasVerificationCalls(0)
            .hasJudgeCalls(0);
    }

    @Test
    @DisplayName("MetricsAsserter escalation and context level usage assertions work cleanly")
    public void testEscalationAndContextLevelAssertions()
    {
        final java.util.Map<String, Integer> levelCounts = new java.util.HashMap<>();
        levelCounts.put("MINIMAL", 12);
        levelCounts.put("LEAN", 2);

        final ExecutionMetrics metrics = new ExecutionMetrics(
            ExecutionMode.FORCE_RECORDING,
            14, 12, 0, 2, 0, 12, 0, 0, 0, 0,
            2, levelCounts, new TokenUsage(500, 100, 0)
        );

        final MetricsAsserter asserter = new MetricsAsserter(metrics);

        Assertions.assertEquals(2, metrics.getTotalEscalations());
        Assertions.assertEquals(12, metrics.getContextLevelCount(org.neodymium.ai.executor.selenide.ContextLevel.MINIMAL));
        Assertions.assertEquals(2, metrics.getContextLevelCount(org.neodymium.ai.executor.selenide.ContextLevel.LEAN));

        asserter
            .hasEscalations()
            .hasEscalationCount(2)
            .hasEscalationCount(1, 3)
            .hasContextLevelCount(org.neodymium.ai.executor.selenide.ContextLevel.MINIMAL, 12)
            .hasContextLevelCount(org.neodymium.ai.executor.selenide.ContextLevel.MINIMAL, 10, 15)
            .hasContextLevelCount(org.neodymium.ai.executor.selenide.ContextLevel.LEAN, 2)
            .hasContextLevelCount("MINIMAL", 12)
            .hasContextLevelCount("LEAN", 1, 3);
    }
}
