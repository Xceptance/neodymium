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
package com.xceptance.neodymium.ai.integration;
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;

import static com.codeborne.selenide.Selenide.open;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;
import com.xceptance.neodymium.ai.core.AiExecutionResult;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.ai.util.LogCaptureAppender;
import com.xceptance.neodymium.ai.core.AiStats;
import com.xceptance.neodymium.ai.playbook.Playbook;

/**
 * Verifies the advanced multi-dimensional feature matrix of Aura AI:
 * 1. Multiplication of tests across Chrome viewports.
 * 2. Multi-port setup executing secure HTTPS (self-signed) and insecure HTTP tests.
 * 3. Dynamic dataset placeholder injections and execution latency timings.
 * 4. Multimodal AI image contents evaluation.
 * 5. Replay validation comparing LLM baseline checks against instant offline dHash caches.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
@AiTestVerification({
    VerificationMode.LIVE_LLM,
    VerificationMode.OFFLINE_REPLAY,
    VerificationMode.HUD_OFFLINE_REPLAY,
    VerificationMode.HUD_LLM
})
/**
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class FeatureMatrixTest extends BaseAiTest
{
    private String httpShopUrl;
    private String httpsShopUrl;
    private String httpDashboardUrl;
    private String httpsDashboardUrl;
    private String httpFormsUrl;
    private String httpsFormsUrl;
    private LogCaptureAppender logAppender;

    /**
     * Resolves the multi-port HTTP and HTTPS path URLs before each test runs.
     * 
     * @param testInfo the JUnit 5 test information
     */
    @BeforeEach
    public void setupMultiPortUrls(final TestInfo testInfo)
    {
        final int httpPort = server.getPort();
        final int httpsPort = server.getHttpsPort();

        // HTTP URLs
        httpShopUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/index.html", httpPort);
        httpDashboardUrl = String.format("http://localhost:%d/AuraGlanceTest/dashboard/index.html", httpPort);
        httpFormsUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/forms.html", httpPort);

        // HTTPS secure URLs (reusing self-signed certificates keystore.p12)
        httpsShopUrl = String.format("https://localhost:%d/AuraGlanceTest/shop/index.html", httpsPort);
        httpsDashboardUrl = String.format("https://localhost:%d/AuraGlanceTest/dashboard/index.html", httpsPort);
        httpsFormsUrl = String.format("https://localhost:%d/AuraGlanceTest/shop/forms.html", httpsPort);

        // Start capturing logs in parallel
        logAppender = LogCaptureAppender.startCapture();

    }

    /**
     * Clean up the log appender after each test.
     */
    @AfterEach
    public void tearDownLogCapture()
    {
        if (logAppender != null)
        {
            logAppender.stopCapture();
        }
    }

    /**
     * Verifies dynamic dataset placeholder resolution, secure HTTPS serving,
     * and records test timing metrics.
     */
    @NeodymiumTest
    @DataSet(id = "ShopSetup")
    public void testDataResolutionAndTiming()
    {
        final long startTime = System.currentTimeMillis();

        final Runnable steps = () ->
        {
            // Open secure HTTPS forms sub-application (Chrome ignores cert warning via args)
            open(httpsFormsUrl);

            // Verify dynamic resolution of placeholders injected from FeatureMatrixTest.json
            Neodymium.ai().execute("Type '${userFullName}' into the 'Full Name' field.");
            Neodymium.ai().execute("Type '${userEmail}' into the 'Email Address' field.");
            Neodymium.ai().execute("Click the 'Create Account' button.");
            Neodymium.ai().execute("Verify that the success indicator 'Account Registered!' is visible.");
        };

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertThat(r1)
            .hasPesapCalls(4)
            .hasNoEscalations();

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.OFFLINE_REPLAY);
        assertThat(r2)
            .hasNoPesapCalls();

        // Measure execution latency
        final long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed > 0, "Execution duration should be positive");

        // 1. Programmatic assertions on internal stats
        final AiStats stats = Neodymium.ai().getStats();
        assertTrue(stats.getOverallCallCount() > 0 || stats.getReplayCount() > 0, "Expected either LLM calls or offline replays to be recorded");

        // 2. Programmatic assertions on captured parallel log output
        final List<String> capturedLogs = logAppender.getLogs();
        final boolean hasAuraIndicator = capturedLogs.stream()
                .anyMatch(line -> line.contains("Aura") || line.contains("Tokens") || line.contains("Observe") || line.contains("Type") || line.contains("Click"));
        assertTrue(hasAuraIndicator, "Expected logs to capture Aura framework trace lines");
    }

    /**
     * Verifies that the multimodal AI auditor can perform graphic element evaluations,
     * inspecting raw image context rather than just bounding bounds.
     */
    @NeodymiumTest
    @DataSet(id = "ShopSetup")
    public void testImageChecksWithAI()
    {
        final Runnable steps = () ->
        {
            open(httpShopUrl);

            // Direct the visual linter to audit raw artwork characteristics
            Neodymium.ai().execute("Assert that the 'Aura Neon Gradient Poster' card displays an image showcasing deep ultraviolet and cyan shades.");
        };

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertThat(r1)
            .hasPesapCalls(1)
            .hasNoEscalations();

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.OFFLINE_REPLAY);
        assertThat(r2)
            .hasNoPesapCalls();
    }

    /**
     * Verifies double-stage replay execution:
     * Stage 1: Runs with LLM to build baseline cache hashes.
     * Stage 2: Replays subsequent visual assertions completely offline without calling network.
     */
    @NeodymiumTest
    @DataSet(id = "ShopSetup")
    public void testReplayOfflineVerify()
    {
        open(httpShopUrl);

        // Stage 1: Live visual check (generates local dHash baseline cache)
        Neodymium.ai().execute("Observe page visual consistency (visual)");
        final AiExecutionResult r1 = Neodymium.getLastAiExecutionResult();
        assertThat(r1).hasPesapCalls(1);

        final Playbook playbook = Neodymium.getAiPlaybook();
        if (playbook != null)
        {
            playbook.setRecording(false);
            playbook.setCursor(0);
        }

        final long replayStart = System.nanoTime();

        // Stage 2: Replay the identical step (succeeds instantly offline via cache)
        Neodymium.ai().execute("Observe page visual consistency (visual)");
        final AiExecutionResult r2 = Neodymium.getLastAiExecutionResult();
        assertThat(r2).hasNoPesapCalls();

        final long durationUs = (System.nanoTime() - replayStart) / 1000;
        assertTrue(durationUs < 1000000, "Offline cached replay check exceeded time boundary: " + durationUs + " us");
    }
}
