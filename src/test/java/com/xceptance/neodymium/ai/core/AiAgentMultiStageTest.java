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


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.ai.testing.MockActionExecutor;
import com.xceptance.neodymium.ai.testing.MockLlmClient;
import com.xceptance.neodymium.ai.testing.MockPageAnalyzer;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration/unit tests for the multi-stage compound step support in {@link AiAgent}.
 * Verifies that instructions executed across multiple LLM turns with "done: false"
 * accumulate all actions into a single playbook step, and replay them cleanly in one turn.
 */
public final class AiAgentMultiStageTest
{
    private Playbook originalPlaybook;
    private String originalApiKey;

    @BeforeEach
    public void setUp()
    {
        // Clear existing thread context first to ensure fresh initialization with our system property
        Neodymium.clearThreadContext();

        // Ensure an API key is set so AiAgent initialization doesn't fail
        System.setProperty("neodymium.ai.apiKey", "mock-api-key");
        System.setProperty("neodymium.ai.pesap.linter.enabled", "false");

        originalPlaybook = Neodymium.getAiPlaybook();
        originalApiKey = Neodymium.aiConfiguration().aiApiKey();
    }

    @AfterEach
    public void tearDown()
    {
        Neodymium.setAiPlaybook(originalPlaybook);
        if (originalApiKey != null)
        {
            System.setProperty("neodymium.ai.apiKey", originalApiKey);
        }
        else
        {
            System.clearProperty("neodymium.ai.apiKey");
        }
        System.clearProperty("neodymium.ai.pesap.linter.enabled");
        Neodymium.clearThreadContext();
    }

    @Test
    public void testMultiStageCompoundStepRecordingAndReplay()
    {
        // 1. Setup a fresh Playbook in recording mode
        final Playbook playbook = new Playbook("test-multi-stage-playbook");
        playbook.setRecording(true);
        Neodymium.setAiPlaybook(playbook);

        // 2. Setup mock LLM Client that responds in two stages
        final MockLlmClient mockLlm = new MockLlmClient();
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": false,
                      "r": "Opening the dropdown first.",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#dropdown",
                          "desc": "Click the dropdown"
                        }
                      ]
                    }
                    """)
                .build());
        mockLlm.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "d": true,
                      "r": "Dropdown open. Now selecting the option.",
                      "a": [
                        {
                          "t": "CLICK",
                          "tg": "#option-item",
                          "desc": "Click the option item"
                        }
                      ]
                    }
                    """)
                .build());

        final MockPageAnalyzer mockAnalyzer = new MockPageAnalyzer("<html><body>Mock DOM Context</body></html>");
        final MockActionExecutor mockExecutor = new MockActionExecutor();

        final AiAgent agent = new AiAgent(mockLlm, mockAnalyzer, mockExecutor, Neodymium.aiConfiguration());

        // 3. Execute a compound multi-stage instruction
        agent.execute("Click the dropdown and select the option item");

        // 4. Verify recording: the playbook should have exactly 1 step
        assertEquals(1, playbook.getSteps().size());
        final PlaybookStep recordedStep = playbook.getSteps().get(0);
        
        // The single recorded step should have accumulated BOTH actions!
        assertEquals(2, recordedStep.getActions().size());
        
        final Action firstAction = recordedStep.getActions().get(0);
        assertEquals("CLICK", firstAction.getType());
        assertEquals("#dropdown", firstAction.getTarget());

        final Action secondAction = recordedStep.getActions().get(1);
        assertEquals("CLICK", secondAction.getType());
        assertEquals("#option-item", secondAction.getTarget());

        // Verify both actions were executed during recording
        assertEquals(2, mockExecutor.getExecutedActions().size());
        assertEquals("#dropdown", mockExecutor.getExecutedActions().get(0).getTarget());
        assertEquals("#option-item", mockExecutor.getExecutedActions().get(1).getTarget());

        // 5. Test Replay Mode: set playbook recording to false, reset execution counts
        playbook.setRecording(false);
        playbook.setCursor(0);
        mockExecutor.getExecutedActions().clear();
        mockLlm.getAiStats().reset();

        // Execute the same instruction under replay mode
        agent.execute("Click the dropdown and select the option item");

        // LLM should NOT have been called during replay
        assertEquals(0, mockLlm.getAiStats().getCallCount());

        // Both recorded actions should have been executed in one go
        assertEquals(2, mockExecutor.getExecutedActions().size());
        assertEquals("#dropdown", mockExecutor.getExecutedActions().get(0).getTarget());
        assertEquals("#option-item", mockExecutor.getExecutedActions().get(1).getTarget());
    }
}
