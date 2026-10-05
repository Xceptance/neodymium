package com.xceptance.aura.mcp.service;

import com.xceptance.aura.mcp.dto.DistilledFailureDto;
import com.xceptance.aura.mcp.dto.FailureClusterDto;
import com.xceptance.aura.mcp.dto.RunStatusResponseDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formats test run results into an adaptive, context-window-safe payload.
 * Applies inline reporting for <= 5 failures, clustering for > 5 failures,
 * and high-entropy spillover notices when > 5 distinct failure categories exist.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Component
public class AdaptivePayloadReporter
{
    private static final int INLINE_THRESHOLD = 5;
    private static final int MAX_CLUSTERS_THRESHOLD = 5;

    /**
     * Builds an adaptive RunStatusResponseDto based on run statistics and failure details.
     *
     * @param runId target run ID
     * @param status execution state (ENQUEUED, RUNNING, PASSED, FAILED, CANCELLED, ERROR)
     * @param total total tests planned/executed
     * @param passed total passed tests
     * @param failed total failed tests
     * @param skipped total skipped tests
     * @param durationMs duration in milliseconds
     * @param allFailures list of distilled failure objects
     * @return populated, token-safe status response DTO
     */
    public RunStatusResponseDto buildReport(final String runId, final String status,
                                            final int total, final int passed, final int failed, final int skipped,
                                            final long durationMs, final List<DistilledFailureDto> allFailures)
    {
        final RunStatusResponseDto response = new RunStatusResponseDto();
        response.setRunId(runId);
        response.setStatus(status);

        // 1. Universal metrics envelope is always included
        final RunStatusResponseDto.MetricsDto metrics = new RunStatusResponseDto.MetricsDto(
            total, passed, failed, skipped, durationMs
        );
        response.setMetrics(metrics);

        if (failed == 0)
        {
            response.setSummary("Run completed with 0 failures.");
            response.setNextSteps("All tests passed. No failure investigation required.");
            return response;
        }

        final List<DistilledFailureDto> safeFailures = (allFailures != null) ? allFailures : List.of();

        // 2. Inline representation if failures <= 5
        if (failed <= INLINE_THRESHOLD)
        {
            response.setSummary(failed + " failure(s) detected. Full failure details are included inline.");
            response.setInlineFailures(safeFailures);
            response.setNextSteps("All " + failed + " failure details are included inline above. No further inspection tools required.");
            return response;
        }

        // 3. Cluster failures by category
        final Map<String, List<DistilledFailureDto>> clusterMap = new LinkedHashMap<>();
        for (final DistilledFailureDto f : safeFailures)
        {
            final String category = (f.getClusterCategory() != null) ? f.getClusterCategory() : "UNKNOWN_FAILURE";
            clusterMap.computeIfAbsent(category, k -> new ArrayList<>()).add(f);
        }

        final int clusterCount = clusterMap.size();

        // 4. Check for high failure entropy (> 5 distinct clusters)
        if (clusterCount > MAX_CLUSTERS_THRESHOLD)
        {
            response.setSummary(failed + " failures detected across " + clusterCount + " distinct root causes.");
            response.setHighEntropyNotice(new RunStatusResponseDto.HighEntropyNoticeDto(
                clusterCount,
                "High failure entropy detected (" + clusterCount + " distinct root causes). Detailed error causes omitted to preserve context window. Fetch paginated failures via tool 'get_run_failures' or full report via resource: test://runs/" + runId + "/summary"
            ));
            response.setNextSteps("High entropy failure storm. Use tool 'get_run_failures(runId=\"" + runId + "\", cursor=\"0\")' to paginate failures, or read resource 'test://runs/" + runId + "/summary'.");
            return response;
        }

        // 5. Clustered presentation (<= 5 clusters)
        final List<FailureClusterDto> clusters = new ArrayList<>();
        String sampleClusterCategory = null;
        String sampleAffectedSuite = null;

        for (final Map.Entry<String, List<DistilledFailureDto>> entry : clusterMap.entrySet())
        {
            final String cat = entry.getKey();
            final List<DistilledFailureDto> clusterFailures = entry.getValue();

            if (sampleClusterCategory == null)
            {
                sampleClusterCategory = cat;
            }

            final List<String> affectedSuites = clusterFailures.stream()
                .map(DistilledFailureDto::getTestName)
                .distinct()
                .limit(5)
                .toList();

            if (sampleAffectedSuite == null && !affectedSuites.isEmpty())
            {
                sampleAffectedSuite = affectedSuites.getFirst();
            }

            final DistilledFailureDto rep = clusterFailures.getFirst();
            final String sampleMsg = (rep.getCleanedStackTrace() != null && !rep.getCleanedStackTrace().isBlank())
                ? rep.getCleanedStackTrace().split("\\r?\\n")[0]
                : "Unknown error occurred";

            clusters.add(new FailureClusterDto(
                cat,
                clusterFailures.size(),
                sampleMsg,
                affectedSuites,
                rep.getCleanedStackTrace()
            ));
        }

        response.setSummary(failed + " failures detected across " + clusterCount + " distinct root causes. Failures have been clustered below.");
        response.setFailureClusters(clusters);

        final String nextStepsGuidance;
        if (sampleAffectedSuite != null && !sampleAffectedSuite.isBlank())
        {
            nextStepsGuidance = "Use tool 'get_execution_failure(runId=\"" + runId + "\", testName=\"" + sampleAffectedSuite
                + "\")' to inspect failure details for a single test, or 'get_run_failures(runId=\"" + runId
                + "\", category=\"" + sampleClusterCategory + "\")' to paginate all failures in this cluster.";
        }
        else
        {
            nextStepsGuidance = "Use tool 'get_run_failures(runId=\"" + runId + "\", category=\"" + sampleClusterCategory
                + "\")' to inspect detailed stack traces for specific clusters.";
        }
        response.setNextSteps(nextStepsGuidance);

        return response;
    }
}
