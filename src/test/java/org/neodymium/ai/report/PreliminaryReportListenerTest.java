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
import java.util.List;
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
    @DisplayName("Verify preliminary disk report generation on successful test run")
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

        // Markdown Content check
        final String mdContent = Files.readString(mdPath);
        assertTrue(mdContent.contains("# ✅ Test Execution Report:"));
        assertTrue(mdContent.contains("**PASSED**"));
        assertTrue(mdContent.contains("Open Home Page"));
        assertTrue(mdContent.contains("Search for Shoes"));
        assertTrue(mdContent.contains("NAVIGATE"));

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
        assertEquals(1200, root.get("metrics").get("tokenUsageInput").asLong());
        assertEquals(150, root.get("metrics").get("tokenUsageOutput").asLong());
        assertEquals(400, root.get("metrics").get("tokenUsageCached").asLong());
        assertEquals(1, root.get("metrics").get("healedSteps").asInt());
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
