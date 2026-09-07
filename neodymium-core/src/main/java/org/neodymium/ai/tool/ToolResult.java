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
package org.neodymium.ai.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured outcome of an {@link AiTool} execution, capturing textual/data output,
 * execution status, session variable mutations, and report artifacts.
 *
 * @param callId identifier of the tool call this result responds to
 * @param status execution status (SUCCESS, ERROR, POLICY_VIOLATION)
 * @param content human- and LLM-readable text outcome or serialized data
 * @param variables session variables exported by the tool execution
 * @param artifacts binary artifacts (screenshots, reports) produced during tool execution
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public record ToolResult(
        String callId,
        Status status,
        String content,
        Map<String, Object> variables,
        Map<String, Artifact> artifacts)
{
    /**
     * Execution status of a tool invocation.
     */
    public enum Status
    {
        /**
         * The tool executed successfully and achieved its intended outcome.
         */
        SUCCESS,

        /**
         * The tool execution failed due to an error (e.g. element not found, evaluation error).
         */
        ERROR,

        /**
         * The tool execution was blocked by a safety or policy guard (e.g. Journey Fidelity violation).
         */
        POLICY_VIOLATION
    }

    /**
     * Represents a binary or structured artifact attached during tool execution.
     *
     * @param name name of the artifact (e.g. screenshot or report name)
     * @param mimeType MIME type of the artifact data (e.g. image/png, application/json)
     * @param data raw byte payload
     */
    public record Artifact(String name, String mimeType, byte[] data)
    {
        public Artifact
        {
            if (name == null || name.isBlank())
            {
                throw new IllegalArgumentException("Artifact name must not be null or blank.");
            }
            if (mimeType == null || mimeType.isBlank())
            {
                throw new IllegalArgumentException("Artifact mimeType must not be null or blank.");
            }
            if (data == null)
            {
                throw new IllegalArgumentException("Artifact data must not be null.");
            }
            data = data.clone();
        }

        @Override
        public byte[] data()
        {
            return this.data.clone();
        }
    }

    /**
     * Compact constructor ensuring immutability of variables and artifacts maps.
     */
    public ToolResult
    {
        if (callId == null || callId.isBlank())
        {
            throw new IllegalArgumentException("ToolResult callId must not be null or blank.");
        }
        if (status == null)
        {
            throw new IllegalArgumentException("ToolResult status must not be null.");
        }
        content = content != null ? content : "";
        variables = variables != null ? Collections.unmodifiableMap(new LinkedHashMap<>(variables)) : Collections.emptyMap();
        artifacts = artifacts != null ? Collections.unmodifiableMap(new LinkedHashMap<>(artifacts)) : Collections.emptyMap();
    }

    /**
     * Returns true if this tool execution finished with {@link Status#SUCCESS}.
     *
     * @return true if successful
     */
    public boolean isSuccess()
    {
        return this.status == Status.SUCCESS;
    }

    /**
     * Returns true if this tool execution failed with {@link Status#ERROR} or {@link Status#POLICY_VIOLATION}.
     *
     * @return true if failed or blocked by policy
     */
    public boolean isError()
    {
        return this.status == Status.ERROR || this.status == Status.POLICY_VIOLATION;
    }

    /**
     * Creates a successful tool result with the given content.
     *
     * @param callId call identifier
     * @param content text outcome
     * @return successful ToolResult
     */
    public static ToolResult success(final String callId, final String content)
    {
        return new ToolResult(callId, Status.SUCCESS, content, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Creates an error tool result with the given error message.
     *
     * @param callId call identifier
     * @param errorMessage diagnostic error description
     * @return error ToolResult
     */
    public static ToolResult error(final String callId, final String errorMessage)
    {
        return new ToolResult(callId, Status.ERROR, errorMessage, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Creates a policy violation tool result with the given violation explanation.
     *
     * @param callId call identifier
     * @param violationReason reason for the policy rejection
     * @return policy violation ToolResult
     */
    public static ToolResult policyViolation(final String callId, final String violationReason)
    {
        return new ToolResult(callId, Status.POLICY_VIOLATION, violationReason, Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * Creates a fluent builder for constructing complex tool results with variables and artifacts.
     *
     * @param callId call identifier
     * @param status execution status
     * @return ToolResultBuilder instance
     */
    public static Builder builder(final String callId, final Status status)
    {
        return new Builder(callId, status);
    }

    /**
     * Fluent builder for {@link ToolResult}.
     */
    public static final class Builder
    {
        private final String callId;
        private final Status status;
        private String content = "";
        private final Map<String, Object> variables = new LinkedHashMap<>();
        private final Map<String, Artifact> artifacts = new LinkedHashMap<>();

        private Builder(final String callId, final Status status)
        {
            this.callId = callId;
            this.status = status;
        }

        public Builder withContent(final String content)
        {
            this.content = content != null ? content : "";
            return this;
        }

        public Builder withVariable(final String key, final Object value)
        {
            this.variables.put(key, value);
            return this;
        }

        public Builder withVariables(final Map<String, Object> variables)
        {
            if (variables != null)
            {
                this.variables.putAll(variables);
            }
            return this;
        }

        public Builder withArtifact(final String name, final String mimeType, final byte[] data)
        {
            this.artifacts.put(name, new Artifact(name, mimeType, data));
            return this;
        }

        public Builder withArtifact(final Artifact artifact)
        {
            if (artifact != null)
            {
                this.artifacts.put(artifact.name(), artifact);
            }
            return this;
        }

        public ToolResult build()
        {
            return new ToolResult(this.callId, this.status, this.content, this.variables, this.artifacts);
        }
    }
}
