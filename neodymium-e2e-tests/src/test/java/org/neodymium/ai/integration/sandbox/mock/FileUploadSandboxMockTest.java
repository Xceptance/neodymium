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
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
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
 * Mock integration test for the File Upload &amp; Styled Dropzones sandbox challenge.
 * Tests uploading files to styled container dropzones enclosing hidden file inputs.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "file_upload_mock_playbook")
public class FileUploadSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SAMPLE_FILENAME = "sample.txt";

    /**
     * Constructs a default FileUploadSandboxMockTest.
     */
    public FileUploadSandboxMockTest()
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
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/file-upload.html", server.getPort());
        session.data().putDynamic("file_upload_url", pageUrl, false);
        session.data().putDynamic("sample_filename", SAMPLE_FILENAME, false);

        final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        final MockLlmProvider mock;
        if (provider instanceof MockLlmProvider mlp)
        {
            mock = mlp;
        }
        else
        {
            mock = new MockLlmProvider();
            session.getLlmRegistry().registerProvider(mock);
        }
        mock.clearResponses();

        // 1. Open SUT
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to file upload sandbox page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Upload file to dropzone container
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "UPLOAD",
                  "locator": "#document-dropzone",
                  "value": "%s",
                  "reasoning": "Upload document to styled dropzone container"
                }
              ]
            }
            """.formatted(SAMPLE_FILENAME), null, "mock"));

        // 3. Verify upload status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#upload-status",
                  "value": "File uploaded: %s",
                  "reasoning": "Verify file upload status reflects uploaded filename"
                }
              ]
            }
            """.formatted(SAMPLE_FILENAME), null, "mock"));

        // 4. Click Process Document button
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#process-btn",
                  "value": "",
                  "reasoning": "Process the uploaded document"
                }
              ]
            }
            """, null, "mock"));

        // 5. Verify process status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#process-status",
                  "value": "Document %s processed successfully",
                  "reasoning": "Verify processing status confirms successful document processing"
                }
              ]
            }
            """.formatted(SAMPLE_FILENAME), null, "mock"));
    }

    /**
     * Tests multi-step file upload flow using simulated LLM responses.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testFileUploadMock(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${file_upload_url} in the browser
              Upload "${sample_filename}" to #document-dropzone
              Verify that #upload-status shows "File uploaded: ${sample_filename}"
              Click #process-btn
              Verify that #process-status shows "Document ${sample_filename} processed successfully"
            """);

        $("#process-status").shouldHave(text("Document " + SAMPLE_FILENAME + " processed successfully"));
    }

    /**
     * Tests browser_upload_file native tool directly against a styled dropzone container.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(recordingFileName = "direct_upload_tool_playbook")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testDirectUploadTool(final AiSession session) throws Exception
    {
        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);

        final AiTool uploadTool = registry.getTool("browser_upload_file").orElseThrow();
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/file-upload.html", server.getPort());
        Selenide.open(pageUrl);

        $("#upload-status").shouldHave(text("No file uploaded"));

        // Upload using container selector #document-dropzone (which encloses hidden #document-input)
        final ToolResult uploadResult = uploadTool.execute(
                new ToolCall("call-upload-1", "browser_upload_file",
                        MAPPER.createObjectNode()
                                .put("selector", "#document-dropzone")
                                .put("filePath", "direct-test.pdf")),
                null
        );

        Assertions.assertEquals(ToolResult.Status.SUCCESS, uploadResult.status());
        final JsonNode resultJson = MAPPER.readTree(uploadResult.content());
        Assertions.assertTrue(resultJson.path("fileName").asText().contains("direct-test"));
        Assertions.assertTrue(resultJson.path("fileSize").asLong() > 0);

        // Verify UI updated
        $("#upload-status").shouldHave(text("File uploaded: " + resultJson.path("fileName").asText()));
        $("#uploaded-filename").shouldHave(text(resultJson.path("fileName").asText()));

        // Click Process Document
        $("#process-btn").click();
        $("#process-status").shouldHave(text("Document " + resultJson.path("fileName").asText() + " processed successfully"));
    }
}
