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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.executor.selenide.frame.FrameContextManager;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Browser-less tests for the frame decorators of {@link BrowserToolProvider}, using a recording fake tool
 * (no WebDriver is started, so frame entering is a no-op and only argument handling is verified).
 *
 * @author AI-generated: Claude Sonnet 5.5
 * @author Xceptance GmbH 2026
 */
public class BrowserToolProviderFrameWrapperTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AtomicReference<JsonNode> seenArgs = new AtomicReference<>();

    @AfterEach
    public void tearDown()
    {
        FrameContextManager.resetActiveFramePath();
    }

    private AiTool fakeTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("selector").put("type", "string");
        final ToolDefinition def = new ToolDefinition("fake", "fake tool", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                BrowserToolProviderFrameWrapperTest.this.seenArgs.set(call.arguments());
                return new ToolResult(call.callId(), ToolResult.Status.SUCCESS, "{\"status\":\"SUCCESS\"}", Map.of(), Map.of());
            }
        };
    }

    @Test
    public void testExplicitFrameAndSelectorPrefixAreMergedAndStripped() throws Exception
    {
        final AiTool wrapped = BrowserToolProvider.wrapWithFrameSupport(fakeTool());
        final ObjectNode args = MAPPER.createObjectNode().put("frame", "#outer").put("selector", "#inner >>> #btn");

        final ToolResult result = wrapped.execute(new ToolCall("c1", "fake", args), null);

        assertEquals("#btn", this.seenArgs.get().path("selector").asText());
        assertEquals("#outer >>> #inner", MAPPER.readTree(result.content()).path("frame").asText());
        assertEquals("#inner >>> #btn", args.path("selector").asText(), "Original call arguments must not be mutated");
    }

    @Test
    public void testSelectorPrefixAloneBecomesFrame() throws Exception
    {
        final AiTool wrapped = BrowserToolProvider.wrapWithFrameSupport(fakeTool());
        final ObjectNode args = MAPPER.createObjectNode().put("selector", "#a >>> #b >>> #btn");

        final ToolResult result = wrapped.execute(new ToolCall("c2", "fake", args), null);

        assertEquals("#btn", this.seenArgs.get().path("selector").asText());
        assertEquals("#a >>> #b", MAPPER.readTree(result.content()).path("frame").asText());
    }

    @Test
    public void testNoFrameLeavesResultAndArgumentsUntouched() throws Exception
    {
        final AiTool wrapped = BrowserToolProvider.wrapWithFrameSupport(fakeTool());
        final ObjectNode args = MAPPER.createObjectNode().put("selector", "#btn");

        final ToolResult result = wrapped.execute(new ToolCall("c3", "fake", args), null);

        assertEquals("#btn", this.seenArgs.get().path("selector").asText());
        assertTrue(MAPPER.readTree(result.content()).path("frame").isMissingNode());
    }

    @Test
    public void testFrameParameterAdvertisedInSchema()
    {
        final AiTool wrapped = BrowserToolProvider.wrapWithFrameSupport(fakeTool());
        assertEquals("string", wrapped.getDefinition().parametersSchema().path("properties").path("frame").path("type").asText());
    }

    @Test
    public void testFrameResetToolClearsTrackedPathEvenOnFailure()
    {
        final AiTool failing = new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return fakeTool().getDefinition();
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                throw new IllegalStateException("boom");
            }
        };
        FrameContextManager.setActiveFramePath("#stale");

        final AiTool wrapped = BrowserToolProvider.wrapWithFrameReset(failing);
        try
        {
            wrapped.execute(new ToolCall("c4", "fake", MAPPER.createObjectNode()), null);
        }
        catch (final IllegalStateException expected)
        {
            assertEquals("boom", expected.getMessage());
        }
        catch (final Exception e)
        {
            throw new AssertionError(e);
        }
        assertNull(FrameContextManager.getActiveFramePath());
    }
}
