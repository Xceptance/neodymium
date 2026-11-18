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
package org.neodymium.ai.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.steps.AgentToolLoopStep;
import org.neodymium.ai.tool.ToolCall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests validating prompt compilation, action argument sanitization,
 * and JSON response parsing in {@link VerificationPrompt}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerificationPromptTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VerificationPrompt prompt;

    private ExecutionContext context;

    @BeforeEach
    public void setUp()
    {
        this.prompt = new VerificationPrompt();
        this.context = new ExecutionContext(null);
    }

    @Test
    public void testCompileUserMessageMarkdownFormattingAndSanitization()
    {
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Öffne die Länderauswahl.");
        this.context.getTransientData().put(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY, "Successfully opened the country selector.");

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "#country-trigger-btn");
        args.put("thought", "Internal LLM chain-of-thought");

        final ObjectNode vector = args.putObject("domFeatureVector");
        vector.put("tag", "button");
        vector.put("text", "🇺🇸");
        vector.put("role", "");
        vector.put("accessibleName", "Select Country");
        vector.put("parentTag", "div");
        vector.put("siblingIndex", 1);
        vector.put("x", 1091);
        vector.put("y", 63);
        vector.put("width", 41);
        vector.put("height", 32);
        vector.put("visualHash", "");
        vector.put("tileSsim", "");

        final ToolCall call = new ToolCall("call_1", "browser_click", args);
        this.context.getTransientData().put(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS, List.of(call));

        final String userMessage = this.prompt.compileUserMessage(this.context);

        // Verify Markdown headings
        assertTrue(userMessage.contains("## Instruction\nÖffne die Länderauswahl."));
        assertTrue(userMessage.contains("## Agent Claimed Summary\nSuccessfully opened the country selector."));
        assertTrue(userMessage.contains("## Executed Tool Calls / Actions"));

        // Verify sanitized tool call
        assertTrue(userMessage.contains("- browser_click(selector=\"#country-trigger-btn\", text=\"🇺🇸\", accessibleName=\"Select Country\")"));

        // Verify removal of legacy triple quotes and noisy internal fields
        assertFalse(userMessage.contains("\"\"\""));
        assertFalse(userMessage.contains("domFeatureVector"));
        assertFalse(userMessage.contains("visualHash"));
        assertFalse(userMessage.contains("tileSsim"));
        assertFalse(userMessage.contains("siblingIndex"));
        assertFalse(userMessage.contains("parentTag"));
        assertFalse(userMessage.contains("Internal LLM chain-of-thought"));
    }

    @Test
    public void testSanitizeToolCallArgumentsVariousTools()
    {
        final ObjectNode typeArgs = MAPPER.createObjectNode();
        typeArgs.put("selector", "#username");
        typeArgs.put("text", "admin");
        typeArgs.put("clearFirst", true);
        typeArgs.put("thought", "Typing username");

        final ToolCall typeCall = new ToolCall("call_type", "browser_type", typeArgs);
        final String typeFormatted = VerificationPrompt.formatToolCallForVerification(typeCall);
        assertEquals("browser_type(selector=\"#username\", text=\"admin\", clearFirst=true)", typeFormatted);

        final ObjectNode navArgs = MAPPER.createObjectNode();
        navArgs.put("url", "https://example.com/checkout");
        final ToolCall navCall = new ToolCall("call_nav", "browser_navigate", navArgs);
        final String navFormatted = VerificationPrompt.formatToolCallForVerification(navCall);
        assertEquals("browser_navigate(url=\"https://example.com/checkout\")", navFormatted);

        final ObjectNode waitArgs = MAPPER.createObjectNode();
        waitArgs.put("milliseconds", 500);
        final ToolCall waitCall = new ToolCall("call_wait", "browser_wait", waitArgs);
        final String waitFormatted = VerificationPrompt.formatToolCallForVerification(waitCall);
        assertEquals("browser_wait(milliseconds=500)", waitFormatted);

        final ToolCall backCall = new ToolCall("call_back", "browser_back", MAPPER.createObjectNode());
        final String backFormatted = VerificationPrompt.formatToolCallForVerification(backCall);
        assertEquals("browser_back()", backFormatted);
    }

    @Test
    public void testCompileUserMessageWithActionsFallback()
    {
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Submit login form");
        final Action action = new Action("CLICK", "#login-btn", "Submit Login Form");
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_STEP_ACTIONS, List.of(action));

        final String userMessage = this.prompt.compileUserMessage(this.context);

        assertTrue(userMessage.contains("## Instruction\nSubmit login form"));
        assertTrue(userMessage.contains("## Executed Tool Calls / Actions\n- CLICK: Submit Login Form"));
        assertFalse(userMessage.contains("## Agent Claimed Summary"));
    }

    @Test
    public void testCompileUserMessageNoActions()
    {
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify text visible");
        this.context.getTransientData().put(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS, Collections.emptyList());

        final String userMessage = this.prompt.compileUserMessage(this.context);

        assertTrue(userMessage.contains("## Instruction\nVerify text visible"));
        assertTrue(userMessage.contains("## Executed Tool Calls / Actions\n(No actions executed)"));
    }

    @Test
    public void testParseResponseValidJson() throws Exception
    {
        final String rawResponse = """
            {
              "rubrics": {
                "intentMatch": {
                  "analysis": "Action matches intent",
                  "score": "PASS"
                },
                "visualDelta": {
                  "analysis": "Country selector opened",
                  "score": "PASS"
                },
                "absenceOfErrors": {
                  "analysis": "No errors visible",
                  "score": "PASS"
                }
              },
              "overallVerdict": {
                "passed": true,
                "summary": "Step passed successfully"
              }
            }
            """;

        final VerificationResult result = this.prompt.parseResponse(rawResponse, this.context);

        assertNotNull(result);
        assertTrue(result.passed());
        assertNotNull(result.getOverallVerdict());
        assertEquals("Step passed successfully", result.getOverallVerdict().summary());
        assertNotNull(result.getRubrics());
        assertEquals("PASS", result.getRubrics().intentMatch().score());
    }

    @Test
    public void testParseResponseEmpty() throws Exception
    {
        final VerificationResult result = this.prompt.parseResponse("", this.context);
        assertNotNull(result);
        assertFalse(result.passed());
        assertNotNull(result.getOverallVerdict());
        assertTrue(result.getOverallVerdict().summary().contains("Empty validation response"));
    }
}
