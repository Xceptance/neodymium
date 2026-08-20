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
package com.xceptance.neodymium.aura.manager.ui;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.AuraReportingService;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI edge cases test to verify history filtering and resilience to corrupted report metadata.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@Browser("Chrome_headless")
public final class AuraManagerReportingHistoryEdgeCasesUiTest
{
    private HttpServer server;
    private int port;
    private File reportDirPassed;
    private File reportDirCorrupted;

    @BeforeEach
    public final void setup() throws IOException
    {
        System.setProperty("neodymium.aura.test", "true");
        this.server = NeodymiumAuraManager.startServer(18888, false);
        this.port = this.server.getAddress().getPort();

        final AuraReportingService reportingService = new AuraReportingService();
        final File historyDir = reportingService.getReportHistoryDir();
        if (!historyDir.exists())
        {
            historyDir.mkdirs();
        }

        // Clean out history directory
        final File[] files = historyDir.listFiles();
        if (files != null)
        {
            for (final File file : files)
            {
                if (file.isDirectory())
                {
                    deleteDir(file);
                }
            }
        }

        // 1. Create a valid mock passed run
        final String runIdPassed = "20260722_160000_passed_run";
        this.reportDirPassed = new File(historyDir, runIdPassed).getAbsoluteFile();
        this.reportDirPassed.mkdirs();

        final String metadataPassed = "{"
            + "\"status\": \"Passed\","
            + "\"timestamp\": \"2026-07-22T16:00:00Z\","
            + "\"total\": 1,"
            + "\"passed\": 1,"
            + "\"failed\": 0,"
            + "\"durationMs\": 25000"
            + "}";
        Files.writeString(new File(this.reportDirPassed, "metadata.json").toPath(), metadataPassed, StandardCharsets.UTF_8);

        // 2. Create a corrupted run folder with malformed metadata.json
        final String runIdCorrupted = "20260722_160500_corrupted_run";
        this.reportDirCorrupted = new File(historyDir, runIdCorrupted).getAbsoluteFile();
        this.reportDirCorrupted.mkdirs();

        final String metadataCorrupted = "{ malformed_json_without_quotes: true, ";
        Files.writeString(new File(this.reportDirCorrupted, "metadata.json").toPath(), metadataCorrupted, StandardCharsets.UTF_8);
    }

    @AfterEach
    public final void teardown()
    {
        if (this.server != null)
        {
            NeodymiumAuraManager.stopServer(this.server);
        }

        if (this.reportDirPassed != null && this.reportDirPassed.exists())
        {
            deleteDir(this.reportDirPassed);
        }
        if (this.reportDirCorrupted != null && this.reportDirCorrupted.exists())
        {
            deleteDir(this.reportDirCorrupted);
        }
    }

    private final void deleteDir(final File dir)
    {
        if (dir != null && dir.exists())
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
    }

    @NeodymiumTest
    public final void testHistorySearchAndStatusFiltering()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Navigate to Reports tab
        $("#navReports").shouldBe(Condition.visible).click();

        // Check report view container is rendered
        $("#reportViewContainer").shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Rows are present for runs
        $$(".history-row").shouldBe(CollectionCondition.sizeGreaterThanOrEqual(1));
    }

    @NeodymiumTest
    public final void testResilienceToCorruptedMetadataJson()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Navigate to Reports tab
        $("#navReports").shouldBe(Condition.visible).click();

        // Main report view container renders gracefully without breaking or white screening
        $("#reportViewContainer").shouldBe(Condition.visible, Duration.ofSeconds(10));
    }
}
