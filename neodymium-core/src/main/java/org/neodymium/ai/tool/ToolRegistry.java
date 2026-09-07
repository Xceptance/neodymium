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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for discovering, registering, and retrieving {@link AiTool} instances.
 * Provides definition aggregation for prompt generation and tool lookup for agent dispatch.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class ToolRegistry
{
    private final Map<String, AiTool> tools = new ConcurrentHashMap<>();

    /**
     * Registers an {@link AiTool} instance. If a tool with the same name already exists,
     * it will be replaced.
     *
     * @param tool the tool to register
     * @throws IllegalArgumentException if tool or tool definition is null
     */
    public void register(final AiTool tool)
    {
        if (tool == null)
        {
            throw new IllegalArgumentException("Cannot register null AiTool.");
        }
        final ToolDefinition definition = tool.getDefinition();
        if (definition == null)
        {
            throw new IllegalArgumentException("AiTool must provide a non-null ToolDefinition.");
        }
        this.tools.put(definition.name(), tool);
    }

    /**
     * Unregisters a tool by name.
     *
     * @param toolName name of the tool to unregister
     * @return true if the tool was found and removed, false otherwise
     */
    public boolean unregister(final String toolName)
    {
        if (toolName == null || toolName.isBlank())
        {
            return false;
        }
        return this.tools.remove(toolName) != null;
    }

    /**
     * Checks if a tool with the specified name is registered.
     *
     * @param toolName name of the tool
     * @return true if registered
     */
    public boolean hasTool(final String toolName)
    {
        if (toolName == null || toolName.isBlank())
        {
            return false;
        }
        return this.tools.containsKey(toolName);
    }

    /**
     * Retrieves an {@link AiTool} by name.
     *
     * @param toolName name of the tool
     * @return optional containing the tool if found, or empty
     */
    public Optional<AiTool> getTool(final String toolName)
    {
        if (toolName == null || toolName.isBlank())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(this.tools.get(toolName));
    }

    /**
     * Returns an unmodifiable collection of all registered tools.
     *
     * @return all registered tools
     */
    public Collection<AiTool> getAllTools()
    {
        return Collections.unmodifiableCollection(new ArrayList<>(this.tools.values()));
    }

    /**
     * Returns an unmodifiable list of definitions for all registered tools.
     *
     * @return list of ToolDefinition instances
     */
    public List<ToolDefinition> getDefinitions()
    {
        final List<ToolDefinition> definitions = new ArrayList<>(this.tools.size());
        for (final AiTool tool : this.tools.values())
        {
            definitions.add(tool.getDefinition());
        }
        return Collections.unmodifiableList(definitions);
    }

    /**
     * Clears all registered tools from this registry.
     */
    public void clear()
    {
        this.tools.clear();
    }
}
