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
package com.xceptance.aura.report.service;

import com.xceptance.aura.mcp.dto.DistilledFailureDto;
import com.xceptance.aura.mcp.service.SeleniumErrorDistiller;
import com.xceptance.aura.report.dto.BatchLatestVariationDto;
import com.xceptance.aura.report.dto.ReportDifferentialDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseVariationHistoryDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.dto.TestTransitionDto;
import com.xceptance.aura.report.dto.TestTransitionDto.TransitionType;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestRunRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service evaluating test lifecycle state transitions across multiple execution reports,
 * identifying tests that turned green, unticketed failures (ticket gaps), known bugs, and regressions.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Service
public class ReportDifferentialService
{
    private static final Logger LOG = LoggerFactory.getLogger(ReportDifferentialService.class);

    private final AuraReportDataService dataService;
    private final TestRunRepository runRepository;
    private final SeleniumErrorDistiller errorDistiller;

    /**
     * Constructs the report differential service with optional Spring dependencies.
     *
     * @param dataService report data aggregation service
     * @param runRepository test run database repository
     * @param errorDistiller Selenium error distillation engine
     */
    @Autowired
    public ReportDifferentialService(
        @Autowired(required = false) final AuraReportDataService dataService,
        @Autowired(required = false) final TestRunRepository runRepository,
        @Autowired(required = false) final SeleniumErrorDistiller errorDistiller)
    {
        this.dataService = dataService;
        this.runRepository = runRepository;
        this.errorDistiller = (errorDistiller != null) ? errorDistiller : new SeleniumErrorDistiller();
    }

    /**
     * Default constructor for standalone or fallback instantiation.
     */
    public ReportDifferentialService()
    {
        this(null, null, null);
    }

    /**
     * Computes multi-report differential analysis across the provided test runs.
     *
     * @param runs test runs to analyze chronologically
     * @return structured differential report DTO
     */
    public ReportDifferentialDto computeDifferential(final List<TestRunEntity> runs)
    {
        return computeDifferential(runs, Collections.emptyList(), this.dataService);
    }

    /**
     * Computes multi-report differential analysis across the provided test runs,
     * with fallback support for pre-aggregated latest variations.
     *
     * @param runs test runs to analyze chronologically
     * @param fallbackVariations fallback variations if runs or dataService are empty
     * @param reportDataService report data service for loading individual run reports
     * @return structured differential report DTO
     */
    public ReportDifferentialDto computeDifferential(
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> fallbackVariations,
        final AuraReportDataService reportDataService)
    {
        final List<TestRunEntity> validRuns = (runs != null)
            ? runs.stream().filter(Objects::nonNull).filter(r -> !Boolean.TRUE.equals(r.getIsDeleted())).toList()
            : Collections.emptyList();

        if (validRuns.isEmpty() || reportDataService == null)
        {
            return computeFromFallbackVariations(validRuns, fallbackVariations);
        }

        // Sort runs chronologically ascending (oldest run first, newest run last)
        final List<TestRunEntity> sortedRuns = new ArrayList<>(validRuns);
        sortedRuns.sort(Comparator.comparing(
            (final TestRunEntity r) -> (r.getStartTimeMs() != null && r.getStartTimeMs() > 0)
                ? r.getStartTimeMs()
                : TestBaseVariationHistoryDto.parseTimestampToEpoch(null, r.getId()),
            Comparator.naturalOrder()
        ));

        final List<String> analyzedRunIds = sortedRuns.stream()
            .map(TestRunEntity::getId)
            .filter(Objects::nonNull)
            .toList();

        final Map<String, VariationTracker> variationTrackers = new LinkedHashMap<>();

        for (final TestRunEntity run : sortedRuns)
        {
            final String runId = run.getId();
            final RunReportDto report;
            try
            {
                report = reportDataService.getRunReport(runId);
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to load run report for runId={}: {}", runId, e.getMessage());
                continue;
            }

            if (report == null || report.getExecutions() == null)
            {
                continue;
            }

            for (final TestExecutionDto exec : report.getExecutions())
            {
                if (exec == null)
                {
                    continue;
                }

                final String varId = AuraReportDataService.generateVariationId(
                    exec.getTestClass(), exec.getTestMethod(), exec.getTitle(), exec.getLocation(), exec.getBrowser()
                );

                final VariationTracker tracker = variationTrackers.computeIfAbsent(varId, k -> new VariationTracker(
                    k, varId, exec.getTestClass(), exec.getTestMethod(), exec.getTitle(), exec.getBrowser(), exec.getLocation()
                ));

                final boolean isFailed = isExecutionFailed(exec);
                final boolean isPassed = isExecutionPassed(exec, isFailed);
                final boolean hasBugs = (exec.getBugs() != null && !exec.getBugs().isEmpty())
                    || (exec.getPrimaryBug() != null && !exec.getPrimaryBug().isBlank());

                final String failureText = AuraReportDataService.extractFailureText(exec);

                final List<String> bugs = (exec.getBugs() != null) ? exec.getBugs() : Collections.emptyList();
                final List<String> bugSeverities = (exec.getBugDetails() != null)
                    ? exec.getBugDetails().stream().map(TestExecutionDto.BugDetailDto::getSeverity).filter(Objects::nonNull).toList()
                    : Collections.emptyList();

                final List<String> tags = new ArrayList<>();
                if (exec.getBrowser() != null && !exec.getBrowser().isBlank())
                {
                    tags.add(exec.getBrowser());
                }
                if (exec.getLocation() != null && !exec.getLocation().isBlank())
                {
                    tags.add(exec.getLocation());
                }

                tracker.addSnapshot(new ExecutionSnapshot(
                    runId,
                    exec.getId(),
                    exec.getStatus(),
                    isPassed,
                    isFailed,
                    hasBugs,
                    bugs,
                    bugSeverities,
                    failureText,
                    tags
                ));
            }
        }

        if (variationTrackers.isEmpty())
        {
            return computeFromFallbackVariations(validRuns, fallbackVariations);
        }

        final List<TestTransitionDto> turnedGreen = new ArrayList<>();
        final List<TestTransitionDto> unticketedFailures = new ArrayList<>();
        final List<TestTransitionDto> knownFailures = new ArrayList<>();
        final List<TestTransitionDto> regressions = new ArrayList<>();
        final List<TestTransitionDto> allTransitions = new ArrayList<>();
        int stablePassCount = 0;

        for (final VariationTracker tracker : variationTrackers.values())
        {
            final TestTransitionDto transition = evaluateTransition(tracker);
            allTransitions.add(transition);

            switch (transition.getTransition())
            {
                case TURNED_GREEN -> turnedGreen.add(transition);
                case UNTICKETED_FAIL -> unticketedFailures.add(transition);
                case KNOWN_FAIL -> knownFailures.add(transition);
                case REGRESSION -> regressions.add(transition);
                case STABLE_PASS -> stablePassCount++;
                case INTERMITTENT_FLAKY -> {
                    if (transition.getLatestStatus().contains("fail") || transition.getLatestStatus().contains("error"))
                    {
                        if (transition.hasBugs())
                        {
                            knownFailures.add(transition);
                        }
                        else
                        {
                            unticketedFailures.add(transition);
                        }
                    }
                    else
                    {
                        turnedGreen.add(transition);
                    }
                }
            }
        }

        return new ReportDifferentialDto(
            analyzedRunIds,
            allTransitions.size(),
            turnedGreen.size(),
            unticketedFailures.size(),
            knownFailures.size(),
            regressions.size(),
            stablePassCount,
            turnedGreen,
            unticketedFailures,
            knownFailures,
            regressions,
            allTransitions
        );
    }

    /**
     * Resolves the latest N completed runs for a batch and computes their differential.
     *
     * @param batchName batch name filter (or null/"all")
     * @param limit maximum historical runs to inspect
     * @return structured differential report DTO
     */
    public ReportDifferentialDto computeBatchDifferential(final String batchName, final int limit)
    {
        final int effectiveLimit = (limit > 0) ? Math.min(limit, 50) : 5;
        final List<TestRunEntity> matchingRuns;

        if (runRepository != null)
        {
            if (batchName != null && !batchName.isBlank() && !"all".equalsIgnoreCase(batchName) && !"Default".equalsIgnoreCase(batchName))
            {
                matchingRuns = runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(batchName).stream()
                    .limit(effectiveLimit)
                    .toList();
            }
            else
            {
                matchingRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
                    .limit(effectiveLimit)
                    .toList();
            }
        }
        else if (dataService != null)
        {
            matchingRuns = dataService.getAllRuns().stream().limit(effectiveLimit).toList();
        }
        else
        {
            matchingRuns = Collections.emptyList();
        }

        return computeDifferential(matchingRuns);
    }

    private TestTransitionDto evaluateTransition(final VariationTracker tracker)
    {
        final List<ExecutionSnapshot> snapshots = tracker.snapshots;
        if (snapshots.isEmpty())
        {
            return new TestTransitionDto(
                tracker.key, tracker.variationId, tracker.testClass, tracker.testMethod, tracker.title,
                tracker.browser, tracker.location, TransitionType.STABLE_PASS, "passed-clean", "", "", "", "",
                Collections.emptyList(), Collections.emptyList(), "", "", "", Collections.emptyList()
            );
        }

        final ExecutionSnapshot latest = snapshots.get(snapshots.size() - 1);
        final boolean isMultiRun = snapshots.size() >= 2;

        boolean hadPriorFailure = false;
        boolean hadPriorPass = false;
        String priorFailedRunId = "";
        String priorFailedStatus = "";
        String priorFailureText = "";

        if (isMultiRun)
        {
            for (int i = 0; i < snapshots.size() - 1; i++)
            {
                final ExecutionSnapshot snap = snapshots.get(i);
                if (snap.isFailed)
                {
                    hadPriorFailure = true;
                    priorFailedRunId = snap.runId;
                    priorFailedStatus = snap.status;
                    if (snap.failureText != null && !snap.failureText.isBlank())
                    {
                        priorFailureText = snap.failureText;
                    }
                }
                if (snap.isPassed)
                {
                    hadPriorPass = true;
                }
            }
        }

        final TransitionType transitionType;
        if (latest.isPassed)
        {
            if (hadPriorFailure || "succeeded-fixed".equalsIgnoreCase(latest.status))
            {
                transitionType = TransitionType.TURNED_GREEN;
            }
            else
            {
                transitionType = TransitionType.STABLE_PASS;
            }
        }
        else if (latest.isFailed)
        {
            if (hadPriorPass && !hadPriorFailure)
            {
                transitionType = TransitionType.REGRESSION;
            }
            else if (latest.hasBugs || "failed-known".equalsIgnoreCase(latest.status))
            {
                transitionType = TransitionType.KNOWN_FAIL;
            }
            else
            {
                transitionType = TransitionType.UNTICKETED_FAIL;
            }
        }
        else
        {
            transitionType = TransitionType.STABLE_PASS;
        }

        final String rawFailureToDistill = latest.isFailed
            ? latest.failureText
            : (hadPriorFailure ? priorFailureText : "");

        final DistilledFailureDto distilled = (errorDistiller != null && rawFailureToDistill != null && !rawFailureToDistill.isBlank())
            ? errorDistiller.distill(tracker.title, rawFailureToDistill, Collections.emptyList())
            : new DistilledFailureDto();

        return new TestTransitionDto(
            tracker.key,
            tracker.variationId,
            tracker.testClass,
            tracker.testMethod,
            tracker.title,
            tracker.browser,
            tracker.location,
            transitionType,
            latest.status,
            latest.runId,
            latest.executionId,
            priorFailedRunId,
            priorFailedStatus,
            latest.bugs,
            latest.bugSeverities,
            distilled.getClusterCategory(),
            distilled.getCleanedStackTrace(),
            distilled.getLocator(),
            latest.tags
        );
    }

    private ReportDifferentialDto computeFromFallbackVariations(
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> fallbackVariations)
    {
        final List<String> runIds = (runs != null) ? runs.stream().map(TestRunEntity::getId).filter(Objects::nonNull).toList() : Collections.emptyList();
        if (fallbackVariations == null || fallbackVariations.isEmpty())
        {
            return new ReportDifferentialDto(runIds, 0, 0, 0, 0, 0, 0,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        final List<TestTransitionDto> turnedGreen = new ArrayList<>();
        final List<TestTransitionDto> unticketedFailures = new ArrayList<>();
        final List<TestTransitionDto> knownFailures = new ArrayList<>();
        final List<TestTransitionDto> regressions = new ArrayList<>();
        final List<TestTransitionDto> allTransitions = new ArrayList<>();
        int stablePassCount = 0;

        for (final BatchLatestVariationDto v : fallbackVariations)
        {
            if (v == null)
            {
                continue;
            }

            final String st = (v.getStatus() != null) ? v.getStatus().toLowerCase(Locale.ROOT) : "";
            final boolean isPassed = st.contains("pass") || st.contains("success") || st.contains("fixed") || st.contains("healed");
            final boolean isFailed = st.contains("fail") || st.contains("error");
            final boolean hasBugs = v.hasBugs();

            final TransitionType tType;
            if ("succeeded-fixed".equals(st) || (v.isFlaky() && isPassed))
            {
                tType = TransitionType.TURNED_GREEN;
            }
            else if (isFailed)
            {
                tType = hasBugs ? TransitionType.KNOWN_FAIL : TransitionType.UNTICKETED_FAIL;
            }
            else
            {
                tType = TransitionType.STABLE_PASS;
            }

            final DistilledFailureDto distilled = (errorDistiller != null && v.getFlakyReason() != null && !v.getFlakyReason().isBlank())
                ? errorDistiller.distill(v.getKey(), v.getFlakyReason(), Collections.emptyList())
                : new DistilledFailureDto();

            final TestTransitionDto dto = new TestTransitionDto(
                v.getKey(),
                v.getVariationId(),
                "",
                "",
                v.getKey(),
                v.getBrowser(),
                v.getLocation(),
                tType,
                v.getStatus(),
                v.getRunId(),
                v.getExecutionId(),
                "",
                "",
                v.getBugs(),
                v.getBugSeverities(),
                distilled.getClusterCategory(),
                distilled.getCleanedStackTrace(),
                distilled.getLocator(),
                Collections.emptyList()
            );

            allTransitions.add(dto);
            switch (tType)
            {
                case TURNED_GREEN -> turnedGreen.add(dto);
                case UNTICKETED_FAIL -> unticketedFailures.add(dto);
                case KNOWN_FAIL -> knownFailures.add(dto);
                case REGRESSION -> regressions.add(dto);
                case STABLE_PASS, INTERMITTENT_FLAKY -> stablePassCount++;
            }
        }

        return new ReportDifferentialDto(
            runIds,
            allTransitions.size(),
            turnedGreen.size(),
            unticketedFailures.size(),
            knownFailures.size(),
            regressions.size(),
            stablePassCount,
            turnedGreen,
            unticketedFailures,
            knownFailures,
            regressions,
            allTransitions
        );
    }

    private static boolean isExecutionFailed(final TestExecutionDto exec)
    {
        final String st = (exec.getStatus() != null) ? exec.getStatus().toLowerCase(Locale.ROOT) : "";
        final boolean statusFailed = st.contains("fail") || st.contains("error");
        final boolean stepsFailed = exec.getFailedStepsCount() > 0;
        final boolean failureTextPresent = exec.getFailure() != null && !exec.getFailure().isBlank() && !"NONE".equalsIgnoreCase(exec.getFailure().trim());
        return statusFailed || stepsFailed || failureTextPresent;
    }

    private static boolean isExecutionPassed(final TestExecutionDto exec, final boolean isFailed)
    {
        if (isFailed)
        {
            return false;
        }
        final String st = (exec.getStatus() != null) ? exec.getStatus().toLowerCase(Locale.ROOT) : "";
        return st.contains("pass") || st.contains("success") || st.contains("fixed") || st.contains("healed");
    }

    private static class VariationTracker
    {
        final String key;
        final String variationId;
        final String testClass;
        final String testMethod;
        final String title;
        final String browser;
        final String location;
        final List<ExecutionSnapshot> snapshots = new ArrayList<>();

        VariationTracker(
            final String key,
            final String variationId,
            final String testClass,
            final String testMethod,
            final String title,
            final String browser,
            final String location)
        {
            this.key = key;
            this.variationId = variationId;
            this.testClass = testClass;
            this.testMethod = testMethod;
            this.title = title;
            this.browser = browser;
            this.location = location;
        }

        void addSnapshot(final ExecutionSnapshot snapshot)
        {
            snapshots.add(snapshot);
        }
    }

    private record ExecutionSnapshot(
        String runId,
        String executionId,
        String status,
        boolean isPassed,
        boolean isFailed,
        boolean hasBugs,
        List<String> bugs,
        List<String> bugSeverities,
        String failureText,
        List<String> tags
    )
    {
    }
}
