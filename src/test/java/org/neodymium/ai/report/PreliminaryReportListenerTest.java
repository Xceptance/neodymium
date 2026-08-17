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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.diagnostic.DiagnosticErrorEvent;
import org.neodymium.ai.event.diagnostic.DiagnosticWarningEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StateCapturedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.StepStats;

/**
 * Unit test suite for {@link PreliminaryReportListener}, {@link HtmlReportGenerator},
 * {@link MarkdownReportGenerator}, {@link JsonReportGenerator}, and {@link DiskReportFormat}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class PreliminaryReportListenerTest
{
    @TempDir
    Path tempFolder;

    @Test
    @DisplayName("Verify parsing and normalization of DiskReportFormat enum")
    public void testDiskReportFormatParser()
    {
        final Set<DiskReportFormat> allDefault = DiskReportFormat.parseFormats(null);
        assertTrue(allDefault.contains(DiskReportFormat.HTML));
        assertTrue(allDefault.contains(DiskReportFormat.MARKDOWN));
        assertTrue(allDefault.contains(DiskReportFormat.JSON));

        final Set<DiskReportFormat> allExplicit = DiskReportFormat.parseFormats("ALL");
        assertEquals(3, allExplicit.size());

        final Set<DiskReportFormat> htmlOnly = DiskReportFormat.parseFormats("html");
        assertEquals(Set.of(DiskReportFormat.HTML), htmlOnly);

        final Set<DiskReportFormat> mdAndJson = DiskReportFormat.parseFormats("md, json");
        assertEquals(Set.of(DiskReportFormat.MARKDOWN, DiskReportFormat.JSON), mdAndJson);
    }

    @Test
    @DisplayName("Verify preliminary disk report generation on successful test run with Light Mode UI")
    public void testSuccessfulRunReportGeneration() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.MARKDOWN, DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("org.neodymium.ai.integration.AddToCartTest");
        listener.getReport().setTestMethod("testAddToCart");
        listener.getReport().setDatasetId("us");
        listener.getReport().setExecutionMode("LLM_RECORDING");

        // Step 1: Open Home Page
        final PlaybookStep step1 = new PlaybookStep("Open Home Page");
        step1.setSourceFile("add_to_cart.yaml");
        step1.setLineNumber(10);
        step1.setReasoning("Navigate to store root");

        bus.dispatch(new StepStartedEvent(step1, 0));

        final Action act1 = new Action("NAVIGATE", "https://example.org", Collections.emptyList(), "Go to site", "Initial page load");
        bus.dispatch(new ActionExecutedEvent(act1, true));

        final LlmRequest llmReq = new LlmRequest("System", "User", Collections.emptyList(), null, 0.0, 30);
        final LlmResponse llmResp = new LlmResponse("{\"actions\":[{\"type\":\"CLICK\"}]}", new TokenUsage(1200, 150, 1350, 400), "gemini-3.5-flash");
        bus.dispatch(new LlmResponseReceivedEvent(llmReq, llmResp, 320, "ACTION_EXTRACTION"));

        bus.dispatch(new StepFinishedEvent(step1, PlaybookStepStatus.SUCCESS));

        // Step 2: Search for Product
        final PlaybookStep step2 = new PlaybookStep("Search for Shoes");
        step2.setSourceFile("add_to_cart.yaml");
        step2.setLineNumber(15);
        bus.dispatch(new StepStartedEvent(step2, 1));

        final Action act2 = new Action("FILL", "#search-input", List.of("Shoes"), "Type search query", "Input keyword");
        bus.dispatch(new ActionExecutedEvent(act2, true));
        bus.dispatch(new StepFinishedEvent(step2, PlaybookStepStatus.HEALED));

        // Warnings
        bus.dispatch(new DiagnosticWarningEvent("Slow network response detected"));

        // Finish Session
        bus.dispatch(new SessionFinishedEvent(1850, true, List.of("Non-fatal step warning")));

        // Verification: Files created
        final Path htmlPath = reportDir.resolve("AddToCartTest_testAddToCart_us.html");
        final Path mdPath = reportDir.resolve("AddToCartTest_testAddToCart_us.md");
        final Path jsonPath = reportDir.resolve("AddToCartTest_testAddToCart_us.json");

        assertTrue(Files.exists(htmlPath), "HTML report must exist");
        assertTrue(Files.exists(mdPath), "Markdown report must exist");
        assertTrue(Files.exists(jsonPath), "JSON report must exist");

        // HTML Content check
        final String htmlContent = Files.readString(htmlPath);
        assertTrue(htmlContent.contains("AddToCartTest"));
        assertTrue(htmlContent.contains("PASSED"));
        assertTrue(htmlContent.contains("Open Home Page"));
        assertTrue(htmlContent.contains("Search for Shoes"));
        assertTrue(htmlContent.contains("NAVIGATE"));
        assertTrue(htmlContent.contains("FILL"));
        assertTrue(htmlContent.contains("gemini-3.5-flash"));
        assertTrue(htmlContent.contains("Slow network response detected"));
        assertTrue(htmlContent.contains("--bg: #f8fafc;"), "HTML report must use Light Mode UI variables");
        assertTrue(htmlContent.contains("--card-bg: #ffffff;"));
        assertTrue(htmlContent.contains("AI LLM Responsibility"));
        assertTrue(htmlContent.contains("steps-split-layout"), "HTML report must include split pane layout");
        assertTrue(htmlContent.contains("steps-inspector-pane"), "HTML report must include inspector pane");
        assertTrue(htmlContent.contains("stepDataPayload"), "HTML report must embed step data payload");

        // Markdown Content check
        final String mdContent = Files.readString(mdPath);
        assertTrue(mdContent.contains("# ✅ Test Execution Report:"));
        assertTrue(mdContent.contains("**PASSED**"));
        assertTrue(mdContent.contains("Open Home Page"));
        assertTrue(mdContent.contains("Search for Shoes"));
        assertTrue(mdContent.contains("NAVIGATE"));
        assertTrue(mdContent.contains("LLM Responsibility Breakdown"));

        // JSON Content check
        final String jsonContent = Files.readString(jsonPath);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode root = mapper.readTree(jsonContent);

        assertTrue(root.get("success").asBoolean());
        assertEquals("PASSED", root.get("status").asText());
        assertEquals("org.neodymium.ai.integration.AddToCartTest", root.get("testClass").asText());
        assertEquals("testAddToCart", root.get("testMethod").asText());
        assertEquals("us", root.get("datasetId").asText());
        assertEquals(2, root.get("steps").size());
        assertEquals(1, root.get("llmCalls").size());
        assertEquals(1, root.get("steps").get(0).get("llmCalls").size(), "Step 0 must have associated LLM call");
        assertEquals(1200, root.get("metrics").get("tokenUsageInput").asLong());
        assertEquals(150, root.get("metrics").get("tokenUsageOutput").asLong());
        assertEquals(400, root.get("metrics").get("tokenUsageCached").asLong());
        assertEquals(1, root.get("metrics").get("healedSteps").asInt());
    }

    @Test
    @DisplayName("Verify comprehensive LLM category token usages and step stats aggregation")
    public void testExecutionContextStatisticsIntegration() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-stats");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.MARKDOWN, DiskReportFormat.JSON), true);

        final ExecutionContext ctx = new ExecutionContext(new SessionData());
        ExecutionContext.setActiveContext(ctx);

        try
        {
            // Populate category usages
            ctx.getTransientData().put(ExecutionContext.KEY_STANDARD_CALL_COUNT, 18);
            ctx.getTransientData().put(ExecutionContext.KEY_STANDARD_TOKEN_USAGE, new TokenUsage(62374, 2208, 64582, 0));

            ctx.getTransientData().put(ExecutionContext.KEY_PESAP_CALL_COUNT, 16);
            ctx.getTransientData().put(ExecutionContext.KEY_PESAP_TOKEN_USAGE, new TokenUsage(10809, 181, 10990, 0));

            ctx.getTransientData().put(ExecutionContext.KEY_TOTAL_REPLAYS, 2);
            ctx.getTransientData().put(ExecutionContext.KEY_INTERNAL_CACHE_HITS, 5);

            // Populate step stats
            final StepStats stats1 = new StepStats("Locate product card and hover", 1000);
            stats1.setDurationMs(8412);
            stats1.addContextLevel("LEAN");
            stats1.addContextLevel("STANDARD");
            stats1.addPesapCall(662, 10, 0);
            stats1.addStandardCall(9136, 247, 0);

            ctx.getTransientData().put("execution.stepStatsList", List.of(stats1));

            final ExecutionEventBus bus = new ExecutionEventBus();
            bus.registerListener(listener);

            listener.getReport().setTestClass("org.neodymium.ai.integration.VerlaCartTest");
            listener.getReport().setTestMethod("testCartLive");

            final PlaybookStep pbStep = new PlaybookStep("Locate product card and hover");
            bus.dispatch(new StepStartedEvent(pbStep, 0));
            bus.dispatch(new StepFinishedEvent(pbStep, PlaybookStepStatus.SUCCESS));
            bus.dispatch(new SessionFinishedEvent(8412, true));

            final Path mdPath = reportDir.resolve("VerlaCartTest_testCartLive.md");
            final Path htmlPath = reportDir.resolve("VerlaCartTest_testCartLive.html");
            final Path jsonPath = reportDir.resolve("VerlaCartTest_testCartLive.json");

            assertTrue(Files.exists(mdPath));
            assertTrue(Files.exists(htmlPath));
            assertTrue(Files.exists(jsonPath));

            final String md = Files.readString(mdPath);
            assertTrue(md.contains("34")); // 18 + 16 total calls
            assertTrue(md.contains("75,572")); // 64582 + 10990 total tokens
            assertTrue(md.contains("PESAP"));
            assertTrue(md.contains("Action (Standard)"));
            assertTrue(md.contains("Context Escalations"));
            assertTrue(md.contains("LEAN → STANDARD"));

            final String html = Files.readString(htmlPath);
            assertTrue(html.contains("75,572"));
            assertTrue(html.contains("PESAP (Pre-Execution Semantic Anchor)"));
            assertTrue(html.contains("LEAN → STANDARD"));
            assertTrue(html.contains("⚡ 1 esc"));

            final String json = Files.readString(jsonPath);
            final JsonNode root = new ObjectMapper().readTree(json);
            assertEquals(34, root.get("metrics").get("totalLlmCalls").asInt());
            assertEquals(75572, root.get("metrics").get("totalTokens").asLong());
            assertEquals(16, root.get("metrics").get("pesap").get("calls").asInt());
            assertEquals(18, root.get("metrics").get("action").get("calls").asInt());
            assertEquals(1, root.get("steps").get(0).get("escalations").asInt());
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }

    @Test
    @DisplayName("Verify failure diagnostics, visual RCA, and base64 screenshots in report")
    public void testFailedRunReportGenerationWithScreenshots() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-fail");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.MARKDOWN, DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("org.neodymium.ai.integration.CheckoutTest");
        listener.getReport().setTestMethod("testFailure");
        listener.getReport().setDatasetId("default");

        // Step 1: Click Pay Button (Fails)
        final PlaybookStep step1 = new PlaybookStep("Click Complete Order");
        step1.setSourceFile("checkout.yaml");
        step1.setLineNumber(42);
        bus.dispatch(new StepStartedEvent(step1, 0));

        final Action act = new Action("CLICK", "#btn-pay", Collections.emptyList(), "Submit payment", "Complete purchase");
        bus.dispatch(new ActionExecutedEvent(act, false));

        // Screenshot capture
        final String fakeBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        final SutAttachment attachment = new SutAttachment("image/png", null, fakeBase64);
        final MockSutState state = new MockSutState("<html><body>Payment Error</body></html>", List.of(attachment), "hash123");
        bus.dispatch(new StateCapturedEvent(state));

        // Diagnostic Error
        final IllegalStateException ex = new IllegalStateException("Payment gateway timed out");
        bus.dispatch(new DiagnosticErrorEvent("Payment execution failed: gateway timeout", ex));

        bus.dispatch(new StepFinishedEvent(step1, PlaybookStepStatus.FAILED));

        listener.getReport().setVisualRcaExplanation("Payment button disabled due to missing CVV input");

        // Finish Session with failure
        bus.dispatch(new SessionFinishedEvent(3400, false));

        final Path htmlPath = reportDir.resolve("CheckoutTest_testFailure.html");
        final Path mdPath = reportDir.resolve("CheckoutTest_testFailure.md");
        final Path jsonPath = reportDir.resolve("CheckoutTest_testFailure.json");

        assertTrue(Files.exists(htmlPath));
        assertTrue(Files.exists(mdPath));
        assertTrue(Files.exists(jsonPath));

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("FAILED"));
        assertTrue(html.contains("Payment execution failed"));
        assertTrue(html.contains("Payment button disabled due to missing CVV input"));
        assertTrue(html.contains("data:image/png;base64," + fakeBase64));

        final String md = Files.readString(mdPath);
        assertTrue(md.contains("❌"));
        assertTrue(md.contains("**FAILED**"));
        assertTrue(md.contains("Payment execution failed"));
        assertTrue(md.contains("Visual RCA Diagnosis"));

        final String json = Files.readString(jsonPath);
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode root = mapper.readTree(json);

        assertFalse(root.get("success").asBoolean());
        assertEquals("FAILED", root.get("status").asText());
        assertEquals(1, root.get("screenshots").size());
        assertEquals(fakeBase64, root.get("screenshots").get(0).get("base64Data").asText());
    }

    @Test
    @DisplayName("Verify output format filtering")
    public void testReportFormatFiltering() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-json-only");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("TestClass");
        listener.getReport().setTestMethod("testOnlyJson");

        bus.dispatch(new SessionFinishedEvent(100, true));

        assertTrue(Files.exists(reportDir.resolve("TestClass_testOnlyJson.json")));
        assertFalse(Files.exists(reportDir.resolve("TestClass_testOnlyJson.html")));
        assertFalse(Files.exists(reportDir.resolve("TestClass_testOnlyJson.md")));
    }

    @Test
    @DisplayName("Verify that a step left in RUNNING state is auto-resolved to FAILED when session terminates with failure")
    public void testUnfinishedStepAutoResolutionOnFailure() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-unresolved");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.JSON, DiskReportFormat.MARKDOWN, DiskReportFormat.HTML), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("org.neodymium.ai.integration.verla.AddToCartTest");
        listener.getReport().setTestMethod("testCartLivePerfect");

        // Step 1: Finished successfully
        final PlaybookStep step1 = new PlaybookStep("Open Home Page");
        bus.dispatch(new StepStartedEvent(step1, 0));
        bus.dispatch(new StepFinishedEvent(step1, PlaybookStepStatus.SUCCESS));

        // Step 2: Started, had an action, but crashed without dispatching StepFinishedEvent
        final PlaybookStep step2 = new PlaybookStep("Assert that cart table contains Free Bonus Gift");
        bus.dispatch(new StepStartedEvent(step2, 1));
        final Action act = new Action("ASSERT", ".cart-table-wrapper", List.of("Free Bonus Gift"), "Assert line item", "Bonus gift check");
        bus.dispatch(new ActionExecutedEvent(act, false));
        bus.dispatch(new DiagnosticErrorEvent("AssertionError: Element '.cart-table-wrapper' not found", new AssertionError("Element not found")));

        // Session finishes with failure (StepFinishedEvent was never dispatched for step2)
        bus.dispatch(new SessionFinishedEvent(5000, false));

        final Path jsonPath = reportDir.resolve("AddToCartTest_testCartLivePerfect.json");
        final Path mdPath = reportDir.resolve("AddToCartTest_testCartLivePerfect.md");
        final Path htmlPath = reportDir.resolve("AddToCartTest_testCartLivePerfect.html");

        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(mdPath));
        assertTrue(Files.exists(htmlPath));

        final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
        assertEquals("FAILED", root.get("steps").get(1).get("status").asText(), "Step 2 must be auto-resolved to FAILED instead of RUNNING");
        assertTrue(root.get("steps").get(1).get("durationMs").asLong() > 0, "Step 2 must have a non-zero duration");
        assertEquals("AssertionError: Element '.cart-table-wrapper' not found", root.get("steps").get(1).get("failureReason").asText());

        final String md = Files.readString(mdPath);
        assertTrue(md.contains("Step 2: ❌ Assert that cart table contains Free Bonus Gift"));
        assertTrue(md.contains("- **Status:** `FAILED`"));

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("pill-fail"));
        assertTrue(html.contains("FAILED"));
    }

    @Test
    @DisplayName("Verify PESAP LLM call capture and sub-step hierarchy rendering")
    public void testPesapAndSubStepsHierarchy() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-substeps");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON, DiskReportFormat.MARKDOWN), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("org.neodymium.ai.integration.verla.AddToCartTest");
        listener.getReport().setTestMethod("testCompoundSplit");

        // Compound Parent Step
        final PlaybookStep parentStep = new PlaybookStep("Locate promo input, clear content, and type FREEGIFT");
        bus.dispatch(new StepStartedEvent(parentStep, 0));

        // PESAP call on parent step
        final LlmRequest pesapReq = new LlmRequest("PESAP System", "Analyze step: Locate promo input...", Collections.emptyList(), null, 0.0, 30);
        final LlmResponse pesapResp = new LlmResponse("{\"splitSteps\":[\"Locate promo input and clear content\",\"Type FREEGIFT into promo input\"]}", new TokenUsage(650, 45, 695, 100), "gemini-3.5-flash");
        bus.dispatch(new LlmResponseReceivedEvent(pesapReq, pesapResp, 180, "PESAP"));

        // Sub-step 1
        final PlaybookStep subStep1 = new PlaybookStep("Locate promo input and clear content");
        subStep1.setParent(parentStep);
        parentStep.getSubSteps().add(subStep1);
        bus.dispatch(new StepStartedEvent(subStep1, 0));
        final Action act1 = new Action("CLEAR", "#couponCode", Collections.emptyList(), "Clear coupon", "Input reset");
        bus.dispatch(new ActionExecutedEvent(act1, true));
        bus.dispatch(new StepFinishedEvent(subStep1, PlaybookStepStatus.SUCCESS));

        // Sub-step 2
        final PlaybookStep subStep2 = new PlaybookStep("Type FREEGIFT into promo input");
        subStep2.setParent(parentStep);
        parentStep.getSubSteps().add(subStep2);
        bus.dispatch(new StepStartedEvent(subStep2, 0));
        final Action act2 = new Action("TYPE", "#couponCode", List.of("FREEGIFT"), "Type coupon code", "Apply promo");
        bus.dispatch(new ActionExecutedEvent(act2, true));
        bus.dispatch(new StepFinishedEvent(subStep2, PlaybookStepStatus.SUCCESS));

        // Parent step finished (all sub-steps passed)
        bus.dispatch(new SessionFinishedEvent(3000, true));

        final Path htmlPath = reportDir.resolve("AddToCartTest_testCompoundSplit.html");
        final Path jsonPath = reportDir.resolve("AddToCartTest_testCompoundSplit.json");

        assertTrue(Files.exists(htmlPath));
        assertTrue(Files.exists(jsonPath));

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("sub-steps-container"), "HTML report must contain sub-steps container");
        assertTrue(html.contains("#1.1"), "HTML report must show sub-step 1.1");
        assertTrue(html.contains("#1.2"), "HTML report must show sub-step 1.2");
        assertTrue(html.contains("btn-toggle-inspector"), "HTML report must contain toggle inspector button");

        final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
        assertEquals(1, root.get("steps").size(), "Root steps must contain 1 parent step");
        final JsonNode parentNode = root.get("steps").get(0);
        assertEquals(2, parentNode.get("subSteps").size(), "Parent step must contain exactly 2 deduplicated sub-steps");
        assertEquals(1, parentNode.get("llmCalls").size(), "Parent step must have 1 PESAP LLM call attached");
        assertEquals("PESAP", parentNode.get("llmCalls").get(0).get("capability").asText());
    }

    @Test
    @DisplayName("Verify variable resolution in step instructions from SessionData")
    public void testVariableResolutionInStepInstructions() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-vars");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON, DiskReportFormat.MARKDOWN), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        final Map<String, SessionData.DataEntry> staticData = new HashMap<>();
        staticData.put("verla.url", new SessionData.DataEntry("https://localhost:8543", false));
        staticData.put("testId", new SessionData.DataEntry("perfect", false));
        final SessionData sessionData = new SessionData(staticData);

        final ExecutionContext ctx = new ExecutionContext(sessionData);
        ExecutionContext.setActiveContext(ctx);

        try
        {
            listener.getReport().setTestClass("AddToCartTest");
            listener.getReport().setTestMethod("testVarResolution");

            final PlaybookStep step = new PlaybookStep("Open ${verla.url}/verla-${testId}/index.html");
            bus.dispatch(new StepStartedEvent(step, 0));
            bus.dispatch(new StepFinishedEvent(step, PlaybookStepStatus.SUCCESS));
            bus.dispatch(new SessionFinishedEvent(100, true));

            final Path jsonPath = reportDir.resolve("AddToCartTest_testVarResolution.json");
            final Path htmlPath = reportDir.resolve("AddToCartTest_testVarResolution.html");

            assertTrue(Files.exists(jsonPath));
            assertTrue(Files.exists(htmlPath));

            final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
            assertEquals("Open https://localhost:8543/verla-perfect/index.html", root.get("steps").get(0).get("instruction").asText());
            assertEquals("Open ${verla.url}/verla-${testId}/index.html", root.get("steps").get(0).get("rawInstruction").asText());

            final String html = Files.readString(htmlPath);
            assertTrue(html.contains("Open https://localhost:8543/verla-perfect/index.html"));
            assertTrue(html.contains("split-resizer"));
            assertTrue(html.contains("btn-preset"));
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }

    @Test
    @DisplayName("Verify expected bug tag display and suppression of test failure banner on passed test")
    public void testExpectedBugHandling() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-bug");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON, DiskReportFormat.MARKDOWN), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("AddToCartTest");
        listener.getReport().setTestMethod("testExpectedBug");

        final PlaybookStep bugStep = new PlaybookStep("Assert that the cart items table contains Free Bonus Gift (bug: Cart gift omission).");
        bus.dispatch(new StepStartedEvent(bugStep, 0));
        bus.dispatch(new DiagnosticErrorEvent("Element '.cart-table' not found", new AssertionError("Defect detected")));
        bus.dispatch(new StepFinishedEvent(bugStep, PlaybookStepStatus.SUCCESS));

        // Test passes overall because bug was expected
        bus.dispatch(new SessionFinishedEvent(1200, true));

        final Path jsonPath = reportDir.resolve("AddToCartTest_testExpectedBug.json");
        final Path htmlPath = reportDir.resolve("AddToCartTest_testExpectedBug.html");
        final Path mdPath = reportDir.resolve("AddToCartTest_testExpectedBug.md");

        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(htmlPath));
        assertTrue(Files.exists(mdPath));

        final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
        assertTrue(root.get("steps").get(0).get("bug").asBoolean());
        assertEquals("Cart gift omission", root.get("steps").get(0).get("bugDetails").asText());

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("BUG EXPECTED") || html.contains("Cart gift omission"));
        assertFalse(html.contains("🚨 Execution Failure Details"), "Passed bug test must NOT show test-level failure banner");

        final String md = Files.readString(mdPath);
        assertTrue(md.contains("Expected Bug"));
        assertFalse(md.contains("## 🚨 Failure Diagnostics"));
    }

    @Test
    @DisplayName("Verify no disk reports are written when disabled")
    public void testDisabledReporting() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-disabled");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON), false);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("DisabledClass");
        listener.getReport().setTestMethod("testDisabled");

        bus.dispatch(new SessionFinishedEvent(100, true));

        final File[] files = reportDir.toFile().listFiles();
        assertTrue(files == null || files.length == 0);
    }
}
