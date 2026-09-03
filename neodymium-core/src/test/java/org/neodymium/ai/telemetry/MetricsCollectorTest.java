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
package org.neodymium.ai.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.llm.LlmRequestSentEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;

/**
 * Unit test suite for MetricsCollector, SessionTelemetry, and TelemetrySink functionality.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class MetricsCollectorTest
{
    @TempDir
    Path tempFolder;

    @Test
    public void testCostCalculationForSupportedGeminiModels()
    {
        final TokenUsage usage = new TokenUsage(1_000_000, 100_000, 1_600_000, 500_000);

        // gemini-3.5-flash-lite: (1M * 0.30) + (0.1M * 2.50) + (0.5M * 0.075) = 0.30 + 0.25 + 0.0375 = 0.5875
        final double flashLiteCost = MetricsCollector.calculateCost(usage, "gemini-3.5-flash-lite");
        assertEquals(0.5875, flashLiteCost, 0.00001);

        // gemini-2.5-flash-lite: (1M * 0.10) + (0.1M * 0.40) + (0.5M * 0.025) = 0.10 + 0.04 + 0.0125 = 0.1525
        final double flashLite25Cost = MetricsCollector.calculateCost(usage, "gemini-2.5-flash-lite");
        assertEquals(0.1525, flashLite25Cost, 0.00001);

        // gemini-3.7-flash: (1M * 0.75) + (0.1M * 3.75) + (0.5M * 0.1875) = 0.75 + 0.375 + 0.09375 = 1.21875
        final double flash37Cost = MetricsCollector.calculateCost(usage, "gemini-3.7-flash");
        assertEquals(1.21875, flash37Cost, 0.00001);

        // gemini-3.5-flash: (1M * 0.50) + (0.1M * 3.00) + (0.5M * 0.125) = 0.50 + 0.30 + 0.0625 = 0.8625
        final double flash35Cost = MetricsCollector.calculateCost(usage, "gemini-3.5-flash");
        assertEquals(0.8625, flash35Cost, 0.00001);

        // gemini-3.6-flash: (1M * 0.50) + (0.1M * 3.00) + (0.5M * 0.125) = 0.50 + 0.30 + 0.0625 = 0.8625
        final double flash36Cost = MetricsCollector.calculateCost(usage, "gemini-3.6-flash");
        assertEquals(0.8625, flash36Cost, 0.00001);

        // Unknown model: should return 0.0 and skip price calculation
        final double unknownCost = MetricsCollector.calculateCost(usage, "custom-unknown-model");
        assertEquals(0.0, unknownCost, 0.00001);
    }

    @Test
    public void testMetricsCollectorEventAccumulationAndFileExport() throws Exception
    {
        final Path auditDir = this.tempFolder.resolve("ai-audit");
        Files.createDirectories(auditDir);
        final Path auditFile = auditDir.resolve("session-telemetry.json");
        final MetricsCollector collector = new MetricsCollector(auditFile);
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        eventBus.registerListener(collector);

        // Step 1: Start and finish step
        final PlaybookStep step1 = new PlaybookStep("Click Login");
        step1.setSourceFile("login.yaml");
        step1.setLineNumber(10);
        eventBus.dispatch(new StepStartedEvent(step1, 0));
        eventBus.dispatch(new StepFinishedEvent(step1, PlaybookStepStatus.SUCCESS));

        // Step 2: Start and finish healed step
        final PlaybookStep step2 = new PlaybookStep("Enter password");
        step2.setSourceFile("login.yaml");
        step2.setLineNumber(12);
        eventBus.dispatch(new StepStartedEvent(step2, 1));
        eventBus.dispatch(new StepFinishedEvent(step2, PlaybookStepStatus.HEALED));

        // LLM call
        final LlmRequest request = new LlmRequest("System", "User", List.of(), null, 0.2, 30);
        final TokenUsage usage = new TokenUsage(100_000, 10_000, 130_000, 20_000);
        final LlmResponse response = new LlmResponse("{\"action\":\"CLICK\"}", usage, "gemini-3.5-flash-lite");

        eventBus.dispatch(new LlmRequestSentEvent(request, "ACTION_EXTRACTION"));
        eventBus.dispatch(new LlmResponseReceivedEvent(request, response, 450, "ACTION_EXTRACTION"));

        // Finish session
        eventBus.dispatch(new SessionFinishedEvent(1250, true));

        // Verify thread-safe snapshot
        final SessionTelemetry telemetry = collector.getTelemetrySnapshot();
        assertEquals(2, telemetry.totalSteps());
        assertEquals(1, telemetry.healedSteps());
        assertEquals(100_000, telemetry.tokenUsageInput());
        assertEquals(10_000, telemetry.tokenUsageOutput());
        assertEquals(20_000, telemetry.tokenUsageCached());
        assertEquals(1250, telemetry.totalDurationMs());
        assertTrue(telemetry.estimatedCostUsd() > 0.0);

        // Verify JSON file export
        assertTrue(Files.exists(auditFile));
        final String jsonContent = Files.readString(auditFile);
        assertNotNull(jsonContent);

        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode root = mapper.readTree(jsonContent);
        assertEquals(2, root.get("totalSteps").asInt());
        assertEquals(1, root.get("healedSteps").asInt());
        assertEquals(100000, root.get("tokenUsageInput").asInt());
        assertEquals(10000, root.get("tokenUsageOutput").asInt());
        assertEquals(20000, root.get("tokenUsageCached").asInt());
        assertEquals(1250, root.get("totalDurationMs").asLong());
    }

    @Test
    public void testCustomTelemetrySinkConsumer()
    {
        final MetricsCollector collector = new MetricsCollector(null);
        final List<SessionTelemetry> receivedSnapshots = new ArrayList<>();

        final TelemetrySink customSink = receivedSnapshots::add;
        collector.addSink(customSink);

        final ExecutionEventBus eventBus = new ExecutionEventBus();
        eventBus.registerListener(collector);

        final PlaybookStep step = new PlaybookStep("Verify Header");
        step.setSourceFile("test.yaml");
        step.setLineNumber(1);
        eventBus.dispatch(new StepStartedEvent(step, 0));
        eventBus.dispatch(new StepFinishedEvent(step, PlaybookStepStatus.SUCCESS));
        eventBus.dispatch(new SessionFinishedEvent(500, true));

        assertEquals(1, receivedSnapshots.size());
        final SessionTelemetry snapshot = receivedSnapshots.get(0);
        assertEquals(1, snapshot.totalSteps());
        assertEquals(500, snapshot.totalDurationMs());
    }
}
