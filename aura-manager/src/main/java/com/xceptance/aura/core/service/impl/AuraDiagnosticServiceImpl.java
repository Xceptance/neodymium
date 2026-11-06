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
package com.xceptance.aura.core.service.impl;

import com.xceptance.aura.core.dto.FailurePageResultDto;
import com.xceptance.aura.core.service.AuraDiagnosticService;
import com.xceptance.aura.core.service.AuraExecutionService;
import com.xceptance.aura.mcp.dto.DistilledFailureDto;
import com.xceptance.aura.mcp.dto.RunStatusResponseDto;
import com.xceptance.aura.mcp.service.AdaptivePayloadReporter;
import com.xceptance.aura.mcp.service.SeleniumErrorDistiller;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.neodymium.aura.AuraQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of AuraDiagnosticService providing status aggregation,
 * metric envelopes, and token-safe failure distillation.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Service
public class AuraDiagnosticServiceImpl implements AuraDiagnosticService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraDiagnosticServiceImpl.class);

    private final AuraReportDataService reportDataService;
    private final AuraQueueService queueService;
    private final AuraExecutionService executionService;
    private final SeleniumErrorDistiller errorDistiller;
    private final AdaptivePayloadReporter payloadReporter;

    public AuraDiagnosticServiceImpl(
        final AuraReportDataService reportDataService,
        final AuraQueueService queueService,
        final AuraExecutionService executionService,
        final SeleniumErrorDistiller errorDistiller,
        final AdaptivePayloadReporter payloadReporter)
    {
        this.reportDataService = reportDataService;
        this.queueService = queueService;
        this.executionService = executionService;
        this.errorDistiller = errorDistiller;
        this.payloadReporter = payloadReporter;
    }

    @Override
    public RunStatusResponseDto getRunStatus(final String runId)
    {
        if (runId == null || runId.isBlank())
        {
            final RunStatusResponseDto invalid = new RunStatusResponseDto();
            invalid.setStatus("ERROR");
            invalid.setSummary("Invalid or missing runId parameter.");
            invalid.setMetrics(new RunStatusResponseDto.MetricsDto(0, 0, 0, 0, 0L));
            return invalid;
        }

        try
        {
            final String coordinatorStatus = executionService.getCoordinatorRunStatus(runId);
            if ("ENQUEUED".equalsIgnoreCase(coordinatorStatus))
            {
                final RunStatusResponseDto enq = new RunStatusResponseDto();
                enq.setRunId(runId);
                enq.setStatus("ENQUEUED");
                enq.setMetrics(new RunStatusResponseDto.MetricsDto(0, 0, 0, 0, 0L));
                enq.setSummary("Test run is waiting in FIFO queue for previous test execution to complete.");
                enq.setNextSteps("Check back periodically using get_status(runId=\"" + runId + "\").");
                return enq;
            }

            final RunReportDto report = reportDataService.getRunReport(runId);
            final boolean isEmptyReport = report == null || report.getRunId() == null || report.getRunId().isBlank()
                || (report.getTotalCount() == 0 && (report.getExecutions() == null || report.getExecutions().isEmpty()));

            if (isEmptyReport)
            {
                final boolean isRunning = "RUNNING".equalsIgnoreCase(coordinatorStatus);
                final boolean isCancelled = "CANCELLED".equalsIgnoreCase(coordinatorStatus);
                final RunStatusResponseDto fallback = new RunStatusResponseDto();
                fallback.setRunId(runId);
                fallback.setStatus(isRunning ? "RUNNING" : (isCancelled ? "CANCELLED" : "NOT_FOUND"));
                fallback.setMetrics(new RunStatusResponseDto.MetricsDto(0, 0, 0, 0, 0L));
                fallback.setSummary(isRunning ? "Test run is currently active."
                    : (isCancelled ? "Test run was cancelled before execution." : "Test run not found."));
                fallback.setNextSteps(isRunning ? "Poll get_status(runId=\"" + runId + "\") until completion."
                    : (isCancelled ? "No further action needed for cancelled run." : "Verify runId parameter."));
                return fallback;
            }

            final List<DistilledFailureDto> distilledFailures = extractDistilledFailures(report);
            final String status = determineStatus(report);
            final int total = report.getTotalCount();
            final int passed = report.getPassCount() + report.getFixedCount();
            final int failed = report.getUnknownCount() + report.getKnownCount();
            final int skipped = report.getIgnoredCount();

            return payloadReporter.buildReport(
                runId,
                status,
                total,
                passed,
                failed,
                skipped,
                parseDurationMs(report.getDuration()),
                distilledFailures
            );
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to compile diagnostic status for runId '{}'", runId, e);
            final RunStatusResponseDto err = new RunStatusResponseDto();
            err.setRunId(runId);
            err.setStatus("ERROR");
            err.setSummary("Error retrieving execution status: " + e.getMessage());
            err.setMetrics(new RunStatusResponseDto.MetricsDto(0, 0, 0, 0, 0L));
            return err;
        }
    }

    @Override
    public FailurePageResultDto<DistilledFailureDto> getRunFailures(
        final String runId,
        final String category,
        final String cursor,
        final Integer limit)
    {
        final int effLimit = (limit != null && limit > 0) ? Math.min(limit, 10) : 5;
        int startIndex = 0;
        if (cursor != null && !cursor.isBlank())
        {
            try
            {
                startIndex = Math.max(0, Integer.parseInt(cursor.trim()));
            }
            catch (final NumberFormatException ignored)
            {
                startIndex = 0;
            }
        }

        final RunReportDto report;
        try
        {
            report = reportDataService.getRunReport(runId);
        }
        catch (final Exception e)
        {
            LOGGER.error("Error reading run report for failures on runId '{}'", runId, e);
            return new FailurePageResultDto<>(runId, category, String.valueOf(startIndex), null, false, effLimit, 0, Collections.emptyList());
        }

        if (report == null || report.getExecutions() == null)
        {
            return new FailurePageResultDto<>(runId, category, String.valueOf(startIndex), null, false, effLimit, 0, Collections.emptyList());
        }

        final List<DistilledFailureDto> matching = new ArrayList<>();
        for (final TestExecutionDto exec : report.getExecutions())
        {
            if ("FAILED".equalsIgnoreCase(exec.getStatus()) || (exec.getFailure() != null && !exec.getFailure().isBlank()))
            {
                final DistilledFailureDto distilled = errorDistiller.distill(
                    exec.getTestName() != null ? exec.getTestName() : exec.getTitle(),
                    exec.getFailure(),
                    List.of()
                );

                final String normalizedCategory = (category != null) ? category.trim() : null;
                final boolean matchesCategory;
                if (normalizedCategory == null || normalizedCategory.isBlank() || "ALL".equalsIgnoreCase(normalizedCategory))
                {
                    matchesCategory = true;
                }
                else
                {
                    final String clusterCat = distilled.getClusterCategory();
                    if (clusterCat == null)
                    {
                        matchesCategory = false;
                    }
                    else if ("UNCLASSIFIED".equalsIgnoreCase(normalizedCategory) || "UNKNOWN".equalsIgnoreCase(normalizedCategory))
                    {
                        final String upper = clusterCat.toUpperCase();
                        matchesCategory = upper.startsWith("UNCLASSIFIED")
                                || upper.startsWith("UNKNOWN");
                    }
                    else
                    {
                        final String upperCluster = clusterCat.toUpperCase();
                        final String upperFilter = normalizedCategory.toUpperCase();
                        matchesCategory = upperCluster.equals(upperFilter)
                                || upperCluster.startsWith(upperFilter + ":")
                                || upperCluster.startsWith(upperFilter + "_");
                    }
                }

                if (matchesCategory)
                {
                    matching.add(distilled);
                }
            }
        }

        final int total = matching.size();
        final int fromIndex = Math.min(startIndex, total);
        final int toIndex = Math.min(fromIndex + effLimit, total);
        final List<DistilledFailureDto> pageItems = matching.subList(fromIndex, toIndex);

        final boolean hasMore = toIndex < total;
        final String nextCursor = hasMore ? String.valueOf(toIndex) : null;

        return new FailurePageResultDto<>(
            runId,
            (category != null && !category.isBlank()) ? category : "ALL",
            String.valueOf(startIndex),
            nextCursor,
            hasMore,
            effLimit,
            total,
            pageItems
        );
    }

    @Override
    public DistilledFailureDto getExecutionFailure(final String runId, final String testName)
    {
        if (runId == null || runId.isBlank() || testName == null || testName.isBlank())
        {
            return null;
        }

        final RunReportDto report;
        try
        {
            report = reportDataService.getRunReport(runId);
        }
        catch (final Exception e)
        {
            LOGGER.error("Error reading run report for execution '{}' on runId '{}'", testName, runId, e);
            return null;
        }

        if (report == null || report.getExecutions() == null)
        {
            return null;
        }

        for (final TestExecutionDto exec : report.getExecutions())
        {
            final String name = exec.getTestName() != null ? exec.getTestName() : exec.getTitle();
            if (testName.equalsIgnoreCase(name) || testName.equalsIgnoreCase(exec.getTitle()) || testName.equalsIgnoreCase(exec.getId()))
            {
                if ("FAILED".equalsIgnoreCase(exec.getStatus()) || (exec.getFailure() != null && !exec.getFailure().isBlank()))
                {
                    return errorDistiller.distill(name, exec.getFailure(), List.of());
                }
            }
        }

        return null;
    }

    private List<DistilledFailureDto> extractDistilledFailures(final RunReportDto report)
    {
        final List<DistilledFailureDto> distilledFailures = new ArrayList<>();
        if (report.getExecutions() != null)
        {
            for (final TestExecutionDto exec : report.getExecutions())
            {
                if ("FAILED".equalsIgnoreCase(exec.getStatus()) || (exec.getFailure() != null && !exec.getFailure().isBlank()))
                {
                    final DistilledFailureDto distilled = errorDistiller.distill(
                        exec.getTestName() != null ? exec.getTestName() : exec.getTitle(),
                        exec.getFailure(),
                        List.of()
                    );
                    distilledFailures.add(distilled);
                }
            }
        }
        return distilledFailures;
    }

    private String determineStatus(final RunReportDto report)
    {
        if (report.getRunningCount() > 0)
        {
            return "RUNNING";
        }
        final int failed = report.getUnknownCount() + report.getKnownCount();
        if (failed > 0)
        {
            return "FAILED";
        }
        final int passed = report.getPassCount() + report.getFixedCount();
        if (passed == 0 && report.getIgnoredCount() > 0)
        {
            return "SKIPPED";
        }
        return "PASSED";
    }

    private long parseDurationMs(final String durationStr)
    {
        if (durationStr == null || durationStr.isBlank())
        {
            return 0L;
        }
        try
        {
            if (durationStr.endsWith("ms"))
            {
                return Long.parseLong(durationStr.replace("ms", "").trim());
            }
            if (durationStr.endsWith("s"))
            {
                final double seconds = Double.parseDouble(durationStr.replace("s", "").trim());
                return (long) (seconds * 1000);
            }
        }
        catch (final Exception ignored)
        {
        }
        return 0L;
    }
}
