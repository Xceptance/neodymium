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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.GeminiLlmProvider;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.steps.CaptureStateStep;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
import org.neodymium.ai.pipeline.steps.CallLlmStep;
import org.neodymium.ai.prompt.ActionExtractionPrompt;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.ai.session.AiSession;

/**
 * End-to-end integration test validating the AI pipeline (org.neodymium.ai)
 * with a live Gemini LLM API and a real Selenide browser hitting the Aura Sandbox.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumTest
public class AuraE2ELiveIntegrationTest extends BaseAiTest
{
    private String url;

    private String getApiKey()
    {
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isBlank())
        {
            key = System.getProperty("neodymium.ai.apiKey");
        }
        return key;
    }

    @BeforeEach
    public void setupTestEnvironment()
    {
        final String key = getApiKey();
        if (key == null || key.isBlank())
        {
            throw new IllegalStateException("GEMINI_API_KEY environment variable or neodymium.ai.apiKey property is missing. This test requires a valid API key to run. No silent fallbacks allowed.");
        }

        System.setProperty("neodymium.ai.apiKey", key);
        
        // Target the floating-labels.html sandbox page served by EmbeddedHtmlServer
        this.url = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/floating-labels.html", server.getPort());
        
        // Open the Aura sandbox page via Selenide
        Selenide.open(this.url);
    }

    @org.junit.jupiter.api.Test
    public void testLiveE2EFloatingLabels() throws Exception
    {
        // 1. Setup the test environment
        final org.neodymium.ai.model.SessionData sessionData = new org.neodymium.ai.model.SessionData(new HashMap<>());
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final SelenideTargetExecutor executor = new SelenideTargetExecutor();
        final LlmRegistry registry = new LlmRegistry();
        
        final GeminiLlmProvider provider = new GeminiLlmProvider();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        // 2. Create the AiSession and load the playbook
        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        // 3. Build the pipeline stack (LIFO order execution)
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Type 'testuser' into the username field");
        
        context.pushStep(new ExecuteActionsStep());
        context.pushStep(new CallLlmStep<>(new ActionExtractionPrompt(), LlmCapability.TEXT_ONLY));
        context.pushStep(new CaptureStateStep());

        // 4. Run the pipeline
        final StateMachineRunner runner = new StateMachineRunner(session);
        runner.run();

        // 5. Verification: Real DOM Manipulation
        $("#username-input").shouldHave(value("testuser"));
    }
}
