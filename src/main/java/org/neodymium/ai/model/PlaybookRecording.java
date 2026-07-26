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
     * Constructs a PlaybookRecording.
     *
     * @param recordedSteps the list of recorded playbook steps
     */
    public PlaybookRecording(final List<PlaybookStep> recordedSteps)
    {
        this(recordedSteps, Collections.emptyMap());
    }

    /**
     * Constructs a PlaybookRecording with explicit steps and metadata.
     *
     * @param recordedSteps the list of recorded playbook steps
     * @param metadata the execution metadata map
     */
    @JsonCreator
    public PlaybookRecording(
        @JsonProperty("recordedSteps") final List<PlaybookStep> recordedSteps,
        @JsonProperty("metadata") final Map<String, Object> metadata
    )
    {
        this.recordedSteps = recordedSteps != null ? new ArrayList<>(recordedSteps) : new ArrayList<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
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
