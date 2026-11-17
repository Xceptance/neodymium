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
import com.xceptance.aura.mcp.tool.AuraAnalyticsToolService;
import com.xceptance.aura.mcp.tool.AuraMcpToolService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolExecutionException;

/**
 * Unit tests for {@link AuraMcpToolCallbackProvider}.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
public class AuraMcpToolCallbackProviderTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void testAllElevenMcpToolsRegisteredWithEnrichedSchemasAndRequiredFields() throws Exception
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        final AuraMcpToolCallbackProvider provider = new AuraMcpToolCallbackProvider(mcpToolService);

        final ToolCallback[] callbacks = provider.getToolCallbacks();
        final Set<String> toolNames = Arrays.stream(callbacks)
                .map(cb -> cb.getToolDefinition().name())
                .collect(Collectors.toSet());

        final Set<String> expectedTools = Set.of(
                "list_tests",
                "run_test",
                "run_suite",
                "get_status",
                "get_run_failures",
                "get_execution_failure",
                "create_test",
                "delete_test",
                "cancel_run",
                "list_browser_profiles",
                "get_test_content"
        );

        Assertions.assertEquals(expectedTools, toolNames);

        final ToolCallback runTestCallback = findTool(callbacks, "run_test");
        final JsonNode runTestSchema = OBJECT_MAPPER.readTree(runTestCallback.getToolDefinition().inputSchema());
        Assertions.assertTrue(runTestSchema.path("properties").has("testPath"));
        Assertions.assertFalse(runTestSchema.path("properties").path("testPath").path("description").asText().isBlank());
        Assertions.assertEquals(List.of("testPath"), readRequiredList(runTestSchema));

        final ToolCallback listTestsCallback = findTool(callbacks, "list_tests");
        final JsonNode listTestsSchema = OBJECT_MAPPER.readTree(listTestsCallback.getToolDefinition().inputSchema());
        Assertions.assertEquals(List.of(), readRequiredList(listTestsSchema));

        final ToolCallback deleteTestCallback = findTool(callbacks, "delete_test");
        final JsonNode deleteTestSchema = OBJECT_MAPPER.readTree(deleteTestCallback.getToolDefinition().inputSchema());
        Assertions.assertEquals(List.of("testPath"), readRequiredList(deleteTestSchema));
    }

    @Test
    public void testGetToolCallbacksDelegatesDirectlyToMcpToolService() throws Exception
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.listTests("smoke")).thenReturn(Map.of("tests", List.of("smoke.yaml")));

        final AuraMcpToolCallbackProvider provider = new AuraMcpToolCallbackProvider(mcpToolService);
        final ToolCallback[] callbacks = provider.getToolCallbacks();
        final ToolCallback listTestsCallback = findTool(callbacks, "list_tests");

        final String result = listTestsCallback.call("{\"filter\":\"smoke\"}");
        final JsonNode resultJson = OBJECT_MAPPER.readTree(result);

        Assertions.assertEquals("smoke.yaml", resultJson.path("tests").get(0).asText());
        Mockito.verify(mcpToolService).listTests("smoke");
    }

    @Test
    public void testGetToolCallbacksPropagatesDelegateExceptionAsToolExecutionException()
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        Mockito.when(mcpToolService.listTests(Mockito.any()))
                .thenThrow(new IllegalStateException("Disk read failure"));

        final AuraMcpToolCallbackProvider provider = new AuraMcpToolCallbackProvider(mcpToolService);
        final ToolCallback[] callbacks = provider.getToolCallbacks();
        final ToolCallback listTestsCallback = findTool(callbacks, "list_tests");

        final ToolExecutionException thrown = Assertions.assertThrows(
                ToolExecutionException.class,
                () -> listTestsCallback.call("{\"filter\":\"all\"}"));
        Assertions.assertInstanceOf(IllegalStateException.class, thrown.getCause());
        Assertions.assertTrue(thrown.getCause().getMessage().contains("Disk read failure"));
    }

    @Test
    public void testJsonPropertyAnnotationOverridesParameterNameInSchema() throws Exception
    {
        final Method method = SampleAnnotatedTool.class.getDeclaredMethod("execute", String.class, String.class);
        final String rawSchema = "{\"type\":\"object\",\"properties\":{"
                + "\"renamed_target\":{\"type\":\"string\"},"
                + "\"optional_note\":{\"type\":\"string\"}"
                + "},\"required\":[]}";

        final String enriched = AuraMcpToolCallbackProvider.enrichInputSchemaWithMcpParams(rawSchema, method);
        final JsonNode schemaNode = OBJECT_MAPPER.readTree(enriched);

        Assertions.assertEquals(List.of("renamed_target"), readRequiredList(schemaNode));
        Assertions.assertEquals("Target test path",
                schemaNode.path("properties").path("renamed_target").path("description").asText());
        Assertions.assertEquals("Optional note",
                schemaNode.path("properties").path("optional_note").path("description").asText());
    }

    @Test
    public void testOptionalAnalyticsToolServiceBridgedAlongsideCoreMcpTools() throws Exception
    {
        final AuraMcpToolService mcpToolService = Mockito.mock(AuraMcpToolService.class);
        final AuraAnalyticsToolService analyticsToolService = Mockito.mock(AuraAnalyticsToolService.class);
        final AuraMcpToolCallbackProvider provider = new AuraMcpToolCallbackProvider(mcpToolService, analyticsToolService);

        final ToolCallback[] callbacks = provider.getToolCallbacks();
        final Set<String> toolNames = Arrays.stream(callbacks)
                .map(cb -> cb.getToolDefinition().name())
                .collect(Collectors.toSet());

        Assertions.assertTrue(toolNames.containsAll(Set.of(
                "list_tests",
                "list_custom_dashboards",
                "analyze_dashboard_page",
                "list_runs",
                "list_batches")));
        Assertions.assertEquals(15, toolNames.size());

        final ToolCallback analyzeCallback = findTool(callbacks, "analyze_dashboard_page");
        final JsonNode analyzeSchema = OBJECT_MAPPER.readTree(analyzeCallback.getToolDefinition().inputSchema());
        Assertions.assertEquals(List.of("pageId"), readRequiredList(analyzeSchema));
    }

    private static final class SampleAnnotatedTool
    {
        @SuppressWarnings("unused")
        void execute(
                @JsonProperty("renamed_target") @McpToolParam(required = true, description = "Target test path") final String target,
                @JsonProperty("optional_note") @McpToolParam(required = false, description = "Optional note") final String note)
        {
        }
    }

    private static ToolCallback findTool(final ToolCallback[] callbacks, final String name)
    {
        return Arrays.stream(callbacks)
                .filter(cb -> name.equals(cb.getToolDefinition().name()))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> readRequiredList(final JsonNode schema)
    {
        final List<String> required = new ArrayList<>();
        final JsonNode reqNode = schema.get("required");
        if (reqNode != null && reqNode.isArray())
        {
            for (final JsonNode item : reqNode)
            {
                required.add(item.asText());
            }
        }
        return required;
    }
}
