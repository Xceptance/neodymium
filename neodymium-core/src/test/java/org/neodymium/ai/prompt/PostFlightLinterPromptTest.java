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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.playbook.linter.LinterCategory;
import org.neodymium.ai.playbook.linter.LinterSeverity;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;
import org.neodymium.ai.prompt.PostFlightLinterPrompt.StepTelemetryInfo;

/**
 * Unit tests for {@link PostFlightLinterPrompt}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class PostFlightLinterPromptTest
{
    @Test
    @DisplayName("Verify PostFlightLinterPrompt compiles system message and response schema")
    public void testSystemMessageAndResponseSchema()
    {
        final PostFlightLinterPrompt prompt = new PostFlightLinterPrompt("Checkout Scenario", List.of());
        assertEquals(ResponseSchema.LINTER, prompt.getResponseSchema());

        final String sysMsg = prompt.compileSystemMessage(null);
        assertNotNull(sysMsg);
        assertTrue(sysMsg.contains("Empirical Post-Flight Playbook Linter"));
        assertTrue(sysMsg.contains("EMPIRICAL_MULTI_ACTION"));
    }

    @Test
    @DisplayName("Verify user message includes scenario context and detailed step telemetry")
    public void testCompileUserMessageWithFrictionSteps()
    {
        final StepTelemetryInfo s1 = new StepTelemetryInfo(
            1,
            15,
            "playbooks/checkout.yaml",
            "Hover over the mini cart and click '${action}'",
            "Hover over the mini cart and click 'Checkout'",
            "PASSED",
            2,
            1250,
            List.of("HOVER on '.mini-cart'", "CLICK on '#checkout-btn'"),
            "View Cart & Checkout",
            LinterCategory.EMPIRICAL_MULTI_ACTION,
            "Step executed 2 mutating actions in a single flat step."
        );

        final PostFlightLinterPrompt prompt = new PostFlightLinterPrompt("Guest Checkout Flow", List.of(s1));
        final String userMsg = prompt.compileUserMessage(null);

        assertTrue(userMsg.contains("Guest Checkout Flow"));
        assertTrue(userMsg.contains("Step #1 (line 15)"));
        assertTrue(userMsg.contains("Hover over the mini cart and click '${action}'"));
        assertTrue(userMsg.contains("Hover over the mini cart and click 'Checkout'"));
        assertTrue(userMsg.contains("Agent Turns:** 2"));
        assertTrue(userMsg.contains("Duration:** 1250 ms"));
        assertTrue(userMsg.contains("EMPIRICAL_MULTI_ACTION"));
        assertTrue(userMsg.contains("View Cart & Checkout"));
        assertTrue(userMsg.contains("HOVER on '.mini-cart'"));
    }

    @Test
    @DisplayName("Verify parseResponse correctly maps structured JSON into PlaybookLinterFinding list")
    public void testParseResponseValidJson()
    {
        final StepTelemetryInfo s1 = new StepTelemetryInfo(
            1,
            20,
            "playbooks/cart.yaml",
            "Click 'Cart'",
            "Click 'Cart'",
            "PASSED",
            1,
            800,
            List.of("CLICK on '#cart'"),
            "Shopping Bag (0)",
            LinterCategory.LABEL_DIVERGENCE,
            "Instruction references 'Cart' but DOM text is 'Shopping Bag (0)'"
        );

        final PostFlightLinterPrompt prompt = new PostFlightLinterPrompt(null, List.of(s1));

        final String json = """
            {
              "findings": [
                {
                  "stepIndex": 1,
                  "category": "LABEL_DIVERGENCE",
                  "severity": "INFO",
                  "message": "Instruction references 'Cart', but clicked element has DOM text 'Shopping Bag (0)'.",
                  "suggestedRewrite": "Click 'Shopping Bag (0)'",
                  "scope": null
                }
              ]
            }
            """;

        final List<PlaybookLinterFinding> findings = prompt.parseResponse(json, null);
        assertEquals(1, findings.size());

        final PlaybookLinterFinding f = findings.get(0);
        assertEquals(1, f.stepIndex());
        assertEquals(20, f.lineNumber());
        assertEquals("playbooks/cart.yaml", f.sourceFile());
        assertEquals("Click 'Cart'", f.rawInstruction());
        assertEquals(LinterCategory.LABEL_DIVERGENCE, f.category());
        assertEquals(LinterSeverity.INFO, f.severity());
        assertEquals("Instruction references 'Cart', but clicked element has DOM text 'Shopping Bag (0)'.", f.message());
        assertEquals("Click 'Shopping Bag (0)'", f.suggestedRewrite());
    }

    @Test
    @DisplayName("Verify parseResponse handles malformed or markdown-wrapped JSON gracefully")
    public void testParseResponseMarkdownWrappedAndMalformed()
    {
        final PostFlightLinterPrompt prompt = new PostFlightLinterPrompt(null, List.of());

        final String wrappedJson = """
            ```json
            {
              "findings": []
            }
            ```
            """;
        assertTrue(prompt.parseResponse(wrappedJson, null).isEmpty());

        final String invalid = "NOT_JSON_AT_ALL";
        assertTrue(prompt.parseResponse(invalid, null).isEmpty());
        assertTrue(prompt.parseResponse("", null).isEmpty());
        assertTrue(prompt.parseResponse(null, null).isEmpty());
    }
}
