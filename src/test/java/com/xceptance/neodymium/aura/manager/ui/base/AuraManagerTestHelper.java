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
package com.xceptance.neodymium.aura.manager.ui.base;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.AuraReportingService;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import org.neodymium.util.Neodymium;
import org.junit.jupiter.api.Assertions;
import org.neodymium.ai.executor.selenide.plugins.AiMethod;
import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Static test helper class for Aura Manager UI tests.
 * Declares all helper methods as public static and annotates them with @AiMethod.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerTestHelper
{
    private static HttpServer auraServer;
    private static int auraPort;
    private static int startPort = 18150;

    private AuraManagerTestHelper()
    {
        // Prevent instantiation
    }

    public static void setupMockHistoryReport() throws IOException
    {
        final AuraReportingService reportingService = new AuraReportingService();
        final File historyDir = reportingService.getReportHistoryDir();
        if (!historyDir.exists())
        {
            historyDir.mkdirs();
        }

        final String runId = "20260803_120000_layout_test_run";
        final File reportDir = new File(historyDir, runId).getAbsoluteFile();
        if (!reportDir.exists())
        {
            reportDir.mkdirs();

            final String metadataJson = """
                {
                  "status": "Passed",
                  "timestamp": "2026-08-03T12:00:00Z",
                  "total": 1,
                  "passed": 1,
                  "failed": 0,
                  "durationMs": 15000,
                  "headless": true,
                  "allureEnabled": true,
                  "videoEnabled": false
                }""";
            Files.writeString(new File(reportDir, "metadata.json").toPath(), metadataJson, StandardCharsets.UTF_8);

            final String executionJson = """
                {
                  "testId": "T101",
                  "testName": "Layout Blueprint Verification Test",
                  "status": "Passed",
                  "browser": "Chrome",
                  "stats": {
                    "durationMs": 15000
                  },
                  "yamlSource": "tests/layout_test.yaml",
                  "playbookMode": "false",
                  "steps": [
                    {
                      "index": 0,
                      "step": "Open homepage and verify header",
                      "status": "passed",
                      "action": "open",
                      "target": "http://localhost",
                      "durationMs": 1200
                    },
                    {
                      "index": 1,
                      "step": "Click navigation menu item",
                      "status": "passed",
                      "action": "click",
                      "target": "#navReports",
                      "durationMs": 850
                    }
                  ]
                }""";
            Files.writeString(new File(reportDir, "console-execution-layout.json").toPath(), executionJson, StandardCharsets.UTF_8);
        }
    }

    public static void setStartPort(final int port)
    {
        startPort = port;
    }

    public static HttpServer getAuraServer()
    {
        return auraServer;
    }

    public static void startManager() throws IOException
    {
        if (auraServer != null)
        {
            return;
        }
        setupMockHistoryReport();
        final File targetFile = new File("src/test/resources/automated-workspace-test.yaml").getAbsoluteFile();
        final String testName = Neodymium.getTestName();
        if (testName != null)
        {
            if (testName.contains("testCreateTest") || testName.contains("Create_Test"))
            {
                if (targetFile.exists())
                {
                    Files.delete(targetFile.toPath());
                }
            }
            else if (testName.contains("testDeleteTest") || testName.contains("Delete_Test"))
            {
                if (!targetFile.exists())
                {
                    final String boilerplate = """
                        # Neodymium YAML Test Data File
                        steps: |
                          Open browser
                        data:
                          - testId: "Automated Workspace Test"
                        """;
                    Files.writeString(targetFile.toPath(), boilerplate, StandardCharsets.UTF_8);
                }
            }
        }

        // Start manager on dynamic port
        auraServer = NeodymiumAuraManager.startServer(startPort, true);
        auraPort = auraServer.getAddress().getPort();
        Neodymium.getData().put("auraManagerUrl", "http://127.0.0.1:" + auraPort);
        System.out.println("Started AuraManager on http://127.0.0.1:" + auraPort);
        try
        {
            Thread.sleep(1500);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public static void stopManager()
    {
        // No-op for AI Playbook. Server must stay alive across phases.
    }

    public static void forceStopManager()
    {
        if (auraServer != null)
        {
            NeodymiumAuraManager.stopServer(auraServer);
            auraServer = null;
            try
            {
                Thread.sleep(500);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    @AiMethod("Confirms and accepts any active browser alert prompt")
    public static void acceptAlert()
    {
        Selenide.confirm();
    }

    @AiMethod("Verifies that the checkbox element targeted by the selector is checked")
    public static void verifyCheckboxChecked(final String selector)
    {
        Assertions.assertTrue(Selenide.$(selector).isSelected());
    }

    @AiMethod("Verifies that the checkbox element targeted by the selector is unchecked")
    public static void verifyCheckboxUnchecked(final String selector)
    {
        Assertions.assertFalse(Selenide.$(selector).isSelected());
    }

    @AiMethod("Verifies that the automated-workspace-test.yaml file is present in the workspace file list")
    public static void verifyAutomatedWorkspaceTestPresent()
    {
        Selenide.$("#yamlFileList").shouldHave(Condition.text("automated-workspace-test.yaml"));
    }

    @AiMethod("Verifies that the automated-workspace-test.yaml file is not present in the workspace file list")
    public static void verifyAutomatedWorkspaceTestNotPresent()
    {
        Selenide.$("#yamlFileList").shouldNot(Condition.text("automated-workspace-test.yaml"));
    }

    @AiMethod("Deletes the test file automated-workspace-test.yaml from disk")
    public static void deleteWorkspaceTestFile() throws IOException
    {
        final File targetFile = new File("src/test/resources/automated-workspace-test.yaml").getAbsoluteFile();
        if (targetFile.exists())
        {
            Files.delete(targetFile.toPath());
        }
    }

    public static void createWorkspaceTestFile() throws IOException
    {
        final File targetFile = new File("src/test/resources/automated-workspace-test.yaml").getAbsoluteFile();
        if (!targetFile.exists())
        {
            final String boilerplate = """
                # Neodymium YAML Test Data File
                steps: |
                  Open browser
                data:
                  - testId: "Automated Workspace Test"
                """;
            Files.writeString(targetFile.toPath(), boilerplate, StandardCharsets.UTF_8);
        }
    }
}
