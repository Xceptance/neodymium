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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;

/**
 * Immutable container storing recorded playbook steps and execution metadata
 * produced during an AI test run.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookRecording
{
    /**
     * The unmodifiable list of executed playbook steps with recorded actions.
     */
    private final List<PlaybookStep> recordedSteps;

    /**
     * Optional metadata dictionary associated with this execution run.
     */
    private final Map<String, Object> metadata;

    /**
     * The execution mode governing this recording run.
     */
    private final ExecutionMode executionMode;

    /**
     * Constructs a PlaybookRecording.
     *
     * @param recordedSteps the list of recorded playbook steps
     */
    public PlaybookRecording(final List<PlaybookStep> recordedSteps)
    {
        this(recordedSteps, Collections.emptyMap(), ExecutionMode.LLM_ONLY);
    }

    /**
     * Constructs a PlaybookRecording with explicit steps and metadata.
     *
     * @param recordedSteps the list of recorded playbook steps
     * @param metadata the execution metadata map
     */
    public PlaybookRecording(final List<PlaybookStep> recordedSteps, final Map<String, Object> metadata)
    {
        this(recordedSteps, metadata, ExecutionMode.LLM_ONLY);
    }

    /**
     * Constructs a PlaybookRecording with explicit steps, metadata, and execution mode.
     *
     * @param recordedSteps the list of recorded playbook steps
     * @param metadata the execution metadata map
     * @param executionMode the execution mode
     */
    @JsonCreator
    public PlaybookRecording(
        @JsonProperty("recordedSteps") final List<PlaybookStep> recordedSteps,
        @JsonProperty("metadata") final Map<String, Object> metadata,
        @JsonProperty("executionMode") final ExecutionMode executionMode
    )
    {
        this.recordedSteps = recordedSteps != null ? new ArrayList<>(recordedSteps) : new ArrayList<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.executionMode = executionMode != null ? executionMode : ExecutionMode.LLM_ONLY;
    }

    /**
     * Retrieves the unmodifiable list of recorded steps.
     *
     * @return the list of playbook steps
     */
    public List<PlaybookStep> getRecordedSteps()
    {
        return Collections.unmodifiableList(this.recordedSteps);
    }

    /**
     * Retrieves the unmodifiable metadata dictionary.
     *
     * @return the execution metadata map
     */
    public Map<String, Object> getMetadata()
    {
        return Collections.unmodifiableMap(this.metadata);
    }

    /**
     * Returns the execution mode governing this recording run.
     *
     * @return the execution mode
     */
    public ExecutionMode getExecutionMode()
    {
        return this.executionMode;
    }

    /**
     * Checks if this run performed live LLM action generation.
     *
     * @return true if live execution mode, false otherwise
     */
    public boolean isLive()
    {
        return this.executionMode.isLive();
    }

    /**
     * Checks if this run executed pre-recorded actions from replay cache.
     *
     * @return true if replay mode, false otherwise
     */
    public boolean isReplay()
    {
        return this.executionMode.isReplay();
    }

    /**
     * Checks if this run automatically recorded executed actions to playbook storage.
     *
     * @return true if recording mode, false otherwise
     */
    public boolean isRecording()
    {
        return this.executionMode.isRecording();
    }

    /**
     * Checks if this run supports self-healing on step replay failure.
     *
     * @return true if healing supported, false otherwise
     */
    public boolean supportsHealing()
    {
        return this.executionMode.supportsHealing();
    }

    /**
     * Checks if this run executed under strict replay mode without LLM fallbacks.
     *
     * @return true if strict replay mode, false otherwise
     */
    public boolean isStrictReplay()
    {
        return this.executionMode == ExecutionMode.REPLAY_STRICT;
    }

    /**
     * Returns the total count of executed playbook steps.
     *
     * @return step count
     */
    public int getStepCount()
    {
        return this.recordedSteps.size();
    }

    /**
     * Checks if any recorded step was resolved via self-healing.
     *
     * @return true if healed step count > 0, false otherwise
     */
    public boolean wasHealed()
    {
        return this.recordedSteps.stream().anyMatch(step -> step.getStatus() == PlaybookStepStatus.HEALED);
    }

    /**
     * Returns the count of steps resolved via self-healing.
     *
     * @return healed step count
     */
    public long getHealedStepCount()
    {
        return this.recordedSteps.stream().filter(step -> step.getStatus() == PlaybookStepStatus.HEALED).count();
    }

    /**
     * Returns the count of soft/optional playbook steps that failed but were tolerated.
     *
     * @return soft failed step count
     */
    public long getSoftFailedStepCount()
    {
        return this.recordedSteps.stream().filter(step -> step.isFailed() || step.getStatus() == PlaybookStepStatus.FAILED).count();
    }

    /**
     * Returns the list of steps resolved via self-healing.
     *
     * @return list of healed steps
     */
    public List<PlaybookStep> getHealedSteps()
    {
        return this.recordedSteps.stream().filter(step -> step.getStatus() == PlaybookStepStatus.HEALED).toList();
    }

    /**
     * Computes an ExecutionMetrics snapshot instance from this recording metadata.
     *
     * @return execution metrics snapshot
     */
    public ExecutionMetrics getMetrics()
    {
        final Integer totalCalls = (Integer) this.metadata.getOrDefault("totalLlmCalls", 0);
        final Integer stdCalls = (Integer) this.metadata.getOrDefault("standardCallCount", 0);
        final Integer verifCalls = (Integer) this.metadata.getOrDefault("verificationCallCount", 0);
        final Integer pesapCalls = (Integer) this.metadata.getOrDefault("pesapCallCount", 0);
        final Integer judgeCalls = (Integer) this.metadata.getOrDefault("judgeCallCount", 0);
        final Integer rcaCalls = (Integer) this.metadata.getOrDefault("rcaCallCount", 0);
        final Integer replays = (Integer) this.metadata.getOrDefault("totalReplays", 0);
        final Integer cacheHits = (Integer) this.metadata.getOrDefault("internalCacheHits", 0);
        final Integer escalations = (Integer) this.metadata.getOrDefault("totalEscalations", 0);
        @SuppressWarnings("unchecked")
        final Map<String, Integer> contextLevelCounts = (Map<String, Integer>) this.metadata.get("contextLevelCounts");
        final TokenUsage tokenUsage = (TokenUsage) this.metadata.get("totalTokenUsage");

        return new ExecutionMetrics(
            this.executionMode,
            totalCalls != null ? totalCalls : 0,
            stdCalls != null ? stdCalls : 0,
            verifCalls != null ? verifCalls : 0,
            pesapCalls != null ? pesapCalls : 0,
            judgeCalls != null ? judgeCalls : 0,
            rcaCalls != null ? rcaCalls : 0,
            getStepCount(),
            (int) getHealedStepCount(),
            (int) getSoftFailedStepCount(),
            replays != null ? replays : 0,
            cacheHits != null ? cacheHits : 0,
            escalations != null ? escalations : 0,
            contextLevelCounts,
            tokenUsage
        );
    }

    /**
     * Creates a fluent MetricsAsserter for executing chainable assertions and mode-conditional lambdas.
     *
     * @return fluent metrics asserter
     */
    public MetricsAsserter verifyMetrics()
    {
        return new MetricsAsserter(getMetrics());
    }

    /**
     * Serializes this recording instance to an indented JSON string.
     *
     * @return the formatted JSON representation
     * @throws Exception if serialization fails
     */
    public String toJson() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(this);
    }

    /**
     * Deserializes a PlaybookRecording instance from a JSON string.
     *
     * @param json the JSON payload to parse
     * @return the parsed PlaybookRecording instance
     * @throws Exception if deserialization fails
     */
    public static PlaybookRecording fromJson(final String json) throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, PlaybookRecording.class);
    }
}
