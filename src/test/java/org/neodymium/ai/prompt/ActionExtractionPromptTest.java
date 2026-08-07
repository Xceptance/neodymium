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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.selenide.ContextLevel;
import org.neodymium.ai.pipeline.DivergenceException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.ToLevelEscalationException;

/**
 * Unit tests validating user prompt message compilation, action extraction on FAILED responses,
 * and escalation level progression in ActionExtractionPrompt.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class ActionExtractionPromptTest
{
    /**
     * Default constructor.
     */
    public ActionExtractionPromptTest()
    {
    }

    /**
     * Verifies that compileUserMessage includes the current context level and next escalation target level.
     */
    @Test
    public void testCompileUserMessageIncludesContextLevel()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify free gift item");
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.RICH);

        final String userMessage = prompt.compileUserMessage(context);

        assertNotNull(userMessage);
        assertTrue(userMessage.contains("## Execution Context"));
        assertTrue(userMessage.contains("[INSTRUCTION]      Verify free gift item"));
        assertTrue(userMessage.contains("[CURRENT_LEVEL]    RICH"));
        assertTrue(userMessage.contains("[NEXT_ESCALATION]  VISUAL"));
    }

    /**
     * Verifies that parseResponse returns extracted ASSERT actions even when status is FAILED.
     */
    @Test
    public void testParseResponseFailedStatusWithActions() throws Exception
    {
        final String rawJson = """
            {
              "status": "FAILED",
              "targetContextLevel": "STANDARD",
              "reasoning": "The cart table currently only contains 'Premium Off-White shirts' and does not include any free bonus gift item.",
              "actions": [ {
                "action": "ASSERT",
                "locator": ".cart-table-wrapper",
                "value": "Free",
                "reasoning": "Verify that the cart table includes a free bonus gift item."
              } ]
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        final List<Action> actions = prompt.parseResponse(rawJson, context);

        assertNotNull(actions);
        assertEquals(1, actions.size());
        final Action action = actions.get(0);
        assertEquals("ASSERT", action.getType());
        assertEquals(".cart-table-wrapper", action.getTarget());
        assertEquals("Free", action.getValue());
    }

    /**
     * Verifies that parseResponse throws DivergenceException when status is FAILED and actions array is empty.
     */
    @Test
    public void testParseResponseFailedStatusWithoutActions()
    {
        final String rawJson = """
            {
              "status": "FAILED",
              "reasoning": "Visual element missing.",
              "actions": []
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);

        final DivergenceException ex = assertThrows(DivergenceException.class, () -> {
            prompt.parseResponse(rawJson, context);
        });

        assertTrue(ex.getMessage().contains("Visual element missing."));
    }

    /**
     * Verifies that parseResponse auto-corrects a retrograde escalation level when requested target is lower or equal.
     */
    @Test
    public void testParseResponseEscalateRetrogradeCorrection()
    {
        final String rawJson = """
            {
              "status": "ESCALATE",
              "targetContextLevel": "STANDARD",
              "reasoning": "Need higher context."
            }
            """;

        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.RICH);

        final ToLevelEscalationException ex = assertThrows(ToLevelEscalationException.class, () -> {
            prompt.parseResponse(rawJson, context);
        });

        // Current level is RICH, so requesting STANDARD should auto-correct to VISUAL
        assertEquals("VISUAL", ex.getTargetLevel());
    }

    /**
     * Verifies system prompt compilation with embedded judging enabled vs disabled.
     */
    @Test
    public void testCompileSystemMessageWithAndWithoutEmbeddedJudging()
    {
        final ActionExtractionPrompt prompt = new ActionExtractionPrompt();
        final ExecutionContext context = new ExecutionContext(null);

        // 1. Embedded judging enabled (default) -> loads action-extraction-prompt-judging.md
        System.setProperty("neodymium.ai.action.embeddedJudging.enabled", "true");
        org.neodymium.ai.config.AiConfiguration.resetInstance();
        final String judgingSystemMsg = prompt.compileSystemMessage(context);
        assertNotNull(judgingSystemMsg);
        assertTrue(judgingSystemMsg.contains("candidateLocators"), "Judging prompt must contain candidateLocators instructions.");
        assertTrue(judgingSystemMsg.contains("selfCritique"), "Judging prompt must contain selfCritique instructions.");

        // 2. Embedded judging disabled -> loads action-extraction-prompt-non-judging.md
        System.setProperty("neodymium.ai.action.embeddedJudging.enabled", "false");
        org.neodymium.ai.config.AiConfiguration.resetInstance();
        final String nonJudgingSystemMsg = prompt.compileSystemMessage(context);
        assertNotNull(nonJudgingSystemMsg);
        assertTrue(!nonJudgingSystemMsg.contains("candidateLocators"), "Non-judging prompt must not contain candidateLocators.");
        assertTrue(!nonJudgingSystemMsg.contains("selfCritique"), "Non-judging prompt must not contain selfCritique.");

        // Clean up system property
        System.clearProperty("neodymium.ai.action.embeddedJudging.enabled");
        org.neodymium.ai.config.AiConfiguration.resetInstance();
    }
}
