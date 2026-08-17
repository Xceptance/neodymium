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
import java.nio.file.Files;
import java.nio.file.Path;
import com.xceptance.aura.report.dto.AreaSummaryDto;
import com.xceptance.aura.report.dto.BatchOverviewDataDto;
import com.xceptance.aura.report.dto.BatchSummaryDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestBaseDataDto;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class managing report data, persistent database operations, local disk run JSONs, and attachment storage.
 * Reads 100% from database and local run JSON files on disk — zero hardcoded sample data.
 *
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

    @Transactional
    public void clearAllDatabaseEntries()
    {
        bugRepository.deleteAllInBatch();
        variationRepository.deleteAllInBatch();
        batchRepository.deleteAllInBatch();
        runRepository.deleteAllInBatch();
        LOG.info("Removed all entries from all database tables (TestBaseBug, TestBaseVariation, TestBatch, TestRun).");
    }

    public List<BatchSummaryDto> getAllBatches()
    {
        final List<TestBatchEntity> batchEntities = batchRepository.findAll();
        final List<BatchSummaryDto> batches = new ArrayList<>();

        for (final TestBatchEntity b : batchEntities)
        {
            String passRateText = "N/A";
            String lastExecutedText = "No runs";

            final List<TestRunEntity> batchRuns = runRepository.findByBatchNameAndIsDeletedFalseOrderByStartTimeMsDesc(b.getBatchName());

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
            }

            final int batchBugCount = activeBatchBugs.size();
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
        final List<BatchSummaryDto> batches = getAllBatches();
        final List<TestRunEntity> runs = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();

        // 1. Dynamic Environments
        final List<String> envsFromBatches = batchRepository.findAll().stream()
            .map(TestBatchEntity::getEnvironment)
            .collect(Collectors.toList());
        final List<String> envsFromRuns = runs.stream()
            .map(TestRunEntity::getEnvironment)
            .collect(Collectors.toList());
        final List<String> filterEnvs = Stream.concat(envsFromBatches.stream(), envsFromRuns.stream())
            .filter(e -> e != null && !e.trim().isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        // 2. Dynamic Locales
        final List<String> filterLocales = new ArrayList<>();
        batchRepository.findAll().forEach(b -> {
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
        runs.forEach(r -> {
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
        batchRepository.findAll().forEach(b -> {
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
        runs.forEach(r -> {
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
            runs,
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
                    if (area == null || area.trim().isEmpty())
                    {
                        return "@General";
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
                "", "No Batch", "N/A", "0s",
                0, 0, 0, 0, 0, 0,
                List.of(), List.of()
            );
        }

        final Optional<TestRunEntity> runOpt = runRepository.findById(effectiveRunId);
        final TestRunEntity runEntity = runOpt.orElseGet(() -> new TestRunEntity(
            effectiveRunId,
            "Unknown Batch",
            "COMPLETED",
            "Manual",
            "Staging",
            "US",
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
                final JsonNode execArray = root.path("executions");
                if (execArray.isArray())
                {
                    rawExecutions = objectMapper.convertValue(execArray, new TypeReference<List<TestExecutionDto>>() {});
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

        for (final TestExecutionDto exec : rawExecutions)
        {
            final String varId = generateVariationId(exec.getTestClass(), exec.getTitle(), exec.getLocation(), exec.getBrowser());
            final List<String> bugTickets = bugRepository.findByVariationIdAndEnvironmentIn(varId, List.of(runEnv, "ALL")).stream()
                .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(effectiveRunId, b.getLinkedRunId()))
                          && (b.getRemovedRunId() == null || !isRunAtOrAfter(effectiveRunId, b.getRemovedRunId())))
                .map(TestBaseBugEntity::getBugTicket)
                .distinct()
                .collect(Collectors.toList());
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

            executions.add(exec);
        }

        return buildRunReportDto(runEntity, executions);
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
            batchName != null ? batchName : "US Nightly Regression",
            "IN_PROGRESS",
            triggerSource != null ? triggerSource : "Manual Trigger",
            environment != null ? environment : "Staging",
            "US, EU, DE",
            "Chrome, Firefox, Edge",
            timestampLabel,
            System.currentTimeMillis()
        );

        runRepository.save(newRun);
        liveRunBuffer.put(runId, new ArrayList<>());

        LOG.info("Started new test run: runId={}, batch={}", runId, batchName);
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
                final String varId = generateVariationId(dto.getTestClass(), dto.getTitle(), dto.getLocation(), dto.getBrowser());
                final TestBaseVariationEntity var = variationRepository.findById(varId)
                    .orElseGet(() -> new TestBaseVariationEntity(varId, dto.getTestClass(), dto.getTitle(), "@" + dto.getAreaName(), dto.getLocation(), dto.getBrowser()));

                var.setTotalExecutionsCount(var.getTotalExecutionsCount() + 1);
                var.setLastStatus(dto.getStatus());
                var.setLastExecutedAt(System.currentTimeMillis());
                variationRepository.save(var);
            }

            final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
            if (runOpt.isPresent())
            {
                final TestRunEntity run = runOpt.get();
                run.setTotalTests(run.getTotalTests() + 1);
                final String status = dto.getStatus() != null ? dto.getStatus() : "passed-clean";
                switch (status)
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
                final String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "runId", runId,
                    "batchName", run.getBatchName(),
                    "environment", run.getEnvironment(),
                    "trigger", run.getTriggerSource(),
                    "timestamp", run.getTimestampLabel(),
                    "summary", Map.of(
                        "total", run.getTotalTests(),
                        "pass", run.getPassedCount(),
                        "fixed", run.getSucceededFixedCount(),
                        "known", run.getFailedKnownCount(),
                        "unknown", run.getFailedUnknownCount(),
                        "ignored", run.getIgnoredCount(),
                        "passRate", run.getPassRate()
                    ),
                    "executions", executions
                ));

                localRunJsonStorageService.writeRunJson(runId, jsonContent);
                run.setRunJsonPath("storage/runs/" + runId + "/run.json");
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
            }
            catch (final Exception e)
            {
                LOG.error("Failed to serialize run.json for runId {}: {}", runId, e.getMessage(), e);
            }

            liveRunBuffer.remove(runId);
            LOG.info("Finished run runId={} and persisted to disk.", runId);
        }
    }

    @Transactional
    public TestExecutionDto addBugToExecution(final String runId, final String rowId, final String bugTicket)
    {
        if (bugTicket == null || bugTicket.trim().isEmpty())
        {
            return new TestExecutionDto();
        }

        final String cleanTicket = bugTicket.trim().replaceAll("^#+", "");

        try
        {
            final RunReportDto report = getRunReport(runId);
            final Optional<TestExecutionDto> targetOpt = report.getExecutions().stream()
                .filter(e -> rowId.equalsIgnoreCase(e.getId()))
                .findFirst();

            if (targetOpt.isPresent())
            {
                final TestExecutionDto target = targetOpt.get();
                final String varId = generateVariationId(target.getTestClass(), target.getTitle(), target.getLocation(), target.getBrowser());
                final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
                final String runEnv = runOpt.map(TestRunEntity::getEnvironment).orElse("ALL");
                final long runStartTime = runOpt.map(TestRunEntity::getStartTimeMs).orElse(System.currentTimeMillis());

                final List<TestBaseBugEntity> existing = bugRepository.findByVariationIdAndEnvironmentIn(varId, List.of(runEnv, "ALL"));
                final boolean existsActive = existing.stream()
                    .anyMatch(b -> normalizeTicket(b.getBugTicket()).equals(normalizeTicket(cleanTicket)) && b.getRemovedRunId() == null);

                if (!existsActive)
                {
                    bugRepository.save(new TestBaseBugEntity(varId, cleanTicket, runEnv, runStartTime, runId));
                }

                final RunReportDto updatedReport = getRunReport(runId);
                saveRunReportToDisk(runId, updatedReport);
                recalculateRunEntityStats(runId);

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
        if (bugTicket == null || bugTicket.trim().isEmpty())
        {
            return new TestExecutionDto();
        }

        final String cleanTicket = bugTicket.trim().replaceAll("^#+", "");

        try
        {
            final RunReportDto report = getRunReport(runId);
            final Optional<TestExecutionDto> targetOpt = report.getExecutions().stream()
                .filter(e -> rowId.equalsIgnoreCase(e.getId()))
                .findFirst();

            if (targetOpt.isPresent())
            {
                final TestExecutionDto target = targetOpt.get();
                final String varId = generateVariationId(target.getTestClass(), target.getTitle(), target.getLocation(), target.getBrowser());
                final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
                final String runEnv = runOpt.map(TestRunEntity::getEnvironment).orElse("ALL");
                final long runStartTime = runOpt.map(TestRunEntity::getStartTimeMs).orElse(System.currentTimeMillis());

                final List<TestBaseBugEntity> existing = bugRepository.findByVariationIdAndEnvironmentIn(varId, List.of(runEnv, "ALL"));
                for (final TestBaseBugEntity bug : existing)
                {
                    if (normalizeTicket(bug.getBugTicket()).equals(normalizeTicket(cleanTicket)) && bug.getRemovedRunId() == null)
                    {
                        bug.setRemovedAtMs(runStartTime);
                        bug.setRemovedRunId(runId);
                        bugRepository.save(bug);
                    }
                }

                final RunReportDto updatedReport = getRunReport(runId);
                saveRunReportToDisk(runId, updatedReport);
                recalculateRunEntityStats(runId);

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

    public void saveRunReportToDisk(final String runId, final RunReportDto report)
    {
        if (runId == null || report == null)
        {
            return;
        }
        try
        {
            final Optional<TestRunEntity> runOpt = runRepository.findById(runId);
            final String batchName = report.getBatchName() != null ? report.getBatchName() : runOpt.map(TestRunEntity::getBatchName).orElse("Unknown");
            final String env = runOpt.map(TestRunEntity::getEnvironment).orElse("Unknown");
            final String duration = report.getDuration() != null ? report.getDuration() : "0s";
            final int total = report.getTotalCount();
            final double passRate = total > 0 ? Math.round((report.getPassCount() + report.getFixedCount()) * 100.0 / total * 10.0) / 10.0 : 0.0;

            final String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                "runId", runId,
                "batchName", batchName,
                "environment", env,
                "duration", duration,
                "summary", Map.of(
                    "total", total,
                    "passed", report.getPassCount(),
                    "fixed", report.getFixedCount(),
                    "known", report.getKnownCount(),
                    "unknown", report.getUnknownCount(),
                    "ignored", report.getIgnoredCount(),
                    "passRate", passRate
                ),
                "executions", report.getExecutions()
            ));
            localRunJsonStorageService.writeRunJson(runId, jsonContent);

            for (final TestExecutionDto exec : report.getExecutions())
            {
                localRunJsonStorageService.updateExecutionInRun(runId, exec.getId(), node -> {
                    if (node instanceof com.fasterxml.jackson.databind.node.ObjectNode objNode)
                    {
                        objNode.put("status", exec.getStatus());
                        final com.fasterxml.jackson.databind.node.ArrayNode bugsArray = objectMapper.createArrayNode();
                        if (exec.getBugs() != null)
                        {
                            for (final String bug : exec.getBugs())
                            {
                                bugsArray.add(bug);
                            }
                        }
                        objNode.set("bugs", bugsArray);
                    }
                });
            }
        }
        catch (final Exception e)
        {
            LOG.error("Failed to save run.json to disk for runId {}: {}", runId, e.getMessage(), e);
        }
    }

    private String normalizeTicket(final String ticket)
    {
        if (ticket == null)
        {
            return "";
        }
        return ticket.trim().replaceAll("^#+", "").toUpperCase();
    }

    private boolean isRunAtOrAfter(final String currentRunId, final String refRunId)
    {
        if (currentRunId == null || refRunId == null)
        {
            return true;
        }
        if (currentRunId.equalsIgnoreCase(refRunId))
        {
            return true;
        }
        try
        {
            final long cId = Long.parseLong(currentRunId);
            final long rId = Long.parseLong(refRunId);
            return cId >= rId;
        }
        catch (final NumberFormatException e)
        {
            final Optional<TestRunEntity> currentRun = runRepository.findById(currentRunId);
            final Optional<TestRunEntity> refRun = runRepository.findById(refRunId);
            if (currentRun.isPresent() && refRun.isPresent()
                && currentRun.get().getStartTimeMs() != null
                && refRun.get().getStartTimeMs() != null)
            {
                return currentRun.get().getStartTimeMs() >= refRun.get().getStartTimeMs();
            }
            return currentRunId.compareTo(refRunId) >= 0;
        }
    }

    private void recalculateRunEntityStats(final String runId)
    {
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

    private RunReportDto buildRunReportDto(final TestRunEntity runEntity, final List<TestExecutionDto> executions)
    {
        if (runEntity == null)
        {
            return new RunReportDto(
                "", "No Batch", "N/A", "0s",
                0, 0, 0, 0, 0, 0,
                List.of(), List.of()
            );
        }

        final String runEnv = runEntity.getEnvironment() != null ? runEntity.getEnvironment() : "ALL";
        final String runId = runEntity.getId();

        // Enrich executions with database bug tickets and compute status dynamically
        for (final TestExecutionDto e : executions)
        {
            final String varId = generateVariationId(e.getTestClass(), e.getTitle(), e.getLocation(), e.getBrowser());
            final List<String> bugTickets = bugRepository.findByVariationIdAndEnvironmentIn(varId, List.of(runEnv, "ALL")).stream()
                .filter(b -> (b.getLinkedRunId() == null || isRunAtOrAfter(runId, b.getLinkedRunId()))
                          && (b.getRemovedRunId() == null || !isRunAtOrAfter(runId, b.getRemovedRunId())))
                .map(TestBaseBugEntity::getBugTicket)
                .filter(t -> t != null && !t.trim().isEmpty() && !"NONE".equalsIgnoreCase(t))
                .distinct()
                .collect(Collectors.toList());
            e.setBugs(bugTickets);

            final String currentStatus = e.getStatus();
            if ("failed".equalsIgnoreCase(currentStatus) || "failed-unknown".equalsIgnoreCase(currentStatus) || "failed-known".equalsIgnoreCase(currentStatus))
            {
                if (!bugTickets.isEmpty())
                {
                    e.setStatus("failed-known");
                }
                else
                {
                    e.setStatus("failed-unknown");
                }
            }
            else if ("passed".equalsIgnoreCase(currentStatus) || "succeeded-fixed".equalsIgnoreCase(currentStatus) || "passed-clean".equalsIgnoreCase(currentStatus))
            {
                if (!bugTickets.isEmpty())
                {
                    e.setStatus("succeeded-fixed");
                }
                else
                {
                    e.setStatus("passed-clean");
                }
            }
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
                    if (!tc.getExecutions().isEmpty() && tc.getExecutions().get(0).getAreaName() != null)
                    {
                        return tc.getExecutions().get(0).getAreaName();
                    }
                    return "General";
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

        return new RunReportDto(
            runEntity.getId(),
            runEntity.getBatchName(),
            runEntity.getTimestampLabel(),
            "00:12:45",
            total, pass, fixed, known, unknown, ignored,
            executions, areas
        );
    }

    private String generateVariationId(final String testClass, final String dataSet, final String location, final String browser)
    {
        final String raw = (testClass != null ? testClass : "") + "|" +
                           (dataSet != null ? dataSet : "") + "|" +
                           (location != null ? location : "") + "|" +
                           (browser != null ? browser : "");
        try
        {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++)
            {
                hex.append(String.format("%02x", hash[i]));
            }
            return "var_" + hex;
        }
        catch (final NoSuchAlgorithmException e)
        {
            return "var_" + Math.abs(raw.hashCode());
        }
    }
}
