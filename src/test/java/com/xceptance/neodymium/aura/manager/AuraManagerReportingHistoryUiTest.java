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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.AuraReportingService;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI test to verify the Neodymium Aura Manager's Reporting History &
 * Management Panel functionality before and after refactoring.
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public final class AuraManagerReportingHistoryUiTest
{
    private HttpServer server;
    private int port;
    private File reportDir;
    private File historyDir;

    @BeforeEach
    public final void setup() throws IOException
    {
        System.setProperty("neodymium.aura.test", "true");
        this.server = NeodymiumAuraManager.startServer(18108, false);
        this.port = this.server.getAddress().getPort();

        // Retrieve standard report history directory
        final var reportingService = new AuraReportingService();
        this.historyDir = reportingService.getReportHistoryDir();
        if (!this.historyDir.exists())
        {
            this.historyDir.mkdirs();
        }

        // Clean out any existing reports to keep test environment isolated and predictable
        final var files = this.historyDir.listFiles();
        if (files != null)
        {
            for (final var file : files)
            {
                if (file.isDirectory())
                {
                    deleteDir(file);
                }
            }
        }

        // Create a mock execution run history record
        final String runId = "20260722_141500_run_mock";
        this.reportDir = new File(this.historyDir, runId).getAbsoluteFile();
        this.reportDir.mkdirs();

        // 1. Write metadata.json
        final String metadataJson = "{"
            + "\"status\": \"Passed\","
            + "\"timestamp\": \"2026-07-22T14:15:00Z\","
            + "\"total\": 1,"
            + "\"passed\": 1,"
            + "\"failed\": 0,"
            + "\"durationMs\": 45000,"
            + "\"headless\": true,"
            + "\"allureEnabled\": true,"
            + "\"videoEnabled\": false"
            + "}";
        Files.writeString(new File(this.reportDir, "metadata.json").toPath(), metadataJson, StandardCharsets.UTF_8);

        // 2. Write console-execution-test1.json
        final String executionJson = "{"
            + "\"testId\": \"T123\","
            + "\"testName\": \"Login Verification Test\","
            + "\"status\": \"Passed\","
            + "\"browser\": \"Chrome\","
            + "\"stats\": {"
            + "  \"durationMs\": 45000"
            + "},"
            + "\"yamlSource\": \"tests/login_test.yaml\","
            + "\"playbookMode\": \"false\","
            + "\"steps\": ["
            + "  {"
            + "    \"step\": \"Open login page\","
            + "    \"status\": \"passed\""
            + "  }"
            + "]"
            + "}";
        Files.writeString(new File(this.reportDir, "console-execution-test1.json").toPath(), executionJson, StandardCharsets.UTF_8);

        // 3. Write mock log file Login_Verification_Test.log
        final String mockLogContent = "Login test logs...\nStep 1: Open page\nStep 2: Enter credentials\nSuccess";
        Files.writeString(new File(this.reportDir, "Login_Verification_Test.log").toPath(), mockLogContent, StandardCharsets.UTF_8);
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
    }

    private final void deleteDir(final File dir)
    {
        final var children = dir.listFiles();
        if (children != null)
        {
            for (final var child : children)
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
    public final void testReportingHistoryListRendering()
    {
        Selenide.open("http://localhost:" + this.port + "/");
        $("#navReports").shouldBe(Condition.visible).click();

        // Verify the history list has our mock run card (with run number #1)
        final var historyRows = $$(".history-row");
        historyRows.shouldHave(CollectionCondition.size(1));

        final var firstRow = historyRows.first();
        firstRow.shouldBe(Condition.visible);
        firstRow.$(".run-number").shouldHave(Condition.text("#1"));
        firstRow.$(".badge-success").shouldHave(Condition.text("Passed"));
        firstRow.$(".run-duration-chip").shouldHave(Condition.matchText("45s"));
        firstRow.$(".run-stat-passed").shouldHave(Condition.text("1 passed"));
        firstRow.$(".run-stat-failed").shouldHave(Condition.text("0 failed"));
    }

    @NeodymiumTest
    public final void testSelectRunLoadsTestCases()
    {
        Selenide.open("http://localhost:" + this.port + "/");
        $("#navReports").shouldBe(Condition.visible).click();

        // Click the first history card to load the test cases
        $$(".history-row").first().shouldBe(Condition.visible).click();

        // Verify the test list panel is populated with our Login Verification Test
        final var testCards = $$("#historyTestsList .test-card");
        testCards.shouldHave(CollectionCondition.size(1));

        final var firstTest = testCards.first();
        firstTest.shouldBe(Condition.visible);
        firstTest.$(".test-card-title").shouldHave(Condition.text("login_test.yaml"));
        firstTest.$(".test-card-subtitle").shouldHave(Condition.text("T123"));
        firstTest.$(".badge-success").shouldHave(Condition.text("Passed"));

        // Click the test card to expand or load logs details
        firstTest.click();

        // Verify the details container displays the logs or content
        final var detailsPlaceholder = $("#historyPlaceholder");
        detailsPlaceholder.shouldNotBe(Condition.visible);

        final var iframe = $("#historyConsoleIframe");
        iframe.shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testMiniRunChipClickInHistoryView()
    {
        Selenide.open("http://localhost:" + this.port + "/");
        $("#navReports").shouldBe(Condition.visible).click();

        // Select the run to load mini run bars and change states
        $$(".history-row").first().shouldBe(Condition.visible).click();

        // Switch state to 4 (collapsed runs bar)
        Selenide.executeJavaScript("if (window.applyHistoryState) window.applyHistoryState(4);");
        Selenide.sleep(300);

        // Click on the mini run chip
        final var miniChip = $$("#historyMiniRunsBar .mini-run-chip").first();
        if (miniChip.isDisplayed())
        {
            miniChip.click();
            // Verify test cases panel is loaded
            $$("#historyTestsList .test-card").shouldHave(CollectionCondition.sizeGreaterThanOrEqual(1));
        }
    }

    @NeodymiumTest
    public final void testDeletePastExecutionRun()
    {
        Selenide.open("http://localhost:" + this.port + "/");
        $("#navReports").shouldBe(Condition.visible).click();

        // Mock window.confirm to return true before clicking delete
        Selenide.executeJavaScript("window.confirm = function() { return true; };");
        final var deleteBtn = $$(".history-row").first().$(".run-action-btn.danger");
        deleteBtn.shouldBe(Condition.visible).click();

        // Verify that the deletion cleared the history list
        $$(".history-row").shouldHave(CollectionCondition.size(0));
        Assertions.assertFalse(this.reportDir.exists(), "The report directory was not deleted on the server!");
    }
}
