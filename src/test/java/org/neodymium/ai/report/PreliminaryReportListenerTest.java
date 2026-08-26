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

    @org.junit.jupiter.api.BeforeEach
    @org.junit.jupiter.api.AfterEach
    public void resetContext()
    {
        ExecutionContext.setActiveContext(null);
    }

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
        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");
        final Path mdPath = reportDir.resolve(listener.getLastBaseFileName() + ".md");
        final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");

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

            final Path mdPath = reportDir.resolve(listener.getLastBaseFileName() + ".md");
            final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");
            final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");

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

        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");
        final Path mdPath = reportDir.resolve(listener.getLastBaseFileName() + ".md");
        final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");

        assertTrue(Files.exists(htmlPath));
        assertTrue(Files.exists(mdPath));
        assertTrue(Files.exists(jsonPath));

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("FAILED"));
        assertTrue(html.contains("Payment execution failed"));
        assertTrue(html.contains("Payment button disabled due to missing CVV input"));
        assertTrue(html.contains("data:image/png;base64," + fakeBase64));
        assertTrue(html.contains("id=\"reportLightbox\""), "HTML report must include screenshot Lightbox modal");
        assertTrue(html.contains("openLightbox("), "HTML report must attach openLightbox click handler to screenshots");
        assertTrue(html.contains("id=\"lightboxDownloadBtn\""), "Lightbox must contain download button");

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

        assertTrue(Files.exists(reportDir.resolve(listener.getLastBaseFileName() + ".json")));
        assertFalse(Files.exists(reportDir.resolve(listener.getLastBaseFileName() + ".html")));
        assertFalse(Files.exists(reportDir.resolve(listener.getLastBaseFileName() + ".md")));
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

        final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");
        final Path mdPath = reportDir.resolve(listener.getLastBaseFileName() + ".md");
        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");

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

        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");
        final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");

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
    @DisplayName("Verify sub-steps attach strictly to their respective parent step and never to Step #1")
    public void testSubStepsAttachedToCorrectLaterStep() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-multistep-parent");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON, DiskReportFormat.MARKDOWN), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("VerlaGuestCheckout_Us_German");
        listener.getReport().setTestMethod("testCheckoutLivePerfect");

        // Step 1: Open URL
        final PlaybookStep step1 = new PlaybookStep("Öffne https://localhost:8543/verla-perfect/index.html");
        bus.dispatch(new StepStartedEvent(step1, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("NAVIGATE", "https://localhost:8543", Collections.emptyList(), "Navigate", "Open site"), true));
        bus.dispatch(new StepFinishedEvent(step1, PlaybookStepStatus.SUCCESS));

        // Step 2: Go to checkout
        final PlaybookStep step2 = new PlaybookStep("Klicke auf 'Kasse'");
        bus.dispatch(new StepStartedEvent(step2, 1));
        bus.dispatch(new ActionExecutedEvent(new Action("CLICK", "#checkout-btn", Collections.emptyList(), "Checkout", "Proceed"), true));
        bus.dispatch(new StepFinishedEvent(step2, PlaybookStepStatus.SUCCESS));

        // Step 3: Compound step (Payment details)
        final PlaybookStep step3 = new PlaybookStep("Kartennummer ist '4111 1111 1111 1111', Ablaufdatum '12/29' und CVV ist '111'.");
        bus.dispatch(new StepStartedEvent(step3, 2));

        // Sub-step 3.1
        final PlaybookStep sub31 = new PlaybookStep("Kartennummer ist '4111 1111 1111 1111'");
        sub31.setParent(step3);
        bus.dispatch(new StepStartedEvent(sub31, 0)); // Note: stepStarted with index 0 to simulate unindexed sub-step
        bus.dispatch(new ActionExecutedEvent(new Action("TYPE", "#cardNumber", List.of("4111111111111111"), "Type card", "Card"), true));
        bus.dispatch(new StepFinishedEvent(sub31, PlaybookStepStatus.SUCCESS));

        // Sub-step 3.2
        final PlaybookStep sub32 = new PlaybookStep("Ablaufdatum '12/29' und CVV ist '111'");
        sub32.setParent(step3);
        bus.dispatch(new StepStartedEvent(sub32, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("TYPE", "#cardExpiry", List.of("12/29"), "Type expiry", "Expiry"), true));
        bus.dispatch(new StepFinishedEvent(sub32, PlaybookStepStatus.SUCCESS));

        bus.dispatch(new StepFinishedEvent(step3, PlaybookStepStatus.SUCCESS));
        bus.dispatch(new SessionFinishedEvent(10000, true));

        final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");
        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");

        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(htmlPath));

        final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
        assertEquals(3, root.get("steps").size(), "Report must contain exactly 3 top-level steps");

        // Verify Step 1 has NO sub-steps
        assertEquals(0, root.get("steps").get(0).get("subSteps").size(), "Step 1 (Open URL) must NOT have any sub-steps attached");

        // Verify Step 2 has NO sub-steps
        assertEquals(0, root.get("steps").get(1).get("subSteps").size(), "Step 2 (Checkout) must NOT have any sub-steps attached");

        // Verify Step 3 HAS the 2 sub-steps
        final JsonNode step3Node = root.get("steps").get(2);
        assertEquals(2, step3Node.get("subSteps").size(), "Step 3 (Payment) must contain the 2 sub-steps");
        assertEquals("Kartennummer ist '4111 1111 1111 1111'", step3Node.get("subSteps").get(0).get("instruction").asText());
        assertEquals("Ablaufdatum '12/29' und CVV ist '111'", step3Node.get("subSteps").get(1).get("instruction").asText());

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("#3.1"));
        assertTrue(html.contains("#3.2"));
        assertFalse(html.contains("#1.1"), "HTML must NOT contain #1.1 sub-step under Step #1");
    }

    @Test
    @DisplayName("Verify recursive JIT step splitting correctly flattens leaf sub-steps into the root step without creating stray root steps")
    public void testRecursiveCompoundStepSplittingHierarchy() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-recursive-split");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON, DiskReportFormat.MARKDOWN), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("VerlaGuestCheckout_Us_German");
        listener.getReport().setTestMethod("testCheckoutLivePerfect");

        // Root Step 1: Open URL
        final PlaybookStep root1 = new PlaybookStep("Öffne https://localhost:8543/verla-perfect/index.html");
        bus.dispatch(new StepStartedEvent(root1, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("NAVIGATE", "https://localhost:8543", Collections.emptyList(), "Navigate", "Open site"), true));
        bus.dispatch(new StepFinishedEvent(root1, PlaybookStepStatus.SUCCESS));

        // Root Step 2 (Step 21): Compound Name & Email
        final PlaybookStep root2 = new PlaybookStep("Gib 'Mario' als Vorname, 'Meier' als Nachname und die E-Mail-Adresse 'foo@varmail.net' ein.");
        bus.dispatch(new StepStartedEvent(root2, 1));

        // Level 1: Sub-step A (Compound Name)
        final PlaybookStep subA = new PlaybookStep("Gib 'Mario' als Vorname, 'Meier' als Nachname ein");
        subA.setParent(root2);
        bus.dispatch(new StepStartedEvent(subA, 0));

        // Level 2: Sub-step A1 (First Name)
        final PlaybookStep subA1 = new PlaybookStep("Gib 'Mario' als Vorname ein");
        subA1.setParent(subA);
        bus.dispatch(new StepStartedEvent(subA1, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("TYPE", "#firstName", List.of("Mario"), "Type first name", "Name"), true));
        bus.dispatch(new StepFinishedEvent(subA1, PlaybookStepStatus.SUCCESS));

        // Level 2: Sub-step A2 (Last Name)
        final PlaybookStep subA2 = new PlaybookStep("Gib 'Meier' als Nachname ein");
        subA2.setParent(subA);
        bus.dispatch(new StepStartedEvent(subA2, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("TYPE", "#lastName", List.of("Meier"), "Type last name", "Last name"), true));
        bus.dispatch(new StepFinishedEvent(subA2, PlaybookStepStatus.SUCCESS));

        // Level 1: Sub-step B (Email)
        final PlaybookStep subB = new PlaybookStep("Gib die E-Mail-Adresse 'foo@varmail.net' ein");
        subB.setParent(root2);
        bus.dispatch(new StepStartedEvent(subB, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("TYPE", "#email", List.of("foo@varmail.net"), "Type email", "Email"), true));
        bus.dispatch(new StepFinishedEvent(subB, PlaybookStepStatus.SUCCESS));

        bus.dispatch(new StepFinishedEvent(root2, PlaybookStepStatus.SUCCESS));

        // Root Step 3 (Step 22): Address details
        final PlaybookStep root3 = new PlaybookStep("Gib '123 Main St' als Straße, 'Manchester' als Stadt und '12345' als Postleitzahl ein.");
        bus.dispatch(new StepStartedEvent(root3, 2));

        final PlaybookStep subC1 = new PlaybookStep("Gib '123 Main St' als Straße ein");
        subC1.setParent(root3);
        bus.dispatch(new StepStartedEvent(subC1, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("TYPE", "#street", List.of("123 Main St"), "Type street", "Street"), true));
        bus.dispatch(new StepFinishedEvent(subC1, PlaybookStepStatus.SUCCESS));

        final PlaybookStep subC2 = new PlaybookStep("Gib 'Manchester' als Stadt ein");
        subC2.setParent(root3);
        bus.dispatch(new StepStartedEvent(subC2, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("TYPE", "#city", List.of("Manchester"), "Type city", "City"), true));
        bus.dispatch(new StepFinishedEvent(subC2, PlaybookStepStatus.SUCCESS));

        final PlaybookStep subC3 = new PlaybookStep("Gib '12345' als Postleitzahl ein");
        subC3.setParent(root3);
        bus.dispatch(new StepStartedEvent(subC3, 0));
        bus.dispatch(new ActionExecutedEvent(new Action("TYPE", "#postcode", List.of("12345"), "Type zip", "Zip"), true));
        bus.dispatch(new StepFinishedEvent(subC3, PlaybookStepStatus.SUCCESS));

        bus.dispatch(new StepFinishedEvent(root3, PlaybookStepStatus.SUCCESS));
        bus.dispatch(new SessionFinishedEvent(12000, true));

        final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");
        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");

        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(htmlPath));

        final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
        assertEquals(3, root.get("steps").size(), "Must contain exactly 3 top-level steps, not 5 or 6");

        // Verify Step 1
        assertEquals("SUCCESS", root.get("steps").get(0).get("status").asText());
        assertEquals(0, root.get("steps").get(0).get("subSteps").size());

        // Verify Step 2 (contains exactly the 3 executable leaf sub-steps)
        final JsonNode step2Node = root.get("steps").get(1);
        assertEquals("SUCCESS", step2Node.get("status").asText(), "Step 2 must have status SUCCESS and not RUNNING");
        assertEquals(3, step2Node.get("subSteps").size(), "Step 2 must have exactly 3 leaf sub-steps");
        assertEquals("Gib 'Mario' als Vorname ein", step2Node.get("subSteps").get(0).get("instruction").asText());
        assertEquals("Gib 'Meier' als Nachname ein", step2Node.get("subSteps").get(1).get("instruction").asText());
        assertEquals("Gib die E-Mail-Adresse 'foo@varmail.net' ein", step2Node.get("subSteps").get(2).get("instruction").asText());

        // Verify Step 3 (contains the 3 address sub-steps)
        final JsonNode step3Node = root.get("steps").get(2);
        assertEquals("SUCCESS", step3Node.get("status").asText(), "Step 3 must have status SUCCESS");
        assertEquals(3, step3Node.get("subSteps").size(), "Step 3 must have exactly 3 sub-steps");
        assertEquals("Gib '123 Main St' als Straße ein", step3Node.get("subSteps").get(0).get("instruction").asText());
        assertEquals("Gib 'Manchester' als Stadt ein", step3Node.get("subSteps").get(1).get("instruction").asText());
        assertEquals("Gib '12345' als Postleitzahl ein", step3Node.get("subSteps").get(2).get("instruction").asText());

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("#2.1"));
        assertTrue(html.contains("#2.2"));
        assertTrue(html.contains("#2.3"));
        assertTrue(html.contains("#3.1"));
        assertTrue(html.contains("#3.2"));
        assertTrue(html.contains("#3.3"));
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

            final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");
            final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");

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

        final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");
        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");
        final Path mdPath = reportDir.resolve(listener.getLastBaseFileName() + ".md");

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

    @Test
    @DisplayName("Verify visual step screenshots are properly captured, tagged, and rendered across all reports")
    public void testVisualStepScreenshotCaptureAndAssociation() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-visual");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON, DiskReportFormat.MARKDOWN), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("OrderSummaryTest");
        listener.getReport().setTestMethod("testVisualVerification");

        final PlaybookStep visualStep = new PlaybookStep("Assert that the order confirmation summary is displayed (visual)");
        assertTrue(visualStep.isVisualStep());

        bus.dispatch(new StepStartedEvent(visualStep, 0));

        final String dummyBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        final MockSutState state = new MockSutState(
            "<html><body>Order Confirmed</body></html>",
            List.of(new SutAttachment("image/png", null, dummyBase64)),
            "hash123"
        );
        bus.dispatch(new StateCapturedEvent(state));
        bus.dispatch(new StepFinishedEvent(visualStep, PlaybookStepStatus.SUCCESS));
        bus.dispatch(new SessionFinishedEvent(2500, true));

        final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");
        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");
        final Path mdPath = reportDir.resolve(listener.getLastBaseFileName() + ".md");

        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(htmlPath));
        assertTrue(Files.exists(mdPath));

        final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
        assertTrue(root.get("steps").get(0).get("visual").asBoolean(), "Step must be marked as visual");
        assertEquals(1, root.get("steps").get(0).get("screenshots").size(), "Step must have 1 attached screenshot");
        assertEquals(1, root.get("screenshots").size(), "Global report must contain 1 screenshot");

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("📸 VISUAL"), "HTML report must include VISUAL badge");
        assertTrue(html.contains("📸 1 screenshot(s)") || html.contains("screenshot-img"), "HTML report must render screenshot elements");
        assertTrue(html.contains(dummyBase64), "HTML report must contain base64 image data");

        final String md = Files.readString(mdPath);
        assertTrue(md.contains("📸 Captured Visual Screenshots"), "Markdown report must have Screenshots section");
        assertTrue(md.contains("📸 true"), "Markdown report must mark step as visual");
    }

    @Test
    @DisplayName("Verify index.html and index-data.json are dynamically created and sorted reverse-chronologically with KPI summaries and links")
    public void testDynamicIndexHtmlDashboardGenerationAndReverseChronologicalOrder() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-index");

        // 1. Run First Test (Oldest, Passed)
        final PreliminaryReportListener listener1 = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON), true);
        listener1.getReport().setTestClass("com.example.FirstTest");
        listener1.getReport().setTestMethod("testA");
        listener1.getReport().setDatasetId("us");
        listener1.getReport().setExecutionMode("REPLAY_STRICT");
        listener1.getReport().setStartTimeMs(1000000L);
        listener1.getReport().setDurationMs(1500L);
        final ExecutionEventBus bus1 = new ExecutionEventBus();
        bus1.registerListener(listener1);
        final PlaybookStep s1 = new PlaybookStep("Click first button");
        bus1.dispatch(new StepStartedEvent(s1, 0));
        bus1.dispatch(new StepFinishedEvent(s1, PlaybookStepStatus.SUCCESS));
        bus1.dispatch(new SessionFinishedEvent(1500, true));

        // 2. Run Second Test (Middle, Failed)
        final PreliminaryReportListener listener2 = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON), true);
        listener2.getReport().setTestClass("com.example.SecondTest");
        listener2.getReport().setTestMethod("testB");
        listener2.getReport().setDatasetId("de");
        listener2.getReport().setExecutionMode("FORCE_RECORDING");
        listener2.getReport().setStartTimeMs(2000000L);
        listener2.getReport().setDurationMs(3200L);
        final ExecutionEventBus bus2 = new ExecutionEventBus();
        bus2.registerListener(listener2);
        final PlaybookStep s2 = new PlaybookStep("Click second button");
        bus2.dispatch(new StepStartedEvent(s2, 0));
        bus2.dispatch(new DiagnosticErrorEvent("Button 2 missing", new AssertionError("Element not found")));
        bus2.dispatch(new StepFinishedEvent(s2, PlaybookStepStatus.FAILED));
        bus2.dispatch(new SessionFinishedEvent(3200, false));

        // 3. Run Third Test (Newest, Healed)
        final PreliminaryReportListener listener3 = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON), true);
        listener3.getReport().setTestClass("com.example.ThirdTest");
        listener3.getReport().setTestMethod("testC");
        listener3.getReport().setDatasetId("fr");
        listener3.getReport().setExecutionMode("REPLAY_WITH_HEALING");
        listener3.getReport().setStartTimeMs(3000000L);
        listener3.getReport().setDurationMs(2100L);
        final ExecutionEventBus bus3 = new ExecutionEventBus();
        bus3.registerListener(listener3);
        final PlaybookStep s3 = new PlaybookStep("Click third button");
        bus3.dispatch(new StepStartedEvent(s3, 0));
        bus3.dispatch(new StepFinishedEvent(s3, PlaybookStepStatus.HEALED));
        bus3.dispatch(new SessionFinishedEvent(2100, true));

        final Path indexPath = reportDir.resolve("index.html");
        final Path indexDataPath = reportDir.resolve("index-data.json");

        assertTrue(Files.exists(indexPath), "index.html must be created");
        assertTrue(Files.exists(indexDataPath), "index-data.json must be created");

        // Verify index-data.json reverse-chronological order (Newest: ThirdTest -> SecondTest -> FirstTest)
        final JsonNode dataRoot = new ObjectMapper().readTree(Files.readString(indexDataPath));
        assertEquals(3, dataRoot.size(), "Registry must contain 3 tests");
        assertTrue(dataRoot.get(0).get("baseFileName").asText().startsWith("ThirdTest_testC_fr_"), "Top entry must be newest test");
        assertTrue(dataRoot.get(1).get("baseFileName").asText().startsWith("SecondTest_testB_de_"), "Middle entry must be intermediate test");
        assertTrue(dataRoot.get(2).get("baseFileName").asText().startsWith("FirstTest_testA_us_"), "Bottom entry must be oldest test");

        // Verify HTML Content
        final String indexHtml = Files.readString(indexPath);
        assertTrue(indexHtml.contains("Neodymium Aura • AI Test Execution Index"));
        assertTrue(indexHtml.contains("All (3)"), "Must count 3 total tests in filter tabs");
        assertTrue(indexHtml.contains("Passed (1)"), "Must count 1 passed test");
        assertTrue(indexHtml.contains("Failed (1)"), "Must count 1 failed test");
        assertTrue(indexHtml.contains("Healed (1)"), "Must count 1 healed test");

        // Verify Mode Filter and Options
        assertTrue(indexHtml.contains("id=\"modeFilter\""), "Must contain mode filter select");
        assertTrue(indexHtml.contains("REPLAY_STRICT (1)"), "Must count 1 REPLAY_STRICT test");
        assertTrue(indexHtml.contains("FORCE_RECORDING (1)"), "Must count 1 FORCE_RECORDING test");
        assertTrue(indexHtml.contains("REPLAY_WITH_HEALING (1)"), "Must count 1 REPLAY_WITH_HEALING test");
        assertTrue(indexHtml.contains("data-mode=\"REPLAY_STRICT\""), "Row must have data-mode attribute");
        assertTrue(indexHtml.contains("filterByMode('REPLAY_STRICT')"), "Mode badge must have onclick filter handler");

        // Verify Report Links
        assertTrue(indexHtml.contains("href=\"" + listener3.getLastBaseFileName() + ".html\""), "Must link to third test HTML");
        assertTrue(indexHtml.contains("href=\"" + listener2.getLastBaseFileName() + ".html\""), "Must link to second test HTML");
        assertTrue(indexHtml.contains("href=\"" + listener1.getLastBaseFileName() + ".html\""), "Must link to first test HTML");

        // Verify Table Column Structure and Styling
        assertTrue(indexHtml.contains("onclick=\"sortTable(0)\" style=\"cursor:pointer; width:90px;\">Timestamp ⬍</th>"), "Timestamp must be the first column");
        assertTrue(indexHtml.contains("<span class=\"steps-count\">1</span>"), "Steps column must not repeat the word 'steps'");
        assertFalse(indexHtml.contains("<span class=\"steps-count\">1 steps</span>"), "Must not contain '1 steps'");
        assertTrue(indexHtml.contains(">📊</a>"), "Report button must be an icon-only button");
        assertFalse(indexHtml.contains("📊 Report ↗"), "Report button must not contain 'Report' text");

        // Verify ordering in HTML source
        final int posThird = indexHtml.indexOf(listener3.getLastBaseFileName() + ".html");
        final int posSecond = indexHtml.indexOf(listener2.getLastBaseFileName() + ".html");
        final int posFirst = indexHtml.indexOf(listener1.getLastBaseFileName() + ".html");
        assertTrue(posThird < posSecond && posSecond < posFirst, "HTML table rows must be in reverse-chronological order (newest first)");
    }

    @Test
    @DisplayName("Verify compound step sub-step hierarchy during replay when composite parent executes children")
    public void testReplayCompoundStepSubStepHierarchy() throws Exception
    {
        final Path reportDir = this.tempFolder.resolve("ai-reports-replay-substeps");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("VerlaAutoTranslateCheckoutIntegrationTest");
        listener.getReport().setTestMethod("testCheckoutReplayGerman");

        // Step 1: Open homepage
        final PlaybookStep step1 = new PlaybookStep("Öffne https://localhost:8543/verla-normal/index.html");
        bus.dispatch(new StepStartedEvent(step1, 0));
        bus.dispatch(new StepFinishedEvent(step1, PlaybookStepStatus.SUCCESS));

        // Step 2: Open region selector
        final PlaybookStep step2 = new PlaybookStep("Öffne die Regionsauswahl.");
        bus.dispatch(new StepStartedEvent(step2, 1));
        bus.dispatch(new StepFinishedEvent(step2, PlaybookStepStatus.SUCCESS));

        // Step 3: Click 'Germany'
        final PlaybookStep step3 = new PlaybookStep("Klicke auf 'Deutschland'.");
        bus.dispatch(new StepStartedEvent(step3, 2));
        bus.dispatch(new StepFinishedEvent(step3, PlaybookStepStatus.SUCCESS));

        // Step 4: Compound step with 3 recorded sub-steps (deserialized from recording JSON)
        final PlaybookStep step4 = new PlaybookStep("Suche eine Produktkarte, klicke auf 'In den Warenkorb' und wähle eine verfügbare Größe.");
        final PlaybookStep sub41 = new PlaybookStep("Suche eine Produktkarte");
        sub41.setParent(step4);
        final PlaybookStep sub42 = new PlaybookStep("Klicke auf 'In den Warenkorb'");
        sub42.setParent(step4);
        final PlaybookStep sub43 = new PlaybookStep("Wähle eine verfügbare Größe");
        sub43.setParent(step4);
        step4.getSubSteps().addAll(List.of(sub41, sub42, sub43));

        // Replay lifecycle: Step 4 starts
        bus.dispatch(new StepStartedEvent(step4, 3));

        // Sub 4.1 runs
        bus.dispatch(new StepStartedEvent(sub41, 3));
        bus.dispatch(new ActionExecutedEvent(new Action("CLICK", "#product-1", List.of(), "Click card", "Select"), true));
        bus.dispatch(new StepFinishedEvent(sub41, PlaybookStepStatus.SUCCESS));

        // Sub 4.2 runs
        bus.dispatch(new StepStartedEvent(sub42, 3));
        bus.dispatch(new ActionExecutedEvent(new Action("CLICK", "#add-to-cart", List.of(), "Add to cart", "Cart"), true));
        bus.dispatch(new StepFinishedEvent(sub42, PlaybookStepStatus.SUCCESS));

        // Sub 4.3 runs
        bus.dispatch(new StepStartedEvent(sub43, 3));
        bus.dispatch(new ActionExecutedEvent(new Action("CLICK", ".size-btn", List.of(), "Select size", "Size"), true));
        bus.dispatch(new StepFinishedEvent(sub43, PlaybookStepStatus.SUCCESS));

        // Step 4 finishes
        bus.dispatch(new StepFinishedEvent(step4, PlaybookStepStatus.SUCCESS));

        // Step 5: Assert mini-cart
        final PlaybookStep step5 = new PlaybookStep("Die Anzahl im Mini-Warenkorb ist jetzt 1.");
        bus.dispatch(new StepStartedEvent(step5, 4));
        bus.dispatch(new StepFinishedEvent(step5, PlaybookStepStatus.SUCCESS));

        bus.dispatch(new SessionFinishedEvent(6000, true));

        final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");
        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");

        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(htmlPath));

        final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
        assertEquals(5, root.get("steps").size(), "Report must contain exactly 5 top-level steps");

        // Verify Step 3 has 0 sub-steps
        assertEquals(0, root.get("steps").get(2).get("subSteps").size(), "Step 3 ('Deutschland') must have 0 sub-steps");

        // Verify Step 4 has 3 sub-steps
        final JsonNode step4Node = root.get("steps").get(3);
        assertEquals(3, step4Node.get("subSteps").size(), "Step 4 must contain all 3 sub-steps");
        assertEquals("Suche eine Produktkarte", step4Node.get("subSteps").get(0).get("instruction").asText());
        assertEquals("Klicke auf 'In den Warenkorb'", step4Node.get("subSteps").get(1).get("instruction").asText());
        assertEquals("Wähle eine verfügbare Größe", step4Node.get("subSteps").get(2).get("instruction").asText());

        // Verify Step 5 has 0 sub-steps
        assertEquals(0, root.get("steps").get(4).get("subSteps").size(), "Step 5 (Mini-cart) must have 0 sub-steps");

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("#4.1"));
        assertTrue(html.contains("#4.2"));
        assertTrue(html.contains("#4.3"));
        assertFalse(html.contains("#3.1"), "Step #3 must not have #3.1");
    }

    @Test
    public void testResolvedTargetAndValueInReport(@TempDir final Path tempDir) throws Exception
    {
        final Path reportDir = tempDir.resolve("ai-reports-resolved");
        final PreliminaryReportListener listener = new PreliminaryReportListener(reportDir, EnumSet.of(DiskReportFormat.HTML, DiskReportFormat.MARKDOWN, DiskReportFormat.JSON), true);

        final ExecutionEventBus bus = new ExecutionEventBus();
        bus.registerListener(listener);

        listener.getReport().setTestClass("org.neodymium.ai.integration.ResolvedActionTest");
        listener.getReport().setTestMethod("testResolvedActions");
        listener.getReport().setDatasetId("us");
        listener.getReport().setExecutionMode("FORCE_RECORDING");
        listener.getReport().setPlaybookFile("ResolvedActionTest.yaml");

        final PlaybookStep step = new PlaybookStep("Open site and log in");
        bus.dispatch(new StepStartedEvent(step, 0));

        final Action parameterized = new Action("NAVIGATE", "${verla.url}/index.html", List.of("${user}"), "Open", "Nav reason");
        final Action resolved = new Action("NAVIGATE", "https://localhost:8543/index.html", List.of("john_doe"), "Open", "Nav reason");

        bus.dispatch(new ActionExecutedEvent(parameterized, resolved, true));
        bus.dispatch(new StepFinishedEvent(step, PlaybookStepStatus.SUCCESS));
        bus.dispatch(new SessionFinishedEvent(2000, true));

        final Path jsonPath = reportDir.resolve(listener.getLastBaseFileName() + ".json");
        final Path htmlPath = reportDir.resolve(listener.getLastBaseFileName() + ".html");
        final Path mdPath = reportDir.resolve(listener.getLastBaseFileName() + ".md");

        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(htmlPath));
        assertTrue(Files.exists(mdPath));

        final JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath));
        final JsonNode actionNode = root.get("steps").get(0).get("actions").get(0);
        assertEquals("${verla.url}/index.html", actionNode.get("target").asText());
        assertEquals("https://localhost:8543/index.html", actionNode.get("resolvedTarget").asText());
        assertEquals("${user}", actionNode.get("value").asText());
        assertEquals("john_doe", actionNode.get("resolvedValue").asText());

        final String html = Files.readString(htmlPath);
        assertTrue(html.contains("https://localhost:8543/index.html"));
        assertTrue(html.contains("action-tpl-note"));

        final String md = Files.readString(mdPath);
        assertTrue(md.contains("https://localhost:8543/index.html (Tpl: ${verla.url}/index.html)"));
        assertTrue(md.contains("john_doe (Tpl: ${user})"));
    }
}
