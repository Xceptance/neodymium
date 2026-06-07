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
package com.xceptance.neodymium.ai;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.core.EscalationDetails;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Test cases verifying agent self-healing and context level escalation logic.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class AiContextEscalationTest extends BaseAiOfflineTest
{
    /**
     * Verifies that if an action execution fails on the browser page context,
     * the AI agent self-heals by escalating the detail level of the context prompt
     * (from AXTREE to LEAN) and retrying. Asserts that the escalation details
     * (fromLevel, toLevel, exception details) are captured in the execution result
     * and that the retry count is logged as 1.
     */
    @Test
    public final void testSelfHealingAndContextEscalation()
    {
        final List<Action> executed = new ArrayList<>();
        final ActionExecutor customExecutor = new ActionExecutor(this)
        {
            private int callCount = 0;

            @Override
            public final void executeAll(final List<Action> actions)
            {
                this.callCount++;
                if (this.callCount == 1)
                {
                    throw new ActionExecutionException("Simulated click failure", null);
                }
                executed.addAll(actions);
            }
        };

        // 1. Initial LLM Response: CLICK button
        final AiMockResponse mockRes1 = AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Initial click",
                      "a": [{"t": "CLICK", "tg": "#btn"}],
                      "d": true
                    }
                    """)
                .delayMs(5L)
                .tokens(80L, 40L, 0L)
                .build();
        
        // 2. Second LLM Response (after healing/escalation): CLICK link instead
        final AiMockResponse mockRes2 = AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Retry clicking link",
                      "a": [{"t": "CLICK", "tg": "#link"}],
                      "d": true
                    }
                    """)
                .delayMs(5L)
                .tokens(100L, 50L, 10L)
                .build();

        this.llmClient.addResponse(mockRes1);
        this.llmClient.addResponse(mockRes2);

        final AiConfiguration config = Neodymium.aiConfiguration();
        try (final AiBrowser browser = new AiBrowser(config, this, this.llmClient, this.pageAnalyzer, customExecutor))
        {
            Neodymium.setAiBrowser(browser);

            final AiExecutionResult result = browser.execute("Click SUT Button");

            Assertions.assertNotNull(result);
            Assertions.assertEquals(180L, result.getInputTokens());
            Assertions.assertEquals(90L, result.getOutputTokens());
            Assertions.assertEquals(10L, result.getCachedTokens());
            
            // 1 error escalation occurred
            Assertions.assertEquals(1, result.getEscalationCount());
            Assertions.assertEquals(1, result.getEscalations().size());
            final EscalationDetails escalation = result.getEscalations().get(0);
            Assertions.assertEquals(ContextLevel.AXTREE, escalation.getFromLevel());
            Assertions.assertEquals(ContextLevel.LEAN, escalation.getToLevel());
            Assertions.assertFalse(escalation.isLlmRequested());
            
            // Actions executed successfully on attempt 2
            Assertions.assertEquals(1, executed.size());
            Assertions.assertEquals("#link", executed.get(0).getTarget());
        }
    }

    /**
     * Verifies that when the LLM returns an explicit context escalation direction
     * (e.g., status "ESCALATE" because of visual overlap issues), the agent shifts to the 
     * requested context (e.g., VISUAL) and retries the instruction. Asserts that the 
     * escalation details record this as LLM-requested along with the mock reasoning text.
     */
    @Test
    public final void testLlmRequestedContextEscalation()
    {
        // Response 1: LLM directs context escalation to VISUAL
        final AiMockResponse mockRes1 = AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "st": "ESCALATE",
                      "r": "Need screenshot for verification",
                      "tc": "VISUAL",
                      "a": [],
                      "d": false
                    }
                    """)
                .delayMs(10L)
                .tokens(60L, 30L, 0L)
                .build();

        // Response 2: Success
        final AiMockResponse mockRes2 = AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Verified visually",
                      "a": [{"t": "CLICK", "tg": "#logo"}],
                      "d": true
                    }
                    """)
                .delayMs(10L)
                .tokens(120L, 40L, 20L)
                .build();

        this.llmClient.addResponse(mockRes1);
        this.llmClient.addResponse(mockRes2);

        final AiExecutionResult result = this.mockBrowser.execute("Verify SUT visually");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getEscalationCount());
        Assertions.assertEquals(1, result.getEscalations().size());
        final EscalationDetails escalation = result.getEscalations().get(0);
        Assertions.assertEquals(ContextLevel.AXTREE, escalation.getFromLevel());
        Assertions.assertEquals(ContextLevel.VISUAL, escalation.getToLevel());
        Assertions.assertTrue(escalation.isLlmRequested());
        Assertions.assertEquals("Need screenshot for verification", escalation.getReason());
    }

    /**
     * Recipe 2: Asserting Context Level Escalations.
     * Validate that if the SUT layout shifts or elements are obstructed under STANDARD view,
     * the engine correctly escalates to VISUAL context and executes the final action.
     */
    @Test
    public final void testVisualEscalationVerification()
    {
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "st": "ESCALATE",
                      "r": "Elements overlap in layout",
                      "tc": "VISUAL",
                      "a": [],
                      "d": false
                    }
                    """)
                .build());
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#menu"}],
                      "d": true
                    }
                    """)
                .build());

        final AiExecutionResult result = this.mockBrowser.execute("Click the Menu button");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(1, result.getEscalationCount());
        Assertions.assertEquals(2, result.getLlmCalls().size());
        
        // Assert the escalation details
        Assertions.assertEquals(1, result.getEscalations().size());
        Assertions.assertTrue(result.getEscalations().get(0).isLlmRequested());
        Assertions.assertTrue(result.getEscalations().get(0).getReason().contains("Elements overlap"));
    }

    /**
     * Verifies that if the LLM returns a definitive verification failure (success=false, done=true),
     * the AI agent throws an AssertionError (specifically AiAgent.DefinitiveAssertionError)
     * and does NOT escalate context or retry.
     */
    @Test
    public final void testNoEscalationOnDefinitiveVerificationFailure()
    {
        final AiMockResponse mockRes = AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": false,
                      "d": true,
                      "a": [],
                      "e": "Visual verification failed: Expected image not present",
                      "r": "Verified that image is missing."
                    }
                    """)
                .delayMs(5L)
                .tokens(50L, 25L, 0L)
                .build();

        this.llmClient.addResponse(mockRes);

        final java.lang.AssertionError thrown = Assertions.assertThrows(java.lang.AssertionError.class, () ->
        {
            this.mockBrowser.execute("Verify visual element (visual)");
        });

        Assertions.assertTrue(thrown.getMessage().contains("Verification failed: Visual verification failed: Expected image not present"));
        
        // Assert that no context escalation occurred and only 1 LLM call was made
        final AiExecutionResult result = this.mockBrowser.getLastExecutionResult();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.getEscalationCount());
        Assertions.assertEquals(1, result.getLlmCalls().size());
    }
}
