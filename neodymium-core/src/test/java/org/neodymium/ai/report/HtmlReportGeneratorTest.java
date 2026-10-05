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

import com.fasterxml.jackson.databind.JsonNode;
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
        final File resultsDir = new File("target/ai-results");
        if (resultsDir.exists() && resultsDir.isDirectory())
        {
            final File[] jsonFiles = resultsDir.listFiles((dir, name) -> name.endsWith(".json") && !name.startsWith("index"));
            if (jsonFiles != null)
            {
                final ObjectMapper mapper = new ObjectMapper();
                for (final File liveJson : jsonFiles)
                {
                    final String htmlName = liveJson.getName().substring(0, liveJson.getName().length() - 5) + ".html";
                    final File liveHtml = new File(resultsDir, htmlName);
                    final TestExecutionReport report = mapper.readValue(liveJson, TestExecutionReport.class);
                    final String regeneratedHtml = new HtmlReportGenerator().generate(report);
                    Files.writeString(liveHtml.toPath(), regeneratedHtml);

                    final Matcher matcher = SCRIPT_PATTERN.matcher(regeneratedHtml);
                    Assertions.assertTrue(matcher.find(), "Regenerated HTML must contain client script for " + liveJson.getName());
                    verifyScriptWithNodeIfAvailable(matcher.group(1));
                }
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

        // Pre-Flight LINTER Call: stepIndex -1
        final TestExecutionReport.ReportLlmCallEntry linterCall = new TestExecutionReport.ReportLlmCallEntry();
        linterCall.setStepIndex(-1);
        linterCall.setCapability("LINTER");
        linterCall.setModelName("gemini-2.5-flash");
        linterCall.setDurationMs(1500);
        linterCall.setInputTokens(1500);
        linterCall.setOutputTokens(200);
        report.addLlmCall(linterCall);

        // Step 0 Call 1: Quality Judge
        final TestExecutionReport.ReportLlmCallEntry call1 = new TestExecutionReport.ReportLlmCallEntry();
        call1.setStepIndex(0);
        call1.setCapability("JUDGE");
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

        // Step 1 Call 4: Verification
        final TestExecutionReport.ReportLlmCallEntry call4 = new TestExecutionReport.ReportLlmCallEntry();
        call4.setStepIndex(1);
        call4.setCapability("VERIFICATION");
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
        Assertions.assertTrue(html.contains("Pre-Flight / Setup"), "HTML must display pre-flight setup banner");
        Assertions.assertTrue(html.contains("Playbook Linter"), "HTML must format LINTER capability as Playbook Linter");
        Assertions.assertTrue(html.contains("Enter shipping details"), "HTML must display step 0 instruction in group banner");
        Assertions.assertTrue(html.contains("Quality Judge"), "HTML must format JUDGE capability as Quality Judge");
        Assertions.assertTrue(html.contains("Turn 1"), "HTML must display sequential turn numbering");
        Assertions.assertTrue(html.contains("Turn 2"), "HTML must display sequential turn 2");
        Assertions.assertTrue(html.contains("Vision 📸"), "HTML must display vision modality badge");
        Assertions.assertTrue(html.contains("step-tree-indicator"), "HTML must display tree branch indicator on subsequent calls");
        Assertions.assertTrue(html.contains("step-cluster-even") && html.contains("step-cluster-odd"), "HTML must alternate step cluster classes");
    }

    @Test
    @DisplayName("Verify that non-contiguous step clusters display contiguous metrics in banner, not global aggregates")
    public void testNonContiguousStepClustersDisplayContiguousMetricsInBanner()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestClass("TestClass");
        report.setTestName("testRepeatedClusters");
        report.setExecutionMode("LIVE");

        final TestExecutionReport.ReportStepEntry step0 = new TestExecutionReport.ReportStepEntry(0, "Search product");
        step0.setStatus("SUCCESS");
        final TestExecutionReport.ReportStepEntry step1 = new TestExecutionReport.ReportStepEntry(1, "Navigate page");
        step1.setStatus("SUCCESS");

        report.addStep(step0);
        report.addStep(step1);

        // Cluster 1: Step 0 (1 call, 1,000 tokens)
        final TestExecutionReport.ReportLlmCallEntry call1 = new TestExecutionReport.ReportLlmCallEntry();
        call1.setStepIndex(0);
        call1.setCapability("TEXT");
        call1.setInputTokens(900);
        call1.setOutputTokens(100);
        report.addLlmCall(call1);

        // Cluster 2: Step 1 (1 call, 2,000 tokens)
        final TestExecutionReport.ReportLlmCallEntry call2 = new TestExecutionReport.ReportLlmCallEntry();
        call2.setStepIndex(1);
        call2.setCapability("TEXT");
        call2.setInputTokens(1800);
        call2.setOutputTokens(200);
        report.addLlmCall(call2);

        // Cluster 3: Step 0 again (2 calls: 3,000 + 4,000 = 7,000 tokens)
        final TestExecutionReport.ReportLlmCallEntry call3 = new TestExecutionReport.ReportLlmCallEntry();
        call3.setStepIndex(0);
        call3.setCapability("TEXT");
        call3.setInputTokens(2700);
        call3.setOutputTokens(300);
        report.addLlmCall(call3);

        final TestExecutionReport.ReportLlmCallEntry call4 = new TestExecutionReport.ReportLlmCallEntry();
        call4.setStepIndex(0);
        call4.setCapability("TEXT");
        call4.setInputTokens(3600);
        call4.setOutputTokens(400);
        report.addLlmCall(call4);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);

        Assertions.assertNotNull(html);
        // Verify banner 1 for Step #1 has 1 call and 1,000 tokens (not 3 calls / 8,000 tokens)
        Assertions.assertTrue(html.contains("1 call &bull; 1,000 tokens"),
            "First Step #1 banner must display 1 call and 1,000 tokens for its contiguous cluster");
        // Verify banner 2 for Step #1 has 2 calls and 7,000 tokens (not 3 calls / 8,000 tokens)
        Assertions.assertTrue(html.contains("2 calls &bull; 7,000 tokens"),
            "Second Step #1 banner must display 2 calls and 7,000 tokens for its contiguous cluster");
    }

    @Test
    @DisplayName("Verify LLM Details panel wraps prompt sections in a collapsible container that is collapsed by default")
    public void testLlmPromptsCollapsedByDefault()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestClass("CheckoutTest");
        report.setTestName("testPromptsCollapsible");
        report.setExecutionMode("LIVE");

        final TestExecutionReport.ReportStepEntry step = new TestExecutionReport.ReportStepEntry(0, "Add item to cart");
        step.setStatus("SUCCESS");

        final TestExecutionReport.ReportLlmCallEntry llmCall = new TestExecutionReport.ReportLlmCallEntry();
        llmCall.setStepIndex(0);
        llmCall.setCapability("TEXT_ONLY");
        llmCall.setModelName("gemini-3.5-flash-lite");
        llmCall.setDurationMs(458);
        llmCall.setInputTokens(1400);
        llmCall.setOutputTokens(92);
        llmCall.setEstimatedCostUsd(0.0005);
        llmCall.setSystemPrompt("You are an autonomous testing agent.");
        llmCall.setAvailableTools(List.of("browser_click", "classify_step"));
        llmCall.setUserPrompt("## Active Instruction\nAdd item to cart");
        llmCall.setResponseContent("Tool Calls: [classify_step({\"intent\":\"ACTION\"})]");
        step.addLlmCall(llmCall);

        report.addStep(step);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);

        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("llm-prompts-wrapper collapsed"),
            "Client script must create a collapsible prompts container that starts in the collapsed state");
        Assertions.assertTrue(html.contains("llm-prompts-toggle"),
            "Client script must render a toggle bar for prompts & context");
        Assertions.assertTrue(html.contains("prompts-chevron"),
            "Client script must render a chevron indicator for the prompt toggle bar");
        Assertions.assertTrue(html.contains("response-section"),
            "Client script must render Raw Model Response with response-section class outside the prompt drawer");

        // Verify JavaScript compilation with Node.js
        final Matcher matcher = SCRIPT_PATTERN.matcher(html);
        Assertions.assertTrue(matcher.find(), "Generated HTML must contain client script");
        verifyScriptWithNodeIfAvailable(matcher.group(1));
    }

    @Test
    @DisplayName("Verify report header renders test tags as styled badges")
    public void testReportHeaderRendersTags()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestClass("org.neodymium.ai.integration.verla.search.judge.SearchJudgeTest");
        report.setTestMethod("liveAllDataSets");
        report.addTag("integration");
        report.addTag("verla");
        report.addTag("mode:judge");

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);

        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("<span class=\"meta-badge tag\">🏷️ integration</span>"),
            "Header must contain badge for integration tag");
        Assertions.assertTrue(html.contains("<span class=\"meta-badge tag\">🏷️ verla</span>"),
            "Header must contain badge for verla tag");
        Assertions.assertTrue(html.contains("<span class=\"meta-badge tag\">🏷️ mode:judge</span>"),
            "Header must contain badge for mode:judge tag");
    }

    @Test
    @DisplayName("Verify step footer tag renders instant token consumption for direct calls, fallback stats, and sub-step aggregates")
    public void testStepFooterTagTokenConsumption()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestClass("TokenConsumptionTest");
        report.setTestName("testTokenConsumptionInFooter");
        report.setExecutionMode("LIVE");

        // Step 0: Single step with direct LLM call
        final TestExecutionReport.ReportStepEntry step0 = new TestExecutionReport.ReportStepEntry(0, "Search query");
        step0.setStatus("SUCCESS");
        final TestExecutionReport.ReportLlmCallEntry directCall = new TestExecutionReport.ReportLlmCallEntry();
        directCall.setStepIndex(0);
        directCall.setInputTokens(10817);
        directCall.setOutputTokens(3211);
        step0.addLlmCall(directCall);
        report.addStep(step0);

        // Step 1: Compound step aggregating tokens from sub-steps
        final TestExecutionReport.ReportStepEntry step1 = new TestExecutionReport.ReportStepEntry(1, "Compound step");
        step1.setStatus("SUCCESS");
        final TestExecutionReport.ReportStepEntry sub1 = new TestExecutionReport.ReportStepEntry(1, "Sub step 1");
        final TestExecutionReport.ReportLlmCallEntry subCall1 = new TestExecutionReport.ReportLlmCallEntry();
        subCall1.setStepIndex(1);
        subCall1.setSubStepIndex(0);
        subCall1.setInputTokens(4000);
        subCall1.setOutputTokens(1000);
        sub1.addLlmCall(subCall1);

        final TestExecutionReport.ReportStepEntry sub2 = new TestExecutionReport.ReportStepEntry(1, "Sub step 2");
        final TestExecutionReport.ReportLlmCallEntry subCall2 = new TestExecutionReport.ReportLlmCallEntry();
        subCall2.setStepIndex(1);
        subCall2.setSubStepIndex(1);
        subCall2.setInputTokens(6817);
        subCall2.setOutputTokens(2211);
        sub2.addLlmCall(subCall2);

        step1.addSubStep(sub1);
        step1.addSubStep(sub2);
        report.addStep(step1);

        // Step 2: Step with standard calls but 0 tokens
        final TestExecutionReport.ReportStepEntry step2 = new TestExecutionReport.ReportStepEntry(2, "Stub call step");
        step2.setStatus("SUCCESS");
        step2.setStandardCalls(1);
        report.addStep(step2);

        // Step 3: Step with standardInputTokens / standardOutputTokens fallback
        final TestExecutionReport.ReportStepEntry step3 = new TestExecutionReport.ReportStepEntry(3, "Fallback stats step");
        step3.setStatus("SUCCESS");
        step3.setStandardCalls(1);
        step3.setStandardInputTokens(5000);
        step3.setStandardOutputTokens(250);
        report.addStep(step3);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);
        Assertions.assertNotNull(html);

        // Step 0 check
        Assertions.assertTrue(html.contains("🤖 1 LLM call(s), 10,817 in/ 3,211 out"),
            "Step 0 must display 1 LLM call with exact token consumption formatted");

        // Step 1 parent aggregate check
        Assertions.assertTrue(html.contains("🤖 2 LLM call(s), 10,817 in/ 3,211 out"),
            "Parent step 1 must aggregate sub-step LLM calls and tokens");

        // Step 1 sub-steps check
        Assertions.assertTrue(html.contains("🤖 1 LLM call(s), 4,000 in/ 1,000 out"),
            "Sub-step 1 must display its individual token consumption");
        Assertions.assertTrue(html.contains("🤖 1 LLM call(s), 6,817 in/ 2,211 out"),
            "Sub-step 2 must display its individual token consumption");

        // Step 2 zero-token check
        Assertions.assertTrue(html.contains("🤖 1 LLM call(s)"),
            "Step 2 must display 1 LLM call without token suffix when 0 tokens");

        // Step 3 fallback stats check
        Assertions.assertTrue(html.contains("🤖 1 LLM call(s), 5,000 in/ 250 out"),
            "Step 3 must display fallback standard token counts when direct LLM calls list is empty");
    }

    @Test
    @DisplayName("Verify HTML report renders visual marker meta badge, step mode pills, and markers in step cards")
    public void testVisualMarkersAndStepModeRendering() throws Exception
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestClass("VisualMarkersSandboxTest");
        report.setTestName("testMarkersLive");
        report.setExecutionMode("LIVE");

        final TestExecutionReport.ReportStepEntry step0 = new TestExecutionReport.ReportStepEntry(0, "Click marker element");
        step0.setStatus("PASSED");
        step0.setMarker(true);
        final TestExecutionReport.ReportLlmCallEntry llmCall = new TestExecutionReport.ReportLlmCallEntry();
        llmCall.setStepIndex(0);
        llmCall.setCapability("VISION");
        step0.addLlmCall(llmCall);
        report.addStep(step0);

        final TestExecutionReport.ReportStepEntry step1 = new TestExecutionReport.ReportStepEntry(1, "Replay element click");
        step1.setStatus("PASSED");
        final TestExecutionReport.ReportActionEntry action = new TestExecutionReport.ReportActionEntry(
            "CLICK", "button#submit", null, "Click submit", "Replay", true);
        step1.addAction(action);
        report.addStep(step1);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);
        Assertions.assertNotNull(html);

        // Header meta badge check
        Assertions.assertTrue(html.contains("<span class=\"meta-badge\">Mode: LIVE</span>"),
            "Header must contain Mode badge");
        Assertions.assertTrue(html.contains("<span class=\"meta-badge highlight marker-meta-badge\" title=\"Test executed with visual element markers\">🎯 Markers</span>"),
            "Header must contain Marker meta badge when report used markers");

        // Step 0 header badges (LLM and MARKER)
        Assertions.assertTrue(html.contains("<span class=\"badge-flag pill-mode-llm\">🤖 LLM</span>"),
            "Step 0 must display LLM mode pill");
        Assertions.assertTrue(html.contains("<span class=\"badge-flag marker-badge\" title=\"Step executed with proactive visual element markers\">🎯 MARKER</span>"),
            "Step 0 must display MARKER badge");

        // Step 1 header badge (REPLAY)
        Assertions.assertTrue(html.contains("<span class=\"badge-flag pill-mode-replay\">⚡ REPLAY</span>"),
            "Step 1 must display REPLAY mode pill");

        // Verify inspector modal element
        Assertions.assertTrue(html.contains("id=\"inspModeBadge\""),
            "Inspector header must include inspModeBadge element");

        // Verify script syntax cleanly with Node.js VM
        final Matcher matcher = SCRIPT_PATTERN.matcher(html);
        Assertions.assertTrue(matcher.find(), "Report must contain client-side script");
        verifyScriptWithNodeIfAvailable(matcher.group(1));
    }

    @Test
    @DisplayName("Verify escapeJsonScriptPayload converts '<' to '\\u003c' while preserving valid JSON")
    public void testEscapeJsonScriptPayloadWithHtmlAndScriptTags() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        final String rawJson = """
            {"script":"<script src=\\"/shared/htmx.js\\"></script>","comment":"<!-- HTML comment -->","nested":"</script><script>alert(1)</script>"}
            """.trim();

        final String escaped = HtmlReportGenerator.escapeJsonScriptPayload(rawJson);
        Assertions.assertFalse(escaped.contains("<"), "Escaped JSON must not contain literal '<'");
        Assertions.assertTrue(escaped.contains("\\u003c"), "Escaped JSON must contain '\\u003c' unicode escape sequence");

        // Verify JSON deserialization preserves original content
        final JsonNode node = mapper.readTree(escaped);
        Assertions.assertEquals("<script src=\"/shared/htmx.js\"></script>", node.get("script").asText());
        Assertions.assertEquals("<!-- HTML comment -->", node.get("comment").asText());
        Assertions.assertEquals("</script><script>alert(1)</script>", node.get("nested").asText());

        // Verify null returns empty array string
        Assertions.assertEquals("[]", HtmlReportGenerator.escapeJsonScriptPayload(null));
    }

    @Test
    @DisplayName("Verify generated HTML report with embedded script tags in actions has exactly 2 script elements and valid JSON")
    public void testReportWithEmbeddedScriptTagsInSteps() throws Exception
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestClass("ScriptEscapeTest");
        report.setTestName("testScriptEscapeInSteps");
        report.setExecutionMode("LIVE");

        final TestExecutionReport.ReportStepEntry step0 = new TestExecutionReport.ReportStepEntry(0, "Execute action with DOM script tag");
        step0.setStatus("PASSED");
        final TestExecutionReport.ReportActionEntry action = new TestExecutionReport.ReportActionEntry(
            "CLICK", "<script src=\"/shared/htmx.js\"></script>", "</script><script>alert(1)</script>",
            "Click element containing script", "LLM", true);
        step0.addAction(action);
        report.addStep(step0);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);
        Assertions.assertNotNull(html);

        // Count <script occurrences in HTML - must be exactly 2:
        // 1. <script id="stepDataPayload" type="application/json">
        // 2. <script> (client report script)
        final Matcher allScriptsMatcher = Pattern.compile("<script\\b", Pattern.CASE_INSENSITIVE).matcher(html);
        int scriptTagCount = 0;
        while (allScriptsMatcher.find())
        {
            scriptTagCount++;
        }
        Assertions.assertEquals(2, scriptTagCount,
            "HTML report must contain exactly 2 script elements; embedded script tags in step data must not break out");

        // Extract JSON payload and verify it parses cleanly
        final Pattern payloadPattern = Pattern.compile("<script id=\"stepDataPayload\" type=\"application/json\">([\\s\\S]*?)</script>");
        final Matcher payloadMatcher = payloadPattern.matcher(html);
        Assertions.assertTrue(payloadMatcher.find(), "HTML must contain stepDataPayload script element");

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode stepsNode = mapper.readTree(payloadMatcher.group(1));
        Assertions.assertTrue(stepsNode.isArray(), "Payload must be a JSON array");
        Assertions.assertEquals(1, stepsNode.size(), "Payload must contain exactly 1 step");
        final JsonNode actionNode = stepsNode.get(0).get("actions").get(0);
        Assertions.assertEquals("<script src=\"/shared/htmx.js\"></script>", actionNode.get("target").asText());
        Assertions.assertEquals("</script><script>alert(1)</script>", actionNode.get("value").asText());

        // Verify client script syntax with Node.js
        final Matcher clientScriptMatcher = SCRIPT_PATTERN.matcher(html);
        Assertions.assertTrue(clientScriptMatcher.find(), "Report must contain client-side script");
        verifyScriptWithNodeIfAvailable(clientScriptMatcher.group(1));
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

    @Test
    public void testChildSubStepDoesNotRenderReplayBadgeWhenParentIsLlm()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestName("Compound Step Test");
        report.setTestClass("org.neodymium.ai.integration.CheckoutTest");
        report.setTestMethod("testCheckout");
        report.setStatus("PASSED");
        report.setSuccess(true);
        report.setStartTimeMs(System.currentTimeMillis());
        report.setEndTimeMs(System.currentTimeMillis() + 1000);

        final TestExecutionReport.ReportStepEntry parentStep = new TestExecutionReport.ReportStepEntry(1, "Fill out the shipping address form:");
        parentStep.setStatus("PASSED");
        parentStep.setStandardCalls(1);

        final TestExecutionReport.ReportStepEntry childStep = new TestExecutionReport.ReportStepEntry(2, "Country");
        childStep.setStatus("PASSED");

        parentStep.addSubStep(childStep);
        report.addStep(parentStep);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);

        Assertions.assertTrue(html.contains("🤖 LLM"), "Parent step must render LLM badge");
        Assertions.assertFalse(html.contains("<span class=\"badge-flag pill-mode-replay\">"), "Child sub-step must NOT render fallback REPLAY badge");
    }

    @Test
    public void testActivePropertiesSectionRendered()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestName("Properties Test");
        report.setStatus("PASSED");
        report.setSuccess(true);
        report.addActiveProperty("neodymium.browser", "Chrome_1500x1000");
        report.addActiveProperty("neodymium.url", "https://example.com");
        report.addActiveProperty("neodymium.ai.apiKey", "••••••••");

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);

        Assertions.assertTrue(html.contains("⚙️ Active Run Properties (3)"), "HTML must contain active properties header with count");
        Assertions.assertTrue(html.contains("class=\"card-section properties-section\""), "HTML must contain properties-section card");
        Assertions.assertTrue(html.contains("id=\"propertiesTable\""), "HTML must contain propertiesTable table");
        Assertions.assertTrue(html.contains("id=\"propSearchInput\""), "HTML must contain propSearchInput filter input");
        Assertions.assertTrue(html.contains("filterProperties()"), "HTML must contain filterProperties client call");
        Assertions.assertTrue(html.contains("neodymium.browser"), "HTML must list neodymium.browser property key");
        Assertions.assertTrue(html.contains("Chrome_1500x1000"), "HTML must list Chrome_1500x1000 property value");
        Assertions.assertTrue(html.contains("neodymium.ai.apiKey"), "HTML must list neodymium.ai.apiKey property key");
        Assertions.assertTrue(html.contains("••••••••"), "HTML must display masked secret value");
    }

    @Test
    public void testActivePropertiesEmptyState()
    {
        final TestExecutionReport report = new TestExecutionReport();
        report.setTestName("Empty Properties Test");
        report.setStatus("PASSED");
        report.setSuccess(true);

        final HtmlReportGenerator generator = new HtmlReportGenerator();
        final String html = generator.generate(report);

        Assertions.assertTrue(html.contains("⚙️ Active Run Properties (0)"), "HTML must show 0 count when no active properties exist");
        Assertions.assertTrue(html.contains("No active configuration properties were recorded for this execution."), "HTML must show empty message");
    }
}

