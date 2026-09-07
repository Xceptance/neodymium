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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Unit tests verifying the Phase 1 core tool contracts: {@link AiTool},
 * {@link ToolDefinition}, {@link ToolCall}, {@link ToolResult},
 * {@link ToolContext}, and {@link ToolRegistry}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class AiToolContractTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testToolDefinitionValidationAndAccessors()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");

        final ToolDefinition definition = new ToolDefinition("browser_click", "Clicks an element on the page", schema);

        Assertions.assertEquals("browser_click", definition.name());
        Assertions.assertEquals("Clicks an element on the page", definition.description());
        Assertions.assertEquals(schema, definition.parametersSchema());

        // Validate blank/null rejections
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ToolDefinition(null, "desc", schema));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ToolDefinition("   ", "desc", schema));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ToolDefinition("name", null, schema));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ToolDefinition("name", "desc", null));
    }

    @Test
    public void testToolCallValidationAndAccessors()
    {
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "#submit-btn");

        final ToolCall call = new ToolCall("call_123", "browser_click", args);

        Assertions.assertEquals("call_123", call.callId());
        Assertions.assertEquals("browser_click", call.toolName());
        Assertions.assertEquals(args, call.arguments());

        // Validate blank/null rejections
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ToolCall(null, "tool", args));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ToolCall("call_1", null, args));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ToolCall("call_1", "   ", args));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ToolCall("call_1", "tool", null));
    }

    @Test
    public void testToolResultSuccessAndErrorFactories()
    {
        final ToolResult success = ToolResult.success("call_1", "Element clicked successfully");
        Assertions.assertEquals("call_1", success.callId());
        Assertions.assertEquals(ToolResult.Status.SUCCESS, success.status());
        Assertions.assertEquals("Element clicked successfully", success.content());
        Assertions.assertTrue(success.isSuccess());

        final ToolResult error = ToolResult.error("call_2", "Element not found");
        Assertions.assertEquals("call_2", error.callId());
        Assertions.assertEquals(ToolResult.Status.ERROR, error.status());
        Assertions.assertEquals("Element not found", error.content());
        Assertions.assertFalse(error.isSuccess());

        final ToolResult policyViolation = ToolResult.policyViolation("call_3", "Journey Fidelity Violation");
        Assertions.assertEquals("call_3", policyViolation.callId());
        Assertions.assertEquals(ToolResult.Status.POLICY_VIOLATION, policyViolation.status());
        Assertions.assertFalse(policyViolation.isSuccess());
    }

    @Test
    public void testToolResultVariablesAndArtifacts()
    {
        final byte[] dummyPng = new byte[]{1, 2, 3, 4};
        final ToolResult result = ToolResult.builder("call_1", ToolResult.Status.SUCCESS)
                .withContent("Extracted balance")
                .withVariable("accountBalance", 1250.50)
                .withArtifact("audit_screenshot", "image/png", dummyPng)
                .build();

        Assertions.assertEquals(1250.50, result.variables().get("accountBalance"));
        Assertions.assertEquals(1, result.artifacts().size());
        Assertions.assertEquals("image/png", result.artifacts().get("audit_screenshot").mimeType());
        Assertions.assertArrayEquals(dummyPng, result.artifacts().get("audit_screenshot").data());
    }

    @Test
    public void testToolRegistryRegistrationAndLookup()
    {
        final ToolRegistry registry = new ToolRegistry();
        final ObjectNode schema = MAPPER.createObjectNode();

        final AiTool mockTool = new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return new ToolDefinition("calculate_tax", "Calculates tax for an amount", schema);
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                return ToolResult.success(call.callId(), "Tax calculated: 19.00");
            }
        };

        registry.register(mockTool);

        Assertions.assertTrue(registry.hasTool("calculate_tax"));
        final Optional<AiTool> found = registry.getTool("calculate_tax");
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("calculate_tax", found.get().getDefinition().name());

        // Duplicate registration replaces or updates cleanly
        final AiTool duplicateTool = new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return new ToolDefinition("calculate_tax", "Updated tax tool", schema);
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                return ToolResult.success(call.callId(), "Updated");
            }
        };

        registry.register(duplicateTool);
        Assertions.assertEquals("Updated tax tool", registry.getTool("calculate_tax").get().getDefinition().description());

        // Unregister
        Assertions.assertTrue(registry.unregister("calculate_tax"));
        Assertions.assertFalse(registry.hasTool("calculate_tax"));
        Assertions.assertTrue(registry.getTool("calculate_tax").isEmpty());
    }

    @Test
    public void testToolRegistryConcurrentAccess() throws Exception
    {
        final ToolRegistry registry = new ToolRegistry();
        final int threadCount = 10;
        final int iterationsPerThread = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++)
        {
            final int threadId = i;
            tasks.add(() ->
            {
                for (int j = 0; j < iterationsPerThread; j++)
                {
                    final String toolName = "tool_" + threadId + "_" + j;
                    final ObjectNode s = MAPPER.createObjectNode();
                    final AiTool tool = new AiTool()
                    {
                        @Override
                        public ToolDefinition getDefinition()
                        {
                            return new ToolDefinition(toolName, "Concurrent tool " + toolName, s);
                        }

                        @Override
                        public ToolResult execute(final ToolCall call, final ToolContext context)
                        {
                            return ToolResult.success(call.callId(), "ok");
                        }
                    };

                    registry.register(tool);
                    Assertions.assertTrue(registry.hasTool(toolName));
                    Assertions.assertTrue(registry.getTool(toolName).isPresent());
                }
                return null;
            });
        }

        final List<Future<Void>> futures = executor.invokeAll(tasks);
        for (final Future<Void> future : futures)
        {
            future.get();
        }
        executor.shutdown();

        Assertions.assertEquals(threadCount * iterationsPerThread, registry.getAllTools().size());
        Assertions.assertEquals(threadCount * iterationsPerThread, registry.getDefinitions().size());
    }

    @Test
    public void testToolContextDelegationAndVariableState() throws Exception
    {
        final ToolRegistry registry = new ToolRegistry();
        final ObjectNode schema = MAPPER.createObjectNode();

        // Register a downstream helper tool
        registry.register(new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return new ToolDefinition("downstream_helper", "Helper tool", schema);
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final String input = call.arguments().path("name").asText("world");
                return ToolResult.success(call.callId(), "Hello, " + input);
            }
        });

        final SimpleToolContext context = new SimpleToolContext(registry);

        // Test session variable storage
        context.setVariable("cartSubtotal", "99.95");
        final Optional<String> varValue = context.getVariable("cartSubtotal", String.class);
        Assertions.assertTrue(varValue.isPresent());
        Assertions.assertEquals("99.95", varValue.get());

        // Test missing variable
        Assertions.assertTrue(context.getVariable("nonExistent", String.class).isEmpty());

        // Test downstream tool invocation through context
        final ToolResult helperResult = context.invokeTool("downstream_helper", Map.of("name", "Neodymium"));
        Assertions.assertTrue(helperResult.isSuccess());
        Assertions.assertEquals("Hello, Neodymium", helperResult.content());

        // Test invoking unknown tool throws IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class, () -> context.invokeTool("missing_tool", Map.of()));

        // Test artifact attachment
        final byte[] dummyData = new byte[]{10, 20, 30};
        context.attachArtifact("test_report", "application/json", dummyData);
        Assertions.assertTrue(context.hasArtifact("test_report"));
        Assertions.assertEquals("application/json", context.getArtifact("test_report").mimeType());
        Assertions.assertArrayEquals(dummyData, context.getArtifact("test_report").data());
    }
}
