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
package com.xceptance.neodymium.ai.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.BaseAiOfflineTest;
import com.xceptance.neodymium.ai.core.AiAgent.DefinitiveAssertionError;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Offline integration test verifying that a failure inside an included step block
 * does not trigger a restart of the parent include block/step from the beginning.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class AiAgentIncludeFailureTest extends BaseAiOfflineTest
{
    @BeforeEach
    public final void setupTestConfig()
    {
        // 1. Manually configure the classpathResourcePath for include resolution
        Neodymium.getData().put("neodymium.classpathResourcePath", "com/xceptance/neodymium/ai/integration/IncludeTest.yaml");
        // Limit max retries to 1 for faster test execution
        System.setProperty("neodymium.ai.agent.maxRetries", "1");
    }

    @Test
    public final void testNoParentRestartOnIncludeFailure()
    {
        // 1. Setup playbook in recording mode
        final Playbook playbook = new Playbook("test-no-parent-restart-playbook");
        playbook.setRecording(true);
        Neodymium.setAiPlaybook(playbook);

        // 2. Setup mock LLM responses:
        // Response 1: Parent step "Include the steps file" -> returns INCLUDE action
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Including steps",
                      "a": [
                        {
                          "t": "INCLUDE",
                          "tg": "fragments/testFailureAndRestart.steps"
                        }
                      ]
                    }
                    """)
                .build());

        // Response 2: First step inside include "Click button A" -> returns CLICK #btnA
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Clicking button A",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#btnA"
                        }
                      ]
                    }
                    """)
                .build());

        // Response 3: Second step inside include "Click non-existent button" -> returns CLICK #non-existent
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Clicking non-existent button",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#non-existent"
                        }
                      ]
                    }
                    """)
                .build());

        // Response 4: If the parent step mistakenly restarts, it will call the LLM again for the parent step.
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Including steps again",
                      "a": [
                        {
                          "t": "INCLUDE",
                          "tg": "fragments/testFailureAndRestart.steps"
                        }
                      ]
                    }
                    """)
                .build());

        // 3. Configure mock action executor to throw DefinitiveAssertionError for `#non-existent`
        // so that it fails immediately without retrying or escalating the inner step.
        actionExecutor.setBeforeExecuteHook(action ->
        {
            if (action != null && "#non-existent".equals(action.getTarget()))
            {
                throw new DefinitiveAssertionError("Simulated element not found: " + action.getTarget());
            }
        });

        // 4. Run the AI agent execution with a dummy parent step and expect an AssertionError
        final String steps = "Include the steps file";
        
        assertThrows(AssertionError.class, () ->
        {
            mockBrowser.execute(steps);
        });

        // 5. Inspect the execution result steps and verify that exactly 3 LLM calls were made.
        // If the parent step retried, it would have consumed Response 4 (resulting in 4 or more LLM calls).
        final AiExecutionResult result = Neodymium.getLastAiExecutionResult();
        assertEquals(3, result.getLlmCalls().size(), "Parent step retried/restarted the include block after nested step failure!");
    }

    @Test
    public final void testNoParentRestartOnNestedIncludeFailure()
    {
        // 1. Setup playbook in recording mode
        final Playbook playbook = new Playbook("test-nested-include-failure-playbook");
        playbook.setRecording(true);
        Neodymium.setAiPlaybook(playbook);

        // 2. Setup mock LLM responses:
        // Response 1: Parent step "Include nestedFirst" -> returns INCLUDE action for nestedFirst.steps
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Including nestedFirst",
                      "a": [
                        {
                          "t": "INCLUDE",
                          "tg": "fragments/nestedFirst.steps"
                        }
                      ]
                    }
                    """)
                .build());

        // Response 2: Step 1 in nestedFirst "Click button A" -> returns CLICK #btnA
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Clicking button A",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#btnA"
                        }
                      ]
                    }
                    """)
                .build());

        // Response 3: Step 2 in nestedFirst "Include nestedSecond" -> returns INCLUDE action for nestedSecond.steps
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Including nestedSecond",
                      "a": [
                        {
                          "t": "INCLUDE",
                          "tg": "fragments/nestedSecond.steps"
                        }
                      ]
                    }
                    """)
                .build());

        // Response 4: Step 1 in nestedSecond "Click button B" -> returns CLICK #btnB
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Clicking button B",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#btnB"
                        }
                      ]
                    }
                    """)
                .build());

        // Response 5: Step 2 in nestedSecond "Click non-existent button" -> returns CLICK #non-existent
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Clicking non-existent button",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#non-existent"
                        }
                      ]
                    }
                    """)
                .build());

        // Response 6: If any parent include step mistakenly restarts, it will make a call to the LLM.
        llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Mistaken restart of include",
                      "a": [
                        {
                          "t": "INCLUDE",
                          "tg": "fragments/nestedFirst.steps"
                        }
                      ]
                    }
                    """)
                .build());

        // 3. Configure mock action executor to throw DefinitiveAssertionError for `#non-existent`
        // so that it fails immediately without retrying or escalating the inner step.
        actionExecutor.setBeforeExecuteHook(action ->
        {
            if (action != null && "#non-existent".equals(action.getTarget()))
            {
                throw new DefinitiveAssertionError("Simulated element not found: " + action.getTarget());
            }
        });

        // 4. Run the AI agent execution with a dummy parent step and expect an AssertionError
        final String steps = "Include nestedFirst";
        
        assertThrows(AssertionError.class, () ->
        {
            mockBrowser.execute(steps);
        });

        // 5. Inspect the execution result steps and verify that exactly 5 LLM calls were made.
        // If any parent include block retried, it would have consumed Response 6 (resulting in 6 or more LLM calls).
        final AiExecutionResult result = Neodymium.getLastAiExecutionResult();
        assertEquals(5, result.getLlmCalls().size(), "A parent include step retried/restarted the include block after a nested step failure!");
    }
}
