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

import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.model.ContextLevel;

/**
 * Fluent assertion helper providing chainable validations and conditional mode lambdas
 * for evaluating telemetry metrics and step execution invariants.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class MetricsAsserter
{
    private final ExecutionMetrics metrics;

    /**
     * Constructs a MetricsAsserter wrapping an ExecutionMetrics snapshot.
     *
     * @param metrics the execution metrics to validate
     */
    public MetricsAsserter(final ExecutionMetrics metrics)
    {
        if (metrics == null)
        {
            throw new IllegalArgumentException("ExecutionMetrics must not be null.");
        }
        this.metrics = metrics;
    }

    /**
     * Retrieves the underlying ExecutionMetrics snapshot.
     *
     * @return the metrics snapshot
     */
    public ExecutionMetrics getMetrics()
    {
        return this.metrics;
    }

    /**
     * Checks if any step was resolved via self-healing during execution.
     *
     * @return true if healed steps > 0, false otherwise
     */
    public boolean isHealed()
    {
        return this.metrics.wasHealed();
    }

    /**
     * Checks if this run performed live LLM action generation.
     *
     * @return true if live execution mode, false otherwise
     */
    public boolean isLive()
    {
        return this.metrics.isLive();
    }

    /**
     * Checks if this run executed pre-recorded actions from replay cache.
     *
     * @return true if replay mode, false otherwise
     */
    public boolean isReplay()
    {
        return this.metrics.isReplay();
    }

    /**
     * Checks if this run executed under strict replay mode without LLM fallbacks.
     *
     * @return true if strict replay mode, false otherwise
     */
    public boolean isStrictReplay()
    {
        return this.metrics.isStrictReplay();
    }

    /**
     * Checks if this run automatically recorded executed actions.
     *
     * @return true if recording mode, false otherwise
     */
    public boolean isRecording()
    {
        return this.metrics.isRecording();
    }

    /**
     * Checks if this run supports self-healing on step replay failure.
     *
     * @return true if healing supported, false otherwise
     */
    public boolean supportsHealing()
    {
        return this.metrics.supportsHealing();
    }

    /**
     * Asserts that total executed step count equals the expected value.
     *
     * @param expected expected step count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasStepCount(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getStepCount(), "Unexpected executed step count.");
        return this;
    }

    /**
     * Asserts that total executed step count falls within the expected inclusive range [min, max].
     *
     * @param min minimum expected step count
     * @param max maximum expected step count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasStepCount(final int min, final int max)
    {
        final int actual = this.metrics.getStepCount();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected step count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that total LLM call count equals the expected value.
     *
     * @param expected expected LLM call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasLlmCalls(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getLlmCallCount(), "Unexpected LLM call count.");
        return this;
    }

    /**
     * Asserts that total LLM call count falls within the expected inclusive range [min, max].
     *
     * @param min minimum expected LLM call count
     * @param max maximum expected LLM call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasLlmCalls(final int min, final int max)
    {
        final int actual = this.metrics.getLlmCallCount();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected LLM call count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that standard action extraction LLM calls equal the expected value.
     *
     * @param expected expected standard call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasStandardCalls(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getStandardCallCount(), "Unexpected standard LLM call count.");
        return this;
    }

    /**
     * Asserts that standard action extraction LLM calls fall within the expected inclusive range [min, max].
     *
     * @param min minimum expected standard call count
     * @param max maximum expected standard call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasStandardCalls(final int min, final int max)
    {
        final int actual = this.metrics.getStandardCallCount();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected standard LLM call count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that PESAP pre-step analysis LLM calls equal the expected value.
     *
     * @param expected expected PESAP call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasPesapCalls(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getPesapCallCount(), "Unexpected PESAP LLM call count.");
        return this;
    }

    /**
     * Asserts that PESAP pre-step analysis LLM calls fall within the expected inclusive range [min, max].
     *
     * @param min minimum expected PESAP call count
     * @param max maximum expected PESAP call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasPesapCalls(final int min, final int max)
    {
        final int actual = this.metrics.getPesapCallCount();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected PESAP LLM call count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that post-action verification LLM calls equal the expected value.
     *
     * @param expected expected verification call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasVerificationCalls(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getVerificationCallCount(), "Unexpected verification LLM call count.");
        return this;
    }

    /**
     * Asserts that post-action verification LLM calls fall within the expected inclusive range [min, max].
     *
     * @param min minimum expected verification call count
     * @param max maximum expected verification call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasVerificationCalls(final int min, final int max)
    {
        final int actual = this.metrics.getVerificationCallCount();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected verification LLM call count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that quality judge LLM calls equal the expected value.
     *
     * @param expected expected judge call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasJudgeCalls(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getJudgeCallCount(), "Unexpected judge LLM call count.");
        return this;
    }

    /**
     * Asserts that quality judge LLM calls fall within the expected inclusive range [min, max].
     *
     * @param min minimum expected judge call count
     * @param max maximum expected judge call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasJudgeCalls(final int min, final int max)
    {
        final int actual = this.metrics.getJudgeCallCount();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected judge LLM call count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that Visual RCA LLM calls equal the expected value.
     *
     * @param expected expected RCA call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasRcaCalls(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getRcaCallCount(), "Unexpected Visual RCA LLM call count.");
        return this;
    }

    /**
     * Asserts that Visual RCA LLM calls fall within the expected inclusive range [min, max].
     *
     * @param min minimum expected RCA call count
     * @param max maximum expected RCA call count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasRcaCalls(final int min, final int max)
    {
        final int actual = this.metrics.getRcaCallCount();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected Visual RCA LLM call count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that 0 LLM calls were made during execution.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasNoLlmCalls()
    {
        Assertions.assertEquals(0, this.metrics.getLlmCallCount(), "Expected 0 LLM calls, but calls were made.");
        return this;
    }

    /**
     * Asserts that at least 1 LLM call was made during execution.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasLlmCalls()
    {
        Assertions.assertTrue(this.metrics.getLlmCallCount() > 0, "Expected at least 1 LLM call, but 0 calls were made.");
        return this;
    }

    /**
     * Asserts that at least 1 step was resolved via self-healing.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter wasHealed()
    {
        Assertions.assertTrue(this.metrics.wasHealed(), "Expected execution to contain self-healed steps, but none were healed.");
        return this;
    }

    /**
     * Asserts that 0 steps were resolved via self-healing.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter wasNotHealed()
    {
        Assertions.assertFalse(this.metrics.wasHealed(), "Expected no self-healing, but healed steps were present.");
        return this;
    }

    /**
     * Asserts that healed step count equals the expected value.
     *
     * @param expected expected healed step count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasHealedStepCount(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getHealedStepCount(), "Unexpected healed step count.");
        return this;
    }

    /**
     * Asserts that healed step count falls within the expected inclusive range [min, max].
     *
     * @param min minimum expected healed step count
     * @param max maximum expected healed step count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasHealedStepCount(final int min, final int max)
    {
        final int actual = this.metrics.getHealedStepCount();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected healed step count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that 0 steps were resolved via self-healing.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasNoHealedSteps()
    {
        return hasHealedStepCount(0);
    }

    /**
     * Asserts that soft/optional failed step count equals the expected value.
     *
     * @param expected expected soft failed step count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasSoftFailedStepCount(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getSoftFailedStepCount(), "Unexpected soft failed step count.");
        return this;
    }

    /**
     * Asserts that soft/optional failed step count falls within the expected inclusive range [min, max].
     *
     * @param min minimum expected soft failed step count
     * @param max maximum expected soft failed step count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasSoftFailedStepCount(final int min, final int max)
    {
        final int actual = this.metrics.getSoftFailedStepCount();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected soft failed step count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that 0 soft/optional failed steps occurred during execution.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasNoSoftFailures()
    {
        return hasSoftFailedStepCount(0);
    }

    /**
     * Asserts that replayed step count equals the expected value.
     *
     * @param expected expected replayed step count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasReplayedStepCount(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getReplayedStepCount(), "Unexpected replayed step count.");
        return this;
    }

    /**
     * Asserts that replayed step count falls within the expected inclusive range [min, max].
     *
     * @param min minimum expected replayed step count
     * @param max maximum expected replayed step count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasReplayedStepCount(final int min, final int max)
    {
        final int actual = this.metrics.getReplayedStepCount();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected replayed step count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that 0 steps were executed from replay cache.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasNoReplayedSteps()
    {
        return hasReplayedStepCount(0);
    }

    /**
     * Asserts that all executed steps were executed from offline replay cache.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasAllStepsReplayed()
    {
        Assertions.assertEquals(this.metrics.getStepCount(), this.metrics.getReplayedStepCount(), "Not all steps were replayed from cache.");
        return this;
    }

    /**
     * Asserts that 0 context level escalations occurred during execution.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasNoEscalations()
    {
        return hasEscalationCount(0);
    }

    /**
     * Asserts that at least 1 context level escalation occurred during execution.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasEscalations()
    {
        Assertions.assertTrue(this.metrics.getTotalEscalations() > 0, "Expected at least 1 context level escalation, but 0 occurred.");
        return this;
    }

    /**
     * Asserts that total context level escalation count equals the expected value.
     *
     * @param expected expected escalation count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasEscalationCount(final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getTotalEscalations(), "Unexpected context level escalation count.");
        return this;
    }

    /**
     * Asserts that total context level escalation count falls within the expected inclusive range [min, max].
     *
     * @param min minimum expected escalation count
     * @param max maximum expected escalation count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasEscalationCount(final int min, final int max)
    {
        final int actual = this.metrics.getTotalEscalations();
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected escalation count between %d and %d, but was %d.", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that the usage count for the given ContextLevel enum equals the expected value.
     *
     * @param level expected ContextLevel enum
     * @param expected expected usage count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasContextLevelCount(final ContextLevel level, final int expected)
    {
        final String name = level != null ? level.name() : null;
        Assertions.assertEquals(expected, this.metrics.getContextLevelCount(name), "Unexpected context level usage count for " + name + ".");
        return this;
    }

    /**
     * Asserts that the usage count for the given ContextLevel enum falls within the expected inclusive range [min, max].
     *
     * @param level expected ContextLevel enum
     * @param min minimum expected usage count
     * @param max maximum expected usage count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasContextLevelCount(final ContextLevel level, final int min, final int max)
    {
        final String name = level != null ? level.name() : null;
        return hasContextLevelCount(name, min, max);
    }

    /**
     * Asserts that the usage count for the given context level string name equals the expected value.
     *
     * @param levelName expected context level string name
     * @param expected expected usage count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasContextLevelCount(final String levelName, final int expected)
    {
        Assertions.assertEquals(expected, this.metrics.getContextLevelCount(levelName), "Unexpected context level usage count for " + levelName + ".");
        return this;
    }

    /**
     * Asserts that the usage count for the given context level string name falls within the expected inclusive range [min, max].
     *
     * @param levelName expected context level string name
     * @param min minimum expected usage count
     * @param max maximum expected usage count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasContextLevelCount(final String levelName, final int min, final int max)
    {
        final int actual = this.metrics.getContextLevelCount(levelName);
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected context level usage count for %s between %d and %d, but was %d.", levelName, min, max, actual)
        );
        return this;
    }

    /**
     * Asserts standard mode expectations based on the governing ExecutionMode.
     *
     * @return this asserter instance for chaining
     */
    public MetricsAsserter matchesModeExpectations()
    {
        hasNoSoftFailures();

        if (this.metrics.isStrictReplay())
        {
            hasNoLlmCalls();
            hasNoHealedSteps();
            hasAllStepsReplayed();
        }
        else if (this.metrics.isLive())
        {
            hasLlmCalls();
        }
        else if (this.metrics.supportsHealing())
        {
            if (this.metrics.wasHealed())
            {
                hasLlmCalls();
                Assertions.assertTrue(this.metrics.getHealedStepCount() > 0, "Expected healed step count > 0.");
            }
            else
            {
                hasNoLlmCalls();
                hasNoHealedSteps();
                hasAllStepsReplayed();
            }
        }
        return this;
    }

    /**
     * Executes the consumer lambda if the governing execution mode is live.
     *
     * @param consumer assertion consumer
     * @return this asserter instance for chaining
     */
    public MetricsAsserter onLive(final Consumer<MetricsAsserter> consumer)
    {
        if (this.metrics.isLive() && consumer != null)
        {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Executes the consumer lambda if the governing execution mode is replay.
     *
     * @param consumer assertion consumer
     * @return this asserter instance for chaining
     */
    public MetricsAsserter onReplay(final Consumer<MetricsAsserter> consumer)
    {
        if (this.metrics.isReplay() && consumer != null)
        {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Executes the consumer lambda if the governing execution mode is REPLAY_STRICT.
     *
     * @param consumer assertion consumer
     * @return this asserter instance for chaining
     */
    public MetricsAsserter onStrictReplay(final Consumer<MetricsAsserter> consumer)
    {
        if (this.metrics.isStrictReplay() && consumer != null)
        {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Executes the consumer lambda if the governing execution mode supports healing.
     *
     * @param consumer assertion consumer
     * @return this asserter instance for chaining
     */
    public MetricsAsserter onHealing(final Consumer<MetricsAsserter> consumer)
    {
        if (this.metrics.supportsHealing() && consumer != null)
        {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Executes the consumer lambda if the governing execution mode matches the specified mode.
     *
     * @param targetMode target execution mode
     * @param consumer assertion consumer
     * @return this asserter instance for chaining
     */
    public MetricsAsserter onMode(final ExecutionMode targetMode, final Consumer<MetricsAsserter> consumer)
    {
        if (this.metrics.getExecutionMode() == targetMode && consumer != null)
        {
            consumer.accept(this);
        }
        return this;
    }

    /**
     * Asserts that total input (prompt) tokens match the exact expected count.
     *
     * @param expected expected input token count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasInputTokens(final int expected)
    {
        final int actual = this.metrics.getTotalTokenUsage() != null ? this.metrics.getTotalTokenUsage().inputTokenCount() : 0;
        Assertions.assertEquals(expected, actual, "Expected input tokens: " + expected + ", but was: " + actual);
        return this;
    }

    /**
     * Asserts that total input (prompt) tokens fall within the inclusive [min, max] range.
     *
     * @param min minimum allowed input tokens
     * @param max maximum allowed input tokens
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasInputTokens(final int min, final int max)
    {
        final int actual = this.metrics.getTotalTokenUsage() != null ? this.metrics.getTotalTokenUsage().inputTokenCount() : 0;
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected input tokens between [%d, %d], but was: %d", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that total output (completion) tokens match the exact expected count.
     *
     * @param expected expected output token count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasOutputTokens(final int expected)
    {
        final int actual = this.metrics.getTotalTokenUsage() != null ? this.metrics.getTotalTokenUsage().outputTokenCount() : 0;
        Assertions.assertEquals(expected, actual, "Expected output tokens: " + expected + ", but was: " + actual);
        return this;
    }

    /**
     * Asserts that total output (completion) tokens fall within the inclusive [min, max] range.
     *
     * @param min minimum allowed output tokens
     * @param max maximum allowed output tokens
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasOutputTokens(final int min, final int max)
    {
        final int actual = this.metrics.getTotalTokenUsage() != null ? this.metrics.getTotalTokenUsage().outputTokenCount() : 0;
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected output tokens between [%d, %d], but was: %d", min, max, actual)
        );
        return this;
    }

    /**
     * Asserts that total tokens (input + output) match the exact expected count.
     *
     * @param expected expected total token count
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasTotalTokens(final int expected)
    {
        final int actual = this.metrics.getTotalTokenUsage() != null ? this.metrics.getTotalTokenUsage().totalTokenCount() : 0;
        Assertions.assertEquals(expected, actual, "Expected total tokens: " + expected + ", but was: " + actual);
        return this;
    }

    /**
     * Asserts that total tokens (input + output) fall within the inclusive [min, max] range.
     *
     * @param min minimum allowed total tokens
     * @param max maximum allowed total tokens
     * @return this asserter instance for chaining
     */
    public MetricsAsserter hasTotalTokens(final int min, final int max)
    {
        final int actual = this.metrics.getTotalTokenUsage() != null ? this.metrics.getTotalTokenUsage().totalTokenCount() : 0;
        Assertions.assertTrue(
            actual >= min && actual <= max,
            String.format("Expected total tokens between [%d, %d], but was: %d", min, max, actual)
        );
        return this;
    }
}
