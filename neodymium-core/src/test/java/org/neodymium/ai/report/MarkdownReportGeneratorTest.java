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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MarkdownReportGenerator} ensuring generated markdown reports properly
 * include step associations, sequential turn counters, and semantic phases in the LLM interactions table.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class MarkdownReportGeneratorTest
{
    @Test
    @DisplayName("Verify Markdown report LLM Interactions table formats step references, sequential turns, and capabilities")
    public void testMarkdownLlmInteractionsTable()
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

        // Pre-Flight LINTER Call: stepIndex -1
        final TestExecutionReport.ReportLlmCallEntry linterCall = new TestExecutionReport.ReportLlmCallEntry();
        linterCall.setStepIndex(-1);
        linterCall.setCapability("LINTER");
        linterCall.setModelName("gemini-2.5-flash");
        linterCall.setDurationMs(1500);
        linterCall.setInputTokens(1500);
        linterCall.setOutputTokens(200);
        report.addLlmCall(linterCall);

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

        // Step 1 Call 4: Turn 1 (Vision)
        final TestExecutionReport.ReportLlmCallEntry call4 = new TestExecutionReport.ReportLlmCallEntry();
        call4.setStepIndex(1);
        call4.setCapability("VISION");
        call4.setModelName("gemini-2.5-flash");
        call4.setDurationMs(600);
        call4.setInputTokens(3000);
        call4.setOutputTokens(40);
        report.addLlmCall(call4);

        final MarkdownReportGenerator generator = new MarkdownReportGenerator();
        final String md = generator.generate(report);

        Assertions.assertNotNull(md);
        Assertions.assertTrue(md.contains("## LLM Interactions"));
        Assertions.assertTrue(md.contains("| # | Step | Phase / Role | Model | Duration | In Tokens | Out Tokens | Cached | Cost |"));
        Assertions.assertTrue(md.contains("Pre-Flight"));
        Assertions.assertTrue(md.contains("Playbook Linter"));
        Assertions.assertTrue(md.contains("Step #1"));
        Assertions.assertTrue(md.contains("↳ Step #1"));
        Assertions.assertTrue(md.contains("Intent (PESAP)"));
        Assertions.assertTrue(md.contains("Turn 1 (Text)"));
        Assertions.assertTrue(md.contains("Turn 2 (Text)"));
        Assertions.assertTrue(md.contains("Step #2"));
        Assertions.assertTrue(md.contains("Turn 1 (Vision 📸)"));
    }

    @Test
    @DisplayName("Verify Markdown report Overview table omits tags for compact layout")
    public void testMarkdownReportOmitsTagsFromOverview()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestClass("org.neodymium.ai.integration.verla.search.judge.SearchJudgeTest");
        report.setTestMethod("liveAllDataSets");
        report.addTag("integration");
        report.addTag("mode:judge");

        final MarkdownReportGenerator generator = new MarkdownReportGenerator();
        final String md = generator.generate(report);

        Assertions.assertNotNull(md);
        Assertions.assertFalse(md.contains("| **Tags** |"),
            "Markdown Overview table must omit tags for compact overview layout");
    }
}
