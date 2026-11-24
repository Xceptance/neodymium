/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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

import com.xceptance.aura.report.dto.BatchSummaryDto;
import com.xceptance.aura.report.dto.ReportDifferentialDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.ReportDifferentialService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller providing JSON endpoints for Aura Report Manager.
 *
 * @author Xceptance GmbH 2026
 */
@RestController
@RequestMapping("/api")
public class AuraReportRestController
{
    private final AuraReportDataService dataService;
    private final ReportDifferentialService differentialService;

    public AuraReportRestController(final AuraReportDataService dataService)
    {
        this(dataService, null);
    }

    @Autowired
    public AuraReportRestController(
        final AuraReportDataService dataService,
        @Autowired(required = false) final ReportDifferentialService differentialService)
    {
        this.dataService = dataService;
        this.differentialService = (differentialService != null) ? differentialService : new ReportDifferentialService();
    }

    @GetMapping("/report/batches")
    public List<BatchSummaryDto> getBatches()
    {
        return dataService.getAllBatches();
    }

    @GetMapping("/report/runs/{runId}")
    public RunReportDto getRunReport(@PathVariable("runId") final String runId)
    {
        return dataService.getRunReport(runId);
    }

    @GetMapping("/report/differential")
    public ReportDifferentialDto getReportDifferential(
        @RequestParam(name = "batch", required = false) final String batch,
        @RequestParam(name = "limit", defaultValue = "5") final int limit)
    {
        return differentialService.computeBatchDifferential(batch, limit);
    }

    @GetMapping("/v1/aura-manager/status")
    public Map<String, Object> getAuraManagerStatus()
    {
        return Map.of("running", true, "embedded", true, "url", "/aura-test-manager");
    }
}
