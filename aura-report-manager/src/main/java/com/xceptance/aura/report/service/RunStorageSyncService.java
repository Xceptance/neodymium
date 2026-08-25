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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            final String timestamp = root.path("timestamp").asText(root.path("startTime").asText("Recently"));

            final JsonNode summaryNode = root.path("summary");
            final int totalTests = summaryNode.path("total").asInt(0);
            final int pass = summaryNode.path("pass").asInt(0);
            final int fixed = summaryNode.path("fixed").asInt(0);
            final int known = summaryNode.path("known").asInt(0);
            final int unknown = summaryNode.path("unknown").asInt(0);
            final int ignored = summaryNode.path("ignored").asInt(0);
            final double passRate = summaryNode.path("passRate").asDouble(totalTests > 0 ? (double) (pass + fixed) / totalTests * 100.0 : 0.0);

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
            final String browsersCsv = !browsersList.isEmpty() ? String.join(",", browsersList) : "Chrome";

            final JsonNode execArray = root.path("executions");

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

            if (execArray.isArray())
            {
                for (final JsonNode exec : execArray)
                {
                    final String execId = exec.path("id").asText("");
                    final String testClass = exec.path("testClass").asText("UnknownClass");
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

                    final String varId = generateVariationId(testClass, dataSet, location, browser);
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
                    }
                    else
                    {
                        varEntity = new TestBaseVariationEntity(varId, testClass, dataSet, "@General", location, browser);
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

            final boolean hasExecCounts = execArray.isArray() && execArray.size() > 0;
            final int finalTotal = hasExecCounts ? execArray.size() : totalTests;
            final int finalPass = hasExecCounts ? calcPass : pass;
            final int finalFixed = hasExecCounts ? calcFixed : fixed;
            final int finalKnown = hasExecCounts ? calcKnown : known;
            final int finalUnknown = hasExecCounts ? calcUnknown : unknown;
            final int finalIgnored = hasExecCounts ? calcIgnored : ignored;
            final double finalPassRate = finalTotal > 0 ? (double)(finalPass + finalFixed) / finalTotal * 100.0 : passRate;

            runEntity.setLocalesCsv(localesCsv);
            runEntity.setBrowsersCsv(browsersCsv);
            runEntity.setTotalTests(finalTotal);
            runEntity.setPassedCount(finalPass);
            runEntity.setSucceededFixedCount(finalFixed);
            runEntity.setFailedKnownCount(finalKnown);
            runEntity.setFailedUnknownCount(finalUnknown);
            runEntity.setIgnoredCount(finalIgnored);
            runEntity.setPassRate(finalPassRate);
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

    private String generateVariationId(final String testClass, final String dataSet, final String location, final String browser)
    {
        final String normBrowser = AuraReportDataService.normalizeBrowser(browser);
        final String raw = (testClass != null ? testClass : "") + "|" +
                           (dataSet != null ? dataSet : "") + "|" +
                           (location != null ? location : "") + "|" +
                           (normBrowser != null ? normBrowser : "");
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
