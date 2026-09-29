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
 * Live LLM integration test for the File Upload &amp; Styled Dropzones sandbox challenge.
 * Verifies that the live AI agent correctly discovers styled dropzone containers, uploads files
 * without opening blocking OS file dialogs, and validates subsequent processing.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_file_upload_playbook")
public class FileUploadSandboxLiveTest extends BaseAiTest
{
    private static final String UPLOAD_FILE = "report.pdf";

    /**
     * Constructs a default FileUploadSandboxLiveTest.
     */
    public FileUploadSandboxLiveTest()
    {
    }

    /**
     * Set up test page URL and file name dynamically before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/file-upload.html", server.getPort());
        session.data().putDynamic("file.upload.test.url", pageUrl, false);
        session.data().putDynamic("upload.file.name", UPLOAD_FILE, false);
    }

    /**
     * Tests file upload interaction using live LLM across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testFileUploadLive(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${file.upload.test.url} in the browser
              Upload "${upload.file.name}" to the document dropzone
              Verify that the upload status shows "File uploaded: ${upload.file.name}"
              Click the Process Document button
              Verify that the process status shows "Document ${upload.file.name} processed successfully"
            """);

        $("#process-status").shouldHave(text("Document " + UPLOAD_FILE + " processed successfully"));
    }
}
