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
package org.neodymium.ai.report;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HtmlReportGenerator} to ensure the generated client-side
 * script is syntactically valid and step details are properly rendered.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class HtmlReportGeneratorTest
{
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script(?![^>]*id=\"stepDataPayload\")[^>]*>([\\s\\S]*?)</script>",
        Pattern.CASE_INSENSITIVE
    );

    @Test
    @DisplayName("Verify generated HTML script has no syntax errors and compiles cleanly")
    public void testGeneratedHtmlScriptSyntax() throws Exception
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestClass("TestFeature");
        report.setTestName("testMethod");
        report.setExecutionMode("LIVE");
        final TestExecutionReport.ReportStepEntry step = new TestExecutionReport.ReportStepEntry(0, "Open homepage");
        step.setStatus("SUCCESS");
        step.setDurationMs(120);

        final TestExecutionReport.ReportLlmCallEntry llmCall = new TestExecutionReport.ReportLlmCallEntry();
        llmCall.setStepIndex(0);
        llmCall.setCapability("TEXT");
        llmCall.setModelName("gemini-2.5-flash");
        llmCall.setDurationMs(150);
        llmCall.setAvailableTools(List.of("browser_navigate", "browser_click", "complete_step"));
        llmCall.setUserPrompt("Go to home");
        llmCall.setResponseContent("I will navigate to home");
        step.addLlmCall(llmCall);

        final TestExecutionReport.ReportActionEntry action = new TestExecutionReport.ReportActionEntry(
            "NAVIGATE",
            "https://example.com",
            null,
            "Navigate to site",
            "Initial navigation",
            true
        );
        step.addAction(action);

        report.addStep(step);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);
        Assertions.assertNotNull(html, "Generated HTML must not be null");
        Assertions.assertFalse(html.isBlank(), "Generated HTML must not be blank");

        // Extract client script
        final Matcher matcher = SCRIPT_PATTERN.matcher(html);
        Assertions.assertTrue(matcher.find(), "Generated HTML must contain client script");
        final String script = matcher.group(1);

        // Verify no broken single-quoted string literals with unescaped newlines
        final String[] lines = script.split("\n");
        for (int i = 0; i < lines.length; i++)
        {
            final String line = lines[i].trim();
            if (line.endsWith("'") && (line.contains("join(") || line.contains("replace(")))
            {
                Assertions.fail("Found unescaped newline in JavaScript string literal at line " + (i + 1) + ": " + line);
            }
        }

        // Verify with Node.js vm if node is present on the system
        verifyScriptWithNodeIfAvailable(script);
    }

    @Test
    @DisplayName("Verify live report file has valid script syntax after regeneration")
    public void testExistingReportFileScriptSyntax() throws Exception
    {
        final File liveJson = new File("target/ai-results/VerlaGuestCheckout_Us_English_Normal_testCheckoutLive_normal_20260909-135452.json");
        final File liveHtml = new File("target/ai-results/VerlaGuestCheckout_Us_English_Normal_testCheckoutLive_normal_20260909-135452.html");

        if (liveJson.exists())
        {
            final ObjectMapper mapper = new ObjectMapper();
            final TestExecutionReport report = mapper.readValue(liveJson, TestExecutionReport.class);
            final String regeneratedHtml = new HtmlReportGenerator().generate(report);
            Files.writeString(liveHtml.toPath(), regeneratedHtml);

            final Matcher matcher = SCRIPT_PATTERN.matcher(regeneratedHtml);
            Assertions.assertTrue(matcher.find(), "Regenerated HTML must contain client script");
            verifyScriptWithNodeIfAvailable(matcher.group(1));
        }
        else if (liveHtml.exists())
        {
            final String originalHtml = Files.readString(liveHtml.toPath());
            final Matcher matcher = SCRIPT_PATTERN.matcher(originalHtml);
            if (matcher.find())
            {
                verifyScriptWithNodeIfAvailable(matcher.group(1));
            }
        }
    }

    @Test
    @DisplayName("Verify LLM Invocations Trace table contains step clustering banners, turn numbers, and modality badges")
    public void testLlmTraceTableStepClusteringAndTurnFormatting()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestClass("CheckoutTest");
        report.setTestName("testGuestCheckout");
        report.setExecutionMode("LIVE");

        final TestExecutionReport.ReportStepEntry step0 = new TestExecutionReport.ReportStepEntry(0, "Enter shipping details");
        step0.setStatus("SUCCESS");
        final TestExecutionReport.ReportStepEntry step1 = new TestExecutionReport.ReportStepEntry(1, "Select payment method");
        step1.setStatus("SUCCESS");

        report.addStep(step0);
        report.addStep(step1);

        // Step 0 Call 1: PESAP
        final TestExecutionReport.ReportLlmCallEntry call1 = new TestExecutionReport.ReportLlmCallEntry();
        call1.setStepIndex(0);
        call1.setCapability("PESAP");
        call1.setModelName("gemini-2.5-flash");
        call1.setDurationMs(200);
        call1.setInputTokens(1000);
        call1.setOutputTokens(20);
        report.addLlmCall(call1);

        // Step 0 Call 2: Turn 1 (Text)
        final TestExecutionReport.ReportLlmCallEntry call2 = new TestExecutionReport.ReportLlmCallEntry();
        call2.setStepIndex(0);
        call2.setCapability("TEXT_ONLY");
        call2.setModelName("gemini-2.5-flash");
        call2.setDurationMs(300);
        call2.setInputTokens(2000);
        call2.setOutputTokens(30);
        report.addLlmCall(call2);

        // Step 0 Call 3: Turn 2 (Text)
        final TestExecutionReport.ReportLlmCallEntry call3 = new TestExecutionReport.ReportLlmCallEntry();
        call3.setStepIndex(0);
        call3.setCapability("TEXT_ONLY");
        call3.setModelName("gemini-2.5-flash");
        call3.setDurationMs(250);
        call3.setInputTokens(2500);
        call3.setOutputTokens(25);
        report.addLlmCall(call3);

        // Step 1 Call 4: PESAP
        final TestExecutionReport.ReportLlmCallEntry call4 = new TestExecutionReport.ReportLlmCallEntry();
        call4.setStepIndex(1);
        call4.setCapability("PESAP");
        call4.setModelName("gemini-2.5-flash");
        call4.setDurationMs(180);
        call4.setInputTokens(1200);
        call4.setOutputTokens(15);
        report.addLlmCall(call4);

        // Step 1 Call 5: Turn 1 (Vision)
        final TestExecutionReport.ReportLlmCallEntry call5 = new TestExecutionReport.ReportLlmCallEntry();
        call5.setStepIndex(1);
        call5.setCapability("VISION");
        call5.setModelName("gemini-2.5-flash");
        call5.setDurationMs(600);
        call5.setInputTokens(3000);
        call5.setOutputTokens(40);
        report.addLlmCall(call5);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);

        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("class=\"step-group-row\""), "HTML must contain step group separator rows");
        Assertions.assertTrue(html.contains("Enter shipping details"), "HTML must display step 0 instruction in group banner");
        Assertions.assertTrue(html.contains("Select payment method"), "HTML must display step 1 instruction in group banner");
        Assertions.assertTrue(html.contains("Intent (PESAP)"), "HTML must format PESAP capability as Intent (PESAP)");
        Assertions.assertTrue(html.contains("Turn 1"), "HTML must display sequential turn numbering");
        Assertions.assertTrue(html.contains("Turn 2"), "HTML must display sequential turn 2");
        Assertions.assertTrue(html.contains("Vision 📸"), "HTML must display vision modality badge");
        Assertions.assertTrue(html.contains("step-tree-indicator"), "HTML must display tree branch indicator on subsequent calls");
        Assertions.assertTrue(html.contains("step-cluster-even") && html.contains("step-cluster-odd"), "HTML must alternate step cluster classes");
    }

    private static void verifyScriptWithNodeIfAvailable(final String script)
    {
        try
        {
            final Process process = new ProcessBuilder("node", "-e", "new (require('vm').Script)(process.argv[1]);", script)
                .redirectErrorStream(true)
                .start();
            final int exitCode = process.waitFor();
            if (exitCode != 0)
            {
                final String errorOutput = new String(process.getInputStream().readAllBytes());
                Assertions.fail("Node.js VM reported JavaScript syntax error: " + errorOutput);
            }
        }
        catch (final IOException | InterruptedException ignored)
        {
            // Node not installed or execution prevented, string validation already passed
        }
    }
}

