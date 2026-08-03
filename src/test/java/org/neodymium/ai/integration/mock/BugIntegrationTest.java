/*
 * Apache License 2.0
 *
 * Copyright (c) 2026 Xceptance
 */
package org.neodymium.ai.integration.mock;

import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.util.Neodymium;

/**
 * Mock programmatic integration test verifying (bug) tag and (no-healing) support.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", name = "custom_bug_playbook")
public class BugIntegrationTest extends BaseAiTest
{

    private String pageUrl;

    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session)
    {
        pageUrl = String.format("http://localhost:%d/AllActionsTest/test.html", server.getPort());
        session.getExecutionContext().getSessionData().putDynamic("bug.test.url", pageUrl, false);
    }

    /**
     * Test case 1: A failing step marked with (bug) halts the test successfully (passes).
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testBugFailureStopsTestSuccessfully(final AiSession session)
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open SUT (NAVIGATE)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // Step 2: Click expected bug button #non-existent-button (fails)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#non-existent-button",
                  "value": "",
                  "reasoning": "Click the non-existent button"
                }
              ]
            }
            """, null, "mock"));

        // Note: No mock response for Step 3, because it should not be executed!

        runPlaybook(session, """
            data:
              - testId: bugData
            steps: |
              Open ${bug.test.url} in the browser
              Click the expected bug button #non-existent-button (bug: expected_failure_test)
              Click another button #some-button
            """);

        // The playbook run should complete successfully since the failure was expected (bug).
    }

    /**
     * Test case 2: A failing step marked with (bug) (continue-on-error) continues the test successfully (passes).
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testBugFailureContinueOnError(final AiSession session)
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open SUT (NAVIGATE)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // Step 2: Click expected bug button #non-existent-button (fails)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#non-existent-button",
                  "value": "",
                  "reasoning": "Click the non-existent button"
                }
              ]
            }
            """, null, "mock"));

        // Step 3: Click another button (should run!)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#btn-click",
                  "value": "",
                  "reasoning": "Click Me"
                }
              ]
            }
            """, null, "mock"));

        runPlaybook(session, """
            data:
              - testId: bugData
            steps: |
              Open ${bug.test.url} in the browser
              Click the expected bug button #non-existent-button (bug: continue_test) (continue-on-error) (no-healing)
              Click the button #btn-click (no-replay)
            """);

        // The playbook run should complete successfully and execute Step 3 as well.
    }

    /**
     * Test case 3: A succeeding step marked with (bug) fails the test.
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testBugSuccessFailsTest(final AiSession session)
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open SUT (NAVIGATE)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // Step 2: Verify that page title is 'All Actions Integration Test Page' (succeeds)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "title",
                  "value": "All Actions Integration Test Page",
                  "reasoning": "Assert title"
                }
              ]
            }
            """, null, "mock"));

        final RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            runPlaybook(session, """
                data:
                  - testId: bugData
                steps: |
                  Open ${bug.test.url} in the browser
                  Verify that page title is 'All Actions Integration Test Page' (bug: expected_bug)
                """);
        });

        org.junit.jupiter.api.Assertions.assertTrue(ex.getCause() instanceof org.neodymium.ai.pipeline.UnexpectedSuccessException,
            "Cause of the exception should be UnexpectedSuccessException");
    }

    /**
     * Test case 4: A succeeding step marked with (bug) (continue-on-error) continues the test but reports a warning.
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testBugSuccessContinueOnError(final AiSession session)
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open SUT (NAVIGATE)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // Step 2: Verify that page title is 'All Actions Integration Test Page' (succeeds)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "title",
                  "value": "All Actions Integration Test Page",
                  "reasoning": "Assert title"
                }
              ]
            }
            """, null, "mock"));

        // Step 3: Click another button (should run!)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#btn-click",
                  "value": "",
                  "reasoning": "Click Me"
                }
              ]
            }
            """, null, "mock"));

        runPlaybook(session, """
            data:
              - testId: bugData
            steps: |
              Open ${bug.test.url} in the browser
              Verify that page title is 'All Actions Integration Test Page' (bug: expected_bug) (continue-on-error)
              Click the button #btn-click (no-replay)
            """);

        // The playbook run should complete successfully and report the warning.
    }

    /**
     * Test case 5: When we split a step up, the bug flag should be copied to all split steps.
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testBugSplitStepInheritsFlags(final AiSession session)
    {
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "true");
        try
        {
            final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

            // Step 1: Open SUT (NAVIGATE)
            mock.addResponse(new LlmResponse("""
                {
                  "actions": [
                    {
                      "action": "NAVIGATE",
                      "locator": "",
                      "value": "%s",
                      "reasoning": "Navigate to page"
                    }
                  ]
                }
                """.formatted(pageUrl), null, "mock"));

            // Step 2: Split expected bug compound step (PESAP split request)
            mock.addResponse(new LlmResponse("""
                {
                  "c": "LEAN",
                  "jm": false,
                  "sp": [
                    "Click the first button #non-existent-button-1",
                    "Click the second button #non-existent-button-2"
                  ]
                }
                """, null, "mock"));

            // Step 2a: Sub-step 1: Click the first button (fails)
            mock.addResponse(new LlmResponse("""
                {
                  "actions": [
                    {
                      "action": "CLICK",
                      "locator": "#non-existent-button-1",
                      "value": "",
                      "reasoning": "Click first button"
                    }
                  ]
                }
                """, null, "mock"));

            // Note: Sub-step 2 is skipped because Sub-step 1 fails and it's a bug step, which halts the test!

            runPlaybook(session, """
                data:
                  - testId: bugData
                steps: |
                  Open ${bug.test.url} in the browser
                  Click button 1 and click button 2 (bug: split_bug_test)
                """);
        }
        finally
        {
            Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
        }
    }
}
