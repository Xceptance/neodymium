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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.AiTestRunResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.core.EscalationDetails;
import com.xceptance.neodymium.ai.core.LlmCallDetails;
import com.xceptance.neodymium.ai.core.LookupDetails;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.ai.testing.MockLlmClient;
import com.xceptance.neodymium.ai.testing.MockPageAnalyzer;
import com.xceptance.neodymium.common.testdata.TestData;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Rich offline integration tests validating the creation, populating, and caching
 * of the new AiExecutionResult and AiTestRunResult metrics and details.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class AiExecutionResultDemoTest
{
    private Map<String, String> originalSystemProperties;

    @BeforeEach
    public final void setup()
    {
        this.originalSystemProperties = new HashMap<>();
        this.storeAndClearSystemProperty("neodymium.ai.apiKey");
        this.storeAndClearSystemProperty("neodymium.ai.pesap.enabled");
        this.storeAndClearSystemProperty("neodymium.ai.agent.maxRetries");

        MockLlmClient.configureForOffline("mock-offline-demo-key", false);

        Neodymium.setAiPlaybook(null);
        Neodymium.getData().clear();
    }

    private void storeAndClearSystemProperty(final String key)
    {
        final String val = System.getProperty(key);
        if (val != null)
        {
            this.originalSystemProperties.put(key, val);
            System.clearProperty(key);
        }
    }

    @AfterEach
    public final void teardown()
    {
        Neodymium.getData().clear();
        Neodymium.setAiPlaybook(null);
        Neodymium.setAiBrowser(null);

        System.clearProperty("neodymium.ai.apiKey");
        System.clearProperty("neodymium.ai.pesap.enabled");
        System.clearProperty("neodymium.ai.agent.maxRetries");

        for (final Map.Entry<String, String> entry : this.originalSystemProperties.entrySet())
        {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public final void testTemplateResolutionAndLookups()
    {
        final TestData data = Neodymium.getData();
        data.put("username", "demoUser123");
        data.put("password", "demoPass456");
        data.put("nested", "${username}_secure");

        final List<LookupDetails> lookupsCollector = new ArrayList<>();
        final String resolved = AiBrowser.resolveTestDataToPrompt("Login using ${username} and ${nested}", lookupsCollector);

        Assertions.assertEquals("Login using demoUser123 and demoUser123_secure", resolved);
        Assertions.assertEquals(3, lookupsCollector.size());

        final LookupDetails userLookup = lookupsCollector.get(0);
        Assertions.assertEquals("username", userLookup.getKey());
        Assertions.assertEquals("demoUser123", userLookup.getResolvedValue());
        Assertions.assertEquals("TestData Map", userLookup.getSource());
        Assertions.assertFalse(userLookup.isLocalized());

        final LookupDetails nestedLookup = lookupsCollector.get(1);
        Assertions.assertEquals("username", nestedLookup.getKey());
        Assertions.assertEquals("demoUser123", nestedLookup.getResolvedValue());
        Assertions.assertEquals("TestData Map", nestedLookup.getSource());

        final LookupDetails fullNestedLookup = lookupsCollector.get(2);
        Assertions.assertEquals("nested", fullNestedLookup.getKey());
        Assertions.assertEquals("demoUser123_secure", fullNestedLookup.getResolvedValue());
        Assertions.assertEquals("TestData Map", fullNestedLookup.getSource());
    }

    @Test
    public final void testOfflineMockSequenceAndTokenDeltas()
    {
        final MockLlmClient llmClient = new MockLlmClient();
        final MockPageAnalyzer pageAnalyzer = new MockPageAnalyzer("<html><body>Mock SUT</body></html>");
        final ActionExecutor actionExecutor = new ActionExecutor(this)
        {
            @Override
            public final void executeAll(final List<Action> actions)
            {
                // NOOP
            }
        };

        final AiMockResponse mockResponse = AiMockResponse.builder()
                .responseText("{\"s\": true, \"r\": \"Entering text\", \"a\": [{\"t\": \"CLICK\", \"tg\": \"#login\"}], \"d\": true}")
                .delayMs(10L)
                .tokens(100L, 50L, 20L)
                .build();

        llmClient.addResponse(mockResponse);

        final AiConfiguration config = Neodymium.aiConfiguration();
        try (final AiBrowser browser = new AiBrowser(config, this, llmClient, pageAnalyzer, actionExecutor))
        {
            Neodymium.setAiBrowser(browser);

            final AiExecutionResult result = browser.execute("Click login button");
            
            Assertions.assertNotNull(result);
            Assertions.assertEquals(100L, result.getInputTokens());
            Assertions.assertEquals(50L, result.getOutputTokens());
            Assertions.assertEquals(20L, result.getCachedTokens());
            Assertions.assertEquals(150L, result.getTotalTokens());
            Assertions.assertEquals(0, result.getRetryCount());
            Assertions.assertEquals(0, result.getEscalationCount());
            
            Assertions.assertEquals(1, result.getSteps().size());
            final StepDetails step = result.getSteps().get(0);
            Assertions.assertEquals("Click login button", step.getRawInstruction());
            Assertions.assertEquals("Click login button", step.getExpandedInstruction());
            Assertions.assertEquals(1, step.getActions().size());
            Assertions.assertEquals("CLICK", step.getActions().get(0).getType());

            Assertions.assertEquals(1, step.getLlmCalls().size());
            final LlmCallDetails call = step.getLlmCalls().get(0);
            Assertions.assertEquals(ContextLevel.AXTREE, call.getContextLevel());
            Assertions.assertEquals(100L, call.getInputTokens());
            Assertions.assertEquals(50L, call.getOutputTokens());
            Assertions.assertEquals(20L, call.getCachedTokens());
            Assertions.assertEquals(200, call.getResponseCode().intValue());

            // Static getter lookup validation
            Assertions.assertSame(result, Neodymium.getLastAiExecutionResult());
        }
    }

    @Test
    public final void testSelfHealingAndContextEscalation()
    {
        final MockLlmClient llmClient = new MockLlmClient();
        final MockPageAnalyzer pageAnalyzer = new MockPageAnalyzer("<html><body>Mock SUT</body></html>");
        
        final List<Action> executed = new ArrayList<>();
        final ActionExecutor actionExecutor = new ActionExecutor(this)
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
                .responseText("{\"s\": true, \"r\": \"Initial click\", \"a\": [{\"t\": \"CLICK\", \"tg\": \"#btn\"}], \"d\": true}")
                .delayMs(5L)
                .tokens(80L, 40L, 0L)
                .build();
        
        // 2. Second LLM Response (after healing/escalation): CLICK link instead
        final AiMockResponse mockRes2 = AiMockResponse.builder()
                .responseText("{\"s\": true, \"r\": \"Retry clicking link\", \"a\": [{\"t\": \"CLICK\", \"tg\": \"#link\"}], \"d\": true}")
                .delayMs(5L)
                .tokens(100L, 50L, 10L)
                .build();

        llmClient.addResponse(mockRes1);
        llmClient.addResponse(mockRes2);

        final AiConfiguration config = Neodymium.aiConfiguration();
        try (final AiBrowser browser = new AiBrowser(config, this, llmClient, pageAnalyzer, actionExecutor))
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

    @Test
    public final void testLlmRequestedContextEscalation()
    {
        final MockLlmClient llmClient = new MockLlmClient();
        final MockPageAnalyzer pageAnalyzer = new MockPageAnalyzer("<html><body>Mock SUT</body></html>");
        final ActionExecutor actionExecutor = new ActionExecutor(this)
        {
            @Override
            public final void executeAll(final List<Action> actions)
            {
                // NOOP
            }
        };

        // Response 1: LLM directs context escalation to VISUAL
        final AiMockResponse mockRes1 = AiMockResponse.builder()
                .responseText("{\"st\": \"ESCALATE\", \"r\": \"Need screenshot for verification\", \"tc\": \"VISUAL\", \"a\": [], \"d\": false}")
                .delayMs(10L)
                .tokens(60L, 30L, 0L)
                .build();

        // Response 2: Success
        final AiMockResponse mockRes2 = AiMockResponse.builder()
                .responseText("{\"s\": true, \"r\": \"Verified visually\", \"a\": [{\"t\": \"CLICK\", \"tg\": \"#logo\"}], \"d\": true}")
                .delayMs(10L)
                .tokens(120L, 40L, 20L)
                .build();

        llmClient.addResponse(mockRes1);
        llmClient.addResponse(mockRes2);

        final AiConfiguration config = Neodymium.aiConfiguration();
        try (final AiBrowser browser = new AiBrowser(config, this, llmClient, pageAnalyzer, actionExecutor))
        {
            Neodymium.setAiBrowser(browser);

            final AiExecutionResult result = browser.execute("Verify SUT visually");

            Assertions.assertNotNull(result);
            Assertions.assertEquals(1, result.getEscalationCount());
            Assertions.assertEquals(1, result.getEscalations().size());
            final EscalationDetails escalation = result.getEscalations().get(0);
            Assertions.assertEquals(ContextLevel.AXTREE, escalation.getFromLevel());
            Assertions.assertEquals(ContextLevel.VISUAL, escalation.getToLevel());
            Assertions.assertTrue(escalation.isLlmRequested());
            Assertions.assertEquals("Need screenshot for verification", escalation.getReason());
        }
    }

    @Test
    public final void testHttpExceptionPreservation()
    {
        final MockLlmClient llmClient = new MockLlmClient();
        final MockPageAnalyzer pageAnalyzer = new MockPageAnalyzer("<html><body>Mock SUT</body></html>");
        final ActionExecutor actionExecutor = new ActionExecutor(this)
        {
            @Override
            public final void executeAll(final List<Action> actions)
            {
                // NOOP
            }
        };

        // Simulate 429 Too Many Requests - queue enough responses for retries
        final AiMockResponse mockRes = AiMockResponse.builder()
                .httpStatusCode(429)
                .build();
        for (int i = 0; i < 10; i++)
        {
            llmClient.addResponse(mockRes);
        }

        final AiConfiguration config = Neodymium.aiConfiguration();
        try (final AiBrowser browser = new AiBrowser(config, this, llmClient, pageAnalyzer, actionExecutor))
        {
            Neodymium.setAiBrowser(browser);

            final Throwable t = Assertions.assertThrows(Throwable.class, () -> {
                browser.execute("Click button");
            });

            Assertions.assertTrue(t.getMessage().contains("Simulated HTTP error code: 429") || 
                    (t.getCause() != null && t.getCause().getMessage().contains("Simulated HTTP error code: 429")));

            // Verify cached result on failure
            final AiExecutionResult result = Neodymium.getLastAiExecutionResult();
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1, result.getSteps().size());
            
            final StepDetails step = result.getSteps().get(0);
            Assertions.assertTrue(step.getLlmCalls().size() >= 1);
            
            final LlmCallDetails call = step.getLlmCalls().get(0);
            Assertions.assertEquals(429, call.getResponseCode().intValue());
            Assertions.assertTrue(call.getErrorMessage().contains("Simulated HTTP error code: 429"));
        }
    }

    @Test
    public final void testCompositeRunResult() throws Throwable
    {
        final MockLlmClient llmClient = new MockLlmClient();
        final MockPageAnalyzer pageAnalyzer = new MockPageAnalyzer("<html><body>Mock SUT</body></html>");
        final ActionExecutor actionExecutor = new ActionExecutor(this)
        {
            @Override
            public final void executeAll(final List<Action> actions)
            {
                // NOOP
            }
        };

        // Mock response for "before"
        llmClient.addResponse(AiMockResponse.builder()
                .responseText("{\"s\": true, \"r\": \"SetupSut\", \"a\": [{\"t\": \"CLICK\", \"tg\": \"#setup\"}], \"d\": true}")
                .delayMs(1L)
                .tokens(10L, 5L, 0L)
                .build());

        // Mock response for "steps"
        llmClient.addResponse(AiMockResponse.builder()
                .responseText("{\"s\": true, \"r\": \"RunSut\", \"a\": [{\"t\": \"CLICK\", \"tg\": \"#run\"}], \"d\": true}")
                .delayMs(1L)
                .tokens(20L, 10L, 0L)
                .build());

        // Mock response for "after"
        llmClient.addResponse(AiMockResponse.builder()
                .responseText("{\"s\": true, \"r\": \"TeardownSut\", \"a\": [{\"t\": \"CLICK\", \"tg\": \"#teardown\"}], \"d\": true}")
                .delayMs(1L)
                .tokens(30L, 15L, 0L)
                .build());

        final TestData data = Neodymium.getData();
        data.put("before", "Setup steps");
        data.put("steps", "Run steps");
        data.put("after", "Teardown steps");

        final AiConfiguration config = Neodymium.aiConfiguration();
        try (final AiBrowser browser = new AiBrowser(config, this, llmClient, pageAnalyzer, actionExecutor))
        {
            Neodymium.setAiBrowser(browser);

            final AiTestRunResult runResult = browser.execute();

            Assertions.assertNotNull(runResult);
            Assertions.assertNotNull(runResult.getBeforeResult());
            Assertions.assertNotNull(runResult.getStepsResult());
            Assertions.assertEquals(1, runResult.getAfterResults().size());

            Assertions.assertEquals(10L, runResult.getBeforeResult().getInputTokens());
            Assertions.assertEquals(20L, runResult.getStepsResult().getInputTokens());
            Assertions.assertEquals(30L, runResult.getAfterResults().get(0).getInputTokens());

            Assertions.assertSame(runResult, Neodymium.getLastAiTestRunResult());
        }
    }
}
