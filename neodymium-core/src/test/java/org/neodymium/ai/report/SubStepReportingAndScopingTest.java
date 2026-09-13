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
package org.neodymium.ai.report;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.StepStats;

/**
 * Unit tests validating sub-step stats merging when runtime variables are resolved,
 * consistent parent LLM call count aggregation, and scoping context visibility in HTML reports.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class SubStepReportingAndScopingTest
{
    @TempDir
    Path tempFolder;

    /**
     * Default constructor.
     */
    public SubStepReportingAndScopingTest()
    {
    }

    /**
     * Tests that a sub-step whose instruction was resolved at runtime (e.g. replacing '${cnt}' with '5')
     * is correctly matched and has its StepStats merged into the report entry and parent totals.
     */
    @Test
    @DisplayName("Verify sub-step stats merge properly when runtime variable substitution changes the instruction")
    public void testSubStepStatsMergingWithVariableInterpolation() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-substep-scoping");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        final PlaybookStep parentStep = new PlaybookStep("Locate the first product card");
        final PlaybookStep subStep1 = new PlaybookStep("Capture line count in 'cnt'");
        subStep1.setParent(parentStep);
        final PlaybookStep subStep2 = new PlaybookStep("Verify count is higher than ${cnt}");
        subStep2.setParent(parentStep);

        parentStep.setSubSteps(List.of(subStep1, subStep2));

        // Start parent step
        bus.dispatch(new StepStartedEvent(parentStep, 0));

        // Sub-step 1 execution
        bus.dispatch(new StepStartedEvent(subStep1, 0));
        bus.dispatch(new StepFinishedEvent(subStep1, PlaybookStepStatus.SUCCESS));

        // Sub-step 2 execution
        bus.dispatch(new StepStartedEvent(subStep2, 1));
        bus.dispatch(new StepFinishedEvent(subStep2, PlaybookStepStatus.SUCCESS));

        // Parent step finishes
        bus.dispatch(new StepFinishedEvent(parentStep, PlaybookStepStatus.SUCCESS));

        // Sub-stats
        final StepStats parentStats = new StepStats("Locate the first product card", System.currentTimeMillis());
        final StepStats subStats1 = new StepStats("Capture line count in 'cnt'", System.currentTimeMillis());
        subStats1.addPesapCall(100, 20, 0);
        subStats1.addStandardCall(200, 40, 0);
        subStats1.setDurationMs(1200);

        // SubStats 2 has runtime resolved instruction
        final StepStats subStats2 = new StepStats("Verify count is higher than 5", System.currentTimeMillis());
        subStats2.addPesapCall(100, 20, 0);
        subStats2.addStandardCall(200, 40, 0);
        subStats2.setDurationMs(1500);

        parentStats.getSubStats().add(subStats1);
        parentStats.getSubStats().add(subStats2);

        final ExecutionContext ctx = new ExecutionContext(null);
        ctx.getTransientData().put("execution.stepStatsList", List.of(parentStats));
        ExecutionContext.setActiveContext(ctx);

        try
        {
            bus.dispatch(new SessionFinishedEvent(3000, true));

            final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");
            assertTrue(Files.exists(htmlPath), "HTML report should be generated");

            final String html = Files.readString(htmlPath);

            // Parent step should show 4 LLM calls (1 PESAP + 1 Standard) * 2 = 4 (or 2 pesap + 2 standard)
            assertTrue(html.contains("4 LLM call(s)"), "Parent card footer should display 4 LLM calls aggregated from sub-steps");

            // Sub-steps should have scope badges
            assertTrue(html.contains("scope-badge"), "HTML report should contain scope-badge for sub-steps");
            assertTrue(html.contains("📍 Locate the first product card"), "Sub-steps should display parent scoping header");
            assertTrue(html.contains("inspScopeContext"), "Inspector header should contain scoping context element");
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }

    /**
     * Tests that HtmlReportGenerator calculates parent LLM call counts consistently from its sub-steps
     * when the parent step has no direct LLM calls attached.
     */
    @Test
    @DisplayName("Verify HtmlReportGenerator parent step LLM count aggregates sub-step calls consistently")
    public void testHtmlReportGeneratorParentCallCountConsistency()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestClass("TestClass");
        report.setTestMethod("testMethod");
        final TestExecutionReport.ReportStepEntry parent = new TestExecutionReport.ReportStepEntry(0, "Locate product card");

        final TestExecutionReport.ReportStepEntry sub1 = new TestExecutionReport.ReportStepEntry(0, "Hover over it");
        sub1.setPesapCalls(1);
        sub1.setStandardCalls(2);

        final TestExecutionReport.ReportStepEntry sub2 = new TestExecutionReport.ReportStepEntry(1, "Click its add button");
        sub2.setPesapCalls(1);
        sub2.setStandardCalls(2);

        parent.addSubStep(sub1);
        parent.addSubStep(sub2);

        // Parent pesap/standard calls aggregated
        parent.setPesapCalls(2);
        parent.setStandardCalls(4);

        report.addStep(parent);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);
        assertNotNull(html);

        // Verify parent footer displays 6 LLM calls
        assertTrue(html.contains("6 LLM call(s)"), "Parent step card should display 6 LLM calls");
        // Verify scope badge is present
        assertTrue(html.contains("📍 Locate product card"), "Sub-step should display parent scope chip");
    }
}
