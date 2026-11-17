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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.aura.mcp.tool.AuraAnalyticsToolService;
import com.xceptance.aura.mcp.tool.AuraMcpToolService;
import com.xceptance.aura.mcp.tool.McpToolCatalogHelper;
import com.xceptance.aura.mcp.tool.McpToolCatalogHelper.McpToolDescriptor;
import com.xceptance.aura.mcp.tool.McpToolCatalogHelper.McpToolParamDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Bridges discovered {@code @McpTool}-annotated Spring beans into Spring AI {@link ToolCallback}s
 * so the dashboard chat agent shares full tool parity with the MCP server without duplicating
 * tool definitions.
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
     * Constructs the callback provider by discovering all {@code @McpTool}-annotated beans
     * registered in the Spring {@link ListableBeanFactory}.
     *
     * @param beanFactory Spring bean factory containing registered MCP tool services
     */
    @Autowired
    public AuraMcpToolCallbackProvider(final ListableBeanFactory beanFactory)
    {
        this.delegateCallbacks = buildDelegateCallbacks(McpToolCatalogHelper.resolveFromBeanFactory(beanFactory));
    }

    /**
     * Constructs the callback provider from {@link AuraMcpToolService}.
     *
     * @param mcpToolService canonical MCP tool service instance
     */
    public AuraMcpToolCallbackProvider(final AuraMcpToolService mcpToolService)
    {
        this(mcpToolService, null);
    }

    /**
     * Constructs the callback provider from explicit {@link AuraMcpToolService} and
     * optional {@link AuraAnalyticsToolService} instances.
     *
     * @param mcpToolService canonical MCP tool service instance
     * @param analyticsToolService optional dashboard and run analytics MCP tool service
     */
    public AuraMcpToolCallbackProvider(final AuraMcpToolService mcpToolService,
                                       final AuraAnalyticsToolService analyticsToolService)
    {
        this.delegateCallbacks = buildDelegateCallbacks(
                McpToolCatalogHelper.resolveFromBeans(mcpToolService, analyticsToolService));
    }

    private static List<ToolCallback> buildDelegateCallbacks(final List<McpToolDescriptor> descriptors)
    {
        final List<ToolCallback> delegates = new ArrayList<>(descriptors.size());
        for (final McpToolDescriptor descriptor : descriptors)
        {
            final String inputSchema = enrichInputSchemaWithMcpParams(
                    JsonSchemaGenerator.generateForMethodInput(descriptor.method()), descriptor.method());

            final ToolDefinition definition = ToolDefinition.builder()
                    .name(descriptor.name())
                    .description(descriptor.description())
                    .inputSchema(inputSchema)
                    .build();

            final MethodToolCallback delegate = MethodToolCallback.builder()
                    .toolDefinition(definition)
                    .toolMetadata(ToolMetadata.builder().build())
                    .toolMethod(descriptor.method())
                    .toolObject(descriptor.bean())
                    .build();

            delegates.add(delegate);
        }
        return List.copyOf(delegates);
    }

    /**
     * Returns all bridged MCP tool callbacks.
     *
     * @return array of Spring AI {@link ToolCallback}s
     */
    public ToolCallback[] getToolCallbacks()
    {
        return this.delegateCallbacks.toArray(ToolCallback[]::new);
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

            for (final McpToolParamDescriptor param : McpToolCatalogHelper.extractParameters(method))
            {
                if (propsObj != null && !param.description().isBlank())
                {
                    final JsonNode paramSchema = propsObj.has(param.name())
                            ? propsObj.get(param.name())
                            : propsObj.get(param.rawName());
                    if (paramSchema instanceof ObjectNode paramObj && !paramObj.has("description"))
                    {
                        paramObj.put("description", param.description());
                    }
                }

                if (param.isRequired(originalRequired))
                {
                    updatedRequired.add(param.name());
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
}
