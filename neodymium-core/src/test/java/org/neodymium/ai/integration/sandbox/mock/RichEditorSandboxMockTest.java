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

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.executor.selenide.PageAnalyzer;
import org.neodymium.ai.executor.selenide.plugins.ClearAction;
import org.neodymium.ai.executor.selenide.plugins.TypeAction;
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
 * Mock integration test for the Rich Text &amp; Contenteditable Editors sandbox challenge.
 * Tests autonomous typing into formatted contenteditable divs, caret manipulation, clearing, and saving.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "rich_editor_mock_playbook")
public class RichEditorSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructs a default RichEditorSandboxMockTest.
     */
    public RichEditorSandboxMockTest()
    {
    }

    @BeforeAll
    public static void disableLiveLlm()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
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
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/rich-editor.html", server.getPort());
        session.data().putDynamic("rich_editor_url", pageUrl, false);

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
              "thought": "Navigate to the rich editor sandbox challenge.",
              "actions": [
                {
                  "action": "NAVIGATE",
                  "value": "%s"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Type into rich editor
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Type formatted document text into the rich text editor.",
              "actions": [
                {
                  "action": "TYPE",
                  "selector": "#rich-editor",
                  "value": "Autonomous release notes for Q3 2026."
                }
              ]
            }
            """, null, "mock"));

        // 3. Click save button
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Save the document contents.",
              "actions": [
                {
                  "action": "CLICK",
                  "selector": "#save-content-btn"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests multi-turn autonomous typing into contenteditable editor and saving the content.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testRichEditorAutonomousTypingAndSave(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${rich_editor_url} in the browser
              Type "Autonomous release notes for Q3 2026." into the rich text editor
              Click the Save Document button
            """);

        $("#saved-message").shouldBe(visible).shouldHave(text("Document saved: Autonomous release notes for Q3 2026."));
    }

    /**
     * Tests typing into an inline contenteditable comment box and submitting.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testCommentEditorAutonomousPost(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/rich-editor.html", server.getPort());
        final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        if (provider instanceof final MockLlmProvider mock)
        {
            mock.clearResponses();
            mock.addResponse(new LlmResponse("""
                {
                  "thought": "Navigate to the rich editor sandbox challenge.",
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
                  "thought": "Type a comment into the comment editor.",
                  "actions": [
                    {
                      "action": "TYPE",
                      "selector": "#comment-editor",
                      "value": "Excellent contenteditable support."
                    }
                  ]
                }
                """, null, "mock"));
            mock.addResponse(new LlmResponse("""
                {
                  "thought": "Post the comment.",
                  "actions": [
                    {
                      "action": "CLICK",
                      "selector": "#post-comment-btn"
                    }
                  ]
                }
                """, null, "mock"));
        }

        session.execute("""
            steps: |
              Open ${rich_editor_url} in the browser
              Type "Excellent contenteditable support." into the comment editor
              Click the Post Comment button
            """);

        $("#saved-comment").shouldBe(visible).shouldHave(text("Excellent contenteditable support."));
    }

    /**
     * Direct test verifying that PageAnalyzer captures contenteditable attributes in DOM snapshots.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testContenteditablePerceptionInPageAnalyzer(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/rich-editor.html", server.getPort());
        Selenide.open(pageUrl);

        $("#rich-editor").shouldBe(visible);
        $("#comment-editor").shouldBe(visible);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String minimalDom = analyzer.getPageContext(ContextLevel.MINIMAL);
        final String standardDom = analyzer.getPageContext(ContextLevel.STANDARD);

        Assertions.assertTrue(minimalDom.contains("contenteditable=\"true\"") || minimalDom.contains("rich-editor"),
                "Minimal DOM snapshot must capture contenteditable editor: " + minimalDom);
        Assertions.assertTrue(standardDom.contains("contenteditable=\"true\""),
                "Standard DOM snapshot must include contenteditable=\"true\" attribute: " + standardDom);
        Assertions.assertTrue(standardDom.contains("rich-editor"),
                "Standard DOM snapshot must retain rich-editor id: " + standardDom);
    }

    /**
     * Direct test verifying BrowserToolProvider.browser_type execution with clearFirst true and false on contenteditable.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testDirectBrowserTypeContenteditableExecution(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/rich-editor.html", server.getPort());
        Selenide.open(pageUrl);

        final ToolRegistry reg = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(reg);
        final AiTool typeTool = reg.getTool("browser_type").orElseThrow();
        final AiTool clearTool = reg.getTool("browser_clear").orElseThrow();

        // 1. Initial type with clearFirst=true (default)
        final JsonNode initialArgs = MAPPER.readTree("""
            {"selector": "#rich-editor", "text": "Initial headline."}
            """);
        final ToolResult res1 = typeTool.execute(new ToolCall("call_type_1", "browser_type", initialArgs), null);
        Assertions.assertFalse(res1.isError(), "Initial browser_type should succeed");
        $("#rich-editor").shouldHave(text("Initial headline."));

        // 2. Append type with clearFirst=false
        final JsonNode appendArgs = MAPPER.readTree("""
            {"selector": "#rich-editor", "text": " Appended paragraph.", "clearFirst": false}
            """);
        final ToolResult res2 = typeTool.execute(new ToolCall("call_type_2", "browser_type", appendArgs), null);
        Assertions.assertFalse(res2.isError(), "Appended browser_type should succeed");
        $("#rich-editor").shouldHave(text("Initial headline. Appended paragraph."));

        // 3. Clear via browser_clear
        final JsonNode clearArgs = MAPPER.readTree("""
            {"selector": "#rich-editor"}
            """);
        final ToolResult res3 = clearTool.execute(new ToolCall("call_clear", "browser_clear", clearArgs), null);
        Assertions.assertFalse(res3.isError(), "browser_clear on contenteditable should succeed");
        $("#rich-editor").shouldBe(empty);
    }

    /**
     * Direct test verifying TypeAction and ClearAction plugins execute successfully on contenteditable divs.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testTypeAndClearActionPluginsOnContenteditable(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/rich-editor.html", server.getPort());
        Selenide.open(pageUrl);

        final TypeAction typeAction = new TypeAction();
        final ClearAction clearAction = new ClearAction();

        // 1. Type via TypeAction
        final Action typeAct = new Action("TYPE", "#rich-editor", List.of("Plugin typing validation."), "Type in rich editor", "");
        typeAction.execute(typeAct);

        $("#rich-editor").shouldHave(text("Plugin typing validation."));

        // 2. Clear via ClearAction
        final Action clearAct = new Action("CLEAR", "#rich-editor", "Clear rich editor");
        clearAction.execute(clearAct);

        $("#rich-editor").shouldBe(empty);
    }
}
