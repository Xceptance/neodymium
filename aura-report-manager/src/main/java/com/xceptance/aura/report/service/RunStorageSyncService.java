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
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
                    localRunJsonStorageService.generateRunJsonFromTestExecutions(entry.toFile(), runId);
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

        return importedCount;
    }

    private boolean importOrUpdateRunReport(final String runId)
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

            if (execArray.isArray())
            {
                for (final JsonNode exec : execArray)
                {
                    final String testClass = exec.path("testClass").asText("UnknownClass");
                    final String dataSet = exec.path("title").asText("");
                    final String location = exec.has("locale") && !exec.path("locale").asText().trim().isEmpty() ? exec.path("locale").asText().trim() : exec.path("location").asText("Unknown");
                    final String browser = AuraReportDataService.normalizeBrowser(exec.path("browser").asText("Chrome"));
                    final String rawStatus = exec.path("status").asText("passed-clean");

                    final String varId = generateVariationId(testClass, dataSet, location, browser);

                    final Optional<TestBaseVariationEntity> varOpt = variationRepository.findById(varId);
                    final TestBaseVariationEntity varEntity;
                    if (varOpt.isPresent())
                    {
                        varEntity = varOpt.get();
                    }
                    else
                    {
                        varEntity = new TestBaseVariationEntity(varId, testClass, dataSet, "@General", location, browser);
                    }

                    varEntity.setTotalExecutionsCount(varEntity.getTotalExecutionsCount() + 1);
                    varEntity.setLastStatus(rawStatus);
                    varEntity.setLastExecutedAt(System.currentTimeMillis());
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

            runEntity.setLocalesCsv(localesCsv);
            runEntity.setBrowsersCsv(browsersCsv);
            runEntity.setTotalTests(totalTests);
            runEntity.setPassedCount(pass);
            runEntity.setSucceededFixedCount(fixed);
            runEntity.setFailedKnownCount(known);
            runEntity.setFailedUnknownCount(unknown);
            runEntity.setIgnoredCount(ignored);
            runEntity.setPassRate(passRate);
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
