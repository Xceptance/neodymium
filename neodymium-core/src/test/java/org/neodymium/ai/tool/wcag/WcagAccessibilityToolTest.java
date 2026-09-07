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
package org.neodymium.ai.tool.wcag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Unit tests validating {@link WcagAccessibilityTool} composite execution, Axe-core invocation,
 * violation detection and formatting, and artifact attachments.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class WcagAccessibilityToolTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolRegistry registry;
    private SimpleToolContext context;
    private WcagAccessibilityTool wcagTool;

    @BeforeEach
    public void setUp()
    {
        this.registry = new ToolRegistry();
        this.context = new SimpleToolContext(this.registry);
        this.wcagTool = new WcagAccessibilityTool();
        this.registry.register(this.wcagTool);
    }

    @Test
    public void testToolDefinitionAndSchema()
    {
        final ToolDefinition def = this.wcagTool.getDefinition();
        Assertions.assertEquals("wcag_accessibility_audit", def.name());
        Assertions.assertNotNull(def.description());
        Assertions.assertTrue(def.parametersSchema().has("properties"));
        Assertions.assertTrue(def.parametersSchema().path("properties").has("context"));
        Assertions.assertTrue(def.parametersSchema().path("properties").has("tags"));
        Assertions.assertTrue(def.parametersSchema().path("properties").has("fail_on_violation"));
        Assertions.assertTrue(def.parametersSchema().path("properties").has("min_impact"));
    }

    @Test
    public void testAuditWithNoViolations() throws Exception
    {
        // Mock browser_execute_script returning clean Axe results
        registerMockScriptExecutor("{\"violations\":[],\"passes\":[{\"id\":\"color-contrast\"}]}");

        final ObjectNode args = MAPPER.createObjectNode();
        final ToolCall call = new ToolCall(UUID.randomUUID().toString(), "wcag_accessibility_audit", args);

        final ToolResult result = this.wcagTool.execute(call, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertTrue(result.content().contains("0 violations found"));
        Assertions.assertTrue(this.context.hasArtifact("wcag-audit-report.json"));
        Assertions.assertTrue(this.context.hasArtifact("wcag-audit-summary.txt"));
    }

    @Test
    public void testAuditWithViolationsFormatted() throws Exception
    {
        final String axeResultsJson = """
                {
                  "violations": [
                    {
                      "id": "color-contrast",
                      "impact": "serious",
                      "description": "Elements must have sufficient color contrast",
                      "help": "Elements must have sufficient color contrast",
                      "helpUrl": "https://dequeuniversity.com/rules/axe/4.4/color-contrast",
                      "nodes": [
                        {
                          "html": "<button class=\\"btn-submit\\">Submit</button>",
                          "target": [".btn-submit"],
                          "failureSummary": "Fix color contrast: 2.5:1 is below 4.5:1"
                        }
                      ]
                    },
                    {
                      "id": "image-alt",
                      "impact": "critical",
                      "description": "Images must have alternate text",
                      "help": "Images must have alternate text",
                      "helpUrl": "https://dequeuniversity.com/rules/axe/4.4/image-alt",
                      "nodes": [
                        {
                          "html": "<img src=\\"logo.png\\">",
                          "target": ["#main-logo"],
                          "failureSummary": "Fix alternate text: element does not have an alt attribute"
                        }
                      ]
                    }
                  ]
                }
                """;

        registerMockScriptExecutor(axeResultsJson);

        final ObjectNode args = MAPPER.createObjectNode();
        final ToolCall call = new ToolCall(UUID.randomUUID().toString(), "wcag_accessibility_audit", args);

        final ToolResult result = this.wcagTool.execute(call, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertTrue(result.content().contains("2 violations found"));
        Assertions.assertTrue(result.content().contains("[CRITICAL] image-alt"));
        Assertions.assertTrue(result.content().contains("[SERIOUS] color-contrast"));
        Assertions.assertTrue(result.content().contains("#main-logo"));
        Assertions.assertTrue(result.content().contains(".btn-submit"));

        // Verify artifacts
        Assertions.assertTrue(this.context.hasArtifact("wcag-audit-report.json"));
        Assertions.assertTrue(this.context.hasArtifact("wcag-audit-summary.txt"));

        final String attachedJson = new String(this.context.getArtifact("wcag-audit-report.json").data(), StandardCharsets.UTF_8);
        Assertions.assertTrue(attachedJson.contains("color-contrast"));
    }

    @Test
    public void testAuditWithFailOnViolationThrowsAssertionError() throws Exception
    {
        final String axeResultsJson = """
                {
                  "violations": [
                    {
                      "id": "button-name",
                      "impact": "critical",
                      "description": "Buttons must have discernible text",
                      "help": "Buttons must have discernible text",
                      "helpUrl": "https://dequeuniversity.com/rules/axe/4.4/button-name",
                      "nodes": [
                        {
                          "html": "<button></button>",
                          "target": ["button.empty"],
                          "failureSummary": "Element does not have inner text or aria-label"
                        }
                      ]
                    }
                  ]
                }
                """;

        registerMockScriptExecutor(axeResultsJson);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("fail_on_violation", true);
        final ToolCall call = new ToolCall(UUID.randomUUID().toString(), "wcag_accessibility_audit", args);

        final AssertionError thrown = Assertions.assertThrows(AssertionError.class, () -> this.wcagTool.execute(call, this.context));
        Assertions.assertTrue(thrown.getMessage().contains("button-name"));
        Assertions.assertTrue(thrown.getMessage().toLowerCase(Locale.ROOT).contains("critical"));
    }

    @Test
    public void testAuditWithMinImpactFilter() throws Exception
    {
        final String axeResultsJson = """
                {
                  "violations": [
                    {
                      "id": "minor-hint",
                      "impact": "minor",
                      "description": "Minor hint issue",
                      "help": "Minor hint",
                      "helpUrl": "https://dequeuniversity.com",
                      "nodes": []
                    },
                    {
                      "id": "critical-issue",
                      "impact": "critical",
                      "description": "Critical blocker",
                      "help": "Critical blocker",
                      "helpUrl": "https://dequeuniversity.com",
                      "nodes": []
                    }
                  ]
                }
                """;

        registerMockScriptExecutor(axeResultsJson);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("min_impact", "serious");
        final ToolCall call = new ToolCall(UUID.randomUUID().toString(), "wcag_accessibility_audit", args);

        final ToolResult result = this.wcagTool.execute(call, this.context);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        // Minor violation filtered out; only critical included
        Assertions.assertTrue(result.content().contains("1 violations found"));
        Assertions.assertTrue(result.content().contains("critical-issue"));
        Assertions.assertFalse(result.content().contains("minor-hint"));
    }

    private void registerMockScriptExecutor(final String scriptOutput)
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_execute_script", "Executes script", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final org.neodymium.ai.tool.ToolContext ctx)
            {
                final String script = call.arguments().path("script").asText("");
                if (script.contains("typeof window.axe"))
                {
                    return ToolResult.success(call.callId(), "true");
                }
                return ToolResult.success(call.callId(), scriptOutput);
            }
        });
    }
}
