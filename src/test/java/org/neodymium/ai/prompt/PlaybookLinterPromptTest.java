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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.playbook.linter.LinterCategory;
import org.neodymium.ai.playbook.linter.LinterSeverity;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;

/**
 * Unit tests for {@link PlaybookLinterPrompt}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookLinterPromptTest
{
    @Test
    @DisplayName("Verify prompt compilation with scenario description and steps")
    public void testPromptCompilationWithDescription()
    {
        final PlaybookStep s1 = new PlaybookStep("Open the country selector and click \"${country}\".");
        s1.setLineNumber(12);
        s1.setSourceFile("checkout.yaml");

        final PlaybookStep s2 = new PlaybookStep("Auf der rechten Seite ist ein grünes Symbol.");
        s2.setLineNumber(15);
        s2.setSourceFile("checkout.yaml");

        final PlaybookLinterPrompt prompt = new PlaybookLinterPrompt("Guest Checkout Flow", List.of(s1, s2), null);

        final String userMsg = prompt.compileUserMessage(null);
        assertTrue(userMsg.contains("## Scenario Context"));
        assertTrue(userMsg.contains("Goal: Guest Checkout Flow"));
        assertTrue(userMsg.contains("1. Open the country selector and click \"${country}\"."));
        assertTrue(userMsg.contains("2. Auf der rechten Seite ist ein grünes Symbol."));
        assertEquals(ResponseSchema.LINTER, prompt.getResponseSchema());
    }

    @Test
    @DisplayName("Verify prompt compilation without description")
    public void testPromptCompilationWithoutDescription()
    {
        final PlaybookStep s1 = new PlaybookStep("Click Login");
        final PlaybookLinterPrompt prompt = new PlaybookLinterPrompt(null, List.of(s1), null);

        final String userMsg = prompt.compileUserMessage(null);
        assertFalse(userMsg.contains("## Scenario Context"));
        assertTrue(userMsg.contains("1. Click Login"));
    }

    @Test
    @DisplayName("Verify parsing multilingual JSON response across categories")
    public void testParseMultilingualJsonResponse() throws Exception
    {
        final PlaybookStep s1 = new PlaybookStep("Open the country selector and click \"${country}\".");
        s1.setLineNumber(10);
        s1.setSourceFile("test.yaml");

        final PlaybookStep s2 = new PlaybookStep("Auf der linken Seite ist ein Menü das Abmelden ermöglicht.");
        s2.setLineNumber(14);
        s2.setSourceFile("test.yaml");

        final PlaybookStep s3 = new PlaybookStep("ボタンをクリックして確認する");
        s3.setLineNumber(20);
        s3.setSourceFile("test.yaml");

        final PlaybookLinterPrompt prompt = new PlaybookLinterPrompt("Multilingual Scenario", List.of(s1, s2, s3), null);

        final String jsonResponse = """
            ```json
            {
              "findings": [
                {
                  "stepIndex": 1,
                  "category": "STEP_SPLITTING_CANDIDATE",
                  "severity": "WARNING",
                  "message": "Instruction combines two interactive actions.",
                  "suggestedRewrite": "1. Open the country selector\\n2. Click \\"${country}\\"",
                  "scope": null
                },
                {
                  "stepIndex": 2,
                  "category": "AMBIGUOUS_AFFORDANCE",
                  "severity": "INFO",
                  "message": "Passive capability phrasing instead of explicit imperative action.",
                  "suggestedRewrite": "Klicke auf Abmelden im Menü auf der linken Seite",
                  "scope": null
                },
                {
                  "stepIndex": 3,
                  "category": "STEP_SPLITTING_CANDIDATE",
                  "severity": "WARNING",
                  "message": "Action and verification combined.",
                  "suggestedRewrite": "1. ボタンをクリックする\\n2. 確認メッセージが表示されることを確認する",
                  "scope": null
                }
              ]
            }
            ```
            """;

        final List<PlaybookLinterFinding> findings = prompt.parseResponse(jsonResponse, null);
        assertEquals(3, findings.size());

        final PlaybookLinterFinding f1 = findings.get(0);
        assertEquals(1, f1.stepIndex());
        assertEquals(10, f1.lineNumber());
        assertEquals("test.yaml", f1.sourceFile());
        assertEquals(LinterCategory.STEP_SPLITTING_CANDIDATE, f1.category());
        assertEquals(LinterSeverity.WARNING, f1.severity());
        assertTrue(f1.suggestedRewrite().contains("${country}"));

        final PlaybookLinterFinding f2 = findings.get(1);
        assertEquals(2, f2.stepIndex());
        assertEquals(14, f2.lineNumber());
        assertEquals(LinterCategory.AMBIGUOUS_AFFORDANCE, f2.category());
        assertEquals(LinterSeverity.INFO, f2.severity());
        assertTrue(f2.suggestedRewrite().contains("Klicke auf Abmelden"));

        final PlaybookLinterFinding f3 = findings.get(2);
        assertEquals(3, f3.stepIndex());
        assertEquals(20, f3.lineNumber());
        assertEquals(LinterCategory.STEP_SPLITTING_CANDIDATE, f3.category());
        assertTrue(f3.suggestedRewrite().contains("ボタンをクリックする"));
    }

    @Test
    @DisplayName("Verify parsing invalid or empty response returns empty list")
    public void testParseInvalidOrEmptyResponse() throws Exception
    {
        final PlaybookLinterPrompt prompt = new PlaybookLinterPrompt("Empty", List.of(), null);
        assertTrue(prompt.parseResponse("", null).isEmpty());
        assertTrue(prompt.parseResponse("Invalid content", null).isEmpty());
        assertTrue(prompt.parseResponse("{\"findings\": []}", null).isEmpty());
    }
}
