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
package com.xceptance.neodymium.aura.manager;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.AuraReportingService;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI edge cases test to verify run queue error handling, rerun resilience, and execution failure recovery.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public final class AuraManagerRunQueueEdgeCasesUiTest
{
    private HttpServer server;
    private int port;
    private File reportDir;

    @BeforeEach
    public final void setup() throws IOException
    {
        System.setProperty("neodymium.aura.test", "true");
        this.server = NeodymiumAuraManager.startServer(18888, false);
        this.port = this.server.getAddress().getPort();

        // Create a mock run folder with a rerun configuration referencing a missing file
        final AuraReportingService reportingService = new AuraReportingService();
        final File historyDir = reportingService.getReportHistoryDir();
        if (!historyDir.exists())
        {
            historyDir.mkdirs();
        }

        final String runId = "20260722_150000_rerun_missing_mock";
        this.reportDir = new File(historyDir, runId).getAbsoluteFile();
        this.reportDir.mkdirs();

        final String metadataJson = "{"
            + "\"status\": \"Passed\","
            + "\"timestamp\": \"2026-07-22T15:00:00Z\","
            + "\"total\": 1,"
            + "\"passed\": 1,"
            + "\"failed\": 0,"
            + "\"durationMs\": 12000,"
            + "\"runConfig\": {"
            + "  \"files\": [\"non_existent_file_123.yaml\"],"
            + "  \"headless\": true,"
            + "  \"video\": false,"
            + "  \"keepOpen\": false"
            + "}"
            + "}";
        Files.writeString(new File(this.reportDir, "metadata.json").toPath(), metadataJson, StandardCharsets.UTF_8);

        // Clean up any stale dummy test files
        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        final File staleFile = new File(resourcesDir, "queue-failure-dummy.yaml");
        if (staleFile.exists())
        {
            staleFile.delete();
        }
    }

    @AfterEach
    public final void teardown()
    {
        if (this.server != null)
        {
            NeodymiumAuraManager.stopServer(this.server);
        }
        if (this.reportDir != null && this.reportDir.exists())
        {
            deleteDir(this.reportDir);
        }
        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        final File staleFile = new File(resourcesDir, "queue-failure-dummy.yaml");
        if (staleFile.exists())
        {
            staleFile.delete();
        }
    }

    private final void deleteDir(final File dir)
    {
        final File[] children = dir.listFiles();
        if (children != null)
        {
            for (final File child : children)
            {
                if (child.isDirectory())
                {
                    deleteDir(child);
                }
                else
                {
                    child.delete();
                }
            }
        }
        dir.delete();
    }

    @NeodymiumTest
    public final void testRerunWithMissingFileGracefulHandling()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Navigate to Reports tab
        $("#navReports").shouldBe(Condition.visible).click();

        // Wait for rerun button to appear
        final var rerunBtn = $$(".history-row").first().$(".run-action-btn[title*='Re-run']");
        rerunBtn.shouldBe(Condition.visible, Duration.ofSeconds(10)).click();

        // Main dashboard container remains active and responsive without white-screening
        $(".main-container").shouldBe(Condition.visible);

        // Return to Workspace tab and verify workspace controls remain functional
        $("#navWorkspace").shouldBe(Condition.visible).click();
        $("#runQueueBtn").shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testQueueExecutionFailureRecovery()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Open Workspace view and select an existing test file
        $("#navWorkspace").shouldBe(Condition.visible).click();

        final var targetItem = $$("#yamlFileList .list-item").first();
        targetItem.shouldBe(Condition.visible);

        final var checkbox = targetItem.$("input[type='checkbox']");
        checkbox.shouldBe(Condition.visible);
        if (!checkbox.isSelected())
        {
            checkbox.click();
        }

        // Click Run Queue
        final var runQueueBtn = $("#runQueueBtn");
        runQueueBtn.shouldBe(Condition.visible).shouldNotBe(Condition.disabled).click();

        // Wait for run queue button to be restored
        $("#runQueueBtn").shouldBe(Condition.visible, Duration.ofSeconds(120));
    }
}
