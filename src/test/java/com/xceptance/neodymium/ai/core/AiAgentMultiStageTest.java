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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
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
        // Disable PESAP explicitly so it doesn't consume our step execution mock LLM responses
        System.setProperty("neodymium.ai.pesap.enabled", "false");

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
        System.clearProperty("neodymium.ai.pesap.enabled");
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
        final List<String> llmResponses = new ArrayList<>();
        llmResponses.add("{\n" +
            "  \"s\": true,\n" +
            "  \"d\": false,\n" +
            "  \"r\": \"Opening the dropdown first.\",\n" +
            "  \"a\": [\n" +
            "    {\n" +
            "      \"t\": \"CLICK\",\n" +
            "      \"tg\": \"#dropdown\",\n" +
            "      \"d\": \"Click the dropdown\"\n" +
            "    }\n" +
            "  ]\n" +
            "}");
        llmResponses.add("{\n" +
            "  \"s\": true,\n" +
            "  \"d\": true,\n" +
            "  \"r\": \"Dropdown open. Now selecting the option.\",\n" +
            "  \"a\": [\n" +
            "    {\n" +
            "      \"t\": \"CLICK\",\n" +
            "      \"tg\": \"#option-item\",\n" +
            "      \"d\": \"Click the option item\"\n" +
            "    }\n" +
            "  ]\n" +
            "}");

        final MockLlmClient mockLlm = new MockLlmClient(llmResponses);
        final MockPageAnalyzer mockAnalyzer = new MockPageAnalyzer();
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
        mockLlm.resetCallCount();

        // Execute the same instruction under replay mode
        agent.execute("Click the dropdown and select the option item");

        // LLM should NOT have been called during replay
        assertEquals(0, mockLlm.getCallCount());

        // Both recorded actions should have been executed in one go
        assertEquals(2, mockExecutor.getExecutedActions().size());
        assertEquals("#dropdown", mockExecutor.getExecutedActions().get(0).getTarget());
        assertEquals("#option-item", mockExecutor.getExecutedActions().get(1).getTarget());
    }

    /**
     * Mock LLM Client that returns predefined mock responses in sequence.
     */
    private static final class MockLlmClient extends LlmClient
    {
        private final List<String> responses;
        private int callIndex = 0;

        public MockLlmClient(final List<String> responses)
        {
            super(Neodymium.aiConfiguration(), new AiStats());
            this.responses = responses;
        }

        @Override
        public String chat(final String systemPrompt, final String userPrompt)
        {
            if (callIndex >= responses.size())
            {
                throw new IllegalStateException("Mock LlmClient: unexpected call to chat");
            }
            return responses.get(callIndex++);
        }

        @Override
        public String chatWithScreenshot(final String systemPrompt, final String userPrompt, final String screenshot)
        {
            return chat(systemPrompt, userPrompt);
        }

        public int getCallCount()
        {
            return callIndex;
        }

        public void resetCallCount()
        {
            callIndex = 0;
        }
    }

    /**
     * Mock PageAnalyzer to avoid browser automation calls.
     */
    private static final class MockPageAnalyzer extends PageAnalyzer
    {
        @Override
        public String getPageContext(final ContextLevel level)
        {
            return "<html><body>Mock DOM Context</body></html>";
        }

        @Override
        public String captureScreenshot(final String title)
        {
            return "mock-screenshot-base64";
        }
    }

    /**
     * Mock ActionExecutor that stores and tracks executed actions instead of running actual Selenide interactions.
     */
    private static final class MockActionExecutor extends ActionExecutor
    {
        private final List<Action> executedActions = new ArrayList<>();

        public MockActionExecutor()
        {
            super(new Object());
        }

        @Override
        public void executeAll(final List<Action> actions)
        {
            if (actions != null)
            {
                executedActions.addAll(actions);
            }
        }

        public List<Action> getExecutedActions()
        {
            return executedActions;
        }
    }
}
