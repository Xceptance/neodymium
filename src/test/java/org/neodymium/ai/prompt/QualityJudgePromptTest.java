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
import org.neodymium.ai.executor.rest.RestTargetExecutor;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.pipeline.ExecutionContext;
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
        assertTrue(req.userMessage().contains("Candidate 1: locator='#btn-submit', strategy='ID', score=0.95, reasoning='Primary ID'"));
        assertTrue(req.userMessage().contains("Candidate 2: locator='button.btn-primary', strategy='CLASS', score=0.85, reasoning='Class fallback'"));
    }

    @Test
    public void testCompileRequestCandidateLocatorsWithoutReasoning()
    {
        final QualityJudgePrompt prompt = new QualityJudgePrompt();
        final Action action = new Action("CLICK", ".cart-sidebar form button[type='submit']", List.of(), "Submit order", "Submit cart");
        action.setCandidateLocators(List.of(
                new LocatorCandidate(".cart-sidebar form button[type='submit']", "CLASS_ATTRIBUTE", 0.95, ""),
                new LocatorCandidate("#couponCode + button", "CSS_SELECTOR", 0.85, "   "),
                new LocatorCandidate("[data-ai='xcbnt56b']", "AUTOMATION_ID", 0.70, null)
        ));

        final LlmRequest req = prompt.compileRequest("Submit order", "=== DOM Context ===", action, AiConfiguration.getInstance());
        assertNotNull(req);
        final String userMsg = req.userMessage();
        assertTrue(userMsg.contains("Candidate 1: locator='.cart-sidebar form button[type='submit']', strategy='CLASS_ATTRIBUTE', score=0.95\n"));
        assertTrue(userMsg.contains("Candidate 2: locator='#couponCode + button', strategy='CSS_SELECTOR', score=0.85\n"));
        assertTrue(userMsg.contains("Candidate 3: locator='[data-ai='xcbnt56b']', strategy='AUTOMATION_ID', score=0.70\n"));
        assertFalse(userMsg.contains("reasoning=''"), "User prompt must not contain empty reasoning=''.");
        assertFalse(userMsg.contains("reasoning="), "User prompt must not contain reasoning attribute when candidates have no reasoning.");
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

    @Test
    public void testCompileRequestSelenideModeLocatorRule()
    {
        final QualityJudgePrompt prompt = new QualityJudgePrompt();
        final Action action = new Action("CLICK", "#btn-submit", List.of(), "Click submit", "Submit form");

        try
        {
            // 1. Selenide mode context -> includes Selenide locator rule
            final ExecutionContext selenideContext = new ExecutionContext(null);
            selenideContext.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, new SelenideTargetExecutor());
            ExecutionContext.setActiveContext(selenideContext);

            final LlmRequest selenideReq = prompt.compileRequest("Click the submit button", "=== DOM Context ===", action, AiConfiguration.getInstance());
            assertNotNull(selenideReq);
            assertTrue(selenideReq.systemMessage().contains("## Selenide/Selenium Engine Locators"), "Judge system prompt must include Selenide engine locator rule in Selenide mode.");
            assertTrue(selenideReq.systemMessage().contains("FORBIDDEN: Playwright pseudo-selectors"), "Judge system prompt must forbid Playwright pseudo-selectors.");

            // 2. REST mode context -> excludes Selenide locator rule
            final ExecutionContext restContext = new ExecutionContext(null);
            restContext.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, new RestTargetExecutor());
            ExecutionContext.setActiveContext(restContext);

            final LlmRequest restReq = prompt.compileRequest("Click the submit button", "=== DOM Context ===", action, AiConfiguration.getInstance());
            assertNotNull(restReq);
            assertFalse(restReq.systemMessage().contains("## Selenide/Selenium Engine Locators"), "Judge system prompt must not include Selenide engine locator rule in REST mode.");
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }
}
