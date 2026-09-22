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
package org.neodymium.ai.integration.sandbox.mock;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.executor.selenide.PageAnalyzer;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Mock integration test for the Autocomplete &amp; Debounced Typeahead with Floating Portals sandbox challenge.
 * Tests autonomous search input, debounced portal appearance, keyboard navigation, and mouse selection.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "autocomplete_mock_playbook")
public class AutocompleteSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructs a default AutocompleteSandboxMockTest.
     */
    public AutocompleteSandboxMockTest()
    {
    }

    @BeforeAll
    public static void disableLiveLlm()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
    }

    /**
     * Set up test page URL and queue LLM mock responses before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/autocomplete.html", server.getPort());
        session.data().putDynamic("autocomplete_url", pageUrl, false);

        final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        final MockLlmProvider mock;
        if (provider instanceof final MockLlmProvider mlp)
        {
            mock = mlp;
        }
        else
        {
            mock = new MockLlmProvider();
            session.getLlmRegistry().registerProvider(mock);
        }

        mock.clearResponses();

        // 1. Navigate to page
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Navigate to the autocomplete sandbox challenge.",
              "actions": [
                {
                  "action": "NAVIGATE",
                  "value": "%s"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Type "Ber" into search input
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Type 'Ber' into the destination search input.",
              "actions": [
                {
                  "action": "TYPE",
                  "selector": "#search-input",
                  "value": "Ber"
                }
              ]
            }
            """, null, "mock"));

        // 3. Press ArrowDown to highlight the first suggestion (Berlin)
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Press ArrowDown key to highlight the first suggestion.",
              "actions": [
                {
                  "action": "KEY_PRESS",
                  "selector": "#search-input",
                  "value": "ArrowDown"
                }
              ]
            }
            """, null, "mock"));

        // 4. Press Enter to select the highlighted suggestion
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Press Enter key to confirm the highlighted suggestion.",
              "actions": [
                {
                  "action": "KEY_PRESS",
                  "selector": "#search-input",
                  "value": "Enter"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests autocomplete with debounced floating portal selection via keyboard navigation (ArrowDown + Enter).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testAutocompleteKeyboardSelection(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${autocomplete_url} in the browser
              Type "Ber" into the destination search input
              Navigate down with ArrowDown key
              Press Enter to confirm selection
            """);

        $("#selection-result").shouldBe(visible).shouldHave(text("Berlin, Germany (BER)"));
        $("#search-input").shouldHave(value("Berlin, Germany (BER)"));
    }

    /**
     * Tests autocomplete with direct mouse click selection on a suggestion item.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testAutocompleteMouseClickSelection(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/autocomplete.html", server.getPort());
        final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        if (provider instanceof final MockLlmProvider mock)
        {
            mock.clearResponses();
            mock.addResponse(new LlmResponse("""
                {
                  "thought": "Navigate to the autocomplete sandbox challenge.",
                  "actions": [
                    {
                      "action": "NAVIGATE",
                      "value": "%s"
                    }
                  ]
                }
                """.formatted(pageUrl), null, "mock"));
            mock.addResponse(new LlmResponse("""
                {
                  "thought": "Type 'Tok' into search input.",
                  "actions": [
                    {
                      "action": "TYPE",
                      "selector": "#search-input",
                      "value": "Tok"
                    }
                  ]
                }
                """, null, "mock"));
            mock.addResponse(new LlmResponse("""
                {
                  "thought": "Click on Tokyo suggestion item in the portal.",
                  "actions": [
                    {
                      "action": "CLICK",
                      "selector": ".suggestion-item[data-value*='Tokyo']"
                    }
                  ]
                }
                """, null, "mock"));
        }

        session.execute("""
            steps: |
              Open ${autocomplete_url} in the browser
              Type "Tok" into the search input
              Click on the Tokyo suggestion item
            """);

        $("#selection-result").shouldBe(visible).shouldHave(text("Tokyo, Japan (NRT)"));
        $("#search-input").shouldHave(value("Tokyo, Japan (NRT)"));
    }

    /**
     * Direct test verifying that PageAnalyzer captures floating portal containers in DOM snapshots.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testFloatingPortalPerceptionInPageAnalyzer(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/autocomplete.html", server.getPort());
        Selenide.open(pageUrl);

        $("#search-input").shouldBe(visible).setValue("Ber");
        $("#autocomplete-portal").shouldBe(visible);
        $(".suggestion-item").shouldBe(visible);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String minimalDom = analyzer.getPageContext(ContextLevel.MINIMAL);
        final String standardDom = analyzer.getPageContext(ContextLevel.STANDARD);

        Assertions.assertTrue(minimalDom.contains("autocomplete-portal") || minimalDom.contains("Berlin"),
                "Minimal DOM snapshot must capture portal or its options: " + minimalDom);
        Assertions.assertTrue(standardDom.contains("autocomplete-portal"),
                "Standard DOM snapshot must retain autocomplete-portal container: " + standardDom);
        Assertions.assertTrue(standardDom.contains("Berlin"),
                "Standard DOM snapshot must capture suggestions: " + standardDom);
    }

    /**
     * Direct test verifying BrowserToolProvider.browser_press_key execution with normalized key mapping.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testDirectBrowserPressKeyToolExecution(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/autocomplete.html", server.getPort());
        Selenide.open(pageUrl);

        $("#search-input").shouldBe(visible).setValue("Par");
        $("#autocomplete-portal").shouldBe(visible);

        final ToolRegistry reg = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(reg);
        final AiTool pressKeyTool = reg.getTool("browser_press_key").orElseThrow();

        // 1. Press ArrowDown
        final JsonNode downArgs = MAPPER.readTree("""
            {"key": "ArrowDown", "selector": "#search-input"}
            """);
        final ToolResult downResult = pressKeyTool.execute(new ToolCall("call_down", "browser_press_key", downArgs), null);
        Assertions.assertFalse(downResult.isError());

        // 2. Press Enter
        final JsonNode enterArgs = MAPPER.readTree("""
            {"key": "Enter", "selector": "#search-input"}
            """);
        final ToolResult enterResult = pressKeyTool.execute(new ToolCall("call_enter", "browser_press_key", enterArgs), null);
        Assertions.assertFalse(enterResult.isError());

        $("#selection-result").shouldBe(visible).shouldHave(text("Paris, France (CDG)"));
        $("#search-input").shouldHave(value("Paris, France (CDG)"));
    }
}
