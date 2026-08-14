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
package com.xceptance.aura.report.controller;

import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.RunStorageSyncService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for live execution streaming, run lifecycle signals, storage resyncing, and database management.
 *
 * @author Xceptance GmbH 2026
 */
@RestController
@RequestMapping("/api/v1")
public class ReportIngestionController
{
    private final AuraReportDataService reportDataService;
    private final RunStorageSyncService storageSyncService;

    public ReportIngestionController(final AuraReportDataService reportDataService, final RunStorageSyncService storageSyncService)
    {
        this.reportDataService = reportDataService;
        this.storageSyncService = storageSyncService;
    }

    @PostMapping("/runs/start")
    public ResponseEntity<Map<String, Object>> startRun(@RequestBody final Map<String, String> payload)
    {
        final String batchName = payload.getOrDefault("batchName", "US Nightly Regression");
        final String env = payload.getOrDefault("environment", "Staging");
        final String trigger = payload.getOrDefault("triggerSource", "Jenkins CI");
        final String runId = reportDataService.startRun(batchName, env, trigger);
        return ResponseEntity.ok(Map.of("runId", runId, "status", "IN_PROGRESS", "message", "Run initialized successfully."));
    }

    @PostMapping("/runs/{runId}/executions")
    public ResponseEntity<Map<String, String>> addExecution(@PathVariable final String runId, @RequestBody final Map<String, Object> payload)
    {
        reportDataService.ingestExecution(runId, payload);
        return ResponseEntity.ok(Map.of("runId", runId, "status", "ACCEPTED"));
    }

    @PostMapping("/runs/{runId}/finish")
    public ResponseEntity<Map<String, String>> finishRun(@PathVariable final String runId)
    {
        reportDataService.finishRun(runId);
        return ResponseEntity.ok(Map.of("runId", runId, "status", "COMPLETED", "message", "Run finished and saved to disk."));
    }

    @DeleteMapping("/runs/{runId}")
    public ResponseEntity<Map<String, String>> deleteRun(@PathVariable final String runId)
    {
        reportDataService.removeRunFromHistory(runId);
        return ResponseEntity.ok(Map.of("runId", runId, "status", "DELETED", "message", "Run removed from history."));
    }

    @PostMapping("/storage/resync")
    public ResponseEntity<Map<String, Object>> resyncStorage()
    {
        final int importedCount = storageSyncService.syncLocalRunStorage();
        return ResponseEntity.ok(Map.of("importedCount", importedCount, "message", "Storage re-sync completed. Imported " + importedCount + " new run reports."));
    }

    @PostMapping("/database/clear")
    public ResponseEntity<Map<String, Object>> clearDatabase()
    {
        reportDataService.clearAllDatabaseEntries();
        return ResponseEntity.ok(Map.of("status", "CLEARED", "message", "All entries removed from all database tables."));
    }
}
