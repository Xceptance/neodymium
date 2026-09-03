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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.xceptance.aura.report.service;

import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Tests that an in-progress run report is served from the live run buffer (the authoritative in-memory source during
 * execution) instead of a potentially stale run.json on disk, while completed runs continue to read from disk.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportLiveRunBufferTest
{
    @Autowired
    private AuraReportDataService dataService;

    @Autowired
    private TestRunRepository runRepository;

    @Autowired
    private LocalRunJsonStorageService storageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testInProgressRunPrefersLiveRunBufferOverStaleRunJson() throws Exception
    {
        final String runId = "live-buffer-run-" + System.nanoTime();
        dataService.startRun(runId, "Live Batch", "US", "queue");

        writeStaleRunJson(runId, "stale-exec");

        dataService.ingestExecution(runId, executionPayload("live-exec", "passed"));

        final RunReportDto report = dataService.getRunReport(runId);
        Assertions.assertTrue(report.isInProgress());
        Assertions.assertEquals(1, report.getExecutions().size(),
            "In-progress report must reflect the live run buffer, not the stale run.json");
        Assertions.assertEquals("live-exec", report.getExecutions().get(0).getId());
    }

    @Test
    public void testCompletedRunStillReadsFromRunJson() throws Exception
    {
        final String runId = "completed-run-" + System.nanoTime();
        final TestRunEntity runEntity = new TestRunEntity(
            runId,
            "Completed Batch",
            "COMPLETED",
            "queue",
            "US",
            "en_US",
            "Chrome",
            "2026-08-17 12:00:00",
            System.currentTimeMillis()
        );
        runRepository.save(runEntity);

        writeStaleRunJson(runId, "stale-exec");

        dataService.ingestExecution(runId, executionPayload("live-exec", "passed"));

        final RunReportDto report = dataService.getRunReport(runId);
        Assertions.assertFalse(report.isInProgress());
        Assertions.assertEquals(1, report.getExecutions().size(),
            "Completed run report must keep reading from run.json on disk");
        Assertions.assertEquals("stale-exec", report.getExecutions().get(0).getId());
    }

    private void writeStaleRunJson(final String runId, final String execId) throws Exception
    {
        final com.fasterxml.jackson.databind.node.ObjectNode execNode = objectMapper.createObjectNode();
        execNode.put("id", execId);
        execNode.put("testClass", "StaleTest");
        execNode.put("title", "stale");
        execNode.put("status", "passed");
        execNode.put("browser", "Chrome");
        execNode.put("location", "US");

        final com.fasterxml.jackson.databind.node.ObjectNode metricsNode = objectMapper.createObjectNode();
        metricsNode.set(execId, execNode);

        final com.fasterxml.jackson.databind.node.ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("runId", runId);
        rootNode.set("executionMetrics", metricsNode);

        storageService.writeRunJson(runId, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode));
    }

    private Map<String, Object> executionPayload(final String execId, final String status)
    {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("id", execId);
        payload.put("testClass", "LiveTest");
        payload.put("testMethod", "executeTest");
        payload.put("title", "live");
        payload.put("status", status);
        payload.put("browser", "Chrome");
        payload.put("location", "US");
        return payload;
    }
}
