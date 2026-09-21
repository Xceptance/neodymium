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
package org.neodymium.ai.integration.data;

import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Selenide.$;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Integration test verifying external YAML companion test data loading via convention,
 * intra-dataset variable interpolation, and programmatic step execution in code without external network dependencies.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@NeodymiumAiTest
public class ProgrammaticTestDataYamlTest extends BaseAiTest
{
    @BeforeAll
    public static void configureMockLlm()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
    }

    @AfterAll
    public static void clearMockLlm()
    {
        Neodymium.getData().remove("neodymium.ai.global.provider");
    }

    @BeforeEach
    public void setupMockResponses(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AllActionsTest/test.html", server.getPort());
        session.data().putDynamic("test.url", pageUrl, false);

        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        mock.clearResponses();

        // Step 1: Open SUT
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Open local test page",
              "tool_calls": [
                {
                  "name": "navigate",
                  "arguments": {
                    "url": "%s"
                  }
                },
                {
                  "name": "complete_step",
                  "arguments": {
                    "summary": "Page opened"
                  }
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // Step 2: Type interpolated value into text input
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Type interpolated search phrase into text field",
              "tool_calls": [
                {
                  "name": "type",
                  "arguments": {
                    "selector": "#input-text",
                    "text": "Neodymium"
                  }
                },
                {
                  "name": "complete_step",
                  "arguments": {
                    "summary": "Entered search phrase"
                  }
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Executes programmatic steps in code using variables loaded from the companion YAML file.
     *
     * @param session the Neodymium AI session instance
     * @throws Exception if playbook execution fails
     */
    @AiPlaybook(AiPlaybook.PROGRAMMATIC)
    @AiMode(ExecutionMode.LLM_ONLY)
    public void test(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${test.url} in the browser
              Type '${searchPhrase}' in the Search input field
            """);

        $("#input-text").shouldHave(value("Neodymium"));
        Assertions.assertEquals("Deutsch", session.data().get("language"));
        Assertions.assertEquals("Neo", session.data().get("phrase.Part1"));
        Assertions.assertEquals("dymium", session.data().get("phrase.Part2"));
        Assertions.assertEquals("Neodymium", session.data().resolveVariables("${searchPhrase}"));
    }
}
