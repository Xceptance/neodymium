/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.aura.chat.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.aura.mcp.tool.AuraAnalyticsToolService;
import com.xceptance.aura.mcp.tool.AuraMcpToolService;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Bridges {@link AuraMcpToolService} and optional {@link AuraAnalyticsToolService} {@code @McpTool}
 * methods into Spring AI {@link ToolCallback}s so the dashboard chat agent shares full tool parity
 * with the MCP server without duplicating tool definitions.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
@Component
public class AuraMcpToolCallbackProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraMcpToolCallbackProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<ToolCallback> delegateCallbacks;

    /**
     * Listener notified whenever a bridged MCP tool is invoked during a chat turn.
     */
    @FunctionalInterface
    public interface ToolExecutionObserver
    {
        /**
         * Called after a tool finishes executing or fails with an exception.
         *
         * @param toolName name of the invoked tool
         * @param toolInput raw JSON arguments passed by the model
         * @param toolOutput raw JSON result returned by the tool or error summary
         */
        void onToolExecuted(String toolName, String toolInput, String toolOutput);
    }

    /**
     * Constructs the callback provider by scanning all {@link McpTool}-annotated methods on {@link AuraMcpToolService}.
     *
     * @param mcpToolService canonical MCP tool service instance
     */
    public AuraMcpToolCallbackProvider(final AuraMcpToolService mcpToolService)
    {
        this(mcpToolService, null);
    }

    /**
     * Constructs the callback provider by scanning all {@link McpTool}-annotated methods on
     * {@link AuraMcpToolService} and, when available, {@link AuraAnalyticsToolService}.
     *
     * @param mcpToolService canonical MCP tool service instance
     * @param analyticsToolService optional dashboard and run analytics MCP tool service
     */
    @Autowired
    public AuraMcpToolCallbackProvider(final AuraMcpToolService mcpToolService,
                                       @Autowired(required = false) final AuraAnalyticsToolService analyticsToolService)
    {
        final List<ToolCallback> delegates = new ArrayList<>();
        registerMcpTools(delegates, AuraMcpToolService.class, mcpToolService);
        if (analyticsToolService != null)
        {
            registerMcpTools(delegates, AuraAnalyticsToolService.class, analyticsToolService);
        }
        this.delegateCallbacks = List.copyOf(delegates);
    }

    private static void registerMcpTools(final List<ToolCallback> delegates,
                                         final Class<?> targetClass,
                                         final Object targetInstance)
    {
        if (targetInstance == null)
        {
            return;
        }
        final Method[] methods = targetClass.getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));

        for (final Method method : methods)
        {
            final McpTool annotation = method.getAnnotation(McpTool.class);
            if (annotation == null)
            {
                continue;
            }

            final String toolName = (annotation.name() != null && !annotation.name().isBlank())
                    ? annotation.name()
                    : method.getName();
            final String description = annotation.description() != null ? annotation.description() : "";
            final String inputSchema = enrichInputSchemaWithMcpParams(
                    JsonSchemaGenerator.generateForMethodInput(method), method);

            final ToolDefinition definition = ToolDefinition.builder()
                    .name(toolName)
                    .description(description)
                    .inputSchema(inputSchema)
                    .build();

            final MethodToolCallback delegate = MethodToolCallback.builder()
                    .toolDefinition(definition)
                    .toolMetadata(ToolMetadata.builder().build())
                    .toolMethod(method)
                    .toolObject(targetInstance)
                    .build();

            delegates.add(delegate);
        }
    }

    /**
     * Creates per-turn {@link ToolCallback} instances bound to the supplied observer.
     *
     * @param observer optional observer notified of each tool invocation during the turn
     * @return array of observer-bound Spring AI {@link ToolCallback}s
     */
    public ToolCallback[] createToolCallbacks(final ToolExecutionObserver observer)
    {
        final ToolCallback[] callbacks = new ToolCallback[this.delegateCallbacks.size()];
        for (int i = 0; i < this.delegateCallbacks.size(); i++)
        {
            callbacks[i] = new ObservingToolCallback(this.delegateCallbacks.get(i), observer);
        }
        return callbacks;
    }

    /**
     * Returns all bridged MCP tool callbacks without an attached turn observer.
     *
     * @return array of Spring AI {@link ToolCallback}s
     */
    public ToolCallback[] getToolCallbacks()
    {
        return createToolCallbacks(null);
    }

    static String enrichInputSchemaWithMcpParams(final String rawSchema, final Method method)
    {
        if (rawSchema == null || rawSchema.isBlank())
        {
            return rawSchema;
        }
        try
        {
            final JsonNode root = OBJECT_MAPPER.readTree(rawSchema);
            if (!(root instanceof ObjectNode rootObj))
            {
                return rawSchema;
            }

            final Set<String> originalRequired = new HashSet<>();
            final JsonNode existingReqNode = rootObj.get("required");
            if (existingReqNode != null && existingReqNode.isArray())
            {
                for (final JsonNode reqItem : existingReqNode)
                {
                    if (reqItem.isTextual())
                    {
                        originalRequired.add(reqItem.asText());
                    }
                }
            }

            final ArrayNode updatedRequired = OBJECT_MAPPER.createArrayNode();
            final JsonNode propsNode = rootObj.get("properties");
            final ObjectNode propsObj = (propsNode instanceof ObjectNode obj) ? obj : null;

            for (final Parameter param : method.getParameters())
            {
                if (ToolContext.class.isAssignableFrom(param.getType()))
                {
                    continue;
                }

                final JsonProperty jsonProperty = param.getAnnotation(JsonProperty.class);
                final String paramName = (jsonProperty != null && jsonProperty.value() != null
                        && !jsonProperty.value().isBlank())
                                ? jsonProperty.value()
                                : param.getName();
                final McpToolParam mcpParam = param.getAnnotation(McpToolParam.class);
                final ToolParam toolParam = param.getAnnotation(ToolParam.class);

                if (propsObj != null && mcpParam != null && mcpParam.description() != null && !mcpParam.description().isBlank())
                {
                    final JsonNode paramSchema = propsObj.has(paramName)
                            ? propsObj.get(paramName)
                            : propsObj.get(param.getName());
                    if (paramSchema instanceof ObjectNode paramObj && !paramObj.has("description"))
                    {
                        paramObj.put("description", mcpParam.description());
                    }
                }

                final boolean hasNullable = Arrays.stream(param.getAnnotations())
                        .anyMatch(ann -> "Nullable".equals(ann.annotationType().getSimpleName()));

                final boolean required;
                if (hasNullable)
                {
                    required = false;
                }
                else if (mcpParam != null)
                {
                    required = mcpParam.required();
                }
                else if (toolParam != null)
                {
                    required = toolParam.required();
                }
                else
                {
                    required = originalRequired.contains(paramName) || originalRequired.contains(param.getName());
                }

                if (required)
                {
                    updatedRequired.add(paramName);
                }
            }

            if (updatedRequired.isEmpty())
            {
                rootObj.remove("required");
            }
            else
            {
                rootObj.set("required", updatedRequired);
            }

            return OBJECT_MAPPER.writeValueAsString(rootObj);
        }
        catch (final Exception e)
        {
            LOGGER.warn("Failed to enrich JSON input schema for MCP tool method {}: {}", method.getName(), e.getMessage(), e);
            return rawSchema;
        }
    }

    private static final class ObservingToolCallback implements ToolCallback
    {
        private final ToolCallback delegate;
        private final ToolExecutionObserver observer;

        private ObservingToolCallback(final ToolCallback delegate, final ToolExecutionObserver observer)
        {
            this.delegate = delegate;
            this.observer = observer;
        }

        @Override
        public ToolDefinition getToolDefinition()
        {
            return this.delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata()
        {
            return this.delegate.getToolMetadata();
        }

        @Override
        public String call(final String toolInput)
        {
            try
            {
                final String output = this.delegate.call(toolInput);
                notifyObserver(toolInput, output);
                return output;
            }
            catch (final RuntimeException e)
            {
                notifyObserver(toolInput, formatError(e));
                throw e;
            }
        }

        @Override
        public String call(final String toolInput, final ToolContext toolContext)
        {
            try
            {
                final String output = this.delegate.call(toolInput, toolContext);
                notifyObserver(toolInput, output);
                return output;
            }
            catch (final RuntimeException e)
            {
                notifyObserver(toolInput, formatError(e));
                throw e;
            }
        }

        private void notifyObserver(final String toolInput, final String output)
        {
            if (this.observer != null)
            {
                this.observer.onToolExecuted(this.delegate.getToolDefinition().name(), toolInput, output);
            }
        }

        private static String formatError(final RuntimeException e)
        {
            final String msg = e.getMessage() != null ? e.getMessage() : "Tool execution failed";
            return "[ERROR] " + e.getClass().getSimpleName() + ": " + msg;
        }
    }
}
