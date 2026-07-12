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
package org.neodymium.ai.integration;

import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
import org.neodymium.ai.pipeline.structural.SequenceStep;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.prompt.AiPrompt;
import java.util.List;
/**
 * End-to-end integration test validating the new AI pipeline (org.neodymium.ai)
 * with a mock LLM and a real Selenide browser hitting the Aura Sandbox.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("integration")
public class AuraE2EMockIntegrationTest extends BaseAiTest
{
    private String url;

    /**
     * Mock LLM Provider that records the last request and returns a canned response.
     */
    private static final class TestLlmProvider implements LlmProvider
    {
        private LlmRequest lastRequest;
        private String responseContent = "SUCCESS";

        public void setResponseContent(final String content)
        {
            this.responseContent = content;
        }

        public LlmRequest getLastRequest()
        {
            return lastRequest;
        }

        @Override
        public LlmResponse chat(final LlmRequest request)
        {
            this.lastRequest = request;
            return new LlmResponse(responseContent, new TokenUsage(10, 20, 30), "mock-model");
        }

        @Override
        public Set<LlmCapability> getCapabilities()
        {
            return EnumSet.allOf(LlmCapability.class);
        }
    }

    /**
     * Mock AI Prompt implementation compiling and parsing list of actions from the mock LLM JSON.
     */
    private static final class MockActionsPrompt implements AiPrompt<List<Action>>
    {
        @Override
        public org.neodymium.ai.client.ResponseSchema getResponseSchema()
        {
            return org.neodymium.ai.client.ResponseSchema.ACTIONS;
        }

        @Override
        public String compileSystemMessage(final ExecutionContext context)
        {
            return "system";
        }

        @Override
        public String compileUserMessage(final ExecutionContext context)
        {
            final org.neodymium.ai.executor.SutState state = (org.neodymium.ai.executor.SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
            return "user: " + context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION)
                + (state != null ? "\n\n" + state.getTextContent() : "");
        }

        @Override
        public List<Action> parseResponse(final String rawContent, final ExecutionContext context) throws Exception
        {
            final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            final com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(rawContent);
            final List<Action> actions = new java.util.ArrayList<>();
            for (final com.fasterxml.jackson.databind.JsonNode node : root.path("actions"))
            {
                actions.add(new Action(
                    node.path("action").asText(),
                    node.path("locator").asText(),
                    java.util.Collections.singletonList(node.path("value").asText()),
                    "mocking action",
                    node.path("reasoning").asText()
                ));
            }
            return actions;
        }
    }

    @BeforeEach
    public void setupUrl()
    {
        // Target the floating-labels.html sandbox page served by EmbeddedHtmlServer
        this.url = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/floating-labels.html", server.getPort());
    }

    @NeodymiumTest
    public void testMockE2EFloatingLabels() throws Exception
    {
        // 1. Open the page
        Selenide.open(this.url);
        
        // 2. Setup the test environment
        final SessionData sessionData = new SessionData(new HashMap<>());
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final SelenideTargetExecutor executor = new SelenideTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        // 3. Program the Mock LLM to return an ActionDefinition for typing into the username field
        // The sandbox page has `<input type="text" id="username" class="form-control" placeholder=" ">`
        final String cannedJsonResponse = """
            {
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "#username-input",
                  "value": "testuser",
                  "reasoning": "Mocking the typing action"
                }
              ]
            }
            """;
        provider.setResponseContent(cannedJsonResponse);

        // 4. Create the AiSession and load the playbook
        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        // 5. Build the pipeline manually since we are testing the core runner
        // Pushing in LIFO order (last pushed, first executed)
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Type 'testuser' into the username field");
        
        context.pushStep(new ExecuteActionsStep());
        context.pushStep(new org.neodymium.ai.pipeline.steps.CallLlmStep<>(new MockActionsPrompt(), LlmCapability.TEXT_ONLY));
        context.pushStep(new org.neodymium.ai.pipeline.steps.CaptureStateStep());

        // 6. Run the StateMachineRunner
        final StateMachineRunner runner = new StateMachineRunner(session);
        runner.run();

        // 7. Verification: LLM Prompt Check
        assertNotNull(provider.getLastRequest(), "The mock LLM should have been called");
        final String sentMessage = provider.getLastRequest().userMessage();
        assertTrue(sentMessage.contains("testuser"), "The prompt should contain the user intent to type 'testuser'");
        assertTrue(sentMessage.contains("id=\"username-input\""), "The prompt should contain the DOM snippet of the page");

        // 8. Verification: Real DOM Manipulation
        $("#username-input").shouldHave(value("testuser"));
    }
}
