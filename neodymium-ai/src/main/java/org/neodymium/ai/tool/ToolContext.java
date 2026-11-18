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

import org.neodymium.ai.tool.ToolResult.Artifact;

import java.util.Map;
import java.util.Optional;

/**
 * Execution context provided to {@link AiTool} instances during execution.
 * Enables composite tools to invoke other registered tools, read and mutate
 * session variables, and attach report artifacts without leaking direct driver handles.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public interface ToolContext
{
    /**
     * Invokes another registered tool by name with the given argument map.
     *
     * @param toolName name of the tool to invoke
     * @param arguments structured arguments for the tool
     * @return result of the tool invocation
     * @throws Exception if tool lookup fails or execution encounters an exception
     */
    ToolResult invokeTool(final String toolName, final Map<String, Object> arguments) throws Exception;

    /**
     * Sets a session-scoped variable available to subsequent step resolution.
     *
     * @param key variable key
     * @param value variable value
     */
    void setVariable(final String key, final Object value);

    /**
     * Retrieves a typed session variable if present.
     *
     * @param <T> expected type
     * @param key variable key
     * @param type class of expected type
     * @return optional containing the typed variable value, or empty if absent/incompatible
     */
    <T> Optional<T> getVariable(final String key, final Class<T> type);

    /**
     * Attaches a binary or report artifact to the test execution outcome.
     *
     * @param name artifact identifier
     * @param mimeType MIME type of the artifact
     * @param data binary payload
     */
    void attachArtifact(final String name, final String mimeType, final byte[] data);

    /**
     * Checks if an artifact with the given name has been attached.
     *
     * @param name artifact name
     * @return true if present
     */
    boolean hasArtifact(final String name);

    /**
     * Retrieves an attached artifact by name.
     *
     * @param name artifact name
     * @return artifact or null if not present
     */
    Artifact getArtifact(final String name);

    /**
     * Returns an unmodifiable view of all artifacts attached in this context.
     *
     * @return map of artifact name to Artifact
     */
    Map<String, Artifact> getArtifacts();
}
