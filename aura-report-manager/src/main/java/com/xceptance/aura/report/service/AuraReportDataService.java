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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import com.xceptance.aura.report.dto.AreaSummaryDto;
import com.xceptance.aura.report.dto.BatchOverviewDataDto;
import com.xceptance.aura.report.dto.BatchSummaryDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
import com.xceptance.aura.report.dto.TestBaseVariationHistoryDto;
import com.xceptance.aura.report.dto.TestClassSummaryDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
    private final LocalRunJsonStorageService localRunJsonStorageService;
    private final AttachmentStorageService attachmentStorageService;
    private final ObjectMapper objectMapper;

    private final Map<String, List<TestExecutionDto>> liveRunBuffer = new ConcurrentHashMap<>();
    private final Map<String, RunReportDto> runReportCache = new ConcurrentHashMap<>();

    @Autowired
    public AuraReportDataService(
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository,
        final LocalRunJsonStorageService localRunJsonStorageService,
        final AttachmentStorageService attachmentStorageService)
    {
        this.runRepository = runRepository;
        this.batchRepository = batchRepository;
        this.variationRepository = variationRepository;
        this.bugRepository = bugRepository;
        this.localRunJsonStorageService = localRunJsonStorageService;
        this.attachmentStorageService = attachmentStorageService;
        this.objectMapper = new ObjectMapper();
    }

    public void clearCache()
    {
        runReportCache.clear();
    }

    @Transactional
    public void clearAllDatabaseEntries()
    {
        bugRepository.deleteAllInBatch();
        variationRepository.deleteAllInBatch();
        batchRepository.deleteAllInBatch();
        runRepository.deleteAllInBatch();
        runReportCache.clear();
        LOG.info("Removed all entries from all database tables (TestBaseBug, TestBaseVariation, TestBatch, TestRun).");
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
                    if (latestReport != null && latestReport.getExecutions() != null)
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
                List.of(b.getLocalesCsv() != null ? b.getLocalesCsv().split(", ") : new String[0]),
                List.of(b.getBrowsersCsv() != null ? b.getBrowsersCsv().split(", ") : new String[0]),
                latestRunId,
                latestPassed,
                latestFixed,
                latestKnown,
                latestUnknown,
                latestIgnored,
                latestTotal
            ));
        }
        return batches;
    }

    public BatchOverviewDataDto getBatchOverviewData()
    {
        final List<TestBatchEntity> allBatchEntities = batchRepository.findAll();
        final List<TestRunEntity> allRunEntities = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();

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

    public TestBaseDataDto getTestBaseData()
    {
        final List<TestBaseVariationEntity> variations = variationRepository.findAllByOrderByLastExecutedAtDesc();
        final List<TestBaseBugEntity> allBugs = bugRepository.findAll();

        final Map<String, List<String>> bugsMap = allBugs.stream()
            .filter(b -> b.getRemovedRunId() == null)
            .collect(Collectors.groupingBy(
                TestBaseBugEntity::getVariationId,
                Collectors.mapping(TestBaseBugEntity::getBugTicket, Collectors.toList())
            ));

        for (final TestBaseVariationEntity var : variations)
        {
            final boolean hasBugs = (bugsMap.containsKey(var.getId()) && !bugsMap.get(var.getId()).isEmpty())
                                 || hasBugsInLatestHistoryLink(var.getHistoryLinks());
            final String curStatus = var.getLastStatus() != null ? var.getLastStatus() : "passed-clean";
            String effectiveLastStatus = curStatus;

            if ("failed".equalsIgnoreCase(curStatus) || "failed-unknown".equalsIgnoreCase(curStatus) || "failed-known".equalsIgnoreCase(curStatus) || "error".equalsIgnoreCase(curStatus))
            {
                effectiveLastStatus = hasBugs ? "failed-known" : "failed-unknown";
            }
            else if ("passed".equalsIgnoreCase(curStatus) || "succeeded-fixed".equalsIgnoreCase(curStatus) || "passed-clean".equalsIgnoreCase(curStatus) || "succeeded".equalsIgnoreCase(curStatus))
            {
                effectiveLastStatus = hasBugs ? "succeeded-fixed" : "passed-clean";
            }

            if (!effectiveLastStatus.equalsIgnoreCase(curStatus))
            {
                var.setLastStatus(effectiveLastStatus);
                variationRepository.save(var);
            }
        }

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

        // 4. Dynamic Grouping by Area & Test Class
        final Map<String, List<TestBaseVariationEntity>> byArea = variations.stream()
            .collect(Collectors.groupingBy(
                v -> {
                    String area = v.getAreaTag();
                    if (area == null || area.trim().isEmpty() || "@General".equalsIgnoreCase(area.trim()))
                    {
                        return "@Browsing (default)";
                    }
                    if (!area.startsWith("@"))
                    {
                        area = "@" + area;
                    }
                    return area;
                },
                LinkedHashMap::new,
                Collectors.toList()
            ));

        final List<TestBaseAreaDto> areas = new ArrayList<>();
        int totalTestClassesCount = 0;
        final int totalVariationsCount = variations.size();

        for (final Map.Entry<String, List<TestBaseVariationEntity>> areaEntry : byArea.entrySet())
        {
            final String areaName = areaEntry.getKey();
            final List<TestBaseVariationEntity> areaVars = areaEntry.getValue();

            final Map<String, List<TestBaseVariationEntity>> byClass = areaVars.stream()
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
                final String classContainerId = "classContainer" + className.replaceAll("[^a-zA-Z0-9]", "");

                classDtos.add(new TestBaseClassDto(
                    className,
                    classContainerId,
                    classVars.size(),
                    classVars
                ));
            }

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

    public RunReportDto getRunReport(final String runId)
    {
        if (runId != null && "#RUN_ID".equalsIgnoreCase(runId.trim()))
        {
            return new RunReportDto(
                "#RUN_ID", "Unknown", "N/A", "0s",
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
                "", "Unknown", "N/A", "0s",
                0, 0, 0, 0, 0, 0,
                List.of(), List.of()
            );
        }

        if (runReportCache.containsKey(effectiveRunId))
        {
            LOG.debug("[PERF] getRunReport cache HIT for runId={}", effectiveRunId);
            return runReportCache.get(effectiveRunId);
        }

        synchronized (effectiveRunId.intern())
        {
            if (runReportCache.containsKey(effectiveRunId))
            {
                LOG.debug("[PERF] getRunReport cache HIT (after sync lock) for runId={}", effectiveRunId);
                return runReportCache.get(effectiveRunId);
            }

            final long getReportStartMs = System.currentTimeMillis();

            final Optional<TestRunEntity> runOpt = runRepository.findById(effectiveRunId);
            final TestRunEntity runEntity = runOpt.orElseGet(() -> new TestRunEntity(
                effectiveRunId,
                "Unknown",
                "COMPLETED",
                "Unknown",
                "Unknown",
                "Unknown",
                "Chrome",
                "Recently",
                System.currentTimeMillis()
            ));

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
                                final TestExecutionDto exec = objectMapper.treeToValue(entry.getValue(), TestExecutionDto.class);
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

            if (rawExecutions.isEmpty() && liveRunBuffer.containsKey(effectiveRunId))
            {
                rawExecutions = liveRunBuffer.get(effectiveRunId);
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

            final Map<String, List<String>> dbBugsByVarId = allRunBugs.stream()
                .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(effectiveRunId, b.getLinkedRunId(), runStartTimes))
                          && (b.getRemovedRunId() == null || !isRunAtOrAfter(effectiveRunId, b.getRemovedRunId(), runStartTimes)))
                .collect(Collectors.groupingBy(
                    TestBaseBugEntity::getVariationId,
                    Collectors.mapping(TestBaseBugEntity::getBugTicket, Collectors.toList())
                ));

            final Map<String, java.util.Set<String>> removedBugsByVarId = allRunBugs.stream()
                .filter(b -> b.getRemovedRunId() != null && isRunAtOrAfter(effectiveRunId, b.getRemovedRunId(), runStartTimes))
                .collect(Collectors.groupingBy(
                    TestBaseBugEntity::getVariationId,
                    Collectors.mapping(b -> normalizeTicket(b.getBugTicket()), Collectors.toSet())
                ));

            for (final TestExecutionDto exec : rawExecutions)
            {
                // Omit heavy step trees and screenshots on initial report opening (loaded on demand when clicked)
                exec.setSteps(null);
                exec.setBlocks(null);

                final String varId = generateVariationId(exec.getTestClass(), exec.getTestMethod(), exec.getTitle(), exec.getLocation(), exec.getBrowser());
                final List<String> dbBugTickets = dbBugsByVarId.getOrDefault(varId, List.of()).stream()
                    .distinct()
                    .collect(Collectors.toList());

                final java.util.Set<String> removedTickets = removedBugsByVarId.getOrDefault(varId, java.util.Set.of());
                final java.util.Set<String> combinedBugs = new java.util.LinkedHashSet<>();
                if (exec.getBugs() != null)
                {
                    for (final String b : exec.getBugs())
                    {
                        if (!removedTickets.contains(normalizeTicket(b)))
                        {
                            combinedBugs.add(b);
                        }
                    }
                }
                combinedBugs.addAll(dbBugTickets);

                final List<String> bugTickets = new ArrayList<>(combinedBugs);
                exec.setBugs(bugTickets);

                final boolean hasBugs = !bugTickets.isEmpty();
                final String raw = exec.getStatus() != null ? exec.getStatus() : "passed";
                if ("failed".equalsIgnoreCase(raw) || "failed-known".equalsIgnoreCase(raw) || "failed-unknown".equalsIgnoreCase(raw))
                {
                    exec.setStatus(hasBugs ? "failed-known" : "failed-unknown");
                }
                else if ("passed".equalsIgnoreCase(raw) || "succeeded-fixed".equalsIgnoreCase(raw) || "passed-clean".equalsIgnoreCase(raw))
                {
                    exec.setStatus(hasBugs ? "succeeded-fixed" : "passed-clean");
                }
                else if ("fixed".equalsIgnoreCase(raw) || "healed".equalsIgnoreCase(raw))
                {
                    exec.setStatus("succeeded-fixed");
                }
                else
                {
                    exec.setStatus(raw.toLowerCase());
                }

                executions.add(exec);
            }

            final RunReportDto report = buildRunReportDto(runEntity, executions);
            runReportCache.put(effectiveRunId, report);

            final long getReportMs = System.currentTimeMillis() - getReportStartMs;
            LOG.info("[PERF] getRunReport cache MISS built report for runId={} with {} executions in {} ms",
                effectiveRunId, executions.size(), getReportMs);

            return report;
        }
    }

    public TestExecutionDto getExecutionDetails(final String runId, final String executionId)
    {
        if (runId == null || executionId == null || executionId.isBlank())
        {
            return null;
        }

        final Path runDir = localRunJsonStorageService.getRunDir(runId);
        if (Files.exists(runDir) && Files.isDirectory(runDir))
        {
            final List<File> files = new ArrayList<>();
            localRunJsonStorageService.scanForTestExecJsonFiles(runDir.toFile(), files);
            for (final File f : files)
            {
                try
                {
                    final JsonNode node = objectMapper.readTree(f);
                    final String id = node.path("id").asText(f.getName().replaceAll("\\.json$", ""));
                    final String testClass = node.path("testClass").asText("DefaultClass");
                    final String testMethod = LocalRunJsonStorageService.extractTestMethod(node);
                    final String title = node.path("title").asText("Default");
                    final String browser = node.path("browser").asText("Chrome");

                    final String execKey = LocalRunJsonStorageService.buildExecutionKey(testClass, testMethod, title, browser);
                    final String fullKey = testClass + "#" + id + "#" + browser;
                    final String execKeyWithId = execKey + "#" + id;
                    final String classMethodBrowser = testClass + "#" + testMethod + "#" + browser;
                    final String fileName = f.getName();
                    final String fileNameNoExt = fileName.replaceAll("\\.json$", "");

                    if (executionId.equalsIgnoreCase(id)
                            || executionId.equalsIgnoreCase(fullKey)
                            || executionId.equalsIgnoreCase(execKey)
                            || executionId.equalsIgnoreCase(execKeyWithId)
                            || executionId.equalsIgnoreCase(classMethodBrowser)
                            || executionId.equalsIgnoreCase(fileNameNoExt)
                            || executionId.equalsIgnoreCase(fileName))
                    {
                        final TestExecutionDto dto = objectMapper.treeToValue(node, TestExecutionDto.class);
                        if (dto.getId() == null || dto.getId().isBlank()
                                || "bad".equalsIgnoreCase(dto.getId())
                                || "perfect".equalsIgnoreCase(dto.getId())
                                || "default".equalsIgnoreCase(dto.getId()))
                        {
                            dto.setId(executionId);
                        }
                        resolveBugsForExecution(runId, dto);
                        return dto;
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
        }

        final Optional<String> runJsonOpt = localRunJsonStorageService.readRunJson(runId);
        if (runJsonOpt.isPresent())
        {
            try
            {
                final JsonNode root = objectMapper.readTree(runJsonOpt.get());
                final JsonNode execMetrics = root.path("executionMetrics");
                if (execMetrics.isObject() && !execMetrics.isEmpty())
                {
                    for (final java.util.Map.Entry<String, JsonNode> entry : (Iterable<java.util.Map.Entry<String, JsonNode>>) () -> execMetrics.fields())
                    {
                        final JsonNode node = entry.getValue();
                        final String id = node.path("id").asText(entry.getKey());
                        if (executionId.equalsIgnoreCase(id) || executionId.equalsIgnoreCase(entry.getKey()))
                        {
                            final TestExecutionDto dto = objectMapper.treeToValue(node, TestExecutionDto.class);
                            resolveBugsForExecution(runId, dto);
                            return dto;
                        }
                    }
                }
                final JsonNode execArray = root.path("executions");
                if (execArray.isArray())
                {
                    for (final JsonNode execNode : execArray)
                    {
                        if (executionId.equalsIgnoreCase(execNode.path("id").asText("")))
                        {
                            final TestExecutionDto dto = objectMapper.treeToValue(execNode, TestExecutionDto.class);
                            resolveBugsForExecution(runId, dto);
                            return dto;
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                LOG.error("Failed reading execution details from run.json for runId={}, execId={}: {}", runId, executionId, e.getMessage());
            }
        }

        return null;
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

        final List<String> dbBugTickets = allRunBugs.stream()
            .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(runId, b.getLinkedRunId(), runStartTimes))
                      && (b.getRemovedRunId() == null || !isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes)))
            .filter(b -> varId.equals(b.getVariationId()))
            .map(TestBaseBugEntity::getBugTicket)
            .distinct()
            .collect(Collectors.toList());

        final java.util.Set<String> removedTickets = allRunBugs.stream()
            .filter(b -> b.getRemovedRunId() != null && isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes))
            .filter(b -> varId.equals(b.getVariationId()))
            .map(b -> normalizeTicket(b.getBugTicket()))
            .collect(Collectors.toSet());

        final java.util.Set<String> combinedBugs = new java.util.LinkedHashSet<>();
        if (dto.getBugs() != null)
        {
            for (final String b : dto.getBugs())
            {
                if (!removedTickets.contains(normalizeTicket(b)))
                {
                    combinedBugs.add(b);
                }
            }
        }
        combinedBugs.addAll(dbBugTickets);
        dto.setBugs(new ArrayList<>(combinedBugs));
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

        final Map<String, List<String>> dbBugsByVarId = allRunBugs.stream()
            .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(runId, b.getLinkedRunId(), runStartTimes))
                      && (b.getRemovedRunId() == null || !isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes)))
            .collect(Collectors.groupingBy(
                TestBaseBugEntity::getVariationId,
                Collectors.mapping(TestBaseBugEntity::getBugTicket, Collectors.toList())
            ));

        final Map<String, java.util.Set<String>> removedBugsByVarId = allRunBugs.stream()
            .filter(b -> b.getRemovedRunId() != null && isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes))
            .collect(Collectors.groupingBy(
                TestBaseBugEntity::getVariationId,
                Collectors.mapping(b -> normalizeTicket(b.getBugTicket()), Collectors.toSet())
            ));

        int pass = 0, fixed = 0, known = 0, unknown = 0, ignored = 0;
        final List<TestExecutionDto> updatedExecutions = new ArrayList<>();

        for (final TestExecutionDto exec : cachedReport.getExecutions())
        {
            final String varId = generateVariationId(exec.getTestClass(), exec.getTestMethod(), exec.getTitle(), exec.getLocation(), exec.getBrowser());
            final List<String> dbBugTickets = dbBugsByVarId.getOrDefault(varId, List.of()).stream()
                .distinct()
                .collect(Collectors.toList());

            final java.util.Set<String> removedTickets = removedBugsByVarId.getOrDefault(varId, java.util.Set.of());
            final java.util.Set<String> combinedBugs = new java.util.LinkedHashSet<>();
            if (exec.getBugs() != null)
            {
                for (final String b : exec.getBugs())
                {
                    if (!removedTickets.contains(normalizeTicket(b)))
                    {
                        combinedBugs.add(b);
                    }
                }
            }
            combinedBugs.addAll(dbBugTickets);

            final List<String> bugTickets = new ArrayList<>(combinedBugs);
            exec.setBugs(bugTickets);

            final boolean hasBugs = !bugTickets.isEmpty();
            final String raw = exec.getStatus() != null ? exec.getStatus() : "passed";
            if ("failed".equalsIgnoreCase(raw) || "failed-known".equalsIgnoreCase(raw) || "failed-unknown".equalsIgnoreCase(raw))
            {
                exec.setStatus(hasBugs ? "failed-known" : "failed-unknown");
            }
            else if ("passed".equalsIgnoreCase(raw) || "succeeded-fixed".equalsIgnoreCase(raw) || "passed-clean".equalsIgnoreCase(raw))
            {
                exec.setStatus(hasBugs ? "succeeded-fixed" : "passed-clean");
            }
            else
            {
                exec.setStatus("ignored");
            }

            final String st = exec.getStatus();
            if ("passed-clean".equalsIgnoreCase(st) || "passed".equalsIgnoreCase(st)) pass++;
            else if ("succeeded-fixed".equalsIgnoreCase(st)) fixed++;
            else if ("failed-known".equalsIgnoreCase(st)) known++;
            else if ("failed-unknown".equalsIgnoreCase(st) || "failed".equalsIgnoreCase(st)) unknown++;
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
            updatedExecutions,
            updatedAreas,
            cachedReport.getTotalLlmCalls(),
            cachedReport.getTotalLlmTokens(),
            cachedReport.getTotalLlmCost(),
            cachedReport.isInProgress()
        );

        runReportCache.put(runId, updatedReport);

        runEntity.setTotalTests(updatedExecutions.size());
        runEntity.setPassedCount(pass);
        runEntity.setSucceededFixedCount(fixed);
        runEntity.setFailedKnownCount(known);
        runEntity.setFailedUnknownCount(unknown);
        runEntity.setIgnoredCount(ignored);
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
                if (!statusFilter.equalsIgnoreCase(exec.getStatus()))
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
    public String startRun(final String batchName, final String environment, final String triggerSource)
    {
        final String runId = String.valueOf(System.currentTimeMillis() / 1000);
        final String timestampLabel = "Just Now";

        final TestRunEntity newRun = new TestRunEntity(
            runId,
            batchName != null ? batchName : "Unknown",
            "IN_PROGRESS",
            triggerSource != null ? triggerSource : "Unknown",
            environment != null ? environment : "Unknown",
            "Unknown",
            "Chrome",
            timestampLabel,
            System.currentTimeMillis()
        );

        runRepository.save(newRun);
        liveRunBuffer.put(runId, new ArrayList<>());

        LOG.info("Started new test run: runId={}, batch={}", runId, batchName);
        return runId;
    }

    @Transactional
    public String startRun(final String runId, final String batchName, final String environment, final String triggerSource)
    {
        if (runId == null || runId.isEmpty())
        {
            return startRun(batchName, environment, triggerSource);
        }

        final String timestampLabel = "Just Now";

        final TestRunEntity newRun = new TestRunEntity(
            runId,
            batchName != null ? batchName : "Unknown",
            "IN_PROGRESS",
            triggerSource != null ? triggerSource : "Unknown",
            environment != null ? environment : "Unknown",
            "Unknown",
            "Chrome",
            timestampLabel,
            System.currentTimeMillis()
        );

        runRepository.save(newRun);
        liveRunBuffer.put(runId, new ArrayList<>());

        LOG.info("Started new test run with explicit runId={}, batch={}", runId, batchName);
        return runId;
    }

    @Transactional
    public void ingestExecution(final String runId, final Map<String, Object> payload)
    {
        try
        {
            final TestExecutionDto dto = objectMapper.convertValue(payload, TestExecutionDto.class);
            final List<TestExecutionDto> list = liveRunBuffer.computeIfAbsent(runId, k -> new ArrayList<>());
            list.add(dto);

            if (dto.getTestClass() != null && !dto.getTestClass().isEmpty())
            {
                final String varId = generateVariationId(dto.getTestClass(), dto.getTestMethod(), dto.getTitle(), dto.getLocation(), dto.getBrowser());
                final TestBaseVariationEntity var = variationRepository.findById(varId)
                    .orElseGet(() -> new TestBaseVariationEntity(varId, dto.getTestClass(), dto.getTestMethod(), dto.getTitle(), "@" + dto.getAreaName(), dto.getLocation(), dto.getBrowser()));

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
                    final String varId = generateVariationId(dto.getTestClass(), dto.getTestMethod(), dto.getTitle(), dto.getLocation(), dto.getBrowser());
                    final List<TestBaseBugEntity> activeBugs = bugRepository.findByVariationIdAndBatchNameInAndEnvironmentIn(
                        varId, List.of(runBatch, "ALL"), List.of(runEnv, "ALL")).stream()
                        .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(runId, b.getLinkedRunId()))
                                  && (b.getRemovedRunId() == null || !isRunAtOrAfter(runId, b.getRemovedRunId())))
                        .collect(Collectors.toList());

                    if (!activeBugs.isEmpty())
                    {
                        final List<String> bugTickets = activeBugs.stream()
                            .map(TestBaseBugEntity::getBugTicket)
                            .distinct()
                            .collect(Collectors.toList());

                        final java.util.Set<String> combined = new java.util.LinkedHashSet<>();
                        if (dto.getBugs() != null)
                        {
                            combined.addAll(dto.getBugs());
                        }
                        combined.addAll(bugTickets);
                        dto.setBugs(new ArrayList<>(combined));
                    }
                }

                final boolean hasBugs = dto.getBugs() != null && !dto.getBugs().isEmpty();
                final String raw = dto.getStatus() != null ? dto.getStatus() : "passed";
                String effectiveStatus = raw;
                if ("failed".equalsIgnoreCase(raw) || "failed-known".equalsIgnoreCase(raw) || "failed-unknown".equalsIgnoreCase(raw))
                {
                    effectiveStatus = hasBugs ? "failed-known" : "failed-unknown";
                }
                else if ("passed".equalsIgnoreCase(raw) || "succeeded-fixed".equalsIgnoreCase(raw) || "passed-clean".equalsIgnoreCase(raw))
                {
                    effectiveStatus = hasBugs ? "succeeded-fixed" : "passed-clean";
                }
                else
                {
                    effectiveStatus = "ignored";
                }
                dto.setStatus(effectiveStatus);

                if (dto.getTestClass() != null && !dto.getTestClass().isEmpty())
                {
                    final String varId = generateVariationId(dto.getTestClass(), dto.getTestMethod(), dto.getTitle(), dto.getLocation(), dto.getBrowser());
                    final Optional<TestBaseVariationEntity> varOpt = variationRepository.findById(varId);
                    if (varOpt.isPresent())
                    {
                        final TestBaseVariationEntity varEntity = varOpt.get();
                        varEntity.setLastStatus(effectiveStatus);
                        variationRepository.save(varEntity);
                    }
                }

                run.setTotalTests(run.getTotalTests() + 1);
                switch (effectiveStatus)
                {
                    case "passed-clean" -> run.setPassedCount(run.getPassedCount() + 1);
                    case "succeeded-fixed" -> run.setSucceededFixedCount(run.getSucceededFixedCount() + 1);
                    case "failed-known" -> run.setFailedKnownCount(run.getFailedKnownCount() + 1);
                    case "failed-unknown" -> run.setFailedUnknownCount(run.getFailedUnknownCount() + 1);
                    case "ignored" -> run.setIgnoredCount(run.getIgnoredCount() + 1);
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
            run.setStatus("COMPLETED");

            final List<TestExecutionDto> executions = liveRunBuffer.getOrDefault(runId, List.of());
            try
            {
                run.setRunJsonPath("storage/runs/" + runId);
                runRepository.save(run);

                final Path runDir = localRunJsonStorageService.getRunDir(runId);
                final Path batchJsonPath = runDir.resolve("batch.json");
                if (!Files.exists(batchJsonPath))
                {
                    final ObjectNode batchNode = objectMapper.createObjectNode();
                    batchNode.put("name", run.getBatchName() != null ? run.getBatchName() : "Unknown");
                    batchNode.put("description", "");
                    batchNode.put("environment", run.getEnvironment() != null ? run.getEnvironment() : "Unknown");
                    Files.createDirectories(batchJsonPath.getParent());
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(batchJsonPath.toFile(), batchNode);
                }

                try
                {
                    localRunJsonStorageService.buildRunJsonContent(runDir.toFile(), runId, true);
                }
                catch (final Exception e)
                {
                    LOG.warn("Failed to rebuild run.json for finished runId {}: {}", runId, e.getMessage());
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
    public TestExecutionDto addBugToExecution(final String runId, final String rowId, final String bugTicket)
    {
        final long startMs = System.currentTimeMillis();
        if (bugTicket == null || bugTicket.trim().isEmpty())
        {
            return new TestExecutionDto();
        }

        final String cleanTicket = bugTicket.trim().replaceAll("^#+", "");

        try
        {
            final long step1Start = System.currentTimeMillis();
            final RunReportDto report = getRunReport(runId);
            final long step1Ms = System.currentTimeMillis() - step1Start;

            final Optional<TestExecutionDto> targetOpt = report.getExecutions().stream()
                .filter(e -> rowId.equalsIgnoreCase(e.getId()))
                .findFirst();

            if (targetOpt.isPresent())
            {
                final TestExecutionDto target = targetOpt.get();
                final String varId = generateVariationId(target.getTestClass(), target.getTestMethod(), target.getTitle(), target.getLocation(), target.getBrowser());
                final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
                final String runBatch = runOpt.map(TestRunEntity::getBatchName).orElse("ALL");
                final String runEnv = runOpt.map(TestRunEntity::getEnvironment).orElse("ALL");
                final long runStartTime = runOpt.map(TestRunEntity::getStartTimeMs).orElse(System.currentTimeMillis());

                final long dbStart = System.currentTimeMillis();
                final List<TestBaseBugEntity> existing = bugRepository.findByVariationIdAndBatchNameInAndEnvironmentIn(
                    varId, List.of(runBatch, "ALL"), List.of(runEnv, "ALL"));
                final boolean existsActive = existing.stream()
                    .anyMatch(b -> normalizeTicket(b.getBugTicket()).equals(normalizeTicket(cleanTicket)) && b.getRemovedRunId() == null);

                if (!existsActive)
                {
                    bugRepository.save(new TestBaseBugEntity(varId, cleanTicket, runBatch, runEnv, runStartTime, runId));
                }
                final long dbMs = System.currentTimeMillis() - dbStart;

                final long reevalStart = System.currentTimeMillis();
                reevaluateBatchRunsFrom(runBatch, runStartTime, varId);
                final long syncMs = System.currentTimeMillis() - reevalStart;

                final long finalReportStart = System.currentTimeMillis();
                final RunReportDto updatedReport = getRunReport(runId);
                final long finalReportMs = System.currentTimeMillis() - finalReportStart;

                final long totalMs = System.currentTimeMillis() - startMs;
                LOG.info("[PERF] addBugToExecution total={} ms (initialReport={} ms, dbSave={} ms, syncUpdate={} ms, finalReport={} ms) runId={}, rowId={}, ticket={}",
                    totalMs, step1Ms, dbMs, syncMs, finalReportMs, runId, rowId, cleanTicket);

                return updatedReport.getExecutions().stream()
                    .filter(e -> rowId.equalsIgnoreCase(e.getId()))
                    .findFirst()
                    .orElse(new TestExecutionDto());
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
        if (bugTicket == null || bugTicket.trim().isEmpty())
        {
            return new TestExecutionDto();
        }

        final String cleanTicket = bugTicket.trim().replaceAll("^#+", "");

        try
        {
            final long step1Start = System.currentTimeMillis();
            final RunReportDto report = getRunReport(runId);
            final long step1Ms = System.currentTimeMillis() - step1Start;

            final Optional<TestExecutionDto> targetOpt = report.getExecutions().stream()
                .filter(e -> rowId.equalsIgnoreCase(e.getId()))
                .findFirst();

            if (targetOpt.isPresent())
            {
                final TestExecutionDto target = targetOpt.get();
                final String varId = generateVariationId(target.getTestClass(), target.getTestMethod(), target.getTitle(), target.getLocation(), target.getBrowser());
                final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
                final String runBatch = runOpt.map(TestRunEntity::getBatchName).orElse("ALL");
                final String runEnv = runOpt.map(TestRunEntity::getEnvironment).orElse("ALL");
                final long runStartTime = runOpt.map(TestRunEntity::getStartTimeMs).orElse(System.currentTimeMillis());

                final long dbStart = System.currentTimeMillis();
                final Map<String, Long> runStartTimes = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
                    .collect(Collectors.toMap(
                        TestRunEntity::getId,
                        r -> r.getStartTimeMs() != null ? r.getStartTimeMs() : 0L,
                        (a, b) -> a
                    ));

                final List<TestBaseBugEntity> existing = bugRepository.findByVariationIdAndBatchNameInAndEnvironmentIn(
                    varId, List.of(runBatch, "ALL"), List.of(runEnv, "ALL"));
                for (final TestBaseBugEntity bug : existing)
                {
                    final boolean isActiveAtRun = (bug.getLinkedRunId() == null || isRunAtOrAfter(runId, bug.getLinkedRunId(), runStartTimes))
                        && (bug.getRemovedRunId() == null || !isRunAtOrAfter(runId, bug.getRemovedRunId(), runStartTimes));

                    if (normalizeTicket(bug.getBugTicket()).equals(normalizeTicket(cleanTicket)) && isActiveAtRun)
                    {
                        bug.setRemovedAtMs(runStartTime);
                        bug.setRemovedRunId(runId);
                        bugRepository.save(bug);
                    }
                }
                final long dbMs = System.currentTimeMillis() - dbStart;

                final long reevalStart = System.currentTimeMillis();
                reevaluateBatchRunsFrom(runBatch, runStartTime, varId);
                final long syncMs = System.currentTimeMillis() - reevalStart;

                final long finalReportStart = System.currentTimeMillis();
                final RunReportDto updatedReport = getRunReport(runId);
                final long finalReportMs = System.currentTimeMillis() - finalReportStart;

                final long totalMs = System.currentTimeMillis() - startMs;
                LOG.info("[PERF] removeBugFromExecution total={} ms (initialReport={} ms, dbSave={} ms, syncUpdate={} ms, finalReport={} ms) runId={}, rowId={}, ticket={}",
                    totalMs, step1Ms, dbMs, syncMs, finalReportMs, runId, rowId, cleanTicket);

                return updatedReport.getExecutions().stream()
                    .filter(e -> rowId.equalsIgnoreCase(e.getId()))
                    .findFirst()
                    .orElse(new TestExecutionDto());
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
        reevaluateBatchRunsFrom(batchName, startTimeMs, null);
    }

    private void reevaluateBatchRunsFrom(final String batchName, final long startTimeMs, final String targetVariationId)
    {
        final long reevalStart = System.currentTimeMillis();
        final List<TestRunEntity> affectedRuns = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
            .filter(r -> (batchName.equalsIgnoreCase("ALL") || batchName.equalsIgnoreCase(r.getBatchName()))
                      && r.getStartTimeMs() != null && r.getStartTimeMs() >= startTimeMs)
            .collect(Collectors.toList());

        LOG.info("[PERF] reevaluateBatchRunsFrom starting for batch='{}', startTimeMs={}, totalAffectedRuns={}",
            batchName, startTimeMs, affectedRuns.size());

        for (final TestRunEntity run : affectedRuns)
        {
            final long runStart = System.currentTimeMillis();
            if (runReportCache.containsKey(run.getId()))
            {
                updateCachedReportBugsAndStats(run.getId());
            }
            else
            {
                recalculateRunEntityStats(run.getId());
            }
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

    static boolean hasBugsInLatestHistoryLink(final String historyLinks)
    {
        if (historyLinks == null || historyLinks.trim().isEmpty())
        {
            return false;
        }
        final String[] links = historyLinks.split(",");
        if (links.length > 0)
        {
            final String first = links[0].trim();
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
            final boolean hasBugs = !activeBugs.isEmpty() || hasBugsInLatestHistoryLink(var.getHistoryLinks());
            final String cur = var.getLastStatus() != null ? var.getLastStatus() : "passed-clean";
            String effective = cur;
            if ("failed".equalsIgnoreCase(cur) || "failed-unknown".equalsIgnoreCase(cur) || "failed-known".equalsIgnoreCase(cur) || "error".equalsIgnoreCase(cur))
            {
                effective = hasBugs ? "failed-known" : "failed-unknown";
            }
            else if ("passed".equalsIgnoreCase(cur) || "succeeded-fixed".equalsIgnoreCase(cur) || "passed-clean".equalsIgnoreCase(cur) || "succeeded".equalsIgnoreCase(cur))
            {
                effective = hasBugs ? "succeeded-fixed" : "passed-clean";
            }
            if (!effective.equalsIgnoreCase(cur))
            {
                var.setLastStatus(effective);
                variationRepository.save(var);
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
            final String batchName = report.getBatchName() != null ? report.getBatchName() : runOpt.map(TestRunEntity::getBatchName).orElse("Unknown");
            final String env = runOpt.map(TestRunEntity::getEnvironment).orElse("Unknown");
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
                        final String area = (exec.getAreaName() != null && !exec.getAreaName().trim().isEmpty() && !"General".equalsIgnoreCase(exec.getAreaName().trim()))
                                            ? exec.getAreaName().trim() : "Browsing (default)";
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
            run.recalculatePassRate();
            runRepository.save(run);
            final long durationMs = System.currentTimeMillis() - startMs;
            LOG.info("[PERF] recalculateRunEntityStats for runId={} took {} ms", runId, durationMs);
        }
    }

    @Transactional
    public void removeRunFromHistory(final String runId)
    {
        final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
        if (runOpt.isPresent())
        {
            final TestRunEntity run = runOpt.get();
            run.setIsDeleted(true);
            runRepository.save(run);
            LOG.info("Soft-deleted run runId={}", runId);
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

            int cPass = 0, cFixed = 0, cKnown = 0, cUnknown = 0, cIgnored = 0;
            for (final TestExecutionDto ce : classExecs)
            {
                final String status = ce.getStatus() != null ? ce.getStatus() : "passed-clean";
                switch (status)
                {
                    case "passed-clean" -> cPass++;
                    case "succeeded-fixed" -> cFixed++;
                    case "failed-known" -> cKnown++;
                    case "failed-unknown" -> cUnknown++;
                    case "ignored" -> cIgnored++;
                }
            }

            testClasses.add(new TestClassSummaryDto(
                testClass,
                "classContainer" + testClass.replace(".", ""),
                classExecs.size(),
                cPass, cFixed, cKnown, cUnknown, cIgnored,
                classExecs
            ));
        }

        final Map<String, List<TestClassSummaryDto>> byArea = testClasses.stream()
            .collect(Collectors.groupingBy(
                tc -> {
                    if (!tc.getExecutions().isEmpty() && tc.getExecutions().get(0).getAreaName() != null && !"General".equalsIgnoreCase(tc.getExecutions().get(0).getAreaName()))
                    {
                        return tc.getExecutions().get(0).getAreaName();
                    }
                    return "Browsing (default)";
                },
                LinkedHashMap::new,
                Collectors.toList()
            ));

        final List<AreaSummaryDto> areas = new ArrayList<>();
        for (final Map.Entry<String, List<TestClassSummaryDto>> areaEntry : byArea.entrySet())
        {
            final String areaName = areaEntry.getKey();
            final List<TestClassSummaryDto> areaClasses = areaEntry.getValue();

            int aPass = 0, aFixed = 0, aKnown = 0, aUnknown = 0, aIgnored = 0;
            for (final TestClassSummaryDto tc : areaClasses)
            {
                aPass += tc.getPassCount();
                aFixed += tc.getSucceededFixedCount();
                aKnown += tc.getFailedKnownCount();
                aUnknown += tc.getFailedUnknownCount();
                aIgnored += tc.getIgnoredCount();
            }

            areas.add(new AreaSummaryDto(
                areaName,
                "areaGroup" + areaName.replace(" ", "").replace("@", ""),
                areaClasses,
                aPass, aFixed, aKnown, aUnknown, aIgnored
            ));
        }
        return areas;
    }

    private RunReportDto buildRunReportDto(final TestRunEntity runEntity, final List<TestExecutionDto> executions)
    {
        if (runEntity == null)
        {
            return new RunReportDto(
                "", "Unknown", "N/A", "0s",
                0, 0, 0, 0, 0, 0,
                List.of(), List.of()
            );
        }



        int pass = 0, fixed = 0, known = 0, unknown = 0, ignored = 0;
        for (final TestExecutionDto e : executions)
        {
            final String status = e.getStatus() != null ? e.getStatus() : "passed-clean";
            switch (status)
            {
                case "passed-clean" -> pass++;
                case "succeeded-fixed" -> fixed++;
                case "failed-known" -> known++;
                case "failed-unknown" -> unknown++;
                case "ignored" -> ignored++;
            }
        }

        final int total = executions.size();

        final List<AreaSummaryDto> areas = buildAreaSummaries(executions);

        final boolean inProgress = runEntity.getStatus() != null && "IN_PROGRESS".equalsIgnoreCase(runEntity.getStatus());

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
            executions, areas,
            runEntity.getTotalLlmCalls(),
            runEntity.getTotalLlmTokens(),
            runEntity.getTotalLlmCost(),
            inProgress
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

        Optional<TestBaseVariationEntity> varOpt = variationRepository.findById(primaryVarId);
        if (varOpt.isEmpty() && (targetTestMethod == null || targetTestMethod.trim().isEmpty()))
        {
            final String legacyVarId = generateVariationId(targetTestClass, targetDataSet, targetLocation, targetBrowser);
            varOpt = variationRepository.findById(legacyVarId);
        }

        final String varId = varOpt.map(TestBaseVariationEntity::getId).orElse(primaryVarId);
        if (varOpt.isPresent())
        {
            final TestBaseVariationEntity varEntity = varOpt.get();
            final String historyLinks = varEntity.getHistoryLinks();
            if (historyLinks != null && !historyLinks.trim().isEmpty())
            {
                final String[] links = historyLinks.split(",");
                final List<String> runIdsToVerify = new ArrayList<>();
                final List<Map<String, String>> parsedLinks = new ArrayList<>();

                for (final String link : links)
                {
                    final String trimmedLink = link.trim();
                    if (trimmedLink.isEmpty())
                    {
                        continue;
                    }
                    final Map<String, String> params = parseQueryParams(trimmedLink);
                    final String runId = params.get("runId");
                    if (runId != null && !runId.isEmpty())
                    {
                        runIdsToVerify.add(runId);
                        parsedLinks.add(params);
                    }
                }

                if (!runIdsToVerify.isEmpty())
                {
                    final Map<String, TestRunEntity> activeRunMap = runRepository.findAllById(runIdsToVerify).stream()
                        .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                        .collect(Collectors.toMap(TestRunEntity::getId, r -> r, (a, b) -> a));

                    final Map<String, Long> runStartTimes = activeRunMap.values().stream()
                        .collect(Collectors.toMap(
                            TestRunEntity::getId,
                            r -> r.getStartTimeMs() != null ? r.getStartTimeMs() : 0L,
                            (a, b) -> a
                        ));

                    final List<TestBaseBugEntity> varBugs = bugRepository.findByVariationId(varId);

                    final List<TestBaseVariationHistoryDto> historyList = new ArrayList<>();
                    final List<String> upgradedLinks = new ArrayList<>();
                    boolean modifiedAny = false;

                    for (final Map<String, String> params : parsedLinks)
                    {
                        final String runId = params.get("runId");
                        final TestRunEntity run = activeRunMap.get(runId);
                        if (run == null)
                        {
                            continue;
                        }

                        final String execId = params.get("executionId");
                        final String batchName = params.get("batch") != null ? params.get("batch") : run.getBatchName();
                        final String engine = params.get("engine");
                        final String timestamp = params.get("ts");
                        final String rawStatus = params.get("status");

                        if (batchName != null && engine != null && timestamp != null && rawStatus != null)
                        {
                            final String runBatch = run.getBatchName() != null ? run.getBatchName() : "ALL";
                            final String runEnv = run.getEnvironment() != null ? run.getEnvironment() : "ALL";

                            final List<String> activeDbBugs = varBugs.stream()
                                .filter(b -> (b.getBatchName().equalsIgnoreCase("ALL") || b.getBatchName().equalsIgnoreCase(runBatch))
                                          && (b.getEnvironment().equalsIgnoreCase("ALL") || b.getEnvironment().equalsIgnoreCase(runEnv))
                                          && (b.getLinkedRunId() == null || isRunAtOrAfter(runId, b.getLinkedRunId(), runStartTimes))
                                          && (b.getRemovedRunId() == null || !isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes)))
                                .map(TestBaseBugEntity::getBugTicket)
                                .distinct()
                                .collect(Collectors.toList());

                            final java.util.Set<String> combinedBugsSet = new java.util.LinkedHashSet<>();
                            final String bugsParam = params.getOrDefault("bugs", "");
                            if (!bugsParam.isEmpty())
                            {
                                combinedBugsSet.addAll(List.of(bugsParam.split(";")));
                            }
                            combinedBugsSet.addAll(activeDbBugs);

                            final List<String> bugsList = new ArrayList<>(combinedBugsSet);
                            final boolean hasBugs = !bugsList.isEmpty();

                            final String effectiveStatus;
                            if ("failed".equalsIgnoreCase(rawStatus) || "failed-known".equalsIgnoreCase(rawStatus) || "failed-unknown".equalsIgnoreCase(rawStatus))
                            {
                                effectiveStatus = hasBugs ? "failed-known" : "failed-unknown";
                            }
                            else if ("passed".equalsIgnoreCase(rawStatus) || "succeeded-fixed".equalsIgnoreCase(rawStatus) || "passed-clean".equalsIgnoreCase(rawStatus))
                            {
                                effectiveStatus = hasBugs ? "succeeded-fixed" : "passed-clean";
                            }
                            else
                            {
                                effectiveStatus = rawStatus;
                            }

                            final String joinedBugs = String.join(";", bugsList);
                            final String[] badgeInfo = getStatusBadgeInfo(effectiveStatus);
                            String legacyMode = params.get("mode");
                            if (legacyMode == null || legacyMode.isBlank())
                            {
                                final RunReportDto legacyReport = getRunReport(runId);
                                if (legacyReport != null && legacyReport.getExecutions() != null)
                                {
                                    for (final TestExecutionDto legacyExec : legacyReport.getExecutions())
                                    {
                                        if (execId != null && !execId.isEmpty() && execId.equals(legacyExec.getId()))
                                        {
                                            legacyMode = legacyExec.getMode();
                                            break;
                                        }
                                    }
                                    if (legacyMode == null || legacyMode.isBlank())
                                    {
                                        for (final TestExecutionDto legacyExec : legacyReport.getExecutions())
                                        {
                                            if (legacyExec.getMode() != null && !legacyExec.getMode().isBlank())
                                            {
                                                legacyMode = legacyExec.getMode();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            historyList.add(new TestBaseVariationHistoryDto(
                                runId,
                                execId != null ? execId : "",
                                batchName,
                                legacyMode,
                                timestamp,
                                effectiveStatus,
                                badgeInfo[0],
                                badgeInfo[1],
                                bugsList
                            ));

                            final String relUrl = "/run-report?runId=" + java.net.URLEncoder.encode(runId, StandardCharsets.UTF_8)
                                + (execId != null && !execId.trim().isEmpty() ? "&executionId=" + java.net.URLEncoder.encode(execId.trim(), StandardCharsets.UTF_8) : "")
                                + "&batch=" + java.net.URLEncoder.encode(batchName, StandardCharsets.UTF_8)
                                + "&engine=" + java.net.URLEncoder.encode(engine, StandardCharsets.UTF_8)
                                + (legacyMode != null && !legacyMode.isBlank() ? "&mode=" + java.net.URLEncoder.encode(legacyMode, StandardCharsets.UTF_8) : "")
                                + "&ts=" + java.net.URLEncoder.encode(timestamp, StandardCharsets.UTF_8)
                                + "&status=" + java.net.URLEncoder.encode(effectiveStatus, StandardCharsets.UTF_8)
                                + (!joinedBugs.isEmpty() ? "&bugs=" + java.net.URLEncoder.encode(joinedBugs, StandardCharsets.UTF_8) : "");
                            upgradedLinks.add(relUrl);
                            modifiedAny = true;
                        }
                        else
                        {
                            final RunReportDto report = getRunReport(runId);
                            if (report != null && report.getExecutions() != null)
                            {
                                TestExecutionDto matchedExec = null;
                                if (execId != null && !execId.isEmpty())
                                {
                                    for (final TestExecutionDto exec : report.getExecutions())
                                    {
                                        if (execId.equals(exec.getId()))
                                        {
                                            matchedExec = exec;
                                            break;
                                        }
                                    }
                                }
                                if (matchedExec == null)
                                {
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

                                        final String execNormBrowser = normalizeBrowser(exec.getBrowser());
                                        if (!normTargetBrowser.equalsIgnoreCase(execNormBrowser))
                                        {
                                            continue;
                                        }

                                        matchedExec = exec;
                                        break;
                                    }
                                }

                                if (matchedExec != null)
                                {
                                    final String execRawStatus = matchedExec.getStatus() != null ? matchedExec.getStatus() : "passed-clean";
                                    final String[] badgeInfo = getStatusBadgeInfo(execRawStatus);
                                    final String curEngine = matchedExec.getEngine() != null ? matchedExec.getEngine() : "Java";
                                    final String curMode = matchedExec.getMode() != null && !matchedExec.getMode().isBlank() ? matchedExec.getMode() : "FORCE_RECORDING";
                                    final String curTs = run.getTimestampLabel() != null ? run.getTimestampLabel() : "Recently";
                                    final List<String> curBugs = matchedExec.getBugs() != null ? matchedExec.getBugs() : List.of();
                                    final String bugsStr = !curBugs.isEmpty() ? String.join(";", curBugs) : "";

                                    historyList.add(new TestBaseVariationHistoryDto(
                                        runId,
                                        matchedExec.getId(),
                                        run.getBatchName(),
                                        curMode,
                                        curTs,
                                        execRawStatus,
                                        badgeInfo[0],
                                        badgeInfo[1],
                                        curBugs
                                    ));

                                    final String enrichedUrl = "/run-report?runId=" + java.net.URLEncoder.encode(runId, StandardCharsets.UTF_8)
                                        + (matchedExec.getId() != null && !matchedExec.getId().trim().isEmpty() ? "&executionId=" + java.net.URLEncoder.encode(matchedExec.getId().trim(), StandardCharsets.UTF_8) : "")
                                        + "&batch=" + java.net.URLEncoder.encode(run.getBatchName(), StandardCharsets.UTF_8)
                                        + "&engine=" + java.net.URLEncoder.encode(curEngine, StandardCharsets.UTF_8)
                                        + (curMode != null && !curMode.isBlank() ? "&mode=" + java.net.URLEncoder.encode(curMode, StandardCharsets.UTF_8) : "")
                                        + "&ts=" + java.net.URLEncoder.encode(curTs, StandardCharsets.UTF_8)
                                        + "&status=" + java.net.URLEncoder.encode(execRawStatus, StandardCharsets.UTF_8)
                                        + (!bugsStr.isEmpty() ? "&bugs=" + java.net.URLEncoder.encode(bugsStr, StandardCharsets.UTF_8) : "");

                                    upgradedLinks.add(enrichedUrl);
                                    modifiedAny = true;
                                }
                            }
                        }
                    }

                    if (!historyList.isEmpty())
                    {
                        final String latestStatus = historyList.get(0).getStatus();
                        if (latestStatus != null && !latestStatus.equalsIgnoreCase(varEntity.getLastStatus()))
                        {
                            varEntity.setLastStatus(latestStatus);
                            if (modifiedAny && !upgradedLinks.isEmpty())
                            {
                                varEntity.setHistoryLinks(String.join(",", upgradedLinks));
                            }
                            variationRepository.save(varEntity);
                        }
                        else if (modifiedAny && !upgradedLinks.isEmpty())
                        {
                            varEntity.setHistoryLinks(String.join(",", upgradedLinks));
                            variationRepository.save(varEntity);
                        }
                        return historyList;
                    }
                }
            }
        }

        final List<TestRunEntity> runs = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
        final List<TestBaseVariationHistoryDto> historyList = new ArrayList<>();
        final List<String> newLinksList = new ArrayList<>();

        for (final TestRunEntity run : runs)
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

                if (targetLocation != null && !targetLocation.isEmpty() && !"Unknown".equalsIgnoreCase(targetLocation)
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

                final String rawStatus = exec.getStatus() != null ? exec.getStatus() : "passed-clean";
                final String[] badgeInfo = getStatusBadgeInfo(rawStatus);
                final String timestamp = run.getTimestampLabel() != null ? run.getTimestampLabel() : "Recently";
                final String engine = exec.getEngine() != null ? exec.getEngine() : "Java";
                final String mode = exec.getMode() != null && !exec.getMode().isBlank() ? exec.getMode() : "FORCE_RECORDING";
                final String bugsStr = exec.getBugs() != null && !exec.getBugs().isEmpty() ? String.join(";", exec.getBugs()) : "";

                final String relUrl = "/run-report?runId=" + java.net.URLEncoder.encode(run.getId(), StandardCharsets.UTF_8)
                    + (exec.getId() != null && !exec.getId().trim().isEmpty() ? "&executionId=" + java.net.URLEncoder.encode(exec.getId().trim(), StandardCharsets.UTF_8) : "")
                    + "&batch=" + java.net.URLEncoder.encode(run.getBatchName(), StandardCharsets.UTF_8)
                    + "&engine=" + java.net.URLEncoder.encode(engine, StandardCharsets.UTF_8)
                    + (mode != null && !mode.isBlank() ? "&mode=" + java.net.URLEncoder.encode(mode, StandardCharsets.UTF_8) : "")
                    + "&ts=" + java.net.URLEncoder.encode(timestamp, StandardCharsets.UTF_8)
                    + "&status=" + java.net.URLEncoder.encode(rawStatus, StandardCharsets.UTF_8)
                    + (!bugsStr.isEmpty() ? "&bugs=" + java.net.URLEncoder.encode(bugsStr, StandardCharsets.UTF_8) : "");

                if (!newLinksList.contains(relUrl))
                {
                    newLinksList.add(relUrl);
                }

                historyList.add(new TestBaseVariationHistoryDto(
                    run.getId(),
                    exec.getId(),
                    run.getBatchName(),
                    mode,
                    timestamp,
                    rawStatus,
                    badgeInfo[0],
                    badgeInfo[1],
                    exec.getBugs() != null ? exec.getBugs() : List.of()
                ));
            }
        }

        if (!newLinksList.isEmpty() && varOpt.isPresent())
        {
            final TestBaseVariationEntity varEntity = varOpt.get();
            varEntity.setHistoryLinks(String.join(",", newLinksList));
            variationRepository.save(varEntity);
        }

        return historyList;
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
                        final String key = java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                        final String value = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
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

    public static String[] getStatusBadgeInfo(final String rawStatus)
    {
        final String status = rawStatus != null ? rawStatus : "passed-clean";
        final String statusClass;
        final String statusLabel;

        switch (status.toLowerCase())
        {
            case "succeeded-fixed", "healed", "fixed" ->
            {
                statusClass = "badge-healed";
                statusLabel = "SUCCEEDED-FIXED";
            }
            case "passed-clean", "passed", "succeeded" ->
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
                statusLabel = "UNKNOWN FAIL";
            }
            case "ignored", "skipped" ->
            {
                statusClass = "badge-ignored";
                statusLabel = "SKIPPED";
            }
            default ->
            {
                statusClass = "badge-pass";
                statusLabel = status.toUpperCase();
            }
        }
        return new String[]{statusClass, statusLabel};
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
        final String normBrowser = normalizeBrowser(browser);
        final String raw = (testClass != null ? testClass : "") + "|" +
                           (testMethod != null ? testMethod : "") + "|" +
                           (dataSet != null ? dataSet : "") + "|" +
                           (location != null ? location : "") + "|" +
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
}

