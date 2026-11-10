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
package org.neodymium.ai.integration.sandbox.live;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live LLM integration test for the Rich Text &amp; Contenteditable Editors sandbox challenge.
 * Verifies that the live AI agent perceives contenteditable attributes in DOM snapshots,
 * types formatted content into rich editors, saves documents, and validates status feedback.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_rich_editor_playbook")
public class RichEditorSandboxLiveTest extends BaseAiTest
{
    /**
     * Constructs a default RichEditorSandboxLiveTest.
     */
    public RichEditorSandboxLiveTest()
    {
    }

    /**
     * Set up test page URL dynamically before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/rich-editor.html", server.getPort());
        session.data().putDynamic("rich_editor.test.url", pageUrl, false);
    }

    /**
     * Tests contenteditable typing and saving using live LLM across execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testRichEditorLive(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${rich_editor.test.url} in the browser
              Type "Neodymium AI modern rich editor automation." into the rich text editor
              Click the Save Document button
              Verify that the saved message confirms the document was saved
            """);

        $("#saved-message").shouldHave(text("Document saved: Neodymium AI modern rich editor automation."));
    }
}
