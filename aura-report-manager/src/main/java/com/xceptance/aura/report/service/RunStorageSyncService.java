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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.entity.TestBatchEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestBaseVariationRepository;
import com.xceptance.aura.report.repository.TestBatchRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service analyzing report directories in storage, generating run.json files containing test execution data,
 * and synchronizing database records on startup and upon user refresh.
 *
 * Format aligned with console-execution-1.json schema while retaining all features (bugs, comments, failure details).
 *
 * @author Xceptance GmbH 2026
 */
@Service
public class RunStorageSyncService
{
    private static final Logger LOG = LoggerFactory.getLogger(RunStorageSyncService.class);

    private final TestRunRepository runRepository;
    private final TestBatchRepository batchRepository;
    private final TestBaseVariationRepository variationRepository;
    private final TestBaseBugRepository bugRepository;
    private final LocalRunJsonStorageService localRunJsonStorageService;
    private final ObjectMapper objectMapper;

    @Value("${aura.report.storage.runs.base-dir:storage/runs/}")
    private String baseDir;

    @Autowired
    public RunStorageSyncService(
        final TestRunRepository runRepository,
        final TestBatchRepository batchRepository,
        final TestBaseVariationRepository variationRepository,
        final TestBaseBugRepository bugRepository,
        final LocalRunJsonStorageService localRunJsonStorageService)
    {
        this.runRepository = runRepository;
        this.batchRepository = batchRepository;
        this.variationRepository = variationRepository;
        this.bugRepository = bugRepository;
        this.localRunJsonStorageService = localRunJsonStorageService;
        this.objectMapper = new ObjectMapper();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup()
    {
        LOG.info("Aura Report Manager startup: Analyzing storage directory '{}' for test executions...", baseDir);
        final int importedCount = syncLocalRunStorage();
        LOG.info("Startup report analysis complete. Synced {} run reports into database.", importedCount);
    }

    @Transactional
    public int syncLocalRunStorage()
    {
        final Path dir = Paths.get(baseDir);
        if (!Files.exists(dir) || !Files.isDirectory(dir))
        {
            LOG.info("Storage directory '{}' does not exist on disk. Skipping report storage sync.", baseDir);
            return 0;
        }

        int importedCount = 0;
        try (final Stream<Path> stream = Files.list(dir))
        {
            final List<Path> entries = stream.toList();

            for (final Path entry : entries)
            {
                if (Files.isDirectory(entry))
                {
                    final String runId = entry.getFileName().toString();
                    final boolean success = importOrUpdateRunReport(runId);
                    if (success)
                    {
                        importedCount++;
                    }
                }
            }
        }
        catch (final IOException e)
        {
            LOG.error("Error scanning storage directory {}: {}", baseDir, e.getMessage());
        }

        upgradeLegacyHistoryLinks();

        return importedCount;
    }

    @Transactional
    public void upgradeLegacyHistoryLinks()
    {
        final List<TestBaseVariationEntity> variations = variationRepository.findAll();
        for (final TestBaseVariationEntity varEntity : variations)
        {
            final String history = varEntity.getHistoryLinks();
            if (history != null && history.contains("/run-report?") && !history.contains("&batch="))
            {
                final String[] links = history.split(",");
                final List<String> upgradedLinks = new ArrayList<>();
                boolean modified = false;

                for (final String link : links)
                {
                    final String trimmed = link.trim();
                    if (trimmed.isEmpty())
                    {
                        continue;
                    }
                    final Map<String, String> params = AuraReportDataService.parseQueryParams(trimmed);
                    if (!params.containsKey("batch"))
                    {
                        final String rId = params.get("runId");
                        final String eId = params.get("executionId");
                        if (rId != null && !rId.isEmpty())
                        {
                            final Optional<String> runJsonOpt = localRunJsonStorageService.readRunJson(rId);
                            if (runJsonOpt.isPresent())
                            {
                                try
                                {
                                    final JsonNode root = objectMapper.readTree(runJsonOpt.get());
                                    final File runDir = new File(baseDir, rId);
                                    final LocalRunJsonStorageService.BatchInfo batchInfo = localRunJsonStorageService.resolveOrCreateBatchJson(runDir, null);
                                    final String bName = root.path("batchName").asText(batchInfo.name);
                                    final String ts = root.path("timestamp").asText(root.path("startTime").asText("Recently"));
                                    final JsonNode execArray = root.path("executions");

                                    if (execArray.isArray())
                                    {
                                        for (final JsonNode exec : execArray)
                                        {
                                            final String curExecId = exec.path("id").asText("");
                                            if (eId == null || eId.isEmpty() || eId.equals(curExecId))
                                            {
                                                final String engine = exec.has("engine") ? exec.path("engine").asText("Java") : "Java";
                                                final String rawStatus = exec.path("status").asText("passed-clean");
                                                final List<String> bugList = new ArrayList<>();
                                                if (exec.has("bugs") && exec.path("bugs").isArray())
                                                {
                                                    for (final JsonNode bugNode : exec.path("bugs"))
                                                    {
                                                        if (!bugNode.asText().trim().isEmpty())
                                                        {
                                                            bugList.add(bugNode.asText().trim());
                                                        }
                                                    }
                                                }
                                                final String bugsStr = !bugList.isEmpty() ? String.join(";", bugList) : "";

                                                final String enrichedUrl = "/run-report?runId=" + java.net.URLEncoder.encode(rId, java.nio.charset.StandardCharsets.UTF_8)
                                                    + (curExecId != null && !curExecId.trim().isEmpty() ? "&executionId=" + java.net.URLEncoder.encode(curExecId.trim(), java.nio.charset.StandardCharsets.UTF_8) : "")
                                                    + "&batch=" + java.net.URLEncoder.encode(bName, java.nio.charset.StandardCharsets.UTF_8)
                                                    + "&engine=" + java.net.URLEncoder.encode(engine, java.nio.charset.StandardCharsets.UTF_8)
                                                    + "&ts=" + java.net.URLEncoder.encode(ts, java.nio.charset.StandardCharsets.UTF_8)
                                                    + "&status=" + java.net.URLEncoder.encode(rawStatus, java.nio.charset.StandardCharsets.UTF_8)
                                                    + (!bugsStr.isEmpty() ? "&bugs=" + java.net.URLEncoder.encode(bugsStr, java.nio.charset.StandardCharsets.UTF_8) : "");

                                                upgradedLinks.add(enrichedUrl);
                                                modified = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                catch (final Exception e)
                                {
                                    upgradedLinks.add(trimmed);
                                }
                            }
                            else
                            {
                                upgradedLinks.add(trimmed);
                            }
                        }
                        else
                        {
                            upgradedLinks.add(trimmed);
                        }
                    }
                    else
                    {
                        upgradedLinks.add(trimmed);
                    }
                }

                if (modified && !upgradedLinks.isEmpty())
                {
                    varEntity.setHistoryLinks(String.join(",", upgradedLinks));
                    variationRepository.save(varEntity);
                }
            }
        }
    }

    @Transactional
    public boolean importOrUpdateRunReport(final String runId)
    {
        try
        {
            final File runDirFile = localRunJsonStorageService.getRunDir(runId).toFile();
            if (runDirFile.exists() && runDirFile.isDirectory())
            {
                localRunJsonStorageService.buildRunJsonContent(runDirFile, runId, true);
            }

            final Optional<String> fullJsonOpt = localRunJsonStorageService.readRunJson(runId);
            if (fullJsonOpt.isEmpty())
            {
                LOG.debug("No valid run JSON found for runId '{}'. Skipping import.", runId);
                return false;
            }

            final JsonNode root = objectMapper.readTree(fullJsonOpt.get());

            final File runDir = new File(baseDir, runId);
            final LocalRunJsonStorageService.BatchInfo batchInfo = localRunJsonStorageService.resolveOrCreateBatchJson(runDir, null);

            final String batchName = root.path("batchName").asText(batchInfo.name);
            final String env = root.path("environment").asText(batchInfo.environment);
            final String batchDesc = batchInfo.description;
            final String trigger = root.path("trigger").asText(root.path("triggerSource").asText("Unknown"));

            final JsonNode summaryNode = root.path("summary");
            final int totalTests = summaryNode.path("total").asInt(0);
            final int pass = summaryNode.path("pass").asInt(0);
            final int fixed = summaryNode.path("fixed").asInt(0);
            final int known = summaryNode.path("known").asInt(0);
            final int unknown = summaryNode.path("unknown").asInt(0);
            final int ignored = summaryNode.path("ignored").asInt(0);
            final double passRate = summaryNode.path("passRate").asDouble(totalTests > 0 ? (double) (pass + fixed) / totalTests * 100.0 : 0.0);

            int sumLlmCalls = summaryNode.path("totalLlmCalls").asInt(0);
            long sumLlmTokens = summaryNode.path("totalLlmTokens").asLong(0L);
            double sumLlmCost = summaryNode.path("totalLlmCost").asDouble(0.0);

            if (sumLlmCalls == 0 && sumLlmTokens == 0L && sumLlmCost == 0.0)
            {
                final JsonNode execMetricsNode = root.path("executionMetrics");
                if (execMetricsNode.isObject())
                {
                    for (final JsonNode mNode : execMetricsNode)
                    {
                        sumLlmCalls += mNode.path("llmCallsCount").asInt(0);
                        sumLlmTokens += mNode.path("llmTotalTokens").asLong(0L);
                        sumLlmCost += mNode.path("llmCost").asDouble(0.0);
                    }
                }
            }

            final JsonNode execMetricsNode = root.path("executionMetrics");

            final List<String> localesList = new ArrayList<>();
            final JsonNode localesArr = root.path("locales");
            if (localesArr.isArray())
            {
                for (final JsonNode l : localesArr)
                {
                    if (!l.asText().trim().isEmpty())
                    {
                        localesList.add(l.asText().trim());
                    }
                }
            }
            if (localesList.isEmpty() && execMetricsNode.isObject())
            {
                final Set<String> locSet = new LinkedHashSet<>();
                for (final JsonNode mNode : execMetricsNode)
                {
                    final String loc = mNode.path("location").asText(mNode.path("locale").asText("")).trim();
                    if (!loc.isEmpty() && !"Unknown".equalsIgnoreCase(loc))
                    {
                        locSet.add(loc);
                    }
                }
                localesList.addAll(locSet);
            }
            final String localesCsv = !localesList.isEmpty() ? String.join(",", localesList) : "Unknown";

            final List<String> browsersList = new ArrayList<>();
            final JsonNode browsersArr = root.path("browsers");
            if (browsersArr.isArray())
            {
                for (final JsonNode b : browsersArr)
                {
                    if (!b.asText().trim().isEmpty())
                    {
                        browsersList.add(b.asText().trim());
                    }
                }
            }
            if (browsersList.isEmpty() && execMetricsNode.isObject())
            {
                final Set<String> bSet = new LinkedHashSet<>();
                for (final JsonNode mNode : execMetricsNode)
                {
                    final String br = mNode.path("browser").asText("").trim();
                    if (!br.isEmpty() && !"Unknown".equalsIgnoreCase(br))
                    {
                        bSet.add(br);
                    }
                }
                browsersList.addAll(bSet);
            }
            final String browsersCsv = !browsersList.isEmpty() ? String.join(",", browsersList) : "Chrome";

            String timestampVal = root.path("timestamp").asText(root.path("startTime").asText("")).trim();
            if (execMetricsNode.isObject() && !execMetricsNode.isEmpty())
            {
                String earliestTime = null;
                Long minMs = null;
                if (!timestampVal.isEmpty() && !"Recently".equalsIgnoreCase(timestampVal))
                {
                    earliestTime = timestampVal;
                    minMs = parseTimestampToMs(timestampVal);
                }
                for (final JsonNode mNode : execMetricsNode)
                {
                    final Long ms = parseStartTimeMs(mNode);
                    String st = mNode.path("startTime").asText("").trim();
                    if (st.isEmpty() && mNode.has("timestamp"))
                    {
                        st = mNode.path("timestamp").asText("").trim();
                    }
                    if (ms != null && ms > 0L)
                    {
                        if (minMs == null || ms < minMs)
                        {
                            minMs = ms;
                            if (!st.isEmpty())
                            {
                                earliestTime = st;
                            }
                        }
                    }
                    else if (!st.isEmpty() && (earliestTime == null || st.compareTo(earliestTime) < 0))
                    {
                        earliestTime = st;
                    }
                }
                if (earliestTime != null)
                {
                    timestampVal = earliestTime;
                }
            }
            if (timestampVal.isEmpty())
            {
                timestampVal = "Recently";
            }
            final String timestamp = timestampVal;

            final List<JsonNode> execList = new ArrayList<>();
            if (execMetricsNode.isObject() && !execMetricsNode.isEmpty())
            {
                execMetricsNode.elements().forEachRemaining(execList::add);
            }
            else if (root.path("executions").isArray())
            {
                root.path("executions").elements().forEachRemaining(execList::add);
            }

            final String runEnv = env != null ? env : "ALL";
            final String runBatch = batchName != null ? batchName : "ALL";

            final Map<String, Long> runStartTimes = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc().stream()
                .collect(Collectors.toMap(
                    TestRunEntity::getId,
                    r -> r.getStartTimeMs() != null ? r.getStartTimeMs() : 0L,
                    (a, b) -> a
                ));

            final List<TestBaseBugEntity> allRunBugs = bugRepository.findByBatchNameInAndEnvironmentIn(List.of(runBatch, "ALL"), List.of(runEnv, "ALL"));

            final Map<String, List<String>> dbBugsByVarId = allRunBugs.stream()
                .filter(b -> (b.getLinkedRunId() == null || AuraReportDataService.isRunAtOrAfter(runId, b.getLinkedRunId(), runStartTimes))
                          && (b.getRemovedRunId() == null || !AuraReportDataService.isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes)))
                .collect(Collectors.groupingBy(
                    TestBaseBugEntity::getVariationId,
                    Collectors.mapping(TestBaseBugEntity::getBugTicket, Collectors.toList())
                ));

            final Map<String, java.util.Set<String>> removedBugsByVarId = allRunBugs.stream()
                .filter(b -> b.getRemovedRunId() != null && AuraReportDataService.isRunAtOrAfter(runId, b.getRemovedRunId(), runStartTimes))
                .collect(Collectors.groupingBy(
                    TestBaseBugEntity::getVariationId,
                    Collectors.mapping(b -> AuraReportDataService.normalizeTicket(b.getBugTicket()), Collectors.toSet())
                ));

            int calcPass = 0;
            int calcFixed = 0;
            int calcKnown = 0;
            int calcUnknown = 0;
            int calcIgnored = 0;

            Long minStartTimeMs = null;
            Long maxStartTimeMs = null;
            long durationOfLatestExecMs = 0L;
            long sumDurationMs = 0L;

            if (!execList.isEmpty())
            {
                for (final JsonNode exec : execList)
                {
                    final long execDurationMs = exec.hasNonNull("duration") ? exec.path("duration").asLong(0L) : exec.path("durationMs").asLong(0L);
                    sumDurationMs += execDurationMs;

                    final Long execStartMs = parseStartTimeMs(exec);
                    if (execStartMs != null && execStartMs > 0L)
                    {
                        if (minStartTimeMs == null || execStartMs < minStartTimeMs)
                        {
                            minStartTimeMs = execStartMs;
                        }
                        if (maxStartTimeMs == null || execStartMs >= maxStartTimeMs)
                        {
                            maxStartTimeMs = execStartMs;
                            durationOfLatestExecMs = execDurationMs;
                        }
                    }
                    final String execId = exec.path("id").asText("");
                    final String testClass = exec.path("testClass").asText("UnknownClass");
                    final String testMethod = LocalRunJsonStorageService.extractTestMethod(exec);
                    String rawTitle = exec.path("title").asText("").trim();
                    if (rawTitle.isEmpty())
                    {
                        rawTitle = exec.path("datasetId").asText("").trim();
                    }
                    if (rawTitle.isEmpty())
                    {
                        rawTitle = exec.path("testId").asText("").trim();
                    }
                    final String dataSet = !rawTitle.isEmpty() ? rawTitle : "Default";
                    final String location = exec.has("locale") && !exec.path("locale").asText().trim().isEmpty() ? exec.path("locale").asText().trim() : exec.path("location").asText("Unknown");
                    final String browser = AuraReportDataService.normalizeBrowser(exec.path("browser").asText("Chrome"));
                    final String rawStatus = exec.path("status").asText("passed-clean");

                    final String varId = generateVariationId(testClass, testMethod, dataSet, location, browser);
                    final List<String> dbBugTickets = dbBugsByVarId.getOrDefault(varId, List.of()).stream()
                        .distinct()
                        .collect(Collectors.toList());

                    final java.util.Set<String> removedTickets = removedBugsByVarId.getOrDefault(varId, java.util.Set.of());
                    final java.util.Set<String> combinedBugs = new java.util.LinkedHashSet<>();
                    if (exec.has("bugs") && exec.path("bugs").isArray())
                    {
                        for (final JsonNode bugNode : exec.path("bugs"))
                        {
                            final String rawB = bugNode.asText().trim();
                            if (!rawB.isEmpty() && !removedTickets.contains(AuraReportDataService.normalizeTicket(rawB)))
                            {
                                combinedBugs.add(rawB);
                            }
                        }
                    }
                    combinedBugs.addAll(dbBugTickets);

                    final List<String> bugList = new ArrayList<>(combinedBugs);
                    final boolean hasBugs = !bugList.isEmpty();
                    final String bugsStr = hasBugs ? String.join(";", bugList) : "";

                    final String effectiveStatus;
                    if ("failed".equalsIgnoreCase(rawStatus) || "failed-known".equalsIgnoreCase(rawStatus) || "failed-unknown".equalsIgnoreCase(rawStatus) || "error".equalsIgnoreCase(rawStatus))
                    {
                        if (hasBugs)
                        {
                            effectiveStatus = "failed-known";
                            calcKnown++;
                        }
                        else
                        {
                            effectiveStatus = "failed-unknown";
                            calcUnknown++;
                        }
                    }
                    else if ("passed".equalsIgnoreCase(rawStatus) || "succeeded-fixed".equalsIgnoreCase(rawStatus) || "passed-clean".equalsIgnoreCase(rawStatus) || "succeeded".equalsIgnoreCase(rawStatus))
                    {
                        if (hasBugs)
                        {
                            effectiveStatus = "succeeded-fixed";
                            calcFixed++;
                        }
                        else
                        {
                            effectiveStatus = "passed-clean";
                            calcPass++;
                        }
                    }
                    else if ("ignored".equalsIgnoreCase(rawStatus) || "skipped".equalsIgnoreCase(rawStatus))
                    {
                        effectiveStatus = rawStatus;
                        calcIgnored++;
                    }
                    else
                    {
                        effectiveStatus = rawStatus;
                        calcPass++;
                    }

                    final Optional<TestBaseVariationEntity> varOpt = variationRepository.findById(varId);
                    final TestBaseVariationEntity varEntity;
                    if (varOpt.isPresent())
                    {
                        varEntity = varOpt.get();
                        if (dataSet != null && !dataSet.isBlank() && ("Default".equals(varEntity.getDataSetLabel()) || varEntity.getDataSetLabel() == null || varEntity.getDataSetLabel().isBlank()))
                        {
                            varEntity.setDataSetLabel(dataSet);
                        }
                        if (testMethod != null && !testMethod.isBlank() && (varEntity.getTestMethodName() == null || varEntity.getTestMethodName().isBlank()))
                        {
                            varEntity.setTestMethodName(testMethod);
                        }
                    }
                    else
                    {
                        varEntity = new TestBaseVariationEntity(varId, testClass, testMethod, dataSet, "@General", location, browser);
                    }

                    varEntity.setTotalExecutionsCount(varEntity.getTotalExecutionsCount() + 1);
                    varEntity.setLastStatus(effectiveStatus);
                    varEntity.setLastExecutedAt(System.currentTimeMillis());

                    final String engine = exec.has("engine") ? exec.path("engine").asText("Java") : "Java";

                    final String relUrl = "/run-report?runId=" + java.net.URLEncoder.encode(runId, java.nio.charset.StandardCharsets.UTF_8)
                        + (execId != null && !execId.trim().isEmpty() ? "&executionId=" + java.net.URLEncoder.encode(execId.trim(), java.nio.charset.StandardCharsets.UTF_8) : "")
                        + "&batch=" + java.net.URLEncoder.encode(batchName, java.nio.charset.StandardCharsets.UTF_8)
                        + "&engine=" + java.net.URLEncoder.encode(engine, java.nio.charset.StandardCharsets.UTF_8)
                        + "&ts=" + java.net.URLEncoder.encode(timestamp, java.nio.charset.StandardCharsets.UTF_8)
                        + "&status=" + java.net.URLEncoder.encode(effectiveStatus, java.nio.charset.StandardCharsets.UTF_8)
                        + (!bugsStr.isEmpty() ? "&bugs=" + java.net.URLEncoder.encode(bugsStr, java.nio.charset.StandardCharsets.UTF_8) : "");

                    final String currentHistory = varEntity.getHistoryLinks();
                    if (currentHistory == null || currentHistory.trim().isEmpty())
                    {
                        varEntity.setHistoryLinks(relUrl);
                    }
                    else
                    {
                        final List<String> linksList = new ArrayList<>(List.of(currentHistory.split(",")));
                        boolean replaced = false;
                        for (int i = 0; i < linksList.size(); i++)
                        {
                            final String existing = linksList.get(i).trim();
                            final Map<String, String> existingParams = AuraReportDataService.parseQueryParams(existing);
                            final String existingRunId = existingParams.get("runId");
                            final String existingExecId = existingParams.get("executionId");

                            if (runId.equals(existingRunId) && java.util.Objects.equals(execId, existingExecId))
                            {
                                linksList.set(i, relUrl);
                                replaced = true;
                                break;
                            }
                        }
                        if (!replaced && !linksList.contains(relUrl))
                        {
                            linksList.add(relUrl);
                        }
                        varEntity.setHistoryLinks(String.join(",", linksList));
                    }

                    variationRepository.save(varEntity);
                }
            }

            final Optional<TestRunEntity> existingRunOpt = runRepository.findById(runId);
            final TestRunEntity runEntity;
            if (existingRunOpt.isPresent())
            {
                runEntity = existingRunOpt.get();
                runEntity.setBatchName(batchName);
                runEntity.setEnvironment(env);
                runEntity.setTriggerSource(trigger);
                runEntity.setTimestampLabel(timestamp);
            }
            else
            {
                runEntity = new TestRunEntity(
                    runId,
                    batchName,
                    "COMPLETED",
                    trigger,
                    env,
                    localesCsv,
                    browsersCsv,
                    timestamp,
                    System.currentTimeMillis()
                );
            }

            final boolean hasExecCounts = !execList.isEmpty();
            final int finalTotal = hasExecCounts ? execList.size() : totalTests;
            final int finalPass = hasExecCounts ? calcPass : pass;
            final int finalFixed = hasExecCounts ? calcFixed : fixed;
            final int finalKnown = hasExecCounts ? calcKnown : known;
            final int finalUnknown = hasExecCounts ? calcUnknown : unknown;
            final int finalIgnored = hasExecCounts ? calcIgnored : ignored;
            final double finalPassRate = finalTotal > 0 ? (double)(finalPass + finalFixed) / finalTotal * 100.0 : passRate;

            final long calculatedRunDurationMs;
            if (minStartTimeMs != null && maxStartTimeMs != null && maxStartTimeMs >= minStartTimeMs)
            {
                calculatedRunDurationMs = (maxStartTimeMs - minStartTimeMs) + durationOfLatestExecMs;
            }
            else
            {
                calculatedRunDurationMs = sumDurationMs;
            }

            if (minStartTimeMs != null)
            {
                runEntity.setStartTimeMs(minStartTimeMs);
                runEntity.setEndTimeMs(maxStartTimeMs != null ? maxStartTimeMs + durationOfLatestExecMs : minStartTimeMs + calculatedRunDurationMs);
            }
            runEntity.setDurationMs(calculatedRunDurationMs);
            runEntity.setFormattedDuration(formatDurationMs(calculatedRunDurationMs));

            runEntity.setLocalesCsv(localesCsv);
            runEntity.setBrowsersCsv(browsersCsv);
            runEntity.setTotalTests(finalTotal);
            runEntity.setPassedCount(finalPass);
            runEntity.setSucceededFixedCount(finalFixed);
            runEntity.setFailedKnownCount(finalKnown);
            runEntity.setFailedUnknownCount(finalUnknown);
            runEntity.setIgnoredCount(finalIgnored);
            runEntity.setPassRate(finalPassRate);
            runEntity.setTotalLlmCalls(sumLlmCalls);
            runEntity.setTotalLlmTokens(sumLlmTokens);
            runEntity.setTotalLlmCost(Math.ceil(sumLlmCost * 1000000.0) / 1000000.0);
            runEntity.setAttachmentsSyncedToS3(true);

            runRepository.save(runEntity);

            final Optional<TestBatchEntity> batchOpt = batchRepository.findById(batchName);
            if (batchOpt.isPresent())
            {
                final TestBatchEntity b = batchOpt.get();
                b.setLatestRunId(runId);
                b.setEnvironment(env);
                b.setLocalesCsv(localesCsv);
                b.setBrowsersCsv(browsersCsv);
                if (batchDesc != null && !batchDesc.isEmpty())
                {
                    b.setDescription(batchDesc);
                }
                batchRepository.save(b);
            }
            else
            {
                batchRepository.save(new TestBatchEntity(
                    batchName,
                    env,
                    batchDesc != null && !batchDesc.isEmpty() ? batchDesc : "",
                    localesCsv,
                    browsersCsv,
                    runId
                ));
            }

            LOG.info("Imported/Updated run JSON report from storage into DB: runId={}, batch={}, totalTests={}, pass={}, fixed={}, known={}, unknown={}",
                runId, batchName, totalTests, pass, fixed, known, unknown);
            return true;
        }
        catch (final Exception e)
        {
            LOG.error("Failed to parse and import run report for runId {}: {}", runId, e.getMessage(), e);
            return false;
        }
    }

    private String generateVariationId(final String testClass, final String testMethod, final String dataSet, final String location, final String browser)
    {
        return AuraReportDataService.generateVariationId(testClass, testMethod, dataSet, location, browser);
    }

    private String generateVariationId(final String testClass, final String dataSet, final String location, final String browser)
    {
        return AuraReportDataService.generateVariationId(testClass, null, dataSet, location, browser);
    }

    public static Long parseStartTimeMs(final JsonNode node)
    {
        if (node == null || !node.isObject())
        {
            return null;
        }

        for (final String key : List.of("startTimeMs", "startTime", "timestamp", "startDate", "time", "createdAt"))
        {
            if (node.hasNonNull(key))
            {
                final JsonNode valNode = node.get(key);
                if (valNode.isNumber())
                {
                    long val = valNode.asLong();
                    if (val > 0L)
                    {
                        if (val < 100000000000L)
                        {
                            val *= 1000L;
                        }
                        return val;
                    }
                }
                final String raw = valNode.asText("").trim();
                if (!raw.isEmpty())
                {
                    final Long parsed = parseTimestampToMs(raw);
                    if (parsed != null && parsed > 0L)
                    {
                        return parsed;
                    }
                }
            }
        }
        return null;
    }

    public static Long parseTimestampToMs(final String rawVal)
    {
        if (rawVal == null || rawVal.trim().isEmpty())
        {
            return null;
        }
        final String trimmed = rawVal.trim();
        try
        {
            double num = Double.parseDouble(trimmed);
            if (num > 0.0)
            {
                if (num < 1e11)
                {
                    num *= 1000.0;
                }
                return (long) num;
            }
        }
        catch (final NumberFormatException ignored)
        {
        }

        try
        {
            return java.time.Instant.parse(trimmed).toEpochMilli();
        }
        catch (final Exception ignored)
        {
        }

        for (final String pattern : List.of("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyyMMdd_HHmmss"))
        {
            try
            {
                final java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(trimmed, java.time.format.DateTimeFormatter.ofPattern(pattern));
                return ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
            }
            catch (final Exception ignored)
            {
            }
        }
        return null;
    }

    public static String formatDurationMs(final long totalMs)
    {
        if (totalMs <= 0L)
        {
            return "0 min 0 s";
        }
        final long totalSeconds = Math.round(totalMs / 1000.0);
        final long minutes = totalSeconds / 60L;
        final long seconds = totalSeconds % 60L;
        return minutes + " min " + seconds + " s";
    }
}
