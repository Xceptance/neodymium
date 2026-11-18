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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neodymium.ai.tool.ToolResult.Artifact;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard implementation of {@link ToolContext} providing tool delegation via {@link ToolRegistry},
 * session variable state management, and artifact collection.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class SimpleToolContext implements ToolContext
{
    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    private final ToolRegistry registry;
    private final ObjectMapper mapper;
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private final Map<String, Artifact> artifacts = new ConcurrentHashMap<>();

    /**
     * Constructs a SimpleToolContext with the given registry and default ObjectMapper.
     *
     * @param registry tool registry for delegating tool invocations
     */
    public SimpleToolContext(final ToolRegistry registry)
    {
        this(registry, DEFAULT_MAPPER);
    }

    /**
     * Constructs a SimpleToolContext with the given registry and custom ObjectMapper.
     *
     * @param registry tool registry for delegating tool invocations
     * @param mapper object mapper for argument conversion
     */
    public SimpleToolContext(final ToolRegistry registry, final ObjectMapper mapper)
    {
        if (registry == null)
        {
            throw new IllegalArgumentException("ToolRegistry must not be null.");
        }
        if (mapper == null)
        {
            throw new IllegalArgumentException("ObjectMapper must not be null.");
        }
        this.registry = registry;
        this.mapper = mapper;
    }

    @Override
    public ToolResult invokeTool(final String toolName, final Map<String, Object> arguments) throws Exception
    {
        if (toolName == null || toolName.isBlank())
        {
            throw new IllegalArgumentException("Tool name must not be null or blank.");
        }
        final Optional<AiTool> toolOpt = this.registry.getTool(toolName);
        if (toolOpt.isEmpty())
        {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }

        final JsonNode argsNode = arguments != null ? this.mapper.valueToTree(arguments) : this.mapper.createObjectNode();
        final ToolCall call = new ToolCall(UUID.randomUUID().toString(), toolName, argsNode);
        final ToolResult result = toolOpt.get().execute(call, this);

        // Ingest exported variables and artifacts
        if (!result.variables().isEmpty())
        {
            this.variables.putAll(result.variables());
        }
        if (!result.artifacts().isEmpty())
        {
            this.artifacts.putAll(result.artifacts());
        }

        return result;
    }

    @Override
    public void setVariable(final String key, final Object value)
    {
        if (key != null && !key.isBlank())
        {
            if (value != null)
            {
                this.variables.put(key, value);
            }
            else
            {
                this.variables.remove(key);
            }
        }
    }

    @Override
    public <T> Optional<T> getVariable(final String key, final Class<T> type)
    {
        if (key == null || key.isBlank() || type == null)
        {
            return Optional.empty();
        }
        final Object val = this.variables.get(key);
        if (val == null)
        {
            return Optional.empty();
        }
        if (type.isInstance(val))
        {
            return Optional.of(type.cast(val));
        }
        return Optional.empty();
    }

    @Override
    public void attachArtifact(final String name, final String mimeType, final byte[] data)
    {
        final Artifact artifact = new Artifact(name, mimeType, data);
        this.artifacts.put(name, artifact);
    }

    @Override
    public boolean hasArtifact(final String name)
    {
        if (name == null || name.isBlank())
        {
            return false;
        }
        return this.artifacts.containsKey(name);
    }

    @Override
    public Artifact getArtifact(final String name)
    {
        if (name == null || name.isBlank())
        {
            return null;
        }
        return this.artifacts.get(name);
    }

    @Override
    public Map<String, Artifact> getArtifacts()
    {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.artifacts));
    }
}
