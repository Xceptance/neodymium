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
import com.xceptance.neodymium.ai.core.AiTestRunResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.core.EscalationDetails;
import com.xceptance.neodymium.ai.core.LlmCallDetails;
import com.xceptance.neodymium.ai.core.LookupDetails;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.ai.testing.AiMockResponse;
import com.xceptance.neodymium.common.testdata.TestData;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Rich offline integration tests validating the creation, populating, and caching
 * of the {@link AiExecutionResult} and {@link AiTestRunResult} metrics and details.
 * <p>
 * This class extends {@link BaseAiOfflineTest} to run visual anomalies and placeholder resolutions
 * completely offline with zero LLM API costs.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class AiExecutionResultDemoTest extends BaseAiOfflineTest
{
    /**
     * Verifies that the variable resolution system parses template placeholders 
     * (e.g. {@code ${username}}) from the active TestData map, handles nested placeholders, 
     * and compiles the precise details of every variable resolution (key, value, source, 
     * localization status) into a lookup collector.
     */
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

    /**
     * Validates browserless mock execution under standard success conditions.
     * Uses a mock LLM client to return a mock JSON response containing click actions
     * and specified token counts. Asserts that the final result aggregates these tokens,
     * captures the step-level details (raw vs expanded instructions), tracks the executed actions, 
     * records the HTTP response code (200), and registers lookup statistics.
     */
    @Test
    public final void testOfflineMockSequenceAndTokenDeltas()
    {
        this.pageAnalyzer.setMockDomText("<html><body>Mock SUT</body></html>");

        final AiMockResponse mockResponse = AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Entering text",
                      "a": [{"t": "CLICK", "tg": "#login"}],
                      "d": true
                    }
                    """)
                .delayMs(10L)
                .tokens(100L, 50L, 20L)
                .build();

        this.llmClient.addResponse(mockResponse);

        Neodymium.getData().put("loginLabel", "login button");

        final AiExecutionResult result = this.mockBrowser.execute("Click ${loginLabel}");
        
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(100L, result.getInputTokens());
        Assertions.assertEquals(50L, result.getOutputTokens());
        Assertions.assertEquals(20L, result.getCachedTokens());
        Assertions.assertEquals(150L, result.getTotalTokens());
        Assertions.assertEquals(0, result.getRetryCount());
        Assertions.assertEquals(0, result.getEscalationCount());
        
        // Assert lookup details gathered during execute
        Assertions.assertEquals(1, result.getLookups().size());
        Assertions.assertEquals("loginLabel", result.getLookups().get(0).getKey());
        Assertions.assertEquals("login button", result.getLookups().get(0).getResolvedValue());

        Assertions.assertEquals(1, result.getSteps().size());
        final StepDetails step = result.getSteps().get(0);
        Assertions.assertEquals("Click ${loginLabel}", step.getRawInstruction());
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
     * Verifies that when the LLM service returns HTTP communication failures (e.g., 429 Too Many Requests),
     * the framework propagates the exception, preserves the HTTP status code, and captures the details
     * in the execution result. Asserts that the step's overall outcome is marked as failed.
     */
    @Test
    public final void testHttpExceptionPreservation()
    {
        // Simulate 429 Too Many Requests - queue enough responses for retries
        final AiMockResponse mockRes = AiMockResponse.builder()
                .httpStatusCode(429)
                .build();
        for (int i = 0; i < 10; i++)
        {
            this.llmClient.addResponse(mockRes);
        }

        final Throwable t = Assertions.assertThrows(Throwable.class, () -> {
            this.mockBrowser.execute("Click button");
        });

        Assertions.assertTrue(t.getMessage().contains("Simulated HTTP error code: 429") || 
                (t.getCause() != null && t.getCause().getMessage().contains("Simulated HTTP error code: 429")));

        // Verify cached result on failure
        final AiExecutionResult result = Neodymium.getLastAiExecutionResult();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(1, result.getSteps().size());
        
        final StepDetails step = result.getSteps().get(0);
        Assertions.assertTrue(step.getLlmCalls().size() >= 1);
        
        final LlmCallDetails call = step.getLlmCalls().get(0);
        Assertions.assertEquals(429, call.getResponseCode().intValue());
        Assertions.assertTrue(call.getErrorMessage().contains("Simulated HTTP error code: 429"));
    }

    /**
     * Verifies the composition of before, steps, and after lifecycle execution stages
     * inside a data-driven test run. Enqueues mock responses for all three phases and
     * asserts that {@link AiTestRunResult} contains individual non-null results and
     * maps the cumulative token usage across all lifecycle stages.
     */
    @Test
    public final void testCompositeRunResult() throws Throwable
    {
        // Mock response for "before"
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "SetupSut",
                      "a": [{"t": "CLICK", "tg": "#setup"}],
                      "d": true
                    }
                    """)
                .delayMs(1L)
                .tokens(10L, 5L, 0L)
                .build());

        // Mock response for "steps"
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "RunSut",
                      "a": [{"t": "CLICK", "tg": "#run"}],
                      "d": true
                    }
                    """)
                .delayMs(1L)
                .tokens(20L, 10L, 0L)
                .build());

        // Mock response for "after"
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "TeardownSut",
                      "a": [{"t": "CLICK", "tg": "#teardown"}],
                      "d": true
                    }
                    """)
                .delayMs(1L)
                .tokens(30L, 15L, 0L)
                .build());

        final TestData data = Neodymium.getData();
        data.put("before", "Setup steps");
        data.put("steps", "Run steps");
        data.put("after", "Teardown steps");

        final AiTestRunResult runResult = this.mockBrowser.execute();

        Assertions.assertNotNull(runResult);
        Assertions.assertNotNull(runResult.getBeforeResult());
        Assertions.assertNotNull(runResult.getStepsResult());
        Assertions.assertEquals(1, runResult.getAfterResults().size());

        Assertions.assertEquals(10L, runResult.getBeforeResult().getInputTokens());
        Assertions.assertEquals(20L, runResult.getStepsResult().getInputTokens());
        Assertions.assertEquals(30L, runResult.getAfterResults().get(0).getInputTokens());

        Assertions.assertSame(runResult, Neodymium.getLastAiTestRunResult());
    }

    /**
     * Recipe 1: Asserting Retry & Self-Healing on HTTP 503 Errors.
     * Validate that Neodymium correctly logs communication errors, handles retry rules,
     * and succeeds once the service returns.
     */
    @Test
    public final void testSelfHealingOnHttp503Error()
    {
        // Set up the virtual LLM queue: a 503 failure, followed by a clean action success response
        this.llmClient.addResponse(AiMockResponse.builder()
                .httpStatusCode(503)
                .delayMs(10L)
                .build());
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#btn"}],
                      "d": true
                    }
                    """)
                .tokens(150L, 45L)
                .build());

        // Execute
        final AiExecutionResult result = this.mockBrowser.execute("Click on the blue button");

        // Verify self-healing occurred
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(1, result.getEscalationCount());
        Assertions.assertEquals(0, result.getRetryCount());
        Assertions.assertEquals(2, result.getLlmCalls().size());
        
        // Assert that the first call captured the 503 error details
        Assertions.assertNotNull(result.getLlmCalls().get(0).getErrorMessage());
        Assertions.assertTrue(result.getLlmCalls().get(0).getErrorMessage().contains("503"));
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
     * Recipe 3: Intercepting and Verifying Action Sequence Order.
     * Use MockActionExecutor to assert that correct Selenium/WebDriver interactions
     * are performed by the execution loop in the correct relative sequence, without running real browsers.
     */
    @Test
    public final void testBrowserActionSequenceVerification()
    {
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#input-field"}],
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
                      "a": [{"t": "TYPE", "tg": "#input-field", "v": "Demo"}],
                      "d": true
                    }
                    """)
                .build());

        this.mockBrowser.execute("Enter 'Demo' into input field");

        final List<Action> actionLog = this.actionExecutor.getExecutedActions();
        Assertions.assertEquals(2, actionLog.size());
        Assertions.assertTrue(actionLog.get(0).toString().contains("CLICK"));
        Assertions.assertTrue(actionLog.get(1).toString().contains("TYPE"));
        Assertions.assertTrue(actionLog.get(1).toString().contains("Demo"));
    }

    /**
     * Recipe 4: Asserting Template Variable Lookups.
     * Assert that instructions containing dynamic placeholders resolve variables from the correct,
     * authorized scope sources.
     */
    @Test
    public final void testTestDataVariableResolutionScope()
    {
        // Set up test data variables in Neodymium
        Neodymium.getData().put("accountEmail", "user@neodymium.com");

        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [],
                      "d": true
                    }
                    """)
                .build());

        // Execute instruction containing placeholder
        final AiExecutionResult result = this.mockBrowser.execute("Type '${accountEmail}' into email input");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(1, result.getLookups().size());
        
        final LookupDetails lookup = result.getLookups().get(0);
        Assertions.assertEquals("accountEmail", lookup.getKey());
        Assertions.assertEquals("user@neodymium.com", lookup.getResolvedValue());
        Assertions.assertEquals("TestData Map", lookup.getSource()); // Asserts it resolved from standard test data scope
    }
}
