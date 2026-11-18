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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.action.Action;
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
        subStats1.addStandardCall(100, 20, 0);
        subStats1.addStandardCall(200, 40, 0);
        subStats1.setDurationMs(1200);

        // SubStats 2 has runtime resolved instruction
        final StepStats subStats2 = new StepStats("Verify count is higher than 5", System.currentTimeMillis());
        subStats2.addStandardCall(100, 20, 0);
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

            // Parent step should show 4 LLM calls aggregated from sub-steps
            assertTrue(html.contains("4 LLM call(s)"), "Parent card footer should display 4 LLM calls aggregated from sub-steps");

            // Sub-steps should not repeat parent step as a badge in the card header,
            // while the modal inspector preserves scoping context
            assertFalse(html.contains("span class=\"badge-flag scope-badge\""), "Sub-steps card header should not repeat parent step as a scope badge");
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
        sub1.setStandardCalls(3);

        final TestExecutionReport.ReportStepEntry sub2 = new TestExecutionReport.ReportStepEntry(1, "Click its add button");
        sub2.setStandardCalls(3);

        parent.addSubStep(sub1);
        parent.addSubStep(sub2);

        // Parent standard calls aggregated
        parent.setStandardCalls(6);

        report.addStep(parent);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);
        assertNotNull(html);

        // Verify parent footer displays 6 LLM calls
        assertTrue(html.contains("6 LLM call(s)"), "Parent step card should display 6 LLM calls");
        // Verify scope badge is not present in sub-step card header
        assertFalse(html.contains("span class=\"badge-flag scope-badge\""), "Sub-step should not display parent scope badge");
    }

    /**
     * Tests that LLM calls executed during sub-steps cluster under their parent compound step
     * with sub-step notation (e.g. Step #2.1, Step #2.2) and do NOT produce duplicate root step banners.
     */
    @Test
    @DisplayName("Verify compound step sub-step LLM calls cluster under parent without duplicate step banners")
    public void testCompoundStepSubStepLlmTraceGroupingAndNoDuplicateBanners() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-substep-llm-grouping");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON, DiskReportFormat.MARKDOWN), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("VerlaGuestCheckout");
        listener.getReport().setTestMethod("testCheckoutLive");

        // Step 1: simple top-level step
        final PlaybookStep step1 = new PlaybookStep("Open homepage");
        bus.dispatch(new StepStartedEvent(step1, 0));
        final LlmRequest req1 = new LlmRequest("System", "Open homepage", Collections.emptyList(), null, 0.0, 30);
        final LlmResponse resp1 = new LlmResponse("{\"action\":\"OPEN\"}", new TokenUsage(100, 20, 120, 0), "gemini-flash");
        bus.dispatch(new LlmResponseReceivedEvent(req1, resp1, 100, "TEXT"));
        bus.dispatch(new StepFinishedEvent(step1, PlaybookStepStatus.SUCCESS));

        // Step 2: compound step with 2 sub-steps
        final PlaybookStep parentStep2 = new PlaybookStep("Locate the first product card");
        final PlaybookStep subStep21 = new PlaybookStep("Click its 'Add to Cart' button");
        subStep21.setParent(parentStep2);
        final PlaybookStep subStep22 = new PlaybookStep("Click the size 'L' button");
        subStep22.setParent(parentStep2);
        parentStep2.setSubSteps(List.of(subStep21, subStep22));

        // Start parent step 2 (index 1)
        bus.dispatch(new StepStartedEvent(parentStep2, 1));

        // Sub-step 2.1 starts (index 0)
        bus.dispatch(new StepStartedEvent(subStep21, 0));
        final LlmRequest req21 = new LlmRequest("System", "Add to Cart", Collections.emptyList(), null, 0.0, 30);
        final LlmResponse resp21 = new LlmResponse("{\"action\":\"CLICK\"}", new TokenUsage(200, 30, 230, 0), "gemini-flash");
        bus.dispatch(new LlmResponseReceivedEvent(req21, resp21, 150, "TEXT"));
        bus.dispatch(new StepFinishedEvent(subStep21, PlaybookStepStatus.SUCCESS));

        // Sub-step 2.2 starts (index 1)
        bus.dispatch(new StepStartedEvent(subStep22, 1));
        final LlmRequest req22 = new LlmRequest("System", "Click size L", Collections.emptyList(), null, 0.0, 30);
        final LlmResponse resp22 = new LlmResponse("{\"action\":\"CLICK\"}", new TokenUsage(300, 40, 340, 0), "gemini-flash");
        bus.dispatch(new LlmResponseReceivedEvent(req22, resp22, 180, "TEXT"));
        bus.dispatch(new StepFinishedEvent(subStep22, PlaybookStepStatus.SUCCESS));

        // Parent finishes
        bus.dispatch(new StepFinishedEvent(parentStep2, PlaybookStepStatus.SUCCESS));

        // Step 3: simple top-level step
        final PlaybookStep step3 = new PlaybookStep("Proceed to checkout");
        bus.dispatch(new StepStartedEvent(step3, 2));
        final LlmRequest req3 = new LlmRequest("System", "Checkout", Collections.emptyList(), null, 0.0, 30);
        final LlmResponse resp3 = new LlmResponse("{\"action\":\"NAVIGATE\"}", new TokenUsage(400, 50, 450, 0), "gemini-flash");
        bus.dispatch(new LlmResponseReceivedEvent(req3, resp3, 200, "TEXT"));
        bus.dispatch(new StepFinishedEvent(step3, PlaybookStepStatus.SUCCESS));

        bus.dispatch(new SessionFinishedEvent(5000, true));

        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");
        final Path mdPath = reportDir.resolve(listener.getLastBaseFileName() + ".md");
        assertTrue(Files.exists(htmlPath), "HTML report must exist");
        assertTrue(Files.exists(mdPath), "Markdown report must exist");

        final String html = Files.readString(htmlPath);
        final String md = Files.readString(mdPath);

        // Verify report calls attribution
        final List<TestExecutionReport.ReportLlmCallEntry> calls = listener.getReport().getLlmCalls();
        assertEquals(4, calls.size(), "Should have 4 LLM calls in total");

        // Call 0: step 1 (top-level 0)
        assertEquals(0, calls.get(0).getStepIndex());
        assertEquals(-1, calls.get(0).getSubStepIndex());

        // Call 1: subStep 2.1 (parent 1, sub 0)
        assertEquals(1, calls.get(1).getStepIndex());
        assertEquals(0, calls.get(1).getSubStepIndex());

        // Call 2: subStep 2.2 (parent 1, sub 1)
        assertEquals(1, calls.get(2).getStepIndex());
        assertEquals(1, calls.get(2).getSubStepIndex());

        // Call 3: step 3 (top-level 2)
        assertEquals(2, calls.get(3).getStepIndex());
        assertEquals(-1, calls.get(3).getSubStepIndex());

        // Verify HTML group banner count: exactly 3 banners for 3 top-level steps
        final int bannerCount = html.split("<tr class=\"step-group-row\">").length - 1;
        assertEquals(3, bannerCount, "Must contain exactly 3 step group banners (no duplicate Step #1 or Step #2 banners)");

        // Verify sub-step indicators in HTML table
        assertTrue(html.contains("Step #2.1"), "HTML trace table must render Step #2.1 notation");
        assertTrue(html.contains("Step #2.2"), "HTML trace table must render Step #2.2 notation");
        assertTrue(html.contains("openAndSelectStep(1, 0, 'llm')"), "HTML trace table must link to sub-step 0");
        assertTrue(html.contains("openAndSelectStep(1, 1, 'llm')"), "HTML trace table must link to sub-step 1");

        // Verify Markdown report
        assertTrue(md.contains("Step #2.1"), "Markdown trace table must render Step #2.1");
        assertTrue(md.contains("Step #2.2"), "Markdown trace table must render Step #2.2");
    }

    /**
     * Tests that a step containing a conditional include (e.g. 'Add product to cart:' with child
     * '- If ... then _include: ...') records the main step instruction on the parent level,
     * and its subSteps collection contains ONLY the leaf steps from the included fragment.
     */
    @Test
    @DisplayName("Verify conditional include subSteps contains only included fragment steps and excludes main/conditional step")
    public void testConditionalIncludeSubStepsExcludesMainStep() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-conditional-include");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        final PlaybookStep parentStep = new PlaybookStep("Add product to cart");
        final PlaybookStep conditionalStep = new PlaybookStep("If (condition) then _include: fragment.steps else _include: fragment2.steps");
        conditionalStep.setParent(parentStep);
        parentStep.setSubSteps(List.of(conditionalStep));

        final PlaybookStep incStep1 = new PlaybookStep("Set quantity of the product to 1.");
        incStep1.setParent(conditionalStep);
        final PlaybookStep incStep2 = new PlaybookStep("Click add to cart button.");
        incStep2.setParent(conditionalStep);
        conditionalStep.setSubSteps(List.of(incStep1, incStep2));

        // Start parent step
        bus.dispatch(new StepStartedEvent(parentStep, 0));

        // Start included step 1
        bus.dispatch(new StepStartedEvent(incStep1, 0));
        bus.dispatch(new StepFinishedEvent(incStep1, PlaybookStepStatus.SUCCESS));

        // Start included step 2
        bus.dispatch(new StepStartedEvent(incStep2, 1));
        bus.dispatch(new StepFinishedEvent(incStep2, PlaybookStepStatus.SUCCESS));

        // Finish parent step
        bus.dispatch(new StepFinishedEvent(parentStep, PlaybookStepStatus.SUCCESS));

        final TestExecutionReport report = listener.getReport();
        assertNotNull(report);
        assertEquals(1, report.getSteps().size());

        final TestExecutionReport.ReportStepEntry mainEntry = report.getSteps().get(0);
        assertEquals("Add product to cart", mainEntry.getInstruction());

        final List<TestExecutionReport.ReportStepEntry> subSteps = mainEntry.getSubSteps();
        assertEquals(2, subSteps.size(), "subSteps should contain exactly 2 items from included fragment");
        assertEquals("Set quantity of the product to 1.", subSteps.get(0).getInstruction());
        assertEquals("Click add to cart button.", subSteps.get(1).getInstruction());
    }

    @Test
    @DisplayName("Verify compound step retains actions and tool calls on parent without slicing into sub-steps")
    public void testCompoundStepRetainsActionsOnParentWithoutArtificialPartitioning() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-compound-retention");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        final PlaybookStep parentStep = new PlaybookStep("Fill out shipping address form:");
        final PlaybookStep subStep1 = new PlaybookStep("First Name");
        subStep1.setParent(parentStep);
        final PlaybookStep subStep2 = new PlaybookStep("Country");
        subStep2.setParent(parentStep);
        parentStep.setSubSteps(List.of(subStep1, subStep2));

        bus.dispatch(new StepStartedEvent(parentStep, 0));

        // Actions executed during the compound turn belong to the parent step
        final Action action1 = new Action("TYPE", "#firstName", List.of("Alice"), "Type Alice", "Type Alice");
        final Action action2 = new Action("SELECT", "#shipping-country", List.of("US"), "Select US", "Select US");
        bus.dispatch(new ActionExecutedEvent(action1, true));
        bus.dispatch(new ActionExecutedEvent(action2, true));

        bus.dispatch(new StepFinishedEvent(parentStep, PlaybookStepStatus.SUCCESS));
        bus.dispatch(new SessionFinishedEvent(1000, true));

        final TestExecutionReport report = listener.getReport();
        assertNotNull(report);
        assertEquals(1, report.getSteps().size());

        final TestExecutionReport.ReportStepEntry parentEntry = report.getSteps().get(0);
        assertEquals("Fill out shipping address form:", parentEntry.getInstruction());
        assertEquals(2, parentEntry.getSubSteps().size());

        // Child sub-steps do not receive false replay actions
        for (final TestExecutionReport.ReportStepEntry child : parentEntry.getSubSteps())
        {
            assertTrue(child.getActions().isEmpty(), "Child sub-steps should not have sliced actions");
            assertNotNull(child.getParent(), "Parent linkage must be established");
        }
    }

    @Test
    @DisplayName("Verify that optional flag in child sub-step is not promoted to parent include or composite step")
    public void testOptionalChildStepNotPromotedToParent() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-opt-not-promoted");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        final PlaybookStep parentStep = new PlaybookStep("Open product detail page");
        final PlaybookStep subStep1 = new PlaybookStep("Navigate to URL");
        final PlaybookStep subStep2 = new PlaybookStep("Close cookie consent banner if present (optional)");
        subStep2.setOptional(true);
        final PlaybookStep subStep3 = new PlaybookStep("Wait until product details loaded");

        subStep1.setParent(parentStep);
        subStep2.setParent(parentStep);
        subStep3.setParent(parentStep);
        parentStep.setSubSteps(List.of(subStep1, subStep2, subStep3));

        assertFalse(parentStep.isOptional(), "Parent playbook step must not be optional just because a child is optional");
        assertTrue(subStep2.isOptional(), "Sub-step 2 must retain its optional flag");
        assertFalse(subStep3.isOptional(), "Sub-step 3 must not inherit optional from sibling");

        bus.dispatch(new StepStartedEvent(parentStep, 0));
        bus.dispatch(new StepStartedEvent(subStep1, 0));
        bus.dispatch(new StepFinishedEvent(subStep1, PlaybookStepStatus.SUCCESS));
        bus.dispatch(new StepStartedEvent(subStep2, 1));
        bus.dispatch(new StepFinishedEvent(subStep2, PlaybookStepStatus.SUCCESS));
        bus.dispatch(new StepStartedEvent(subStep3, 2));
        bus.dispatch(new StepFinishedEvent(subStep3, PlaybookStepStatus.SUCCESS));
        bus.dispatch(new StepFinishedEvent(parentStep, PlaybookStepStatus.SUCCESS));
        bus.dispatch(new SessionFinishedEvent(1000, true));

        final TestExecutionReport report = listener.getReport();
        assertNotNull(report);
        assertEquals(1, report.getSteps().size());

        final TestExecutionReport.ReportStepEntry parentEntry = report.getSteps().get(0);
        assertFalse(parentEntry.isOptional(), "Parent report entry must not evaluate to optional");
        assertEquals(3, parentEntry.getSubSteps().size());
        assertFalse(parentEntry.getSubSteps().get(0).isOptional());
        assertTrue(parentEntry.getSubSteps().get(1).isOptional());
        assertFalse(parentEntry.getSubSteps().get(2).isOptional());
    }

    @Test
    @DisplayName("Verify tolerated optional sub-step failure does not mark parent or subsequent sub-steps as failed")
    public void testToleratedOptionalSubStepFailureDoesNotMarkParentOrSubsequentSubStepsAsFailed() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-opt-failure-isolation");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        final PlaybookStep parentStep = new PlaybookStep("Execute include fragment");
        final PlaybookStep subStep1 = new PlaybookStep("Navigate to page");
        final PlaybookStep subStep2 = new PlaybookStep("Close cookie consent banner (optional)");
        subStep2.setOptional(true);
        final PlaybookStep subStep3 = new PlaybookStep("Wait for product details");

        subStep1.setParent(parentStep);
        subStep2.setParent(parentStep);
        subStep3.setParent(parentStep);
        parentStep.setSubSteps(List.of(subStep1, subStep2, subStep3));

        bus.dispatch(new StepStartedEvent(parentStep, 0));

        bus.dispatch(new StepStartedEvent(subStep1, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("NAVIGATE", "https://example.org", List.of(), "Open URL", "Open URL"), true));
        bus.dispatch(new StepFinishedEvent(subStep1, PlaybookStepStatus.SUCCESS));

        // Sub-step 2 fails, but is optional
        bus.dispatch(new StepStartedEvent(subStep2, 1));
        subStep2.setStatus(PlaybookStepStatus.FAILED);
        subStep2.setFailed(true);
        subStep2.setFailureReason("Element not found: #cookie-banner");
        bus.dispatch(new StepFinishedEvent(subStep2, PlaybookStepStatus.FAILED));

        // Sub-step 3 succeeds
        bus.dispatch(new StepStartedEvent(subStep3, 2));
        bus.dispatch(new ActionExecutedEvent(new Action("ASSERT_VISIBLE", ".product-detail", List.of(), "Wait visible", "Wait visible"), true));
        subStep3.setStatus(PlaybookStepStatus.SUCCESS);
        bus.dispatch(new StepFinishedEvent(subStep3, PlaybookStepStatus.SUCCESS));

        // Parent step finishes with SUCCESS because optional child failure was tolerated
        bus.dispatch(new StepFinishedEvent(parentStep, PlaybookStepStatus.SUCCESS));
        bus.dispatch(new SessionFinishedEvent(1000, true));

        final TestExecutionReport report = listener.getReport();
        assertNotNull(report);
        assertEquals(1, report.getSteps().size());

        final TestExecutionReport.ReportStepEntry parentEntry = report.getSteps().get(0);
        assertEquals("SUCCESS", parentEntry.getStatus(), "Parent step status must be SUCCESS");
        assertFalse(parentEntry.isOptional(), "Parent step must not be optional");

        final List<TestExecutionReport.ReportStepEntry> subs = parentEntry.getSubSteps();
        assertEquals(3, subs.size());
        assertEquals("SUCCESS", subs.get(0).getStatus());
        assertEquals("FAILED", subs.get(1).getStatus(), "Optional sub-step should preserve its soft failure");
        assertTrue(subs.get(1).isOptional());
        assertEquals("SUCCESS", subs.get(2).getStatus(), "Sub-step 3 must remain SUCCESS and not be falsely blamed for sub-step 2 failure");
        assertNull(subs.get(2).getFailureReason());
    }

    @Test
    @DisplayName("Verify subsequent step failure does not overwrite earlier composite step or its sub-steps")
    public void testSubsequentStepFailureDoesNotLeakOntoPrecedingCompositeStepOrItsSubSteps() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-opt-leak-prevention");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        // Step 1: Composite include step
        final PlaybookStep step1 = new PlaybookStep("Include open-pdp.yaml");
        final PlaybookStep sub11 = new PlaybookStep("Open PDP");
        final PlaybookStep sub12 = new PlaybookStep("Close cookie consent (optional)");
        sub12.setOptional(true);
        final PlaybookStep sub19 = new PlaybookStep("Wait until product details loaded");

        sub11.setParent(step1);
        sub12.setParent(step1);
        sub19.setParent(step1);
        step1.setSubSteps(List.of(sub11, sub12, sub19));

        // Step 2: Regular step
        final PlaybookStep step2 = new PlaybookStep("Select product size");

        // Step 3: Failing step
        final PlaybookStep step3 = new PlaybookStep("Validate visual checkout baseline (visual)");

        // Execution of Step 1
        bus.dispatch(new StepStartedEvent(step1, 0));
        bus.dispatch(new StepStartedEvent(sub11, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("NAVIGATE", "https://example.org", List.of(), "Open URL", "Open URL"), true));
        bus.dispatch(new StepFinishedEvent(sub11, PlaybookStepStatus.SUCCESS));

        bus.dispatch(new StepStartedEvent(sub12, 1));
        sub12.setStatus(PlaybookStepStatus.FAILED);
        sub12.setFailed(true);
        sub12.setFailureReason("Banner not present");
        bus.dispatch(new StepFinishedEvent(sub12, PlaybookStepStatus.FAILED));

        bus.dispatch(new StepStartedEvent(sub19, 2));
        bus.dispatch(new ActionExecutedEvent(new Action("ASSERT_VISIBLE", ".product-detail", List.of(), "Wait visible", "Wait visible"), true));
        sub19.setStatus(PlaybookStepStatus.SUCCESS);
        bus.dispatch(new StepFinishedEvent(sub19, PlaybookStepStatus.SUCCESS));

        bus.dispatch(new StepFinishedEvent(step1, PlaybookStepStatus.SUCCESS));

        // Execution of Step 2
        bus.dispatch(new StepStartedEvent(step2, 1));
        bus.dispatch(new ActionExecutedEvent(new Action("CLICK", "#size-M", List.of(), "Select size M", "Select size M"), true));
        bus.dispatch(new StepFinishedEvent(step2, PlaybookStepStatus.SUCCESS));

        // Execution of Step 3 - fails visual comparison
        bus.dispatch(new StepStartedEvent(step3, 2));
        step3.setStatus(PlaybookStepStatus.FAILED);
        step3.setFailed(true);
        final String visualErrorMessage = "Visual SSIM score below threshold (score: 0.9870 < 0.99)";
        step3.setFailureReason(visualErrorMessage);
        bus.dispatch(new StepFinishedEvent(step3, PlaybookStepStatus.FAILED));

        listener.getReport().setFailureReason(visualErrorMessage);
        bus.dispatch(new SessionFinishedEvent(3000, false));

        final TestExecutionReport report = listener.getReport();
        assertNotNull(report);
        assertFalse(report.isSuccess());
        assertEquals(3, report.getSteps().size());

        final TestExecutionReport.ReportStepEntry step1Entry = report.getSteps().get(0);
        assertEquals("SUCCESS", step1Entry.getStatus(), "Step 1 must remain SUCCESS");
        assertFalse(step1Entry.isOptional(), "Step 1 must not be marked optional");
        assertNull(step1Entry.getFailureReason(), "Step 1 must not receive Step 3's failure reason");

        final TestExecutionReport.ReportStepEntry sub19Entry = step1Entry.getSubSteps().get(2);
        assertEquals("SUCCESS", sub19Entry.getStatus(), "Step 1.9 must remain SUCCESS");
        assertNull(sub19Entry.getFailureReason(), "Step 1.9 must not receive Step 3's visual error message");

        final TestExecutionReport.ReportStepEntry step2Entry = report.getSteps().get(1);
        assertEquals("SUCCESS", step2Entry.getStatus(), "Step 2 must remain SUCCESS");

        final TestExecutionReport.ReportStepEntry step3Entry = report.getSteps().get(2);
        assertEquals("FAILED", step3Entry.getStatus(), "Step 3 must be FAILED");
        assertEquals(visualErrorMessage, step3Entry.getFailureReason(), "Step 3 must contain the visual error");
    }
}
