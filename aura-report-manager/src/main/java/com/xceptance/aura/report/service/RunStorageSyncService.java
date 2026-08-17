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
                    generateRunJsonFromTestExecutions(entry.toFile(), runId);
                    importOrUpdateRunReport(runId);
                    importedCount++;
                }
            }
        }
        catch (final IOException e)
        {
            LOG.error("Error scanning storage directory {}: {}", baseDir, e.getMessage());
        }

        return importedCount;
    }

    private void generateRunJsonFromTestExecutions(final File runDir, final String runId)
    {
        try
        {
            final List<File> testExecJsonFiles = new ArrayList<>();
            scanForTestExecJsonFiles(runDir, testExecJsonFiles);

            if (testExecJsonFiles.isEmpty())
            {
                return;
            }

            final ArrayNode execsArray = objectMapper.createArrayNode();
            int pass = 0, fixed = 0, known = 0, unknown = 0, ignoredCount = 0;

            final BatchInfo batchInfo = resolveOrCreateBatchJson(runDir, testExecJsonFiles);
            String batchName = batchInfo.name;
            String env = batchInfo.environment;
            String trigger = "Jenkins CI";
            String timestamp = "Today, 14:22:10";

            final File runJsonFile = new File(runDir, "run.json");
            if (runJsonFile.exists())
            {
                try
                {
                    final JsonNode existingRoot = objectMapper.readTree(runJsonFile);
                    if (existingRoot.has("batchName")) batchName = existingRoot.path("batchName").asText(batchName);
                    if (existingRoot.has("environment")) env = existingRoot.path("environment").asText(env);
                    if (existingRoot.has("trigger")) trigger = existingRoot.path("trigger").asText(trigger);
                    if (existingRoot.has("timestamp")) timestamp = existingRoot.path("timestamp").asText(timestamp);
                }
                catch (final Exception ignored)
                {
                }
            }

            for (final File f : testExecJsonFiles)
            {
                try
                {
                    final JsonNode node = objectMapper.readTree(f);
                    if (isTestExecutionJson(node))
                    {
                        if (node instanceof ObjectNode objNode)
                        {
                            final Path relPath = runDir.toPath().relativize(f.toPath());
                            if (relPath.getNameCount() >= 2)
                            {
                                final String folderArea = relPath.getName(0).toString();
                                final String folderClass = relPath.getName(1).toString();
                                objNode.put("areaName", folderArea);
                                if (!objNode.has("testClass") || objNode.path("testClass").asText().isEmpty())
                                {
                                    objNode.put("testClass", folderClass);
                                }
                            }

                            final String testClass = objNode.path("testClass").asText("");
                            final String title = objNode.path("title").asText("");
                            final String areaName = objNode.path("areaName").asText("General");
                            final String location = objNode.path("location").asText("US");
                            final String browser = objNode.path("browser").asText("Chrome");
                            final String engine = objNode.path("engine").asText("Java");

                            if (!objNode.has("runId") || objNode.path("runId").asText().isEmpty())
                            {
                                objNode.put("runId", runId);
                            }

                            if (!objNode.has("testName") || objNode.path("testName").asText().isEmpty())
                            {
                                objNode.put("testName", testClass + " · " + (title.isEmpty() ? "Default" : title));
                            }

                            if (!objNode.has("playbookFile") || objNode.path("playbookFile").asText().isEmpty())
                            {
                                objNode.put("playbookFile", "tests/suites/" + areaName.toLowerCase() + "/" + testClass + ".yml");
                            }

                            if (!objNode.has("testFile") || objNode.path("testFile").asText().isEmpty())
                            {
                                objNode.put("testFile", "com.xceptance.neodymium.aura.tests." + areaName.toLowerCase() + "." + testClass + "#executeTest");
                            }

                            if (!objNode.has("junitTags") || !objNode.path("junitTags").isArray() || objNode.path("junitTags").isEmpty())
                            {
                                final ArrayNode tags = objectMapper.createArrayNode();
                                tags.add(areaName);
                                tags.add(testClass);
                                if (!title.isEmpty()) tags.add("Dataset: " + title);
                                tags.add("Location: " + location);
                                tags.add("Browser: " + browser);
                                objNode.set("junitTags", tags);
                            }

                            if (!objNode.has("localDataBindings") || objNode.path("localDataBindings").isMissingNode() || objNode.path("localDataBindings").isEmpty())
                            {
                                final ObjectNode localData = objectMapper.createObjectNode();
                                localData.put("areaName", areaName);
                                localData.put("testClass", testClass);
                                if (!title.isEmpty()) localData.put("dataSet", title);
                                localData.put("location", location);
                                localData.put("browser", browser);
                                localData.put("engine", engine);
                                objNode.set("localDataBindings", localData);
                            }

                            if (!objNode.has("dataBindings") || objNode.path("dataBindings").isMissingNode() || objNode.path("dataBindings").isEmpty())
                            {
                                final ObjectNode dataBind = objectMapper.createObjectNode();
                                dataBind.put("neodymium.url", "https://staging.shop.xceptance.com");
                                dataBind.put("neodymium.selenide.timeout", "3000");
                                dataBind.put("neodymium.screenshots.enableOnSuccess", "true");
                                dataBind.put("location", location);
                                dataBind.put("browser", browser);
                                objNode.set("dataBindings", dataBind);
                            }

                            if (!objNode.has("blocks") || !objNode.path("blocks").isObject())
                            {
                                final ObjectNode blocksNode = objectMapper.createObjectNode();
                                final ArrayNode beforeArr = objectMapper.createArrayNode();
                                final ArrayNode stepsArr = objectMapper.createArrayNode();
                                final ArrayNode afterArr = objectMapper.createArrayNode();

                                final JsonNode legacySteps = objNode.path("steps");
                                final JsonNode triesNode = legacySteps.path("tries");
                                final JsonNode try1 = triesNode.has("1") ? triesNode.get("1") : (triesNode.elements().hasNext() ? triesNode.elements().next() : null);

                                if (try1 != null)
                                {
                                    populateBlockSteps(try1.path("beforeSteps"), beforeArr, "before", testClass, engine, objectMapper);
                                    populateBlockSteps(try1.path("coreSteps"), stepsArr, "playbook", testClass, engine, objectMapper);
                                    populateBlockSteps(try1.path("afterSteps"), afterArr, "after", testClass, engine, objectMapper);
                                }

                                blocksNode.set("before", beforeArr);
                                blocksNode.set("steps", stepsArr);
                                blocksNode.set("after", afterArr);

                                objNode.set("blocks", blocksNode);
                            }

                            try
                            {
                                objectMapper.writerWithDefaultPrettyPrinter().writeValue(f, objNode);
                            }
                            catch (final Exception ignored)
                            {
                            }
                        }

                        execsArray.add(node);
                        final String rawStatus = node.path("status").asText("passed-clean");
                        switch (rawStatus)
                        {
                            case "passed-clean", "passed", "succeeded" -> pass++;
                            case "succeeded-fixed" -> fixed++;
                            case "failed-known" -> known++;
                            case "failed-unknown", "failed", "error" -> unknown++;
                            case "ignored", "skipped" -> ignoredCount++;
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOG.warn("Could not parse test execution JSON file {}: {}", f.getAbsolutePath(), e.getMessage());
                }
            }

            final int total = execsArray.size();
            final double passRate = total > 0 ? (double)(pass + fixed) / total * 100.0 : 0.0;

            final ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("runId", runId);
            rootNode.put("batchName", batchName);
            rootNode.put("environment", env);
            rootNode.put("trigger", trigger);
            rootNode.put("timestamp", timestamp);

            final ObjectNode summaryNode = objectMapper.createObjectNode();
            summaryNode.put("total", total);
            summaryNode.put("pass", pass);
            summaryNode.put("fixed", fixed);
            summaryNode.put("known", known);
            summaryNode.put("unknown", unknown);
            summaryNode.put("ignored", ignoredCount);
            summaryNode.put("passRate", Math.round(passRate * 10.0) / 10.0);

            rootNode.set("summary", summaryNode);
            rootNode.set("executions", execsArray);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(runJsonFile, rootNode);
            LOG.info("Generated/Updated run.json for runId={} from {} test execution JSON files (Total: {}, Pass: {}, Known: {}, Unknown: {}).",
                runId, total, total, pass, known, unknown);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to generate run.json for runId {}: {}", runId, e.getMessage(), e);
        }
    }

    private void populateBlockSteps(final JsonNode sourceSteps, final ArrayNode targetArray, final String prefix, final String file, final String defaultEngine, final ObjectMapper mapper)
    {
        if (sourceSteps != null && sourceSteps.isArray())
        {
            int idx = 1;
            for (final JsonNode stepNode : sourceSteps)
            {
                final ObjectNode stepObj = mapper.createObjectNode();
                stepObj.put("id", prefix + "_" + (idx - 1));
                stepObj.put("index", idx);

                final String name = stepNode.path("name").asText(stepNode.path("title").asText(stepNode.path("instruction").asText("Step " + idx)));
                stepObj.put("instruction", name);
                stepObj.put("line", idx);
                stepObj.put("file", file + ".java");
                stepObj.put("source", stepNode.path("engine").asText(stepNode.path("source").asText(defaultEngine)));

                final boolean isPassed = stepNode.path("passed").asBoolean(true);
                stepObj.put("status", isPassed ? "passed" : "failed");
                stepObj.put("screenshot", stepNode.path("screenshot").asText(""));
                stepObj.put("error", stepNode.path("error").asText(""));

                final ArrayNode actionsArr = mapper.createArrayNode();
                final JsonNode sourceActions = stepNode.path("actions");
                if (sourceActions.isArray())
                {
                    for (final JsonNode actNode : sourceActions)
                    {
                        final ObjectNode actObj = mapper.createObjectNode();
                        final String actName = actNode.path("name").asText(actNode.path("type").asText("ACTION"));
                        actObj.put("type", actName);
                        actObj.put("name", actName);
                        actObj.put("target", actNode.path("target").asText(""));
                        actObj.put("value", actNode.path("value").asText(""));
                        actObj.put("description", actNode.path("description").asText("Executed " + actName + " action"));
                        actionsArr.add(actObj);
                    }
                }
                stepObj.set("actions", actionsArr);
                targetArray.add(stepObj);
                idx++;
            }
        }
    }

    private void scanForTestExecJsonFiles(final File dir, final List<File> results)
    {
        final File[] files = dir.listFiles();
        if (files == null) return;

        for (final File f : files)
        {
            if (f.isDirectory())
            {
                scanForTestExecJsonFiles(f, results);
            }
            else if (f.getName().endsWith(".json") && !"run.json".equalsIgnoreCase(f.getName()))
            {
                results.add(f);
            }
        }
    }

    private boolean isTestExecutionJson(final JsonNode node)
    {
        return node.isObject() && (node.has("status") || node.has("testClass") || node.has("id") || node.has("testName"));
    }

    private void importOrUpdateRunReport(final String runId)
    {
        try
        {
            final Optional<String> fullJsonOpt = localRunJsonStorageService.readRunJson(runId);
            if (fullJsonOpt.isEmpty())
            {
                return;
            }

            final JsonNode root = objectMapper.readTree(fullJsonOpt.get());

            final File runDir = new File(baseDir, runId);
            final List<File> testExecJsonFiles = new ArrayList<>();
            if (runDir.exists() && runDir.isDirectory())
            {
                scanForTestExecJsonFiles(runDir, testExecJsonFiles);
            }
            final BatchInfo batchInfo = resolveOrCreateBatchJson(runDir, testExecJsonFiles);

            final String batchName = root.path("batchName").asText(batchInfo.name);
            final String env = root.path("environment").asText(batchInfo.environment);
            final String batchDesc = batchInfo.description;
            final String trigger = root.path("trigger").asText(root.path("triggerSource").asText("Jenkins CI"));
            final String timestamp = root.path("timestamp").asText(root.path("timestampLabel").asText("Today, 14:22:10"));

            long runStartTime = root.path("startTimeMs").asLong(0L);
            if (runStartTime <= 0L)
            {
                try
                {
                    runStartTime = Long.parseLong(runId) * 100000000L;
                }
                catch (final Exception e)
                {
                    runStartTime = System.currentTimeMillis();
                }
            }
            final long finalRunStartTime = runStartTime;

            final TestRunEntity runEntity = runRepository.findById(runId).orElseGet(() -> new TestRunEntity(
                runId,
                batchName,
                "COMPLETED",
                trigger,
                env,
                "US, EU, DE",
                "Chrome, Firefox, Edge",
                timestamp,
                finalRunStartTime
            ));

            runEntity.setBatchName(batchName);
            runEntity.setEnvironment(env);
            runEntity.setTriggerSource(trigger);
            runEntity.setTimestampLabel(timestamp);
            runEntity.setStartTimeMs(finalRunStartTime);

            int pass = 0, fixed = 0, known = 0, unknown = 0, ignored = 0;
            final JsonNode execArray = root.path("executions");
            if (execArray.isArray())
            {
                for (final JsonNode exec : execArray)
                {
                    final String testClass = exec.path("testClass").asText("");
                    final String title = exec.path("title").asText("");
                    final String location = exec.path("location").asText("US");
                    final String browser = exec.path("browser").asText("Chrome");
                    final String areaName = exec.path("areaName").asText("General");

                    final String varId = (!testClass.isEmpty()) ? generateVariationId(testClass, title, location, browser) : "var_" + Math.abs((testClass + title).hashCode());
                    final List<String> envs = List.of(env, "ALL");
                    final boolean hasBugsInDb = !bugRepository.findByVariationIdAndEnvironmentIn(varId, envs).isEmpty();

                    final String rawStatusInput = exec.path("status").asText("passed");
                    final String normalizedRaw;
                    if ("failed".equalsIgnoreCase(rawStatusInput) || "failed-known".equalsIgnoreCase(rawStatusInput) || "failed-unknown".equalsIgnoreCase(rawStatusInput))
                    {
                        normalizedRaw = "failed";
                    }
                    else if ("passed".equalsIgnoreCase(rawStatusInput) || "passed-clean".equalsIgnoreCase(rawStatusInput) || "succeeded-fixed".equalsIgnoreCase(rawStatusInput))
                    {
                        normalizedRaw = "passed";
                    }
                    else
                    {
                        normalizedRaw = "ignored";
                    }

                    final String status;
                    if ("failed".equalsIgnoreCase(normalizedRaw))
                    {
                        status = hasBugsInDb ? "failed-known" : "failed-unknown";
                    }
                    else if ("passed".equalsIgnoreCase(normalizedRaw))
                    {
                        status = hasBugsInDb ? "succeeded-fixed" : "passed-clean";
                    }
                    else
                    {
                        status = "ignored";
                    }

                    switch (status)
                    {
                        case "passed-clean" -> pass++;
                        case "succeeded-fixed" -> fixed++;
                        case "failed-known" -> known++;
                        case "failed-unknown" -> unknown++;
                        case "ignored" -> ignored++;
                    }

                    String formattedArea = areaName != null && !areaName.trim().isEmpty() ? areaName.trim() : "@General";
                    if (!formattedArea.startsWith("@"))
                    {
                        formattedArea = "@" + formattedArea;
                    }

                    final String finalArea = formattedArea;
                    final TestBaseVariationEntity var = variationRepository.findById(varId)
                        .orElseGet(() -> new TestBaseVariationEntity(varId, testClass, title, finalArea, location, browser));

                    var.setAreaTag(finalArea);
                    var.setTestClassName(testClass);
                    var.setTotalExecutionsCount(var.getTotalExecutionsCount() + 1);
                    var.setLastStatus(status);
                    var.setLastExecutedAt(System.currentTimeMillis());
                    variationRepository.save(var);
                }
            }

            final JsonNode summaryNode = root.path("summary");
            final int totalTests;

            if (summaryNode.isObject() && summaryNode.has("total"))
            {
                pass = summaryNode.path("pass").asInt(pass);
                fixed = summaryNode.path("fixed").asInt(fixed);
                known = summaryNode.path("known").asInt(known);
                unknown = summaryNode.path("unknown").asInt(unknown);
                ignored = summaryNode.path("ignored").asInt(ignored);
                totalTests = summaryNode.path("total").asInt();
            }
            else
            {
                totalTests = execArray.isArray() ? execArray.size() : (pass + fixed + known + unknown + ignored);
            }

            runEntity.setTotalTests(totalTests);
            runEntity.setPassedCount(pass);
            runEntity.setSucceededFixedCount(fixed);
            runEntity.setFailedKnownCount(known);
            runEntity.setFailedUnknownCount(unknown);
            runEntity.setIgnoredCount(ignored);
            runEntity.recalculatePassRate();
            runEntity.setRunJsonPath("storage/runs/" + runId + "/run.json");
            runEntity.setAttachmentsSyncedToS3(true);

            runRepository.save(runEntity);

            final Optional<TestBatchEntity> batchOpt = batchRepository.findById(batchName);
            if (batchOpt.isPresent())
            {
                final TestBatchEntity b = batchOpt.get();
                b.setLatestRunId(runId);
                b.setEnvironment(env);
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
                    batchDesc != null && !batchDesc.isEmpty() ? batchDesc : "Nightly regression and smoke test suite.",
                    "US, EU, DE",
                    "Chrome, Firefox, Edge",
                    runId
                ));
            }

            LOG.info("Imported/Updated run JSON report from storage into DB: runId={}, batch={}, totalTests={}, pass={}, fixed={}, known={}, unknown={}",
                runId, batchName, totalTests, pass, fixed, known, unknown);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to parse and import run report for runId {}: {}", runId, e.getMessage(), e);
        }
    }

    public static class BatchInfo
    {
        public final String name;
        public final String description;
        public final String environment;

        public BatchInfo(final String name, final String description, final String environment)
        {
            this.name = name;
            this.description = description;
            this.environment = environment;
        }
    }

    private BatchInfo resolveOrCreateBatchJson(final File runDir, final List<File> testExecJsonFiles)
    {
        final File batchJsonFile = new File(runDir, "batch.json");
        if (batchJsonFile.exists())
        {
            try
            {
                final JsonNode batchNode = objectMapper.readTree(batchJsonFile);
                final String name = batchNode.path("name").asText(batchNode.path("batchName").asText("Unknown"));
                final String desc = batchNode.path("description").asText("");
                final String env = batchNode.path("environment").asText(batchNode.path("env").asText("Unknown"));
                return new BatchInfo(name, desc, env);
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read existing batch.json in {}: {}", runDir.getAbsolutePath(), e.getMessage());
            }
        }

        // Extract environment from test execution JSONs if present, else default "Unknown"
        String detectedEnv = null;
        if (testExecJsonFiles != null)
        {
            for (final File f : testExecJsonFiles)
            {
                try
                {
                    final JsonNode node = objectMapper.readTree(f);
                    if (node.has("environment") && !node.path("environment").asText().isEmpty())
                    {
                        detectedEnv = node.path("environment").asText();
                        break;
                    }
                    if (node.has("env") && !node.path("env").asText().isEmpty())
                    {
                        detectedEnv = node.path("env").asText();
                        break;
                    }
                    if (node.path("dataBindings").has("environment") && !node.path("dataBindings").path("environment").asText().isEmpty())
                    {
                        detectedEnv = node.path("dataBindings").path("environment").asText();
                        break;
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
        }

        final String finalEnv = (detectedEnv != null && !detectedEnv.trim().isEmpty()) ? detectedEnv.trim() : "Unknown";
        final String defaultName = "Unknown";
        final String defaultDesc = "";

        try
        {
            final ObjectNode batchNode = objectMapper.createObjectNode();
            batchNode.put("name", defaultName);
            batchNode.put("description", defaultDesc);
            batchNode.put("environment", finalEnv);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(batchJsonFile, batchNode);
            LOG.info("Created batch.json for runDir={} with default name='Unknown', description='', environment='{}'", runDir.getName(), finalEnv);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to write default batch.json in {}: {}", runDir.getAbsolutePath(), e.getMessage());
        }

        return new BatchInfo(defaultName, defaultDesc, finalEnv);
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
