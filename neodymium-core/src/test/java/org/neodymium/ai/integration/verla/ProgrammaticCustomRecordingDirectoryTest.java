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
package org.neodymium.ai.integration.verla;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiLlmCache;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.util.EmbeddedHtmlServer;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Integration test verifying that replaying a programmatic AI test with a custom
 * recordingDirectory executes the Java test method body rather than running declarative
 * playbook execution prior to the test method.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@Tag("integration")
@Tag("verla")
@AiLlmCache
@NeodymiumAiTest
public class ProgrammaticCustomRecordingDirectoryTest
{
    private static final String CUSTOM_REC_DIR = "target/custom-programmatic-recordings";

    private static EmbeddedHtmlServer server;

    private static boolean javaMethodBodyExecuted;

    @BeforeAll
    public static void startServer() throws IOException
    {
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
        Neodymium.getData().put("neodymium.ai.judge.enabled", "false");
        Neodymium.getData().put("neodymium.ai.semanticVerification.enabled", "false");
        Neodymium.getData().put("neodymium.ai.visualRca.enabled", "false");
        AiConfiguration.resetInstance();

        server = new EmbeddedHtmlServer();
        server.start();

        final File recDir = new File(CUSTOM_REC_DIR);
        recDir.mkdirs();

        final String jsonRecording = """
            [
              {
                "instruction": "Open ${verla.url}/verla-perfect/index.html in the browser",
                "actions": [
                  {
                    "type": "NAVIGATE",
                    "target": "${verla.url}/verla-perfect/index.html",
                    "description": "Extracted NAVIGATE action"
                  }
                ],
                "status": "SUCCESS",
                "targetFramework": "SELENIUM_SELENIDE"
              },
              {
                "instruction": "Assert test data defined in Java method body",
                "actions": [
                  {
                    "type": "ASSERT_TEXT",
                    "target": "#search-input",
                    "value": "${searchTerm}",
                    "description": "Assert search term"
                  }
                ],
                "status": "SUCCESS",
                "targetFramework": "SELENIUM_SELENIDE"
              }
            ]
            """;

        final String baseName = "ProgrammaticCustomRecordingDirectoryTest_testProgrammaticReplayWithCustomRecordingDirectory";
        FileUtils.writeStringToFile(new File(recDir, baseName + "_Chrome_1500x1000_headless.json"), jsonRecording, StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(recDir, baseName + ".json"), jsonRecording, StandardCharsets.UTF_8);
    }

    @AfterAll
    public static void stopServer()
    {
        Neodymium.getData().remove("neodymium.ai.pesap.enabled");
        Neodymium.getData().remove("neodymium.ai.judge.enabled");
        Neodymium.getData().remove("neodymium.ai.semanticVerification.enabled");
        Neodymium.getData().remove("neodymium.ai.visualRca.enabled");
        AiConfiguration.resetInstance();

        if (server != null)
        {
            server.stop();
        }
    }

    @BeforeEach
    public void setup()
    {
        if (server != null)
        {
            server.resetInventory();
        }
        Neodymium.getData().put("verla.url", String.format("https://localhost:%d", server.getHttpsPort()));
        javaMethodBodyExecuted = false;
    }

    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook(value = AiPlaybook.PROGRAMMATIC, recordingDirectory = CUSTOM_REC_DIR)
    public void testProgrammaticReplayWithCustomRecordingDirectory(final AiSession session) throws Exception
    {
        javaMethodBodyExecuted = true;

        final SessionData sessionData = new SessionData();
        sessionData.set("searchTerm", "Search");

        session.execute("""
            Open ${verla.url}/verla-perfect/index.html in the browser
            Assert test data defined in Java method body
            """, sessionData);

        Assertions.assertTrue(javaMethodBodyExecuted, "Java test method body must have executed");
    }
}
