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
package org.neodymium.ai.tool.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.monitor.ConsoleLogMonitor;
import org.neodymium.ai.monitor.NetworkTrafficMonitor;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for browser protocol and console introspection tools exposed by {@link BrowserToolProvider}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class BrowserToolProviderIntrospectionTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private NetworkTrafficMonitor networkMonitor;
    private ConsoleLogMonitor consoleMonitor;

    @BeforeEach
    public void setUp()
    {
        networkMonitor = new NetworkTrafficMonitor(10, null);
        consoleMonitor = new ConsoleLogMonitor(10);
        BrowserToolProvider.setNetworkTrafficMonitor(networkMonitor);
        BrowserToolProvider.setConsoleLogMonitor(consoleMonitor);
    }

    @AfterEach
    public void tearDown()
    {
        BrowserToolProvider.setNetworkTrafficMonitor(null);
        BrowserToolProvider.setConsoleLogMonitor(null);
    }

    @Test
    public void testNetworkFailuresToolSchema()
    {
        final AiTool tool = BrowserToolProvider.createGetNetworkFailuresTool();
        final ToolDefinition def = tool.getDefinition();

        assertEquals("browser_get_network_failures", def.name());
        assertNotNull(def.description());

        final JsonNode schema = def.parametersSchema();
        assertEquals("object", schema.path("type").asText());

        final JsonNode props = schema.path("properties");
        assertTrue(props.has("status_filter"));
        assertTrue(props.has("limit"));
        assertTrue(props.has("clear_buffer"));
    }

    @Test
    public void testConsoleErrorsToolSchema()
    {
        final AiTool tool = BrowserToolProvider.createGetConsoleErrorsTool();
        final ToolDefinition def = tool.getDefinition();

        assertEquals("browser_get_console_errors", def.name());
        assertNotNull(def.description());

        final JsonNode schema = def.parametersSchema();
        assertEquals("object", schema.path("type").asText());

        final JsonNode props = schema.path("properties");
        assertTrue(props.has("min_level"));
        assertTrue(props.has("limit"));
        assertTrue(props.has("clear_buffer"));
    }

    @Test
    public void testNetworkFailuresToolExecutionAndOutput() throws Exception
    {
        networkMonitor.recordFailure("https://example.com/api/item/404", 404, "GET", "Not Found");
        networkMonitor.recordFailure("https://example.com/api/cart/500", 500, "POST", "Internal Server Error");

        final AiTool tool = BrowserToolProvider.createGetNetworkFailuresTool();

        // 1. Filter by 5xx with clear_buffer=true
        final JsonNode args5xx = MAPPER.readTree("{\"status_filter\": \"5xx\", \"clear_buffer\": true}");
        final ToolCall call5xx = new ToolCall("call-1", "browser_get_network_failures", args5xx);
        final ToolResult res5xx = tool.execute(call5xx, null);

        assertTrue(res5xx.isSuccess());
        final JsonNode out5xx = MAPPER.readTree(res5xx.content());
        assertEquals("SUCCESS", out5xx.path("status").asText());
        assertEquals(1, out5xx.path("count").asInt());
        assertEquals("https://example.com/api/cart/500", out5xx.path("failures").get(0).path("url").asText());
        assertEquals(500, out5xx.path("failures").get(0).path("statusCode").asInt());

        // 2. Query again for 5xx - should be 0 because buffer was drained
        final ToolCall call5xxAgain = new ToolCall("call-2", "browser_get_network_failures", args5xx);
        final ToolResult res5xxAgain = tool.execute(call5xxAgain, null);
        final JsonNode out5xxAgain = MAPPER.readTree(res5xxAgain.content());
        assertEquals(0, out5xxAgain.path("count").asInt());

        // 3. Query 4xx - still exists in buffer
        final JsonNode args4xx = MAPPER.readTree("{\"status_filter\": \"4xx\", \"clear_buffer\": false}");
        final ToolCall call4xx = new ToolCall("call-3", "browser_get_network_failures", args4xx);
        final ToolResult res4xx = tool.execute(call4xx, null);
        final JsonNode out4xx = MAPPER.readTree(res4xx.content());
        assertEquals(1, out4xx.path("count").asInt());
        assertEquals("https://example.com/api/item/404", out4xx.path("failures").get(0).path("url").asText());
    }

    @Test
    public void testConsoleErrorsToolExecutionAndOutput() throws Exception
    {
        consoleMonitor.recordEntry("INFO", "App initializing", "init.js", 1, null);
        consoleMonitor.recordException("Uncaught ReferenceError: foo is not defined", "main.js", 88, "at main.js:88:5");

        final AiTool tool = BrowserToolProvider.createGetConsoleErrorsTool();

        // 1. Query SEVERE with clear_buffer=true
        final JsonNode argsSevere = MAPPER.readTree("{\"min_level\": \"SEVERE\", \"clear_buffer\": true}");
        final ToolCall callSevere = new ToolCall("call-1", "browser_get_console_errors", argsSevere);
        final ToolResult resSevere = tool.execute(callSevere, null);

        assertTrue(resSevere.isSuccess());
        final JsonNode outSevere = MAPPER.readTree(resSevere.content());
        assertEquals("SUCCESS", outSevere.path("status").asText());
        assertEquals(1, outSevere.path("count").asInt());

        final JsonNode log0 = outSevere.path("logs").get(0);
        assertEquals("SEVERE", log0.path("level").asText());
        assertEquals("Uncaught ReferenceError: foo is not defined", log0.path("message").asText());
        assertEquals("main.js", log0.path("source").asText());
        assertEquals(88, log0.path("lineNumber").asInt());

        // 2. Second query should be 0 because buffer was drained
        final ToolCall callSevere2 = new ToolCall("call-2", "browser_get_console_errors", argsSevere);
        final ToolResult resSevere2 = tool.execute(callSevere2, null);
        final JsonNode outSevere2 = MAPPER.readTree(resSevere2.content());
        assertEquals(0, outSevere2.path("count").asInt());
    }

    @Test
    public void testStandardToolsRegistration()
    {
        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerStandardTools(registry);

        assertNotNull(registry.getTool("browser_get_network_failures"));
        assertNotNull(registry.getTool("browser_get_console_errors"));
    }
}
