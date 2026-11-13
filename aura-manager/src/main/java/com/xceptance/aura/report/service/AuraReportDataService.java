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
package com.xceptance.aura.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import com.xceptance.aura.report.dto.AreaSummaryDto;
import com.xceptance.aura.report.dto.BatchLatestExecutionsDto;
import com.xceptance.aura.report.dto.BatchLatestVariationDto;
import com.xceptance.aura.report.dto.BatchOverviewDataDto;
import com.xceptance.aura.report.dto.BatchSuiteEvolutionDto;
import com.xceptance.aura.report.dto.BatchSuiteEvolutionPointDto;
import com.xceptance.aura.report.dto.BatchSummaryDto;
import com.xceptance.aura.report.dto.SubAreaSummaryDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.dto.TestBaseSubAreaDto;
import com.xceptance.aura.report.dto.ContributingRunCostDto;
import com.xceptance.aura.report.dto.DistributionRatioDto;
import com.xceptance.aura.report.dto.widget.WidgetPointScope;
import java.util.Locale;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseVariationHistoryDto;
import com.xceptance.aura.report.dto.TestClassSummaryDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.dto.TestExecutionDto.BugDetailDto;
import com.xceptance.aura.report.dto.TokenCostPointGroupDto;
import com.xceptance.aura.report.dto.TokenCostTrendDto;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.entity.TestRunTagEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.repository.TestRunTagRepository;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import org.neodymium.util.TagUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business service managing report data, execution runs, test base variations, and bugs.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Service
public class AuraReportDataService
{
    private static final Logger LOG = LoggerFactory.getLogger(AuraReportDataService.class);

    private final TestRunRepository runRepository;
    private final TestBatchRepository batchRepository;
    private final TestBaseVariationRepository variationRepository;
    private final TestBaseBugRepository bugRepository;
    private final TestRunTagRepository tagRepository;
    private final LocalRunJsonStorageService localRunJsonStorageService;
    private final AttachmentStorageService attachmentStorageService;
    private final AuraInteractiveService interactiveService;
    private final ReportDashboardSettingService dashboardSettingService;
    private final ObjectMapper objectMapper;

    private final Map<String, List<TestExecutionDto>> liveRunBuffer = new ConcurrentHashMap<>();
    private final Map<String, RunReportDto> runReportCache = new ConcurrentHashMap<>();

    public AuraReportDataService(
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository,
        final LocalRunJsonStorageService localRunJsonStorageService,
        final AttachmentStorageService attachmentStorageService)
    {
        this(runRepository, batchRepository, variationRepository, bugRepository, null, localRunJsonStorageService, attachmentStorageService, null, null);
    }

    public AuraReportDataService(
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository,
        final LocalRunJsonStorageService localRunJsonStorageService,
        final AttachmentStorageService attachmentStorageService,
        final AuraInteractiveService interactiveService)
    {
        this(runRepository, batchRepository, variationRepository, bugRepository, null, localRunJsonStorageService, attachmentStorageService, interactiveService, null);
    }

    public AuraReportDataService(
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository,
        final LocalRunJsonStorageService localRunJsonStorageService,
        final AttachmentStorageService attachmentStorageService,
        final AuraInteractiveService interactiveService,
        final ReportDashboardSettingService dashboardSettingService)
    {
        this(runRepository, batchRepository, variationRepository, bugRepository, null, localRunJsonStorageService, attachmentStorageService, interactiveService, dashboardSettingService);
    }

    public AuraReportDataService(
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository,
        final TestRunTagRepository tagRepository,
        final LocalRunJsonStorageService localRunJsonStorageService,
        final AttachmentStorageService attachmentStorageService)
    {
        this(runRepository, batchRepository, variationRepository, bugRepository, tagRepository, localRunJsonStorageService, attachmentStorageService, null, null);
    }

    public AuraReportDataService(
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository,
        final TestRunTagRepository tagRepository,
        final LocalRunJsonStorageService localRunJsonStorageService,
        final AttachmentStorageService attachmentStorageService,
        final AuraInteractiveService interactiveService)
    {
        this(runRepository, batchRepository, variationRepository, bugRepository, tagRepository, localRunJsonStorageService, attachmentStorageService, interactiveService, null);
    }

    @Autowired
    public AuraReportDataService(
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository,
        @Autowired(required = false) final TestRunTagRepository tagRepository,
        final LocalRunJsonStorageService localRunJsonStorageService,
        final AttachmentStorageService attachmentStorageService,
        @Autowired(required = false) final AuraInteractiveService interactiveService,
        @Autowired(required = false) final ReportDashboardSettingService dashboardSettingService)
    {
        this.runRepository = runRepository;
        this.batchRepository = batchRepository;
        this.variationRepository = variationRepository;
        this.bugRepository = bugRepository;
        this.tagRepository = tagRepository;
        this.localRunJsonStorageService = localRunJsonStorageService;
        this.attachmentStorageService = attachmentStorageService;
        this.interactiveService = interactiveService;
        this.dashboardSettingService = dashboardSettingService;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void normalizeTagBoundBugsScope()
    {
        if (bugRepository != null)
        {
            try
            {
                final List<TestBaseBugEntity> allBugs = bugRepository.findAll();
                for (final TestBaseBugEntity b : allBugs)
                {
                    if (b.getTagCriteria() != null && !b.getTagCriteria().isBlank())
                    {
                        boolean changed = false;
                        if (!"ALL".equalsIgnoreCase(b.getBatchName()))
                        {
                            b.setBatchName("ALL");
                            changed = true;
                        }
                        if (!"ALL".equalsIgnoreCase(b.getEnvironment()))
                        {
                            b.setEnvironment("ALL");
                            changed = true;
                        }
                        if (changed)
                        {
                            bugRepository.save(b);
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to normalize tag-bound bugs scope on startup: {}", e.getMessage());
            }
        }
    }

    public void evictRunReportCache(final String runId)
    {
        if (runId != null)
        {
            runReportCache.remove(runId);
            liveRunBuffer.remove(runId);
        }
    }

    public void clearCache()
    {
        runReportCache.clear();
        liveRunBuffer.clear();
    }

    @Transactional
    public void clearAllDatabaseEntries()
    {
        if (tagRepository != null)
        {
            tagRepository.deleteAllInBatch();
        }
        bugRepository.deleteAllInBatch();
        variationRepository.deleteAllInBatch();
        batchRepository.deleteAllInBatch();
        runRepository.deleteAllInBatch();
        runReportCache.clear();
        LOG.info("Removed all entries from all database tables (TestRunTag, TestBaseBug, TestBaseVariation, TestBatch, TestRun).");
    }

    public List<BatchSummaryDto> getAllBatches()
    {
        return getAllBatches(batchRepository.findAll(), runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc());
    }

    public List<BatchSummaryDto> getAllBatches(final List<TestBatchEntity> batchEntities, final List<TestRunEntity> allRuns)
    {
        final Map<String, List<TestRunEntity>> runsByBatch = allRuns.stream()
            .filter(r -> r.getBatchName() != null)
            .collect(Collectors.groupingBy(TestRunEntity::getBatchName));

        final List<BatchSummaryDto> batches = new ArrayList<>();

        for (final TestBatchEntity b : batchEntities)
        {
            String passRateText = "N/A";
            String lastExecutedText = "No runs";

            final List<TestRunEntity> batchRuns = runsByBatch.getOrDefault(b.getBatchName(), List.of());

            final java.util.Set<String> activeBatchBugs = new java.util.HashSet<>();
            int totalPassed = 0;
            int totalExecuted = 0;

            String latestRunId = null;
            int latestPassed = 0;
            int latestFixed = 0;
            int latestKnown = 0;
            int latestUnknown = 0;
            int latestIgnored = 0;
            int latestTotal = 0;
            int latestPassHealed = 0;
            int latestFixedHealed = 0;
            int latestKnownHealed = 0;
            int latestUnknownHealed = 0;
            int latestPassAi = 0;
            int latestFixedAi = 0;
            int latestKnownAi = 0;
            int latestUnknownAi = 0;

            if (!batchRuns.isEmpty())
            {
                final TestRunEntity latestRun = batchRuns.get(0);
                latestRunId = latestRun.getId();
                latestPassed = latestRun.getPassedCount() != null ? latestRun.getPassedCount() : 0;
                latestFixed = latestRun.getSucceededFixedCount() != null ? latestRun.getSucceededFixedCount() : 0;
                latestKnown = latestRun.getFailedKnownCount() != null ? latestRun.getFailedKnownCount() : 0;
                latestUnknown = latestRun.getFailedUnknownCount() != null ? latestRun.getFailedUnknownCount() : 0;
                latestIgnored = latestRun.getIgnoredCount() != null ? latestRun.getIgnoredCount() : 0;
                latestTotal = latestRun.getTotalTests() != null ? latestRun.getTotalTests() : 0;
                latestPassHealed = latestRun.getPassedHealedCountSafe();
                latestFixedHealed = latestRun.getFixedHealedCountSafe();
                latestKnownHealed = latestRun.getKnownHealedCountSafe();
                latestUnknownHealed = latestRun.getUnknownHealedCountSafe();
                latestPassAi = latestRun.getPassedAiCountSafe();
                latestFixedAi = latestRun.getFixedAiCountSafe();
                latestKnownAi = latestRun.getKnownAiCountSafe();
                latestUnknownAi = latestRun.getUnknownAiCountSafe();

                lastExecutedText = latestRun.getTimestampLabel() != null ? latestRun.getTimestampLabel() : "Recently";

                for (final TestRunEntity r : batchRuns)
                {
                    totalPassed += r.getPassedCount() != null ? r.getPassedCount() : 0;
                    totalExecuted += r.getTotalTests() != null ? r.getTotalTests() : 0;
                }

                final double overallPassRate = totalExecuted > 0 ? ((double) totalPassed / totalExecuted * 100.0) : (latestRun.getPassRate() != null ? latestRun.getPassRate() : 0.0);
                passRateText = String.format("%.0f%% Pass", overallPassRate);

                try
                {
                    final RunReportDto latestReport = getRunReport(latestRunId);
                    if (latestReport != null)
                    {
                        boolean needsSave = false;
                        if (latestReport.getTotalHealedCount() > 0 && (latestPassHealed == 0 && latestFixedHealed == 0 && latestKnownHealed == 0 && latestUnknownHealed == 0))
                        {
                            latestPassHealed = latestReport.getPassHealedCount();
                            latestFixedHealed = latestReport.getFixedHealedCount();
                            latestKnownHealed = latestReport.getKnownHealedCount();
                            latestUnknownHealed = latestReport.getUnknownHealedCount();
                            latestRun.setPassedHealedCount(latestPassHealed);
                            latestRun.setSucceededFixedHealedCount(latestFixedHealed);
                            latestRun.setFailedKnownHealedCount(latestKnownHealed);
                            latestRun.setFailedUnknownHealedCount(latestUnknownHealed);
                            needsSave = true;
                        }

                        if (latestReport.getTotalAiCount() > 0 && (latestPassAi == 0 && latestFixedAi == 0 && latestKnownAi == 0 && latestUnknownAi == 0))
                        {
                            latestPassAi = latestReport.getPassAiCount();
                            latestFixedAi = latestReport.getFixedAiCount();
                            latestKnownAi = latestReport.getKnownAiCount();
                            latestUnknownAi = latestReport.getUnknownAiCount();
                            latestRun.setPassedAiCount(latestPassAi);
                            latestRun.setSucceededFixedAiCount(latestFixedAi);
                            latestRun.setFailedKnownAiCount(latestKnownAi);
                            latestRun.setFailedUnknownAiCount(latestUnknownAi);
                            needsSave = true;
                        }

                        if (needsSave)
                        {
                            runRepository.save(latestRun);
                        }

                        if (latestReport.getExecutions() != null)
                        {
                            for (final TestExecutionDto exec : latestReport.getExecutions())
                            {
                                if (exec.getBugs() != null)
                                {
                                    for (final String bug : exec.getBugs())
                                    {
                                        if (bug != null && !bug.trim().isEmpty())
                                        {
                                            activeBatchBugs.add(bug.trim());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOG.error("Failed to calculate active batch bugs for runId {}: {}", latestRunId, e.getMessage());
                }
            }

            final int batchBugCount = !activeBatchBugs.isEmpty() ? activeBatchBugs.size() : latestKnown;
            final String activeBugsText = batchBugCount + " Active Bug" + (batchBugCount != 1 ? "s" : "");

            batches.add(new BatchSummaryDto(
                b.getBatchName().toLowerCase().replace(" ", "-"),
                b.getBatchName(),
                b.getEnvironment(),
                b.getDescription(),
                lastExecutedText,
                passRateText,
                activeBugsText,
                List.of(b.getLocalesCsv() != null ? b.getLocalesCsv().split(",\\s*") : new String[0]),
                List.of(b.getBrowsersCsv() != null ? b.getBrowsersCsv().split(",\\s*") : new String[0]),
                latestRunId,
                latestPassed,
                latestFixed,
                latestKnown,
                latestUnknown,
                latestIgnored,
                latestTotal,
                latestPassHealed,
                latestFixedHealed,
                latestKnownHealed,
                latestUnknownHealed,
                latestPassAi,
                latestFixedAi,
                latestKnownAi,
                latestUnknownAi
            ));
        }
        return batches;
    }

    public List<TestRunEntity> getAllRuns()
    {
        if (runRepository == null)
        {
            return Collections.emptyList();
        }
        final List<TestRunEntity> runs = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
        populateRunTags(runs);
        return runs;
    }

    public BatchOverviewDataDto getBatchOverviewData()
    {
        final List<TestBatchEntity> allBatchEntities = batchRepository.findAll();
        final List<TestRunEntity> allRunEntities = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();

        for (final TestRunEntity r : allRunEntities)
        {
            try
            {
                final RunReportDto report = getRunReport(r.getId());
                if (report != null)
                {
                    boolean changed = false;
                    if (!Objects.equals(r.getPassedHealedCount(), report.getPassHealedCount()))
                    {
                        r.setPassedHealedCount(report.getPassHealedCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getSucceededFixedHealedCount(), report.getFixedHealedCount()))
                    {
                        r.setSucceededFixedHealedCount(report.getFixedHealedCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getFailedKnownHealedCount(), report.getKnownHealedCount()))
                    {
                        r.setFailedKnownHealedCount(report.getKnownHealedCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getFailedUnknownHealedCount(), report.getUnknownHealedCount()))
                    {
                        r.setFailedUnknownHealedCount(report.getUnknownHealedCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getPassedAiCount(), report.getPassAiCount()))
                    {
                        r.setPassedAiCount(report.getPassAiCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getSucceededFixedAiCount(), report.getFixedAiCount()))
                    {
                        r.setSucceededFixedAiCount(report.getFixedAiCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getFailedKnownAiCount(), report.getKnownAiCount()))
                    {
                        r.setFailedKnownAiCount(report.getKnownAiCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getFailedUnknownAiCount(), report.getUnknownAiCount()))
                    {
                        r.setFailedUnknownAiCount(report.getUnknownAiCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getPlaybookCount(), report.getPlaybookCount()))
                    {
                        r.setPlaybookCount(report.getPlaybookCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getPassedCount(), report.getPassCount()))
                    {
                        r.setPassedCount(report.getPassCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getSucceededFixedCount(), report.getFixedCount()))
                    {
                        r.setSucceededFixedCount(report.getFixedCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getFailedKnownCount(), report.getKnownCount()))
                    {
                        r.setFailedKnownCount(report.getKnownCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getFailedUnknownCount(), report.getUnknownCount()))
                    {
                        r.setFailedUnknownCount(report.getUnknownCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getIgnoredCount(), report.getIgnoredCount()))
                    {
                        r.setIgnoredCount(report.getIgnoredCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getRunningCount(), report.getRunningCount()))
                    {
                        r.setRunningCount(report.getRunningCount());
                        changed = true;
                    }
                    if (!Objects.equals(r.getTotalTests(), report.getTotalCount()))
                    {
                        r.setTotalTests(report.getTotalCount());
                        changed = true;
                    }
                    if (changed)
                    {
                        r.recalculatePassRate();
                        runRepository.save(r);
                    }
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        final List<BatchSummaryDto> batches = getAllBatches(allBatchEntities, allRunEntities);

        // 1. Dynamic Environments
        final List<String> envsFromBatches = allBatchEntities.stream()
            .map(TestBatchEntity::getEnvironment)
            .collect(Collectors.toList());
        final List<String> envsFromRuns = allRunEntities.stream()
            .map(TestRunEntity::getEnvironment)
            .collect(Collectors.toList());
        final List<String> filterEnvs = Stream.concat(envsFromBatches.stream(), envsFromRuns.stream())
            .filter(e -> e != null && !e.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        // 2. Dynamic Locales
        final List<String> filterLocales = new ArrayList<>();
        allBatchEntities.forEach(b -> {
            if (b.getLocalesCsv() != null)
            {
                for (final String s : b.getLocalesCsv().split(","))
                {
                    final String trimmed = s.trim();
                    if (!trimmed.isEmpty() && !filterLocales.contains(trimmed))
                    {
                        filterLocales.add(trimmed);
                    }
                }
            }
        });
        allRunEntities.forEach(r -> {
            if (r.getLocalesCsv() != null)
            {
                for (final String s : r.getLocalesCsv().split(","))
                {
                    final String trimmed = s.trim();
                    if (!trimmed.isEmpty() && !filterLocales.contains(trimmed))
                    {
                        filterLocales.add(trimmed);
                    }
                }
            }
        });
        filterLocales.sort(String::compareTo);

        // 3. Dynamic Browsers
        final List<String> filterBrowsers = new ArrayList<>();
        allBatchEntities.forEach(b -> {
            if (b.getBrowsersCsv() != null)
            {
                for (final String s : b.getBrowsersCsv().split(","))
                {
                    final String trimmed = s.trim();
                    if (!trimmed.isEmpty() && !filterBrowsers.contains(trimmed))
                    {
                        filterBrowsers.add(trimmed);
                    }
                }
            }
        });
        allRunEntities.forEach(r -> {
            if (r.getBrowsersCsv() != null)
            {
                for (final String s : r.getBrowsersCsv().split(","))
                {
                    final String trimmed = s.trim();
                    if (!trimmed.isEmpty() && !filterBrowsers.contains(trimmed))
                    {
                        filterBrowsers.add(trimmed);
                    }
                }
            }
        });
        filterBrowsers.sort(String::compareTo);

        return new BatchOverviewDataDto(
            batches,
            allRunEntities,
            filterEnvs,
            filterLocales,
            filterBrowsers
        );
    }

    public BatchOverviewDataDto getGroupOverviewData(final String groupBy)
    {
        if (groupBy == null || groupBy.isBlank())
        {
            return getGroupOverviewData(List.of("batch"));
        }
        final String[] tokens = groupBy.split(",");
        final List<String> dimensions = new ArrayList<>();
        for (final String t : tokens)
        {
            final String trimmed = t.trim();
            if (!trimmed.isEmpty())
            {
                dimensions.add(trimmed);
            }
        }
        return getGroupOverviewData(dimensions);
    }

    public BatchOverviewDataDto getGroupOverviewData(final List<String> groupDimensions)
    {
        return getGroupOverviewData(groupDimensions, null, null, null, Collections.emptyMap());
    }

    public BatchOverviewDataDto getGroupOverviewData(
        final List<String> groupDimensions,
        final List<TestRunEntity> baseRuns,
        final String parentGroupType,
        final String parentGroupValue,
        final Map<String, String> parentCriteria)
    {
        return getGroupOverviewData(groupDimensions, baseRuns, parentGroupType, parentGroupValue, parentCriteria, WidgetPointScope.LATEST_TEST_STATUS);
    }

    public BatchOverviewDataDto getGroupOverviewData(
        final List<String> groupDimensions,
        final List<TestRunEntity> baseRuns,
        final String parentGroupType,
        final String parentGroupValue,
        final Map<String, String> parentCriteria,
        final WidgetPointScope pointScope)
    {
        final Set<String> validDimensions = new LinkedHashSet<>();
        if (groupDimensions != null)
        {
            for (final String dim : groupDimensions)
            {
                if (dim == null || dim.isBlank())
                {
                    continue;
                }
                final String clean = dim.trim().toLowerCase(Locale.ROOT);
                if ("none".equals(clean) || "all".equals(clean))
                {
                    continue;
                }
                if ("tag".equals(clean) || "batch".equals(clean))
                {
                    validDimensions.add("batch");
                }
                else if ("environment".equals(clean) || "env".equals(clean))
                {
                    validDimensions.add("environment");
                }
                else if ("locale".equals(clean) || "locales".equals(clean))
                {
                    validDimensions.add("locale");
                }
                else if ("browser".equals(clean) || "browsers".equals(clean))
                {
                    validDimensions.add("browser");
                }
                else if ("area".equals(clean) || "areas".equals(clean))
                {
                    validDimensions.add("area");
                }
                else
                {
                    validDimensions.add(dim.trim());
                }
            }
        }
        if (validDimensions.isEmpty())
        {
            validDimensions.add("batch");
        }
        final List<String> activeDimensions = new ArrayList<>(validDimensions);

        final List<TestRunEntity> allRuns;
        if (baseRuns == null)
        {
            allRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
            populateRunTags(allRuns);

            for (final TestRunEntity r : allRuns)
            {
                try
                {
                    final RunReportDto report = getRunReport(r.getId());
                    if (report != null)
                    {
                        boolean changed = false;
                        if (!Objects.equals(r.getPassedHealedCount(), report.getPassHealedCount()))
                        {
                            r.setPassedHealedCount(report.getPassHealedCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getSucceededFixedHealedCount(), report.getFixedHealedCount()))
                        {
                            r.setSucceededFixedHealedCount(report.getFixedHealedCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getFailedKnownHealedCount(), report.getKnownHealedCount()))
                        {
                            r.setFailedKnownHealedCount(report.getKnownHealedCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getFailedUnknownHealedCount(), report.getUnknownHealedCount()))
                        {
                            r.setFailedUnknownHealedCount(report.getUnknownHealedCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getPassedAiCount(), report.getPassAiCount()))
                        {
                            r.setPassedAiCount(report.getPassAiCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getSucceededFixedAiCount(), report.getFixedAiCount()))
                        {
                            r.setSucceededFixedAiCount(report.getFixedAiCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getFailedKnownAiCount(), report.getKnownAiCount()))
                        {
                            r.setFailedKnownAiCount(report.getKnownAiCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getFailedUnknownAiCount(), report.getUnknownAiCount()))
                        {
                            r.setFailedUnknownAiCount(report.getUnknownAiCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getPlaybookCount(), report.getPlaybookCount()))
                        {
                            r.setPlaybookCount(report.getPlaybookCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getPassedCount(), report.getPassCount()))
                        {
                            r.setPassedCount(report.getPassCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getSucceededFixedCount(), report.getFixedCount()))
                        {
                            r.setSucceededFixedCount(report.getFixedCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getFailedKnownCount(), report.getKnownCount()))
                        {
                            r.setFailedKnownCount(report.getKnownCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getFailedUnknownCount(), report.getUnknownCount()))
                        {
                            r.setFailedUnknownCount(report.getUnknownCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getIgnoredCount(), report.getIgnoredCount()))
                        {
                            r.setIgnoredCount(report.getIgnoredCount());
                            changed = true;
                        }
                        if (!Objects.equals(r.getTotalTests(), report.getTotalCount()))
                        {
                            r.setTotalTests(report.getTotalCount());
                            changed = true;
                        }
                        if (changed)
                        {
                            r.recalculatePassRate();
                            runRepository.save(r);
                        }
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
        }
        else
        {
            allRuns = new ArrayList<>(baseRuns);
            populateRunTags(allRuns);
        }
        final Map<String, List<TestRunEntity>> groupedRuns = new LinkedHashMap<>();

        if (baseRuns == null && activeDimensions.size() == 1 && "batch".equals(activeDimensions.get(0)))
        {
            final List<TestBatchEntity> allBatchEntities = batchRepository.findAll();
            for (final TestBatchEntity b : allBatchEntities)
            {
                if (b.getBatchName() != null && !b.getBatchName().isBlank())
                {
                    groupedRuns.putIfAbsent(b.getBatchName().trim(), new ArrayList<>());
                }
            }
        }
        else if (baseRuns == null && activeDimensions.size() == 2 && activeDimensions.contains("batch") && activeDimensions.contains("environment"))
        {
            final List<TestBatchEntity> allBatchEntities = batchRepository.findAll();
            for (final TestBatchEntity b : allBatchEntities)
            {
                if (b.getBatchName() != null && !b.getBatchName().isBlank())
                {
                    final String bName = b.getBatchName().trim();
                    final String bEnv = (b.getEnvironment() != null && !b.getEnvironment().isBlank()) ? b.getEnvironment().trim() : "Default";
                    final String compoundKey = "batch".equals(activeDimensions.get(0)) ? (bName + " • " + bEnv) : (bEnv + " • " + bName);
                    groupedRuns.putIfAbsent(compoundKey, new ArrayList<>());
                }
            }
        }

        for (final TestRunEntity r : allRuns)
        {
            final List<List<String>> dimValuesList = new ArrayList<>();
            for (final String dim : activeDimensions)
            {
                final List<String> vals = new ArrayList<>();
                if ("environment".equals(dim))
                {
                    final String env = (r.getEnvironment() != null && !r.getEnvironment().isBlank()) ? r.getEnvironment().trim() : "Default";
                    vals.add(env);
                }
                else if ("locale".equals(dim))
                {
                    if (r.getLocalesCsv() != null && !r.getLocalesCsv().isBlank())
                    {
                        final String[] locs = r.getLocalesCsv().split(",");
                        for (final String l : locs)
                        {
                            final String trimmed = l.trim();
                            if (!trimmed.isEmpty() && !vals.contains(trimmed))
                            {
                                vals.add(trimmed);
                            }
                        }
                    }
                    if (vals.isEmpty())
                    {
                        vals.add("Default");
                    }
                }
                else if ("browser".equals(dim))
                {
                    if (r.getBrowsersCsv() != null && !r.getBrowsersCsv().isBlank())
                    {
                        final String[] brs = r.getBrowsersCsv().split(",");
                        for (final String b : brs)
                        {
                            final String trimmed = b.trim();
                            if (!trimmed.isEmpty() && !vals.contains(trimmed))
                            {
                                vals.add(trimmed);
                            }
                        }
                    }
                    if (vals.isEmpty())
                    {
                        vals.add("Chrome");
                    }
                }
                else if ("area".equals(dim))
                {
                    final List<String> rAreas = getRunAreas(r);
                    for (final String a : rAreas)
                    {
                        final String norm = normalizeArea(a);
                        if (!vals.contains(norm))
                        {
                            vals.add(norm);
                        }
                    }
                    if (vals.isEmpty())
                    {
                        vals.add("Default");
                    }
                }
                else if ("batch".equalsIgnoreCase(dim))
                {
                    final String batch = (r.getBatchName() != null && !r.getBatchName().isBlank()) ? r.getBatchName().trim() : "Default";
                    vals.add(batch);
                }
                else
                {
                    final boolean isBool = isBooleanTag(dim, allRuns);
                    final List<String> tagVals = getRunTagValues(r, dim);
                    if (isBool)
                    {
                        final boolean hasTrue = tagVals.stream().anyMatch(tv -> "true".equalsIgnoreCase(tv.trim()) || dim.equalsIgnoreCase(tv.trim()));
                        if (hasTrue)
                        {
                            vals.add(dim);
                        }
                        else
                        {
                            vals.add("Not " + dim);
                        }
                    }
                    else
                    {
                        for (final String tv : tagVals)
                        {
                            final String trimmed = tv.trim();
                            if (!trimmed.isEmpty() && !vals.contains(trimmed))
                            {
                                vals.add(trimmed);
                            }
                        }
                        if (vals.isEmpty())
                        {
                            vals.add("Default");
                        }
                    }
                }
                dimValuesList.add(vals);
            }

            final List<List<String>> combinations = computeCartesianCombinations(dimValuesList);
            for (final List<String> combo : combinations)
            {
                final String groupName = String.join(" • ", combo);
                groupedRuns.computeIfAbsent(groupName, k -> new ArrayList<>()).add(r);
            }
        }

        final List<BatchSummaryDto> groupCards = new ArrayList<>();
        for (final Map.Entry<String, List<TestRunEntity>> entry : groupedRuns.entrySet())
        {
            final String groupName = entry.getKey();
            final List<TestRunEntity> gRuns = entry.getValue();

            final Set<String> groupRunBrowsers = new LinkedHashSet<>();
            if (activeDimensions.contains("browser"))
            {
                final String brVal = extractDimensionValue(activeDimensions, groupName, "browser");
                if (brVal != null && !brVal.isEmpty())
                {
                    groupRunBrowsers.add(brVal);
                }
            }
            if (parentCriteria != null && parentCriteria.containsKey("browser"))
            {
                groupRunBrowsers.add(parentCriteria.get("browser"));
            }

            final Set<String> groupRunLocales = new LinkedHashSet<>();
            if (activeDimensions.contains("locale"))
            {
                final String locVal = extractDimensionValue(activeDimensions, groupName, "locale");
                if (locVal != null && !locVal.isEmpty())
                {
                    groupRunLocales.add(locVal);
                }
            }
            if (parentCriteria != null && parentCriteria.containsKey("locale"))
            {
                groupRunLocales.add(parentCriteria.get("locale"));
            }

            final Set<String> groupRunEnvs = new LinkedHashSet<>();
            if (activeDimensions.contains("environment"))
            {
                final String envVal = extractDimensionValue(activeDimensions, groupName, "environment");
                if (envVal != null && !envVal.isEmpty())
                {
                    groupRunEnvs.add(envVal);
                }
            }
            if (parentCriteria != null && parentCriteria.containsKey("environment"))
            {
                groupRunEnvs.add(parentCriteria.get("environment"));
            }

            final Set<String> groupRunAreas = new LinkedHashSet<>();
            if (activeDimensions.contains("area"))
            {
                final String areaVal = extractDimensionValue(activeDimensions, groupName, "area");
                if (areaVal != null && !areaVal.isEmpty())
                {
                    groupRunAreas.add(areaVal);
                }
            }
            if (parentCriteria != null && parentCriteria.containsKey("area"))
            {
                groupRunAreas.add(parentCriteria.get("area"));
            }

            final Set<String> groupRunTags = new LinkedHashSet<>();
            if (activeDimensions.contains("batch"))
            {
                final String batchVal = extractDimensionValue(activeDimensions, groupName, "batch");
                if (batchVal != null && !batchVal.isEmpty())
                {
                    groupRunTags.add(batchVal);
                }
            }
            if (parentCriteria != null && parentCriteria.containsKey("batch"))
            {
                groupRunTags.add(parentCriteria.get("batch"));
            }

            final Map<String, Set<String>> groupRunTagsByType = new LinkedHashMap<>();
            for (final String dim : activeDimensions)
            {
                if (!"browser".equalsIgnoreCase(dim) && !"locale".equalsIgnoreCase(dim)
                    && !"environment".equalsIgnoreCase(dim) && !"area".equalsIgnoreCase(dim)
                    && !"batch".equalsIgnoreCase(dim))
                {
                    final String val = extractDimensionValue(activeDimensions, groupName, dim);
                    if (val != null && !val.isEmpty())
                    {
                        groupRunTagsByType.computeIfAbsent(dim, k -> new LinkedHashSet<>()).add(val);
                    }
                }
            }
            if (parentCriteria != null)
            {
                for (final Map.Entry<String, String> pEntry : parentCriteria.entrySet())
                {
                    final String pDim = pEntry.getKey();
                    final String pVal = pEntry.getValue();
                    if (!"browser".equalsIgnoreCase(pDim) && !"locale".equalsIgnoreCase(pDim)
                        && !"environment".equalsIgnoreCase(pDim) && !"area".equalsIgnoreCase(pDim)
                        && !"batch".equalsIgnoreCase(pDim))
                    {
                        if (pVal != null && !pVal.isBlank())
                        {
                            groupRunTagsByType.computeIfAbsent(pDim, k -> new LinkedHashSet<>()).add(pVal);
                        }
                    }
                }
            }

            final List<TestRunEntity> effectiveExecutionRuns = (pointScope == WidgetPointScope.LAST_RUN && !gRuns.isEmpty())
                ? List.of(gRuns.get(0))
                : gRuns;

            final BatchLatestExecutionsDto latestExec = getBatchLatestTestExecutionsSummary(
                groupName,
                effectiveExecutionRuns,
                Collections.emptySet(),
                null,
                null,
                groupRunBrowsers,
                groupRunLocales,
                groupRunEnvs,
                groupRunAreas,
                groupRunTags,
                Collections.emptyMap(),
                groupRunTagsByType
            );
            final BatchSummaryDto summary = (latestExec != null) ? latestExec.getBatchSummary() : null;

            final List<String> gLocales;
            if (activeDimensions.contains("locale"))
            {
                final String locVal = extractDimensionValue(activeDimensions, groupName, "locale");
                gLocales = (locVal != null && !locVal.isBlank()) ? List.of(locVal) : List.of("Default");
            }
            else
            {
                gLocales = gRuns.stream()
                    .flatMap(r -> r.getLocalesCsv() != null ? Arrays.stream(r.getLocalesCsv().split(",")) : Stream.empty())
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            }

            final List<String> gBrowsers;
            if (activeDimensions.contains("browser"))
            {
                final String brVal = extractDimensionValue(activeDimensions, groupName, "browser");
                gBrowsers = (brVal != null && !brVal.isBlank()) ? List.of(brVal) : List.of("Chrome");
            }
            else
            {
                gBrowsers = gRuns.stream()
                    .flatMap(r -> r.getBrowsersCsv() != null ? Arrays.stream(r.getBrowsersCsv().split(",")) : Stream.empty())
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            }

            final String gEnv = activeDimensions.contains("environment")
                ? Optional.ofNullable(extractDimensionValue(activeDimensions, groupName, "environment")).orElse("Default")
                : gRuns.stream().map(TestRunEntity::getEnvironment).filter(e -> e != null && !e.isBlank()).findFirst().orElse("Default");

            final String gBatch = activeDimensions.contains("batch")
                ? extractDimensionValue(activeDimensions, groupName, "batch")
                : null;
            final String gDescription = (gBatch != null)
                ? batchRepository.findById(gBatch).map(TestBatchEntity::getDescription).orElse("")
                : "";

            final String latestRunId = (summary != null && summary.getLatestRunId() != null)
                ? summary.getLatestRunId()
                : (!gRuns.isEmpty() ? gRuns.get(0).getId() : null);

            final String lastExecuted = (summary != null && summary.getLastExecuted() != null && !summary.getLastExecuted().isBlank())
                ? summary.getLastExecuted()
                : (!gRuns.isEmpty() && gRuns.get(0).getTimestampLabel() != null ? gRuns.get(0).getTimestampLabel() : "No runs");

            final int pass = (summary != null) ? summary.getLatestPassedCount() : 0;
            final int fixed = (summary != null) ? summary.getLatestFixedCount() : 0;
            final int known = (summary != null) ? summary.getLatestKnownCount() : 0;
            final int unknown = (summary != null) ? summary.getLatestUnknownCount() : 0;
            final int ignored = (summary != null) ? summary.getLatestIgnoredCount() : 0;
            final int total = (summary != null) ? summary.getLatestTotalTests() : 0;
            final int passHealed = (summary != null) ? summary.getLatestPassHealedCount() : 0;
            final int fixedHealed = (summary != null) ? summary.getLatestFixedHealedCount() : 0;
            final int knownHealed = (summary != null) ? summary.getLatestKnownHealedCount() : 0;
            final int unknownHealed = (summary != null) ? summary.getLatestUnknownHealedCount() : 0;
            final int passAi = (summary != null) ? summary.getLatestPassAiCount() : 0;
            final int fixedAi = (summary != null) ? summary.getLatestFixedAiCount() : 0;
            final int knownAi = (summary != null) ? summary.getLatestKnownAiCount() : 0;
            final int unknownAi = (summary != null) ? summary.getLatestUnknownAiCount() : 0;

            final String passRateText = total > 0
                ? String.format(Locale.ROOT, "%.0f%% Pass", ((double) (pass + fixed) / total * 100.0))
                : "N/A";

            final String sanitizedDims = String.join("-", activeDimensions).replaceAll("[^a-zA-Z0-9_-]", "_");
            final String sanitizedGroup = groupName.replaceAll("[^a-zA-Z0-9_-]", "_");
            final String cardId = "group-" + sanitizedDims + "-" + sanitizedGroup;

            final String historyUrl;
            if (parentGroupType != null && !parentGroupType.isBlank() && parentGroupValue != null && !parentGroupValue.isBlank())
            {
                final String compoundType = parentGroupType + "," + String.join(",", activeDimensions);
                final String compoundValue = parentGroupValue + " • " + groupName;
                historyUrl = "/batch-history?groupType=" + URLEncoder.encode(compoundType, StandardCharsets.UTF_8)
                    + "&groupValue=" + URLEncoder.encode(compoundValue, StandardCharsets.UTF_8);
            }
            else
            {
                historyUrl = "/batch-history?groupType=" + URLEncoder.encode(String.join(",", activeDimensions), StandardCharsets.UTF_8)
                    + "&groupValue=" + URLEncoder.encode(groupName, StandardCharsets.UTF_8);
            }

            groupCards.add(new BatchSummaryDto(
                cardId,
                groupName,
                gEnv,
                gDescription,
                lastExecuted,
                passRateText,
                "",
                gLocales,
                gBrowsers,
                latestRunId,
                pass,
                fixed,
                known,
                unknown,
                ignored,
                total,
                passHealed,
                fixedHealed,
                knownHealed,
                unknownHealed,
                passAi,
                fixedAi,
                knownAi,
                unknownAi,
                historyUrl
            ));
        }

        final List<String> filterEnvs = allRuns.stream()
            .map(TestRunEntity::getEnvironment)
            .filter(e -> e != null && !e.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        final List<String> filterLocales = allRuns.stream()
            .flatMap(r -> r.getLocalesCsv() != null ? Arrays.stream(r.getLocalesCsv().split(",")) : Stream.empty())
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        final List<String> filterBrowsers = allRuns.stream()
            .flatMap(r -> r.getBrowsersCsv() != null ? Arrays.stream(r.getBrowsersCsv().split(",")) : Stream.empty())
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        return new BatchOverviewDataDto(groupCards, allRuns, filterEnvs, filterLocales, filterBrowsers);
    }

    private static List<List<String>> computeCartesianCombinations(final List<List<String>> lists)
    {
        List<List<String>> result = new ArrayList<>();
        if (lists == null || lists.isEmpty())
        {
            return result;
        }
        result.add(new ArrayList<>());
        for (final List<String> list : lists)
        {
            final List<List<String>> current = new ArrayList<>();
            for (final List<String> combination : result)
            {
                for (final String item : list)
                {
                    final List<String> newCombination = new ArrayList<>(combination);
                    newCombination.add(item);
                    current.add(newCombination);
                }
            }
            result = current;
        }
        return result;
    }

    private static String extractDimensionValue(final List<String> dimensions, final String groupName, final String targetDimension)
    {
        if (dimensions == null || groupName == null || targetDimension == null)
        {
            return null;
        }
        final int idx = dimensions.indexOf(targetDimension);
        if (idx == -1)
        {
            return null;
        }
        final String[] parts;
        if (groupName.contains(" • "))
        {
            parts = groupName.split(" • ");
        }
        else if (groupName.contains(" / "))
        {
            parts = groupName.split(" / ");
        }
        else if (groupName.contains(" | "))
        {
            parts = groupName.split(" \\| ");
        }
        else
        {
            parts = new String[] { groupName };
        }
        if (idx < parts.length)
        {
            return parts[idx].trim();
        }
        return null;
    }

    public TestBaseDataDto getTestBaseData()
    {
        final List<TestBaseVariationEntity> allVariations = variationRepository.findAllByOrderByLastExecutedAtDesc();
        final List<TestBaseVariationEntity> rawVariations = allVariations.stream()
            .filter(v -> v.getTestMethodName() != null && !v.getTestMethodName().trim().isEmpty())
            .collect(Collectors.toList());

        final Map<String, TestBaseVariationEntity> mergedMap = new LinkedHashMap<>();
        final List<TestBaseVariationEntity> duplicatesToDelete = new ArrayList<>();

        for (final TestBaseVariationEntity var : rawVariations)
        {
            final String normLoc = normalizeLocation(var.getLocation());
            final String normBr = normalizeBrowser(var.getBrowser());
            var.setLocation(normLoc);
            var.setBrowser(normBr);

            final String logicalKey = (var.getTestClassName() != null ? var.getTestClassName().trim() : "") + "|" +
                                       (var.getTestMethodName() != null ? var.getTestMethodName().trim() : "") + "|" +
                                       (var.getDataSetLabel() != null ? var.getDataSetLabel().trim() : "") + "|" +
                                       normLoc + "|" + normBr;

            final String canonicalId = generateVariationId(
                var.getTestClassName(),
                var.getTestMethodName(),
                var.getDataSetLabel(),
                normLoc,
                normBr
            );

            if (!mergedMap.containsKey(logicalKey))
            {
                mergedMap.put(logicalKey, var);
            }
            else
            {
                TestBaseVariationEntity primary = mergedMap.get(logicalKey);
                TestBaseVariationEntity secondary = var;
                if (!primary.getId().equals(canonicalId) && secondary.getId().equals(canonicalId))
                {
                    primary = var;
                    secondary = mergedMap.get(logicalKey);
                    mergedMap.put(logicalKey, primary);
                }

                final int totalExecs = (primary.getTotalExecutionsCount() != null ? primary.getTotalExecutionsCount() : 0)
                                     + (secondary.getTotalExecutionsCount() != null ? secondary.getTotalExecutionsCount() : 0);
                primary.setTotalExecutionsCount(totalExecs);

                final long t1 = primary.getLastExecutedAt() != null ? primary.getLastExecutedAt() : 0L;
                final long t2 = secondary.getLastExecutedAt() != null ? secondary.getLastExecutedAt() : 0L;
                if (t2 > t1)
                {
                    primary.setLastExecutedAt(t2);
                    if (secondary.getLastStatus() != null)
                    {
                        primary.setLastStatus(secondary.getLastStatus());
                    }
                    if (secondary.getHealed() != null)
                    {
                        primary.setHealed(secondary.getHealed());
                    }
                    if (secondary.getExecutionMode() != null)
                    {
                        primary.setExecutionMode(secondary.getExecutionMode());
                    }
                }

                duplicatesToDelete.add(secondary);
            }
        }

        if (!duplicatesToDelete.isEmpty())
        {
            try
            {
                variationRepository.deleteAll(duplicatesToDelete);
            }
            catch (final Exception e)
            {
                LOG.warn("Could not delete duplicate variations from DB: {}", e.getMessage());
            }
        }

        final List<TestBaseVariationEntity> variations = new ArrayList<>(mergedMap.values());
        final List<TestBaseBugEntity> allBugs = bugRepository.findAll();

        final Map<String, List<String>> bugsMap = allBugs.stream()
            .filter(b -> b.getRemovedRunId() == null)
            .collect(Collectors.groupingBy(
                TestBaseBugEntity::getVariationId,
                Collectors.mapping(TestBaseBugEntity::getBugTicket, Collectors.toList())
            ));

        for (final TestBaseVariationEntity var : variations)
        {
            final boolean hasBugs = bugsMap.containsKey(var.getId()) && !bugsMap.get(var.getId()).isEmpty();
            final String curStatus = var.getLastStatus() != null ? var.getLastStatus() : "passed-clean";

            String effectiveLastStatus = curStatus;
            if ("failed".equalsIgnoreCase(curStatus) || "failed-unknown".equalsIgnoreCase(curStatus) || "failed-known".equalsIgnoreCase(curStatus) || "error".equalsIgnoreCase(curStatus))
            {
                effectiveLastStatus = hasBugs ? "failed-known" : "failed-unknown";
            }
            else if ("passed".equalsIgnoreCase(curStatus) || "succeeded-fixed".equalsIgnoreCase(curStatus) || "passed-clean".equalsIgnoreCase(curStatus) || "succeeded".equalsIgnoreCase(curStatus) || "healed".equalsIgnoreCase(curStatus))
            {
                effectiveLastStatus = hasBugs ? "succeeded-fixed" : "passed-clean";
            }

            if (!effectiveLastStatus.equalsIgnoreCase(var.getLastStatus()))
            {
                var.setLastStatus(effectiveLastStatus);
                variationRepository.save(var);
            }
        }

        annotateFlakyVariations(variations);

        // 1. Dynamic Batches
        final List<String> batchNamesFromBatches = batchRepository.findAll().stream()
            .map(TestBatchEntity::getBatchName)
            .collect(Collectors.toList());
        final List<String> batchNamesFromRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
            .map(TestRunEntity::getBatchName)
            .collect(Collectors.toList());
        final List<String> filterBatches = Stream.concat(batchNamesFromBatches.stream(), batchNamesFromRuns.stream())
            .filter(b -> b != null && !b.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        // 2. Dynamic Locales
        final List<String> filterLocales = variations.stream()
            .map(TestBaseVariationEntity::getLocation)
            .filter(loc -> loc != null && !loc.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        // 3. Dynamic Browsers
        final List<String> filterBrowsers = variations.stream()
            .map(TestBaseVariationEntity::getBrowser)
            .filter(br -> br != null && !br.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        // 4. Dynamic Grouping by Test Class First, then by Primary Area
        final Map<String, List<TestBaseVariationEntity>> byClass = variations.stream()
            .collect(Collectors.groupingBy(
                v -> (v.getTestClassName() != null && !v.getTestClassName().isEmpty()) ? v.getTestClassName() : "GeneralTest",
                LinkedHashMap::new,
                Collectors.toList()
            ));

        final Map<String, List<TestBaseClassDto>> byAreaMap = new LinkedHashMap<>();

        for (final Map.Entry<String, List<TestBaseVariationEntity>> classEntry : byClass.entrySet())
        {
            final String className = classEntry.getKey();
            final List<TestBaseVariationEntity> classVars = classEntry.getValue();
            final String classContainerId = "classContainer" + className.replaceAll("[^a-zA-Z0-9]", "");

            final TestBaseClassDto classDto = new TestBaseClassDto(
                className,
                classContainerId,
                classVars.size(),
                classVars
            );

            String areaName = "Default";
            for (final TestBaseVariationEntity v : classVars)
            {
                final String area = v.getAreaTag();
                if (isRealArea(area))
                {
                    areaName = normalizeArea(area);
                    break;
                }
            }

            byAreaMap.computeIfAbsent(areaName, k -> new ArrayList<>()).add(classDto);
        }

        final List<TestBaseAreaDto> areas = new ArrayList<>();
        int totalTestClassesCount = 0;
        final int totalVariationsCount = variations.size();

        for (final Map.Entry<String, List<TestBaseClassDto>> areaEntry : byAreaMap.entrySet())
        {
            final String areaName = areaEntry.getKey();
            final List<TestBaseClassDto> classDtos = areaEntry.getValue();

            totalTestClassesCount += classDtos.size();

            final String cleanAreaName = areaName.replace("@", "").replaceAll("[^a-zA-Z0-9]", "");
            final String areaPaneId = "tbArea" + cleanAreaName;
            final String tabBtnId = "tbTabBtn" + cleanAreaName;
            final String icon = resolveAreaIcon(areaName);

            areas.add(new TestBaseAreaDto(
                areaName,
                areaPaneId,
                tabBtnId,
                icon,
                classDtos.size(),
                classDtos
            ));
        }

        return new TestBaseDataDto(
            filterBatches,
            filterLocales,
            filterBrowsers,
            areas,
            bugsMap,
            totalTestClassesCount,
            totalVariationsCount
        );
    }

    private String resolveAreaIcon(final String areaName)
    {
        final String lower = areaName.toLowerCase();
        if (lower.contains("checkout")) return "shopping_cart";
        if (lower.contains("auth") || lower.contains("login") || lower.contains("security")) return "shield";
        if (lower.contains("cart") || lower.contains("basket")) return "shopping_bag";
        if (lower.contains("search") || lower.contains("catalog")) return "search";
        if (lower.contains("payment") || lower.contains("billing")) return "payments";
        return "folder";
    }

    public List<TestBaseClassDto> getBatchTestBaseClasses(final String batchName)
    {
        final String effectiveBatchName = (batchName != null && !batchName.isBlank()) ? batchName.trim() : "Default";
        final List<TestRunEntity> allRuns = runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(effectiveBatchName);
        return getBatchTestBaseClasses(effectiveBatchName, allRuns);
    }

    public List<TestBaseClassDto> getBatchTestBaseClasses(final String batchName, final List<TestRunEntity> allRuns)
    {
        return getBatchTestBaseClasses(
            batchName,
            allRuns,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet()
        );
    }

    public List<TestBaseClassDto> getBatchTestBaseClasses(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> runBrowsers,
        final Set<String> runLocales,
        final Set<String> runEnvs,
        final Set<String> runAreas,
        final Set<String> runTags)
    {
        return getBatchTestBaseClasses(
            batchName,
            allRuns,
            runBrowsers,
            Collections.emptySet(),
            runLocales,
            Collections.emptySet(),
            runEnvs,
            Collections.emptySet(),
            runAreas,
            Collections.emptySet(),
            runTags,
            Collections.emptySet()
        );
    }

    public List<TestBaseClassDto> getBatchTestBaseClasses(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final List<String> uniquenessDimensions)
    {
        return getBatchTestBaseClasses(
            batchName,
            allRuns,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            uniquenessDimensions
        );
    }

    public List<TestBaseClassDto> getBatchTestBaseClasses(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> runBrowsers,
        final Set<String> runLocales,
        final Set<String> runEnvs,
        final Set<String> runAreas,
        final Set<String> runTags,
        final List<String> uniquenessDimensions)
    {
        return getBatchTestBaseClasses(
            batchName,
            allRuns,
            runBrowsers,
            Collections.emptySet(),
            runLocales,
            Collections.emptySet(),
            runEnvs,
            Collections.emptySet(),
            runAreas,
            Collections.emptySet(),
            runTags,
            Collections.emptySet(),
            uniquenessDimensions
        );
    }

    public List<TestBaseClassDto> getBatchTestBaseClasses(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> runBrowsers,
        final Set<String> excludedRunBrowsers,
        final Set<String> runLocales,
        final Set<String> excludedRunLocales,
        final Set<String> runEnvs,
        final Set<String> excludedRunEnvs,
        final Set<String> runAreas,
        final Set<String> excludedRunAreas,
        final Set<String> runTags,
        final Set<String> excludedRunTags)
    {
        return getBatchTestBaseClasses(
            batchName,
            allRuns,
            runBrowsers,
            excludedRunBrowsers,
            runLocales,
            excludedRunLocales,
            runEnvs,
            excludedRunEnvs,
            runAreas,
            excludedRunAreas,
            runTags,
            excludedRunTags,
            null
        );
    }

    public List<TestBaseClassDto> getBatchTestBaseClasses(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> runBrowsers,
        final Set<String> excludedRunBrowsers,
        final Set<String> runLocales,
        final Set<String> excludedRunLocales,
        final Set<String> runEnvs,
        final Set<String> excludedRunEnvs,
        final Set<String> runAreas,
        final Set<String> excludedRunAreas,
        final Set<String> runTags,
        final Set<String> excludedRunTags,
        final List<String> uniquenessDimensions)
    {
        if (allRuns == null || allRuns.isEmpty())
        {
            return List.of();
        }

        if ((runBrowsers != null && runBrowsers.contains("_none_"))
            || (runLocales != null && runLocales.contains("_none_"))
            || (runEnvs != null && runEnvs.contains("_none_"))
            || (runAreas != null && runAreas.contains("_none_"))
            || (runTags != null && runTags.contains("_none_")))
        {
            return List.of();
        }

        final List<TestBaseBugEntity> allBugs = bugRepository.findAll();
        final Map<String, List<String>> bugsMap = allBugs.stream()
            .filter(b -> b.getRemovedRunId() == null)
            .collect(Collectors.groupingBy(
                TestBaseBugEntity::getVariationId,
                Collectors.mapping(TestBaseBugEntity::getBugTicket, Collectors.toList())
            ));

        final List<TestRunEntity> sortedRuns = allRuns.stream()
            .sorted((a, b) -> Long.compare(
                b.getStartTimeMs() != null ? b.getStartTimeMs() : 0L,
                a.getStartTimeMs() != null ? a.getStartTimeMs() : 0L))
            .collect(Collectors.toList());

        populateRunTags(sortedRuns);
        final List<String> knownTagKeys = getAllTagKeys();
        final Map<String, TestBaseVariationEntity> variationMap = new LinkedHashMap<>();
        final Map<String, Integer> execCountMap = new HashMap<>();

        for (final TestRunEntity run : sortedRuns)
        {
            if (runEnvs != null && !runEnvs.isEmpty())
            {
                final String runEnv = run.getEnvironment() != null ? run.getEnvironment().trim() : "Default";
                final boolean matchEnv = runEnvs.stream().anyMatch(re -> re.equalsIgnoreCase(runEnv));
                if (!matchEnv)
                {
                    continue;
                }
            }
            if (excludedRunEnvs != null && !excludedRunEnvs.isEmpty())
            {
                final String runEnv = run.getEnvironment() != null ? run.getEnvironment().trim() : "Default";
                final boolean matchEnv = excludedRunEnvs.stream().anyMatch(re -> re.equalsIgnoreCase(runEnv));
                if (matchEnv)
                {
                    continue;
                }
            }

            if (runTags != null && !runTags.isEmpty())
            {
                final String runBatch = run.getBatchName() != null ? run.getBatchName().trim() : "Default";
                final boolean matchTag = runTags.stream().anyMatch(rt -> rt.equalsIgnoreCase(runBatch));
                if (!matchTag)
                {
                    continue;
                }
            }
            if (excludedRunTags != null && !excludedRunTags.isEmpty())
            {
                final String runBatch = run.getBatchName() != null ? run.getBatchName().trim() : "Default";
                final boolean matchTag = excludedRunTags.stream().anyMatch(rt -> rt.equalsIgnoreCase(runBatch));
                if (matchTag)
                {
                    continue;
                }
            }

            final RunReportDto report = getRunReport(run.getId());
            if (report == null || report.getExecutions() == null)
            {
                continue;
            }

            for (final TestExecutionDto exec : report.getExecutions())
            {
                final String testClass = (exec.getTestClass() != null && !exec.getTestClass().isBlank())
                    ? exec.getTestClass().trim()
                    : "GeneralTest";
                final String testMethod = (exec.getTestMethod() != null && !exec.getTestMethod().isBlank())
                    ? exec.getTestMethod().trim()
                    : "";
                final String dataSet = cleanDataSetString(exec.getTitle());
                if (!isValidTestVariation(testClass, testMethod, dataSet))
                {
                    continue;
                }
                final String normLoc = normalizeLocation(exec.getLocation());
                final String normBr = normalizeBrowser(exec.getBrowser());
                final String normArea = normalizeArea(exec.getAreaName());

                if (runBrowsers != null && !runBrowsers.isEmpty())
                {
                    final boolean matchBr = runBrowsers.stream().anyMatch(rb ->
                        rb.equalsIgnoreCase(normBr) || rb.equalsIgnoreCase(exec.getBrowser()) || normalizeBrowser(rb).equalsIgnoreCase(normBr)
                    );
                    if (!matchBr)
                    {
                        continue;
                    }
                }
                if (excludedRunBrowsers != null && !excludedRunBrowsers.isEmpty())
                {
                    final boolean matchBr = excludedRunBrowsers.stream().anyMatch(eb ->
                        eb.equalsIgnoreCase(normBr) || eb.equalsIgnoreCase(exec.getBrowser()) || normalizeBrowser(eb).equalsIgnoreCase(normBr)
                    );
                    if (matchBr)
                    {
                        continue;
                    }
                }

                if (runLocales != null && !runLocales.isEmpty())
                {
                    final boolean matchLoc = runLocales.stream().anyMatch(rl ->
                        rl.equalsIgnoreCase(normLoc) || rl.equalsIgnoreCase(exec.getLocation()) || normalizeLocation(rl).equalsIgnoreCase(normLoc)
                    );
                    if (!matchLoc)
                    {
                        continue;
                    }
                }
                if (excludedRunLocales != null && !excludedRunLocales.isEmpty())
                {
                    final boolean matchLoc = excludedRunLocales.stream().anyMatch(el ->
                        el.equalsIgnoreCase(normLoc) || el.equalsIgnoreCase(exec.getLocation()) || normalizeLocation(el).equalsIgnoreCase(normLoc)
                    );
                    if (matchLoc)
                    {
                        continue;
                    }
                }

                if (runAreas != null && !runAreas.isEmpty())
                {
                    final boolean matchArea = runAreas.stream().anyMatch(ra ->
                        normalizeArea(ra).equalsIgnoreCase(normArea)
                    );
                    if (!matchArea)
                    {
                        continue;
                    }
                }
                if (excludedRunAreas != null && !excludedRunAreas.isEmpty())
                {
                    final boolean matchArea = excludedRunAreas.stream().anyMatch(ea ->
                        normalizeArea(ea).equalsIgnoreCase(normArea)
                    );
                    if (matchArea)
                    {
                        continue;
                    }
                }

                final List<String> effectiveDims = (uniquenessDimensions != null && !uniquenessDimensions.isEmpty())
                    ? uniquenessDimensions
                    : ReportDashboardSettingService.DEFAULT_EXECUTION_IDENTITY_DIMENSIONS;
                final String logicalKey = buildLogicalKey(testClass, testMethod, dataSet, normLoc, normBr, run, exec, effectiveDims);
                final String canonicalId = generateVariationId(testClass, testMethod, dataSet, normLoc, normBr);
                final String varId = isDefaultExecutionIdentity(effectiveDims)
                    ? canonicalId
                    : (canonicalId + "-" + Math.abs(logicalKey.hashCode()));

                execCountMap.put(logicalKey, execCountMap.getOrDefault(logicalKey, 0) + 1);

                if (!variationMap.containsKey(logicalKey))
                {
                    final boolean hasBugs = bugsMap.containsKey(canonicalId) && !bugsMap.get(canonicalId).isEmpty();
                    final String rawStatus = exec.getStatus() != null ? exec.getStatus() : "passed-clean";
                    final String effectiveStatus;
                    if ("failed".equalsIgnoreCase(rawStatus) || "failed-unknown".equalsIgnoreCase(rawStatus) || "failed-known".equalsIgnoreCase(rawStatus) || "error".equalsIgnoreCase(rawStatus))
                    {
                        effectiveStatus = hasBugs ? "failed-known" : "failed-unknown";
                    }
                    else if ("passed".equalsIgnoreCase(rawStatus) || "succeeded-fixed".equalsIgnoreCase(rawStatus) || "passed-clean".equalsIgnoreCase(rawStatus) || "succeeded".equalsIgnoreCase(rawStatus) || "healed".equalsIgnoreCase(rawStatus))
                    {
                        effectiveStatus = hasBugs ? "succeeded-fixed" : "passed-clean";
                    }
                    else
                    {
                        effectiveStatus = rawStatus;
                    }

                    final String mode = (exec.getDisplayMode() != null && !exec.getDisplayMode().isBlank())
                        ? exec.getDisplayMode()
                        : ((exec.getMode() != null && !exec.getMode().isBlank()) ? exec.getMode() : "JAVA");
                    final boolean isHealed = exec.isHealed();
                    final String area = (exec.getAreaName() != null && !exec.getAreaName().isBlank())
                        ? exec.getAreaName()
                        : "@General";

                    final TestBaseVariationEntity varEntity = new TestBaseVariationEntity(
                        varId,
                        testClass,
                        testMethod,
                        dataSet.isEmpty() ? "Default" : dataSet,
                        normalizeArea(area),
                        normLoc,
                        normBr
                    );
                    if (exec.getSubArea() != null && !exec.getSubArea().isBlank())
                    {
                        varEntity.setSubAreaTag(normalizeArea(exec.getSubArea()));
                    }
                    varEntity.setLastStatus(effectiveStatus);
                    varEntity.setLastExecutedAt(run.getStartTimeMs() != null ? run.getStartTimeMs() : 0L);
                    varEntity.setHealed(isHealed);
                    varEntity.setExecutionMode(mode);
                    varEntity.setLastRunId(run.getId());
                    varEntity.setLastEnvironment(run.getEnvironment() != null ? run.getEnvironment() : "Default");
                    varEntity.setLastBatch(run.getBatchName() != null ? run.getBatchName() : "Default");

                    final Map<String, String> uProps = new LinkedHashMap<>();
                    for (final String dim : effectiveDims)
                    {
                        final String normDim = ReportDashboardSettingService.normalizeDimensionName(dim);
                        if ("environment".equals(normDim))
                        {
                            uProps.put("Environment", run.getEnvironment() != null ? run.getEnvironment() : "Default");
                        }
                        else if (!"browser".equals(normDim) && !"locale".equals(normDim))
                        {
                            List<String> tagVals = null;
                            if (exec.getTags() != null)
                            {
                                tagVals = TagUtils.findTagValues(exec.getTags(), dim);
                            }
                            if (tagVals == null || tagVals.isEmpty())
                            {
                                tagVals = getRunTagValues(run, dim);
                            }
                            final String tagVal = (tagVals == null || tagVals.isEmpty()) ? "Default" : String.join(", ", tagVals);
                            uProps.put(dim, tagVal);
                        }
                    }

                    if (knownTagKeys != null)
                    {
                        for (final String tKey : knownTagKeys)
                        {
                            if (tKey != null && !tKey.isBlank() && !uProps.containsKey(tKey) && !"batch".equalsIgnoreCase(tKey))
                            {
                                List<String> tagVals = null;
                                if (exec.getTags() != null)
                                {
                                    tagVals = TagUtils.findTagValues(exec.getTags(), tKey);
                                }
                                if (tagVals == null || tagVals.isEmpty())
                                {
                                    tagVals = getRunTagValues(run, tKey);
                                }
                                final String tagVal = (tagVals == null || tagVals.isEmpty()) ? "Default" : String.join(", ", tagVals);
                                uProps.put(tKey, tagVal);
                            }
                        }
                    }
                    varEntity.setIdentityProperties(uProps);

                    variationMap.put(logicalKey, varEntity);
                }
            }
        }

        for (final Map.Entry<String, TestBaseVariationEntity> entry : variationMap.entrySet())
        {
            final String logicalKey = entry.getKey();
            final TestBaseVariationEntity var = entry.getValue();
            var.setTotalExecutionsCount(execCountMap.getOrDefault(logicalKey, 1));
        }

        final Map<String, List<TestBaseVariationEntity>> byClass = variationMap.values().stream()
            .collect(Collectors.groupingBy(
                v -> (v.getTestClassName() != null && !v.getTestClassName().isEmpty()) ? v.getTestClassName() : "GeneralTest",
                LinkedHashMap::new,
                Collectors.toList()
            ));

        final List<TestBaseClassDto> classDtos = new ArrayList<>();
        for (final Map.Entry<String, List<TestBaseVariationEntity>> classEntry : byClass.entrySet())
        {
            final String className = classEntry.getKey();
            final List<TestBaseVariationEntity> classVars = classEntry.getValue();
            final String classContainerId = "batchClassContainer" + className.replaceAll("[^a-zA-Z0-9]", "");

            classDtos.add(new TestBaseClassDto(
                className,
                classContainerId,
                classVars.size(),
                classVars
            ));
        }

        return classDtos;
    }

    public List<TestBaseAreaDto> groupBatchTestClassesByArea(final List<TestBaseClassDto> classDtos)
    {
        if (classDtos == null || classDtos.isEmpty())
        {
            return List.of();
        }

        final Map<String, List<TestBaseClassDto>> byAreaMap = new LinkedHashMap<>();
        for (final TestBaseClassDto classDto : classDtos)
        {
            final List<TestBaseVariationEntity> classVars = classDto.getVariations();
            String areaName = "Default";
            if (classVars != null)
            {
                for (final TestBaseVariationEntity v : classVars)
                {
                    final String area = v.getAreaTag();
                    if (isRealArea(area))
                    {
                        areaName = normalizeArea(area);
                        break;
                    }
                }
            }
            byAreaMap.computeIfAbsent(areaName, k -> new ArrayList<>()).add(classDto);
        }

        final List<TestBaseAreaDto> areas = new ArrayList<>();
        for (final Map.Entry<String, List<TestBaseClassDto>> areaEntry : byAreaMap.entrySet())
        {
            final String areaName = areaEntry.getKey();
            final List<TestBaseClassDto> areaClasses = areaEntry.getValue();

            final String cleanAreaName = areaName.replace("@", "").replaceAll("[^a-zA-Z0-9]", "");
            final String areaPaneId = "batchArea" + cleanAreaName;
            final String tabBtnId = "batchTabBtn" + cleanAreaName;
            final String icon = resolveAreaIcon(areaName);

            final List<TestBaseSubAreaDto> subAreas = new ArrayList<>();
            final boolean hasSub = areaClasses.stream()
                .anyMatch(cd -> cd.getVariations() != null && cd.getVariations().stream().anyMatch(v -> v.getSubAreaTag() != null && !v.getSubAreaTag().isBlank()));

            if (hasSub)
            {
                final Map<String, List<TestBaseVariationEntity>> varsBySub = new LinkedHashMap<>();
                for (final TestBaseClassDto cd : areaClasses)
                {
                    if (cd.getVariations() != null)
                    {
                        for (final TestBaseVariationEntity v : cd.getVariations())
                        {
                            String rawSub = (v.getSubAreaTag() != null && !v.getSubAreaTag().isBlank()) ? v.getSubAreaTag().trim() : "General";
                            while (rawSub.startsWith("@"))
                            {
                                rawSub = rawSub.substring(1).trim();
                            }
                            final String subTag = rawSub.isEmpty() ? "General" : rawSub;
                            varsBySub.computeIfAbsent(subTag, k -> new ArrayList<>()).add(v);
                        }
                    }
                }

                for (final Map.Entry<String, List<TestBaseVariationEntity>> subEntry : varsBySub.entrySet())
                {
                    final String subAreaName = subEntry.getKey();
                    final List<TestBaseVariationEntity> subVars = subEntry.getValue();

                    final Map<String, List<TestBaseVariationEntity>> classMap = new LinkedHashMap<>();
                    for (final TestBaseVariationEntity v : subVars)
                    {
                        classMap.computeIfAbsent(v.getTestClassName(), k -> new ArrayList<>()).add(v);
                    }

                    final List<TestBaseClassDto> subClasses = new ArrayList<>();
                    final String cleanSub = subAreaName.replace("@", "").replaceAll("[^a-zA-Z0-9]", "");
                    for (final Map.Entry<String, List<TestBaseVariationEntity>> cEntry : classMap.entrySet())
                    {
                        final String cName = cEntry.getKey();
                        final List<TestBaseVariationEntity> cvList = cEntry.getValue();
                        final String cId = "batchClass" + cName.replaceAll("[^a-zA-Z0-9]", "") + cleanSub;
                        subClasses.add(new TestBaseClassDto(cName, cId, cvList.size(), cvList));
                    }

                    final String subAreaPaneId = "batchSubArea" + cleanAreaName + cleanSub;
                    subAreas.add(new TestBaseSubAreaDto(subAreaName, subAreaPaneId, subClasses.size(), subClasses));
                }
            }

            areas.add(new TestBaseAreaDto(
                areaName,
                areaPaneId,
                tabBtnId,
                icon,
                areaClasses.size(),
                areaClasses,
                subAreas
            ));
        }

        return areas;
    }

    public List<TestBaseAreaDto> getBatchTestBaseAreas(final String batchName, final List<TestRunEntity> allRuns)
    {
        final List<TestBaseClassDto> classDtos = getBatchTestBaseClasses(batchName, allRuns);
        return groupBatchTestClassesByArea(classDtos);
    }

    public BatchLatestExecutionsDto getBatchLatestTestExecutionsSummary(final String batchName, final List<TestRunEntity> allRuns)
    {
        return getBatchLatestTestExecutionsSummary(batchName, allRuns, Collections.emptySet(), null);
    }

    public BatchLatestExecutionsDto getBatchLatestTestExecutionsSummary(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> excludedRunIds,
        final Long cutoffTimestampMs)
    {
        return getBatchLatestTestExecutionsSummary(
            batchName,
            allRuns,
            excludedRunIds,
            cutoffTimestampMs,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptyMap()
        );
    }

    public BatchLatestExecutionsDto getBatchLatestTestExecutionsSummary(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> excludedRunIds,
        final Long cutoffTimestampMs,
        final Set<String> runBrowsers,
        final Set<String> runLocales,
        final Set<String> runEnvs,
        final Set<String> runAreas,
        final Set<String> runTags,
        final Map<String, String> runAreasMap)
    {
        return getBatchLatestTestExecutionsSummary(
            batchName,
            allRuns,
            excludedRunIds,
            cutoffTimestampMs,
            null,
            runBrowsers,
            runLocales,
            runEnvs,
            runAreas,
            runTags,
            runAreasMap,
            Collections.emptyMap()
        );
    }

    public BatchLatestExecutionsDto getBatchLatestTestExecutionsSummary(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> excludedRunIds,
        final Long fromTimestampMs,
        final Long toTimestampMs,
        final Set<String> runBrowsers,
        final Set<String> runLocales,
        final Set<String> runEnvs,
        final Set<String> runAreas,
        final Set<String> runTags,
        final Map<String, String> runAreasMap)
    {
        return getBatchLatestTestExecutionsSummary(
            batchName,
            allRuns,
            excludedRunIds,
            fromTimestampMs,
            toTimestampMs,
            runBrowsers,
            runLocales,
            runEnvs,
            runAreas,
            runTags,
            runAreasMap,
            Collections.emptyMap()
        );
    }

    public BatchLatestExecutionsDto getBatchLatestTestExecutionsSummary(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> excludedRunIds,
        final Long fromTimestampMs,
        final Long toTimestampMs,
        final Set<String> runBrowsers,
        final Set<String> runLocales,
        final Set<String> runEnvs,
        final Set<String> runAreas,
        final Set<String> runTags,
        final Map<String, String> runAreasMap,
        final Map<String, Set<String>> runTagsByType)
    {
        return getBatchLatestTestExecutionsSummary(
            batchName,
            allRuns,
            excludedRunIds,
            fromTimestampMs,
            toTimestampMs,
            runBrowsers,
            Collections.emptySet(),
            runLocales,
            Collections.emptySet(),
            runEnvs,
            Collections.emptySet(),
            runAreas,
            Collections.emptySet(),
            runTags,
            Collections.emptySet(),
            runAreasMap,
            runTagsByType,
            Collections.emptyMap()
        );
    }

    public BatchLatestExecutionsDto getBatchLatestTestExecutionsSummary(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final List<String> uniquenessDimensions)
    {
        return getBatchLatestTestExecutionsSummary(
            batchName,
            allRuns,
            Collections.emptySet(),
            null,
            null,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            uniquenessDimensions
        );
    }


    public BatchLatestExecutionsDto getBatchLatestTestExecutionsSummary(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> excludedRunIds,
        final Long fromTimestampMs,
        final Long toTimestampMs,
        final Set<String> runBrowsers,
        final Set<String> excludedRunBrowsers,
        final Set<String> runLocales,
        final Set<String> excludedRunLocales,
        final Set<String> runEnvs,
        final Set<String> excludedRunEnvs,
        final Set<String> runAreas,
        final Set<String> excludedRunAreas,
        final Set<String> runTags,
        final Set<String> excludedRunTags,
        final Map<String, String> runAreasMap,
        final Map<String, Set<String>> runTagsByType,
        final Map<String, Set<String>> excludedRunTagsByType)
    {
        return getBatchLatestTestExecutionsSummary(
            batchName,
            allRuns,
            excludedRunIds,
            fromTimestampMs,
            toTimestampMs,
            runBrowsers,
            excludedRunBrowsers,
            runLocales,
            excludedRunLocales,
            runEnvs,
            excludedRunEnvs,
            runAreas,
            excludedRunAreas,
            runTags,
            excludedRunTags,
            runAreasMap,
            runTagsByType,
            excludedRunTagsByType,
            null
        );
    }

    public BatchLatestExecutionsDto getBatchLatestTestExecutionsSummary(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> excludedRunIds,
        final Long fromTimestampMs,
        final Long toTimestampMs,
        final Set<String> runBrowsers,
        final Set<String> excludedRunBrowsers,
        final Set<String> runLocales,
        final Set<String> excludedRunLocales,
        final Set<String> runEnvs,
        final Set<String> excludedRunEnvs,
        final Set<String> runAreas,
        final Set<String> excludedRunAreas,
        final Set<String> runTags,
        final Set<String> excludedRunTags,
        final Map<String, String> runAreasMap,
        final Map<String, Set<String>> runTagsByType,
        final Map<String, Set<String>> excludedRunTagsByType,
        final List<String> uniquenessDimensions)
    {
        return getBatchLatestTestExecutionsSummary(
            batchName, allRuns, excludedRunIds, fromTimestampMs, toTimestampMs,
            runBrowsers, excludedRunBrowsers, runLocales, excludedRunLocales,
            runEnvs, excludedRunEnvs, runAreas, excludedRunAreas,
            runTags, excludedRunTags, runAreasMap, runTagsByType,
            excludedRunTagsByType, uniquenessDimensions, null
        );
    }

    public BatchLatestExecutionsDto getBatchLatestTestExecutionsSummary(
        final String batchName,
        final List<TestRunEntity> allRuns,
        final Set<String> excludedRunIds,
        final Long fromTimestampMs,
        final Long toTimestampMs,
        final Set<String> runBrowsers,
        final Set<String> excludedRunBrowsers,
        final Set<String> runLocales,
        final Set<String> excludedRunLocales,
        final Set<String> runEnvs,
        final Set<String> excludedRunEnvs,
        final Set<String> runAreas,
        final Set<String> excludedRunAreas,
        final Set<String> runTags,
        final Set<String> excludedRunTags,
        final Map<String, String> runAreasMap,
        final Map<String, Set<String>> runTagsByType,
        final Map<String, Set<String>> excludedRunTagsByType,
        final List<String> uniquenessDimensions,
        final String asOfRunId)
    {
        final String effectiveBatchName = (batchName != null && !batchName.isBlank()) ? batchName.trim() : "Default";

        if (allRuns == null || allRuns.isEmpty())
        {
            final BatchSummaryDto emptySummary = new BatchSummaryDto(
                "batch-" + effectiveBatchName,
                effectiveBatchName,
                "Default",
                "",
                "No runs",
                "N/A",
                "",
                List.of(),
                List.of()
            );
            final TokenCostTrendDto emptyTrend = new TokenCostTrendDto(0.0, -1.0, 0L, List.of());
            final BatchSuiteEvolutionDto emptyEvolution = new BatchSuiteEvolutionDto(Collections.emptyList());
            return new BatchLatestExecutionsDto(emptySummary, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Collections.emptyMap(), List.of(), 0, 0, List.of(), emptyTrend, emptyEvolution, null, "No runs", false);
        }

        final Set<String> exclusions = (excludedRunIds != null) ? excludedRunIds : Collections.emptySet();

        final List<TestRunEntity> eligibleRuns = allRuns.stream()
            .filter(r -> r.getId() != null && !exclusions.contains(r.getId()))
            .filter(r -> fromTimestampMs == null || (r.getStartTimeMs() != null && r.getStartTimeMs() >= fromTimestampMs))
            .filter(r -> toTimestampMs == null || (r.getStartTimeMs() != null && r.getStartTimeMs() <= toTimestampMs))
            .filter(r -> matchesRunCriteria(r, runBrowsers, excludedRunBrowsers, runLocales, excludedRunLocales, runEnvs, excludedRunEnvs, runAreas, excludedRunAreas, runTags, excludedRunTags, runAreasMap, runTagsByType, excludedRunTagsByType))
            .sorted((a, b) -> Long.compare(
                b.getStartTimeMs() != null ? b.getStartTimeMs() : 0L,
                a.getStartTimeMs() != null ? a.getStartTimeMs() : 0L))
            .collect(Collectors.toList());

        if (eligibleRuns.isEmpty())
        {
            final BatchSummaryDto emptySummary = new BatchSummaryDto(
                "batch-" + effectiveBatchName,
                effectiveBatchName,
                "Default",
                "",
                "No runs",
                "N/A",
                "",
                List.of(),
                List.of()
            );
            final TokenCostTrendDto emptyTrend = new TokenCostTrendDto(0.0, -1.0, 0L, List.of());
            final BatchSuiteEvolutionDto emptyEvolution = new BatchSuiteEvolutionDto(Collections.emptyList());
            return new BatchLatestExecutionsDto(emptySummary, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Collections.emptyMap(), List.of(), 0, 0, List.of(), emptyTrend, emptyEvolution, null, "No runs", false);
        }

        int targetIdx = -1;
        if (asOfRunId != null && !asOfRunId.isBlank())
        {
            for (int i = 0; i < eligibleRuns.size(); i++)
            {
                if (asOfRunId.equalsIgnoreCase(eligibleRuns.get(i).getId()))
                {
                    targetIdx = i;
                    break;
                }
            }
        }

        final boolean isHistoricalSnapshot = (targetIdx > 0);
        final String effectiveAsOfRunId = (targetIdx >= 0)
            ? eligibleRuns.get(targetIdx).getId()
            : (!eligibleRuns.isEmpty() ? eligibleRuns.get(0).getId() : null);
        final String asOfTimestampLabel = (targetIdx >= 0)
            ? eligibleRuns.get(targetIdx).getTimestampLabel()
            : (!eligibleRuns.isEmpty() ? eligibleRuns.get(0).getTimestampLabel() : "No runs");

        final List<TestBaseBugEntity> allBugs = bugRepository.findAll();
        final Map<String, List<String>> bugsMap = allBugs.stream()
            .filter(b -> b.getRemovedRunId() == null)
            .collect(Collectors.groupingBy(
                TestBaseBugEntity::getVariationId,
                Collectors.mapping(TestBaseBugEntity::getBugTicket, Collectors.toList())
            ));
        final Map<String, List<String>> bugSeveritiesMap = allBugs.stream()
            .filter(b -> b.getRemovedRunId() == null)
            .collect(Collectors.groupingBy(
                TestBaseBugEntity::getVariationId,
                Collectors.mapping(b -> b.getSeverity() != null ? b.getSeverity().toUpperCase(Locale.ROOT) : "MAJOR", Collectors.toList())
            ));

        final Map<String, String> variationStatus = new LinkedHashMap<>();
        final Set<String> runningVariationKeys = new HashSet<>();
        final Map<String, Boolean> variationHealed = new LinkedHashMap<>();
        final Map<String, String> variationMode = new LinkedHashMap<>();
        final Map<String, String> variationRunId = new LinkedHashMap<>();
        final Map<String, String> variationEnvironment = new LinkedHashMap<>();
        final Map<String, String> variationBrowser = new LinkedHashMap<>();
        final Map<String, String> variationLocation = new LinkedHashMap<>();
        final Map<String, String> variationArea = new LinkedHashMap<>();
        final Map<String, String> variationBatch = new LinkedHashMap<>();
        final Map<String, Map<String, List<String>>> variationTags = new LinkedHashMap<>();
        final Map<String, Double> variationLlmCost = new LinkedHashMap<>();
        final Map<String, Long> variationLlmTokens = new LinkedHashMap<>();
        final Map<String, String> variationExecutionId = new LinkedHashMap<>();
        final Map<String, String> variationCanonicalId = new LinkedHashMap<>();
        final Map<String, List<String>> variationBugs = new LinkedHashMap<>();
        final Map<String, List<String>> variationBugSeverities = new LinkedHashMap<>();
        final Map<String, Boolean> variationFlaky = new LinkedHashMap<>();
        final Map<String, Integer> variationFlakyFailureCount = new LinkedHashMap<>();
        final Map<String, Integer> variationFlakyTotalRuns = new LinkedHashMap<>();
        final Map<String, String> variationFlakyReason = new LinkedHashMap<>();
        final Map<String, List<RunVariationCost>> runVariationsMap = new HashMap<>();
        final Map<String, Map<String, VariationRunState>> runExecutionsMap = new HashMap<>();
        final Map<String, String> varAreaMap = new HashMap<>();
        final Map<String, String> varBrowserMap = new HashMap<>();
        final Map<String, String> varLocationMap = new HashMap<>();
        final Map<String, String> varEnvMap = new HashMap<>();
        final Map<String, Map<String, List<String>>> varTagsMap = new HashMap<>();
        final Map<String, Map<String, Map<String, List<String>>>> runVariationTagsMap = new HashMap<>();

        for (int runOrderIdx = 0; runOrderIdx < eligibleRuns.size(); runOrderIdx++)
        {
            final TestRunEntity run = eligibleRuns.get(runOrderIdx);
            final boolean isRunInSnapshot = (targetIdx < 0) || (runOrderIdx >= targetIdx);
            final RunReportDto report = getRunReport(run.getId());
            if (report == null || report.getExecutions() == null)
            {
                continue;
            }

            for (final TestExecutionDto exec : report.getExecutions())
            {
                final String testClass = (exec.getTestClass() != null && !exec.getTestClass().isBlank())
                    ? exec.getTestClass().trim()
                    : "GeneralTest";
                final String testMethod = (exec.getTestMethod() != null && !exec.getTestMethod().isBlank())
                    ? exec.getTestMethod().trim()
                    : "";
                final String dataSet = cleanDataSetString(exec.getTitle());
                if (!isValidTestVariation(testClass, testMethod, dataSet))
                {
                    continue;
                }
                final String normLoc = normalizeLocation(exec.getLocation());
                final String normBr = normalizeBrowser(exec.getBrowser());
                final String normArea = normalizeArea(exec.getAreaName());

                if (runBrowsers != null && !runBrowsers.isEmpty())
                {
                    final boolean matchBr = runBrowsers.stream().anyMatch(rb ->
                        rb.equalsIgnoreCase(normBr) || rb.equalsIgnoreCase(exec.getBrowser()) || normalizeBrowser(rb).equalsIgnoreCase(normBr)
                    );
                    if (!matchBr)
                    {
                        continue;
                    }
                }
                if (excludedRunBrowsers != null && !excludedRunBrowsers.isEmpty())
                {
                    final boolean matchBr = excludedRunBrowsers.stream().anyMatch(eb ->
                        eb.equalsIgnoreCase(normBr) || eb.equalsIgnoreCase(exec.getBrowser()) || normalizeBrowser(eb).equalsIgnoreCase(normBr)
                    );
                    if (matchBr)
                    {
                        continue;
                    }
                }

                if (runLocales != null && !runLocales.isEmpty())
                {
                    final boolean matchLoc = runLocales.stream().anyMatch(rl ->
                        rl.equalsIgnoreCase(normLoc) || rl.equalsIgnoreCase(exec.getLocation()) || normalizeLocation(rl).equalsIgnoreCase(normLoc)
                    );
                    if (!matchLoc)
                    {
                        continue;
                    }
                }
                if (excludedRunLocales != null && !excludedRunLocales.isEmpty())
                {
                    final boolean matchLoc = excludedRunLocales.stream().anyMatch(el ->
                        el.equalsIgnoreCase(normLoc) || el.equalsIgnoreCase(exec.getLocation()) || normalizeLocation(el).equalsIgnoreCase(normLoc)
                    );
                    if (matchLoc)
                    {
                        continue;
                    }
                }

                if (runAreas != null && !runAreas.isEmpty())
                {
                    final boolean matchArea = runAreas.stream().anyMatch(ra ->
                        normalizeArea(ra).equalsIgnoreCase(normArea)
                    );
                    if (!matchArea)
                    {
                        continue;
                    }
                }
                if (excludedRunAreas != null && !excludedRunAreas.isEmpty())
                {
                    final boolean matchArea = excludedRunAreas.stream().anyMatch(ea ->
                        normalizeArea(ea).equalsIgnoreCase(normArea)
                    );
                    if (matchArea)
                    {
                        continue;
                    }
                }

                final List<String> effectiveDims = getEffectiveExecutionIdentityDimensions(uniquenessDimensions);
                final String logicalKey = buildLogicalKey(testClass, testMethod, dataSet, normLoc, normBr, run, exec, effectiveDims);
                final String canonicalId = generateVariationId(testClass, testMethod, dataSet, normLoc, normBr);

                varAreaMap.putIfAbsent(logicalKey, normArea);
                varBrowserMap.putIfAbsent(logicalKey, normBr);
                varLocationMap.putIfAbsent(logicalKey, normLoc);
                varEnvMap.putIfAbsent(logicalKey, (run.getEnvironment() != null && !run.getEnvironment().isBlank()) ? run.getEnvironment() : "Default");
                final Map<String, List<String>> combinedTags = new LinkedHashMap<>();
                final Map<String, List<String>> currentRunTags = getRunTagsMap(run);
                if (currentRunTags != null && !currentRunTags.isEmpty())
                {
                    for (final Map.Entry<String, List<String>> rEntry : currentRunTags.entrySet())
                    {
                        if (rEntry.getKey() != null && rEntry.getValue() != null)
                        {
                            combinedTags.put(rEntry.getKey().toLowerCase(Locale.ROOT), new ArrayList<>(rEntry.getValue()));
                        }
                    }
                }
                if (exec.getTags() != null && !exec.getTags().isEmpty())
                {
                    for (final Map.Entry<String, List<String>> eEntry : exec.getTags().entrySet())
                    {
                        if (eEntry.getKey() != null && eEntry.getValue() != null)
                        {
                            combinedTags.put(eEntry.getKey().toLowerCase(Locale.ROOT), new ArrayList<>(eEntry.getValue()));
                        }
                    }
                }
                if (!combinedTags.isEmpty())
                {
                    varTagsMap.putIfAbsent(logicalKey, combinedTags);
                    runVariationTagsMap.computeIfAbsent(run.getId(), k -> new HashMap<>()).put(logicalKey, combinedTags);
                }

                runVariationsMap.computeIfAbsent(run.getId(), k -> new ArrayList<>())
                    .add(new RunVariationCost(logicalKey, exec.getLlmCost(), exec.getLlmTotalTokens()));

                final boolean hasBugs = bugsMap.containsKey(canonicalId) && !bugsMap.get(canonicalId).isEmpty();
                final String rawStatus = exec.getStatus() != null ? exec.getStatus() : "passed-clean";
                final String effectiveStatus;
                if ("failed".equalsIgnoreCase(rawStatus) || "failed-unknown".equalsIgnoreCase(rawStatus) || "failed-known".equalsIgnoreCase(rawStatus) || "error".equalsIgnoreCase(rawStatus))
                {
                    effectiveStatus = hasBugs ? "failed-known" : "failed-unknown";
                }
                else if ("passed".equalsIgnoreCase(rawStatus) || "succeeded-fixed".equalsIgnoreCase(rawStatus) || "passed-clean".equalsIgnoreCase(rawStatus) || "succeeded".equalsIgnoreCase(rawStatus) || "healed".equalsIgnoreCase(rawStatus))
                {
                    effectiveStatus = hasBugs ? "succeeded-fixed" : "passed-clean";
                }
                else
                {
                    effectiveStatus = rawStatus;
                }

                final String mode = (exec.getDisplayMode() != null && !exec.getDisplayMode().isBlank())
                    ? exec.getDisplayMode()
                    : ((exec.getMode() != null && !exec.getMode().isBlank()) ? exec.getMode() : "JAVA");

                runExecutionsMap.computeIfAbsent(run.getId(), k -> new LinkedHashMap<>())
                    .put(logicalKey, new VariationRunState(effectiveStatus, exec.isHealed(), mode, exec.isFlaky()));

                if (isRunInSnapshot && !exec.isFinished())
                {
                    runningVariationKeys.add(logicalKey);
                }
                else if (isRunInSnapshot && !variationStatus.containsKey(logicalKey) && !runningVariationKeys.contains(logicalKey))
                {
                    final String env = (run.getEnvironment() != null && !run.getEnvironment().isBlank())
                        ? run.getEnvironment()
                        : "Default";
                    final String bName = (run.getBatchName() != null && !run.getBatchName().isBlank())
                        ? run.getBatchName()
                        : "Default";

                    variationStatus.put(logicalKey, effectiveStatus);
                    variationHealed.put(logicalKey, exec.isHealed());
                    variationMode.put(logicalKey, mode);
                    variationFlaky.put(logicalKey, exec.isFlaky());
                    variationFlakyFailureCount.put(logicalKey, exec.getFlakyFailureCount());
                    variationFlakyTotalRuns.put(logicalKey, exec.getFlakyTotalRuns());
                    variationFlakyReason.put(logicalKey, exec.getFlakyReason());
                    variationRunId.put(logicalKey, run.getId());
                    variationEnvironment.put(logicalKey, env);
                    variationBrowser.put(logicalKey, normBr);
                    variationLocation.put(logicalKey, normLoc);
                    variationArea.put(logicalKey, normArea);
                    variationBatch.put(logicalKey, bName);
                    variationTags.put(logicalKey, exec.getTags() != null ? exec.getTags() : Collections.emptyMap());
                    variationLlmCost.put(logicalKey, exec.getLlmCost());
                    variationLlmTokens.put(logicalKey, exec.getLlmTotalTokens());
                    variationExecutionId.put(logicalKey, exec.getId());
                    variationCanonicalId.put(logicalKey, canonicalId);
                    if (bugsMap.containsKey(canonicalId))
                    {
                        variationBugs.put(logicalKey, bugsMap.get(canonicalId));
                    }
                    if (bugSeveritiesMap.containsKey(canonicalId))
                    {
                        variationBugSeverities.put(logicalKey, bugSeveritiesMap.get(canonicalId));
                    }
                }
            }
        }

        int passClean = 0;
        int passHealed = 0;
        int passAi = 0;
        int fixedClean = 0;
        int fixedHealed = 0;
        int fixedAi = 0;
        int knownClean = 0;
        int knownHealed = 0;
        int knownAi = 0;
        int unknownClean = 0;
        int unknownHealed = 0;
        int unknownAi = 0;
        int ignored = 0;

        final Map<String, ScopedRunStats> runStatsMap = new LinkedHashMap<>();
        for (final String key : variationStatus.keySet())
        {
            final String rId = variationRunId.get(key);
            final ScopedRunStats runStats = (rId != null)
                ? runStatsMap.computeIfAbsent(rId, k -> new ScopedRunStats())
                : null;
            if (runStats != null)
            {
                final String b = variationBrowser.get(key);
                if (b != null && !b.isBlank())
                {
                    runStats.browsers.add(b);
                }
                final String l = variationLocation.get(key);
                if (l != null && !l.isBlank())
                {
                    runStats.locales.add(l);
                }
            }

            if (Boolean.TRUE.equals(variationFlaky.get(key)) && runStats != null)
            {
                runStats.flakyCount++;
            }

            final String status = variationStatus.get(key);
            final boolean healed = Boolean.TRUE.equals(variationHealed.get(key));
            final String mode = variationMode.getOrDefault(key, "JAVA");
            final boolean ai = "AI".equalsIgnoreCase(mode);

            if ("passed-clean".equalsIgnoreCase(status) || "passed".equalsIgnoreCase(status) || "succeeded".equalsIgnoreCase(status))
            {
                if (healed)
                {
                    passHealed++;
                    if (runStats != null)
                    {
                        runStats.passHealed++;
                    }
                }
                else if (ai)
                {
                    passAi++;
                    if (runStats != null)
                    {
                        runStats.passAi++;
                    }
                }
                else
                {
                    passClean++;
                    if (runStats != null)
                    {
                        runStats.passClean++;
                    }
                }
            }
            else if ("succeeded-fixed".equalsIgnoreCase(status) || "fixed".equalsIgnoreCase(status))
            {
                if (healed)
                {
                    fixedHealed++;
                    if (runStats != null)
                    {
                        runStats.fixedHealed++;
                    }
                }
                else if (ai)
                {
                    fixedAi++;
                    if (runStats != null)
                    {
                        runStats.fixedAi++;
                    }
                }
                else
                {
                    fixedClean++;
                    if (runStats != null)
                    {
                        runStats.fixedClean++;
                    }
                }
            }
            else if ("failed-known".equalsIgnoreCase(status) || "known".equalsIgnoreCase(status))
            {
                if (healed)
                {
                    knownHealed++;
                    if (runStats != null)
                    {
                        runStats.knownHealed++;
                    }
                }
                else if (ai)
                {
                    knownAi++;
                    if (runStats != null)
                    {
                        runStats.knownAi++;
                    }
                }
                else
                {
                    knownClean++;
                    if (runStats != null)
                    {
                        runStats.knownClean++;
                    }
                }
            }
            else if ("failed-unknown".equalsIgnoreCase(status) || "unknown".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status))
            {
                if (healed)
                {
                    unknownHealed++;
                    if (runStats != null)
                    {
                        runStats.unknownHealed++;
                    }
                }
                else if (ai)
                {
                    unknownAi++;
                    if (runStats != null)
                    {
                        runStats.unknownAi++;
                    }
                }
                else
                {
                    unknownClean++;
                    if (runStats != null)
                    {
                        runStats.unknownClean++;
                    }
                }
            }
            else
            {
                ignored++;
                if (runStats != null)
                {
                    runStats.ignored++;
                }
            }
        }

        final int totalPassed = passClean + passHealed + passAi;
        final int totalFixed = fixedClean + fixedHealed + fixedAi;
        final int totalKnown = knownClean + knownHealed + knownAi;
        final int totalUnknown = unknownClean + unknownHealed + unknownAi;
        final int totalTests = totalPassed + totalFixed + totalKnown + totalUnknown + ignored;

        final Set<String> contributingRunIds = new LinkedHashSet<>(variationRunId.values());
        final List<TestRunEntity> contributingRuns = new ArrayList<>();
        for (final TestRunEntity r : eligibleRuns)
        {
            if (r.getId() != null && contributingRunIds.contains(r.getId()))
            {
                final ScopedRunStats stats = runStatsMap.get(r.getId());
                final TestRunEntity scopedRun = new TestRunEntity();
                scopedRun.setId(r.getId());
                scopedRun.setBatchName(r.getBatchName());
                scopedRun.setStatus(r.getStatus());
                scopedRun.setTriggerSource(r.getTriggerSource());
                scopedRun.setEnvironment(r.getEnvironment());
                scopedRun.setTimestampLabel(r.getTimestampLabel());
                scopedRun.setStartTimeMs(r.getStartTimeMs());
                scopedRun.setEndTimeMs(r.getEndTimeMs());
                scopedRun.setRunComment(r.getRunComment());
                scopedRun.setRunJsonPath(r.getRunJsonPath());
                scopedRun.setIsDeleted(false);
                scopedRun.setTags(r.getTags());

                if (stats != null)
                {
                    scopedRun.setPassedHealedCount(stats.passHealed);
                    scopedRun.setPassedAiCount(stats.passAi);
                    final int runPassed = stats.passClean + stats.passHealed + stats.passAi;
                    scopedRun.setPassedCount(runPassed);

                    scopedRun.setSucceededFixedHealedCount(stats.fixedHealed);
                    scopedRun.setSucceededFixedAiCount(stats.fixedAi);
                    final int runFixed = stats.fixedClean + stats.fixedHealed + stats.fixedAi;
                    scopedRun.setSucceededFixedCount(runFixed);

                    scopedRun.setFailedKnownHealedCount(stats.knownHealed);
                    scopedRun.setFailedKnownAiCount(stats.knownAi);
                    final int runKnown = stats.knownClean + stats.knownHealed + stats.knownAi;
                    scopedRun.setFailedKnownCount(runKnown);

                    scopedRun.setFailedUnknownHealedCount(stats.unknownHealed);
                    scopedRun.setFailedUnknownAiCount(stats.unknownAi);
                    final int runUnknown = stats.unknownClean + stats.unknownHealed + stats.unknownAi;
                    scopedRun.setFailedUnknownCount(runUnknown);

                    scopedRun.setIgnoredCount(stats.ignored);
                    scopedRun.setFlakyCount(stats.flakyCount);
                    final int runTotal = runPassed + runFixed + runKnown + runUnknown + stats.ignored;
                    scopedRun.setTotalTests(runTotal);
                    scopedRun.setPassRate(runTotal > 0 ? ((double) (runPassed + runFixed) / runTotal * 100.0) : 0.0);

                    if (!stats.browsers.isEmpty())
                    {
                        scopedRun.setBrowsersCsv(String.join(",", stats.browsers));
                    }
                    else
                    {
                        scopedRun.setBrowsersCsv(r.getBrowsersCsv());
                    }

                    if (!stats.locales.isEmpty())
                    {
                        scopedRun.setLocalesCsv(String.join(",", stats.locales));
                    }
                    else
                    {
                        scopedRun.setLocalesCsv(r.getLocalesCsv());
                    }
                }
                else
                {
                    scopedRun.setTotalTests(0);
                    scopedRun.setPassRate(0.0);
                    scopedRun.setBrowsersCsv(r.getBrowsersCsv());
                    scopedRun.setLocalesCsv(r.getLocalesCsv());
                }

                contributingRuns.add(scopedRun);
            }
        }

        final String latestRunId = !contributingRuns.isEmpty() ? contributingRuns.get(0).getId() : null;
        final String lastExecuted = !contributingRuns.isEmpty() ? contributingRuns.get(0).getTimestampLabel() : "No runs";
        final String passRateText = totalTests > 0
            ? String.format(Locale.ROOT, "%.0f%% Pass", ((double) (totalPassed + totalFixed) / totalTests * 100.0))
            : "N/A";

        final List<String> distinctLocales = variationLocation.values().stream().distinct().sorted().collect(Collectors.toList());
        final List<String> distinctBrowsers = variationBrowser.values().stream().distinct().sorted().collect(Collectors.toList());

        final String environment = !eligibleRuns.isEmpty() && eligibleRuns.get(0).getEnvironment() != null
            ? eligibleRuns.get(0).getEnvironment()
            : "Default";

        final BatchSummaryDto summary = new BatchSummaryDto(
            "batch-" + effectiveBatchName,
            effectiveBatchName,
            environment,
            "",
            lastExecuted,
            passRateText,
            "",
            distinctLocales,
            distinctBrowsers,
            latestRunId,
            totalPassed,
            totalFixed,
            totalKnown,
            totalUnknown,
            ignored,
            totalTests,
            passHealed,
            fixedHealed,
            knownHealed,
            unknownHealed,
            passAi,
            fixedAi,
            knownAi,
            unknownAi
        );

        final String[] palette = new String[] {
            "#10b981", "#3b82f6", "#f59e0b", "#8b5cf6", "#ec4899", "#06b6d4", "#6366f1", "#14b8a6"
        };

        final Map<String, Integer> bMap = new LinkedHashMap<>();
        for (final String br : variationBrowser.values())
        {
            bMap.put(br, bMap.getOrDefault(br, 0) + 1);
        }
        final List<DistributionRatioDto> browserRatios = new ArrayList<>();
        final int bTotal = bMap.values().stream().mapToInt(Integer::intValue).sum();
        int colorIdx = 0;
        for (final Map.Entry<String, Integer> entry : bMap.entrySet())
        {
            final double pct = bTotal > 0 ? ((double) entry.getValue() / bTotal * 100.0) : 0.0;
            browserRatios.add(new DistributionRatioDto(
                entry.getKey(),
                entry.getValue(),
                pct,
                palette[colorIdx % palette.length]
            ));
            colorIdx++;
        }

        final Map<String, Integer> lMap = new LinkedHashMap<>();
        for (final String loc : variationLocation.values())
        {
            lMap.put(loc, lMap.getOrDefault(loc, 0) + 1);
        }
        final List<DistributionRatioDto> localeRatios = new ArrayList<>();
        final int lTotal = lMap.values().stream().mapToInt(Integer::intValue).sum();
        colorIdx = 0;
        for (final Map.Entry<String, Integer> entry : lMap.entrySet())
        {
            final double pct = lTotal > 0 ? ((double) entry.getValue() / lTotal * 100.0) : 0.0;
            localeRatios.add(new DistributionRatioDto(
                entry.getKey(),
                entry.getValue(),
                pct,
                palette[colorIdx % palette.length]
            ));
            colorIdx++;
        }

        final Map<String, Integer> eMap = new LinkedHashMap<>();
        for (final String env : variationEnvironment.values())
        {
            eMap.put(env, eMap.getOrDefault(env, 0) + 1);
        }
        final List<DistributionRatioDto> environmentRatios = new ArrayList<>();
        final int eTotal = eMap.values().stream().mapToInt(Integer::intValue).sum();
        colorIdx = 0;
        for (final Map.Entry<String, Integer> entry : eMap.entrySet())
        {
            final double pct = eTotal > 0 ? ((double) entry.getValue() / eTotal * 100.0) : 0.0;
            environmentRatios.add(new DistributionRatioDto(
                entry.getKey(),
                entry.getValue(),
                pct,
                palette[colorIdx % palette.length]
            ));
            colorIdx++;
        }

        final Map<String, Integer> aMap = new LinkedHashMap<>();
        for (final String ar : variationArea.values())
        {
            final String normAr = isRealArea(ar) ? normalizeArea(ar) : "Default";
            aMap.put(normAr, aMap.getOrDefault(normAr, 0) + 1);
        }
        final List<DistributionRatioDto> areaRatios = new ArrayList<>();
        final int aTotal = aMap.values().stream().mapToInt(Integer::intValue).sum();
        colorIdx = 0;
        for (final Map.Entry<String, Integer> entry : aMap.entrySet())
        {
            final double pct = aTotal > 0 ? ((double) entry.getValue() / aTotal * 100.0) : 0.0;
            areaRatios.add(new DistributionRatioDto(
                entry.getKey(),
                entry.getValue(),
                pct,
                palette[colorIdx % palette.length]
            ));
            colorIdx++;
        }

        final Map<String, Integer> tagMap = new LinkedHashMap<>();
        for (final String tg : variationBatch.values())
        {
            tagMap.put(tg, tagMap.getOrDefault(tg, 0) + 1);
        }
        final List<DistributionRatioDto> tagRatios = new ArrayList<>();
        final int tagTotal = tagMap.values().stream().mapToInt(Integer::intValue).sum();
        colorIdx = 0;
        for (final Map.Entry<String, Integer> entry : tagMap.entrySet())
        {
            final double pct = tagTotal > 0 ? ((double) entry.getValue() / tagTotal * 100.0) : 0.0;
            tagRatios.add(new DistributionRatioDto(
                entry.getKey(),
                entry.getValue(),
                pct,
                palette[colorIdx % palette.length]
            ));
            colorIdx++;
        }

        final Map<String, TestRunEntity> eligibleRunMap = new LinkedHashMap<>();
        for (final TestRunEntity r : eligibleRuns)
        {
            if (r.getId() != null)
            {
                eligibleRunMap.put(r.getId(), r);
            }
        }

        populateRunTags(eligibleRuns);

        final Set<String> discoveredTagKeys = new LinkedHashSet<>(getAllTagKeys());
        for (final TestRunEntity r : eligibleRuns)
        {
            if (r.getTags() != null)
            {
                for (final TestRunTagEntity t : r.getTags())
                {
                    if (t.getTagKey() != null && !t.getTagKey().isBlank())
                    {
                        discoveredTagKeys.add(t.getTagKey().trim());
                    }
                }
            }
        }
        for (final Map<String, List<String>> vTags : variationTags.values())
        {
            if (vTags != null)
            {
                for (final String vKey : vTags.keySet())
                {
                    if (vKey != null && !vKey.isBlank())
                    {
                        discoveredTagKeys.add(vKey.trim());
                    }
                }
            }
        }
        final Set<String> builtInKeywords = Set.of("browser", "browsers", "none", "all");
        final List<String> distinctTagKeys = discoveredTagKeys.stream()
            .filter(k -> k != null && !k.isBlank() && !builtInKeywords.contains(k.toLowerCase(Locale.ROOT)))
            .collect(Collectors.toList());

        final Map<String, Boolean> isBoolByTag = new HashMap<>();
        for (final String tagKey : distinctTagKeys)
        {
            isBoolByTag.put(tagKey, isBooleanTag(tagKey, eligibleRuns));
        }

        final Map<String, List<DistributionRatioDto>> tagRatiosByType = new LinkedHashMap<>();
        for (final String tagKey : distinctTagKeys)
        {
            final Map<String, Integer> valCounts = new LinkedHashMap<>();
            final boolean isBool = Boolean.TRUE.equals(isBoolByTag.get(tagKey));
            for (final String key : variationStatus.keySet())
            {
                final String rId = variationRunId.get(key);
                final TestRunEntity varRun = (rId != null) ? eligibleRunMap.get(rId) : null;
                final Map<String, List<String>> vTags = variationTags.get(key);
                final List<String> execTagVals = (vTags != null) ? TagUtils.findTagValues(vTags, tagKey) : Collections.emptyList();
                final String effectiveVal;
                if (isBool)
                {
                    if (!execTagVals.isEmpty())
                    {
                        final boolean hasTrue = execTagVals.stream().anyMatch(v -> "true".equalsIgnoreCase(v) || tagKey.equalsIgnoreCase(v));
                        effectiveVal = hasTrue ? tagKey : "Not " + tagKey;
                    }
                    else
                    {
                        effectiveVal = (varRun != null && matchesTagCriteria(varRun, tagKey, Set.of(tagKey, "true")))
                            ? tagKey
                            : "Not " + tagKey;
                    }
                }
                else
                {
                    if (!execTagVals.isEmpty())
                    {
                        effectiveVal = execTagVals.get(0);
                    }
                    else
                    {
                        final List<String> vals = (varRun != null) ? getRunTagValues(varRun, tagKey) : Collections.emptyList();
                        if (!vals.isEmpty())
                        {
                            effectiveVal = vals.get(0);
                        }
                        else if ("locale".equalsIgnoreCase(tagKey) && variationLocation.containsKey(key))
                        {
                            effectiveVal = variationLocation.get(key);
                        }
                        else if ("area".equalsIgnoreCase(tagKey) && variationArea.containsKey(key))
                        {
                            effectiveVal = variationArea.get(key);
                        }
                        else
                        {
                            effectiveVal = "Default";
                        }
                    }
                }
                valCounts.put(effectiveVal, valCounts.getOrDefault(effectiveVal, 0) + 1);
            }
            final List<DistributionRatioDto> ratios = new ArrayList<>();
            final int tTotal = valCounts.values().stream().mapToInt(Integer::intValue).sum();
            int cIdx = 0;
            for (final Map.Entry<String, Integer> entry : valCounts.entrySet())
            {
                final double pct = tTotal > 0 ? ((double) entry.getValue() / tTotal * 100.0) : 0.0;
                ratios.add(new DistributionRatioDto(
                    entry.getKey(),
                    entry.getValue(),
                    pct,
                    palette[cIdx % palette.length]
                ));
                cIdx++;
            }
            tagRatiosByType.put(tagKey, ratios);
        }
        if (!tagRatiosByType.containsKey("batch") && !tagRatios.isEmpty())
        {
            tagRatiosByType.put("batch", tagRatios);
        }

        final List<BatchLatestVariationDto> latestVariations = new ArrayList<>();
        for (final String key : variationStatus.keySet())
        {
            final String rId = variationRunId.get(key);
            final TestRunEntity varRun = (rId != null) ? eligibleRunMap.get(rId) : null;
            final Map<String, List<String>> vTags = variationTags.get(key);
            final Map<String, String> varTags = new LinkedHashMap<>();
            for (final String tagKey : distinctTagKeys)
            {
                final boolean isBool = Boolean.TRUE.equals(isBoolByTag.get(tagKey));
                final List<String> execTagVals = (vTags != null) ? TagUtils.findTagValues(vTags, tagKey) : Collections.emptyList();
                if (isBool)
                {
                    final String val;
                    if (!execTagVals.isEmpty())
                    {
                        final boolean hasTrue = execTagVals.stream().anyMatch(v -> "true".equalsIgnoreCase(v) || tagKey.equalsIgnoreCase(v));
                        val = hasTrue ? tagKey : "Not " + tagKey;
                    }
                    else
                    {
                        val = (varRun != null && matchesTagCriteria(varRun, tagKey, Set.of(tagKey, "true")))
                            ? tagKey
                            : "Not " + tagKey;
                    }
                    varTags.put(tagKey, val);
                }
                else
                {
                    if (!execTagVals.isEmpty())
                    {
                        varTags.put(tagKey, execTagVals.get(0));
                    }
                    else
                    {
                        final List<String> vals = (varRun != null) ? getRunTagValues(varRun, tagKey) : Collections.emptyList();
                        if (!vals.isEmpty())
                        {
                            varTags.put(tagKey, vals.get(0));
                        }
                        else if ("locale".equalsIgnoreCase(tagKey) && variationLocation.containsKey(key))
                        {
                            varTags.put(tagKey, variationLocation.get(key));
                        }
                        else if ("area".equalsIgnoreCase(tagKey) && variationArea.containsKey(key))
                        {
                            varTags.put(tagKey, variationArea.get(key));
                        }
                        else
                        {
                            varTags.put(tagKey, "Default");
                        }
                    }
                }
            }

            latestVariations.add(new BatchLatestVariationDto(
                key,
                variationCanonicalId.getOrDefault(key, key),
                variationStatus.get(key),
                Boolean.TRUE.equals(variationHealed.get(key)),
                variationMode.getOrDefault(key, "JAVA"),
                rId,
                variationEnvironment.getOrDefault(key, "Default"),
                variationBrowser.getOrDefault(key, "Default"),
                variationLocation.getOrDefault(key, "Default"),
                variationArea.getOrDefault(key, "Default"),
                variationBatch.getOrDefault(key, "Default"),
                varTags,
                variationLlmCost.getOrDefault(key, 0.0),
                variationLlmTokens.getOrDefault(key, 0L),
                variationExecutionId.get(key),
                variationBugs.getOrDefault(key, Collections.emptyList()),
                variationBugSeverities.getOrDefault(key, Collections.emptyList()),
                Boolean.TRUE.equals(variationFlaky.get(key)),
                variationFlakyFailureCount.getOrDefault(key, 0),
                variationFlakyTotalRuns.getOrDefault(key, 0),
                variationFlakyReason.getOrDefault(key, "")
            ));
        }

        double latestTotalLlmCost = 0.0;
        long latestTotalLlmTokens = 0L;
        for (final String key : variationStatus.keySet())
        {
            latestTotalLlmCost += variationLlmCost.getOrDefault(key, 0.0);
            latestTotalLlmTokens += variationLlmTokens.getOrDefault(key, 0L);
        }

        // Group eligible runs by calendar date (chronologically ascending)
        final Map<LocalDate, List<TestRunEntity>> runsByDate = new TreeMap<>();
        for (final TestRunEntity r : eligibleRuns)
        {
            final LocalDate d = resolveRunDate(r);
            runsByDate.computeIfAbsent(d, k -> new ArrayList<>()).add(r);
        }

        final List<TokenCostPointGroupDto> pointGroups = new ArrayList<>();
        final List<Double> trendPoints = new ArrayList<>();
        double prevDayCost = -1.0;

        int dayIdx = 0;
        final int totalDays = runsByDate.size();

        for (final Map.Entry<LocalDate, List<TestRunEntity>> entry : runsByDate.entrySet())
        {
            final LocalDate dayDate = entry.getKey();
            final List<TestRunEntity> dayRuns = entry.getValue();

            // Sort runs on this day chronologically
            dayRuns.sort((a, b) -> Long.compare(
                a.getStartTimeMs() != null ? a.getStartTimeMs() : 0L,
                b.getStartTimeMs() != null ? b.getStartTimeMs() : 0L));

            double dayTotalCost = 0.0;
            long dayTotalTokens = 0L;
            final List<ContributingRunCostDto> dayContribRuns = new ArrayList<>();

            for (final TestRunEntity r : dayRuns)
            {
                final List<RunVariationCost> variations = runVariationsMap.get(r.getId());
                double runCost = 0.0;
                long runTokens = 0L;
                int runTests = 0;
                if (variations != null)
                {
                    runTests = variations.size();
                    for (final RunVariationCost v : variations)
                    {
                        runCost += v.llmCost();
                        runTokens += v.llmTokens();
                    }
                }
                else
                {
                    runTests = (r.getTotalTests() != null) ? r.getTotalTests() : 0;
                }

                dayTotalCost += runCost;
                dayTotalTokens += runTokens;

                dayContribRuns.add(new ContributingRunCostDto(
                    r.getId(),
                    r.getTimestampLabel(),
                    r.getEnvironment(),
                    r.getBrowsersCsv(),
                    r.getBatchName(),
                    runTests,
                    runCost,
                    runTokens,
                    0.0
                ));
            }

            final List<ContributingRunCostDto> finalizedRuns = new ArrayList<>();
            for (final ContributingRunCostDto cr : dayContribRuns)
            {
                final double pct = (dayTotalCost > 0.0) ? (cr.getContributedCost() / dayTotalCost * 100.0) : 0.0;
                finalizedRuns.add(new ContributingRunCostDto(
                    cr.getRunId(),
                    cr.getTimestampLabel(),
                    cr.getEnvironment(),
                    cr.getBrowser(),
                    cr.getBatchName(),
                    cr.getContributedTestCount(),
                    cr.getContributedCost(),
                    cr.getContributedTokens(),
                    pct
                ));
            }

            final double deltaCost;
            final double deltaPercentage;
            if (prevDayCost >= 0.0)
            {
                deltaCost = dayTotalCost - prevDayCost;
                if (prevDayCost > 0.0)
                {
                    deltaPercentage = (deltaCost / prevDayCost) * 100.0;
                }
                else
                {
                    deltaPercentage = (deltaCost > 0.0) ? 100.0 : 0.0;
                }
            }
            else
            {
                deltaCost = 0.0;
                deltaPercentage = 0.0;
            }

            final boolean isLatestDay = (dayIdx == totalDays - 1);
            final String dayDateLabel = dayDate.toString();
            final String anchorRunId = !dayRuns.isEmpty() ? dayRuns.get(dayRuns.size() - 1).getId() : "day-" + dayDateLabel;
            final String pointLabel = String.format(Locale.ROOT, "Day %d of %d%s — %s (%d %s)",
                dayIdx + 1, totalDays, isLatestDay ? " (Latest)" : "", dayDateLabel, dayRuns.size(), dayRuns.size() == 1 ? "run" : "runs");

            pointGroups.add(new TokenCostPointGroupDto(
                dayIdx,
                pointLabel,
                anchorRunId,
                dayDateLabel,
                dayTotalCost,
                dayTotalTokens,
                deltaCost,
                deltaPercentage,
                finalizedRuns
            ));

            trendPoints.add(dayTotalCost);
            prevDayCost = dayTotalCost;
            dayIdx++;
        }

        final double currentDailyCost = !trendPoints.isEmpty() ? trendPoints.get(trendPoints.size() - 1) : 0.0;
        final long currentDailyTokens = !pointGroups.isEmpty() ? pointGroups.get(pointGroups.size() - 1).getTotalTokens() : 0L;
        final double previousDayCost = trendPoints.size() >= 2 ? trendPoints.get(trendPoints.size() - 2) : -1.0;

        final TokenCostTrendDto tokenCostTrend = new TokenCostTrendDto(
            currentDailyCost,
            previousDayCost,
            currentDailyTokens,
            trendPoints,
            pointGroups
        );

        final List<TestRunEntity> chronologicalRuns = new ArrayList<>(eligibleRuns);
        Collections.reverse(chronologicalRuns);

        final Map<String, VariationRunState> cumulativeSuiteState = new LinkedHashMap<>();
        final Map<String, Map<String, List<String>>> cumulativeVariationTags = new HashMap<>();
        final List<BatchSuiteEvolutionPointDto> evolutionPoints = new ArrayList<>();

        int runIdx = 0;
        for (final TestRunEntity r : chronologicalRuns)
        {
            final Map<String, VariationRunState> runExecs = runExecutionsMap.getOrDefault(r.getId(), Collections.emptyMap());
            final Map<String, Map<String, List<String>>> runVarTags = runVariationTagsMap.getOrDefault(r.getId(), Collections.emptyMap());
            int newlyIntroducedCount = 0;

            for (final Map.Entry<String, VariationRunState> entry : runExecs.entrySet())
            {
                final String vKey = entry.getKey();
                if (!cumulativeSuiteState.containsKey(vKey))
                {
                    newlyIntroducedCount++;
                }
                cumulativeSuiteState.put(vKey, entry.getValue());
                final Map<String, List<String>> vTags = runVarTags.get(vKey);
                if (vTags != null)
                {
                    cumulativeVariationTags.put(vKey, vTags);
                }
            }

            int pClean = 0;
            int pHealed = 0;
            int pAi = 0;
            int fClean = 0;
            int fHealed = 0;
            int fAi = 0;
            int kClean = 0;
            int kHealed = 0;
            int kAi = 0;
            int uClean = 0;
            int uHealed = 0;
            int uAi = 0;
            int ign = 0;
            int pPlaybook = 0;
            int flakyCumulative = 0;

            for (final VariationRunState state : cumulativeSuiteState.values())
            {
                if (state.flaky())
                {
                    flakyCumulative++;
                }

                final String st = state.status();
                final boolean isHealed = state.healed();
                final boolean isAi = "AI".equalsIgnoreCase(state.mode());
                final boolean isPlaybook = ("RECORDING".equalsIgnoreCase(state.mode()) || "PLAYBOOK".equalsIgnoreCase(state.mode())) && !isHealed;

                if (isPlaybook)
                {
                    pPlaybook++;
                }

                if ("passed-clean".equalsIgnoreCase(st) || "passed".equalsIgnoreCase(st) || "succeeded".equalsIgnoreCase(st))
                {
                    if (isHealed)
                    {
                        pHealed++;
                    }
                    else if (isAi)
                    {
                        pAi++;
                    }
                    else
                    {
                        pClean++;
                    }
                }
                else if ("succeeded-fixed".equalsIgnoreCase(st) || "fixed".equalsIgnoreCase(st))
                {
                    if (isHealed)
                    {
                        fHealed++;
                    }
                    else if (isAi)
                    {
                        fAi++;
                    }
                    else
                    {
                        fClean++;
                    }
                }
                else if ("failed-known".equalsIgnoreCase(st) || "known".equalsIgnoreCase(st))
                {
                    if (isHealed)
                    {
                        kHealed++;
                    }
                    else if (isAi)
                    {
                        kAi++;
                    }
                    else
                    {
                        kClean++;
                    }
                }
                else if ("failed-unknown".equalsIgnoreCase(st) || "failed".equalsIgnoreCase(st) || "unknown".equalsIgnoreCase(st) || "error".equalsIgnoreCase(st))
                {
                    if (isHealed)
                    {
                        uHealed++;
                    }
                    else if (isAi)
                    {
                        uAi++;
                    }
                    else
                    {
                        uClean++;
                    }
                }
                else if ("ignored".equalsIgnoreCase(st) || "skipped".equalsIgnoreCase(st))
                {
                    ign++;
                }
                else
                {
                    pClean++;
                }
            }

            final int currentKnownTotal = cumulativeSuiteState.size();
            final int passedTotal = pClean + pHealed + pAi + fClean + fHealed + fAi;
            final double passRate = (currentKnownTotal > 0) ? (passedTotal * 100.0 / currentKnownTotal) : 0.0;
            final String tsLabel = (r.getTimestampLabel() != null && !r.getTimestampLabel().isBlank())
                ? r.getTimestampLabel()
                : ("Run " + (runIdx + 1));

            final Map<String, Map<String, Integer>> tagCountsByType = new LinkedHashMap<>();
            for (final String vKey : cumulativeSuiteState.keySet())
            {
                final String areaVal = varAreaMap.getOrDefault(vKey, "Default");
                tagCountsByType.computeIfAbsent("area", k -> new LinkedHashMap<>()).merge(areaVal, 1, Integer::sum);

                final String browserVal = varBrowserMap.getOrDefault(vKey, "Default");
                tagCountsByType.computeIfAbsent("browser", k -> new LinkedHashMap<>()).merge(browserVal, 1, Integer::sum);

                final String locVal = varLocationMap.getOrDefault(vKey, "Default");
                tagCountsByType.computeIfAbsent("locale", k -> new LinkedHashMap<>()).merge(locVal, 1, Integer::sum);
                tagCountsByType.computeIfAbsent("location", k -> new LinkedHashMap<>()).merge(locVal, 1, Integer::sum);

                final String envVal = varEnvMap.getOrDefault(vKey, "Default");
                tagCountsByType.computeIfAbsent("environment", k -> new LinkedHashMap<>()).merge(envVal, 1, Integer::sum);
                tagCountsByType.computeIfAbsent("env", k -> new LinkedHashMap<>()).merge(envVal, 1, Integer::sum);

                Map<String, List<String>> cTags = cumulativeVariationTags.get(vKey);
                if (cTags == null || cTags.isEmpty())
                {
                    cTags = varTagsMap.get(vKey);
                }
                if (cTags != null && !cTags.isEmpty())
                {
                    for (final Map.Entry<String, List<String>> tEntry : cTags.entrySet())
                    {
                        final String tKey = tEntry.getKey().toLowerCase(Locale.ROOT);
                        if ("area".equals(tKey) || "browser".equals(tKey) || "locale".equals(tKey)
                            || "location".equals(tKey) || "environment".equals(tKey) || "env".equals(tKey))
                        {
                            continue;
                        }
                        if (tEntry.getValue() != null)
                        {
                            for (final String tVal : tEntry.getValue())
                            {
                                if (tVal != null && !tVal.isBlank())
                                {
                                    tagCountsByType.computeIfAbsent(tKey, k -> new LinkedHashMap<>()).merge(tVal.trim(), 1, Integer::sum);
                                }
                            }
                        }
                    }
                }
            }

            evolutionPoints.add(new BatchSuiteEvolutionPointDto(
                r.getId(),
                runIdx,
                tsLabel,
                currentKnownTotal,
                newlyIntroducedCount,
                pClean,
                pHealed,
                pAi,
                fClean,
                fHealed,
                fAi,
                kClean,
                kHealed,
                kAi,
                uClean,
                uHealed,
                uAi,
                ign,
                pPlaybook,
                passRate,
                flakyCumulative,
                tagCountsByType
            ));
            runIdx++;
        }

        final BatchSuiteEvolutionDto suiteEvolution = new BatchSuiteEvolutionDto(evolutionPoints, effectiveAsOfRunId);

        return new BatchLatestExecutionsDto(summary, contributingRuns, browserRatios, localeRatios, environmentRatios, areaRatios, tagRatios, tagRatiosByType, latestVariations, variationStatus.size(), eligibleRuns.size(), eligibleRuns, tokenCostTrend, suiteEvolution, effectiveAsOfRunId, asOfTimestampLabel, isHistoricalSnapshot);
    }

    private LocalDate resolveRunDate(final TestRunEntity run)
    {
        if (run == null)
        {
            return LocalDate.now();
        }
        if (run.getStartTimeMs() != null && run.getStartTimeMs() > 0L)
        {
            return Instant.ofEpochMilli(run.getStartTimeMs()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (run.getTimestampLabel() != null && run.getTimestampLabel().trim().length() >= 10)
        {
            final String raw = run.getTimestampLabel().trim().substring(0, 10);
            try
            {
                return LocalDate.parse(raw);
            }
            catch (final Exception e)
            {
                // ignore and fallback
            }
        }
        return LocalDate.now();
    }

    private boolean matchesRunCriteria(
        final TestRunEntity run,
        final Set<String> runBrowsers,
        final Set<String> runLocales,
        final Set<String> runEnvs,
        final Set<String> runAreas,
        final Set<String> runTags,
        final Map<String, String> runAreasMap)
    {
        return matchesRunCriteria(
            run,
            runBrowsers,
            Collections.emptySet(),
            runLocales,
            Collections.emptySet(),
            runEnvs,
            Collections.emptySet(),
            runAreas,
            Collections.emptySet(),
            runTags,
            Collections.emptySet(),
            runAreasMap,
            Collections.emptyMap(),
            Collections.emptyMap()
        );
    }

    private boolean matchesRunCriteria(
        final TestRunEntity run,
        final Set<String> runBrowsers,
        final Set<String> runLocales,
        final Set<String> runEnvs,
        final Set<String> runAreas,
        final Set<String> runTags,
        final Map<String, String> runAreasMap,
        final Map<String, Set<String>> runTagsByType)
    {
        return matchesRunCriteria(
            run,
            runBrowsers,
            Collections.emptySet(),
            runLocales,
            Collections.emptySet(),
            runEnvs,
            Collections.emptySet(),
            runAreas,
            Collections.emptySet(),
            runTags,
            Collections.emptySet(),
            runAreasMap,
            runTagsByType,
            Collections.emptyMap()
        );
    }

    private boolean matchesRunCriteria(
        final TestRunEntity run,
        final Set<String> runBrowsers,
        final Set<String> excludedRunBrowsers,
        final Set<String> runLocales,
        final Set<String> excludedRunLocales,
        final Set<String> runEnvs,
        final Set<String> excludedRunEnvs,
        final Set<String> runAreas,
        final Set<String> excludedRunAreas,
        final Set<String> runTags,
        final Set<String> excludedRunTags,
        final Map<String, String> runAreasMap,
        final Map<String, Set<String>> runTagsByType,
        final Map<String, Set<String>> excludedRunTagsByType)
    {
        if (runBrowsers != null && runBrowsers.contains("_none_"))
        {
            return false;
        }
        if (runLocales != null && runLocales.contains("_none_"))
        {
            return false;
        }
        if (runEnvs != null && runEnvs.contains("_none_"))
        {
            return false;
        }
        if (runAreas != null && runAreas.contains("_none_"))
        {
            return false;
        }
        if (runTags != null && runTags.contains("_none_"))
        {
            return false;
        }

        if (runBrowsers != null && !runBrowsers.isEmpty())
        {
            final List<String> rBrowsers = (run.getBrowsersCsv() != null && !run.getBrowsersCsv().isBlank())
                ? Arrays.stream(run.getBrowsersCsv().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
                : List.of("Chrome");
            final boolean match = rBrowsers.stream().anyMatch(b ->
                runBrowsers.stream().anyMatch(rb -> rb.equalsIgnoreCase(b) || rb.equalsIgnoreCase(normalizeBrowser(b)) || normalizeBrowser(rb).equalsIgnoreCase(normalizeBrowser(b)))
            );
            if (!match)
            {
                return false;
            }
        }
        if (excludedRunBrowsers != null && !excludedRunBrowsers.isEmpty())
        {
            final List<String> rBrowsers = (run.getBrowsersCsv() != null && !run.getBrowsersCsv().isBlank())
                ? Arrays.stream(run.getBrowsersCsv().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
                : List.of("Chrome");
            final boolean allExcluded = rBrowsers.stream().allMatch(b ->
                excludedRunBrowsers.stream().anyMatch(eb -> eb.equalsIgnoreCase(b) || eb.equalsIgnoreCase(normalizeBrowser(b)) || normalizeBrowser(eb).equalsIgnoreCase(normalizeBrowser(b)))
            );
            if (allExcluded)
            {
                return false;
            }
        }

        if (runLocales != null && !runLocales.isEmpty())
        {
            final List<String> rLocales = (run.getLocalesCsv() != null && !run.getLocalesCsv().isBlank())
                ? Arrays.stream(run.getLocalesCsv().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
                : List.of("Default");
            final boolean match = rLocales.stream().anyMatch(l ->
                runLocales.stream().anyMatch(rl -> rl.equalsIgnoreCase(l) || rl.equalsIgnoreCase(normalizeLocation(l)) || normalizeLocation(rl).equalsIgnoreCase(normalizeLocation(l)))
            );
            if (!match)
            {
                return false;
            }
        }
        if (excludedRunLocales != null && !excludedRunLocales.isEmpty())
        {
            final List<String> rLocales = (run.getLocalesCsv() != null && !run.getLocalesCsv().isBlank())
                ? Arrays.stream(run.getLocalesCsv().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
                : List.of("Default");
            final boolean allExcluded = rLocales.stream().allMatch(l ->
                excludedRunLocales.stream().anyMatch(el -> el.equalsIgnoreCase(l) || el.equalsIgnoreCase(normalizeLocation(l)) || normalizeLocation(el).equalsIgnoreCase(normalizeLocation(l)))
            );
            if (allExcluded)
            {
                return false;
            }
        }

        if (runEnvs != null && !runEnvs.isEmpty())
        {
            final String rEnv = (run.getEnvironment() != null && !run.getEnvironment().isBlank())
                ? run.getEnvironment().trim()
                : "Default";
            final boolean match = runEnvs.stream().anyMatch(re -> re.equalsIgnoreCase(rEnv));
            if (!match)
            {
                return false;
            }
        }
        if (excludedRunEnvs != null && !excludedRunEnvs.isEmpty())
        {
            final String rEnv = (run.getEnvironment() != null && !run.getEnvironment().isBlank())
                ? run.getEnvironment().trim()
                : "Default";
            final boolean match = excludedRunEnvs.stream().anyMatch(re -> re.equalsIgnoreCase(rEnv));
            if (match)
            {
                return false;
            }
        }

        if (runTags != null && !runTags.isEmpty())
        {
            final String rBatch = (run.getBatchName() != null && !run.getBatchName().isBlank())
                ? run.getBatchName().trim()
                : "Default";
            final boolean match = runTags.stream().anyMatch(rt -> rt.equalsIgnoreCase(rBatch));
            if (!match)
            {
                return false;
            }
        }
        if (excludedRunTags != null && !excludedRunTags.isEmpty())
        {
            final String rBatch = (run.getBatchName() != null && !run.getBatchName().isBlank())
                ? run.getBatchName().trim()
                : "Default";
            final boolean match = excludedRunTags.stream().anyMatch(rt -> rt.equalsIgnoreCase(rBatch));
            if (match)
            {
                return false;
            }
        }

        if (runTagsByType != null && !runTagsByType.isEmpty())
        {
            for (final Map.Entry<String, Set<String>> entry : runTagsByType.entrySet())
            {
                final String tagType = entry.getKey();
                final Set<String> selectedVals = entry.getValue();
                if (selectedVals == null || selectedVals.isEmpty())
                {
                    continue;
                }
                if (selectedVals.contains("_none_"))
                {
                    return false;
                }
                if (!matchesTagCriteria(run, tagType, selectedVals))
                {
                    return false;
                }
            }
        }
        if (excludedRunTagsByType != null && !excludedRunTagsByType.isEmpty())
        {
            for (final Map.Entry<String, Set<String>> entry : excludedRunTagsByType.entrySet())
            {
                final String tagType = entry.getKey();
                final Set<String> excludedVals = entry.getValue();
                if (excludedVals == null || excludedVals.isEmpty())
                {
                    continue;
                }
                if (isExcludedByTagCriteria(run, tagType, excludedVals))
                {
                    return false;
                }
            }
        }

        if (runAreas != null && !runAreas.isEmpty())
        {
            final String areasCsv = (runAreasMap != null) ? runAreasMap.get(run.getId()) : null;
            final List<String> rAreas;
            if (areasCsv != null && !areasCsv.isBlank())
            {
                rAreas = Arrays.stream(areasCsv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(AuraReportDataService::normalizeArea).collect(Collectors.toList());
            }
            else
            {
                rAreas = getRunAreas(run);
            }
            final boolean match = rAreas.stream().anyMatch(a ->
                runAreas.stream().anyMatch(ra -> normalizeArea(ra).equalsIgnoreCase(a))
            );
            if (!match)
            {
                return false;
            }
        }
        if (excludedRunAreas != null && !excludedRunAreas.isEmpty())
        {
            final String areasCsv = (runAreasMap != null) ? runAreasMap.get(run.getId()) : null;
            final List<String> rAreas;
            if (areasCsv != null && !areasCsv.isBlank())
            {
                rAreas = Arrays.stream(areasCsv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(AuraReportDataService::normalizeArea).collect(Collectors.toList());
            }
            else
            {
                rAreas = getRunAreas(run);
            }
            final boolean allExcluded = rAreas.stream().allMatch(a ->
                excludedRunAreas.stream().anyMatch(ea -> normalizeArea(ea).equalsIgnoreCase(a))
            );
            if (allExcluded)
            {
                return false;
            }
        }

        return true;
    }

    public List<String> getRunTagValues(final TestRunEntity run, final String tagKey)
    {
        if (run == null)
        {
            return Collections.emptyList();
        }
        final List<String> values = new ArrayList<>();
        if ("batch".equalsIgnoreCase(tagKey))
        {
            if (run.getBatchName() != null && !run.getBatchName().isBlank())
            {
                values.add(run.getBatchName().trim());
            }
        }
        else if ("environment".equalsIgnoreCase(tagKey) || "env".equalsIgnoreCase(tagKey))
        {
            if (run.getEnvironment() != null && !run.getEnvironment().isBlank())
            {
                values.add(run.getEnvironment().trim());
            }
        }

        if (run.getTags() != null && !run.getTags().isEmpty())
        {
            for (final TestRunTagEntity t : run.getTags())
            {
                if (t.getTagKey() != null && t.getTagKey().equalsIgnoreCase(tagKey))
                {
                    if (t.getTagValue() != null && !t.getTagValue().isBlank())
                    {
                        final String trimmed = t.getTagValue().trim();
                        if (!values.contains(trimmed))
                        {
                            values.add(trimmed);
                        }
                    }
                }
            }
            return values;
        }

        if (tagRepository != null && run.getId() != null)
        {
            final List<TestRunTagEntity> tags = tagRepository.findByRunId(run.getId());
            if (tags != null && !tags.isEmpty())
            {
                run.setTags(tags);
                for (final TestRunTagEntity t : tags)
                {
                    if (t.getTagKey() != null && t.getTagKey().equalsIgnoreCase(tagKey))
                    {
                        if (t.getTagValue() != null && !t.getTagValue().isBlank())
                        {
                            final String trimmed = t.getTagValue().trim();
                            if (!values.contains(trimmed))
                            {
                                values.add(trimmed);
                            }
                        }
                    }
                }
            }
        }
        return values;
    }

    public boolean isBooleanTag(final String tagKey)
    {
        return isBooleanTag(tagKey, Collections.emptyList());
    }

    public boolean isBooleanTag(final String tagKey, final Collection<TestRunEntity> runs)
    {
        if (tagKey == null || tagKey.isBlank())
        {
            return false;
        }
        final String cleanKey = tagKey.trim();
        final Set<String> distinctVals = new LinkedHashSet<>();
        if (tagRepository != null)
        {
            final List<String> dbVals = tagRepository.findDistinctTagValuesByTagKey(cleanKey);
            if (dbVals != null)
            {
                for (final String v : dbVals)
                {
                    if (v != null && !v.isBlank())
                    {
                        final String lower = v.trim().toLowerCase(Locale.ROOT);
                        distinctVals.add(lower);
                        if (!"true".equals(lower) && !"false".equals(lower))
                        {
                            return false;
                        }
                    }
                }
            }
        }
        if (distinctVals.isEmpty() && runs != null && !runs.isEmpty())
        {
            for (final TestRunEntity r : runs)
            {
                final List<String> rVals = getRunTagValues(r, cleanKey);
                for (final String v : rVals)
                {
                    if (v != null && !v.isBlank())
                    {
                        final String lower = v.trim().toLowerCase(Locale.ROOT);
                        distinctVals.add(lower);
                        if (!"true".equals(lower) && !"false".equals(lower))
                        {
                            return false;
                        }
                    }
                }
            }
        }
        if (distinctVals.isEmpty())
        {
            return false;
        }
        return distinctVals.stream().allMatch(v -> "true".equals(v) || "false".equals(v));
    }

    public boolean matchesTagCriteria(final TestRunEntity run, final String tagKey, final Set<String> targetValues)
    {
        if (run == null || tagKey == null || targetValues == null || targetValues.isEmpty())
        {
            return true;
        }
        if (targetValues.contains("_none_"))
        {
            return false;
        }
        final List<String> runTagVals = getRunTagValues(run, tagKey);
        final boolean isBool = isBooleanTag(tagKey, (run != null ? List.of(run) : Collections.emptyList()));

        if (isBool)
        {
            final boolean hasTrue = runTagVals.stream().anyMatch(v -> "true".equalsIgnoreCase(v) || tagKey.equalsIgnoreCase(v));
            final boolean matchesPositive = targetValues.stream().anyMatch(tv ->
                tagKey.equalsIgnoreCase(tv) || "true".equalsIgnoreCase(tv));
            final boolean matchesNegative = targetValues.stream().anyMatch(tv ->
                ("Not " + tagKey).equalsIgnoreCase(tv)
                || ("Not" + tagKey).equalsIgnoreCase(tv)
                || "false".equalsIgnoreCase(tv)
                || "Default".equalsIgnoreCase(tv));

            if (matchesPositive && matchesNegative)
            {
                return true;
            }
            if (matchesPositive)
            {
                return hasTrue;
            }
            if (matchesNegative)
            {
                return !hasTrue;
            }
            return false;
        }

        if (runTagVals.isEmpty())
        {
            return targetValues.stream().anyMatch("Default"::equalsIgnoreCase);
        }

        return runTagVals.stream().anyMatch(v ->
            targetValues.stream().anyMatch(tv -> tv.equalsIgnoreCase(v))
            || ("Default".equalsIgnoreCase(v) && targetValues.stream().anyMatch("Default"::equalsIgnoreCase))
        );
    }

    public boolean isExcludedByTagCriteria(final TestRunEntity run, final String tagKey, final Set<String> excludedValues)
    {
        if (run == null || tagKey == null || excludedValues == null || excludedValues.isEmpty())
        {
            return false;
        }
        return matchesTagCriteria(run, tagKey, excludedValues);
    }

    public List<String> getRunAreas(final TestRunEntity run)
    {
        if (run == null || run.getId() == null)
        {
            return List.of("Default");
        }
        if (run.getAreasCsv() != null && !run.getAreasCsv().isBlank())
        {
            return Arrays.stream(run.getAreasCsv().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AuraReportDataService::normalizeArea)
                .distinct()
                .collect(Collectors.toList());
        }
        final RunReportDto report = getRunReport(run.getId());
        if (report != null && report.getExecutions() != null && !report.getExecutions().isEmpty())
        {
            final List<String> areas = report.getExecutions().stream()
                .map(TestExecutionDto::getAreaName)
                .map(AuraReportDataService::normalizeArea)
                .distinct()
                .collect(Collectors.toList());
            if (!areas.isEmpty())
            {
                return areas;
            }
        }
        else if (report != null && report.getAreaSummaries() != null && !report.getAreaSummaries().isEmpty())
        {
            final List<String> areas = report.getAreaSummaries().stream()
                .map(AreaSummaryDto::getAreaName)
                .map(AuraReportDataService::normalizeArea)
                .distinct()
                .collect(Collectors.toList());
            if (!areas.isEmpty())
            {
                return areas;
            }
        }
        return List.of("Default");
    }

    public List<String> getRunAreas(final String runId)
    {
        if (runId == null || runId.isBlank())
        {
            return List.of("Default");
        }
        return runRepository.findById(runId)
            .map(this::getRunAreas)
            .orElseGet(() -> {
                final RunReportDto report = getRunReport(runId);
                if (report != null && report.getExecutions() != null && !report.getExecutions().isEmpty())
                {
                    return report.getExecutions().stream()
                        .map(TestExecutionDto::getAreaName)
                        .map(AuraReportDataService::normalizeArea)
                        .distinct()
                        .collect(Collectors.toList());
                }
                return List.of("Default");
            });
    }

    public RunReportDto getRunReport(final String runId)
    {
        if (runId != null && "#RUN_ID".equalsIgnoreCase(runId.trim()))
        {
            return new RunReportDto(
                "#RUN_ID", "Default", "N/A", "0s",
                0, 0, 0, 0, 0, 0,
                List.of(), List.of()
            );
        }

        final String effectiveRunId;
        if (runId != null && !runId.isEmpty())
        {
            effectiveRunId = runId;
        }
        else
        {
            effectiveRunId = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
                .findFirst()
                .map(TestRunEntity::getId)
                .orElse("");
        }

        if (effectiveRunId.isEmpty())
        {
            return new RunReportDto(
                "", "Default", "N/A", "0s",
                0, 0, 0, 0, 0, 0,
                List.of(), List.of()
            );
        }

        final Optional<TestRunEntity> runOpt = runRepository.findById(effectiveRunId);
        final boolean isActiveOnDisk = localRunJsonStorageService.isRunCurrentlyActiveOnDisk(effectiveRunId);
        final boolean isInProgress = (runOpt.isPresent() && "IN_PROGRESS".equalsIgnoreCase(runOpt.get().getStatus()))
                || liveRunBuffer.containsKey(effectiveRunId)
                || isActiveOnDisk;

        if (!isInProgress && runReportCache.containsKey(effectiveRunId))
        {
            LOG.debug("[PERF] getRunReport cache HIT for runId={}", effectiveRunId);
            return runReportCache.get(effectiveRunId);
        }

        synchronized (effectiveRunId.intern())
        {
            if (!isInProgress && runReportCache.containsKey(effectiveRunId))
            {
                LOG.debug("[PERF] getRunReport cache HIT (after sync lock) for runId={}", effectiveRunId);
                return runReportCache.get(effectiveRunId);
            }

            final long getReportStartMs = System.currentTimeMillis();

            final Path runDir = localRunJsonStorageService.getRunDir(effectiveRunId);
            final Path runJsonFile = (runDir != null) ? runDir.resolve("run.json") : null;
            if (runDir != null && Files.exists(runDir) && Files.isDirectory(runDir) && (isInProgress || runJsonFile == null || !Files.exists(runJsonFile)))
            {
                localRunJsonStorageService.buildRunJsonContent(runDir.toFile(), effectiveRunId, true, isInProgress);
            }

            final TestRunEntity runEntity = runOpt.orElseGet(() -> {
                final LocalRunJsonStorageService.BatchInfo batchInfo = (runDir != null && Files.exists(runDir))
                        ? localRunJsonStorageService.resolveOrCreateBatchJson(runDir.toFile(), null)
                        : new LocalRunJsonStorageService.BatchInfo("Default", "", "Default");
                return new TestRunEntity(
                    effectiveRunId,
                    batchInfo.name,
                    isInProgress ? "IN_PROGRESS" : "COMPLETED",
                    "Unknown",
                    batchInfo.environment,
                    "Default",
                    "Chrome",
                    "Recently",
                    System.currentTimeMillis()
                );
            });

            List<TestExecutionDto> rawExecutions = new ArrayList<>();
            final Optional<String> runJsonOpt = localRunJsonStorageService.readRunJson(effectiveRunId);

            if (runJsonOpt.isPresent())
            {
                try
                {
                    final JsonNode root = objectMapper.readTree(runJsonOpt.get());
                    final JsonNode execMetrics = root.path("executionMetrics");
                    if (execMetrics.isObject() && !execMetrics.isEmpty())
                    {
                        final List<TestExecutionDto> metricsList = new ArrayList<>();
                        final java.util.Set<String> seenIds = new java.util.HashSet<>();
                        execMetrics.fields().forEachRemaining(entry -> {
                            try
                            {
                                final JsonNode valueNode = entry.getValue();
                                if (valueNode instanceof ObjectNode obj)
                                {
                                    final JsonNode bNode = obj.path("browser");
                                    if (bNode.isMissingNode() || bNode.asText("").trim().isEmpty()
                                            || "Chrome".equalsIgnoreCase(bNode.asText().trim())
                                            || "Unknown".equalsIgnoreCase(bNode.asText().trim()))
                                    {
                                        final String fallbackBrowser = extractBrowserFromIdOrKey(entry.getKey());
                                        if (fallbackBrowser != null && !fallbackBrowser.isBlank() && !"Chrome".equalsIgnoreCase(fallbackBrowser))
                                        {
                                            obj.put("browser", fallbackBrowser);
                                        }
                                    }
                                }
                                final TestExecutionDto exec = objectMapper.treeToValue(valueNode, TestExecutionDto.class);
                                if (exec.getId() == null || exec.getId().isBlank() || seenIds.contains(exec.getId())
                                        || "bad".equalsIgnoreCase(exec.getId()) || "perfect".equalsIgnoreCase(exec.getId()) || "default".equalsIgnoreCase(exec.getId()))
                                {
                                    exec.setId(entry.getKey());
                                }
                                seenIds.add(exec.getId());
                                metricsList.add(exec);
                            }
                            catch (final Exception e)
                            {
                                LOG.warn("Failed to deserialize execution metric for key {}: {}", entry.getKey(), e.getMessage());
                            }
                        });
                        rawExecutions = metricsList;
                    }
                    else
                    {
                        final JsonNode execArray = root.path("executions");
                        if (execArray.isArray())
                        {
                            rawExecutions = objectMapper.readValue(
                                execArray.traverse(objectMapper),
                                objectMapper.getTypeFactory().constructCollectionType(List.class, TestExecutionDto.class)
                            );
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOG.error("Failed to parse run.json for runId {}: {}", effectiveRunId, e.getMessage());
                }
            }

            if (liveRunBuffer.containsKey(effectiveRunId))
            {
                final List<TestExecutionDto> liveList = liveRunBuffer.get(effectiveRunId);
                if (rawExecutions.isEmpty())
                {
                    rawExecutions = new ArrayList<>(liveList);
                }
                else if (liveList != null && !liveList.isEmpty())
                {
                    for (final TestExecutionDto liveExec : liveList)
                    {
                        boolean matched = false;
                        final String liveId = liveExec.getId();
                        final String liveVarId = generateVariationId(liveExec.getTestClass(), liveExec.getTestMethod(), liveExec.getTitle(), liveExec.getLocation(), liveExec.getBrowser());
                        final String liveVarIdNoMethod = generateVariationId(liveExec.getTestClass(), null, liveExec.getTitle(), liveExec.getLocation(), liveExec.getBrowser());
                        final String liveBrowser = normalizeBrowser(liveExec.getBrowser());
                        final String liveTitle = liveExec.getTitle() != null ? liveExec.getTitle().trim() : "";
                        final String liveLoc = normalizeLocation(liveExec.getLocation());

                        for (int idx = 0; idx < rawExecutions.size(); idx++)
                        {
                            final TestExecutionDto rawExec = rawExecutions.get(idx);
                            final boolean idMatch = (liveId != null && !liveId.isBlank() && !liveId.equalsIgnoreCase("default")
                                    && liveId.equalsIgnoreCase(rawExec.getId()));
                            final String rawVarId = generateVariationId(rawExec.getTestClass(), rawExec.getTestMethod(), rawExec.getTitle(), rawExec.getLocation(), rawExec.getBrowser());
                            final String rawVarIdNoMethod = generateVariationId(rawExec.getTestClass(), null, rawExec.getTitle(), rawExec.getLocation(), rawExec.getBrowser());
                            final boolean varIdMatch = (liveVarId != null && (liveVarId.equalsIgnoreCase(rawVarId) || liveVarId.equalsIgnoreCase(rawVarIdNoMethod)))
                                    || (liveVarIdNoMethod != null && (liveVarIdNoMethod.equalsIgnoreCase(rawVarId) || liveVarIdNoMethod.equalsIgnoreCase(rawVarIdNoMethod)));

                            final boolean sameClass = isSameTestClass(liveExec.getTestClass(), rawExec.getTestClass());
                            final boolean sameBrowser = liveBrowser.equalsIgnoreCase(normalizeBrowser(rawExec.getBrowser()));
                            final String rawTitle = rawExec.getTitle() != null ? rawExec.getTitle().trim() : "";
                            final String rawLoc = normalizeLocation(rawExec.getLocation());

                            final boolean sameTitleOrLocation = (liveTitle.equalsIgnoreCase(rawTitle) || liveTitle.isEmpty() || rawTitle.isEmpty() || "default".equalsIgnoreCase(liveTitle) || "default".equalsIgnoreCase(rawTitle))
                                    && (liveLoc.equalsIgnoreCase(rawLoc) || "default".equalsIgnoreCase(liveLoc) || "default".equalsIgnoreCase(rawLoc));

                            final boolean componentMatch = sameClass && sameBrowser && sameTitleOrLocation;

                            if (idMatch || varIdMatch || componentMatch)
                            {
                                if (liveExec.isFinished() || !rawExec.isFinished())
                                {
                                    rawExecutions.set(idx, liveExec);
                                }
                                matched = true;
                                break;
                            }
                        }
                        if (!matched)
                        {
                            rawExecutions.add(liveExec);
                        }
                    }
                }
            }

            final String runEnv = runEntity.getEnvironment() != null ? runEntity.getEnvironment() : "ALL";
            final List<TestExecutionDto> executions = new ArrayList<>();

            final Map<String, Long> runStartTimes = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
                .collect(Collectors.toMap(
                    TestRunEntity::getId,
                    r -> r.getStartTimeMs() != null ? r.getStartTimeMs() : 0L,
                    (a, b) -> a
                ));

            final String runBatch = runEntity.getBatchName() != null ? runEntity.getBatchName() : "ALL";
            final List<TestBaseBugEntity> allRunBugs = bugRepository.findByBatchNameInAndEnvironmentIn(List.of(runBatch, "ALL"), List.of(runEnv, "ALL"));

            final Map<String, List<TestBaseBugEntity>> activeBugsByVarId = allRunBugs.stream()
                .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(effectiveRunId, b.getLinkedRunId(), runStartTimes))
                          && (b.getRemovedRunId() == null || !isRunAtOrAfter(effectiveRunId, b.getRemovedRunId(), runStartTimes)))
                .collect(Collectors.groupingBy(TestBaseBugEntity::getVariationId));

            final Map<String, java.util.Set<String>> removedBugsByVarId = allRunBugs.stream()
                .filter(b -> b.getRemovedRunId() != null && isRunAtOrAfter(effectiveRunId, b.getRemovedRunId(), runStartTimes))
                .collect(Collectors.groupingBy(
                    TestBaseBugEntity::getVariationId,
                    Collectors.mapping(b -> normalizeTicket(b.getBugTicket()), Collectors.toSet())
                ));

            final Map<String, List<String>> runTagsMap = getRunTagsMap(runEntity);
            for (final TestExecutionDto exec : rawExecutions)
            {
                // Ensure healed steps count is computed and cached before omitting blocks
                exec.getHealedStepsCount();

                // Omit heavy step trees and screenshots on initial report opening (loaded on demand when clicked)
                exec.setSteps(null);
                exec.setBlocks(null);

                final String varId = generateVariationId(exec.getTestClass(), exec.getTestMethod(), exec.getTitle(), exec.getLocation(), exec.getBrowser());
                final String varIdNoMethod = generateVariationId(exec.getTestClass(), null, exec.getTitle(), exec.getLocation(), exec.getBrowser());
                final List<TestBaseBugEntity> candidateActiveBugs = new ArrayList<>(activeBugsByVarId.getOrDefault(varId, List.of()));
                for (final TestBaseBugEntity b : activeBugsByVarId.getOrDefault(varIdNoMethod, List.of()))
                {
                    if (!candidateActiveBugs.contains(b))
                    {
                        candidateActiveBugs.add(b);
                    }
                }

                final Set<String> removedTickets = new HashSet<>(removedBugsByVarId.getOrDefault(varId, Set.of()));
                removedTickets.addAll(removedBugsByVarId.getOrDefault(varIdNoMethod, Set.of()));

                applyBugsToExecution(exec, candidateActiveBugs, removedTickets, runEntity.getEnvironment(), runEntity.getBatchName(), runTagsMap);

                final boolean hasBugs = exec.getBugs() != null && !exec.getBugs().isEmpty();
                final String failureText = extractFailureText(exec);
                final String eff = computeEffectiveStatus(exec.getStatus(), exec.getTotalStepsCount(), exec.getFailedStepsCount(), hasBugs, failureText, isInProgress);
                exec.setStatus(eff);

                executions.add(exec);
            }

            annotateFlakyExecutions(effectiveRunId, runEntity, executions);

            final RunReportDto report = buildRunReportDto(runEntity, executions);
            if (!isInProgress)
            {
                runReportCache.put(effectiveRunId, report);
            }
            else
            {
                final TestRunEntity liveRun = runOpt.orElse(runEntity);
                liveRun.setStatus("IN_PROGRESS");
                liveRun.setTotalTests(report.getTotalCount());
                liveRun.setPassedCount(report.getPassCount());
                liveRun.setSucceededFixedCount(report.getFixedCount());
                liveRun.setFailedKnownCount(report.getKnownCount());
                liveRun.setFailedUnknownCount(report.getUnknownCount());
                liveRun.setIgnoredCount(report.getIgnoredCount());
                liveRun.setRunningCount(report.getRunningCount());
                liveRun.setPassedHealedCount(report.getPassHealedCount());
                liveRun.setSucceededFixedHealedCount(report.getFixedHealedCount());
                liveRun.setFailedKnownHealedCount(report.getKnownHealedCount());
                liveRun.setFailedUnknownHealedCount(report.getUnknownHealedCount());
                if (report.getExecutions() != null && !report.getExecutions().isEmpty())
                {
                    final String bCsv = report.getExecutions().stream()
                        .map(TestExecutionDto::getBrowser)
                        .filter(b -> b != null && !b.isBlank() && !"Unknown".equalsIgnoreCase(b))
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));
                    if (!bCsv.isBlank())
                    {
                        liveRun.setBrowsersCsv(bCsv);
                    }
                    final String lCsv = report.getExecutions().stream()
                        .map(TestExecutionDto::getLocation)
                        .filter(l -> l != null && !l.isBlank() && !"Unknown".equalsIgnoreCase(l))
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));
                    if (!lCsv.isBlank())
                    {
                        liveRun.setLocalesCsv(lCsv);
                    }
                }
                liveRun.recalculatePassRate();
                runRepository.save(liveRun);
            }

            final long getReportMs = System.currentTimeMillis() - getReportStartMs;
            LOG.info("[PERF] getRunReport cache MISS built report for runId={} with {} executions in {} ms",
                effectiveRunId, executions.size(), getReportMs);

            return report;
        }
    }

    private static class FlakyHistoryStats
    {
        private final Map<String, Integer> unknownFailures = new HashMap<>();
        private final Map<String, Integer> totalRuns = new HashMap<>();
    }

    /**
     * Aggregates execution history across previous completed runs within the configured window
     * to identify test variations with recurring unknown failures.
     *
     * @param excludeRunId run ID to exclude from history (e.g. current run), or null
     * @param beforeTimestampMs upper bound timestamp for previous runs
     * @param windowSize maximum number of previous completed runs to inspect
     * @return aggregated stats containing total runs and unknown failure counts per variation ID
     */
    private FlakyHistoryStats computeFlakyHistoryStats(final String excludeRunId, final Long beforeTimestampMs, final int windowSize)
    {
        final FlakyHistoryStats stats = new FlakyHistoryStats();
        final long maxTimestamp = (beforeTimestampMs != null && beforeTimestampMs > 0) ? beforeTimestampMs : Long.MAX_VALUE;

        final List<TestRunEntity> previousRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
            .filter(r -> excludeRunId == null || !r.getId().equals(excludeRunId))
            .filter(r -> r.getStartTimeMs() != null && r.getStartTimeMs() < maxTimestamp)
            .filter(r -> !"IN_PROGRESS".equalsIgnoreCase(r.getStatus()))
            .limit(windowSize > 0 ? windowSize : ReportDashboardSettingService.DEFAULT_FLAKY_HISTORY_WINDOW)
            .collect(Collectors.toList());

        if (previousRuns.isEmpty())
        {
            return stats;
        }

        final Map<String, Long> runStartTimes = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
            .collect(Collectors.toMap(
                TestRunEntity::getId,
                r -> r.getStartTimeMs() != null ? r.getStartTimeMs() : 0L,
                (a, b) -> a
            ));

        final List<TestBaseBugEntity> allBugs = bugRepository.findAll();

        for (final TestRunEntity prevRun : previousRuns)
        {
            final String prevRunId = prevRun.getId();
            final Set<String> runVariations = new HashSet<>();
            final Set<String> runUnknownFailed = new HashSet<>();

            final RunReportDto cachedReport = runReportCache.get(prevRunId);
            if (cachedReport != null && cachedReport.getExecutions() != null)
            {
                for (final TestExecutionDto prevExec : cachedReport.getExecutions())
                {
                    final String vId = generateVariationId(prevExec.getTestClass(), prevExec.getTestMethod(), prevExec.getTitle(), prevExec.getLocation(), prevExec.getBrowser());
                    final String vIdNoMethod = generateVariationId(prevExec.getTestClass(), null, prevExec.getTitle(), prevExec.getLocation(), prevExec.getBrowser());
                    runVariations.add(vId);
                    runVariations.add(vIdNoMethod);

                    final String status = prevExec.getStatus();
                    if ("failed-unknown".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status))
                    {
                        runUnknownFailed.add(vId);
                        runUnknownFailed.add(vIdNoMethod);
                    }
                }
            }
            else
            {
                final Optional<String> prevJsonOpt = localRunJsonStorageService.readRunJson(prevRunId);
                if (prevJsonOpt.isPresent())
                {
                    try
                    {
                        final JsonNode root = objectMapper.readTree(prevJsonOpt.get());
                        final JsonNode execMetrics = root.path("executionMetrics");
                        final List<TestExecutionDto> prevExecs = new ArrayList<>();
                        if (execMetrics.isObject() && !execMetrics.isEmpty())
                        {
                            execMetrics.fields().forEachRemaining(entry -> {
                                try
                                {
                                    final TestExecutionDto prevExec = objectMapper.treeToValue(entry.getValue(), TestExecutionDto.class);
                                    prevExecs.add(prevExec);
                                }
                                catch (final Exception ignored)
                                {
                                }
                            });
                        }
                        else
                        {
                            final JsonNode execArray = root.path("executions");
                            if (execArray.isArray())
                            {
                                prevExecs.addAll(objectMapper.readValue(
                                    execArray.traverse(objectMapper),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, TestExecutionDto.class)
                                ));
                            }
                        }

                        final String prevEnv = prevRun.getEnvironment() != null ? prevRun.getEnvironment() : "ALL";
                        final String prevBatch = prevRun.getBatchName() != null ? prevRun.getBatchName() : "ALL";

                        final Map<String, List<TestBaseBugEntity>> activeBugsByVarId = allBugs.stream()
                            .filter(b -> (b.getBatchName().equalsIgnoreCase("ALL") || b.getBatchName().equalsIgnoreCase(prevBatch))
                                      && (b.getEnvironment().equalsIgnoreCase("ALL") || b.getEnvironment().equalsIgnoreCase(prevEnv)))
                            .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(prevRunId, b.getLinkedRunId(), runStartTimes))
                                      && (b.getRemovedRunId() == null || !isRunAtOrAfter(prevRunId, b.getRemovedRunId(), runStartTimes)))
                            .collect(Collectors.groupingBy(TestBaseBugEntity::getVariationId));

                        final Map<String, Set<String>> removedBugsByVarId = allBugs.stream()
                            .filter(b -> (b.getBatchName().equalsIgnoreCase("ALL") || b.getBatchName().equalsIgnoreCase(prevBatch))
                                      && (b.getEnvironment().equalsIgnoreCase("ALL") || b.getEnvironment().equalsIgnoreCase(prevEnv)))
                            .filter(b -> b.getRemovedRunId() != null && isRunAtOrAfter(prevRunId, b.getRemovedRunId(), runStartTimes))
                            .collect(Collectors.groupingBy(
                                TestBaseBugEntity::getVariationId,
                                Collectors.mapping(b -> normalizeTicket(b.getBugTicket()), Collectors.toSet())
                            ));

                        final Map<String, List<String>> prevRunTags = getRunTagsMap(prevRun);

                        for (final TestExecutionDto prevExec : prevExecs)
                        {
                            final String vId = generateVariationId(prevExec.getTestClass(), prevExec.getTestMethod(), prevExec.getTitle(), prevExec.getLocation(), prevExec.getBrowser());
                            final String vIdNoMethod = generateVariationId(prevExec.getTestClass(), null, prevExec.getTitle(), prevExec.getLocation(), prevExec.getBrowser());

                            runVariations.add(vId);
                            runVariations.add(vIdNoMethod);

                            final List<TestBaseBugEntity> candidateActiveBugs = new ArrayList<>(activeBugsByVarId.getOrDefault(vId, List.of()));
                            for (final TestBaseBugEntity b : activeBugsByVarId.getOrDefault(vIdNoMethod, List.of()))
                            {
                                if (!candidateActiveBugs.contains(b))
                                {
                                    candidateActiveBugs.add(b);
                                }
                            }
                            final Set<String> removedTickets = new HashSet<>(removedBugsByVarId.getOrDefault(vId, Set.of()));
                            removedTickets.addAll(removedBugsByVarId.getOrDefault(vIdNoMethod, Set.of()));

                            applyBugsToExecution(prevExec, candidateActiveBugs, removedTickets, prevEnv, prevBatch, prevRunTags);

                            final boolean hasBugs = prevExec.getBugs() != null && !prevExec.getBugs().isEmpty();
                            final String rawStatus = prevExec.getStatus() != null ? prevExec.getStatus().toLowerCase() : "failed-unknown";
                            final boolean isFailed = rawStatus.contains("fail") || rawStatus.contains("error");

                            if (isFailed && !hasBugs)
                            {
                                runUnknownFailed.add(vId);
                                runUnknownFailed.add(vIdNoMethod);
                            }
                        }
                    }
                    catch (final Exception e)
                    {
                        LOG.warn("Failed to inspect previous run {} for flaky tests: {}", prevRunId, e.getMessage());
                    }
                }
            }

            for (final String vId : runVariations)
            {
                stats.totalRuns.put(vId, stats.totalRuns.getOrDefault(vId, 0) + 1);
            }
            for (final String vId : runUnknownFailed)
            {
                stats.unknownFailures.put(vId, stats.unknownFailures.getOrDefault(vId, 0) + 1);
            }
        }

        return stats;
    }

    /**
     * Evaluates historical unknown failure frequencies for test executions in the current run
     * and annotates flaky status, failure counts, and detailed reason messages.
     *
     * @param currentRunId current run ID
     * @param currentRunEntity current run entity (for start time and environment metadata)
     * @param executions executions list to annotate in-place
     */
    private void annotateFlakyExecutions(
        final String currentRunId,
        final TestRunEntity currentRunEntity,
        final List<TestExecutionDto> executions)
    {
        if (executions == null || executions.isEmpty())
        {
            return;
        }

        final int windowSize = (dashboardSettingService != null)
            ? dashboardSettingService.getFlakyHistoryWindow()
            : ReportDashboardSettingService.DEFAULT_FLAKY_HISTORY_WINDOW;
        final int minUnknownFailures = (dashboardSettingService != null)
            ? dashboardSettingService.getFlakyMinUnknownFailures()
            : ReportDashboardSettingService.DEFAULT_FLAKY_MIN_UNKNOWN_FAILURES;
        final double minFailureRate = (dashboardSettingService != null)
            ? dashboardSettingService.getFlakyMinFailureRate()
            : ReportDashboardSettingService.DEFAULT_FLAKY_MIN_FAILURE_RATE;

        final long currentRunStart = (currentRunEntity != null && currentRunEntity.getStartTimeMs() != null && currentRunEntity.getStartTimeMs() > 0)
            ? currentRunEntity.getStartTimeMs()
            : Long.MAX_VALUE;

        final FlakyHistoryStats stats = computeFlakyHistoryStats(currentRunId, currentRunStart, windowSize);

        for (final TestExecutionDto exec : executions)
        {
            final String vId = generateVariationId(exec.getTestClass(), exec.getTestMethod(), exec.getTitle(), exec.getLocation(), exec.getBrowser());
            final String vIdNoMethod = generateVariationId(exec.getTestClass(), null, exec.getTitle(), exec.getLocation(), exec.getBrowser());

            int failCount = stats.unknownFailures.getOrDefault(vId, 0);
            int totalRuns = stats.totalRuns.getOrDefault(vId, 0);
            if (totalRuns == 0 && !vId.equals(vIdNoMethod))
            {
                failCount = stats.unknownFailures.getOrDefault(vIdNoMethod, 0);
                totalRuns = stats.totalRuns.getOrDefault(vIdNoMethod, 0);
            }

            if (failCount >= minUnknownFailures && totalRuns > 0)
            {
                final double rate = (double) failCount / (double) totalRuns;
                if (rate >= minFailureRate)
                {
                    exec.setFlaky(true);
                    exec.setFlakyFailureCount(failCount);
                    exec.setFlakyTotalRuns(totalRuns);
                    final int pct = (int) Math.round(rate * 100.0);
                    exec.setFlakyReason(String.format(
                        "Flaky test: failed in %d of %d previous runs (%d%%) due to unknown failures",
                        failCount, totalRuns, pct));
                }
            }
        }
    }

    /**
     * Evaluates historical unknown failure frequencies for all variations in Test Base
     * and annotates flaky status, failure counts, and detailed reason messages.
     *
     * @param variations variations list to annotate in-place
     */
    private void annotateFlakyVariations(final List<TestBaseVariationEntity> variations)
    {
        if (variations == null || variations.isEmpty())
        {
            return;
        }

        final int windowSize = (dashboardSettingService != null)
            ? dashboardSettingService.getFlakyHistoryWindow()
            : ReportDashboardSettingService.DEFAULT_FLAKY_HISTORY_WINDOW;
        final int minUnknownFailures = (dashboardSettingService != null)
            ? dashboardSettingService.getFlakyMinUnknownFailures()
            : ReportDashboardSettingService.DEFAULT_FLAKY_MIN_UNKNOWN_FAILURES;
        final double minFailureRate = (dashboardSettingService != null)
            ? dashboardSettingService.getFlakyMinFailureRate()
            : ReportDashboardSettingService.DEFAULT_FLAKY_MIN_FAILURE_RATE;

        final FlakyHistoryStats stats = computeFlakyHistoryStats(null, Long.MAX_VALUE, windowSize);

        for (final TestBaseVariationEntity var : variations)
        {
            final String vId = var.getId();
            final int failCount = stats.unknownFailures.getOrDefault(vId, 0);
            final int totalRuns = stats.totalRuns.getOrDefault(vId, 0);

            final boolean isFlaky = (failCount >= minUnknownFailures && totalRuns > 0 && ((double) failCount / (double) totalRuns) >= minFailureRate);
            var.setFlaky(isFlaky);
            var.setFlakyFailureCount(failCount);
            var.setFlakyTotalRuns(totalRuns);
            if (isFlaky)
            {
                final int pct = (int) Math.round(((double) failCount / (double) totalRuns) * 100.0);
                var.setFlakyReason(String.format(
                    "Flaky test: failed in %d of %d previous runs (%d%%) due to unknown failures",
                    failCount, totalRuns, pct));
            }
        }
    }

    private JsonNode readJsonNodeWithRetry(final File f)
    {
        for (int attempt = 0; attempt < 3; attempt++)
        {
            try
            {
                final JsonNode node = objectMapper.readTree(f);
                if (node != null && !node.isEmpty())
                {
                    return node;
                }
            }
            catch (final Exception e)
            {
                if (attempt < 2)
                {
                    try
                    {
                        Thread.sleep(20L * (attempt + 1));
                    }
                    catch (final InterruptedException ignored)
                    {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return null;
    }

    public TestExecutionDto getExecutionDetails(final String runId, final String executionId)
    {
        if (runId == null || executionId == null || executionId.isBlank())
        {
            return null;
        }

        final String cleanRunId = runId.trim().replaceAll("^#+", "");
        final String effectiveRunId;
        if (!cleanRunId.isEmpty() && !"RUN_ID".equalsIgnoreCase(cleanRunId))
        {
            effectiveRunId = cleanRunId;
        }
        else
        {
            effectiveRunId = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
                .findFirst()
                .map(TestRunEntity::getId)
                .orElse("");
            LOG.warn("Execution lookup received placeholder runId='{}'; falling back to latest run '{}'.", runId, effectiveRunId);
        }

        if (effectiveRunId.isEmpty())
        {
            return null;
        }

        final Path runDir = localRunJsonStorageService.getRunDir(effectiveRunId);
        if (Files.exists(runDir) && Files.isDirectory(runDir))
        {
            final List<File> files = new ArrayList<>();
            localRunJsonStorageService.scanForTestExecJsonFiles(runDir.toFile(), files);

            // Pass 1: Scan for exact matches (FQCN keys, execution IDs, and specific file names). Simple-class-name
            // matches are gathered as ambiguity-checked candidates.
            final List<JsonNode> simpleKeyCandidates = new ArrayList<>();
            for (final File f : files)
            {
                final JsonNode node = readJsonNodeWithRetry(f);
                if (node != null)
                {
                    try
                    {
                        final String parentFolder = (f.getParentFile() != null && !f.getParentFile().getName().equals(effectiveRunId))
                                ? f.getParentFile().getName() : "";
                        final String rawTestFile = node.path("testFile").asText("");
                        final String testFileClass = rawTestFile.contains("#") ? rawTestFile.substring(0, rawTestFile.indexOf('#')) : rawTestFile;
                        final String testFileMethod = rawTestFile.contains("#") ? rawTestFile.substring(rawTestFile.indexOf('#') + 1) : "";

                        String testClass = node.path("testClass").asText("");
                        if (testClass.isEmpty() || "DefaultClass".equalsIgnoreCase(testClass))
                        {
                            if (!parentFolder.isEmpty())
                            {
                                testClass = parentFolder;
                            }
                            else if (!testFileClass.isEmpty())
                            {
                                testClass = testFileClass;
                            }
                            else
                            {
                                testClass = "DefaultClass";
                            }
                        }

                        String testMethod = LocalRunJsonStorageService.extractTestMethod(node);
                        if (testMethod.isEmpty() && !testFileMethod.isEmpty())
                        {
                            testMethod = testFileMethod;
                        }

                        String title = node.path("title").asText("");
                        if (title.isEmpty() || "Default".equalsIgnoreCase(title))
                        {
                            if (node.hasNonNull("datasetId"))
                            {
                                title = node.path("datasetId").asText();
                            }
                            else if (node.hasNonNull("dataSet"))
                            {
                                title = node.path("dataSet").asText();
                            }
                            else if (node.hasNonNull("testId"))
                            {
                                title = node.path("testId").asText();
                            }
                            else
                            {
                                title = "Default";
                            }
                        }

                        final String id = node.path("id").asText(f.getName().replaceAll("\\.json$", ""));
                        final String browser = node.path("browser").asText("Chrome");
                        final String nodeDatasetId = node.path("datasetId").asText("");
                        final String nodeTestId = node.path("testId").asText("");

                        final String simpleClass = testClass.contains(".") ? testClass.substring(testClass.lastIndexOf('.') + 1) : testClass;

                        final String execKey = LocalRunJsonStorageService.buildExecutionKey(testClass, testMethod, title, browser);
                        final String execKeyNoMethod = LocalRunJsonStorageService.buildExecutionKey(testClass, null, title, browser);
                        final String simpleExecKey = LocalRunJsonStorageService.buildExecutionKey(simpleClass, testMethod, title, browser);
                        final String fullKey = testClass + "#" + id + "#" + browser;
                        final String simpleFullKey = simpleClass + "#" + id + "#" + browser;
                        final String fullKeyDatasetId = !nodeDatasetId.isBlank() ? testClass + "#" + nodeDatasetId + "#" + browser : "";
                        final String fullKeyTestId = !nodeTestId.isBlank() ? testClass + "#" + nodeTestId + "#" + browser : "";
                        final String execKeyWithId = execKey + "#" + id;
                        final String simpleExecKeyWithId = simpleExecKey + "#" + id;
                        final String classMethodBrowser = testClass + "#" + testMethod + "#" + browser;
                        final String simpleClassMethodBrowser = simpleClass + "#" + testMethod + "#" + browser;
                        final String fileName = f.getName();
                        final String fileNameNoExt = fileName.replaceAll("\\.json$", "");
                        final String parentWithFileName = !parentFolder.isEmpty() ? parentFolder + "/" + fileName : "";
                        final String parentWithId = !parentFolder.isEmpty() ? parentFolder + "#" + id : "";

                        if (executionId.equalsIgnoreCase(id)
                                || executionId.equalsIgnoreCase(fullKey)
                                || (!fullKeyDatasetId.isEmpty() && executionId.equalsIgnoreCase(fullKeyDatasetId))
                                || (!fullKeyTestId.isEmpty() && executionId.equalsIgnoreCase(fullKeyTestId))
                                || executionId.equalsIgnoreCase(execKey)
                                || executionId.equalsIgnoreCase(execKeyNoMethod)
                                || executionId.equalsIgnoreCase(execKeyWithId)
                                || executionId.equalsIgnoreCase(classMethodBrowser)
                                || executionId.equalsIgnoreCase(fileNameNoExt)
                                || executionId.equalsIgnoreCase(fileName)
                                || (!parentWithFileName.isEmpty() && executionId.equalsIgnoreCase(parentWithFileName))
                                || (!parentWithId.isEmpty() && executionId.equalsIgnoreCase(parentWithId)))
                        {
                            return buildExecutionDetailsDto(node, executionId, effectiveRunId);
                        }

                        // Simple-class-name keys are only a weaker match: two classes in different packages may share a
                        // simple name, so such hits are collected and only used if they are unambiguous.
                        if (executionId.equalsIgnoreCase(simpleFullKey)
                                || executionId.equalsIgnoreCase(simpleExecKey)
                                || executionId.equalsIgnoreCase(simpleExecKeyWithId)
                                || executionId.equalsIgnoreCase(simpleClassMethodBrowser))
                        {
                            simpleKeyCandidates.add(node);
                        }
                    }
                    catch (final Exception e)
                    {
                        LOG.debug("Skipping execution file {} while resolving execId={}: {}", f.getName(), executionId, e.getMessage());
                    }
                }
            }

            if (simpleKeyCandidates.size() == 1)
            {
                try
                {
                    return buildExecutionDetailsDto(simpleKeyCandidates.get(0), executionId, effectiveRunId);
                }
                catch (final Exception e)
                {
                    LOG.debug("Failed mapping simple-key candidate for execId={}: {}", executionId, e.getMessage());
                }
            }
            else if (simpleKeyCandidates.size() > 1)
            {
                LOG.warn("Ambiguous simple-class execution key '{}' in runId={} matches {} executions; not guessing.",
                         executionId, effectiveRunId, simpleKeyCandidates.size());
            }

            // Pass 2: Fuzzy fallback matching. Only executed if no file in the run had an exact match in Pass 1.
            // Safely matches single-execution class folders where executionId shares the parent folder prefix.
            for (final File f : files)
            {
                final String parentFolder = (f.getParentFile() != null && !f.getParentFile().getName().equals(effectiveRunId))
                        ? f.getParentFile().getName() : "";
                if (!parentFolder.isEmpty() && (executionId.startsWith(parentFolder + "#") || executionId.equalsIgnoreCase(parentFolder)))
                {
                    final long classFileCount = files.stream()
                            .filter(file -> parentFolder.equals(file.getParentFile() != null ? file.getParentFile().getName() : ""))
                            .count();
                    if (classFileCount == 1)
                    {
                        final JsonNode node = readJsonNodeWithRetry(f);
                        // Guard against hijacking: a lone file of a multi-dataset class (other datasets not yet written
                        // during a live run) must not answer for a different dataset's execution id.
                        if (node != null && isIdentityCompatible(node, executionId))
                        {
                            try
                            {
                                return buildExecutionDetailsDto(node, executionId, effectiveRunId);
                            }
                            catch (final Exception e)
                            {
                                LOG.debug("Fuzzy match mapping failed for {}: {}", f.getName(), e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        final Optional<String> runJsonOpt = localRunJsonStorageService.readRunJson(effectiveRunId);
        if (runJsonOpt.isPresent())
        {
            try
            {
                final JsonNode root = objectMapper.readTree(runJsonOpt.get());
                final JsonNode execMetrics = root.path("executionMetrics");
                if (execMetrics.isObject() && !execMetrics.isEmpty())
                {
                    final List<JsonNode> simpleKeyHits = new ArrayList<>();
                    for (final Entry<String, JsonNode> entry : (Iterable<Entry<String, JsonNode>>) () -> execMetrics.fields())
                    {
                        final JsonNode node = entry.getValue();
                        final String key = entry.getKey();
                        final String id = node.path("id").asText(key);
                        final String simpleKey = extractSimpleExecutionKey(key);

                        if (executionId.equalsIgnoreCase(id) || executionId.equalsIgnoreCase(key))
                        {
                            final TestExecutionDto dto = objectMapper.treeToValue(node, TestExecutionDto.class);
                            resolveBugsForExecution(effectiveRunId, dto);
                            return dto;
                        }
                        if (!simpleKey.isEmpty() && executionId.equalsIgnoreCase(simpleKey))
                        {
                            simpleKeyHits.add(node);
                        }
                    }
                    if (simpleKeyHits.size() == 1)
                    {
                        final TestExecutionDto dto = objectMapper.treeToValue(simpleKeyHits.get(0), TestExecutionDto.class);
                        resolveBugsForExecution(effectiveRunId, dto);
                        return dto;
                    }
                }
                final JsonNode execArray = root.path("executions");
                if (execArray.isArray())
                {
                    final List<TestExecutionDto> parsed = new ArrayList<>();
                    for (final JsonNode execNode : execArray)
                    {
                        parsed.add(objectMapper.treeToValue(execNode, TestExecutionDto.class));
                    }
                    final TestExecutionDto match = findUniqueExecution(parsed, executionId);
                    if (match != null)
                    {
                        resolveBugsForExecution(effectiveRunId, match);
                        return match;
                    }
                }
            }
            catch (final Exception e)
            {
                LOG.error("Failed reading execution details from run.json for runId={}, execId={}: {}", effectiveRunId, executionId, e.getMessage());
            }
        }

        if (interactiveService != null && interactiveService.getCurrentConsoleEngine() != null)
        {
            final String liveJson = interactiveService.getCurrentConsoleEngine().getCurrentStateJson();
            if (liveJson != null && !liveJson.isBlank())
            {
                try
                {
                    final JsonNode node = objectMapper.readTree(liveJson);
                    final String liveRunId = node.path("runId").asText("");
                    if (effectiveRunId.equalsIgnoreCase(liveRunId) || runId.equalsIgnoreCase(liveRunId) || liveRunId.isEmpty())
                    {
                        final TestExecutionDto dto = objectMapper.treeToValue(node, TestExecutionDto.class);
                        final boolean matchesTarget = dto != null && (executionId.equalsIgnoreCase(dto.getId()) || matchesExecution(dto, executionId));
                        final boolean isGenericId = "console-execution".equalsIgnoreCase(executionId)
                                || "default".equalsIgnoreCase(executionId)
                                || "live".equalsIgnoreCase(executionId)
                                || executionId.equalsIgnoreCase(effectiveRunId);

                        if (dto != null && (matchesTarget || isGenericId))
                        {
                            if (dto.getId() == null || dto.getId().isBlank()
                                    || "bad".equalsIgnoreCase(dto.getId())
                                    || "perfect".equalsIgnoreCase(dto.getId())
                                    || "default".equalsIgnoreCase(dto.getId()))
                            {
                                dto.setId(executionId);
                            }
                            resolveBugsForExecution(effectiveRunId, dto);
                            return dto;
                        }
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
        }

        final RunReportDto cachedReport = runReportCache.get(effectiveRunId) != null ? runReportCache.get(effectiveRunId) : runReportCache.get(runId);
        if (cachedReport != null && cachedReport.getExecutions() != null)
        {
            final TestExecutionDto cachedMatch = findUniqueExecution(cachedReport.getExecutions(), executionId);
            if (cachedMatch != null)
            {
                resolveBugsForExecution(effectiveRunId, cachedMatch);
                return cachedMatch;
            }
        }

        final List<TestExecutionDto> liveList = liveRunBuffer.get(effectiveRunId) != null ? liveRunBuffer.get(effectiveRunId) : liveRunBuffer.get(runId);
        if (liveList != null)
        {
            final TestExecutionDto liveMatch = findUniqueExecution(liveList, executionId);
            if (liveMatch != null)
            {
                resolveBugsForExecution(effectiveRunId, liveMatch);
                return liveMatch;
            }
        }

        return null;
    }

    /**
     * Maps a raw execution JSON node to a {@link TestExecutionDto}, normalizing placeholder ids to the requested
     * execution id and resolving linked bugs.
     */
    private TestExecutionDto buildExecutionDetailsDto(final JsonNode node, final String executionId, final String effectiveRunId)
        throws JsonProcessingException
    {
        if (node.has("llmCalls") && node.path("blocks").isObject())
        {
            final ObjectNode blk = (ObjectNode) node.path("blocks");
            if (!blk.has("llmCalls"))
            {
                blk.set("llmCalls", node.get("llmCalls"));
            }
        }
        if (node instanceof ObjectNode objNode && (!objNode.has("playbookContent") || objNode.path("playbookContent").asText().isBlank()))
        {
            try
            {
                final Path runDir = localRunJsonStorageService.getRunDir(effectiveRunId).toAbsolutePath().normalize();
                if (Files.exists(runDir))
                {
                    final String rawTestClass = objNode.path("testClass").asText("");
                    final String cleanClass = rawTestClass.contains("#")
                        ? rawTestClass.substring(0, rawTestClass.indexOf('#')).trim()
                        : rawTestClass.trim();
                    final String simpleClass = cleanClass.contains(".")
                        ? cleanClass.substring(cleanClass.lastIndexOf('.') + 1)
                        : cleanClass;

                    String pbFileName = "playbook.yaml";
                    if (objNode.has("attachments") && objNode.get("attachments").isArray())
                    {
                        for (final JsonNode attNode : objNode.get("attachments"))
                        {
                            final String attName = attNode.path("name").asText("");
                            final String attPath = attNode.path("path").asText("");
                            if ("playbook.yaml".equals(attName) || attPath.startsWith("playbook"))
                            {
                                if (!attPath.isBlank() && !attPath.contains("..") && !attPath.contains("/") && !attPath.contains("\\"))
                                {
                                    pbFileName = attPath;
                                    break;
                                }
                            }
                        }
                    }

                    final List<Path> pbCandidateDirs = new ArrayList<>();
                    if (!cleanClass.isBlank())
                    {
                        final Path classDir = runDir.resolve(cleanClass).normalize();
                        if (classDir.startsWith(runDir))
                        {
                            pbCandidateDirs.add(classDir);
                        }
                    }
                    if (!simpleClass.isBlank() && !simpleClass.equals(cleanClass))
                    {
                        final Path simpleDir = runDir.resolve(simpleClass).normalize();
                        if (simpleDir.startsWith(runDir))
                        {
                            pbCandidateDirs.add(simpleDir);
                        }
                    }
                    pbCandidateDirs.add(runDir);

                    for (final Path cDir : pbCandidateDirs)
                    {
                        final Path pbPath = cDir.resolve(pbFileName).normalize();
                        if (pbPath.startsWith(cDir) && Files.isRegularFile(pbPath) && Files.isReadable(pbPath))
                        {
                            final String yaml = Files.readString(pbPath, StandardCharsets.UTF_8);
                            objNode.put("playbookContent", yaml);
                            break;
                        }
                    }
                }
            }
            catch (final Exception ignored)
            {
            }
        }
        final TestExecutionDto dto = objectMapper.treeToValue(node, TestExecutionDto.class);
        if (dto.getId() == null || dto.getId().isBlank()
                || "bad".equalsIgnoreCase(dto.getId())
                || "perfect".equalsIgnoreCase(dto.getId())
                || "default".equalsIgnoreCase(dto.getId()))
        {
            dto.setId(executionId);
        }

        // Verify and populate attachments based on disk existence
        final Path runDir = localRunJsonStorageService.getRunDir(effectiveRunId);
        final List<TestExecutionDto.ExecutionAttachmentDto> resolvedAttachments = new ArrayList<>();
        final String testClass = dto.getTestClass();
        final String simpleClass = dto.getSimpleClassName();

        final List<Path> candidateDirs = new ArrayList<>();
        if (Files.exists(runDir))
        {
            if (testClass != null && !testClass.isBlank())
            {
                final String cleanClass = testClass.contains("#") ? testClass.substring(0, testClass.indexOf('#')).trim() : testClass.trim();
                candidateDirs.add(runDir.resolve(cleanClass));
            }
            if (!simpleClass.isEmpty() && !simpleClass.equals(testClass))
            {
                candidateDirs.add(runDir.resolve(simpleClass));
            }
            candidateDirs.add(runDir);
        }

        final List<TestExecutionDto.ExecutionAttachmentDto> rawAttachments = dto.getAttachments();
        final Set<String> seenNames = new HashSet<>();

        if (rawAttachments != null)
        {
            for (final TestExecutionDto.ExecutionAttachmentDto att : rawAttachments)
            {
                if (att == null)
                {
                    continue;
                }
                final String effPath = (att.getPath() != null && !att.getPath().isBlank()) ? att.getPath() : att.getName();
                if (effPath == null || effPath.isBlank())
                {
                    continue;
                }
                for (final Path cDir : candidateDirs)
                {
                    final Path fPath = cDir.resolve(effPath).normalize();
                    if (fPath.startsWith(cDir) && Files.isRegularFile(fPath) && Files.isReadable(fPath))
                    {
                        try
                        {
                            att.setSize(Files.size(fPath));
                        }
                        catch (final Exception ignored)
                        {
                        }
                        resolvedAttachments.add(att);
                        if (att.getName() != null)
                        {
                            seenNames.add(att.getName().toLowerCase(Locale.ROOT));
                        }
                        if (att.getPath() != null)
                        {
                            seenNames.add(att.getPath().toLowerCase(Locale.ROOT));
                        }
                        break;
                    }
                }
            }
        }

        // Auto-discover standard attachments if not already present:
        // 1. Video
        if (dto.getVideoUrl() != null && !dto.getVideoUrl().isBlank())
        {
            final String videoName = dto.getVideoUrl().contains("/") ? dto.getVideoUrl().substring(dto.getVideoUrl().lastIndexOf('/') + 1) : dto.getVideoUrl();
            if (!seenNames.contains(videoName.toLowerCase(Locale.ROOT)))
            {
                for (final Path cDir : candidateDirs)
                {
                    final Path vf = cDir.resolve(videoName).normalize();
                    if (vf.startsWith(cDir) && Files.isRegularFile(vf) && Files.isReadable(vf))
                    {
                        final TestExecutionDto.ExecutionAttachmentDto vAtt = new TestExecutionDto.ExecutionAttachmentDto(
                            videoName, videoName, "video", videoName.endsWith(".webm") ? "video/webm" : "video/mp4", true, vf.toFile().length());
                        resolvedAttachments.add(vAtt);
                        seenNames.add(videoName.toLowerCase(Locale.ROOT));
                        break;
                    }
                }
            }
        }

        // 2. Playbook YAML
        if (!seenNames.contains("playbook.yaml"))
        {
            for (final Path cDir : candidateDirs)
            {
                final Path pf = cDir.resolve("playbook.yaml").normalize();
                if (pf.startsWith(cDir) && Files.isRegularFile(pf) && Files.isReadable(pf))
                {
                    final TestExecutionDto.ExecutionAttachmentDto pAtt = new TestExecutionDto.ExecutionAttachmentDto(
                        "playbook.yaml", "playbook.yaml", "yaml", "text/yaml", false, pf.toFile().length());
                    resolvedAttachments.add(pAtt);
                    seenNames.add("playbook.yaml");
                    break;
                }
            }
        }

        // 3. Execution Log
        if (!seenNames.contains("execution.log"))
        {
            for (final Path cDir : candidateDirs)
            {
                final Path lf = cDir.resolve("execution.log").normalize();
                if (lf.startsWith(cDir) && Files.isRegularFile(lf) && Files.isReadable(lf))
                {
                    final TestExecutionDto.ExecutionAttachmentDto lAtt = new TestExecutionDto.ExecutionAttachmentDto(
                        "execution.log", "execution.log", "log", "text/plain", false, lf.toFile().length());
                    resolvedAttachments.add(lAtt);
                    seenNames.add("execution.log");
                    break;
                }
            }
        }

        dto.setAttachments(resolvedAttachments);

        resolveBugsForExecution(effectiveRunId, dto);
        return dto;
    }

    /**
     * Checks that a fuzzily matched execution file does not contradict the requested execution id. If the file carries
     * identifying values (id, title, dataset) that are not placeholders, at least one of them must occur in
     * the requested id; otherwise the file belongs to a different dataset of the same class.
     */
    private static boolean isIdentityCompatible(final JsonNode node, final String executionId)
    {
        final String target = executionId.toLowerCase(Locale.ROOT);
        boolean hasIdentity = false;
        for (final String field : List.of("id", "title", "datasetId", "dataSet", "testId"))
        {
            final String value = node.path(field).asText("").trim();
            if (value.isEmpty() || "default".equalsIgnoreCase(value) || "bad".equalsIgnoreCase(value)
                    || "perfect".equalsIgnoreCase(value))
            {
                continue;
            }
            hasIdentity = true;
            if (target.contains(value.toLowerCase(Locale.ROOT)))
            {
                return true;
            }
        }
        return !hasIdentity;
    }

    private static String extractSimpleExecutionKey(final String key)
    {
        if (key == null || key.isBlank())
        {
            return "";
        }
        final int hashIdx = key.indexOf('#');
        final String classPart = hashIdx >= 0 ? key.substring(0, hashIdx) : key;
        final int dotIdx = classPart.lastIndexOf('.');
        if (dotIdx >= 0)
        {
            return classPart.substring(dotIdx + 1) + (hashIdx >= 0 ? key.substring(hashIdx) : "");
        }
        return key;
    }

    private boolean matchesExecution(final TestExecutionDto dto, final String targetId)
    {
        return matchesExecution(dto, targetId, true);
    }

    /**
     * Matches an execution against an id or composite key.
     *
     * @param includeSimple
     *            whether keys built from the simple (package-less) class name count as a match; those are weaker because
     *            different packages may share a simple class name
     */
    private boolean matchesExecution(final TestExecutionDto dto, final String targetId, final boolean includeSimple)
    {
        if (dto == null || targetId == null || targetId.isBlank())
        {
            return false;
        }

        final String id = dto.getId() != null ? dto.getId() : "";
        if (targetId.equalsIgnoreCase(id))
        {
            return true;
        }

        final String testClass = dto.getTestClass() != null ? dto.getTestClass() : "DefaultClass";
        final String testMethod = dto.getTestMethod() != null ? dto.getTestMethod() : "test";
        final String title = dto.getTitle() != null ? dto.getTitle() : "Default";
        final String browser = dto.getBrowser() != null ? dto.getBrowser() : "Chrome";

        final String execKey = LocalRunJsonStorageService.buildExecutionKey(testClass, testMethod, title, browser);
        final String fullKey = testClass + "#" + id + "#" + browser;
        final String execKeyWithId = execKey + "#" + id;
        final String classMethodBrowser = testClass + "#" + testMethod + "#" + browser;

        final String simpleClass = testClass.contains(".") ? testClass.substring(testClass.lastIndexOf('.') + 1) : testClass;
        final String simpleFullKey = simpleClass + "#" + id + "#" + browser;
        final String simpleExecKey = LocalRunJsonStorageService.buildExecutionKey(simpleClass, testMethod, title, browser);
        final String simpleExecKeyWithId = simpleExecKey + "#" + id;
        final String simpleClassMethodBrowser = simpleClass + "#" + testMethod + "#" + browser;

        return targetId.equalsIgnoreCase(fullKey)
            || targetId.equalsIgnoreCase(execKey)
            || targetId.equalsIgnoreCase(execKeyWithId)
            || targetId.equalsIgnoreCase(classMethodBrowser)
            || (includeSimple
                && (targetId.equalsIgnoreCase(simpleFullKey)
                    || targetId.equalsIgnoreCase(simpleExecKey)
                    || targetId.equalsIgnoreCase(simpleExecKeyWithId)
                    || targetId.equalsIgnoreCase(simpleClassMethodBrowser)));
    }

    /**
     * Finds an execution for the given id: the first exact match wins; a simple-class-key match is only accepted if it is
     * unambiguous, so equally named classes of different packages are never mixed up.
     */
    private TestExecutionDto findUniqueExecution(final Iterable<TestExecutionDto> executions, final String executionId)
    {
        final List<TestExecutionDto> simpleHits = new ArrayList<>();
        for (final TestExecutionDto dto : executions)
        {
            if (matchesExecution(dto, executionId, false))
            {
                return dto;
            }
            if (matchesExecution(dto, executionId, true))
            {
                simpleHits.add(dto);
            }
        }
        return simpleHits.size() == 1 ? simpleHits.get(0) : null;
    }

    private void resolveBugsForExecution(final String runId, final TestExecutionDto dto)
    {
        if (dto == null)
        {
            return;
        }
        final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
        final String runEnv = runOpt.map(TestRunEntity::getEnvironment).orElse("ALL");
        final String runBatch = runOpt.map(TestRunEntity::getBatchName).orElse("ALL");

        final Map<String, Long> runStartTimes = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
            .collect(Collectors.toMap(
                TestRunEntity::getId,
                r -> r.getStartTimeMs() != null ? r.getStartTimeMs() : 0L,
                (a, b) -> a
            ));

        final List<TestBaseBugEntity> allRunBugs = bugRepository.findByBatchNameInAndEnvironmentIn(List.of(runBatch, "ALL"), List.of(runEnv, "ALL"));
        final String varId = generateVariationId(dto.getTestClass(), dto.getTestMethod(), dto.getTitle(), dto.getLocation(), dto.getBrowser());
        final String varIdNoMethod = generateVariationId(dto.getTestClass(), null, dto.getTitle(), dto.getLocation(), dto.getBrowser());

        final List<TestBaseBugEntity> candidateActiveBugs = allRunBugs.stream()
            .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(runId, b.getLinkedRunId(), runStartTimes))
                      && (b.getRemovedRunId() == null || !isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes)))
            .filter(b -> varId.equals(b.getVariationId()) || varIdNoMethod.equals(b.getVariationId()))
            .collect(Collectors.toList());

        final Set<String> removedTickets = allRunBugs.stream()
            .filter(b -> b.getRemovedRunId() != null && isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes))
            .filter(b -> varId.equals(b.getVariationId()) || varIdNoMethod.equals(b.getVariationId()))
            .map(b -> normalizeTicket(b.getBugTicket()))
            .collect(Collectors.toSet());

        applyBugsToExecution(dto, candidateActiveBugs, removedTickets, runEnv, runBatch, getRunTagsMap(runId));
    }

    public void updateCachedReportBugsAndStats(final String runId)
    {
        final RunReportDto cachedReport = runReportCache.get(runId);
        if (cachedReport == null || cachedReport.getExecutions() == null)
        {
            return;
        }

        final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
        if (runOpt.isEmpty())
        {
            return;
        }

        final TestRunEntity runEntity = runOpt.get();
        final String runBatch = runEntity.getBatchName() != null ? runEntity.getBatchName() : "ALL";
        final String runEnv = runEntity.getEnvironment() != null ? runEntity.getEnvironment() : "ALL";

        final Map<String, Long> runStartTimes = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
            .collect(Collectors.toMap(
                TestRunEntity::getId,
                r -> r.getStartTimeMs() != null ? r.getStartTimeMs() : 0L,
                (a, b) -> a
            ));

        final List<TestBaseBugEntity> allRunBugs = bugRepository.findByBatchNameInAndEnvironmentIn(List.of(runBatch, "ALL"), List.of(runEnv, "ALL"));

        final Map<String, List<TestBaseBugEntity>> activeBugsByVarId = allRunBugs.stream()
            .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(runId, b.getLinkedRunId(), runStartTimes))
                      && (b.getRemovedRunId() == null || !isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes)))
            .collect(Collectors.groupingBy(TestBaseBugEntity::getVariationId));

        final Map<String, java.util.Set<String>> removedBugsByVarId = allRunBugs.stream()
            .filter(b -> b.getRemovedRunId() != null && isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes))
            .collect(Collectors.groupingBy(
                TestBaseBugEntity::getVariationId,
                Collectors.mapping(b -> normalizeTicket(b.getBugTicket()), Collectors.toSet())
            ));

        int pass = 0, fixed = 0, known = 0, unknown = 0, ignored = 0, running = 0;
        int passHealed = 0, fixedHealed = 0, knownHealed = 0, unknownHealed = 0;
        int passAi = 0, fixedAi = 0, knownAi = 0, unknownAi = 0;
        final List<TestExecutionDto> updatedExecutions = new ArrayList<>();
        final Map<String, List<String>> runTagsMap = getRunTagsMap(runEntity);

        for (final TestExecutionDto exec : cachedReport.getExecutions())
        {
            final String varId = generateVariationId(exec.getTestClass(), exec.getTestMethod(), exec.getTitle(), exec.getLocation(), exec.getBrowser());
            final String varIdNoMethod = generateVariationId(exec.getTestClass(), null, exec.getTitle(), exec.getLocation(), exec.getBrowser());
            final List<TestBaseBugEntity> candidateActiveBugs = new ArrayList<>(activeBugsByVarId.getOrDefault(varId, List.of()));
            for (final TestBaseBugEntity b : activeBugsByVarId.getOrDefault(varIdNoMethod, List.of()))
            {
                if (!candidateActiveBugs.contains(b))
                {
                    candidateActiveBugs.add(b);
                }
            }

            final Set<String> removedTickets = new HashSet<>(removedBugsByVarId.getOrDefault(varId, Set.of()));
            removedTickets.addAll(removedBugsByVarId.getOrDefault(varIdNoMethod, Set.of()));

            applyBugsToExecution(exec, candidateActiveBugs, removedTickets, runEntity.getEnvironment(), runEntity.getBatchName(), runTagsMap);

            final boolean hasBugs = exec.getBugs() != null && !exec.getBugs().isEmpty();
            final boolean isRunActive = "IN_PROGRESS".equalsIgnoreCase(runEntity.getStatus());
            final String failureText = extractFailureText(exec);
            final String eff = computeEffectiveStatus(exec.getStatus(), exec.getTotalStepsCount(), exec.getFailedStepsCount(), hasBugs, failureText, isRunActive);
            exec.setStatus(eff);

            final String st = exec.getStatus();
            if ("passed-clean".equalsIgnoreCase(st) || "passed".equalsIgnoreCase(st))
            {
                pass++;
                if (exec.isPassHealed())
                {
                    passHealed++;
                }
                if (exec.isPassAi())
                {
                    passAi++;
                }
            }
            else if ("succeeded-fixed".equalsIgnoreCase(st))
            {
                fixed++;
                if (exec.isFixedHealed())
                {
                    fixedHealed++;
                }
                if (exec.isFixedAi())
                {
                    fixedAi++;
                }
            }
            else if ("failed-known".equalsIgnoreCase(st))
            {
                known++;
                if (exec.isKnownHealed())
                {
                    knownHealed++;
                }
                if (exec.isKnownAi())
                {
                    knownAi++;
                }
            }
            else if ("failed-unknown".equalsIgnoreCase(st) || "failed".equalsIgnoreCase(st))
            {
                unknown++;
                if (exec.isUnknownHealed())
                {
                    unknownHealed++;
                }
                if (exec.isUnknownAi())
                {
                    unknownAi++;
                }
            }
            else if ("running".equalsIgnoreCase(st)) running++;
            else ignored++;

            updatedExecutions.add(exec);
        }

        final List<AreaSummaryDto> updatedAreas = buildAreaSummaries(updatedExecutions);

        final RunReportDto updatedReport = new RunReportDto(
            cachedReport.getRunId(),
            cachedReport.getBatchName(),
            cachedReport.getTimestamp(),
            cachedReport.getDuration(),
            updatedExecutions.size(),
            pass,
            fixed,
            known,
            unknown,
            ignored,
            running,
            updatedExecutions,
            updatedAreas
        );

        runReportCache.put(runId, updatedReport);

        runEntity.setTotalTests(updatedExecutions.size());
        runEntity.setPassedCount(pass);
        runEntity.setSucceededFixedCount(fixed);
        runEntity.setFailedKnownCount(known);
        runEntity.setFailedUnknownCount(unknown);
        runEntity.setIgnoredCount(ignored);
        runEntity.setPassedHealedCount(passHealed);
        runEntity.setSucceededFixedHealedCount(fixedHealed);
        runEntity.setFailedKnownHealedCount(knownHealed);
        runEntity.setFailedUnknownHealedCount(unknownHealed);
        runEntity.setPassedAiCount(passAi);
        runEntity.setSucceededFixedAiCount(fixedAi);
        runEntity.setFailedKnownAiCount(knownAi);
        runEntity.setFailedUnknownAiCount(unknownAi);
        runEntity.recalculatePassRate();
        runRepository.save(runEntity);
    }

    public RunReportDto getFilteredRunReport(
        final String runId,
        final String statusFilter,
        final List<String> locations,
        final List<String> browsers,
        final List<String> bugs,
        final List<String> failures)
    {
        final RunReportDto baseReport = getRunReport(runId);
        if (baseReport.getExecutions() == null || baseReport.getExecutions().isEmpty())
        {
            return baseReport;
        }

        final List<TestExecutionDto> filteredExecs = baseReport.getExecutions().stream().filter(exec -> {
            if (statusFilter != null && !statusFilter.isEmpty() && !"ALL".equalsIgnoreCase(statusFilter))
            {
                if ("FLAKY".equalsIgnoreCase(statusFilter))
                {
                    if (!exec.isFlaky())
                    {
                        return false;
                    }
                }
                else if (!statusFilter.equalsIgnoreCase(exec.getStatus()))
                {
                    return false;
                }
            }

            if (locations != null && !locations.isEmpty())
            {
                if (!locations.contains(exec.getLocation()))
                {
                    return false;
                }
            }

            if (browsers != null && !browsers.isEmpty())
            {
                if (!browsers.contains(exec.getBrowser()))
                {
                    return false;
                }
            }

            if (bugs != null && !bugs.isEmpty())
            {
                final boolean matchesBug = exec.getBugs() != null && exec.getBugs().stream().anyMatch(bugs::contains);
                if (!matchesBug)
                {
                    return false;
                }
            }

            if (failures != null && !failures.isEmpty())
            {
                if (!failures.contains(exec.getFailure()))
                {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());

        return buildRunReportDto(
            runRepository.findById(runId).orElse(null),
            filteredExecs
        );
    }

    @Transactional
    public String startRun(final String runId, final String batchName, final String environment, final String triggerSource)
    {
        final String effectiveRunId = (runId != null && !runId.isBlank()) ? runId : String.valueOf(System.currentTimeMillis() / 1000);
        final String timestampLabel = "Just Now";

        final TestRunEntity newRun = new TestRunEntity(
            effectiveRunId,
            batchName != null ? batchName : "Default",
            "IN_PROGRESS",
            triggerSource != null ? triggerSource : "Unknown",
            environment != null ? environment : "Default",
            "Default",
            "Chrome",
            timestampLabel,
            System.currentTimeMillis()
        );

        runRepository.save(newRun);
        liveRunBuffer.put(effectiveRunId, new java.util.concurrent.CopyOnWriteArrayList<>());
        runReportCache.remove(effectiveRunId);

        LOG.info("Started new test run: runId={}, batch={}", effectiveRunId, batchName);
        return effectiveRunId;
    }

    @Transactional
    public String startRun(final String runId, final String batchName, final String environment, final String triggerSource, final Map<String, List<String>> tags)
    {
        final String effectiveRunId = startRun(runId, batchName, environment, triggerSource);
        if (tags != null && !tags.isEmpty() && tagRepository != null)
        {
            final List<TestRunTagEntity> tagEntities = new ArrayList<>();
            for (final Map.Entry<String, List<String>> entry : tags.entrySet())
            {
                final String tagKey = entry.getKey();
                if (tagKey != null && !tagKey.isBlank() && entry.getValue() != null)
                {
                    for (final String tagVal : entry.getValue())
                    {
                        if (tagVal != null && !tagVal.isBlank())
                        {
                            final String trimmedVal = tagVal.trim();
                            if (!"Default".equalsIgnoreCase(trimmedVal) && !"Unknown".equalsIgnoreCase(trimmedVal))
                            {
                                tagEntities.add(new TestRunTagEntity(effectiveRunId, tagKey.trim(), trimmedVal));
                            }
                        }
                    }
                }
            }
            if (!tagEntities.isEmpty())
            {
                tagRepository.saveAll(tagEntities);
            }
        }
        return effectiveRunId;
    }

    @Transactional
    public String startRun(final String batchName, final String environment, final String triggerSource)
    {
        return startRun(null, batchName, environment, triggerSource);
    }

    @Transactional
    public void ingestExecution(final String runId, final Map<String, Object> payload)
    {
        try
        {
            final TestExecutionDto dto = objectMapper.convertValue(payload, TestExecutionDto.class);
            final List<TestExecutionDto> list = liveRunBuffer.computeIfAbsent(runId, k -> new java.util.concurrent.CopyOnWriteArrayList<>());

            boolean replaced = false;
            final String execId = dto.getId();
            final String varId = generateVariationId(dto.getTestClass(), dto.getTestMethod(), dto.getTitle(), dto.getLocation(), dto.getBrowser());
            final String varIdNoMethod = generateVariationId(dto.getTestClass(), null, dto.getTitle(), dto.getLocation(), dto.getBrowser());
            final String dtoBrowser = normalizeBrowser(dto.getBrowser());
            final String dtoTitle = dto.getTitle() != null ? dto.getTitle().trim() : "";
            final String dtoLoc = normalizeLocation(dto.getLocation());

            for (int i = 0; i < list.size(); i++)
            {
                final TestExecutionDto existing = list.get(i);
                final boolean idMatch = (execId != null && !execId.isBlank() && !execId.equalsIgnoreCase("default")
                        && execId.equalsIgnoreCase(existing.getId()));
                final String existingVarId = generateVariationId(existing.getTestClass(), existing.getTestMethod(), existing.getTitle(), existing.getLocation(), existing.getBrowser());
                final String existingVarIdNoMethod = generateVariationId(existing.getTestClass(), null, existing.getTitle(), existing.getLocation(), existing.getBrowser());
                final boolean varIdMatch = (varId != null && (varId.equalsIgnoreCase(existingVarId) || varId.equalsIgnoreCase(existingVarIdNoMethod)))
                        || (varIdNoMethod != null && (varIdNoMethod.equalsIgnoreCase(existingVarId) || varIdNoMethod.equalsIgnoreCase(existingVarIdNoMethod)));

                final boolean sameClass = isSameTestClass(dto.getTestClass(), existing.getTestClass());
                final boolean sameBrowser = dtoBrowser.equalsIgnoreCase(normalizeBrowser(existing.getBrowser()));
                final String existingTitle = existing.getTitle() != null ? existing.getTitle().trim() : "";
                final String existingLoc = normalizeLocation(existing.getLocation());
                final boolean sameTitleOrLocation = (dtoTitle.equalsIgnoreCase(existingTitle) || dtoTitle.isEmpty() || existingTitle.isEmpty() || "default".equalsIgnoreCase(dtoTitle) || "default".equalsIgnoreCase(existingTitle))
                        && (dtoLoc.equalsIgnoreCase(existingLoc) || "default".equalsIgnoreCase(dtoLoc) || "default".equalsIgnoreCase(existingLoc));

                final boolean componentMatch = sameClass && sameBrowser && sameTitleOrLocation;

                if (idMatch || varIdMatch || componentMatch)
                {
                    list.set(i, dto);
                    replaced = true;
                    break;
                }
            }
            if (!replaced)
            {
                list.add(dto);
            }

            if (dto.getTestClass() != null && !dto.getTestClass().isEmpty())
            {
                final String area = (dto.getAreaName() != null && !dto.getAreaName().isBlank())
                    ? normalizeArea(dto.getAreaName())
                    : "Default";
                final TestBaseVariationEntity var = variationRepository.findById(varId)
                    .orElseGet(() -> new TestBaseVariationEntity(varId, dto.getTestClass(), dto.getTestMethod(), dto.getTitle(), area, dto.getLocation(), dto.getBrowser()));
                if (dto.getSubArea() != null && !dto.getSubArea().isBlank())
                {
                    String sArea = dto.getSubArea().trim();
                    while (sArea.startsWith("@"))
                    {
                        sArea = sArea.substring(1).trim();
                    }
                    var.setSubAreaTag(sArea.isEmpty() ? null : sArea);
                }

                if (dto.getTestMethod() != null && !dto.getTestMethod().isBlank())
                {
                    var.setTestMethodName(dto.getTestMethod());
                }
                if (dto.getTitle() != null && !dto.getTitle().isBlank())
                {
                    var.setDataSetLabel(dto.getTitle());
                }
                var.setTotalExecutionsCount(var.getTotalExecutionsCount() + 1);
                var.setLastStatus(dto.getStatus());
                var.setLastExecutedAt(System.currentTimeMillis());
                variationRepository.save(var);
            }

            final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
            if (runOpt.isPresent())
            {
                final TestRunEntity run = runOpt.get();
                final String runBatch = run.getBatchName() != null ? run.getBatchName() : "ALL";
                final String runEnv = run.getEnvironment() != null ? run.getEnvironment() : "ALL";

                if (dto.getTestClass() != null && !dto.getTestClass().isEmpty())
                {
                    final Map<String, Long> runStartTimes = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
                        .collect(Collectors.toMap(
                            TestRunEntity::getId,
                            r -> r.getStartTimeMs() != null ? r.getStartTimeMs() : 0L,
                            (a, b) -> a
                        ));

                    final List<TestBaseBugEntity> candidateActiveBugs = bugRepository.findByVariationIdIn(List.of(varId, varIdNoMethod)).stream()
                        .filter(b -> (b.getBatchName().equalsIgnoreCase("ALL") || b.getBatchName().equalsIgnoreCase(runBatch))
                                  && (b.getEnvironment().equalsIgnoreCase("ALL") || b.getEnvironment().equalsIgnoreCase(runEnv)))
                        .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(runId, b.getLinkedRunId(), runStartTimes))
                                  && (b.getRemovedRunId() == null || !isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes)))
                        .collect(Collectors.toList());

                    final Set<String> removedTickets = bugRepository.findByVariationIdIn(List.of(varId, varIdNoMethod)).stream()
                        .filter(b -> (b.getBatchName().equalsIgnoreCase("ALL") || b.getBatchName().equalsIgnoreCase(runBatch))
                                  && (b.getEnvironment().equalsIgnoreCase("ALL") || b.getEnvironment().equalsIgnoreCase(runEnv)))
                        .filter(b -> b.getRemovedRunId() != null && isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes))
                        .map(b -> normalizeTicket(b.getBugTicket()))
                        .collect(Collectors.toSet());

                    applyBugsToExecution(dto, candidateActiveBugs, removedTickets, runEnv, runBatch, getRunTagsMap(runId));
                }

                final boolean hasBugs = dto.getBugs() != null && !dto.getBugs().isEmpty();
                final String failureText = extractFailureText(dto);
                final boolean isRunActive = "IN_PROGRESS".equalsIgnoreCase(run.getStatus());
                final String effectiveStatus = computeEffectiveStatus(dto.getStatus(), dto.getTotalStepsCount(), dto.getFailedStepsCount(), hasBugs, failureText, isRunActive);
                dto.setStatus(effectiveStatus);

                if (dto.getTestClass() != null && !dto.getTestClass().isEmpty())
                {
                    final Optional<TestBaseVariationEntity> varOpt = variationRepository.findById(varId);
                    if (varOpt.isPresent())
                    {
                        final TestBaseVariationEntity varEntity = varOpt.get();
                        varEntity.setLastStatus(effectiveStatus);
                        variationRepository.save(varEntity);
                    }
                }

                final int totalCount = list.size();
                int passCount = 0;
                int fixedCount = 0;
                int knownCount = 0;
                int unknownCount = 0;
                int runningCount = 0;
                int ignoredCount = 0;
                int passHealedCount = 0;
                int fixedHealedCount = 0;
                int knownHealedCount = 0;
                int unknownHealedCount = 0;
                final Set<String> browsersSet = new LinkedHashSet<>();
                final Set<String> localesSet = new LinkedHashSet<>();

                Long minStartTimeMs = null;
                String earliestFormattedTime = null;

                for (final TestExecutionDto item : list)
                {
                    final String st = item.getStatus() != null ? item.getStatus().trim().toLowerCase(Locale.ROOT) : "passed-clean";
                    switch (st)
                    {
                        case "running", "in_progress", "executing", "pending" -> runningCount++;
                        case "passed-clean", "passed" -> {
                            passCount++;
                            if (item.isPassHealed())
                            {
                                passHealedCount++;
                            }
                        }
                        case "succeeded-fixed", "fixed", "healed" -> {
                            fixedCount++;
                            if (item.isFixedHealed())
                            {
                                fixedHealedCount++;
                            }
                        }
                        case "failed-known" -> {
                            knownCount++;
                            if (item.isKnownHealed())
                            {
                                knownHealedCount++;
                            }
                        }
                        case "failed-unknown", "failed", "error" -> {
                            unknownCount++;
                            if (item.isUnknownHealed())
                            {
                                unknownHealedCount++;
                            }
                        }
                        case "ignored", "skipped" -> ignoredCount++;
                        default -> {
                            unknownCount++;
                            if (item.isUnknownHealed())
                            {
                                unknownHealedCount++;
                            }
                        }
                    }
                    if (item.getBrowser() != null && !item.getBrowser().isBlank() && !"Unknown".equalsIgnoreCase(item.getBrowser()))
                    {
                        browsersSet.add(item.getBrowser().trim());
                    }
                    if (item.getLocation() != null && !item.getLocation().isBlank() && !"Unknown".equalsIgnoreCase(item.getLocation()))
                    {
                        localesSet.add(item.getLocation().trim());
                    }

                    long itemStartMs = item.getTimestampMs();
                    if (itemStartMs <= 0L && item.getStartTime() != null && !item.getStartTime().isBlank())
                    {
                        final Long parsed = RunStorageSyncService.parseTimestampToMs(item.getStartTime());
                        if (parsed != null && parsed > 0L)
                        {
                            itemStartMs = parsed;
                        }
                    }
                    if (itemStartMs > 0L)
                    {
                        if (minStartTimeMs == null || itemStartMs < minStartTimeMs)
                        {
                            minStartTimeMs = itemStartMs;
                            earliestFormattedTime = item.getFormattedTime();
                            if (earliestFormattedTime == null || earliestFormattedTime.isBlank())
                            {
                                earliestFormattedTime = item.getStartTime();
                            }
                        }
                    }
                }

                run.setTotalTests(totalCount);
                run.setPassedCount(passCount);
                run.setSucceededFixedCount(fixedCount);
                run.setFailedKnownCount(knownCount);
                run.setFailedUnknownCount(unknownCount);
                run.setIgnoredCount(ignoredCount);
                run.setRunningCount(runningCount);
                run.setPassedHealedCount(passHealedCount);
                run.setSucceededFixedHealedCount(fixedHealedCount);
                run.setFailedKnownHealedCount(knownHealedCount);
                run.setFailedUnknownHealedCount(unknownHealedCount);

                if (minStartTimeMs != null && minStartTimeMs > 0L)
                {
                    run.setStartTimeMs(minStartTimeMs);
                    if (earliestFormattedTime != null && !earliestFormattedTime.isBlank())
                    {
                        run.setTimestampLabel(earliestFormattedTime);
                    }
                }

                if (!browsersSet.isEmpty())
                {
                    run.setBrowsersCsv(String.join(", ", browsersSet));
                }
                if (!localesSet.isEmpty())
                {
                    run.setLocalesCsv(String.join(", ", localesSet));
                }

                run.recalculatePassRate();
                runRepository.save(run);
            }

            LOG.info("Ingested test execution for runId={}: title={}, status={}", runId, dto.getTitle(), dto.getStatus());
            runReportCache.remove(runId);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to ingest test execution for runId {}: {}", runId, e.getMessage(), e);
        }
    }

    @Transactional
    public void finishRun(final String runId)
    {
        final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
        if (runOpt.isPresent())
        {
            final TestRunEntity run = runOpt.get();
            if (!"CANCELLED".equalsIgnoreCase(run.getStatus()))
            {
                run.setStatus("COMPLETED");
            }

            final List<TestExecutionDto> executions = liveRunBuffer.getOrDefault(runId, List.of());
            Long minStartTimeMs = null;
            String earliestFormattedTime = null;

            for (final TestExecutionDto exec : executions)
            {
                final String st = exec.getStatus();
                if (st == null || "running".equalsIgnoreCase(st) || "in_progress".equalsIgnoreCase(st) || "executing".equalsIgnoreCase(st) || "pending".equalsIgnoreCase(st))
                {
                    exec.setStatus("skipped");
                }
                long execStartMs = exec.getTimestampMs();
                if (execStartMs <= 0L && exec.getStartTime() != null && !exec.getStartTime().isBlank())
                {
                    final Long parsed = RunStorageSyncService.parseTimestampToMs(exec.getStartTime());
                    if (parsed != null && parsed > 0L)
                    {
                        execStartMs = parsed;
                    }
                }
                if (execStartMs > 0L)
                {
                    if (minStartTimeMs == null || execStartMs < minStartTimeMs)
                    {
                        minStartTimeMs = execStartMs;
                        earliestFormattedTime = exec.getFormattedTime();
                        if (earliestFormattedTime == null || earliestFormattedTime.isBlank())
                        {
                            earliestFormattedTime = exec.getStartTime();
                        }
                    }
                }
            }

            if (minStartTimeMs != null && minStartTimeMs > 0L)
            {
                run.setStartTimeMs(minStartTimeMs);
                if (earliestFormattedTime != null && !earliestFormattedTime.isBlank())
                {
                    run.setTimestampLabel(earliestFormattedTime);
                }
            }

            try
            {
                run.setRunningCount(0);
                run.setRunJsonPath("storage/runs/" + runId);
                runRepository.save(run);

                final Path runDir = localRunJsonStorageService.getRunDir(runId);
                final Path batchJsonPath = runDir.resolve("batch.json");
                if (!Files.exists(batchJsonPath))
                {
                    final ObjectNode batchNode = objectMapper.createObjectNode();
                    batchNode.put("name", run.getBatchName() != null ? run.getBatchName() : "Default");
                    batchNode.put("description", "");
                    batchNode.put("environment", run.getEnvironment() != null ? run.getEnvironment() : "Default");
                    Files.createDirectories(batchJsonPath.getParent());
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(batchJsonPath.toFile(), batchNode);
                }
            }
            catch (final Exception e)
            {
                LOG.error("Failed to serialize run.json for runId {}: {}", runId, e.getMessage(), e);
            }

            liveRunBuffer.remove(runId);
            runReportCache.remove(runId);
            LOG.info("Finished run runId={} and persisted to disk.", runId);
        }
    }

    @Transactional
    public void cancelRun(final String runId)
    {
        final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
        if (runOpt.isPresent())
        {
            final TestRunEntity run = runOpt.get();
            run.setStatus("CANCELLED");
            run.setRunningCount(0);
            runRepository.save(run);
        }

        final List<TestExecutionDto> executions = liveRunBuffer.getOrDefault(runId, List.of());
        for (final TestExecutionDto exec : executions)
        {
            final String st = exec.getStatus();
            if (st == null || "running".equalsIgnoreCase(st) || "in_progress".equalsIgnoreCase(st) || "executing".equalsIgnoreCase(st) || "pending".equalsIgnoreCase(st))
            {
                exec.setStatus("skipped");
            }
        }
        finishRun(runId);
    }

    @Transactional
    public TestExecutionDto addBugToExecution(final String runId, final String rowId, final String bugTicket)
    {
        return addBugToExecution(runId, rowId, bugTicket, "MAJOR", null);
    }

    @Transactional
    public TestExecutionDto addBugToExecution(
        final String runId,
        final String rowId,
        final String bugTicket,
        final String severity,
        final String boundTags)
    {
        final long startMs = System.currentTimeMillis();
        if (bugTicket == null || bugTicket.trim().isEmpty())
        {
            return new TestExecutionDto();
        }

        final String cleanTicket = bugTicket.trim().replaceAll("^#+", "");
        final String cleanSeverity = (severity != null && !severity.isBlank()) ? severity.trim().toUpperCase(Locale.ROOT) : "MAJOR";
        final String cleanBoundTags = (boundTags != null && !boundTags.isBlank()) ? boundTags.trim() : null;
        final String effectiveRunId = (runId != null) ? runId.trim().replaceAll("^#+", "") : "";

        try
        {
            final long step1Start = System.currentTimeMillis();
            final RunReportDto report = getRunReport(effectiveRunId);
            final long step1Ms = System.currentTimeMillis() - step1Start;

            Optional<TestExecutionDto> targetOpt = report.getExecutions().stream()
                .filter(e -> rowId.equalsIgnoreCase(e.getId())
                          || (e.getId() != null && e.getId().endsWith("#" + rowId))
                          || (rowId != null && rowId.endsWith("#" + e.getId())))
                .findFirst();
            if (targetOpt.isEmpty())
            {
                targetOpt = Optional.ofNullable(getExecutionDetails(effectiveRunId, rowId));
            }

            if (targetOpt.isPresent())
            {
                final TestExecutionDto target = targetOpt.get();
                final String varId = generateVariationId(target.getTestClass(), target.getTestMethod(), target.getTitle(), target.getLocation(), target.getBrowser());
                final Optional<TestRunEntity> runOpt = runRepository.findById(effectiveRunId);
                final String runBatch = runOpt.map(TestRunEntity::getBatchName).orElse("ALL");
                final String runEnv = runOpt.map(TestRunEntity::getEnvironment).orElse("ALL");
                final long runStartTime = runOpt.map(TestRunEntity::getStartTimeMs).orElse(System.currentTimeMillis());

                final String targetBatch = (cleanBoundTags != null && !cleanBoundTags.isBlank()) ? "ALL" : runBatch;
                final String targetEnv = (cleanBoundTags != null && !cleanBoundTags.isBlank()) ? "ALL" : runEnv;

                final long dbStart = System.currentTimeMillis();
                final List<TestBaseBugEntity> existing = bugRepository.findByVariationIdAndBatchNameInAndEnvironmentIn(
                    varId, List.of(runBatch, targetBatch, "ALL"), List.of(runEnv, targetEnv, "ALL"));
                final Optional<TestBaseBugEntity> activeOpt = existing.stream()
                    .filter(b -> normalizeTicket(b.getBugTicket()).equals(normalizeTicket(cleanTicket)) && b.getRemovedRunId() == null)
                    .findFirst();

                if (activeOpt.isEmpty())
                {
                    bugRepository.save(new TestBaseBugEntity(varId, cleanTicket, targetBatch, targetEnv, runStartTime, effectiveRunId, cleanSeverity, cleanBoundTags));
                }
                else
                {
                    final TestBaseBugEntity entity = activeOpt.get();
                    entity.setSeverity(cleanSeverity);
                    entity.setTagCriteria(cleanBoundTags);
                    entity.setBatchName(targetBatch);
                    entity.setEnvironment(targetEnv);
                    bugRepository.save(entity);
                }
                final long dbMs = System.currentTimeMillis() - dbStart;

                if (runEnv != null && !runEnv.isBlank() && target.getEnvironment().isEmpty())
                {
                    target.setEnvironment(runEnv);
                }
                if (runBatch != null && !runBatch.isBlank() && target.getBatchName().isEmpty())
                {
                    target.setBatchName(runBatch);
                }
                final boolean appliesToTarget = matchesBugTags(target, runEnv, runBatch, getRunTagsMap(effectiveRunId), cleanBoundTags);

                localRunJsonStorageService.updateExecutionInRun(effectiveRunId, rowId, execNode -> {
                    final ArrayNode bugsArray = objectMapper.createArrayNode();
                    final Set<String> currentBugs = new LinkedHashSet<>();
                    if (execNode.has("bugs") && execNode.path("bugs").isArray())
                    {
                        for (final JsonNode bNode : execNode.path("bugs"))
                        {
                            final String existingB = extractTicket(bNode);
                            if (!existingB.isBlank())
                            {
                                if (appliesToTarget || !normalizeTicket(existingB).equals(normalizeTicket(cleanTicket)))
                                {
                                    currentBugs.add(existingB);
                                }
                            }
                        }
                    }
                    if (appliesToTarget)
                    {
                        currentBugs.add(cleanTicket);
                    }
                    for (final String b : currentBugs)
                    {
                        bugsArray.add(b);
                    }
                    execNode.set("bugs", bugsArray);
                    final boolean hasBugs = !currentBugs.isEmpty();
                    final String raw = execNode.has("status") ? execNode.path("status").asText() : "failed-unknown";
                    if ("failed".equalsIgnoreCase(raw) || "failed-known".equalsIgnoreCase(raw) || "failed-unknown".equalsIgnoreCase(raw) || "error".equalsIgnoreCase(raw) || "failure".equalsIgnoreCase(raw))
                    {
                        execNode.put("status", hasBugs ? "failed-known" : "failed-unknown");
                    }
                    else if ("passed".equalsIgnoreCase(raw) || "succeeded-fixed".equalsIgnoreCase(raw) || "passed-clean".equalsIgnoreCase(raw) || "succeeded".equalsIgnoreCase(raw) || "healed".equalsIgnoreCase(raw))
                    {
                        execNode.put("status", hasBugs ? "succeeded-fixed" : "passed-clean");
                    }
                    else if ("finished".equalsIgnoreCase(raw))
                    {
                        final boolean isFinishedSuccess = LocalRunJsonStorageService.extractStepsFailed(execNode) <= 0
                                && (extractFailureText(execNode) == null);
                        execNode.put("status", isFinishedSuccess ? "succeeded-fixed" : "failed-known");
                    }
                });

                final Path runDir = localRunJsonStorageService.getRunDir(effectiveRunId);
                if (Files.isDirectory(runDir))
                {
                    final boolean isRunActive = runOpt.map(r -> "IN_PROGRESS".equalsIgnoreCase(r.getStatus())).orElse(false);
                    localRunJsonStorageService.buildRunJsonContent(runDir.toFile(), effectiveRunId, true, isRunActive);
                }

                final long reevalStart = System.currentTimeMillis();
                reevaluateBatchRunsFrom(runBatch, runStartTime, varId, effectiveRunId);
                final long syncMs = System.currentTimeMillis() - reevalStart;

                runReportCache.remove(effectiveRunId);
                final long finalReportStart = System.currentTimeMillis();
                final RunReportDto updatedReport = getRunReport(effectiveRunId);
                final long finalReportMs = System.currentTimeMillis() - finalReportStart;

                final long totalMs = System.currentTimeMillis() - startMs;
                LOG.info("[PERF] addBugToExecution total={} ms (initialReport={} ms, dbSave={} ms, syncUpdate={} ms, finalReport={} ms) runId={}, rowId={}, ticket={}",
                    totalMs, step1Ms, dbMs, syncMs, finalReportMs, effectiveRunId, rowId, cleanTicket);

                final String targetExecId = target.getId();
                return updatedReport.getExecutions().stream()
                    .filter(e -> matchesExecution(e, rowId) || (targetExecId != null && targetExecId.equalsIgnoreCase(e.getId())))
                    .findFirst()
                    .orElseGet(() -> Optional.ofNullable(getExecutionDetails(effectiveRunId, rowId)).orElseGet(TestExecutionDto::new));
            }
            else
            {
                LOG.warn("Target execution id '{}' not found in run '{}' during addBugToExecution", rowId, effectiveRunId);
            }
        }
        catch (final Exception e)
        {
            LOG.error("Failed to add bug ticket to execution {}: {}", rowId, e.getMessage(), e);
        }

        return new TestExecutionDto();
    }

    @Transactional
    public TestExecutionDto removeBugFromExecution(final String runId, final String rowId, final String bugTicket)
    {
        final long startMs = System.currentTimeMillis();
        final String effectiveRunId = (runId != null) ? runId.trim().replaceAll("^#+", "") : "";
        if (bugTicket == null || bugTicket.trim().isEmpty())
        {
            return Optional.ofNullable(getExecutionDetails(effectiveRunId, rowId)).orElseGet(TestExecutionDto::new);
        }

        final String cleanTicket = bugTicket.trim().replaceAll("^#+", "");

        try
        {
            final long step1Start = System.currentTimeMillis();
            final RunReportDto report = getRunReport(effectiveRunId);
            final long step1Ms = System.currentTimeMillis() - step1Start;

            Optional<TestExecutionDto> targetOpt = report.getExecutions().stream()
                .filter(e -> rowId.equalsIgnoreCase(e.getId())
                          || (e.getId() != null && e.getId().endsWith("#" + rowId))
                          || (rowId != null && rowId.endsWith("#" + e.getId())))
                .findFirst();
            if (targetOpt.isEmpty())
            {
                targetOpt = Optional.ofNullable(getExecutionDetails(effectiveRunId, rowId));
            }

            if (targetOpt.isPresent())
            {
                final TestExecutionDto target = targetOpt.get();
                final String varId = generateVariationId(target.getTestClass(), target.getTestMethod(), target.getTitle(), target.getLocation(), target.getBrowser());
                final String varIdNoMethod = generateVariationId(target.getTestClass(), null, target.getTitle(), target.getLocation(), target.getBrowser());
                final Set<String> targetVarIds = new LinkedHashSet<>(List.of(varId, varIdNoMethod));
                final Optional<TestRunEntity> runOpt = runRepository.findById(effectiveRunId);
                final String runBatch = runOpt.map(TestRunEntity::getBatchName).orElse("ALL");
                final String runEnv = runOpt.map(TestRunEntity::getEnvironment).orElse("ALL");
                final long runStartTime = runOpt.map(TestRunEntity::getStartTimeMs).orElse(System.currentTimeMillis());

                final long dbStart = System.currentTimeMillis();
                final List<TestBaseBugEntity> existing = bugRepository.findByVariationIdIn(new ArrayList<>(targetVarIds));
                boolean matched = false;
                for (final TestBaseBugEntity bug : existing)
                {
                    if (normalizeTicket(bug.getBugTicket()).equals(normalizeTicket(cleanTicket)))
                    {
                        bug.setRemovedAtMs(runStartTime);
                        bug.setRemovedRunId(effectiveRunId);
                        bugRepository.save(bug);
                        matched = true;
                    }
                }
                if (!matched)
                {
                    for (final String vId : targetVarIds)
                    {
                        final TestBaseBugEntity tombstone = new TestBaseBugEntity(vId, cleanTicket, runBatch, runEnv, runStartTime, null);
                        tombstone.setRemovedRunId(effectiveRunId);
                        tombstone.setRemovedAtMs(runStartTime);
                        bugRepository.save(tombstone);
                    }
                }
                final long dbMs = System.currentTimeMillis() - dbStart;

                // Persist removal in disk execution JSON and run.json
                localRunJsonStorageService.updateExecutionInRun(effectiveRunId, rowId, execNode -> {
                    final ArrayNode newBugs = objectMapper.createArrayNode();
                    if (execNode.has("bugs") && execNode.path("bugs").isArray())
                    {
                        for (final JsonNode bNode : execNode.path("bugs"))
                        {
                            final String b = extractTicket(bNode);
                            if (!b.isBlank() && !normalizeTicket(b).equals(normalizeTicket(cleanTicket)))
                            {
                                newBugs.add(b);
                            }
                        }
                    }
                    execNode.set("bugs", newBugs);
                    final boolean hasBugs = newBugs.size() > 0;
                    final String raw = execNode.has("status") ? execNode.path("status").asText() : "failed-unknown";
                    if ("failed".equalsIgnoreCase(raw) || "failed-known".equalsIgnoreCase(raw) || "failed-unknown".equalsIgnoreCase(raw) || "error".equalsIgnoreCase(raw) || "failure".equalsIgnoreCase(raw))
                    {
                        execNode.put("status", hasBugs ? "failed-known" : "failed-unknown");
                    }
                    else if ("passed".equalsIgnoreCase(raw) || "succeeded-fixed".equalsIgnoreCase(raw) || "passed-clean".equalsIgnoreCase(raw) || "succeeded".equalsIgnoreCase(raw) || "healed".equalsIgnoreCase(raw))
                    {
                        execNode.put("status", hasBugs ? "succeeded-fixed" : "passed-clean");
                    }
                    else if ("finished".equalsIgnoreCase(raw))
                    {
                        final boolean isFinishedSuccess = LocalRunJsonStorageService.extractStepsFailed(execNode) <= 0
                                && (extractFailureText(execNode) == null);
                        execNode.put("status", isFinishedSuccess
                                ? (hasBugs ? "succeeded-fixed" : "passed-clean")
                                : (hasBugs ? "failed-known" : "failed-unknown"));
                    }
                });

                final Path runDir = localRunJsonStorageService.getRunDir(effectiveRunId);
                if (Files.isDirectory(runDir))
                {
                    final boolean isRunActive = runOpt.map(r -> "IN_PROGRESS".equalsIgnoreCase(r.getStatus())).orElse(false);
                    localRunJsonStorageService.buildRunJsonContent(runDir.toFile(), effectiveRunId, true, isRunActive);
                }

                // Resync variation statuses
                for (final String vId : targetVarIds)
                {
                    syncVariationLastStatus(vId);
                }

                final long reevalStart = System.currentTimeMillis();
                for (final String vId : targetVarIds)
                {
                    reevaluateBatchRunsFrom(runBatch, runStartTime, vId, effectiveRunId);
                }
                final long syncMs = System.currentTimeMillis() - reevalStart;

                runReportCache.remove(effectiveRunId);
                final long finalReportStart = System.currentTimeMillis();
                final RunReportDto updatedReport = getRunReport(effectiveRunId);
                final long finalReportMs = System.currentTimeMillis() - finalReportStart;

                final long totalMs = System.currentTimeMillis() - startMs;
                LOG.info("[PERF] removeBugFromExecution total={} ms (initialReport={} ms, dbSave={} ms, syncUpdate={} ms, finalReport={} ms) runId={}, rowId={}, ticket={}",
                    totalMs, step1Ms, dbMs, syncMs, finalReportMs, effectiveRunId, rowId, cleanTicket);

                final String targetExecId = target.getId();
                return updatedReport.getExecutions().stream()
                    .filter(e -> matchesExecution(e, rowId) || (targetExecId != null && targetExecId.equalsIgnoreCase(e.getId())))
                    .findFirst()
                    .orElseGet(() -> Optional.ofNullable(getExecutionDetails(effectiveRunId, rowId)).orElseGet(TestExecutionDto::new));
            }
            else
            {
                LOG.warn("Target execution id '{}' not found in run '{}' during removeBugFromExecution", rowId, effectiveRunId);
            }
        }
        catch (final Exception e)
        {
            LOG.error("Failed to remove bug ticket from execution {}: {}", rowId, e.getMessage(), e);
        }

        return new TestExecutionDto();
    }

    private void reevaluateBatchRunsFrom(final String batchName, final long startTimeMs)
    {
        reevaluateBatchRunsFrom(batchName, startTimeMs, null, null);
    }

    private void reevaluateBatchRunsFrom(final String batchName, final long startTimeMs, final String targetVariationId)
    {
        reevaluateBatchRunsFrom(batchName, startTimeMs, targetVariationId, null);
    }

    private void reevaluateBatchRunsFrom(final String batchName, final long startTimeMs, final String targetVariationId, final String triggerRunId)
    {
        final long reevalStart = System.currentTimeMillis();
        final List<TestRunEntity> affectedRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
            .filter(r -> (triggerRunId != null && triggerRunId.equalsIgnoreCase(r.getId()))
                      || ((batchName.equalsIgnoreCase("ALL") || batchName.equalsIgnoreCase(r.getBatchName()))
                          && r.getStartTimeMs() != null && r.getStartTimeMs() >= startTimeMs))
            .collect(Collectors.toList());

        LOG.info("[PERF] reevaluateBatchRunsFrom starting for batch='{}', startTimeMs={}, totalAffectedRuns={}",
            batchName, startTimeMs, affectedRuns.size());

        for (final TestRunEntity run : affectedRuns)
        {
            final long runStart = System.currentTimeMillis();
            runReportCache.remove(run.getId());
            recalculateRunEntityStats(run.getId());
            final long runMs = System.currentTimeMillis() - runStart;
            LOG.info("[PERF] reevaluateBatchRunsFrom recalculated runId={} in {} ms", run.getId(), runMs);
        }

        if (targetVariationId != null && !targetVariationId.isBlank())
        {
            syncVariationLastStatus(targetVariationId);
        }

        final long totalReevalMs = System.currentTimeMillis() - reevalStart;
        LOG.info("[PERF] reevaluateBatchRunsFrom completed for batch='{}', totalAffectedRuns={} in {} ms",
            batchName, affectedRuns.size(), totalReevalMs);
    }

    public static List<String> sortHistoryLinksDescending(final String historyLinks)
    {
        if (historyLinks == null || historyLinks.trim().isEmpty())
        {
            return Collections.emptyList();
        }
        final List<String> linksList = new ArrayList<>(List.of(historyLinks.split(",")));
        linksList.sort((final String u1, final String u2) -> {
            final Map<String, String> p1 = parseQueryParams(u1);
            final Map<String, String> p2 = parseQueryParams(u2);
            final long t1 = TestBaseVariationHistoryDto.parseTimestampToEpoch(p1.get("ts"), p1.get("runId"));
            final long t2 = TestBaseVariationHistoryDto.parseTimestampToEpoch(p2.get("ts"), p2.get("runId"));
            final int cmp = Long.compare(t2, t1);
            if (cmp != 0)
            {
                return cmp;
            }
            final String r1 = p1.getOrDefault("runId", "");
            final String r2 = p2.getOrDefault("runId", "");
            return r2.compareToIgnoreCase(r1);
        });
        return linksList;
    }

    static boolean hasBugsInLatestHistoryLink(final String historyLinks)
    {
        if (historyLinks == null || historyLinks.trim().isEmpty())
        {
            return false;
        }
        final List<String> sorted = sortHistoryLinksDescending(historyLinks);
        if (!sorted.isEmpty())
        {
            final String first = sorted.get(0).trim();
            final Map<String, String> params = parseQueryParams(first);
            final String status = params.get("status");
            final String bugs = params.get("bugs");
            if ("failed-known".equalsIgnoreCase(status) || "succeeded-fixed".equalsIgnoreCase(status) || (bugs != null && !bugs.trim().isEmpty()))
            {
                return true;
            }
        }
        return false;
    }

    static boolean isLatestHistoryLinkHealed(final String historyLinks)
    {
        if (historyLinks == null || historyLinks.trim().isEmpty())
        {
            return false;
        }
        final List<String> sorted = sortHistoryLinksDescending(historyLinks);
        if (!sorted.isEmpty())
        {
            final String first = sorted.get(0).trim();
            final Map<String, String> params = parseQueryParams(first);
            final String status = params.get("status");
            final String healed = params.get("healed");
            if ("healed".equalsIgnoreCase(status) || "true".equalsIgnoreCase(healed))
            {
                return true;
            }
        }
        return false;
    }

    private void syncVariationLastStatus(final String varId)
    {
        if (varId == null || varId.isBlank())
        {
            return;
        }
        final Optional<TestBaseVariationEntity> varOpt = variationRepository.findById(varId);
        if (varOpt.isPresent())
        {
            final TestBaseVariationEntity var = varOpt.get();
            final List<TestBaseBugEntity> activeBugs = bugRepository.findByVariationId(varId).stream()
                .filter(b -> b.getRemovedRunId() == null)
                .collect(Collectors.toList());
            final boolean hasBugs = !activeBugs.isEmpty();

            final List<TestBaseVariationHistoryDto> history = getVariationHistory(
                var.getTestClassName(),
                var.getTestMethodName(),
                var.getDataSetLabel(),
                var.getLocation(),
                var.getBrowser()
            );

            if (!history.isEmpty())
            {
                final TestBaseVariationHistoryDto latest = history.get(0);
                final String prevMode = var.getExecutionMode();
                final String linkMode = latest.getMode();
                boolean modeChanged = false;
                if (linkMode != null && !linkMode.isBlank())
                {
                    var.setExecutionMode(linkMode);
                    modeChanged = !Objects.equals(linkMode, prevMode);
                }
                final boolean isHealed = latest.isHealed();
                var.setHealed(isHealed);
                final String cur = latest.getStatus() != null ? latest.getStatus() : "passed-clean";

                String effective = cur;
                if ("failed".equalsIgnoreCase(cur) || "failed-unknown".equalsIgnoreCase(cur) || "failed-known".equalsIgnoreCase(cur) || "error".equalsIgnoreCase(cur))
                {
                    effective = hasBugs ? "failed-known" : "failed-unknown";
                }
                else if ("passed".equalsIgnoreCase(cur) || "succeeded-fixed".equalsIgnoreCase(cur) || "passed-clean".equalsIgnoreCase(cur) || "succeeded".equalsIgnoreCase(cur) || "healed".equalsIgnoreCase(cur))
                {
                    effective = hasBugs ? "succeeded-fixed" : "passed-clean";
                }

                if (!effective.equalsIgnoreCase(var.getLastStatus()) || !Objects.equals(isHealed, var.getHealed()) || modeChanged)
                {
                    var.setLastStatus(effective);
                    variationRepository.save(var);
                }
            }
        }
    }

    public void saveRunReportToDisk(final String runId, final RunReportDto report)
    {
        saveRunReportToDisk(runId, report, null);
    }

    public void saveRunReportToDisk(final String runId, final RunReportDto report, final String targetVariationId)
    {
        if (runId == null || report == null)
        {
            return;
        }
        runReportCache.remove(runId);
        try
        {
            final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
            final String batchName = report.getBatchName() != null ? report.getBatchName() : runOpt.map(TestRunEntity::getBatchName).orElse("Default");
            final String env = runOpt.map(TestRunEntity::getEnvironment).orElse("Default");
            final String duration = report.getDuration() != null ? report.getDuration() : "0s";
            final int total = report.getTotalCount();
            final double passRate = total > 0 ? Math.round((report.getPassCount() + report.getFixedCount()) * 100.0 / total * 10.0) / 10.0 : 0.0;

            final Path runDir = localRunJsonStorageService.getRunDir(runId);
            Files.createDirectories(runDir);

            if (report.getExecutions() != null && !report.getExecutions().isEmpty())
            {
                int index = 1;
                for (final TestExecutionDto exec : report.getExecutions())
                {
                    if (targetVariationId != null && !targetVariationId.trim().isEmpty())
                    {
                        final String execVarId = generateVariationId(exec.getTestClass(), exec.getTestMethod(), exec.getTitle(), exec.getLocation(), exec.getBrowser());
                        if (!targetVariationId.equalsIgnoreCase(execVarId))
                        {
                            index++;
                            continue;
                        }
                    }
                    final boolean updated = localRunJsonStorageService.updateExecutionInRun(runId, exec.getId(), node -> {
                        node.put("status", exec.getStatus());
                        final com.fasterxml.jackson.databind.node.ArrayNode bugsArray = objectMapper.createArrayNode();
                        if (exec.getBugs() != null)
                        {
                            for (final String bug : exec.getBugs())
                            {
                                bugsArray.add(bug);
                            }
                        }
                        node.set("bugs", bugsArray);
                        if (exec.getComment() != null)
                        {
                            node.put("comment", exec.getComment());
                        }
                        if (exec.getAreaName() != null && !exec.getAreaName().trim().isEmpty())
                        {
                            node.put("areaName", exec.getAreaName().trim());
                        }
                        if (exec.getTestClass() != null && !exec.getTestClass().trim().isEmpty())
                        {
                            node.put("testClass", exec.getTestClass().trim());
                        }
                    });

                    if (!updated)
                    {
                        final String area = (exec.getAreaName() != null && isRealArea(exec.getAreaName()))
                                            ? exec.getAreaName().trim() : "Default";
                        final String testCls = (exec.getTestClass() != null && !exec.getTestClass().trim().isEmpty())
                                               ? exec.getTestClass().trim() : "DefaultClass";
                        final Path execDir = runDir.resolve(area).resolve(testCls);
                        Files.createDirectories(execDir);

                        final String filename = "console-execution-" + index + ".json";
                        final Path execFile = execDir.resolve(filename);

                        final ObjectNode execNode = objectMapper.valueToTree(exec);
                        execNode.put("areaName", area);
                        execNode.put("testClass", testCls);
                        objectMapper.writerWithDefaultPrettyPrinter().writeValue(execFile.toFile(), execNode);
                    }
                    index++;
                }
            }

        }
        catch (final Exception e)
        {
            LOG.error("Failed to save run report to disk for runId {}: {}", runId, e.getMessage(), e);
        }
    }

    static String extractTicket(final JsonNode node)
    {
        return TestExecutionDto.FlexibleBugListDeserializer.extractTicket(node);
    }

    static String normalizeTicket(final String ticket)
    {
        if (ticket == null)
        {
            return "";
        }
        return ticket.trim().replaceAll("^#+", "").toUpperCase();
    }

    static boolean isRunAtOrAfter(final String currentRunId, final String refRunId)
    {
        return isRunAtOrAfter(currentRunId, refRunId, Map.of());
    }

    static boolean isRunAtOrAfter(final String currentRunId, final String refRunId, final Map<String, Long> runStartTimes)
    {
        if (currentRunId == null || refRunId == null)
        {
            return true;
        }
        if (currentRunId.equalsIgnoreCase(refRunId))
        {
            return true;
        }
        final Long currentStart = runStartTimes.get(currentRunId);
        final Long refStart = runStartTimes.get(refRunId);
        if (currentStart != null && refStart != null)
        {
            return currentStart >= refStart;
        }
        if (currentRunId.matches("^\\d+$") && refRunId.matches("^\\d+$"))
        {
            try
            {
                final long cId = Long.parseLong(currentRunId);
                final long rId = Long.parseLong(refRunId);
                return cId >= rId;
            }
            catch (final NumberFormatException ignored)
            {
            }
        }
        return currentRunId.compareTo(refRunId) >= 0;
    }

    private void recalculateRunEntityStats(final String runId)
    {
        final long startMs = System.currentTimeMillis();
        final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
        if (runOpt.isPresent())
        {
            final TestRunEntity run = runOpt.get();
            final RunReportDto report = getRunReport(runId);
            run.setTotalTests(report.getTotalCount());
            run.setPassedCount(report.getPassCount());
            run.setSucceededFixedCount(report.getFixedCount());
            run.setFailedKnownCount(report.getKnownCount());
            run.setFailedUnknownCount(report.getUnknownCount());
            run.setIgnoredCount(report.getIgnoredCount());
            run.setPassedHealedCount(report.getPassHealedCount());
            run.setSucceededFixedHealedCount(report.getFixedHealedCount());
            run.setFailedKnownHealedCount(report.getKnownHealedCount());
            run.setFailedUnknownHealedCount(report.getUnknownHealedCount());
            run.setPassedAiCount(report.getPassAiCount());
            run.setSucceededFixedAiCount(report.getFixedAiCount());
            run.setFailedKnownAiCount(report.getKnownAiCount());
            run.setFailedUnknownAiCount(report.getUnknownAiCount());
            run.setPlaybookCount(report.getPlaybookCount());
            run.recalculatePassRate();
            runRepository.save(run);
            final long durationMs = System.currentTimeMillis() - startMs;
            LOG.info("[PERF] recalculateRunEntityStats for runId={} took {} ms", runId, durationMs);
        }
    }

    @Transactional
    public void removeRunFromHistory(final String runId)
    {
        if (runId == null || runId.trim().isEmpty())
        {
            return;
        }
        final String cleanRunId = runId.trim().replaceAll("^#+", "");
        final Optional<TestRunEntity> runOpt = runRepository.findById(cleanRunId);
        if (runOpt.isEmpty())
        {
            LOG.warn("Cannot remove run {}: not found in repository", cleanRunId);
            return;
        }

        final TestRunEntity run = runOpt.get();
        final String batchName = run.getBatchName();

        // 1. Identify affected variations BEFORE deleting storage or cache
        final Set<String> affectedVarIds = new HashSet<>();
        final RunReportDto deletedReport = getRunReport(cleanRunId);
        if (deletedReport != null && deletedReport.getExecutions() != null)
        {
            for (final TestExecutionDto exec : deletedReport.getExecutions())
            {
                final String vId = generateVariationId(
                    exec.getTestClass(),
                    exec.getTestMethod(),
                    exec.getTitle(),
                    exec.getLocation(),
                    exec.getBrowser()
                );
                affectedVarIds.add(vId);
            }
        }

        // 2. Mark run as deleted
        run.setIsDeleted(true);
        runRepository.save(run);
        if (tagRepository != null)
        {
            try
            {
                tagRepository.deleteByRunId(cleanRunId);
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to delete tags for runId={}: {}", cleanRunId, e.getMessage());
            }
        }
        LOG.info("Soft-deleted run runId={}", cleanRunId);

        // 3. Storage cleanup (disk JSON and attachments)
        try
        {
            localRunJsonStorageService.deleteRunJson(cleanRunId);
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to delete local run JSON for runId={}: {}", cleanRunId, e.getMessage());
        }

        try
        {
            if (attachmentStorageService != null)
            {
                attachmentStorageService.deleteRunAttachments(cleanRunId);
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to delete attachments for runId={}: {}", cleanRunId, e.getMessage());
        }

        // 4. Evict only this deleted run from cache (keeping remaining runs warm in memory)
        runReportCache.remove(cleanRunId);

        // 5. Update parent batch's latestRunId
        if (batchName != null && !batchName.isBlank())
        {
            final List<TestRunEntity> remainingBatchRuns = runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(batchName);
            final Optional<TestBatchEntity> batchOpt = batchRepository.findById(batchName);
            if (batchOpt.isPresent())
            {
                final TestBatchEntity batch = batchOpt.get();
                final String newLatestRunId = !remainingBatchRuns.isEmpty() ? remainingBatchRuns.get(0).getId() : null;
                batch.setLatestRunId(newLatestRunId);
                batchRepository.save(batch);
                LOG.info("Updated batch '{}' latestRunId to '{}'", batchName, newLatestRunId);
            }
        }

        // 6. Update Test Base variations for affected variation IDs only
        final List<TestBaseVariationEntity> variationsToProcess = !affectedVarIds.isEmpty()
            ? variationRepository.findAllById(affectedVarIds)
            : Collections.emptyList();

        final List<TestBaseVariationEntity> variationsToDelete = new ArrayList<>();

        for (final TestBaseVariationEntity var : variationsToProcess)
        {
            final List<TestBaseVariationHistoryDto> history = getVariationHistory(
                var.getTestClassName(),
                var.getTestMethodName(),
                var.getDataSetLabel(),
                var.getLocation(),
                var.getBrowser()
            );

            if (history.isEmpty())
            {
                variationsToDelete.add(var);
                final List<TestBaseBugEntity> varBugs = bugRepository.findByVariationId(var.getId());
                if (!varBugs.isEmpty())
                {
                    bugRepository.deleteAll(varBugs);
                }
                LOG.info("Pruned empty variation varId='{}' as all associated runs were removed", var.getId());
            }
        }

        if (!variationsToDelete.isEmpty())
        {
            variationRepository.deleteAll(variationsToDelete);
        }

        if (tagRepository != null)
        {
            tagRepository.deleteByRunId(cleanRunId);
        }

        // 6. Bug reconciliation under Option B:
        // Clear linkedRunId on bugs linked during this run so they remain active on the variation
        final List<TestBaseBugEntity> allBugs = bugRepository.findAll();
        for (final TestBaseBugEntity bug : allBugs)
        {
            boolean bugChanged = false;
            if (cleanRunId.equalsIgnoreCase(bug.getLinkedRunId()))
            {
                bug.setLinkedRunId(null);
                bugChanged = true;
            }
            if (cleanRunId.equalsIgnoreCase(bug.getRemovedRunId()))
            {
                bug.setRemovedRunId(null);
                bug.setRemovedAtMs(null);
                bugChanged = true;
            }
            if (bugChanged)
            {
                bugRepository.save(bug);
            }
        }
    }

    public List<AreaSummaryDto> buildAreaSummaries(final List<TestExecutionDto> executions)
    {
        if (executions == null || executions.isEmpty())
        {
            return new ArrayList<>();
        }

        final Map<String, List<TestExecutionDto>> byClass = executions.stream()
            .collect(Collectors.groupingBy(
                e -> (e.getTestClass() != null && !e.getTestClass().isEmpty()) ? e.getTestClass() : "GeneralTest",
                LinkedHashMap::new,
                Collectors.toList()
            ));

        final List<TestClassSummaryDto> testClasses = new ArrayList<>();
        for (final Map.Entry<String, List<TestExecutionDto>> entry : byClass.entrySet())
        {
            final String testClass = entry.getKey();
            final List<TestExecutionDto> classExecs = entry.getValue();

            int cPass = 0, cFixed = 0, cKnown = 0, cUnknown = 0, cIgnored = 0, cRunning = 0;
            for (final TestExecutionDto ce : classExecs)
            {
                final String status = ce.getStatus() != null ? ce.getStatus().trim().toLowerCase(Locale.ROOT) : "passed-clean";
                switch (status)
                {
                    case "passed-clean", "passed", "succeeded" -> cPass++;
                    case "succeeded-fixed", "fixed", "healed" -> cFixed++;
                    case "failed-known", "known" -> cKnown++;
                    case "failed-unknown", "failed", "error", "unknown" -> cUnknown++;
                    case "ignored", "skipped" -> cIgnored++;
                    case "running", "in_progress", "executing", "pending" -> cRunning++;
                    default -> cUnknown++;
                }
            }

            testClasses.add(new TestClassSummaryDto(
                testClass,
                "classContainer" + testClass.replace(".", ""),
                "Default",
                classExecs.size(),
                cPass, cFixed, cKnown, cUnknown, cIgnored, cRunning,
                classExecs
            ));
        }

        final Map<String, List<TestClassSummaryDto>> byArea = testClasses.stream()
            .collect(Collectors.groupingBy(
                tc -> {
                    if (!tc.getExecutions().isEmpty() && tc.getExecutions().get(0).getAreaName() != null && isRealArea(tc.getExecutions().get(0).getAreaName()))
                    {
                        return normalizeArea(tc.getExecutions().get(0).getAreaName());
                    }
                    return "Default";
                },
                LinkedHashMap::new,
                Collectors.toList()
            ));

        final List<AreaSummaryDto> areas = new ArrayList<>();
        for (final Map.Entry<String, List<TestClassSummaryDto>> areaEntry : byArea.entrySet())
        {
            final String areaName = areaEntry.getKey();
            final List<TestClassSummaryDto> areaClasses = areaEntry.getValue();

            int aPass = 0, aFixed = 0, aKnown = 0, aUnknown = 0, aIgnored = 0, aRunning = 0;
            for (final TestClassSummaryDto tc : areaClasses)
            {
                aPass += tc.getPassCount();
                aFixed += tc.getSucceededFixedCount();
                aKnown += tc.getFailedKnownCount();
                aUnknown += tc.getFailedUnknownCount();
                aIgnored += tc.getIgnoredCount();
                aRunning += tc.getRunningCount();
            }

            final List<SubAreaSummaryDto> subAreas = new ArrayList<>();
            final boolean hasSub = areaClasses.stream()
                .anyMatch(tc -> tc.getExecutions().stream().anyMatch(e -> e.getSubArea() != null && !e.getSubArea().isBlank()));

            if (hasSub)
            {
                final List<TestExecutionDto> allAreaExecs = areaClasses.stream()
                    .flatMap(tc -> tc.getExecutions().stream())
                    .toList();

                final Map<String, List<TestExecutionDto>> bySubArea = allAreaExecs.stream()
                    .collect(Collectors.groupingBy(
                        e -> {
                            if (e.getSubArea() != null && !e.getSubArea().isBlank())
                            {
                                String sn = e.getSubArea().trim();
                                while (sn.startsWith("@"))
                                {
                                    sn = sn.substring(1).trim();
                                }
                                return sn.isEmpty() ? "General" : sn;
                            }
                            return "General";
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                    ));

                for (final Map.Entry<String, List<TestExecutionDto>> subEntry : bySubArea.entrySet())
                {
                    final String subName = subEntry.getKey();
                    final List<TestExecutionDto> subExecs = subEntry.getValue();

                    final Map<String, List<TestExecutionDto>> subByClass = subExecs.stream()
                        .collect(Collectors.groupingBy(
                            e -> (e.getTestClass() != null && !e.getTestClass().isEmpty()) ? e.getTestClass() : "GeneralTest",
                            LinkedHashMap::new,
                            Collectors.toList()
                        ));

                    final List<TestClassSummaryDto> subClasses = new ArrayList<>();
                    int sPass = 0, sFixed = 0, sKnown = 0, sUnknown = 0, sIgnored = 0, sRunning = 0;
                    for (final Map.Entry<String, List<TestExecutionDto>> scEntry : subByClass.entrySet())
                    {
                        final String scName = scEntry.getKey();
                        final List<TestExecutionDto> scExecs = scEntry.getValue();

                        int cPass = 0, cFixed = 0, cKnown = 0, cUnknown = 0, cIgnored = 0, cRunning = 0;
                        for (final TestExecutionDto ce : scExecs)
                        {
                            final String status = ce.getStatus() != null ? ce.getStatus() : "passed-clean";
                            switch (status)
                            {
                                case "passed-clean", "passed", "succeeded" -> cPass++;
                                case "succeeded-fixed", "fixed", "healed" -> cFixed++;
                                case "failed-known", "known" -> cKnown++;
                                case "failed-unknown", "failed", "error", "unknown" -> cUnknown++;
                                case "ignored", "skipped" -> cIgnored++;
                                case "running", "in_progress", "executing", "pending" -> cRunning++;
                                default -> cPass++;
                            }
                        }
                        sPass += cPass;
                        sFixed += cFixed;
                        sKnown += cKnown;
                        sUnknown += cUnknown;
                        sIgnored += cIgnored;
                        sRunning += cRunning;

                        final String cleanSubName = subName.replace("@", "").replaceAll("[^a-zA-Z0-9]", "");
                        subClasses.add(new TestClassSummaryDto(
                            scName,
                            "classContainer" + scName.replace(".", "") + cleanSubName,
                            "Default",
                            scExecs.size(),
                            cPass, cFixed, cKnown, cUnknown, cIgnored, cRunning,
                            scExecs
                        ));
                    }

                    final String cleanSub = subName.replace("@", "").replaceAll("[^a-zA-Z0-9]", "");
                    final String cleanArea = areaName.replace("@", "").replaceAll("[^a-zA-Z0-9]", "");
                    subAreas.add(new SubAreaSummaryDto(
                        subName,
                        "subAreaGroup" + cleanArea + cleanSub,
                        sPass + sFixed + sKnown + sUnknown + sIgnored + sRunning,
                        sPass, sFixed, sKnown, sUnknown, sIgnored, sRunning,
                        subClasses
                    ));
                }
            }

            areas.add(new AreaSummaryDto(
                areaName,
                "areaGroup" + areaName.replace(" ", "").replace("@", ""),
                "Default",
                aPass + aFixed + aKnown + aUnknown + aIgnored + aRunning,
                aPass, aFixed, aKnown, aUnknown, aIgnored, aRunning,
                areaClasses,
                subAreas
            ));
        }
        return areas;
    }

    private RunReportDto buildRunReportDto(final TestRunEntity runEntity, final List<TestExecutionDto> executions)
    {
        if (runEntity == null)
        {
            return new RunReportDto(
                "", "Default", "N/A", "0s",
                0, 0, 0, 0, 0, 0,
                List.of(), List.of()
            );
        }

        int pass = 0, fixed = 0, known = 0, unknown = 0, ignored = 0, running = 0;
        for (final TestExecutionDto e : executions)
        {
            final String status = e.getStatus() != null ? e.getStatus().trim().toLowerCase(Locale.ROOT) : "passed-clean";
            switch (status)
            {
                case "passed-clean", "passed", "succeeded", "healed" -> pass++;
                case "succeeded-fixed", "fixed" -> fixed++;
                case "failed-known", "known" -> known++;
                case "failed-unknown", "failed", "error", "unknown" -> unknown++;
                case "ignored", "skipped" -> ignored++;
                case "running", "in_progress", "executing", "pending" -> running++;
                default -> unknown++;
            }
        }

        final int total = executions.size();

        final List<AreaSummaryDto> areas = buildAreaSummaries(executions);

        Map<String, String> tags = Collections.emptyMap();
        if (tagRepository != null && runEntity.getId() != null)
        {
            final List<TestRunTagEntity> tagEntities = tagRepository.findByRunId(runEntity.getId());
            if (tagEntities != null && !tagEntities.isEmpty())
            {
                tags = new LinkedHashMap<>();
                for (final TestRunTagEntity t : tagEntities)
                {
                    tags.put(t.getTagKey(), t.getTagValue());
                }
            }
        }

        return new RunReportDto(
            runEntity.getId(),
            runEntity.getBatchName(),
            runEntity.getTimestampLabel(),
            runEntity.getFormattedDuration() != null ? runEntity.getFormattedDuration() : "0s",
            !executions.isEmpty() ? total : (runEntity.getTotalTests() != null && runEntity.getTotalTests() > 0 ? runEntity.getTotalTests() : total),
            !executions.isEmpty() ? pass : (runEntity.getPassedCount() != null ? runEntity.getPassedCount() : pass),
            !executions.isEmpty() ? fixed : (runEntity.getSucceededFixedCount() != null ? runEntity.getSucceededFixedCount() : fixed),
            !executions.isEmpty() ? known : (runEntity.getFailedKnownCount() != null ? runEntity.getFailedKnownCount() : known),
            !executions.isEmpty() ? unknown : (runEntity.getFailedUnknownCount() != null ? runEntity.getFailedUnknownCount() : unknown),
            !executions.isEmpty() ? ignored : (runEntity.getIgnoredCount() != null ? runEntity.getIgnoredCount() : ignored),
            !executions.isEmpty() ? running : (runEntity.getRunningCount() != null ? runEntity.getRunningCount() : 0),
            executions, areas,
            runEntity.getTotalLlmCalls(),
            runEntity.getTotalLlmTokens(),
            runEntity.getTotalLlmCost(),
            tags
        );
    }

    private static final ThreadLocal<MessageDigest> SHA256_DIGEST = ThreadLocal.withInitial(() -> {
        try
        {
            return MessageDigest.getInstance("SHA-256");
        }
        catch (final NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    });

    /**
     * Canonical normalizer resolving raw execution status and metadata into effective reporting outcomes.
     *
     * @param rawStatus raw status string from execution JSON or runner (e.g. "passed", "finished", "error")
     * @param totalStepsCount total number of steps in execution
     * @param failedStepsCount number of failed steps
     * @param hasBugs whether open/linked bug tickets exist for this variation
     * @param failureOrErrorMessage failure/error identifier or message, if any
     * @param isRunActive whether the parent test run is currently active/in-progress
     * @return canonical effective status: "passed-clean", "succeeded-fixed", "failed-known", "failed-unknown", "ignored", or "running"
     */
    public static String computeEffectiveStatus(
            final String rawStatus,
            final int totalStepsCount,
            final int failedStepsCount,
            final boolean hasBugs,
            final String failureOrErrorMessage,
            final boolean isRunActive)
    {
        final String st = (rawStatus != null && !rawStatus.isBlank())
                ? rawStatus.trim().toLowerCase(Locale.ROOT)
                : (isRunActive ? "running" : "failed-unknown");

        final boolean hasFailure = failedStepsCount > 0
                || (failureOrErrorMessage != null
                    && !failureOrErrorMessage.isBlank()
                    && !"NONE".equalsIgnoreCase(failureOrErrorMessage.trim()));

        final boolean isKnown = hasBugs;
        final boolean isFixed = hasBugs;

        // 1. In-flight / transient states
        if ("running".equals(st) || "in_progress".equals(st) || "executing".equals(st) || "pending".equals(st))
        {
            if (isRunActive)
            {
                return "running";
            }
            if (hasFailure || totalStepsCount == 0)
            {
                return isKnown ? "failed-known" : "failed-unknown";
            }
            return "ignored";
        }

        // 2. Explicitly skipped states
        if ("ignored".equals(st) || "skipped".equals(st) || "cancelled".equals(st))
        {
            return "ignored";
        }

        // 3. Finished lifecycle resolution
        if ("finished".equals(st))
        {
            final boolean isSuccess = !hasFailure && (totalStepsCount > 0
                    || failureOrErrorMessage == null
                    || failureOrErrorMessage.isBlank()
                    || "NONE".equalsIgnoreCase(failureOrErrorMessage.trim()));
            return isSuccess ? (isFixed ? "succeeded-fixed" : "passed-clean")
                             : (isKnown ? "failed-known" : "failed-unknown");
        }

        // 4. Standard explicit failure verdicts
        if ("failed".equals(st) || "failed-known".equals(st) || "failed-unknown".equals(st) || "error".equals(st) || "failure".equals(st))
        {
            return isKnown ? "failed-known" : "failed-unknown";
        }

        // 5. Standard explicit pass verdicts
        if ("passed".equals(st) || "succeeded-fixed".equals(st) || "passed-clean".equals(st) || "succeeded".equals(st) || "healed".equals(st))
        {
            return isFixed ? "succeeded-fixed" : "passed-clean";
        }

        // 6. Unmapped / missing status fallback
        if (hasFailure || totalStepsCount == 0)
        {
            return isKnown ? "failed-known" : "failed-unknown";
        }
        return isFixed ? "succeeded-fixed" : "passed-clean";
    }

    public static String extractFailureText(final TestExecutionDto dto)
    {
        if (dto == null)
        {
            return null;
        }
        if (dto.getFailureReason() != null && !dto.getFailureReason().isBlank() && !"NONE".equalsIgnoreCase(dto.getFailureReason().trim()))
        {
            return dto.getFailureReason().trim();
        }
        if (dto.getFailure() != null && !dto.getFailure().isBlank() && !"NONE".equalsIgnoreCase(dto.getFailure().trim()))
        {
            return dto.getFailure().trim();
        }
        return null;
    }

    public static String extractFailureText(final JsonNode node)
    {
        if (node == null)
        {
            return null;
        }
        for (final String field : List.of("failureReason", "errorMessage", "error", "exception", "failure"))
        {
            if (node.hasNonNull(field))
            {
                final String txt = node.get(field).asText().trim();
                if (!txt.isBlank() && !"NONE".equalsIgnoreCase(txt))
                {
                    return txt;
                }
            }
        }
        return null;
    }

    public boolean isRunCurrentlyActiveOnDisk(final String runId)
    {
        return localRunJsonStorageService != null && runId != null && localRunJsonStorageService.isRunCurrentlyActiveOnDisk(runId.trim());
    }

    public static boolean isSameTestClass(final String c1, final String c2)
    {
        if (c1 == null || c2 == null)
        {
            return false;
        }
        if (c1.equalsIgnoreCase(c2))
        {
            return true;
        }
        final String s1 = extractSimpleClassName(c1);
        final String s2 = extractSimpleClassName(c2);
        return !s1.isEmpty() && s1.equalsIgnoreCase(s2);
    }

    public static String extractSimpleClassName(final String raw)
    {
        if (raw == null)
        {
            return "";
        }
        String s = raw.trim();
        if (s.endsWith(".json"))
        {
            s = s.substring(0, s.length() - 5);
        }
        if (s.contains("#"))
        {
            s = s.substring(0, s.indexOf('#'));
        }
        final int lastDot = s.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < s.length() - 1)
        {
            s = s.substring(lastDot + 1);
        }
        return s;
    }

    public static String normalizeLocation(final String raw)
    {
        if (raw == null || raw.trim().isEmpty())
        {
            return "Default";
        }
        final String loc = raw.trim();
        if ("unknown".equalsIgnoreCase(loc) || "default".equalsIgnoreCase(loc))
        {
            return "Default";
        }
        if (loc.length() == 2)
        {
            return loc.toUpperCase(Locale.ROOT);
        }
        return loc;
    }

    public static int countHistoryLinks(final String historyLinks)
    {
        if (historyLinks == null || historyLinks.trim().isEmpty())
        {
            return 0;
        }
        final String[] links = historyLinks.split(",");
        final java.util.Set<String> distinctRuns = new java.util.HashSet<>();
        for (final String link : links)
        {
            final String trimmed = link.trim();
            if (!trimmed.isEmpty())
            {
                distinctRuns.add(trimmed);
            }
        }
        return distinctRuns.size();
    }

    public static String normalizeBrowser(final String raw)
    {
        if (raw == null || raw.trim().isEmpty())
        {
            return "Chrome";
        }
        final String b = raw.trim();
        final String bLower = b.toLowerCase();
        if ("chrome".equals(bLower))
        {
            return "Chrome";
        }
        if ("firefox".equals(bLower) || "ff".equals(bLower))
        {
            return "Firefox";
        }
        if ("edge".equals(bLower))
        {
            return "Edge";
        }
        if ("safari".equals(bLower))
        {
            return "Safari";
        }
        if (Character.isLowerCase(b.charAt(0)))
        {
            return Character.toUpperCase(b.charAt(0)) + b.substring(1);
        }
        return b;
    }

    public static String normalizeArea(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return "Default";
        }
        String trimmed = raw.trim();
        while (trimmed.startsWith("@"))
        {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.isEmpty()
            || "General".equalsIgnoreCase(trimmed)
            || "Browsing (default)".equalsIgnoreCase(trimmed)
            || "Default".equalsIgnoreCase(trimmed))
        {
            return "Default";
        }
        return trimmed;
    }

    /**
     * Determines whether the given area string represents a real user-defined area rather than the
     * synthetic fallback default (@Browsing (default), General, or Default).
     *
     * @param raw area name or tag to evaluate
     * @return true if non-null, non-blank, and not equal to a fallback default
     */
    public static boolean isRealArea(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return false;
        }
        final String trimmed = raw.trim();
        return !"General".equalsIgnoreCase(trimmed)
            && !"@General".equalsIgnoreCase(trimmed)
            && !"Browsing (default)".equalsIgnoreCase(trimmed)
            && !"@Browsing (default)".equalsIgnoreCase(trimmed)
            && !"Default".equalsIgnoreCase(trimmed)
            && !"@Default".equalsIgnoreCase(trimmed);
    }

    /**
     * Determines whether the given environment string represents a real user-defined environment rather than
     * a synthetic fallback placeholder (Default, Unknown, ALL, or N/A).
     *
     * @param raw environment name or tag to evaluate
     * @return true if non-null, non-blank, and not equal to a fallback placeholder
     */
    public static boolean isRealEnvironment(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return false;
        }
        final String trimmed = raw.trim();
        return !"Default".equalsIgnoreCase(trimmed)
            && !"Unknown".equalsIgnoreCase(trimmed)
            && !"ALL".equalsIgnoreCase(trimmed)
            && !"N/A".equalsIgnoreCase(trimmed);
    }

    /**
     * Determines whether the given locale/location string represents a real user-defined locale rather than
     * a synthetic fallback placeholder (Default, Unknown, ALL, or N/A).
     *
     * @param raw locale name or tag to evaluate
     * @return true if non-null, non-blank, and not equal to a fallback placeholder
     */
    public static boolean isRealLocale(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return false;
        }
        final String trimmed = raw.trim();
        return !"Default".equalsIgnoreCase(trimmed)
            && !"Unknown".equalsIgnoreCase(trimmed)
            && !"ALL".equalsIgnoreCase(trimmed)
            && !"N/A".equalsIgnoreCase(trimmed);
    }

    /**
     * Determines whether the given batch/suite name represents a real user-defined batch rather than
     * a synthetic fallback placeholder (Default, Unknown, Queue, Suite, ALL, or N/A).
     *
     * @param raw batch name to evaluate
     * @return true if non-null, non-blank, and not equal to a fallback placeholder
     */
    public static boolean isRealBatch(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return false;
        }
        final String trimmed = raw.trim();
        return !"Default".equalsIgnoreCase(trimmed)
            && !"Unknown".equalsIgnoreCase(trimmed)
            && !"Queue".equalsIgnoreCase(trimmed)
            && !"Suite".equalsIgnoreCase(trimmed)
            && !"ALL".equalsIgnoreCase(trimmed)
            && !"N/A".equalsIgnoreCase(trimmed);
    }

    /**
     * Determines whether the given tag represents a real user-defined or meaningful tag rather than
     * a synthetic fallback placeholder (Default, Unknown, Queue for Suite, etc.).
     *
     * @param tag test run tag entity to evaluate
     * @return true if non-null, valid, and not a synthetic placeholder
     */
    public static boolean isLegitimateTag(final TestRunTagEntity tag)
    {
        if (tag == null || tag.getTagKey() == null || tag.getTagKey().isBlank()
            || tag.getTagValue() == null || tag.getTagValue().isBlank())
        {
            return false;
        }
        final String val = tag.getTagValue().trim();
        if ("Default".equalsIgnoreCase(val) || "Unknown".equalsIgnoreCase(val))
        {
            return false;
        }
        final String key = tag.getTagKey().trim();
        if (("suite".equalsIgnoreCase(key) || "batch".equalsIgnoreCase(key)) && !isRealBatch(val))
        {
            return false;
        }
        if (("environment".equalsIgnoreCase(key) || "env".equalsIgnoreCase(key)) && !isRealEnvironment(val))
        {
            return false;
        }
        return true;
    }

    /**
     * Resolves an effective batch or suite name from a raw batch string and an optional collection of tags.
     * If the batch name is a real user-defined batch (not a synthetic placeholder), it is returned.
     * Otherwise, looks for a "suite" or "batch" tag with a real batch name.
     * If none found, returns an empty string.
     *
     * @param batchName the raw batch name
     * @param tags collection of test run tags
     * @return the resolved batch name, or empty string if no valid name found
     */
    public static String resolveEffectiveBatchName(final String batchName, final Collection<TestRunTagEntity> tags)
    {
        if (isRealBatch(batchName))
        {
            return batchName.trim();
        }
        if (tags != null)
        {
            for (final TestRunTagEntity tag : tags)
            {
                if (tag != null && tag.getTagKey() != null)
                {
                    final String key = tag.getTagKey().trim();
                    if (("suite".equalsIgnoreCase(key) || "batch".equalsIgnoreCase(key)) && isRealBatch(tag.getTagValue()))
                    {
                        return tag.getTagValue().trim();
                    }
                }
            }
        }
        return "";
    }

    /**
     * Resolves an effective batch or suite name from a raw batch string and an optional tag map (e.g. from RunRequest).
     *
     * @param batchName the raw batch name
     * @param tagsMap map of tag key to list of tag values
     * @return the resolved batch name, or empty string if no valid name found
     */
    public static String resolveEffectiveBatchNameFromMap(final String batchName, final Map<String, List<String>> tagsMap)
    {
        if (isRealBatch(batchName))
        {
            return batchName.trim();
        }
        if (tagsMap != null)
        {
            for (final Map.Entry<String, List<String>> entry : tagsMap.entrySet())
            {
                if (entry.getKey() != null)
                {
                    final String key = entry.getKey().trim();
                    if ("suite".equalsIgnoreCase(key) || "batch".equalsIgnoreCase(key))
                    {
                        final List<String> values = entry.getValue();
                        if (values != null)
                        {
                            for (final String val : values)
                            {
                                if (isRealBatch(val))
                                {
                                    return val.trim();
                                }
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    /**
     * Resolves an effective batch or suite name from a raw batch string and an optional single-value tag map (e.g. from RunReportDto).
     *
     * @param batchName the raw batch name
     * @param tagsMap map of tag key to tag value
     * @return the resolved batch name, or empty string if no valid name found
     */
    public static String resolveEffectiveBatchName(final String batchName, final Map<String, String> tagsMap)
    {
        if (isRealBatch(batchName))
        {
            return batchName.trim();
        }
        if (tagsMap != null)
        {
            for (final Map.Entry<String, String> entry : tagsMap.entrySet())
            {
                if (entry.getKey() != null)
                {
                    final String key = entry.getKey().trim();
                    if (("suite".equalsIgnoreCase(key) || "batch".equalsIgnoreCase(key)) && isRealBatch(entry.getValue()))
                    {
                        return entry.getValue().trim();
                    }
                }
            }
        }
        return "";
    }

    public List<TestBaseVariationHistoryDto> getVariationHistory(
        final String targetTestClass,
        final String targetDataSet,
        final String targetLocation,
        final String targetBrowser)
    {
        return getVariationHistory(targetTestClass, null, targetDataSet, targetLocation, targetBrowser);
    }

    public List<TestBaseVariationHistoryDto> getVariationHistory(
        final String targetTestClass,
        final String targetTestMethod,
        final String targetDataSet,
        final String targetLocation,
        final String targetBrowser)
    {
        return getVariationHistory(targetTestClass, targetTestMethod, targetDataSet, targetLocation, targetBrowser, null);
    }

    public List<TestBaseVariationHistoryDto> getVariationHistory(
        final String targetTestClass,
        final String targetTestMethod,
        final String targetDataSet,
        final String targetLocation,
        final String targetBrowser,
        final Map<String, String> tagFilters)
    {
        final String normTargetBrowser = normalizeBrowser(targetBrowser);
        final String cleanTargetDataSet = cleanDataSetString(targetDataSet);
        final String primaryVarId;
        if (targetTestMethod != null && !targetTestMethod.trim().isEmpty())
        {
            primaryVarId = generateVariationId(targetTestClass, targetTestMethod, targetDataSet, targetLocation, targetBrowser);
        }
        else
        {
            primaryVarId = generateVariationId(targetTestClass, targetDataSet, targetLocation, targetBrowser);
        }

        final String legacyVarId = generateVariationId(targetTestClass, targetDataSet, targetLocation, targetBrowser);
        Optional<TestBaseVariationEntity> varOpt = variationRepository.findById(primaryVarId);
        if (varOpt.isEmpty() && !primaryVarId.equals(legacyVarId))
        {
            varOpt = variationRepository.findById(legacyVarId);
        }

        final List<TestRunEntity> candidateRuns;
        if (tagFilters != null && !tagFilters.isEmpty())
        {
            final List<String> matchingRunIds = findRunIdsByTags(tagFilters);
            if (matchingRunIds.isEmpty())
            {
                return List.of();
            }
            final Map<String, TestRunEntity> runMap = runRepository.findAllById(matchingRunIds).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .collect(Collectors.toMap(TestRunEntity::getId, r -> r, (a, b) -> a));
            candidateRuns = matchingRunIds.stream()
                .map(runMap::get)
                .filter(Objects::nonNull)
                .sorted((a, b) -> Long.compare(
                    b.getStartTimeMs() != null ? b.getStartTimeMs() : 0L,
                    a.getStartTimeMs() != null ? a.getStartTimeMs() : 0L))
                .collect(Collectors.toList());
        }
        else
        {
            candidateRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
        }

        final Map<String, Long> runStartTimes = candidateRuns.stream()
            .collect(Collectors.toMap(
                TestRunEntity::getId,
                r -> r.getStartTimeMs() != null ? r.getStartTimeMs() : 0L,
                (a, b) -> a
            ));

        final List<TestBaseBugEntity> varBugs = bugRepository.findByVariationIdIn(List.of(primaryVarId, legacyVarId));
        final List<TestBaseVariationHistoryDto> historyList = new ArrayList<>();

        for (final TestRunEntity run : candidateRuns)
        {
            final RunReportDto report = getRunReport(run.getId());
            if (report == null || report.getExecutions() == null)
            {
                continue;
            }

            for (final TestExecutionDto exec : report.getExecutions())
            {
                final String execClass = exec.getTestClass();
                if (targetTestClass != null && !targetTestClass.isEmpty() && execClass != null
                    && !targetTestClass.equalsIgnoreCase(execClass.trim()))
                {
                    continue;
                }

                final String execMethod = exec.getTestMethod();
                if (targetTestMethod != null && !targetTestMethod.trim().isEmpty() && execMethod != null && !execMethod.trim().isEmpty()
                    && !targetTestMethod.equalsIgnoreCase(execMethod.trim()))
                {
                    continue;
                }

                final String execData = cleanDataSetString(exec.getTitle());
                if (!cleanTargetDataSet.isEmpty() && !execData.isEmpty()
                    && !cleanTargetDataSet.equalsIgnoreCase(execData))
                {
                    continue;
                }

                if (targetLocation != null && !targetLocation.isEmpty() && !"Unknown".equalsIgnoreCase(targetLocation) && !"default".equalsIgnoreCase(targetLocation)
                    && exec.getLocation() != null && !exec.getLocation().isEmpty()
                    && !targetLocation.equalsIgnoreCase(exec.getLocation().trim()))
                {
                    continue;
                }

                final String execNormBrowser = normalizeBrowser(exec.getBrowser());
                if (!normTargetBrowser.equalsIgnoreCase(execNormBrowser))
                {
                    continue;
                }

                final String runBatch = run.getBatchName() != null ? run.getBatchName() : "ALL";
                final String runEnv = run.getEnvironment() != null ? run.getEnvironment() : "ALL";

                final List<TestBaseBugEntity> activeDbBugs = varBugs.stream()
                    .filter(b -> (b.getBatchName().equalsIgnoreCase("ALL") || b.getBatchName().equalsIgnoreCase(runBatch))
                              && (b.getEnvironment().equalsIgnoreCase("ALL") || b.getEnvironment().equalsIgnoreCase(runEnv))
                              && (b.getLinkedRunId() == null || isRunAtOrAfter(run.getId(), b.getLinkedRunId(), runStartTimes))
                              && (b.getRemovedRunId() == null || !isRunAtOrAfter(run.getId(), b.getRemovedRunId(), runStartTimes)))
                    .collect(Collectors.toList());

                final Set<String> removedTickets = varBugs.stream()
                    .filter(b -> (b.getBatchName().equalsIgnoreCase("ALL") || b.getBatchName().equalsIgnoreCase(runBatch))
                              && (b.getEnvironment().equalsIgnoreCase("ALL") || b.getEnvironment().equalsIgnoreCase(runEnv))
                              && b.getRemovedRunId() != null && isRunAtOrAfter(run.getId(), b.getRemovedRunId(), runStartTimes))
                    .map(b -> normalizeTicket(b.getBugTicket()))
                    .collect(Collectors.toSet());

                applyBugsToExecution(exec, activeDbBugs, removedTickets, runEnv, runBatch, getRunTagsMap(run));

                final List<String> bugsList = exec.getBugs() != null ? exec.getBugs() : Collections.emptyList();
                final boolean hasBugs = !bugsList.isEmpty();

                final String rawStatus = exec.getStatus() != null ? exec.getStatus() : "passed-clean";
                final String effectiveStatus;
                final boolean isRunActive = "IN_PROGRESS".equalsIgnoreCase(run.getStatus());
                if ("running".equalsIgnoreCase(rawStatus) || "in_progress".equalsIgnoreCase(rawStatus) || "executing".equalsIgnoreCase(rawStatus) || "pending".equalsIgnoreCase(rawStatus))
                {
                    effectiveStatus = isRunActive ? "running" : "skipped";
                }
                else if ("failed".equalsIgnoreCase(rawStatus) || "failed-known".equalsIgnoreCase(rawStatus) || "failed-unknown".equalsIgnoreCase(rawStatus) || "error".equalsIgnoreCase(rawStatus))
                {
                    effectiveStatus = hasBugs ? "failed-known" : "failed-unknown";
                }
                else if ("passed".equalsIgnoreCase(rawStatus) || "succeeded-fixed".equalsIgnoreCase(rawStatus) || "passed-clean".equalsIgnoreCase(rawStatus) || "succeeded".equalsIgnoreCase(rawStatus))
                {
                    effectiveStatus = hasBugs ? "succeeded-fixed" : "passed-clean";
                }
                else
                {
                    effectiveStatus = rawStatus;
                }

                final String[] badgeInfo = getStatusBadgeInfo(effectiveStatus);
                final String timestamp = run.getTimestampLabel() != null ? run.getTimestampLabel() : "Recently";
                final String engine = exec.getEngine() != null ? exec.getEngine() : "Java";
                final String mode = exec.getDisplayMode() != null && !exec.getDisplayMode().isBlank() ? exec.getDisplayMode() : (exec.getMode() != null && !exec.getMode().isBlank() ? exec.getMode() : "JAVA");

                final boolean isHealed = exec.isHealed();
                final boolean isPassedHealed = exec.isPassedHealed();
                final boolean isFailedHealed = exec.isFailedHealed();

                historyList.add(new TestBaseVariationHistoryDto(
                    run.getId(),
                    exec.getId() != null ? exec.getId() : "",
                    run.getBatchName(),
                    mode,
                    engine,
                    timestamp,
                    effectiveStatus,
                    badgeInfo[0],
                    badgeInfo[1],
                    bugsList,
                    isHealed,
                    isPassedHealed,
                    isFailedHealed
                ));
            }
        }
        if (!historyList.isEmpty())
        {
            historyList.sort(TestBaseVariationHistoryDto.BY_TIMESTAMP_DESC);
        }

        if ((tagFilters == null || tagFilters.isEmpty()) && varOpt.isPresent())
        {
            final TestBaseVariationEntity varEntity = varOpt.get();
            varEntity.setTotalExecutionsCount(historyList.size());
            if (!historyList.isEmpty())
            {
                final TestBaseVariationHistoryDto latest = historyList.get(0);
                varEntity.setLastStatus(latest.getStatus());
                varEntity.setHealed(latest.isHealed());
                if (latest.getMode() != null && !latest.getMode().isBlank())
                {
                    varEntity.setExecutionMode(latest.getMode());
                }
                final Optional<TestRunEntity> latestRunOpt = runRepository.findById(latest.getRunId());
                if (latestRunOpt.isPresent() && latestRunOpt.get().getStartTimeMs() != null)
                {
                    varEntity.setLastExecutedAt(latestRunOpt.get().getStartTimeMs());
                }
            }
            variationRepository.save(varEntity);
        }

        return historyList;
    }

    public List<String> findRunIdsByTags(final Map<String, String> tagFilters)
    {
        if (tagFilters == null || tagFilters.isEmpty())
        {
            return runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
                .map(TestRunEntity::getId)
                .collect(Collectors.toList());
        }
        if (tagRepository == null)
        {
            return List.of();
        }
        final List<String> compositeTags = tagFilters.entrySet().stream()
            .map(e -> e.getKey().trim() + ":::" + e.getValue().trim())
            .collect(Collectors.toList());
        final List<String> candidateRunIds = tagRepository.findRunIdsMatchingAllTags(compositeTags, (long) tagFilters.size());
        if (candidateRunIds.isEmpty())
        {
            return List.of();
        }
        final Set<String> activeRunIds = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
            .map(TestRunEntity::getId)
            .collect(Collectors.toSet());
        return candidateRunIds.stream()
            .filter(activeRunIds::contains)
            .collect(Collectors.toList());
    }

    public List<TestBaseVariationHistoryDto> getLatestVariationsByTags(final Map<String, String> tagFilters)
    {
        final List<String> runIds = findRunIdsByTags(tagFilters);
        if (runIds.isEmpty())
        {
            return List.of();
        }
        final Map<String, TestRunEntity> runMap = runRepository.findAllById(runIds).stream()
            .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
            .collect(Collectors.toMap(TestRunEntity::getId, r -> r, (a, b) -> a));

        final List<TestRunEntity> sortedRuns = runIds.stream()
            .map(runMap::get)
            .filter(Objects::nonNull)
            .sorted((a, b) -> Long.compare(
                b.getStartTimeMs() != null ? b.getStartTimeMs() : 0L,
                a.getStartTimeMs() != null ? a.getStartTimeMs() : 0L))
            .collect(Collectors.toList());

        final Map<String, TestBaseVariationHistoryDto> latestByVarKey = new LinkedHashMap<>();

        for (final TestRunEntity run : sortedRuns)
        {
            final RunReportDto report = getRunReport(run.getId());
            if (report == null || report.getExecutions() == null)
            {
                continue;
            }

            for (final TestExecutionDto exec : report.getExecutions())
            {
                final String testClass = (exec.getTestClass() != null && !exec.getTestClass().isBlank())
                    ? exec.getTestClass().trim()
                    : "GeneralTest";
                final String testMethod = (exec.getTestMethod() != null && !exec.getTestMethod().isBlank())
                    ? exec.getTestMethod().trim()
                    : "";
                final String dataSet = cleanDataSetString(exec.getTitle());

                if (!isValidTestVariation(testClass, testMethod, dataSet))
                {
                    continue;
                }

                final String varKey = generateVariationId(
                    testClass,
                    testMethod,
                    dataSet,
                    normalizeLocation(exec.getLocation()),
                    normalizeBrowser(exec.getBrowser())
                );

                if (!latestByVarKey.containsKey(varKey))
                {
                    final String rawStatus = exec.getStatus() != null ? exec.getStatus() : "passed-clean";
                    final String[] badgeInfo = getStatusBadgeInfo(rawStatus);
                    final String timestamp = run.getTimestampLabel() != null ? run.getTimestampLabel() : "Recently";
                    final String engine = exec.getEngine() != null ? exec.getEngine() : "Java";
                    final String mode = exec.getDisplayMode() != null && !exec.getDisplayMode().isBlank() ? exec.getDisplayMode() : (exec.getMode() != null && !exec.getMode().isBlank() ? exec.getMode() : "JAVA");

                    latestByVarKey.put(varKey, new TestBaseVariationHistoryDto(
                        run.getId(),
                        exec.getId() != null ? exec.getId() : "",
                        run.getBatchName(),
                        mode,
                        engine,
                        timestamp,
                        rawStatus,
                        badgeInfo[0],
                        badgeInfo[1],
                        exec.getBugs() != null ? exec.getBugs() : List.of(),
                        exec.isHealed(),
                        exec.isPassedHealed(),
                        exec.isFailedHealed()
                    ));
                }
            }
        }

        return new ArrayList<>(latestByVarKey.values());
    }

    public List<String> getAllTagKeys()
    {
        if (tagRepository == null)
        {
            return List.of();
        }
        return tagRepository.findDistinctTagKeys();
    }

    public Map<String, List<String>> getAllTags()
    {
        if (tagRepository == null)
        {
            return Collections.emptyMap();
        }
        final List<String> keys = tagRepository.findDistinctTagKeys();
        final Map<String, List<String>> result = new LinkedHashMap<>();
        for (final String key : keys)
        {
            result.put(key, tagRepository.findDistinctTagValuesByTagKey(key));
        }
        return result;
    }

    /**
     * Resolves all non-standard / custom tag dimensions and their distinct candidate values across
     * {@link #tagRepository} and {@link #getAllKnownTagMap()}.
     * Standard dimensions (browser, locale/location, environment/env, area/areatag) are excluded.
     * Original casing from the database is preserved for display labels, and entries are sorted case-insensitively.
     *
     * @return map of custom tag keys to their distinct sorted candidate values
     */
    public Map<String, List<String>> getCandidateCustomTags()
    {
        final Map<String, Set<String>> customTagMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        if (tagRepository != null)
        {
            final List<String> distinctKeys = tagRepository.findDistinctTagKeys();
            for (final String key : distinctKeys)
            {
                if (key == null || key.isBlank() || isStandardDimensionKey(key))
                {
                    continue;
                }
                final List<String> vals = tagRepository.findDistinctTagValuesByTagKey(key);
                if (vals != null && !vals.isEmpty())
                {
                    final Set<String> targetSet = customTagMap.computeIfAbsent(key.trim(), k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
                    for (final String v : vals)
                    {
                        if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v) && !"default".equalsIgnoreCase(v) && !"queue".equalsIgnoreCase(v) && !"all".equalsIgnoreCase(v))
                        {
                            targetSet.add(v.trim());
                        }
                    }
                }
            }
        }

        final Map<String, List<String>> allKnown = getAllKnownTagMap();
        for (final Map.Entry<String, List<String>> entry : allKnown.entrySet())
        {
            final String k = entry.getKey();
            if (k == null || k.isBlank() || isStandardDimensionKey(k))
            {
                continue;
            }
            final Set<String> targetSet = customTagMap.computeIfAbsent(k.trim(), key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
            if (entry.getValue() != null)
            {
                for (final String v : entry.getValue())
                {
                    if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v) && !"default".equalsIgnoreCase(v) && !"queue".equalsIgnoreCase(v) && !"all".equalsIgnoreCase(v))
                    {
                        targetSet.add(v.trim());
                    }
                }
            }
        }

        final Map<String, List<String>> result = new LinkedHashMap<>();
        for (final Map.Entry<String, Set<String>> entry : customTagMap.entrySet())
        {
            if (!entry.getValue().isEmpty())
            {
                result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        return result;
    }

    private static boolean isStandardDimensionKey(final String key)
    {
        if (key == null)
        {
            return false;
        }
        final String lower = key.trim().toLowerCase();
        return "browser".equals(lower)
            || "locale".equals(lower) || "location".equals(lower)
            || "environment".equals(lower) || "env".equals(lower) || "instance".equals(lower)
            || "area".equals(lower) || "areatag".equals(lower)
            || "batch".equals(lower) || "batchname".equals(lower);
    }

    public Map<String, List<String>> getAllKnownTagMap()
    {
        final Map<String, Set<String>> rawTagMap = new LinkedHashMap<>();

        final BiConsumer<String, String> addTag = (rawKey, rawVal) -> {
            if (rawKey == null || rawVal == null)
            {
                return;
            }
            String cleanVal = rawVal.trim();
            if (cleanVal.isEmpty() || "unknown".equalsIgnoreCase(cleanVal) || "default".equalsIgnoreCase(cleanVal) || "all".equalsIgnoreCase(cleanVal) || "queue".equalsIgnoreCase(cleanVal))
            {
                return;
            }
            final String cleanKey = rawKey.trim();
            final String lowerKey = cleanKey.toLowerCase();

            final String canonicalKey;
            if ("environment".equals(lowerKey) || "env".equals(lowerKey) || "instance".equals(lowerKey))
            {
                canonicalKey = "environment";
            }
            else if ("batch".equals(lowerKey) || "batchname".equals(lowerKey))
            {
                canonicalKey = "batch";
            }
            else if ("suite".equals(lowerKey))
            {
                canonicalKey = "Suite";
            }
            else if ("browser".equals(lowerKey))
            {
                canonicalKey = "browser";
            }
            else if ("location".equals(lowerKey) || "locale".equals(lowerKey))
            {
                canonicalKey = "location";
            }
            else if ("area".equals(lowerKey) || "areatag".equals(lowerKey))
            {
                canonicalKey = "area";
                while (cleanVal.startsWith("@"))
                {
                    cleanVal = cleanVal.substring(1).trim();
                }
            }
            else
            {
                canonicalKey = lowerKey;
            }

            if (!cleanVal.isEmpty() && !"unknown".equalsIgnoreCase(cleanVal) && !"default".equalsIgnoreCase(cleanVal) && !"all".equalsIgnoreCase(cleanVal))
            {
                rawTagMap.computeIfAbsent(canonicalKey, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(cleanVal);
            }
        };

        // 1. Query batchRepository
        if (batchRepository != null)
        {
            for (final TestBatchEntity b : batchRepository.findAll())
            {
                addTag.accept("environment", b.getEnvironment());
                addTag.accept("batch", b.getBatchName());
                if (b.getBrowsersCsv() != null)
                {
                    for (final String br : b.getBrowsersCsv().split(","))
                    {
                        addTag.accept("browser", br);
                    }
                }
                if (b.getLocalesCsv() != null)
                {
                    for (final String loc : b.getLocalesCsv().split(","))
                    {
                        addTag.accept("location", loc);
                    }
                }
            }
        }

        // 2. Query runRepository (non-deleted runs only)
        if (runRepository != null)
        {
            for (final TestRunEntity r : runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc())
            {
                addTag.accept("environment", r.getEnvironment());
                addTag.accept("batch", r.getBatchName());
                if (r.getBrowsersCsv() != null)
                {
                    for (final String br : r.getBrowsersCsv().split(","))
                    {
                        addTag.accept("browser", br);
                    }
                }
                if (r.getLocalesCsv() != null)
                {
                    for (final String loc : r.getLocalesCsv().split(","))
                    {
                        addTag.accept("location", loc);
                    }
                }
            }
        }

        // 3. Query variationRepository
        if (variationRepository != null)
        {
            for (final TestBaseVariationEntity v : variationRepository.findAll())
            {
                addTag.accept("environment", v.getLastEnvironment());
                addTag.accept("batch", v.getLastBatch());
                addTag.accept("browser", v.getBrowser());
                addTag.accept("location", v.getLocation());
                if (v.getAreaTag() != null)
                {
                    final String cleanArea = v.getAreaTag().replace("@", "").trim();
                    addTag.accept("area", cleanArea);
                }
            }
        }

        // 4. Query tagRepository (non-deleted runs only)
        if (tagRepository != null)
        {
            final List<String> distinctKeys = tagRepository.findDistinctTagKeys();
            for (final String key : distinctKeys)
            {
                for (final String val : tagRepository.findDistinctTagValuesByTagKey(key))
                {
                    addTag.accept(key, val);
                }
            }
        }

        // 6. Build sorted result: primary dimensions first, then custom keys
        final List<String> primaryDimensions = List.of("environment", "batch", "browser", "location", "area");
        final Map<String, List<String>> result = new LinkedHashMap<>();

        for (final String dim : primaryDimensions)
        {
            final Set<String> values = rawTagMap.remove(dim);
            if (values != null && !values.isEmpty())
            {
                result.put(dim, new ArrayList<>(values));
            }
        }

        final List<String> sortedCustomKeys = new ArrayList<>(rawTagMap.keySet());
        sortedCustomKeys.sort(String.CASE_INSENSITIVE_ORDER);
        for (final String customKey : sortedCustomKeys)
        {
            final Set<String> values = rawTagMap.get(customKey);
            if (values != null && !values.isEmpty())
            {
                result.put(customKey, new ArrayList<>(values));
            }
        }

        return result;
    }

    public List<String> getAllKnownTagsList()
    {
        final Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        final Map<String, List<String>> tagMap = getAllKnownTagMap();
        for (final Map.Entry<String, List<String>> entry : tagMap.entrySet())
        {
            final String key = entry.getKey();
            for (final String val : entry.getValue())
            {
                result.add(key + ": " + val);
            }
        }
        return new ArrayList<>(result);
    }

    public Map<String, List<String>> getCandidateTagValues(final Collection<String> runIds)
    {
        final Map<String, Set<String>> tagMap = new LinkedHashMap<>();
        if (tagRepository != null && runIds != null && !runIds.isEmpty())
        {
            final List<TestRunTagEntity> tags = tagRepository.findByRunIdIn(runIds);
            for (final TestRunTagEntity t : tags)
            {
                if (t.getTagKey() != null && !t.getTagKey().isBlank())
                {
                    final String key = t.getTagKey().trim();
                    String val = t.getTagValue() != null ? t.getTagValue().trim() : "";
                    if ("area".equalsIgnoreCase(key))
                    {
                        while (val.startsWith("@"))
                        {
                            val = val.substring(1).trim();
                        }
                    }
                    if (!val.isEmpty())
                    {
                        tagMap.computeIfAbsent(key, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(val);
                    }
                }
            }
        }
        final Map<String, List<String>> result = new LinkedHashMap<>();
        for (final Map.Entry<String, Set<String>> entry : tagMap.entrySet())
        {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    public Map<String, Map<String, Set<String>>> getRunTagsMap(final Collection<String> runIds)
    {
        final Map<String, Map<String, Set<String>>> runTags = new HashMap<>();
        if (tagRepository != null && runIds != null && !runIds.isEmpty())
        {
            final List<TestRunTagEntity> tags = tagRepository.findByRunIdIn(runIds);
            for (final TestRunTagEntity t : tags)
            {
                if (t.getRunId() != null && t.getTagKey() != null && t.getTagValue() != null)
                {
                    final String k = t.getTagKey().toLowerCase(Locale.ROOT);
                    String val = t.getTagValue().trim();
                    if ("area".equalsIgnoreCase(k))
                    {
                        while (val.startsWith("@"))
                        {
                            val = val.substring(1).trim();
                        }
                    }
                    runTags.computeIfAbsent(t.getRunId(), r -> new HashMap<>())
                        .computeIfAbsent(k, key -> new HashSet<>())
                        .add(val);
                }
            }
        }
        return runTags;
    }

    public Map<String, List<String>> getRunTagsMap(final String runId)
    {
        if (runId == null || runId.isBlank())
        {
            return Collections.emptyMap();
        }
        final Map<String, List<String>> result = new LinkedHashMap<>();
        if (tagRepository != null)
        {
            final List<TestRunTagEntity> tags = tagRepository.findByRunId(runId.trim());
            if (tags != null && !tags.isEmpty())
            {
                for (final TestRunTagEntity t : tags)
                {
                    if (t.getTagKey() != null && !t.getTagKey().isBlank() && t.getTagValue() != null)
                    {
                        final String k = t.getTagKey().toLowerCase(Locale.ROOT);
                        String val = t.getTagValue().trim();
                        if ("area".equalsIgnoreCase(k))
                        {
                            while (val.startsWith("@"))
                            {
                                val = val.substring(1).trim();
                            }
                        }
                        final List<String> list = result.computeIfAbsent(k, key -> new ArrayList<>());
                        if (!list.contains(val))
                        {
                            list.add(val);
                        }
                    }
                }
            }
        }
        if (localRunJsonStorageService != null)
        {
            try
            {
                final Path runDir = localRunJsonStorageService.getRunDir(runId.trim());
                if (Files.exists(runDir))
                {
                    final Path batchFile = runDir.resolve("batch.json");
                    if (Files.exists(batchFile))
                    {
                        final JsonNode batchNode = objectMapper.readTree(batchFile.toFile());
                        if (batchNode.has("name") && isRealBatch(batchNode.path("name").asText()))
                        {
                            final String bName = batchNode.path("name").asText().trim();
                            final List<String> list = result.computeIfAbsent("suite", k -> new ArrayList<>());
                            if (!list.contains(bName))
                            {
                                list.add(bName);
                            }
                        }
                        if (batchNode.has("tags") && batchNode.path("tags").isObject())
                        {
                            batchNode.path("tags").fields().forEachRemaining(entry -> {
                                final String k = entry.getKey().trim().toLowerCase(Locale.ROOT);
                                if (!k.isEmpty())
                                {
                                    final List<String> list = result.computeIfAbsent(k, key -> new ArrayList<>());
                                    if (entry.getValue().isArray())
                                    {
                                        for (final JsonNode item : entry.getValue())
                                        {
                                            final String s = item.asText("").trim();
                                            if (!s.isEmpty() && !list.contains(s)) list.add(s);
                                        }
                                    }
                                    else
                                    {
                                        final String s = entry.getValue().asText("").trim();
                                        if (!s.isEmpty() && !list.contains(s)) list.add(s);
                                    }
                                }
                            });
                        }
                    }
                    final Path runJsonFile = runDir.resolve("run.json");
                    if (Files.exists(runJsonFile))
                    {
                        final JsonNode runNode = objectMapper.readTree(runJsonFile.toFile());
                        if (runNode.has("tags") && runNode.path("tags").isObject())
                        {
                            runNode.path("tags").fields().forEachRemaining(entry -> {
                                final String k = entry.getKey().trim().toLowerCase(Locale.ROOT);
                                if (!k.isEmpty())
                                {
                                    final List<String> list = result.computeIfAbsent(k, key -> new ArrayList<>());
                                    if (entry.getValue().isArray())
                                    {
                                        for (final JsonNode item : entry.getValue())
                                        {
                                            final String s = item.asText("").trim();
                                            if (!s.isEmpty() && !list.contains(s)) list.add(s);
                                        }
                                    }
                                    else
                                    {
                                        final String s = entry.getValue().asText("").trim();
                                        if (!s.isEmpty() && !list.contains(s)) list.add(s);
                                    }
                                }
                            });
                        }
                    }
                }
            }
            catch (final Exception ignored)
            {
            }
        }
        return result;
    }

    public Map<String, List<String>> getRunTagsMap(final TestRunEntity run)
    {
        if (run == null)
        {
            return Collections.emptyMap();
        }
        if (run.getTags() != null && !run.getTags().isEmpty())
        {
            final Map<String, List<String>> result = new LinkedHashMap<>();
            for (final TestRunTagEntity t : run.getTags())
            {
                if (t.getTagKey() != null && !t.getTagKey().isBlank() && t.getTagValue() != null)
                {
                    final String k = t.getTagKey().toLowerCase(Locale.ROOT);
                    String val = t.getTagValue().trim();
                    if ("area".equalsIgnoreCase(k))
                    {
                        while (val.startsWith("@"))
                        {
                            val = val.substring(1).trim();
                        }
                    }
                    final List<String> list = result.computeIfAbsent(k, key -> new ArrayList<>());
                    if (!list.contains(val))
                    {
                        list.add(val);
                    }
                }
            }
            return result;
        }
        return getRunTagsMap(run.getId());
    }

    public void populateRunTags(final List<TestRunEntity> runs)
    {
        if (runs == null || runs.isEmpty())
        {
            return;
        }
        if (tagRepository != null)
        {
            final List<String> runIds = runs.stream().map(TestRunEntity::getId).filter(Objects::nonNull).toList();
            final List<TestRunTagEntity> allTagEntities = tagRepository.findByRunIdIn(runIds);
            final Map<String, List<TestRunTagEntity>> tagsByRunId = allTagEntities.stream()
                .filter(t -> t.getRunId() != null)
                .collect(Collectors.groupingBy(TestRunTagEntity::getRunId));
            for (final TestRunEntity r : runs)
            {
                if (r.getId() != null)
                {
                    final List<TestRunTagEntity> rTags = new ArrayList<>();
                    final Set<String> seenTagKeys = new HashSet<>();
                    if (tagsByRunId.containsKey(r.getId()))
                    {
                        for (final TestRunTagEntity t : tagsByRunId.get(r.getId()))
                        {
                            if (isLegitimateTag(t))
                            {
                                final String rawVal = t.getTagValue().trim();
                                String normVal = rawVal;
                                if (t.getTagKey() != null && "area".equalsIgnoreCase(t.getTagKey().trim()))
                                {
                                    while (normVal.startsWith("@"))
                                    {
                                        normVal = normVal.substring(1).trim();
                                    }
                                }
                                final String compKey = t.getTagKey().toLowerCase(Locale.ROOT) + "::" + normVal.toLowerCase(Locale.ROOT);
                                if (seenTagKeys.add(compKey))
                                {
                                    if (!normVal.equals(rawVal))
                                    {
                                        t.setTagValue(normVal);
                                    }
                                    rTags.add(t);
                                }
                            }
                        }
                    }
                    if (r.getTags() != null)
                    {
                        for (final TestRunTagEntity existing : r.getTags())
                        {
                            if (isLegitimateTag(existing))
                            {
                                final String rawVal = existing.getTagValue().trim();
                                String normVal = rawVal;
                                if (existing.getTagKey() != null && "area".equalsIgnoreCase(existing.getTagKey().trim()))
                                {
                                    while (normVal.startsWith("@"))
                                    {
                                        normVal = normVal.substring(1).trim();
                                    }
                                }
                                final String compKey = existing.getTagKey().toLowerCase(Locale.ROOT) + "::" + normVal.toLowerCase(Locale.ROOT);
                                if (seenTagKeys.add(compKey))
                                {
                                    if (!normVal.equals(rawVal))
                                    {
                                        existing.setTagValue(normVal);
                                    }
                                    rTags.add(existing);
                                }
                            }
                        }
                    }
                    final boolean hasSuiteTag = rTags.stream().anyMatch(t -> t.getTagKey() != null && ("suite".equalsIgnoreCase(t.getTagKey()) || "batch".equalsIgnoreCase(t.getTagKey())));
                    if (!hasSuiteTag)
                    {
                        final Map<String, List<String>> diskTags = getRunTagsMap(r.getId());
                        final String resolvedBatch = resolveEffectiveBatchNameFromMap(r.getBatchName(), diskTags);
                        if (isRealBatch(resolvedBatch))
                        {
                            rTags.add(0, new TestRunTagEntity(r.getId(), "Suite", resolvedBatch.trim()));
                            if (!isRealBatch(r.getBatchName()))
                            {
                                r.setBatchName(resolvedBatch.trim());
                            }
                        }
                        else if (isRealBatch(r.getBatchName()))
                        {
                            rTags.add(0, new TestRunTagEntity(r.getId(), "Suite", r.getBatchName().trim()));
                        }
                    }
                    final boolean hasEnvTag = rTags.stream().anyMatch(t -> t.getTagKey() != null && ("environment".equalsIgnoreCase(t.getTagKey()) || "env".equalsIgnoreCase(t.getTagKey())));
                    if (!hasEnvTag)
                    {
                        final Map<String, List<String>> diskTags = getRunTagsMap(r.getId());
                        final List<String> envVals = diskTags.getOrDefault("instance", diskTags.get("environment"));
                        final String resolvedEnv = (envVals != null && !envVals.isEmpty() && isRealEnvironment(envVals.get(0)))
                            ? envVals.get(0).trim()
                            : (isRealEnvironment(r.getEnvironment()) ? r.getEnvironment().trim() : null);
                        if (resolvedEnv != null)
                        {
                            rTags.add(new TestRunTagEntity(r.getId(), "Environment", resolvedEnv));
                        }
                    }
                    r.setTags(rTags);
                }
            }
        }
        else
        {
            for (final TestRunEntity r : runs)
            {
                final List<TestRunTagEntity> rTags = new ArrayList<>();
                final Set<String> seenTagKeys = new HashSet<>();
                if (r.getTags() != null)
                {
                    for (final TestRunTagEntity existing : r.getTags())
                    {
                        if (isLegitimateTag(existing))
                        {
                            final String rawVal = existing.getTagValue().trim();
                            String normVal = rawVal;
                            if (existing.getTagKey() != null && "area".equalsIgnoreCase(existing.getTagKey().trim()))
                            {
                                while (normVal.startsWith("@"))
                                {
                                    normVal = normVal.substring(1).trim();
                                }
                            }
                            final String compKey = existing.getTagKey().toLowerCase(Locale.ROOT) + "::" + normVal.toLowerCase(Locale.ROOT);
                            if (seenTagKeys.add(compKey))
                            {
                                if (!normVal.equals(rawVal))
                                {
                                    existing.setTagValue(normVal);
                                }
                                rTags.add(existing);
                            }
                        }
                    }
                }
                final boolean hasSuiteTag = rTags.stream().anyMatch(t -> t.getTagKey() != null && ("suite".equalsIgnoreCase(t.getTagKey()) || "batch".equalsIgnoreCase(t.getTagKey())));
                if (!hasSuiteTag)
                {
                    final Map<String, List<String>> diskTags = getRunTagsMap(r.getId());
                    final String resolvedBatch = resolveEffectiveBatchNameFromMap(r.getBatchName(), diskTags);
                    if (isRealBatch(resolvedBatch))
                    {
                        rTags.add(0, new TestRunTagEntity(r.getId(), "Suite", resolvedBatch.trim()));
                        if (!isRealBatch(r.getBatchName()))
                        {
                            r.setBatchName(resolvedBatch.trim());
                        }
                    }
                    else if (isRealBatch(r.getBatchName()))
                    {
                        rTags.add(0, new TestRunTagEntity(r.getId(), "Suite", r.getBatchName().trim()));
                    }
                }
                final boolean hasEnvTag = rTags.stream().anyMatch(t -> t.getTagKey() != null && ("environment".equalsIgnoreCase(t.getTagKey()) || "env".equalsIgnoreCase(t.getTagKey())));
                if (!hasEnvTag)
                {
                    final Map<String, List<String>> diskTags = getRunTagsMap(r.getId());
                    final List<String> envVals = diskTags.getOrDefault("instance", diskTags.get("environment"));
                    final String resolvedEnv = (envVals != null && !envVals.isEmpty() && isRealEnvironment(envVals.get(0)))
                        ? envVals.get(0).trim()
                        : (isRealEnvironment(r.getEnvironment()) ? r.getEnvironment().trim() : null);
                    if (resolvedEnv != null)
                    {
                        rTags.add(new TestRunTagEntity(r.getId(), "Environment", resolvedEnv));
                    }
                }
                r.setTags(rTags);
            }
        }
    }

    public static Map<String, String> parseQueryParams(final String url)
    {
        final Map<String, String> map = new HashMap<>();
        if (url != null && url.contains("?"))
        {
            final String query = url.substring(url.indexOf("?") + 1);
            for (final String param : query.split("&"))
            {
                final String[] kv = param.split("=", 2);
                if (kv.length == 2)
                {
                    try
                    {
                        final String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                        final String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        map.put(key, value);
                    }
                    catch (final Exception e)
                    {
                        map.put(kv[0], kv[1]);
                    }
                }
            }
        }
        return map;
    }

    public static String reconstructUrlWithParams(final String originalUrl, final Map<String, String> params)
    {
        final String base = originalUrl != null && originalUrl.contains("?")
            ? originalUrl.substring(0, originalUrl.indexOf("?"))
            : (originalUrl != null ? originalUrl : "");
        final StringBuilder sb = new StringBuilder(base);
        boolean first = true;
        for (final Map.Entry<String, String> entry : params.entrySet())
        {
            if (entry.getValue() == null || entry.getValue().isBlank())
            {
                continue;
            }
            sb.append(first ? "?" : "&");
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }

    public static String[] getStatusBadgeInfo(final String rawStatus)
    {
        final String status = rawStatus != null ? rawStatus : "passed-clean";
        final String statusClass;
        final String statusLabel;

        switch (status.toLowerCase())
        {
            case "running", "in_progress", "executing", "pending" ->
            {
                statusClass = "badge-running";
                statusLabel = "RUNNING";
            }
            case "succeeded-fixed", "fixed" ->
            {
                statusClass = "badge-fixed";
                statusLabel = "SUCCEEDED-FIXED";
            }
            case "healed", "passed-clean", "passed", "succeeded" ->
            {
                statusClass = "badge-pass";
                statusLabel = "PASSED";
            }
            case "failed-known", "known" ->
            {
                statusClass = "badge-known-fail";
                statusLabel = "KNOWN FAIL";
            }
            case "failed-unknown", "unknown", "failed", "error" ->
            {
                statusClass = "badge-unknown-fail";
                statusLabel = "FAIL";
            }
            case "ignored", "skipped" ->
            {
                statusClass = "badge-ignored";
                statusLabel = "SKIPPED";
            }
            case "not_run", "not run", "unreached" ->
            {
                statusClass = "badge-not-run";
                statusLabel = "NOT RUN";
            }
            default ->
            {
                statusClass = "badge-pass";
                statusLabel = status.toUpperCase();
            }
        }
        return new String[]{statusClass, statusLabel};
    }

    /**
     * Determines whether the given execution parameters correspond to a valid test variation
     * rather than a class- or suite-level placeholder/container event.
     *
     * @param testClass the test class name
     * @param testMethod the test method name
     * @param dataSet the dataset name
     * @return {@code true} if valid variation, {@code false} if container placeholder
     */
    public static boolean isValidTestVariation(final String testClass, final String testMethod, final String dataSet)
    {
        if (testMethod == null || testMethod.isBlank())
        {
            return false;
        }
        if (testClass != null && !testClass.isBlank() && dataSet != null && !dataSet.isBlank()
                && testClass.trim().equalsIgnoreCase(dataSet.trim()))
        {
            return false;
        }
        return true;
    }

    private static String cleanDataSetString(final String raw)
    {
        if (raw == null)
        {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("[Data Set:") && s.endsWith("]"))
        {
            s = s.substring(10, s.length() - 1).trim();
        }
        else if (s.startsWith("[") && s.endsWith("]"))
        {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    public static String generateVariationId(final String testClass, final String testMethod, final String dataSet, final String location, final String browser)
    {
        final String normLocation = normalizeLocation(location);
        final String normBrowser = normalizeBrowser(browser);
        final String raw = (testClass != null ? testClass.trim() : "") + "|" +
                           (testMethod != null ? testMethod.trim() : "") + "|" +
                           (dataSet != null ? dataSet.trim() : "") + "|" +
                           (normLocation != null ? normLocation : "") + "|" +
                           (normBrowser != null ? normBrowser : "");
        try
        {
            final MessageDigest md = SHA256_DIGEST.get();
            md.reset();
            final byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8; i++)
            {
                final int b = hash[i] & 0xff;
                if (b < 16) hex.append('0');
                hex.append(Integer.toHexString(b));
            }
            return "var_" + hex;
        }
        catch (final Exception e)
        {
            return "var_" + Math.abs(raw.hashCode());
        }
    }

    public static String generateVariationId(final String testClass, final String dataSet, final String location, final String browser)
    {
        return generateVariationId(testClass, null, dataSet, location, browser);
    }

    public static boolean matchesBugTags(
        final TestExecutionDto exec,
        final String environment,
        final String batchName,
        final Map<String, List<String>> runTags,
        final String tagCriteria)
    {
        if (exec == null)
        {
            return false;
        }
        if (tagCriteria == null || tagCriteria.isBlank())
        {
            return true;
        }
        final String effEnv = (environment != null && !environment.isBlank())
            ? environment
            : exec.getEnvironment();
        final String effBatch = (batchName != null && !batchName.isBlank())
            ? batchName
            : exec.getBatchName();

        final Map<String, List<String>> combinedTags = new LinkedHashMap<>();
        if (runTags != null && !runTags.isEmpty())
        {
            combinedTags.putAll(runTags);
        }
        if (exec.getTags() != null && !exec.getTags().isEmpty())
        {
            for (final Map.Entry<String, List<String>> entry : exec.getTags().entrySet())
            {
                combinedTags.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
            }
        }

        return TagUtils.matchesTagCriteria(
            combinedTags,
            exec.getJunitTags(),
            exec.getBrowser(),
            exec.getLocale(),
            effEnv,
            effBatch,
            tagCriteria
        );
    }

    public static boolean matchesBugTags(
        final TestExecutionDto exec,
        final String environment,
        final String batchName,
        final String tagCriteria)
    {
        return matchesBugTags(exec, environment, batchName, Collections.emptyMap(), tagCriteria);
    }

    public static boolean matchesBugTags(final TestExecutionDto exec, final String tagCriteria)
    {
        return matchesBugTags(exec, null, null, Collections.emptyMap(), tagCriteria);
    }

    private void applyBugsToExecution(
        final TestExecutionDto exec,
        final List<TestBaseBugEntity> candidateActiveBugs,
        final Set<String> removedTickets)
    {
        applyBugsToExecution(exec, candidateActiveBugs, removedTickets, null, null, Collections.emptyMap());
    }

    private void applyBugsToExecution(
        final TestExecutionDto exec,
        final List<TestBaseBugEntity> candidateActiveBugs,
        final Set<String> removedTickets,
        final String runEnv,
        final String runBatch)
    {
        applyBugsToExecution(exec, candidateActiveBugs, removedTickets, runEnv, runBatch, Collections.emptyMap());
    }

    private void applyBugsToExecution(
        final TestExecutionDto exec,
        final List<TestBaseBugEntity> candidateActiveBugs,
        final Set<String> removedTickets,
        final String runEnv,
        final String runBatch,
        final Map<String, List<String>> runTags)
    {
        if (exec == null)
        {
            return;
        }

        if (runEnv != null && !runEnv.isBlank() && exec.getEnvironment().isEmpty())
        {
            exec.setEnvironment(runEnv);
        }
        if (runBatch != null && !runBatch.isBlank() && exec.getBatchName().isEmpty())
        {
            exec.setBatchName(runBatch);
        }

        final List<TestBaseBugEntity> matchedBugs = new ArrayList<>();
        final Set<String> matchedNormTickets = new HashSet<>();
        if (candidateActiveBugs != null)
        {
            for (final TestBaseBugEntity b : candidateActiveBugs)
            {
                if (matchesBugTags(exec, runEnv, runBatch, runTags, b.getTagCriteria()))
                {
                    matchedBugs.add(b);
                    matchedNormTickets.add(normalizeTicket(b.getBugTicket()));
                }
            }
        }

        final Set<String> mismatchedTickets = new HashSet<>();
        final List<BugDetailDto> inactiveBugDetails = new ArrayList<>();
        if (candidateActiveBugs != null)
        {
            for (final TestBaseBugEntity b : candidateActiveBugs)
            {
                final String norm = normalizeTicket(b.getBugTicket());
                if (!matchedNormTickets.contains(norm))
                {
                    mismatchedTickets.add(norm);
                    if (removedTickets == null || !removedTickets.contains(norm))
                    {
                        final String sev = (b.getSeverity() != null && !b.getSeverity().isBlank()) ? b.getSeverity() : "MAJOR";
                        inactiveBugDetails.add(new BugDetailDto(b.getBugTicket(), sev, b.getTagCriteria()));
                    }
                }
            }
        }

        final Map<String, String> existingSeverities = new HashMap<>();
        if (exec.getBugDetails() != null)
        {
            for (final BugDetailDto detail : exec.getBugDetails())
            {
                if (detail != null && detail.getBugTicket() != null)
                {
                    existingSeverities.put(normalizeTicket(detail.getBugTicket()), detail.getSeverity());
                }
            }
        }

        final Map<String, BugDetailDto> finalDetailsMap = new LinkedHashMap<>();

        if (exec.getBugs() != null)
        {
            for (final String b : exec.getBugs())
            {
                if (b != null && !b.isBlank())
                {
                    final String norm = normalizeTicket(b);
                    if ((removedTickets == null || !removedTickets.contains(norm)) && !mismatchedTickets.contains(norm))
                    {
                        final String existingSev = existingSeverities.getOrDefault(norm, "MAJOR");
                        finalDetailsMap.put(norm, new BugDetailDto(b.trim(), existingSev, null));
                    }
                }
            }
        }

        for (final TestBaseBugEntity b : matchedBugs)
        {
            final String norm = normalizeTicket(b.getBugTicket());
            final String sev = (b.getSeverity() != null && !b.getSeverity().isBlank()) ? b.getSeverity() : "MAJOR";
            finalDetailsMap.put(norm, new BugDetailDto(b.getBugTicket(), sev, b.getTagCriteria()));
        }

        final List<BugDetailDto> bugDetailList = new ArrayList<>(finalDetailsMap.values());
        exec.setBugDetails(bugDetailList);
        exec.setInactiveBugDetails(inactiveBugDetails);
    }

    public List<String> getEffectiveExecutionIdentityDimensions(final List<String> explicitDimensions)
    {
        if (explicitDimensions != null && !explicitDimensions.isEmpty())
        {
            return explicitDimensions;
        }
        if (dashboardSettingService != null)
        {
            final List<String> dims = dashboardSettingService.getExecutionIdentityDimensions();
            if (dims != null && !dims.isEmpty())
            {
                return dims;
            }
        }
        return ReportDashboardSettingService.DEFAULT_EXECUTION_IDENTITY_DIMENSIONS;
    }

    /**
     * @deprecated Use {@link #getEffectiveExecutionIdentityDimensions(List)} instead.
     */
    @Deprecated
    public List<String> getEffectiveUniquenessDimensions(final List<String> explicitDimensions)
    {
        return getEffectiveExecutionIdentityDimensions(explicitDimensions);
    }

    public static boolean isDefaultExecutionIdentity(final List<String> dims)
    {
        if (dims == null || dims.size() != 2)
        {
            return false;
        }
        final Set<String> set = dims.stream()
            .map(ReportDashboardSettingService::normalizeDimensionName)
            .collect(Collectors.toSet());
        return set.contains("browser") && set.contains("locale");
    }

    /**
     * @deprecated Use {@link #isDefaultExecutionIdentity(List)} instead.
     */
    @Deprecated
    public static boolean isDefaultUniqueness(final List<String> dims)
    {
        return isDefaultExecutionIdentity(dims);
    }

    public String buildLogicalKey(final TestRunEntity run, final TestExecutionDto exec)
    {
        if (exec == null)
        {
            return "";
        }
        final String testClass = (exec.getTestClass() != null && !exec.getTestClass().isBlank())
            ? exec.getTestClass().trim()
            : "GeneralTest";
        final String testMethod = (exec.getTestMethod() != null && !exec.getTestMethod().isBlank())
            ? exec.getTestMethod().trim()
            : "";
        final String dataSet = cleanDataSetString(exec.getTitle());
        final String normLoc = normalizeLocation(exec.getLocation());
        final String normBr = normalizeBrowser(exec.getBrowser());
        return buildLogicalKey(testClass, testMethod, dataSet, normLoc, normBr, run, exec, null);
    }

    public String buildLogicalKey(
        final String testClass,
        final String testMethod,
        final String dataSet,
        final String normLoc,
        final String normBr,
        final TestRunEntity run,
        final List<String> uniquenessDimensions)
    {
        return buildLogicalKey(testClass, testMethod, dataSet, normLoc, normBr, run, null, uniquenessDimensions);
    }

    public String buildLogicalKey(
        final String testClass,
        final String testMethod,
        final String dataSet,
        final String normLoc,
        final String normBr,
        final TestRunEntity run,
        final TestExecutionDto exec,
        final List<String> uniquenessDimensions)
    {
        final List<String> dims = getEffectiveExecutionIdentityDimensions(uniquenessDimensions);
        final StringBuilder sb = new StringBuilder();
        sb.append(testClass != null ? testClass.trim() : "").append("|");
        sb.append(testMethod != null ? testMethod.trim() : "").append("|");
        sb.append(dataSet != null ? dataSet.trim() : "");

        for (final String dim : dims)
        {
            final String normDim = ReportDashboardSettingService.normalizeDimensionName(dim);
            if ("browser".equalsIgnoreCase(normDim))
            {
                sb.append("|br:").append(normBr != null && !normBr.isBlank() ? normBr : "Default");
            }
            else if ("locale".equalsIgnoreCase(normDim))
            {
                String loc = null;
                if (exec != null && exec.getTags() != null)
                {
                    final List<String> locTags = TagUtils.findTagValues(exec.getTags(), "locale");
                    if (locTags != null && !locTags.isEmpty())
                    {
                        loc = String.join("+", locTags);
                    }
                }
                if (loc == null && normLoc != null && !normLoc.isBlank())
                {
                    loc = normLoc;
                }
                sb.append("|loc:").append(loc != null ? loc : "Default");
            }
            else if ("environment".equalsIgnoreCase(normDim))
            {
                String env = null;
                if (exec != null && exec.getTags() != null)
                {
                    final List<String> envTags = TagUtils.findTagValues(exec.getTags(), "environment");
                    if (envTags != null && !envTags.isEmpty())
                    {
                        env = String.join("+", envTags);
                    }
                }
                if (env == null && run != null && run.getEnvironment() != null && !run.getEnvironment().isBlank())
                {
                    env = run.getEnvironment().trim();
                }
                if (env == null && run != null)
                {
                    final List<String> tagVals = getRunTagValues(run, "environment");
                    if (!tagVals.isEmpty())
                    {
                        env = tagVals.get(0);
                    }
                }
                sb.append("|env:").append(env != null ? env : "Default");
            }
            else
            {
                List<String> tagVals = null;
                if (exec != null && exec.getTags() != null)
                {
                    tagVals = TagUtils.findTagValues(exec.getTags(), normDim);
                }
                if (tagVals == null || tagVals.isEmpty())
                {
                    tagVals = (run != null) ? getRunTagValues(run, normDim) : Collections.emptyList();
                }
                final String tagVal = (tagVals == null || tagVals.isEmpty()) ? "Default" : String.join("+", tagVals);
                sb.append("|tag:").append(normDim).append("=").append(tagVal);
            }
        }
        return sb.toString();
    }

    public static String extractBrowserFromIdOrKey(final String idOrKey)
    {
        if (idOrKey == null || idOrKey.isBlank() || !idOrKey.contains("#"))
        {
            return null;
        }
        final String[] parts = idOrKey.split("#");
        if (parts.length >= 4 && !parts[3].isBlank())
        {
            return parts[3].trim();
        }
        if (parts.length == 3 && !parts[2].isBlank())
        {
            return parts[2].trim();
        }
        return null;
    }

    /**
     * Creates a scoped in-memory copy of a {@link TestRunEntity} whose test execution metrics
     * reflect only those executions matching the specified filter criteria (browsers, locales,
     * environments, areas, and tags). If no filters are provided, the original run is returned.
     * The returned entity is strictly transient and must not be persisted to the database.
     *
     * @param r the source test run entity
     * @param runBrowsers target browser criteria (or empty/null)
     * @param runLocales target locale criteria (or empty/null)
     * @param runEnvs target environment criteria (or empty/null)
     * @param runAreas target area criteria (or empty/null)
     * @param runTags target tag criteria (or empty/null)
     * @param bugsMap pre-grouped bug tickets by variation id (optional, null to query repository)
     * @return scoped transient {@link TestRunEntity}
     */
    public TestRunEntity scopeRunToCriteria(
        final TestRunEntity r,
        final Set<String> runBrowsers,
        final Set<String> runLocales,
        final Set<String> runEnvs,
        final Set<String> runAreas,
        final Set<String> runTags,
        final Map<String, List<String>> bugsMap)
    {
        return scopeRunToCriteria(
            r,
            runBrowsers,
            Collections.emptySet(),
            runLocales,
            Collections.emptySet(),
            runEnvs,
            Collections.emptySet(),
            runAreas,
            Collections.emptySet(),
            runTags,
            Collections.emptySet(),
            bugsMap,
            Collections.emptyMap(),
            Collections.emptyMap()
        );
    }

    public TestRunEntity scopeRunToCriteria(
        final TestRunEntity r,
        final Set<String> runBrowsers,
        final Set<String> runLocales,
        final Set<String> runEnvs,
        final Set<String> runAreas,
        final Set<String> runTags,
        final Map<String, List<String>> bugsMap,
        final Map<String, Set<String>> runTagsByType)
    {
        return scopeRunToCriteria(
            r,
            runBrowsers,
            Collections.emptySet(),
            runLocales,
            Collections.emptySet(),
            runEnvs,
            Collections.emptySet(),
            runAreas,
            Collections.emptySet(),
            runTags,
            Collections.emptySet(),
            bugsMap,
            runTagsByType,
            Collections.emptyMap()
        );
    }

    public TestRunEntity scopeRunToCriteria(
        final TestRunEntity r,
        final Set<String> runBrowsers,
        final Set<String> excludedRunBrowsers,
        final Set<String> runLocales,
        final Set<String> excludedRunLocales,
        final Set<String> runEnvs,
        final Set<String> excludedRunEnvs,
        final Set<String> runAreas,
        final Set<String> excludedRunAreas,
        final Set<String> runTags,
        final Set<String> excludedRunTags,
        final Map<String, List<String>> bugsMap,
        final Map<String, Set<String>> runTagsByType,
        final Map<String, Set<String>> excludedRunTagsByType)
    {
        if (r == null)
        {
            return null;
        }

        final boolean hasCriteria = (runBrowsers != null && !runBrowsers.isEmpty())
            || (excludedRunBrowsers != null && !excludedRunBrowsers.isEmpty())
            || (runLocales != null && !runLocales.isEmpty())
            || (excludedRunLocales != null && !excludedRunLocales.isEmpty())
            || (runEnvs != null && !runEnvs.isEmpty())
            || (excludedRunEnvs != null && !excludedRunEnvs.isEmpty())
            || (runAreas != null && !runAreas.isEmpty())
            || (excludedRunAreas != null && !excludedRunAreas.isEmpty())
            || (runTags != null && !runTags.isEmpty())
            || (excludedRunTags != null && !excludedRunTags.isEmpty())
            || (runTagsByType != null && !runTagsByType.isEmpty())
            || (excludedRunTagsByType != null && !excludedRunTagsByType.isEmpty());

        if (!hasCriteria)
        {
            return r;
        }

        if ((runBrowsers != null && runBrowsers.contains("_none_"))
            || (runLocales != null && runLocales.contains("_none_"))
            || (runEnvs != null && runEnvs.contains("_none_"))
            || (runAreas != null && runAreas.contains("_none_"))
            || (runTags != null && runTags.contains("_none_")))
        {
            return createEmptyScopedRun(r);
        }

        if (runEnvs != null && !runEnvs.isEmpty())
        {
            final String rEnv = (r.getEnvironment() != null && !r.getEnvironment().isBlank()) ? r.getEnvironment().trim() : "Default";
            final boolean matchEnv = runEnvs.stream().anyMatch(re -> re.equalsIgnoreCase(rEnv));
            if (!matchEnv)
            {
                return createEmptyScopedRun(r);
            }
        }
        if (excludedRunEnvs != null && !excludedRunEnvs.isEmpty())
        {
            final String rEnv = (r.getEnvironment() != null && !r.getEnvironment().isBlank()) ? r.getEnvironment().trim() : "Default";
            final boolean matchEnv = excludedRunEnvs.stream().anyMatch(re -> re.equalsIgnoreCase(rEnv));
            if (matchEnv)
            {
                return createEmptyScopedRun(r);
            }
        }

        if (runTags != null && !runTags.isEmpty())
        {
            final String rTag = (r.getBatchName() != null && !r.getBatchName().isBlank()) ? r.getBatchName().trim() : "Default";
            final boolean matchTag = runTags.stream().anyMatch(rt -> rt.equalsIgnoreCase(rTag));
            if (!matchTag)
            {
                return createEmptyScopedRun(r);
            }
        }
        if (excludedRunTags != null && !excludedRunTags.isEmpty())
        {
            final String rTag = (r.getBatchName() != null && !r.getBatchName().isBlank()) ? r.getBatchName().trim() : "Default";
            final boolean matchTag = excludedRunTags.stream().anyMatch(rt -> rt.equalsIgnoreCase(rTag));
            if (matchTag)
            {
                return createEmptyScopedRun(r);
            }
        }

        if (runTagsByType != null && !runTagsByType.isEmpty())
        {
            for (final Map.Entry<String, Set<String>> entry : runTagsByType.entrySet())
            {
                final String tagType = entry.getKey();
                final Set<String> selectedVals = entry.getValue();
                if (selectedVals == null || selectedVals.isEmpty())
                {
                    continue;
                }
                if (selectedVals.contains("_none_"))
                {
                    return createEmptyScopedRun(r);
                }
                if (!matchesTagCriteria(r, tagType, selectedVals))
                {
                    return createEmptyScopedRun(r);
                }
            }
        }
        if (excludedRunTagsByType != null && !excludedRunTagsByType.isEmpty())
        {
            for (final Map.Entry<String, Set<String>> entry : excludedRunTagsByType.entrySet())
            {
                final String tagType = entry.getKey();
                final Set<String> excludedVals = entry.getValue();
                if (excludedVals == null || excludedVals.isEmpty())
                {
                    continue;
                }
                if (isExcludedByTagCriteria(r, tagType, excludedVals))
                {
                    return createEmptyScopedRun(r);
                }
            }
        }

        final RunReportDto report = getRunReport(r.getId());
        if (report == null || report.getExecutions() == null || report.getExecutions().isEmpty())
        {
            return createEmptyScopedRun(r);
        }

        final Map<String, List<TestBaseBugEntity>> allBugsByVarId;
        if (bugsMap == null)
        {
            final List<TestBaseBugEntity> allBugs = (bugRepository != null) ? bugRepository.findAll() : Collections.emptyList();
            allBugsByVarId = allBugs.stream()
                .filter(b -> b.getRemovedRunId() == null)
                .collect(Collectors.groupingBy(TestBaseBugEntity::getVariationId));
        }
        else
        {
            allBugsByVarId = Collections.emptyMap();
        }

        int passClean = 0;
        int passHealed = 0;
        int passAi = 0;
        int fixedClean = 0;
        int fixedHealed = 0;
        int fixedAi = 0;
        int knownClean = 0;
        int knownHealed = 0;
        int knownAi = 0;
        int unknownClean = 0;
        int unknownHealed = 0;
        int unknownAi = 0;
        int ignored = 0;
        int flakyCount = 0;
        double scopedLlmCost = 0.0;
        long scopedLlmTokens = 0L;

        final Set<String> matchedBrowsers = new LinkedHashSet<>();
        final Set<String> matchedLocales = new LinkedHashSet<>();

        for (final TestExecutionDto exec : report.getExecutions())
        {
            final String testClass = (exec.getTestClass() != null && !exec.getTestClass().isBlank())
                ? exec.getTestClass().trim()
                : "GeneralTest";
            final String testMethod = (exec.getTestMethod() != null && !exec.getTestMethod().isBlank())
                ? exec.getTestMethod().trim()
                : "";
            final String dataSet = cleanDataSetString(exec.getTitle());
            if (!isValidTestVariation(testClass, testMethod, dataSet))
            {
                continue;
            }
            final String normLoc = normalizeLocation(exec.getLocation());
            final String normBr = normalizeBrowser(exec.getBrowser());
            final String normArea = normalizeArea(exec.getAreaName());

            if (runBrowsers != null && !runBrowsers.isEmpty())
            {
                final boolean matchBr = runBrowsers.stream().anyMatch(rb ->
                    rb.equalsIgnoreCase(normBr) || rb.equalsIgnoreCase(exec.getBrowser()) || normalizeBrowser(rb).equalsIgnoreCase(normBr)
                );
                if (!matchBr)
                {
                    continue;
                }
            }
            if (excludedRunBrowsers != null && !excludedRunBrowsers.isEmpty())
            {
                final boolean matchBr = excludedRunBrowsers.stream().anyMatch(eb ->
                    eb.equalsIgnoreCase(normBr) || eb.equalsIgnoreCase(exec.getBrowser()) || normalizeBrowser(eb).equalsIgnoreCase(normBr)
                );
                if (matchBr)
                {
                    continue;
                }
            }

            if (runLocales != null && !runLocales.isEmpty())
            {
                final boolean matchLoc = runLocales.stream().anyMatch(rl ->
                    rl.equalsIgnoreCase(normLoc) || rl.equalsIgnoreCase(exec.getLocation()) || normalizeLocation(rl).equalsIgnoreCase(normLoc)
                );
                if (!matchLoc)
                {
                    continue;
                }
            }
            if (excludedRunLocales != null && !excludedRunLocales.isEmpty())
            {
                final boolean matchLoc = excludedRunLocales.stream().anyMatch(el ->
                    el.equalsIgnoreCase(normLoc) || el.equalsIgnoreCase(exec.getLocation()) || normalizeLocation(el).equalsIgnoreCase(normLoc)
                );
                if (matchLoc)
                {
                    continue;
                }
            }

            if (runAreas != null && !runAreas.isEmpty())
            {
                final boolean matchArea = runAreas.stream().anyMatch(ra ->
                    normalizeArea(ra).equalsIgnoreCase(normArea)
                );
                if (!matchArea)
                {
                    continue;
                }
            }
            if (excludedRunAreas != null && !excludedRunAreas.isEmpty())
            {
                final boolean matchArea = excludedRunAreas.stream().anyMatch(ea ->
                    normalizeArea(ea).equalsIgnoreCase(normArea)
                );
                if (matchArea)
                {
                    continue;
                }
            }

            matchedBrowsers.add(normBr);
            matchedLocales.add(normLoc);

            final String canonicalId = generateVariationId(testClass, testMethod, dataSet, normLoc, normBr);
            final boolean hasBugs;
            if (bugsMap != null)
            {
                hasBugs = bugsMap.containsKey(canonicalId) && !bugsMap.get(canonicalId).isEmpty();
            }
            else
            {
                final List<TestBaseBugEntity> varBugs = allBugsByVarId.getOrDefault(canonicalId, Collections.emptyList());
                hasBugs = varBugs.stream().anyMatch(b -> matchesBugTags(exec, b.getEnvironment(), b.getBatchName(), b.getTagCriteria()));
            }
            final String rawStatus = exec.getStatus() != null ? exec.getStatus() : "passed-clean";
            final String effectiveStatus;
            if ("failed".equalsIgnoreCase(rawStatus) || "failed-unknown".equalsIgnoreCase(rawStatus) || "failed-known".equalsIgnoreCase(rawStatus) || "error".equalsIgnoreCase(rawStatus))
            {
                effectiveStatus = hasBugs ? "failed-known" : "failed-unknown";
            }
            else if ("passed".equalsIgnoreCase(rawStatus) || "succeeded-fixed".equalsIgnoreCase(rawStatus) || "passed-clean".equalsIgnoreCase(rawStatus) || "succeeded".equalsIgnoreCase(rawStatus) || "healed".equalsIgnoreCase(rawStatus))
            {
                effectiveStatus = hasBugs ? "succeeded-fixed" : "passed-clean";
            }
            else
            {
                effectiveStatus = rawStatus;
            }

            final String mode = (exec.getDisplayMode() != null && !exec.getDisplayMode().isBlank())
                ? exec.getDisplayMode()
                : ((exec.getMode() != null && !exec.getMode().isBlank()) ? exec.getMode() : "JAVA");
            final boolean healed = exec.isHealed();
            final boolean ai = "AI".equalsIgnoreCase(mode);

            if ("passed-clean".equalsIgnoreCase(effectiveStatus) || "passed".equalsIgnoreCase(effectiveStatus) || "succeeded".equalsIgnoreCase(effectiveStatus))
            {
                if (healed)
                {
                    passHealed++;
                }
                else if (ai)
                {
                    passAi++;
                }
                else
                {
                    passClean++;
                }
            }
            else if ("succeeded-fixed".equalsIgnoreCase(effectiveStatus) || "fixed".equalsIgnoreCase(effectiveStatus))
            {
                if (healed)
                {
                    fixedHealed++;
                }
                else if (ai)
                {
                    fixedAi++;
                }
                else
                {
                    fixedClean++;
                }
            }
            else if ("failed-known".equalsIgnoreCase(effectiveStatus) || "known".equalsIgnoreCase(effectiveStatus))
            {
                if (healed)
                {
                    knownHealed++;
                }
                else if (ai)
                {
                    knownAi++;
                }
                else
                {
                    knownClean++;
                }
            }
            else if ("failed-unknown".equalsIgnoreCase(effectiveStatus) || "unknown".equalsIgnoreCase(effectiveStatus) || "failed".equalsIgnoreCase(effectiveStatus) || "error".equalsIgnoreCase(effectiveStatus))
            {
                if (healed)
                {
                    unknownHealed++;
                }
                else if (ai)
                {
                    unknownAi++;
                }
                else
                {
                    unknownClean++;
                }
            }
            else
            {
                ignored++;
            }

            if (exec.isFlaky())
            {
                flakyCount++;
            }

            scopedLlmCost += exec.getLlmCost();
            scopedLlmTokens += exec.getLlmTotalTokens();
        }

        final TestRunEntity scopedRun = new TestRunEntity();
        scopedRun.setId(r.getId());
        scopedRun.setBatchName(r.getBatchName());
        scopedRun.setStatus(r.getStatus());
        scopedRun.setTriggerSource(r.getTriggerSource());
        scopedRun.setEnvironment(r.getEnvironment());
        scopedRun.setTimestampLabel(r.getTimestampLabel());
        scopedRun.setStartTimeMs(r.getStartTimeMs());
        scopedRun.setEndTimeMs(r.getEndTimeMs());
        scopedRun.setRunComment(r.getRunComment());
        scopedRun.setRunJsonPath(r.getRunJsonPath());
        scopedRun.setIsDeleted(false);
        scopedRun.setTags(r.getTags());

        scopedRun.setPassedHealedCount(passHealed);
        scopedRun.setPassedAiCount(passAi);
        final int runPassed = passClean + passHealed + passAi;
        scopedRun.setPassedCount(runPassed);

        scopedRun.setSucceededFixedHealedCount(fixedHealed);
        scopedRun.setSucceededFixedAiCount(fixedAi);
        final int runFixed = fixedClean + fixedHealed + fixedAi;
        scopedRun.setSucceededFixedCount(runFixed);

        scopedRun.setFailedKnownHealedCount(knownHealed);
        scopedRun.setFailedKnownAiCount(knownAi);
        final int runKnown = knownClean + knownHealed + knownAi;
        scopedRun.setFailedKnownCount(runKnown);

        scopedRun.setFailedUnknownHealedCount(unknownHealed);
        scopedRun.setFailedUnknownAiCount(unknownAi);
        final int runUnknown = unknownClean + unknownHealed + unknownAi;
        scopedRun.setFailedUnknownCount(runUnknown);

        scopedRun.setIgnoredCount(ignored);
        scopedRun.setFlakyCount(flakyCount);
        final int runTotal = runPassed + runFixed + runKnown + runUnknown + ignored;
        scopedRun.setTotalTests(runTotal);
        scopedRun.setPassRate(runTotal > 0 ? ((double) (runPassed + runFixed) / runTotal * 100.0) : 0.0);
        scopedRun.setTotalLlmCost(Math.ceil(scopedLlmCost * 1000000.0) / 1000000.0);
        scopedRun.setTotalLlmTokens(scopedLlmTokens);

        if (!matchedBrowsers.isEmpty())
        {
            scopedRun.setBrowsersCsv(String.join(",", matchedBrowsers));
        }
        else
        {
            scopedRun.setBrowsersCsv(r.getBrowsersCsv());
        }

        if (!matchedLocales.isEmpty())
        {
            scopedRun.setLocalesCsv(String.join(",", matchedLocales));
        }
        else
        {
            scopedRun.setLocalesCsv(r.getLocalesCsv());
        }

        return scopedRun;
    }

    public TestRunEntity scopeRunToCriteria(
        final TestRunEntity r,
        final Set<String> runBrowsers,
        final Set<String> runLocales,
        final Set<String> runEnvs,
        final Set<String> runAreas,
        final Set<String> runTags)
    {
        return scopeRunToCriteria(r, runBrowsers, runLocales, runEnvs, runAreas, runTags, null, Collections.emptyMap());
    }

    private TestRunEntity createEmptyScopedRun(final TestRunEntity r)
    {
        final TestRunEntity scopedRun = new TestRunEntity();
        scopedRun.setId(r.getId());
        scopedRun.setBatchName(r.getBatchName());
        scopedRun.setStatus(r.getStatus());
        scopedRun.setTriggerSource(r.getTriggerSource());
        scopedRun.setEnvironment(r.getEnvironment());
        scopedRun.setTimestampLabel(r.getTimestampLabel());
        scopedRun.setStartTimeMs(r.getStartTimeMs());
        scopedRun.setEndTimeMs(r.getEndTimeMs());
        scopedRun.setRunComment(r.getRunComment());
        scopedRun.setRunJsonPath(r.getRunJsonPath());
        scopedRun.setIsDeleted(false);
        scopedRun.setTags(r.getTags());
        scopedRun.setPassedHealedCount(0);
        scopedRun.setPassedAiCount(0);
        scopedRun.setPassedCount(0);
        scopedRun.setSucceededFixedHealedCount(0);
        scopedRun.setSucceededFixedAiCount(0);
        scopedRun.setSucceededFixedCount(0);
        scopedRun.setFailedKnownHealedCount(0);
        scopedRun.setFailedKnownAiCount(0);
        scopedRun.setFailedKnownCount(0);
        scopedRun.setFailedUnknownHealedCount(0);
        scopedRun.setFailedUnknownAiCount(0);
        scopedRun.setFailedUnknownCount(0);
        scopedRun.setIgnoredCount(0);
        scopedRun.setFlakyCount(0);
        scopedRun.setTotalTests(0);
        scopedRun.setTotalLlmCost(0.0);
        scopedRun.setTotalLlmTokens(0L);
        scopedRun.setBrowsersCsv(r.getBrowsersCsv());
        scopedRun.setLocalesCsv(r.getLocalesCsv());
        scopedRun.setPassRate(0.0);
        return scopedRun;
    }

    private static final class ScopedRunStats
    {
        private int passClean;
        private int passHealed;
        private int passAi;
        private int fixedClean;
        private int fixedHealed;
        private int fixedAi;
        private int knownClean;
        private int knownHealed;
        private int knownAi;
        private int unknownClean;
        private int unknownHealed;
        private int unknownAi;
        private int ignored;
        private int flakyCount;
        private final Set<String> browsers = new LinkedHashSet<>();
        private final Set<String> locales = new LinkedHashSet<>();
    }

    private record RunVariationCost(String logicalKey, double llmCost, long llmTokens)
    {
    }

    private record VariationRunState(String status, boolean healed, String mode, boolean flaky)
    {
    }
}

