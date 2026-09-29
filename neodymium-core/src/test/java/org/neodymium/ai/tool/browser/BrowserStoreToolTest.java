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
package org.neodymium.ai.tool.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests validating the browser_store tool execution, literal value storage,
 * price adjustment, parameter validation, and session data integration.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class BrowserStoreToolTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolRegistry registry;
    private ExecutionContext context;
    private ToolContext toolContext;

    @BeforeEach
    public void setUp()
    {
        this.registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(this.registry);

        this.context = new ExecutionContext(new SessionData(Collections.emptyMap()));
        ExecutionContext.setActiveContext(this.context);

        this.toolContext = new SimpleToolContext(this.registry);
    }

    @AfterEach
    public void tearDown()
    {
        ExecutionContext.setActiveContext(null);
    }

    @Test
    public void testStoreLiteralValue() throws Exception
    {
        final AiTool storeTool = this.registry.getTool("browser_store").orElseThrow();

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("variableName", "userStatus");
        args.put("value", "active_premium");

        final ToolCall call = new ToolCall("call_1", "browser_store", args);
        final ToolResult result = storeTool.execute(call, this.toolContext);

        assertTrue(result.isSuccess());
        assertEquals("active_premium", this.context.getSessionData().get("userStatus"));
    }

    @Test
    public void testStoreLiteralValueWithPriceAdjustment() throws Exception
    {
        final AiTool storeTool = this.registry.getTool("browser_store").orElseThrow();

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("variableName", "totalPrice");
        args.put("value", "$ 24.99 USD");
        args.put("adjust", true);

        final ToolCall call = new ToolCall("call_2", "browser_store", args);
        final ToolResult result = storeTool.execute(call, this.toolContext);

        assertTrue(result.isSuccess());
        assertEquals("24.99", this.context.getSessionData().get("totalPrice"));
    }

    @Test
    public void testStoreMissingVariableNameFails() throws Exception
    {
        final AiTool storeTool = this.registry.getTool("browser_store").orElseThrow();

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("value", "something");

        final ToolCall call = new ToolCall("call_3", "browser_store", args);
        final ToolResult result = storeTool.execute(call, this.toolContext);

        assertFalse(result.isSuccess());
        assertTrue(result.content().contains("variableName"));
    }

    @Test
    public void testStoreMissingValueAndSelectorFails() throws Exception
    {
        final AiTool storeTool = this.registry.getTool("browser_store").orElseThrow();

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("variableName", "itemCount");

        final ToolCall call = new ToolCall("call_4", "browser_store", args);
        final ToolResult result = storeTool.execute(call, this.toolContext);

        assertFalse(result.isSuccess());
        assertTrue(result.content().contains("selector"));
    }
}
