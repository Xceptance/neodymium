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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.prompt.QualityJudgePrompt.QualityJudgeResult;

/**
 * Unit tests for {@link QualityJudgePrompt}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class QualityJudgePromptTest
{
    @Test
    public void testCompileRequest()
    {
        final QualityJudgePrompt prompt = new QualityJudgePrompt();
        final Action action = new Action("CLICK", "#btn-submit", List.of(), "Click submit", "Submit form");
        action.setCandidateLocators(List.of(
                new LocatorCandidate("#btn-submit", "ID", 0.95, "Primary ID"),
                new LocatorCandidate("button.btn-primary", "CLASS", 0.85, "Class fallback")
        ));

        final LlmRequest req = prompt.compileRequest("Click the submit button", "=== DOM Context ===", action, AiConfiguration.getInstance());
        assertNotNull(req);
        assertTrue(req.userMessage().contains("Click the submit button"));
        assertTrue(req.userMessage().contains("#btn-submit"));
        assertTrue(req.userMessage().contains("button.btn-primary"));
    }

    @Test
    public void testParseResponse()
    {
        final QualityJudgePrompt prompt = new QualityJudgePrompt();
        final String json = """
                ```json
                {
                  "judgment": "REFINED",
                  "chosenLocator": "button.btn-primary",
                  "isRegex": false,
                  "confidence": 0.98,
                  "reasoning": "Class fallback is cleaner than dynamic ID"
                }
                ```
                """;

        final QualityJudgeResult result = prompt.parseResponse(json);
        assertEquals("REFINED", result.getJudgment());
        assertEquals("button.btn-primary", result.getChosenLocator());
        assertFalse(result.isRegex());
        assertEquals(0.98, result.getConfidence(), 0.001);
        assertEquals("Class fallback is cleaner than dynamic ID", result.getReasoning());
    }
}
