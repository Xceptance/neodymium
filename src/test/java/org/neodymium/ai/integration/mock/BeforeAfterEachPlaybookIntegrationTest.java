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
package org.neodymium.ai.integration.mock;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Integration test verifying {@link BeforeEach} and {@link AfterEach} carrying {@link AiPlaybook}
 * across multi-dataset executions in Neodymium AI v2.
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiMode(ExecutionMode.LLM_ONLY)
@AiDataSet({"datasetA", "datasetB"})
public class BeforeAfterEachPlaybookIntegrationTest extends BaseAiTest
{
    private static int setupCount = 0;
    private static int testCount = 0;
    private static int teardownCount = 0;

    /**
     * Default constructor for BeforeAfterEachPlaybookIntegrationTest.
     */
    public BeforeAfterEachPlaybookIntegrationTest()
    {
    }

    @BeforeAll
    public static void configureMockLlm()
    {
        System.setProperty("neodymium.ai.global.provider", "mock");
        System.setProperty("neodymium.ai.pesap.enabled", "false");
        setupCount = 0;
        testCount = 0;
        teardownCount = 0;
    }

    @AfterAll
    public static void clearMockLlm()
    {
        System.clearProperty("neodymium.ai.global.provider");
        System.clearProperty("neodymium.ai.pesap.enabled");
    }

    public void initMock(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AllActionsTest/test.html", server.getPort());
        System.setProperty("demo.url", pageUrl);
        Neodymium.getData().put("demo.url", pageUrl);

        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        mock.clearResponses();

        // 1. Queue responses for @BeforeEach setup-navigate.yaml (NAVIGATE)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to test page in setup"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Queue responses for @Test main-click.yaml (CLICK)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#btn-click",
                  "value": "",
                  "reasoning": "Click button in main test"
                }
              ]
            }
            """, null, "mock"));

        // 3. Queue responses for @AfterEach teardown-verify.yaml (ASSERT)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#click-status",
                  "value": "Clicked",
                  "reasoning": "Verify state in teardown"
                }
              ]
            }
            """, null, "mock"));
    }

    @BeforeEach
    @AiPlaybook("/playbooks/integration/setup-navigate.yaml")
    void setup(final AiSession session)
    {
        initMock(session);
        setupCount++;
    }

    @AiPlaybook("/playbooks/integration/main-click.yaml")
    public void testMultiDatasetWithLifecyclePlaybooks(final AiSession session)
    {
        testCount++;
        assertNotNull(session, "AiSession must be injected and available in test method");
        assertNotNull(session.getExecutionContext().getTransientData(), "Transient data map initialized");
    }

    @AfterEach
    @AiPlaybook("/playbooks/integration/teardown-verify.yaml")
    void teardown(final AiSession session)
    {
        teardownCount++;
        assertNotNull(session, "AiSession must be injected and available in @AfterEach method");
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        mock.clearResponses();
    }
}
