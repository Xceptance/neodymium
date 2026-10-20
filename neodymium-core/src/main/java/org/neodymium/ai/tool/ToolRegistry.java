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
import org.neodymium.ai.config.AiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe registry for discovering, registering, and retrieving {@link AiTool} instances.
 * Provides definition aggregation for prompt generation and tool lookup for agent dispatch.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class ToolRegistry
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolRegistry.class);
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
        return getTool(toolName).isPresent();
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
        AiTool tool = this.tools.get(toolName);
        if (tool == null && toolName.startsWith("browser_"))
        {
            tool = this.tools.get(toolName.substring("browser_".length()));
        }
        if (tool == null && !toolName.startsWith("browser_"))
        {
            tool = this.tools.get("browser_" + toolName);
        }
        if (tool == null && ("type".equals(toolName) || "browser_type".equals(toolName)))
        {
            tool = this.tools.get("fill");
            if (tool == null)
            {
                tool = this.tools.get("browser_fill");
            }
        }
        if (tool == null && ("browser_take_screenshot".equals(toolName) || "take_screenshot".equals(toolName)))
        {
            tool = this.tools.get("screenshot");
            if (tool == null)
            {
                tool = this.tools.get("browser_screenshot");
            }
        }
        if (tool == null)
        {
            for (final Map.Entry<String, AiTool> entry : this.tools.entrySet())
            {
                final String registeredName = entry.getKey();
                if (registeredName.equalsIgnoreCase(toolName)
                        || registeredName.replace("_", "").equalsIgnoreCase(toolName.replace("_", "")))
                {
                    tool = entry.getValue();
                    break;
                }
            }
        }
        return Optional.ofNullable(tool);
    }

    /**
     * Discovers and registers custom Java tools configured in the {@code neodymium.ai.agent.methods.classes}
     * configuration property.
     *
     * @param registry tool registry to populate
     */
    public static void registerConfiguredCustomTools(final ToolRegistry registry)
    {
        if (registry == null)
        {
            return;
        }

        final AiConfiguration config = AiConfiguration.getInstance();
        final String classesStr = config.getProperty("neodymium.ai.agent.methods.classes", "");
        if (classesStr == null || classesStr.isBlank())
        {
            return;
        }

        final ClassLoader cl = Thread.currentThread().getContextClassLoader() != null
                ? Thread.currentThread().getContextClassLoader()
                : ToolRegistry.class.getClassLoader();

        for (final String rawClassName : classesStr.split(","))
        {
            final String className = rawClassName.trim();
            if (className.isEmpty())
            {
                continue;
            }

            try
            {
                final Class<?> clazz = Class.forName(className, true, cl);
                final List<AiTool> customTools = JavaToolFactory.createToolsFromClass(clazz);
                for (final AiTool tool : customTools)
                {
                    registry.register(tool);
                    LOGGER.debug("Registered custom AI tool '{}' from class '{}'", tool.getDefinition().name(), className);
                }
            }
            catch (final ClassNotFoundException e)
            {
                LOGGER.warn("Configured custom tool class '{}' not found on classpath: {}", className, e.getMessage());
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to register custom AI tools from class '{}': {}", className, e.getMessage(), e);
            }
        }
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
