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
import com.xceptance.aura.report.dto.RunReportDto;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.neodymium.ai.util.AtomicFileUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service managing local disk storage for structured run report JSON files (storage/runs/{runId}/run.json and storage/runs/run-{runId}.json).
 * Compatible with Neodymium Aura runs directory organization.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Service
public class LocalRunJsonStorageService
{
    private static final Logger LOG = LoggerFactory.getLogger(LocalRunJsonStorageService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aura.report.storage.runs.base-dir:storage/runs/}")
    private String baseDir;

    public Path getRunDir(final String runId)
    {
        return Paths.get(baseDir, runId);
    }

    /**
     * Checks whether a run.json report file already exists on disk for the given run, either nested inside the run
     * directory ({@code storage/runs/{runId}/run.json}) or as a flat file ({@code storage/runs/run-{runId}.json}).
     *
     * @param runId the unique run identifier
     * @return {@code true} if a run.json report file exists on disk, {@code false} otherwise
     */
    public boolean hasRunJsonFile(final String runId)
    {
        return Files.exists(Paths.get(baseDir, runId, "run.json")) || Files.exists(Paths.get(baseDir, "run-" + runId + ".json"));
    }

    /**
     * Efficiently checks whether the given run currently has active test executions on disk.
     * Uses file modification timestamps to avoid scanning or parsing older completed tests.
     *
     * @param runId the run identifier
     * @return {@code true} if any test execution JSON was modified recently and reports an active status
     */
    public boolean isRunCurrentlyActiveOnDisk(final String runId)
    {
        if (runId == null || runId.isBlank())
        {
            return false;
        }

        final Path runDir = getRunDir(runId);
        if (runDir == null || !Files.exists(runDir) || !Files.isDirectory(runDir))
        {
            return false;
        }

        final long now = System.currentTimeMillis();
        final long activeThresholdMs = 120_000L; // 2 minutes

        final List<File> candidateFiles = new ArrayList<>();
        scanForTestExecJsonFiles(runDir.toFile(), candidateFiles);

        for (final File f : candidateFiles)
        {
            if (now - f.lastModified() <= activeThresholdMs)
            {
                try
                {
                    final JsonNode node = objectMapper.readTree(f);
                    final String status = node.path("status").asText("");
                    if ("running".equalsIgnoreCase(status)
                            || "in_progress".equalsIgnoreCase(status)
                            || "executing".equalsIgnoreCase(status)
                            || "pending".equalsIgnoreCase(status)
                            || status.isBlank())
                    {
                        return true;
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
        }

        return false;
    }

    public Path getRunJsonPath(final String runId)
    {
        final Path dirPath = Paths.get(baseDir, runId, "run.json");
        if (Files.exists(dirPath))
        {
            return dirPath;
        }
        return Paths.get(baseDir, "run-" + runId + ".json");
    }

    public void writeRunJson(final String runId, final String jsonContent) throws IOException
    {
        final Path path = getRunJsonPath(runId);
        final Path parent = path.getParent();
        if (parent != null)
        {
            Files.createDirectories(parent);
        }
        AtomicFileUtils.writeStringAtomic(path, jsonContent);
        LOG.info("Saved run JSON to disk: {}", path.toAbsolutePath());
    }

    public Optional<String> readRunJson(final String runId)
    {
        final Path nestedRunJson = Paths.get(baseDir, runId, "run.json");

        if (Files.exists(nestedRunJson))
        {
            try
            {
                return Optional.of(Files.readString(nestedRunJson, StandardCharsets.UTF_8));
            }
            catch (final Exception e)
            {
                LOG.error("Failed to read nested run JSON from disk for run {}: {}", runId, e.getMessage());
            }
        }

        final Path flatRunJson = Paths.get(baseDir, "run-" + runId + ".json");
        if (Files.exists(flatRunJson))
        {
            try
            {
                return Optional.of(Files.readString(flatRunJson, StandardCharsets.UTF_8));
            }
            catch (final IOException e)
            {
                LOG.error("Failed to read flat run JSON from disk for run {}: {}", runId, e.getMessage());
            }
        }

        final Path runDir = Paths.get(baseDir, runId);
        if (Files.exists(runDir) && Files.isDirectory(runDir))
        {
            return buildRunJsonContent(runDir.toFile(), runId, true);
        }

        return Optional.empty();
    }

    public void generateRunJsonFromTestExecutions(final File runDir, final String runId)
    {
        buildRunJsonContent(runDir, runId, true);
    }

    public Optional<String> buildRunJsonContent(final File runDir, final String runId)
    {
        return buildRunJsonContent(runDir, runId, true);
    }

    public Optional<String> buildRunJsonContent(final File runDir, final String runId, final boolean writeToDisk)
    {
        return buildRunJsonContent(runDir, runId, writeToDisk, isRunCurrentlyActiveOnDisk(runId));
    }

    public Optional<String> buildRunJsonContent(final File runDir, final String runId, final boolean writeToDisk, final boolean isRunActive)
    {
        try
        {
            final List<File> testExecJsonFiles = new ArrayList<>();
            scanForTestExecJsonFiles(runDir, testExecJsonFiles);

            if (testExecJsonFiles.isEmpty())
            {
                return Optional.empty();
            }

            final ArrayNode mergedExecutions = objectMapper.createArrayNode();
            final Map<String, Map<String, List<String>>> areasMap = new LinkedHashMap<>();
            // Cache remembering which first-level folders are category folders (contain class subfolders)
            final Map<String, Boolean> categoryFolderCache = new LinkedHashMap<>();
            final Set<String> localesSet = new LinkedHashSet<>();
            final Set<String> browsersSet = new LinkedHashSet<>();
            final List<String> existingLocales = new ArrayList<>();
            final List<String> existingBrowsers = new ArrayList<>();
            int totalExecsCount = 0;
            int pass = 0, fixed = 0, known = 0, unknown = 0, ignoredCount = 0, runningCount = 0;
            int passHealed = 0, fixedHealed = 0, knownHealed = 0, unknownHealed = 0;
            long earliestMs = Long.MAX_VALUE;
            String earliestTimeStr = null;

            final BatchInfo batchInfo = resolveOrCreateBatchJson(runDir, testExecJsonFiles);
            String batchName = batchInfo.name;
            String env = batchInfo.environment;
            String trigger = "Unknown";
            String timestamp = "Recently";

            final Map<String, List<String>> existingTags = new LinkedHashMap<>();
            final File batchJsonFile = new File(runDir, "batch.json");
            if (batchJsonFile.exists())
            {
                try
                {
                    final JsonNode batchNode = objectMapper.readTree(batchJsonFile);
                    if (batchNode.has("tags") && batchNode.path("tags").isObject())
                    {
                        batchNode.path("tags").fields().forEachRemaining(entry -> {
                            final String k = entry.getKey().trim();
                            if (!k.isEmpty())
                            {
                                final List<String> list = existingTags.computeIfAbsent(k, key -> new ArrayList<>());
                                if (entry.getValue().isArray())
                                {
                                    for (final JsonNode item : entry.getValue())
                                    {
                                        final String s = item.asText("").trim();
                                        if (!s.isEmpty() && !list.contains(s))
                                        {
                                            list.add(s);
                                        }
                                    }
                                }
                                else
                                {
                                    final String s = entry.getValue().asText("").trim();
                                    if (!s.isEmpty() && !list.contains(s))
                                    {
                                        list.add(s);
                                    }
                                }
                            }
                        });
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
            final File runJsonFile = new File(runDir, "run.json");
            if (runJsonFile.exists())
            {
                try
                {
                    final JsonNode existingRoot = objectMapper.readTree(runJsonFile);
                    if (existingRoot.has("tags") && existingRoot.path("tags").isObject())
                    {
                        existingRoot.path("tags").fields().forEachRemaining(entry -> {
                            final String k = entry.getKey().trim();
                            if (!k.isEmpty())
                            {
                                final List<String> list = existingTags.computeIfAbsent(k, key -> new ArrayList<>());
                                if (entry.getValue().isArray())
                                {
                                    for (final JsonNode item : entry.getValue())
                                    {
                                        final String s = item.asText("").trim();
                                        if (!s.isEmpty() && !list.contains(s))
                                        {
                                            list.add(s);
                                        }
                                    }
                                }
                                else
                                {
                                    final String s = entry.getValue().asText("").trim();
                                    if (!s.isEmpty() && !list.contains(s))
                                    {
                                        list.add(s);
                                    }
                                }
                            }
                        });
                    }
                    if (existingRoot.has("batchName")) batchName = existingRoot.path("batchName").asText(batchName);
                    if (existingRoot.has("environment")) env = existingRoot.path("environment").asText(env);
                    if (existingRoot.has("trigger")) trigger = existingRoot.path("trigger").asText(trigger);
                    if (existingRoot.has("timestamp")) timestamp = existingRoot.path("timestamp").asText(timestamp);
                    if (existingRoot.has("locales") && existingRoot.path("locales").isArray())
                    {
                        for (final JsonNode l : existingRoot.path("locales"))
                        {
                            final String val = l.asText("").trim();
                            if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val))
                            {
                                existingLocales.add(val);
                            }
                        }
                    }
                    if (existingRoot.has("browsers") && existingRoot.path("browsers").isArray())
                    {
                        for (final JsonNode b : existingRoot.path("browsers"))
                        {
                            final String val = b.asText("").trim();
                            if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val))
                            {
                                existingBrowsers.add(val);
                            }
                        }
                    }
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
                        final boolean isSynthetic = node.path("synthetic").asBoolean(false) || node.path("fixture").asBoolean(false);
                        if (isSynthetic)
                        {
                            continue;
                        }

                        if (node.has("tags") && node.path("tags").isObject())
                        {
                            node.path("tags").fields().forEachRemaining(entry -> {
                                final String k = entry.getKey().trim();
                                if (!k.isEmpty())
                                {
                                    final List<String> list = existingTags.computeIfAbsent(k, key -> new ArrayList<>());
                                    if (entry.getValue().isArray())
                                    {
                                        for (final JsonNode item : entry.getValue())
                                        {
                                            final String s = item.asText("").trim();
                                            if (!s.isEmpty() && !list.contains(s))
                                            {
                                                list.add(s);
                                            }
                                        }
                                    }
                                    else
                                    {
                                        final String s = entry.getValue().asText("").trim();
                                        if (!s.isEmpty() && !list.contains(s))
                                        {
                                            list.add(s);
                                        }
                                    }
                                }
                            });
                        }
                        if (node.has("variationTags") && node.path("variationTags").isObject())
                        {
                            node.path("variationTags").fields().forEachRemaining(entry -> {
                                final String k = entry.getKey().trim();
                                if (!k.isEmpty())
                                {
                                    final List<String> list = existingTags.computeIfAbsent(k, key -> new ArrayList<>());
                                    final String s = entry.getValue().asText("").trim();
                                    if (!s.isEmpty() && !list.contains(s))
                                    {
                                        list.add(s);
                                    }
                                }
                            });
                        }

                        extractLocalesFromNode(node, localesSet);
                        extractBrowsersFromNode(node, browsersSet);

                        final ExecutionTime execTime = extractExecutionStartTime(node);
                        if (execTime != null)
                        {
                            if (execTime.ms() < earliestMs)
                            {
                                earliestMs = execTime.ms();
                                earliestTimeStr = execTime.timeStr();
                            }
                            else if (earliestTimeStr == null && execTime.timeStr() != null)
                            {
                                earliestTimeStr = execTime.timeStr();
                            }
                        }

                        if (node instanceof ObjectNode objNode)
                        {
                            final Path relPath = runDir.toPath().relativize(f.toPath());
                            String folderArea = "";
                            String folderClass = "";
                            if (relPath.getNameCount() >= 3)
                            {
                                folderArea = relPath.getName(0).toString();
                                folderClass = relPath.getName(1).toString();
                            }
                            else if (relPath.getNameCount() == 2)
                            {
                                // A first-level folder counts as a category folder only when it contains
                                // subdirectories (class folders). Otherwise it is a plain class folder
                                // without a category that logically belongs to the default category while
                                // staying at its current location on disk.
                                final String firstSegment = relPath.getName(0).toString();
                                if (categoryFolderCache.computeIfAbsent(firstSegment, segment -> hasSubdirectories(runDir.toPath().resolve(segment))))
                                {
                                    folderArea = firstSegment;
                                }
                                else
                                {
                                    folderClass = firstSegment;
                                }
                            }

                            String rawClass = objNode.has("testClass") ? objNode.path("testClass").asText("") : "";
                            if (rawClass.endsWith(".json") || rawClass.equalsIgnoreCase(f.getName()))
                            {
                                rawClass = "";
                            }
                            if (rawClass.isEmpty() || "GeneralClass".equalsIgnoreCase(rawClass) || "DefaultClass".equalsIgnoreCase(rawClass))
                            {
                                if (!folderClass.isEmpty())
                                {
                                    rawClass = folderClass;
                                }
                                else if (objNode.has("testFile") && !objNode.path("testFile").asText().isEmpty())
                                {
                                    String tf = objNode.path("testFile").asText();
                                    if (tf.contains("#"))
                                    {
                                        tf = tf.substring(0, tf.indexOf('#'));
                                    }
                                    if (tf.contains("."))
                                    {
                                        tf = tf.substring(tf.lastIndexOf('.') + 1);
                                    }
                                    rawClass = tf;
                                }
                                else if (objNode.has("testId") && !objNode.path("testId").asText().isEmpty())
                                {
                                    rawClass = objNode.path("testId").asText().replaceAll("\\s+", "");
                                }
                                else
                                {
                                    rawClass = "DefaultClass";
                                }
                            }
                            final String testClass = rawClass.trim();

                            String rawArea = objNode.has("areaName") ? objNode.path("areaName").asText("") : (objNode.has("category") ? objNode.path("category").asText("") : "");
                            if (rawArea.equalsIgnoreCase(folderClass) || rawArea.equalsIgnoreCase(f.getName()) || rawArea.equalsIgnoreCase(testClass))
                            {
                                rawArea = "";
                            }
                            if (rawArea.isEmpty() || "General".equalsIgnoreCase(rawArea) || "Browsing (default)".equalsIgnoreCase(rawArea))
                            {
                                final String extractedFromNode = extractAreaFromNode(objNode);
                                if (!extractedFromNode.isEmpty())
                                {
                                    rawArea = extractedFromNode;
                                }
                            }
                            if (rawArea.isEmpty() || !AuraReportDataService.isRealArea(rawArea))
                            {
                                if (!folderArea.isEmpty() && AuraReportDataService.isRealArea(folderArea))
                                {
                                    rawArea = folderArea;
                                }
                                else
                                {
                                    rawArea = "Default";
                                }
                            }
                            final String areaName = rawArea.trim();

                            objNode.put("areaName", areaName);
                            String rawSubArea = objNode.has("subArea") ? objNode.path("subArea").asText("") : "";
                            if (rawSubArea.isEmpty())
                            {
                                final String extractedSub = extractSubAreaFromNode(objNode);
                                if (!extractedSub.isEmpty())
                                {
                                    rawSubArea = extractedSub;
                                }
                            }
                            if (!rawSubArea.isEmpty())
                            {
                                String cleanSub = rawSubArea.trim();
                                while (cleanSub.startsWith("@"))
                                {
                                    cleanSub = cleanSub.substring(1).trim();
                                }
                                objNode.put("subArea", cleanSub);
                            }
                            objNode.put("testClass", testClass);

                            if (!objNode.has("title") || objNode.path("title").asText().trim().isEmpty())
                            {
                                if (objNode.has("datasetId") && !objNode.path("datasetId").asText().trim().isEmpty())
                                {
                                    objNode.put("title", objNode.path("datasetId").asText().trim());
                                }
                                else if (objNode.has("testId") && !objNode.path("testId").asText().trim().isEmpty())
                                {
                                    objNode.put("title", objNode.path("testId").asText().trim());
                                }
                                else if (objNode.has("testName") && !objNode.path("testName").asText().trim().isEmpty()
                                        && !objNode.path("testName").asText().trim().equalsIgnoreCase(testClass))
                                {
                                    objNode.put("title", objNode.path("testName").asText().trim());
                                }
                                else
                                {
                                    objNode.put("title", "Default");
                                }
                            }

                            final String title = objNode.path("title").asText("Default");
                            String location = objNode.path("location").asText(objNode.path("locale").asText(""));
                            if (location.isEmpty() && objNode.has("tags") && objNode.path("tags").isObject())
                            {
                                final JsonNode tagLoc = objNode.path("tags").path("locale");
                                if (tagLoc.isArray() && tagLoc.size() > 0)
                                {
                                    location = tagLoc.get(0).asText("");
                                }
                                else if (!tagLoc.isMissingNode() && !tagLoc.isNull())
                                {
                                    location = tagLoc.asText("");
                                }
                            }
                            if (location.isEmpty())
                            {
                                location = "Default";
                            }
                            final String browser = objNode.path("browser").asText("Chrome");
                            final String engine = objNode.path("engine").asText("Java");

                            if (!objNode.has("id") || objNode.path("id").asText().isEmpty())
                            {
                                if (objNode.has("testId") && !objNode.path("testId").asText().isEmpty())
                                {
                                    objNode.put("id", objNode.path("testId").asText());
                                }
                                else if (objNode.has("datasetId") && !objNode.path("datasetId").asText().isEmpty())
                                {
                                    objNode.put("id", objNode.path("datasetId").asText());
                                }
                                else
                                {
                                    objNode.put("id", f.getName().replaceAll("\\.json$", ""));
                                }
                            }

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
                                if (!rawSubArea.isEmpty())
                                {
                                    String cleanSub = rawSubArea.trim();
                                    while (cleanSub.startsWith("@"))
                                    {
                                        cleanSub = cleanSub.substring(1).trim();
                                    }
                                    localData.put("subArea", cleanSub);
                                }
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

                                if (objNode.has("llmCalls") && !blocksNode.has("llmCalls"))
                                {
                                    blocksNode.set("llmCalls", objNode.get("llmCalls"));
                                }

                                objNode.set("blocks", blocksNode);
                            }
                            else if (objNode.has("llmCalls") && objNode.get("blocks").isObject())
                            {
                                final ObjectNode existingBlocks = (ObjectNode) objNode.get("blocks");
                                if (!existingBlocks.has("llmCalls"))
                                {
                                    existingBlocks.set("llmCalls", objNode.get("llmCalls"));
                                }
                            }

                            // Keep the execution JSON at its current location on disk (class folders
                            // without a category are only logically associated with the default
                            // category); persist the enriched JSON content in place.
                            try
                            {
                                final JsonNode existingNode = objectMapper.readTree(f);
                                if (!existingNode.equals(objNode))
                                {
                                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(f, objNode);
                                }
                            }
                            catch (final Exception ignored)
                            {
                            }
                            areasMap.computeIfAbsent(areaName, k -> new LinkedHashMap<>()).computeIfAbsent(testClass, k -> new ArrayList<>()).add(f.getName());
                            mergedExecutions.add(objNode);
                        }

                        totalExecsCount++;
                        final String rawStatus = node.path("status").asText("").trim();
                        final JsonNode bugsNode = node.path("bugs");
                        final boolean hasBugs = hasBugsNode(bugsNode);
                        final boolean isHealed = (node.has("healed") && node.path("healed").asBoolean(false))
                                              || (node.has("healedStepsCount") && node.path("healedStepsCount").asInt(0) > 0)
                                              || "healed".equalsIgnoreCase(rawStatus);

                        final String failureText = AuraReportDataService.extractFailureText(node);
                        final int failedSteps = extractStepsFailed(node);
                        final int totalSteps = node.has("totalStepsCount") ? node.path("totalStepsCount").asInt(0) : 0;
                        final String eff = AuraReportDataService.computeEffectiveStatus(rawStatus, totalSteps, failedSteps, hasBugs, failureText, isRunActive);

                        if ("failed-known".equals(eff))
                        {
                            known++;
                            if (isHealed)
                            {
                                knownHealed++;
                            }
                        }
                        else if ("failed-unknown".equals(eff))
                        {
                            unknown++;
                            if (isHealed)
                            {
                                unknownHealed++;
                            }
                        }
                        else if ("succeeded-fixed".equals(eff))
                        {
                            fixed++;
                            if (isHealed)
                            {
                                fixedHealed++;
                            }
                        }
                        else if ("passed-clean".equals(eff))
                        {
                            pass++;
                            if (isHealed)
                            {
                                passHealed++;
                            }
                        }
                        else if ("ignored".equals(eff))
                        {
                            ignoredCount++;
                        }
                        else if ("running".equals(eff))
                        {
                            runningCount++;
                        }
                        else
                        {
                            unknown++;
                            if (isHealed)
                            {
                                unknownHealed++;
                            }
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOG.warn("Could not parse test execution JSON file {}: {}", f.getAbsolutePath(), e.getMessage());
                }
            }

            if (!AuraReportDataService.isRealBatch(batchName))
            {
                final String tagBatch = AuraReportDataService.resolveEffectiveBatchNameFromMap(batchName, existingTags);
                if (AuraReportDataService.isRealBatch(tagBatch))
                {
                    batchName = tagBatch;
                }
            }

            final int total = totalExecsCount;
            final double passRate = total > 0 ? (double) Math.round((double)(pass + fixed) / total * 100.0) : 0.0;

            if (earliestTimeStr != null && !earliestTimeStr.isBlank() && !"Recently".equalsIgnoreCase(earliestTimeStr.trim()))
            {
                timestamp = earliestTimeStr;
            }

            final ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("runId", runId);
            rootNode.put("batchName", batchName);
            rootNode.put("environment", env);
            rootNode.put("trigger", trigger);
            rootNode.put("timestamp", timestamp);
            rootNode.put("startTime", timestamp);
            if (earliestMs != Long.MAX_VALUE && earliestMs > 0L)
            {
                rootNode.put("startTimeMs", earliestMs);
            }

            final ArrayNode localesArray = objectMapper.createArrayNode();
            if (!localesSet.isEmpty())
            {
                localesSet.stream().sorted().forEach(localesArray::add);
            }
            else if (!existingLocales.isEmpty())
            {
                existingLocales.forEach(localesArray::add);
            }
            else
            {
                localesArray.add("Default");
            }
            rootNode.set("locales", localesArray);

            if (browsersSet.size() > 1 && browsersSet.contains("Chrome") && browsersSet.stream().anyMatch(s -> s.startsWith("Chrome_")))
            {
                browsersSet.remove("Chrome");
            }
            final ArrayNode browsersArray = objectMapper.createArrayNode();
            if (!browsersSet.isEmpty())
            {
                browsersSet.stream().sorted().forEach(browsersArray::add);
            }
            else if (!existingBrowsers.isEmpty())
            {
                existingBrowsers.forEach(browsersArray::add);
            }
            else
            {
                browsersArray.add("Chrome");
            }
            rootNode.set("browsers", browsersArray);

            final ObjectNode summaryNode = objectMapper.createObjectNode();
            summaryNode.put("total", total);
            summaryNode.put("pass", pass);
            summaryNode.put("fixed", fixed);
            summaryNode.put("known", known);
            summaryNode.put("unknown", unknown);
            summaryNode.put("running", runningCount);
            summaryNode.put("ignored", ignoredCount);
            summaryNode.put("passHealed", passHealed);
            summaryNode.put("fixedHealed", fixedHealed);
            summaryNode.put("knownHealed", knownHealed);
            summaryNode.put("unknownHealed", unknownHealed);
            summaryNode.put("passRate", Math.round(passRate));

            final ArrayNode areasArray = objectMapper.createArrayNode();
            for (final Map.Entry<String, Map<String, List<String>>> areaEntry : areasMap.entrySet())
            {
                final String aName = areaEntry.getKey();
                final ObjectNode areaObj = objectMapper.createObjectNode();
                areaObj.put("areaName", aName);
                final String cleanGroup = aName.replaceAll("[\\s()@]+", "");
                areaObj.put("areaGroup", "areaGroup" + cleanGroup);
                areaObj.put("folder", aName);

                final ArrayNode testClassesArray = objectMapper.createArrayNode();
                for (final Map.Entry<String, List<String>> classEntry : areaEntry.getValue().entrySet())
                {
                    final String cName = classEntry.getKey();
                    final ObjectNode classObj = objectMapper.createObjectNode();
                    classObj.put("className", cName);
                    final String cleanContainer = cName.replaceAll("[\\s.]+", "");
                    classObj.put("classContainer", "classContainer" + cleanContainer);
                    classObj.put("folder", cName);

                    final ArrayNode executionsArray = objectMapper.createArrayNode();
                    for (final String execName : classEntry.getValue())
                    {
                        executionsArray.add(execName);
                    }
                    classObj.set("executions", executionsArray);
                    testClassesArray.add(classObj);
                }
                areaObj.set("testClasses", testClassesArray);
                areasArray.add(areaObj);
            }

            final ObjectNode executionMetricsNode = objectMapper.createObjectNode();
            for (final JsonNode execNode : mergedExecutions)
            {
                if (execNode instanceof ObjectNode objNode)
                {
                    final String tClass = objNode.path("testClass").asText("DefaultClass");
                    final String tMethod = extractTestMethod(objNode);
                    final String tTitle = objNode.path("title").asText("Default");
                    final String tBrowser = objNode.path("browser").asText("Chrome");
                    final String key = buildExecutionKey(tClass, tMethod, tTitle, tBrowser);

                    String loc = objNode.path("location").asText(objNode.path("locale").asText(""));
                    if (loc.isEmpty() && objNode.has("tags") && objNode.path("tags").isObject())
                    {
                        final JsonNode tagLoc = objNode.path("tags").path("locale");
                        if (tagLoc.isArray() && tagLoc.size() > 0)
                        {
                            loc = tagLoc.get(0).asText("");
                        }
                        else if (!tagLoc.isMissingNode() && !tagLoc.isNull())
                        {
                            loc = tagLoc.asText("");
                        }
                    }
                    if (loc.isEmpty())
                    {
                        loc = "Default";
                    }
                    final ObjectNode metricObj = objectMapper.createObjectNode();
                    metricObj.put("id", objNode.path("id").asText(""));
                    metricObj.put("testClass", tClass);
                    metricObj.put("testMethod", tMethod);
                    metricObj.put("title", tTitle);
                    metricObj.put("location", loc);
                    metricObj.put("browser", tBrowser);
                    final ArrayNode normalizedBugs = objectMapper.createArrayNode();
                    final JsonNode rawBugs = objNode.path("bugs");
                    if (rawBugs.isArray())
                    {
                        for (final JsonNode bNode : rawBugs)
                        {
                            final String ticket = AuraReportDataService.extractTicket(bNode);
                            if (!ticket.isBlank())
                            {
                                normalizedBugs.add(ticket);
                            }
                        }
                    }
                    else if (rawBugs.isTextual() || rawBugs.isObject())
                    {
                        final String ticket = AuraReportDataService.extractTicket(rawBugs);
                        if (!ticket.isBlank())
                        {
                            normalizedBugs.add(ticket);
                        }
                    }

                    final String baseStatus = objNode.path("status").asText("failed-unknown");
                    final boolean hasBugsNode = normalizedBugs.size() > 0 || (objNode.has("bugs") && objNode.get("bugs").isArray() && objNode.get("bugs").size() > 0);
                    final String failureText = AuraReportDataService.extractFailureText(objNode);
                    final int failedSteps = extractStepsFailed(objNode);
                    final int totalSteps = objNode.has("totalStepsCount") ? objNode.path("totalStepsCount").asInt(0) : 0;
                    final String effStatus = AuraReportDataService.computeEffectiveStatus(baseStatus, totalSteps, failedSteps, hasBugsNode, failureText, isRunActive);
                    metricObj.put("status", effStatus);
                    metricObj.put("areaName", objNode.path("areaName").asText("Default"));
                    if (objNode.has("startTime"))
                    {
                        metricObj.put("startTime", objNode.path("startTime").asText(""));
                        final Long parsedStartMs = RunStorageSyncService.parseStartTimeMs(objNode);
                        if (parsedStartMs != null)
                        {
                            metricObj.put("timestampMs", parsedStartMs);
                        }
                    }
                    final String execMode = objNode.hasNonNull("executionMode")
                        ? objNode.path("executionMode").asText("")
                        : objNode.path("mode").asText("");
                    metricObj.put("executionMode", execMode);
                    final long durMs = objNode.hasNonNull("duration") ? objNode.path("duration").asLong(0L) : objNode.path("durationMs").asLong(0L);
                    metricObj.put("durationMs", durMs);
                    metricObj.put("durationFormatted", objNode.hasNonNull("durationFormatted") ? objNode.path("durationFormatted").asText() : RunStorageSyncService.formatDurationMs(durMs));

                    final int healedSteps = extractStepsHealed(objNode);
                    final boolean isHealed = (objNode.has("healed") && objNode.path("healed").asBoolean(false))
                        || healedSteps > 0
                        || "healed".equalsIgnoreCase(baseStatus);

                    metricObj.put("totalStepsCount", extractStepsTotal(objNode));
                    metricObj.put("failedStepsCount", extractStepsFailed(objNode));
                    metricObj.put("healedStepsCount", healedSteps);
                    metricObj.put("healed", isHealed);
                    metricObj.put("llmCallsCount", extractLlmCalls(objNode));
                    metricObj.put("llmTotalTokens", extractLlmTokens(objNode));
                    metricObj.put("inputTokensCount", extractInputTokens(objNode));
                    metricObj.put("outputTokensCount", extractOutputTokens(objNode));
                    metricObj.put("cachedTokensCount", extractCachedTokens(objNode));
                    metricObj.put("llmCost", extractLlmCost(objNode));
                    if (objNode.has("metrics"))
                    {
                        metricObj.set("metrics", objNode.get("metrics"));
                    }
                    if (objNode.has("llmResponsibility"))
                    {
                        metricObj.set("llmResponsibility", objNode.get("llmResponsibility"));
                    }
                    metricObj.set("bugs", normalizedBugs);
                    if (objNode.has("tags"))
                    {
                        metricObj.set("tags", objNode.get("tags"));
                    }

                    String uniqueKey = key;
                    if (executionMetricsNode.has(uniqueKey))
                    {
                        final String execId = objNode.path("id").asText("");
                        if (!execId.isEmpty() && !execId.equalsIgnoreCase(tTitle) && !execId.equalsIgnoreCase("default"))
                        {
                            uniqueKey = key + "#" + execId;
                        }
                        else
                        {
                            int count = 2;
                            while (executionMetricsNode.has(key + "#" + count))
                            {
                                count++;
                            }
                            uniqueKey = key + "#" + count;
                        }
                    }

                    final String origId = objNode.path("id").asText("");
                    if (origId.isEmpty() || origId.equalsIgnoreCase(tTitle) || origId.equalsIgnoreCase("default") || origId.equalsIgnoreCase("bad") || origId.equalsIgnoreCase("perfect"))
                    {
                        metricObj.put("id", uniqueKey);
                    }

                    executionMetricsNode.set(uniqueKey, metricObj);
                }
            }

            final ObjectNode compactRootNode = objectMapper.createObjectNode();
            if (earliestTimeStr != null && !earliestTimeStr.isBlank())
            {
                compactRootNode.put("timestamp", earliestTimeStr);
                compactRootNode.put("startTime", earliestTimeStr);
            }
            if (earliestMs != Long.MAX_VALUE && earliestMs > 0L)
            {
                compactRootNode.put("startTimeMs", earliestMs);
            }
            final boolean isStillActive = isRunCurrentlyActiveOnDisk(runId);
            final String runStatus = (isStillActive || runningCount > 0) ? "IN_PROGRESS" : "COMPLETED";
            compactRootNode.put("status", runStatus);
            compactRootNode.set("summary", summaryNode);
            compactRootNode.set("executionMetrics", executionMetricsNode);

            final ObjectNode tagsNode = objectMapper.createObjectNode();
            if (!existingTags.isEmpty())
            {
                existingTags.forEach((k, vList) -> {
                    if (vList != null && !vList.isEmpty())
                    {
                        if (vList.size() == 1)
                        {
                            tagsNode.put(k, vList.get(0));
                        }
                        else
                        {
                            final ArrayNode arr = tagsNode.putArray(k);
                            vList.forEach(arr::add);
                        }
                    }
                });
            }
            final boolean hasSuiteInTags = tagsNode.has("Suite") || tagsNode.has("suite") || tagsNode.has("Batch") || tagsNode.has("batch");
            if (!hasSuiteInTags && AuraReportDataService.isRealBatch(batchName))
            {
                tagsNode.put("Suite", batchName.trim());
            }
            final boolean hasEnvInTags = tagsNode.has("Instance") || tagsNode.has("Environment") || tagsNode.has("env");
            if (!hasEnvInTags && AuraReportDataService.isRealEnvironment(env))
            {
                tagsNode.put("Instance", env.trim());
            }
            compactRootNode.set("locales", localesArray);
            compactRootNode.set("browsers", browsersArray);
            compactRootNode.set("tags", tagsNode);

            final String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(compactRootNode);
            if (writeToDisk)
            {
                try
                {
                    AtomicFileUtils.writeStringAtomic(runJsonFile.toPath(), jsonString);
                    LOG.info("Saved run.json for runId {} at {}", runId, runJsonFile.getAbsolutePath());
                }
                catch (final Exception e)
                {
                    LOG.error("Failed to write run.json file to disk for runId {}: {}", runId, e.getMessage());
                }
            }

            return Optional.of(jsonString);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to build run JSON content for runId {}: {}", runId, e.getMessage(), e);
        }
        return Optional.empty();
    }

    public boolean updateRunJsonSummaryStats(final File runDir, final RunReportDto report)
    {
        if (runDir == null || !runDir.exists() || report == null)
        {
            return false;
        }

        final File runJsonFile = new File(runDir, "run.json");
        if (!runJsonFile.exists())
        {
            return false;
        }

        try
        {
            final JsonNode root = objectMapper.readTree(runJsonFile);
            if (root instanceof ObjectNode rootObj)
            {
                final int total = report.getTotalCount();
                final int pass = report.getPassCount();
                final int fixed = report.getFixedCount();
                final int known = report.getKnownCount();
                final int unknown = report.getUnknownCount();
                final int ignored = report.getIgnoredCount();
                final double passRate = total > 0 ? (double) Math.round((pass + fixed) * 100.0 / total) : 0.0;

                final ObjectNode summaryNode = objectMapper.createObjectNode();
                summaryNode.put("total", total);
                summaryNode.put("pass", pass);
                summaryNode.put("fixed", fixed);
                summaryNode.put("known", known);
                summaryNode.put("unknown", unknown);
                summaryNode.put("ignored", ignored);
                summaryNode.put("passRate", passRate);

                rootObj.set("summary", summaryNode);

                objectMapper.writerWithDefaultPrettyPrinter().writeValue(runJsonFile, rootObj);
                LOG.info("Adjusted summary stats in run.json for runDir={} (Total: {}, Pass: {}, Fixed: {}, Known: {}, Unknown: {}, PassRate: {}%).",
                    runDir.getName(), total, pass, fixed, known, unknown, passRate);
                return true;
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed updating summary stats in run.json for {}: {}", runDir.getAbsolutePath(), e.getMessage());
        }

        return false;
    }

    public static String extractTestMethod(final JsonNode execNode)
    {
        if (execNode == null)
        {
            return "";
        }
        if (execNode.has("testMethod") && !execNode.path("testMethod").asText().trim().isEmpty())
        {
            return execNode.path("testMethod").asText().trim();
        }
        final String testClass = execNode.path("testClass").asText("").trim();
        if (execNode.has("junitTags") && execNode.path("junitTags").isArray() && execNode.path("junitTags").size() >= 2)
        {
            final String tag1 = execNode.path("junitTags").get(1).asText("").trim();
            if (!tag1.isEmpty() && !tag1.startsWith("Dataset:") && !tag1.startsWith("Location:") && !tag1.startsWith("Browser:") && !tag1.equalsIgnoreCase(testClass))
            {
                return tag1;
            }
        }
        if (execNode.has("testFile") && execNode.path("testFile").asText().contains("#"))
        {
            final String tf = execNode.path("testFile").asText();
            final String method = tf.substring(tf.indexOf('#') + 1).trim();
            if (!method.isEmpty() && !"executeTest".equalsIgnoreCase(method))
            {
                return method;
            }
        }
        return "";
    }

    public static String buildExecutionKey(final String testClass, final String testMethod, final String dataSet, final String browser)
    {
        final String c = testClass != null ? testClass.trim() : "";
        final String m = testMethod != null ? testMethod.trim() : "";
        final String d = dataSet != null ? dataSet.trim() : "";
        final String b = browser != null ? browser.trim() : "";
        if (!m.isEmpty())
        {
            return c + "#" + m + "#" + d + "#" + b;
        }
        return c + "#" + d + "#" + b;
    }

    public static String buildExecutionKey(final String testClass, final String dataSet, final String browser)
    {
        return buildExecutionKey(testClass, null, dataSet, browser);
    }

    static boolean hasBugsNode(final JsonNode bugsNode)
    {
        if (bugsNode == null || bugsNode.isNull() || bugsNode.isMissingNode())
        {
            return false;
        }
        if (bugsNode.isArray())
        {
            for (final JsonNode b : bugsNode)
            {
                if (!AuraReportDataService.extractTicket(b).isBlank())
                {
                    return true;
                }
            }
            return false;
        }
        return !AuraReportDataService.extractTicket(bugsNode).isBlank();
    }

    private int extractStepsTotal(final JsonNode node)
    {
        if (node.has("totalStepsCount") && node.path("totalStepsCount").isInt())
        {
            return node.path("totalStepsCount").asInt();
        }
        final JsonNode blocks = node.path("blocks");
        if (blocks.isObject())
        {
            int count = 0;
            final JsonNode b = blocks.path("before");
            if (b.isArray()) count += b.size();
            final JsonNode s = blocks.path("steps");
            if (s.isArray()) count += s.size();
            final JsonNode a = blocks.path("after");
            if (a.isArray()) count += a.size();
            if (count > 0) return count;
        }
        return 0;
    }

    public static int extractStepsFailed(final JsonNode node)
    {
        if (node.has("expectedFailure") && node.path("expectedFailure").asBoolean(false))
        {
            return 0;
        }
        if (node.has("failedStepsCount") && node.path("failedStepsCount").isInt())
        {
            return node.path("failedStepsCount").asInt();
        }
        final JsonNode blocks = node.path("blocks");
        if (blocks.isObject())
        {
            int failed = 0;
            for (final String key : List.of("before", "steps", "after"))
            {
                final JsonNode arr = blocks.path(key);
                if (arr.isArray())
                {
                    for (final JsonNode step : arr)
                    {
                        final String status = step.path("status").asText("");
                        final boolean passed = step.path("passed").asBoolean(true);
                        final boolean expectedFailure = step.path("expectedFailure").asBoolean(false);
                        if (!expectedFailure && ("failed".equalsIgnoreCase(status) || !passed))
                        {
                            failed++;
                        }
                    }
                }
            }
            return failed;
        }
        return 0;
    }

    private int extractStepsHealed(final JsonNode node)
    {
        if (node.has("healedStepsCount") && node.path("healedStepsCount").isInt())
        {
            return node.path("healedStepsCount").asInt();
        }
        int count = 0;
        final JsonNode blocks = node.path("blocks");
        if (blocks.isObject())
        {
            for (final String key : List.of("before", "steps", "after"))
            {
                final JsonNode arr = blocks.path(key);
                if (arr.isArray())
                {
                    for (final JsonNode step : arr)
                    {
                        final String st = step.path("status").asText("");
                        if ("healed".equalsIgnoreCase(st) || step.path("healed").asBoolean(false))
                        {
                            count++;
                        }
                    }
                }
            }
        }
        if (count > 0)
        {
            return count;
        }
        final String status = node.path("status").asText("");
        return (node.path("healed").asBoolean(false) || "healed".equalsIgnoreCase(status)) ? 1 : 0;
    }

    private int extractLlmCalls(final JsonNode node)
    {
        if (node.has("llmCallsCount") && node.path("llmCallsCount").isInt() && node.path("llmCallsCount").asInt() > 0)
        {
            return node.path("llmCallsCount").asInt();
        }
        final JsonNode metrics = node.path("metrics");
        if (metrics.has("totalLlmCalls") && metrics.path("totalLlmCalls").isInt() && metrics.path("totalLlmCalls").asInt() > 0)
        {
            return metrics.path("totalLlmCalls").asInt();
        }
        final JsonNode blocks = node.path("blocks");
        if (blocks.has("llmCalls") && blocks.path("llmCalls").isArray() && !blocks.path("llmCalls").isEmpty())
        {
            return blocks.path("llmCalls").size();
        }
        final JsonNode llmCalls = node.path("llmCalls");
        if (llmCalls.isArray())
        {
            return llmCalls.size();
        }
        return 0;
    }

    private long extractLlmTokens(final JsonNode node)
    {
        if (node.has("llmTotalTokens") && node.path("llmTotalTokens").isNumber() && node.path("llmTotalTokens").asLong() > 0L)
        {
            return node.path("llmTotalTokens").asLong();
        }
        final JsonNode metrics = node.path("metrics");
        if (metrics.has("totalTokens") && metrics.path("totalTokens").isNumber() && metrics.path("totalTokens").asLong() > 0L)
        {
            return metrics.path("totalTokens").asLong();
        }
        final long in = extractInputTokens(node);
        final long out = extractOutputTokens(node);
        if (in > 0L || out > 0L)
        {
            return in + out;
        }
        final JsonNode calls = node.has("llmCalls") && node.get("llmCalls").isArray()
            ? node.get("llmCalls")
            : (node.path("blocks").has("llmCalls") && node.path("blocks").get("llmCalls").isArray()
                ? node.path("blocks").get("llmCalls") : null);
        if (calls != null && calls.isArray() && !calls.isEmpty())
        {
            long sum = 0L;
            for (final JsonNode c : calls)
            {
                if (c.has("totalTokens") && c.path("totalTokens").isNumber())
                {
                    sum += c.path("totalTokens").asLong();
                }
                else
                {
                    sum += (c.path("inputTokens").asLong(0L) + c.path("outputTokens").asLong(0L));
                }
            }
            if (sum > 0L)
            {
                return sum;
            }
        }
        return 0L;
    }

    private long extractInputTokens(final JsonNode node)
    {
        if (node.has("inputTokensCount") && node.path("inputTokensCount").isNumber() && node.path("inputTokensCount").asLong() > 0L)
        {
            return node.path("inputTokensCount").asLong();
        }
        final JsonNode metrics = node.path("metrics");
        if (metrics.has("tokenUsageInput") && metrics.path("tokenUsageInput").isNumber() && metrics.path("tokenUsageInput").asLong() > 0L)
        {
            return metrics.path("tokenUsageInput").asLong();
        }
        if (metrics.has("inputTokens") && metrics.path("inputTokens").isNumber() && metrics.path("inputTokens").asLong() > 0L)
        {
            return metrics.path("inputTokens").asLong();
        }
        return 0L;
    }

    private long extractOutputTokens(final JsonNode node)
    {
        if (node.has("outputTokensCount") && node.path("outputTokensCount").isNumber() && node.path("outputTokensCount").asLong() > 0L)
        {
            return node.path("outputTokensCount").asLong();
        }
        final JsonNode metrics = node.path("metrics");
        if (metrics.has("tokenUsageOutput") && metrics.path("tokenUsageOutput").isNumber() && metrics.path("tokenUsageOutput").asLong() > 0L)
        {
            return metrics.path("tokenUsageOutput").asLong();
        }
        if (metrics.has("outputTokens") && metrics.path("outputTokens").isNumber() && metrics.path("outputTokens").asLong() > 0L)
        {
            return metrics.path("outputTokens").asLong();
        }
        return 0L;
    }

    private long extractCachedTokens(final JsonNode node)
    {
        if (node.has("cachedTokensCount") && node.path("cachedTokensCount").isNumber() && node.path("cachedTokensCount").asLong() > 0L)
        {
            return node.path("cachedTokensCount").asLong();
        }
        final JsonNode metrics = node.path("metrics");
        if (metrics.has("tokenUsageCached") && metrics.path("tokenUsageCached").isNumber() && metrics.path("tokenUsageCached").asLong() > 0L)
        {
            return metrics.path("tokenUsageCached").asLong();
        }
        if (metrics.has("cachedTokens") && metrics.path("cachedTokens").isNumber() && metrics.path("cachedTokens").asLong() > 0L)
        {
            return metrics.path("cachedTokens").asLong();
        }
        return 0L;
    }

    private double extractLlmCost(final JsonNode node)
    {
        if (node.has("llmCost") && node.path("llmCost").isNumber() && node.path("llmCost").asDouble() > 0.0)
        {
            return node.path("llmCost").asDouble();
        }
        final JsonNode metrics = node.path("metrics");
        if (metrics.has("estimatedCostUsd") && metrics.path("estimatedCostUsd").isNumber() && metrics.path("estimatedCostUsd").asDouble() > 0.0)
        {
            return metrics.path("estimatedCostUsd").asDouble();
        }
        return 0.0;
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

                final String stepReasoning = stepNode.path("reasoning").asText("");
                if (!stepReasoning.isBlank())
                {
                    stepObj.put("reasoning", stepReasoning);
                }
                else if (stepNode.has("reasonings") && stepNode.get("reasonings").isArray() && !stepNode.get("reasonings").isEmpty())
                {
                    final JsonNode lastReasoning = stepNode.get("reasonings").get(stepNode.get("reasonings").size() - 1);
                    if (lastReasoning != null && !lastReasoning.asText("").isBlank())
                    {
                        stepObj.put("reasoning", lastReasoning.asText(""));
                    }
                }
                if (stepNode.hasNonNull("reasonings") && stepNode.get("reasonings").isArray())
                {
                    stepObj.set("reasonings", stepNode.get("reasonings"));
                }

                final JsonNode sourceActions = stepNode.path("actions");
                final String stepStatusRaw = stepNode.path("status").asText("");
                boolean isHealed = "healed".equalsIgnoreCase(stepStatusRaw) || stepNode.path("healed").asBoolean(false);
                if (!isHealed && sourceActions.isArray())
                {
                    for (final JsonNode a : sourceActions)
                    {
                        if (a.has("healed") && a.get("healed").asBoolean(false))
                        {
                            isHealed = true;
                            break;
                        }
                    }
                }
                final boolean isPassed = stepNode.path("passed").asBoolean(true);
                final String stepStatus = isHealed ? "healed" : (isPassed ? "passed" : "failed");
                stepObj.put("status", stepStatus);
                if (isHealed)
                {
                    stepObj.put("healed", true);
                }
                stepObj.put("screenshot", stepNode.path("screenshot").asText(""));
                stepObj.put("error", stepNode.path("error").asText(""));

                final ArrayNode actionsArr = mapper.createArrayNode();
                if (sourceActions.isArray())
                {
                    for (final JsonNode actNode : sourceActions)
                    {
                        final ObjectNode actObj = mapper.createObjectNode();
                        final String actName = actNode.path("name").asText(actNode.path("type").asText("ACTION"));
                        actObj.put("type", actName);
                        actObj.put("name", actName);
                        actObj.put("target", actNode.path("target").asText(""));
                        if (actNode.has("resolvedTarget") && !actNode.path("resolvedTarget").asText().isBlank())
                        {
                            actObj.put("resolvedTarget", actNode.path("resolvedTarget").asText());
                        }
                        actObj.put("value", actNode.path("value").asText(""));
                        if (actNode.has("resolvedValue") && !actNode.path("resolvedValue").asText().isBlank())
                        {
                            actObj.put("resolvedValue", actNode.path("resolvedValue").asText());
                        }
                        actObj.put("description", actNode.path("description").asText("Executed " + actName + " action"));
                        final String reasoning = actNode.path("reasoning").asText("");
                        if (!reasoning.isEmpty())
                        {
                            actObj.put("reasoning", reasoning);
                        }
                        if (actNode.has("success") && actNode.get("success").isBoolean())
                        {
                            actObj.put("success", actNode.get("success").asBoolean());
                        }
                        if (actNode.has("healed") && actNode.get("healed").isBoolean())
                        {
                            actObj.put("healed", actNode.get("healed").asBoolean());
                        }
                        if (actNode.has("expectedTarget") && !actNode.path("expectedTarget").asText().isBlank())
                        {
                            actObj.put("expectedTarget", actNode.path("expectedTarget").asText());
                        }
                        if (actNode.has("originalTarget") && !actNode.path("originalTarget").asText().isBlank())
                        {
                            actObj.put("originalTarget", actNode.path("originalTarget").asText());
                        }
                        if (actNode.has("healedFrom") && !actNode.path("healedFrom").asText().isBlank())
                        {
                            actObj.put("healedFrom", actNode.path("healedFrom").asText());
                        }
                        actionsArr.add(actObj);
                    }
                }
                if (stepNode.has("llmCalls"))
                {
                    stepObj.set("llmCalls", stepNode.get("llmCalls"));
                }
                if (stepNode.has("subSteps"))
                {
                    stepObj.set("subSteps", stepNode.get("subSteps"));
                }
                stepObj.set("actions", actionsArr);
                targetArray.add(stepObj);
                idx++;
            }
        }
    }

    public void scanForTestExecJsonFiles(final File dir, final List<File> results)
    {
        final File[] files = dir.listFiles();
        if (files == null) return;

        for (final File f : files)
        {
            if (f.isDirectory())
            {
                scanForTestExecJsonFiles(f, results);
            }
            else if (f.getName().endsWith(".json") && !"run.json".equalsIgnoreCase(f.getName()) && !"batch.json".equalsIgnoreCase(f.getName()))
            {
                results.add(f);
            }
        }
    }

    private boolean isTestExecutionJson(final JsonNode node)
    {
        return node.isObject() && (node.has("status") || node.has("testClass") || node.has("id") || node.has("testName"));
    }

    /**
     * Checks whether the given directory contains at least one subdirectory. Used to distinguish
     * category folders (holding class subfolders) from plain class folders without a category,
     * which directly contain their execution JSON files.
     *
     * @param dir the directory to inspect, may be {@code null}
     * @return {@code true} if the directory exists and contains at least one subdirectory, otherwise {@code false}
     */
    private boolean hasSubdirectories(final Path dir)
    {
        if (dir == null || !Files.isDirectory(dir))
        {
            return false;
        }
        try (final var entries = Files.list(dir))
        {
            return entries.anyMatch(Files::isDirectory);
        }
        catch (final IOException e)
        {
            LOG.warn("Could not list directory {} to detect subfolders: {}", dir, e.getMessage());
            return false;
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

    public BatchInfo resolveOrCreateBatchJson(final File runDir, final List<File> testExecJsonFiles)
    {
        final File batchJsonFile = new File(runDir, "batch.json");
        String existingName = null;
        String existingDesc = null;
        String existingEnv = null;
        final Map<String, List<String>> existingTags = new LinkedHashMap<>();

        if (batchJsonFile.exists())
        {
            try
            {
                final JsonNode batchNode = objectMapper.readTree(batchJsonFile);
                existingName = batchNode.path("name").asText(batchNode.path("batchName").asText(""));
                existingDesc = batchNode.path("description").asText("");
                existingEnv = batchNode.path("environment").asText(batchNode.path("env").asText(""));
                if (batchNode.has("tags") && batchNode.path("tags").isObject())
                {
                    batchNode.path("tags").fields().forEachRemaining(entry -> {
                        final String k = entry.getKey().trim();
                        if (!k.isEmpty())
                        {
                            final List<String> list = existingTags.computeIfAbsent(k, key -> new ArrayList<>());
                            if (entry.getValue().isArray())
                            {
                                for (final JsonNode item : entry.getValue())
                                {
                                    final String s = item.asText("").trim();
                                    if (!s.isEmpty() && !list.contains(s))
                                    {
                                        list.add(s);
                                    }
                                }
                            }
                            else
                            {
                                final String s = entry.getValue().asText("").trim();
                                if (!s.isEmpty() && !list.contains(s))
                                {
                                    list.add(s);
                                }
                            }
                        }
                    });
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read existing batch.json in {}: {}", runDir.getAbsolutePath(), e.getMessage());
            }
        }

        String detectedEnv = null;
        String detectedBatchName = null;
        if (testExecJsonFiles != null)
        {
            for (final File f : testExecJsonFiles)
            {
                try
                {
                    final JsonNode node = objectMapper.readTree(f);
                    if (node.has("tags") && node.path("tags").isObject())
                    {
                        node.path("tags").fields().forEachRemaining(entry -> {
                            final String k = entry.getKey().trim();
                            if (!k.isEmpty())
                            {
                                final List<String> list = existingTags.computeIfAbsent(k, key -> new ArrayList<>());
                                if (entry.getValue().isArray())
                                {
                                    for (final JsonNode item : entry.getValue())
                                    {
                                        final String s = item.asText("").trim();
                                        if (!s.isEmpty() && !list.contains(s))
                                        {
                                            list.add(s);
                                        }
                                    }
                                }
                                else
                                {
                                    final String s = entry.getValue().asText("").trim();
                                    if (!s.isEmpty() && !list.contains(s))
                                    {
                                        list.add(s);
                                    }
                                }
                            }
                        });
                    }
                    if (node.has("variationTags") && node.path("variationTags").isObject())
                    {
                        node.path("variationTags").fields().forEachRemaining(entry -> {
                            final String k = entry.getKey().trim();
                            if (!k.isEmpty())
                            {
                                final List<String> list = existingTags.computeIfAbsent(k, key -> new ArrayList<>());
                                final String s = entry.getValue().asText("").trim();
                                if (!s.isEmpty() && !list.contains(s))
                                {
                                    list.add(s);
                                }
                            }
                        });
                    }
                    if (detectedEnv == null)
                    {
                        if (node.has("environment") && !node.path("environment").asText().isEmpty())
                        {
                            detectedEnv = node.path("environment").asText();
                        }
                        else if (node.has("env") && !node.path("env").asText().isEmpty())
                        {
                            detectedEnv = node.path("env").asText();
                        }
                        else if (node.path("dataBindings").has("environment") && !node.path("dataBindings").path("environment").asText().isEmpty())
                        {
                            detectedEnv = node.path("dataBindings").path("environment").asText();
                        }
                    }
                    if (detectedBatchName == null)
                    {
                        if (node.has("batchName") && AuraReportDataService.isRealBatch(node.path("batchName").asText()))
                        {
                            detectedBatchName = node.path("batchName").asText().trim();
                        }
                        else if (node.has("suite") && AuraReportDataService.isRealBatch(node.path("suite").asText()))
                        {
                            detectedBatchName = node.path("suite").asText().trim();
                        }
                    }
                }
                catch (final Exception ignored)
                {
                }
            }
        }

        if (detectedBatchName == null)
        {
            final String tagBatch = AuraReportDataService.resolveEffectiveBatchNameFromMap("", existingTags);
            if (AuraReportDataService.isRealBatch(tagBatch))
            {
                detectedBatchName = tagBatch;
            }
        }

        final String sysBatchName = System.getProperty("batchName", System.getProperty("neodymium.batchName", null));
        String finalName = (existingName != null && AuraReportDataService.isRealBatch(existingName)) ? existingName.trim() : null;
        if (finalName == null && sysBatchName != null && !sysBatchName.isBlank())
        {
            finalName = sysBatchName.trim();
        }
        if (finalName == null && detectedBatchName != null && !detectedBatchName.isBlank())
        {
            finalName = detectedBatchName.trim();
        }
        if (finalName == null)
        {
            finalName = (existingName != null && !existingName.isBlank()) ? existingName.trim() : "Default";
        }

        final String finalEnv = (existingEnv != null && !existingEnv.isBlank() && !existingEnv.equalsIgnoreCase("Default"))
            ? existingEnv.trim()
            : ((detectedEnv != null && !detectedEnv.trim().isEmpty()) ? detectedEnv.trim() : "Default");
        final String finalDesc = existingDesc != null ? existingDesc : "";

        if (!batchJsonFile.exists() || !existingTags.isEmpty() || (AuraReportDataService.isRealBatch(finalName) && !finalName.equals(existingName)))
        {
            try
            {
                final ObjectNode batchNode = objectMapper.createObjectNode();
                batchNode.put("name", finalName);
                batchNode.put("description", finalDesc);
                batchNode.put("environment", finalEnv);
                if (!existingTags.isEmpty())
                {
                    final ObjectNode tagsNode = objectMapper.createObjectNode();
                    existingTags.forEach((k, vList) -> {
                        final ArrayNode arr = objectMapper.createArrayNode();
                        vList.forEach(arr::add);
                        tagsNode.set(k, arr);
                    });
                    batchNode.set("tags", tagsNode);
                }
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(batchJsonFile, batchNode);
            }
            catch (final Exception e)
            {
                LOG.error("Failed to write batch.json in {}: {}", runDir.getAbsolutePath(), e.getMessage());
            }
        }

        return new BatchInfo(finalName, finalDesc, finalEnv);
    }

    private String buildFullRunJsonFromNestedDir(final String runId, final Path runJsonPath) throws IOException
    {
        final String content = Files.readString(runJsonPath, StandardCharsets.UTF_8);
        if (!content.contains("\"areas\"") && content.contains("\"executions\""))
        {
            return content;
        }

        final ObjectNode root = (ObjectNode) objectMapper.readTree(content);
        final ArrayNode mergedExecutions = objectMapper.createArrayNode();

        final Path runDir = runJsonPath.getParent();
        final JsonNode areasNode = root.path("areas");

        if (areasNode.isArray())
        {
            for (final JsonNode area : areasNode)
            {
                final String areaFolder = area.path("folder").asText(area.path("areaName").asText(""));
                final JsonNode testClassesNode = area.path("testClasses");

                if (testClassesNode.isArray())
                {
                    for (final JsonNode testClass : testClassesNode)
                    {
                        final String classFolder = testClass.path("folder").asText(testClass.path("className").asText(""));
                        final JsonNode executionsNode = testClass.path("executions");

                        if (executionsNode.isArray())
                        {
                            for (final JsonNode execFileNode : executionsNode)
                            {
                                final String execFileName = execFileNode.asText();
                                final Path execFilePath = resolveExecutionFilePath(runDir, areaFolder, classFolder, execFileName);

                                if (execFilePath != null && Files.exists(execFilePath))
                                {
                                    final JsonNode execNode = objectMapper.readTree(execFilePath.toFile());
                                    if (execNode instanceof ObjectNode execObj)
                                    {
                                        String areaToUse = (!areaFolder.isEmpty() && !"General".equalsIgnoreCase(areaFolder)) ? areaFolder : "";
                                        if (areaToUse.isEmpty() && execObj.hasNonNull("areaName") && !execObj.path("areaName").asText().isBlank())
                                        {
                                            final String existing = execObj.path("areaName").asText().trim();
                                            if (!"General".equalsIgnoreCase(existing) && !"Browsing (default)".equalsIgnoreCase(existing))
                                            {
                                                areaToUse = existing;
                                            }
                                        }
                                        if (areaToUse.isEmpty() || "General".equalsIgnoreCase(areaToUse) || "Browsing (default)".equalsIgnoreCase(areaToUse))
                                        {
                                            final String extractedFromNode = extractAreaFromNode(execObj);
                                            if (!extractedFromNode.isEmpty())
                                            {
                                                areaToUse = extractedFromNode;
                                            }
                                        }
                                        if (areaToUse.isEmpty() || !AuraReportDataService.isRealArea(areaToUse))
                                        {
                                            areaToUse = "Default";
                                        }
                                        execObj.put("areaName", areaToUse);

                                        String classToUse = classFolder;
                                        if (classToUse.isEmpty())
                                        {
                                            classToUse = extractClassNameFromExecObj(execObj);
                                        }
                                        execObj.put("testClass", classToUse);

                                        if (!execObj.has("id") || execObj.path("id").asText().isEmpty())
                                        {
                                            if (execObj.has("testId") && !execObj.path("testId").asText().isEmpty())
                                            {
                                                execObj.put("id", execObj.path("testId").asText());
                                            }
                                            else if (execObj.has("datasetId") && !execObj.path("datasetId").asText().isEmpty())
                                            {
                                                execObj.put("id", execObj.path("datasetId").asText());
                                            }
                                            else
                                            {
                                                execObj.put("id", execFileName.replaceAll("\\.json$", ""));
                                            }
                                        }

                                        if (!execObj.has("title") || execObj.path("title").asText().isEmpty())
                                        {
                                            if (execObj.has("datasetId") && !execObj.path("datasetId").asText().isEmpty())
                                            {
                                                execObj.put("title", execObj.path("datasetId").asText());
                                            }
                                            else if (execObj.has("testId") && !execObj.path("testId").asText().isEmpty())
                                            {
                                                execObj.put("title", execObj.path("testId").asText());
                                            }
                                            else if (execObj.has("testName") && !execObj.path("testName").asText().isEmpty()
                                                    && !execObj.path("testName").asText().trim().equalsIgnoreCase(classToUse))
                                            {
                                                execObj.put("title", execObj.path("testName").asText());
                                            }
                                            else
                                            {
                                                execObj.put("title", "Default");
                                            }
                                        }
                                    }
                                    mergedExecutions.add(execNode);
                                }
                            }
                        }
                    }
                }
            }
        }

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private Path resolveExecutionFilePath(final Path runDir, final String areaFolder, final String classFolder, final String execFileName)
    {
        Path path = runDir.resolve(areaFolder).resolve(classFolder).resolve(execFileName);
        if (Files.exists(path))
        {
            return path;
        }
        path = runDir.resolve(classFolder).resolve(execFileName);
        if (Files.exists(path))
        {
            return path;
        }
        path = runDir.resolve(execFileName);
        if (Files.exists(path))
        {
            return path;
        }
        try (var stream = Files.walk(runDir))
        {
            return stream.filter(Files::isRegularFile)
                         .filter(p -> p.getFileName().toString().equals(execFileName))
                         .findFirst()
                         .orElse(null);
        }
        catch (final Exception ignored)
        {
        }
        return null;
    }

    private String extractClassNameFromExecObj(final ObjectNode execObj)
    {
        if (execObj.has("testClass") && !execObj.path("testClass").asText().isEmpty())
        {
            return execObj.path("testClass").asText();
        }
        if (execObj.has("testFile") && !execObj.path("testFile").asText().isEmpty())
        {
            String tf = execObj.path("testFile").asText();
            if (tf.contains("#"))
            {
                tf = tf.substring(0, tf.indexOf('#'));
            }
            if (tf.contains("."))
            {
                tf = tf.substring(tf.lastIndexOf('.') + 1);
            }
            if (!tf.trim().isEmpty())
            {
                return tf.trim();
            }
        }
        if (execObj.has("junitTags") && execObj.path("junitTags").isArray() && execObj.path("junitTags").size() > 0)
        {
            final String firstTag = execObj.path("junitTags").get(0).asText();
            if (!firstTag.trim().isEmpty())
            {
                return firstTag.trim();
            }
        }
        return "DefaultClass";
    }

    public void deleteRunJson(final String runId)
    {
        final Path dirPath = getRunDir(runId);
        try
        {
            if (Files.exists(dirPath))
            {
                deleteDirectoryRecursively(dirPath);
                LOG.info("Deleted nested run directory from disk: {}", dirPath.toAbsolutePath());
            }

            final Path flatPath = Paths.get(baseDir, "run-" + runId + ".json");
            if (Files.exists(flatPath))
            {
                Files.delete(flatPath);
                LOG.info("Deleted flat run JSON from disk: {}", flatPath.toAbsolutePath());
            }
        }
        catch (final IOException e)
        {
            LOG.warn("Failed to delete run storage for run {}: {}", runId, e.getMessage());
        }
    }

    public boolean updateExecutionInRun(final String runId, final String rowId, final Consumer<ObjectNode> updater)
    {
        boolean updatedOnDisk = false;
        final Path runDir = getRunDir(runId);
        if (Files.exists(runDir))
        {
            try (final var stream = Files.walk(runDir))
            {
                final List<Path> jsonFiles = stream.filter(p -> p.toString().endsWith(".json") && !p.getFileName().toString().equals("run.json") && !p.getFileName().toString().equals("batch.json"))
                    .toList();
                for (final Path jsonPath : jsonFiles)
                {
                    final JsonNode node = objectMapper.readTree(jsonPath.toFile());
                    if (node instanceof ObjectNode objNode)
                    {
                        final String nodeId = objNode.has("id") ? objNode.path("id").asText() : "";
                        final String nodeTestId = objNode.has("testId") ? objNode.path("testId").asText() : "";
                        final String nodeDatasetId = objNode.has("datasetId") ? objNode.path("datasetId").asText() : "";
                        final String fileNameNoExt = jsonPath.getFileName().toString().replaceAll("\\.json$", "");

                        final String tClass = objNode.has("testClass") && !objNode.path("testClass").asText("").isBlank()
                            ? objNode.path("testClass").asText("").trim()
                            : (jsonPath.getParent() != null ? jsonPath.getParent().getFileName().toString() : "");
                        final String tMethod = extractTestMethod(objNode);
                        final String tTitle = objNode.has("title") && !objNode.path("title").asText("").isBlank()
                            ? objNode.path("title").asText("").trim()
                            : (!nodeDatasetId.isBlank() ? nodeDatasetId : (!nodeTestId.isBlank() ? nodeTestId : "Default"));
                        final String tBrowser = objNode.has("browser") && !objNode.path("browser").asText("").isBlank()
                            ? objNode.path("browser").asText("").trim()
                            : "Chrome";

                        final String execKey = buildExecutionKey(tClass, tMethod, tTitle, tBrowser);
                        final String execKeyNoMethod = buildExecutionKey(tClass, null, tTitle, tBrowser);
                        final String fullKey = tClass + "#" + nodeId + "#" + tBrowser;
                        final String fullKeyTestId = tClass + "#" + nodeTestId + "#" + tBrowser;
                        final String fullKeyDatasetId = tClass + "#" + nodeDatasetId + "#" + tBrowser;
                        final String classMethodBrowser = tClass + "#" + tMethod + "#" + tBrowser;

                        final boolean matches = (rowId != null && !rowId.isEmpty() && (
                            rowId.equalsIgnoreCase(nodeId)
                            || rowId.equalsIgnoreCase(nodeTestId)
                            || rowId.equalsIgnoreCase(nodeDatasetId)
                            || rowId.equalsIgnoreCase(fileNameNoExt)
                            || rowId.equalsIgnoreCase(execKey)
                            || rowId.equalsIgnoreCase(execKeyNoMethod)
                            || rowId.equalsIgnoreCase(fullKey)
                            || rowId.equalsIgnoreCase(fullKeyTestId)
                            || rowId.equalsIgnoreCase(fullKeyDatasetId)
                            || rowId.equalsIgnoreCase(classMethodBrowser)
                            || (rowId.contains("#") && rowId.contains(tClass) && (!tTitle.isEmpty() && rowId.contains(tTitle)) && rowId.contains(tBrowser))
                        )) || (jsonFiles.size() == 1);

                        if (matches)
                        {
                            final JsonNode beforeNode = objNode.deepCopy();
                            if (!objNode.has("id") || objNode.path("id").asText().isEmpty())
                            {
                                objNode.put("id", rowId != null && !rowId.isEmpty() ? rowId : fileNameNoExt);
                            }
                            updater.accept(objNode);

                            // Keep the execution JSON at its current location on disk (class folders
                            // without a category are only logically associated with the default
                            // category); write updates in place instead of relocating the file.
                            if (!beforeNode.equals(objNode))
                            {
                                objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), objNode);
                                LOG.info("Updated execution JSON on disk: {}", jsonPath.toAbsolutePath());
                            }
                            updatedOnDisk = true;
                            break;
                        }
                    }
                }
            }
            catch (final IOException e)
            {
                LOG.error("Failed updating execution {} in run dir {}: {}", rowId, runId, e.getMessage());
            }

            final Path nestedRunJson = runDir.resolve("run.json");
            final boolean runJsonUpdated = updateRunJsonFile(nestedRunJson, rowId, updater);
            if (runJsonUpdated)
            {
                updatedOnDisk = true;
            }
        }

        final Path flatRunJson = Paths.get(baseDir, "run-" + runId + ".json");
        final boolean flatUpdated = updateRunJsonFile(flatRunJson, rowId, updater);
        if (flatUpdated)
        {
            updatedOnDisk = true;
        }

        return updatedOnDisk;
    }

    private boolean updateRunJsonFile(final Path runJsonPath, final String rowId, final Consumer<ObjectNode> updater)
    {
        if (!Files.exists(runJsonPath) || Files.isDirectory(runJsonPath))
        {
            return false;
        }
        try
        {
            final JsonNode root = objectMapper.readTree(runJsonPath.toFile());
            if (root instanceof ObjectNode runObj)
            {
                boolean updated = false;
                final JsonNode execMetrics = runObj.path("executionMetrics");
                if (execMetrics instanceof ObjectNode metricsObj)
                {
                    final Iterator<Map.Entry<String, JsonNode>> fields = metricsObj.fields();
                    while (fields.hasNext())
                    {
                        final Map.Entry<String, JsonNode> entry = fields.next();
                        if (entry.getValue() instanceof ObjectNode metricObj)
                        {
                            final String mId = metricObj.has("id") ? metricObj.path("id").asText() : "";
                            final String tClass = metricObj.path("testClass").asText("");
                            final String tMethod = metricObj.path("testMethod").asText(extractTestMethod(metricObj));
                            final String tTitle = metricObj.path("title").asText("");
                            final String tBrowser = metricObj.path("browser").asText("Chrome");
                            final String execKey = buildExecutionKey(tClass, tMethod, tTitle, tBrowser);
                            final String execKeyNoMethod = buildExecutionKey(tClass, "", tTitle, tBrowser);
                            final String fullKey = tClass + "#" + mId + "#" + tBrowser;
                            final String classMethodBrowser = tClass + "#" + tMethod + "#" + tBrowser;

                            final boolean matches = (rowId != null && !rowId.isEmpty() && (
                                rowId.equalsIgnoreCase(entry.getKey())
                                || rowId.equalsIgnoreCase(mId)
                                || rowId.equalsIgnoreCase(execKey)
                                || rowId.equalsIgnoreCase(execKeyNoMethod)
                                || rowId.equalsIgnoreCase(fullKey)
                                || rowId.equalsIgnoreCase(classMethodBrowser)
                                || (rowId.contains("#") && rowId.contains(tClass) && (!tTitle.isEmpty() && rowId.contains(tTitle)) && rowId.contains(tBrowser))
                            ));

                            if (matches)
                            {
                                updater.accept(metricObj);
                                updated = true;
                            }
                        }
                    }
                }
                final JsonNode execArr = runObj.path("executions");
                if (execArr instanceof ArrayNode arr)
                {
                    for (final JsonNode item : arr)
                    {
                        if (item instanceof ObjectNode itemObj)
                        {
                            final String itemNodeId = itemObj.has("id") ? itemObj.path("id").asText() : "";
                            final String tClass = itemObj.path("testClass").asText("");
                            final String tMethod = itemObj.path("testMethod").asText(extractTestMethod(itemObj));
                            final String tTitle = itemObj.path("title").asText("");
                            final String tBrowser = itemObj.path("browser").asText("Chrome");
                            final String execKey = buildExecutionKey(tClass, tMethod, tTitle, tBrowser);
                            final String execKeyNoMethod = buildExecutionKey(tClass, "", tTitle, tBrowser);
                            final String fullKey = tClass + "#" + itemNodeId + "#" + tBrowser;
                            final String classMethodBrowser = tClass + "#" + tMethod + "#" + tBrowser;

                            final boolean matches = (rowId != null && !rowId.isEmpty() && (
                                rowId.equalsIgnoreCase(itemNodeId)
                                || rowId.equalsIgnoreCase(execKey)
                                || rowId.equalsIgnoreCase(execKeyNoMethod)
                                || rowId.equalsIgnoreCase(fullKey)
                                || rowId.equalsIgnoreCase(classMethodBrowser)
                                || (rowId.contains("#") && rowId.contains(tClass) && (!tTitle.isEmpty() && rowId.contains(tTitle)) && rowId.contains(tBrowser))
                            ));

                            if (matches)
                            {
                                updater.accept(itemObj);
                                updated = true;
                            }
                        }
                    }
                }
                if (updated)
                {
                    if (execMetrics instanceof ObjectNode metricsObj && !metricsObj.isEmpty())
                    {
                        int sPass = 0;
                        int sFixed = 0;
                        int sKnown = 0;
                        int sUnknown = 0;
                        int sIgnored = 0;
                        int sPassHealed = 0;
                        int sFixedHealed = 0;
                        int sKnownHealed = 0;
                        int sUnknownHealed = 0;
                        for (final JsonNode mNode : metricsObj)
                        {
                            final String mStatus = mNode.path("status").asText("failed-unknown");
                            final boolean mHasBugs = hasBugsNode(mNode.path("bugs"));
                            final boolean mHealed = mNode.path("healed").asBoolean(false) || mNode.path("healedStepsCount").asInt(0) > 0 || "healed".equalsIgnoreCase(mStatus);
                            if ("failed-known".equalsIgnoreCase(mStatus) || (("failed".equalsIgnoreCase(mStatus) || "error".equalsIgnoreCase(mStatus)) && mHasBugs))
                            {
                                sKnown++;
                                if (mHealed)
                                {
                                    sKnownHealed++;
                                }
                            }
                            else if ("failed-unknown".equalsIgnoreCase(mStatus) || "failed".equalsIgnoreCase(mStatus) || "error".equalsIgnoreCase(mStatus))
                            {
                                sUnknown++;
                                if (mHealed)
                                {
                                    sUnknownHealed++;
                                }
                            }
                            else if ("succeeded-fixed".equalsIgnoreCase(mStatus) || ("passed".equalsIgnoreCase(mStatus) && mHasBugs))
                            {
                                sFixed++;
                                if (mHealed)
                                {
                                    sFixedHealed++;
                                }
                            }
                            else if ("passed".equalsIgnoreCase(mStatus) || "passed-clean".equalsIgnoreCase(mStatus))
                            {
                                sPass++;
                                if (mHealed)
                                {
                                    sPassHealed++;
                                }
                            }
                            else if ("ignored".equalsIgnoreCase(mStatus) || "skipped".equalsIgnoreCase(mStatus))
                            {
                                sIgnored++;
                            }
                            else
                            {
                                if (mHasBugs)
                                {
                                    sKnown++;
                                    if (mHealed)
                                    {
                                        sKnownHealed++;
                                    }
                                }
                                else
                                {
                                    sUnknown++;
                                    if (mHealed)
                                    {
                                        sUnknownHealed++;
                                    }
                                }
                            }
                        }
                        final int sTotal = sPass + sFixed + sKnown + sUnknown + sIgnored;
                        final ObjectNode sObj = runObj.has("summary") && runObj.path("summary").isObject()
                            ? (ObjectNode) runObj.path("summary")
                            : objectMapper.createObjectNode();
                        sObj.put("total", sTotal);
                        sObj.put("pass", sPass);
                        sObj.put("fixed", sFixed);
                        sObj.put("known", sKnown);
                        sObj.put("unknown", sUnknown);
                        sObj.put("ignored", sIgnored);
                        sObj.put("passHealed", sPassHealed);
                        sObj.put("fixedHealed", sFixedHealed);
                        sObj.put("knownHealed", sKnownHealed);
                        sObj.put("unknownHealed", sUnknownHealed);
                        sObj.put("passRate", sTotal > 0 ? Math.round((double) (sPass + sFixed) / sTotal * 1000.0) / 10.0 : 0.0);
                        runObj.set("summary", sObj);
                    }

                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(runJsonPath.toFile(), runObj);
                    LOG.info("Updated run JSON on disk: {}", runJsonPath.toAbsolutePath());
                    return true;
                }
            }
        }
        catch (final IOException e)
        {
            LOG.error("Failed updating execution {} in run JSON {}: {}", rowId, runJsonPath, e.getMessage());
        }
        return false;
    }

    private void deleteDirectoryRecursively(final Path path) throws IOException
    {
        if (Files.isDirectory(path))
        {
            try (final var entries = Files.list(path))
            {
                for (final Path entry : entries.toList())
                {
                    deleteDirectoryRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }

    private void extractLocalesFromNode(final JsonNode node, final Set<String> localesSet)
    {
        final Set<String> nodeLocales = new LinkedHashSet<>();
        extractStringOrArrayField(node, "locale", nodeLocales);
        extractStringOrArrayField(node, "location", nodeLocales);
        if (node.has("tags") && node.path("tags").isObject())
        {
            extractStringOrArrayField(node.path("tags"), "locale", nodeLocales);
            extractStringOrArrayField(node.path("tags"), "location", nodeLocales);
        }
        if (node.has("localDataBindings") && node.path("localDataBindings").isObject())
        {
            extractStringOrArrayField(node.path("localDataBindings"), "locale", nodeLocales);
            extractStringOrArrayField(node.path("localDataBindings"), "location", nodeLocales);
        }
        if (node.has("dataBindings") && node.path("dataBindings").isObject())
        {
            extractStringOrArrayField(node.path("dataBindings"), "locale", nodeLocales);
            extractStringOrArrayField(node.path("dataBindings"), "location", nodeLocales);
        }
        if (nodeLocales.isEmpty())
        {
            localesSet.add("Default");
        }
        else
        {
            for (final String l : nodeLocales)
            {
                localesSet.add(AuraReportDataService.normalizeLocation(l));
            }
        }
    }

    private void extractBrowsersFromNode(final JsonNode node, final Set<String> browsersSet)
    {
        final Set<String> nodeBrowsers = new LinkedHashSet<>();
        extractStringOrArrayField(node, "browser", nodeBrowsers);
        if (node.has("localDataBindings") && node.path("localDataBindings").isObject())
        {
            extractStringOrArrayField(node.path("localDataBindings"), "browser", nodeBrowsers);
        }
        if (node.has("dataBindings") && node.path("dataBindings").isObject())
        {
            extractStringOrArrayField(node.path("dataBindings"), "browser", nodeBrowsers);
        }
        if (nodeBrowsers.isEmpty())
        {
            final String mId = node.path("id").asText("");
            final String extracted = AuraReportDataService.extractBrowserFromIdOrKey(mId);
            if (extracted != null && !extracted.isBlank())
            {
                nodeBrowsers.add(extracted);
            }
        }
        if (nodeBrowsers.isEmpty())
        {
            browsersSet.add("Chrome_1920x1080");
        }
        else
        {
            for (final String b : nodeBrowsers)
            {
                browsersSet.add(AuraReportDataService.normalizeBrowser(b));
            }
        }
    }

    private void extractStringOrArrayField(final JsonNode parent, final String fieldName, final Set<String> targetSet)
    {
        if (parent == null || !parent.has(fieldName))
        {
            return;
        }
        final JsonNode fieldNode = parent.get(fieldName);
        if (fieldNode.isArray())
        {
            for (final JsonNode item : fieldNode)
            {
                final String val = item.asText("").trim();
                if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val))
                {
                    targetSet.add(val);
                }
            }
        }
        else
        {
            final String val = fieldNode.asText("").trim();
            if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val))
            {
                targetSet.add(val);
            }
        }
    }

    private static final List<String> TIMESTAMP_CANDIDATES = List.of(
        "startTime", "startTimeMs", "timestamp", "startDate", "time", "createdAt"
    );

    private record ExecutionTime(long ms, String timeStr) {}

    private ExecutionTime extractExecutionStartTime(final JsonNode node)
    {
        String rawVal = null;
        for (final String key : TIMESTAMP_CANDIDATES)
        {
            if (node.hasNonNull(key))
            {
                final String text = node.get(key).asText("").trim();
                if (!text.isEmpty())
                {
                    rawVal = text;
                    break;
                }
            }
        }

        if (rawVal == null)
        {
            if (node.has("localDataBindings") && node.path("localDataBindings").isObject())
            {
                for (final String key : TIMESTAMP_CANDIDATES)
                {
                    if (node.path("localDataBindings").hasNonNull(key))
                    {
                        final String text = node.path("localDataBindings").get(key).asText("").trim();
                        if (!text.isEmpty())
                        {
                            rawVal = text;
                            break;
                        }
                    }
                }
            }
            if (rawVal == null && node.has("dataBindings") && node.path("dataBindings").isObject())
            {
                for (final String key : TIMESTAMP_CANDIDATES)
                {
                    if (node.path("dataBindings").hasNonNull(key))
                    {
                        final String text = node.path("dataBindings").get(key).asText("").trim();
                        if (!text.isEmpty())
                        {
                            rawVal = text;
                            break;
                        }
                    }
                }
            }
        }

        if (rawVal == null)
        {
            return null;
        }

        try
        {
            double num = Double.parseDouble(rawVal);
            if (num < 1e11)
            {
                num *= 1000.0;
            }
            final long ms = (long) num;
            final LocalDateTime ldt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(ms),
                ZoneId.systemDefault()
            );
            final String formatted = ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return new ExecutionTime(ms, formatted);
        }
        catch (final NumberFormatException ignored)
        {
        }

        try
        {
            final Instant instant = Instant.parse(rawVal);
            return new ExecutionTime(instant.toEpochMilli(), rawVal);
        }
        catch (final Exception ignored)
        {
        }

        for (final String pattern : List.of("yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyyMMdd_HHmmss"))
        {
            try
            {
                final LocalDateTime ldt = LocalDateTime.parse(rawVal, DateTimeFormatter.ofPattern(pattern));
                final long ms = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
                return new ExecutionTime(ms, rawVal);
            }
            catch (final Exception ignored)
            {
            }
        }

        return new ExecutionTime(Long.MAX_VALUE, rawVal);
    }

    private static String extractAreaFromNode(final JsonNode node)
    {
        if (node == null || !node.isObject())
        {
            return "";
        }
        if (node.has("tags") && node.path("tags").isObject())
        {
            final String area = extractAreaFromTagsObject(node.path("tags"));
            if (!area.isEmpty())
            {
                return area;
            }
        }
        if (node.has("localDataBindings") && node.path("localDataBindings").isObject())
        {
            final String area = extractAreaFromTagsObject(node.path("localDataBindings"));
            if (!area.isEmpty())
            {
                return area;
            }
        }
        if (node.has("dataBindings") && node.path("dataBindings").isObject())
        {
            final String area = extractAreaFromTagsObject(node.path("dataBindings"));
            if (!area.isEmpty())
            {
                return area;
            }
        }
        return "";
    }

    private static String extractAreaFromTagsObject(final JsonNode tagsNode)
    {
        if (tagsNode == null || !tagsNode.isObject())
        {
            return "";
        }
        final Iterator<String> fieldNames = tagsNode.fieldNames();
        while (fieldNames.hasNext())
        {
            final String field = fieldNames.next();
            if ("area".equalsIgnoreCase(field) || "tags.area".equalsIgnoreCase(field))
            {
                final JsonNode areaNode = tagsNode.path(field);
                if (areaNode.isArray() && areaNode.size() > 0)
                {
                    final String first = areaNode.get(0).asText("");
                    if (!first.isBlank())
                    {
                        return stripLeadingAt(first);
                    }
                }
                else if (!areaNode.isMissingNode() && !areaNode.isNull())
                {
                    String text = areaNode.asText("").trim();
                    if (text.startsWith("[") && text.endsWith("]"))
                    {
                        text = text.substring(1, text.length() - 1).trim();
                        if (text.contains(","))
                        {
                            text = text.substring(0, text.indexOf(',')).trim();
                        }
                    }
                    if (!text.isBlank())
                    {
                        return stripLeadingAt(text);
                    }
                }
            }
        }
        return "";
    }

    private static String extractSubAreaFromNode(final JsonNode node)
    {
        if (node == null || !node.isObject())
        {
            return "";
        }
        if (node.has("subArea") && !node.path("subArea").asText("").trim().isEmpty())
        {
            return stripLeadingAt(node.path("subArea").asText(""));
        }
        if (node.has("tags") && node.path("tags").isObject())
        {
            final String sub = extractSubAreaFromTagsObject(node.path("tags"));
            if (!sub.isEmpty())
            {
                return sub;
            }
        }
        if (node.has("localDataBindings") && node.path("localDataBindings").isObject())
        {
            if (node.path("localDataBindings").has("subArea") && !node.path("localDataBindings").path("subArea").asText("").trim().isEmpty())
            {
                return stripLeadingAt(node.path("localDataBindings").path("subArea").asText(""));
            }
            final String sub = extractSubAreaFromTagsObject(node.path("localDataBindings"));
            if (!sub.isEmpty())
            {
                return sub;
            }
        }
        if (node.has("dataBindings") && node.path("dataBindings").isObject())
        {
            if (node.path("dataBindings").has("subArea") && !node.path("dataBindings").path("subArea").asText("").trim().isEmpty())
            {
                return stripLeadingAt(node.path("dataBindings").path("subArea").asText(""));
            }
            final String sub = extractSubAreaFromTagsObject(node.path("dataBindings"));
            if (!sub.isEmpty())
            {
                return sub;
            }
        }
        return "";
    }

    private static String extractSubAreaFromTagsObject(final JsonNode tagsNode)
    {
        if (tagsNode == null || !tagsNode.isObject())
        {
            return "";
        }
        final Iterator<String> fieldNames = tagsNode.fieldNames();
        while (fieldNames.hasNext())
        {
            final String field = fieldNames.next();
            if ("area".equalsIgnoreCase(field) || "tags.area".equalsIgnoreCase(field))
            {
                final JsonNode areaNode = tagsNode.path(field);
                if (areaNode.isArray() && areaNode.size() > 1)
                {
                    final String second = areaNode.get(1).asText("");
                    if (!second.isBlank())
                    {
                        return stripLeadingAt(second);
                    }
                }
                else if (!areaNode.isMissingNode() && !areaNode.isNull())
                {
                    String text = areaNode.asText("").trim();
                    if (text.startsWith("[") && text.endsWith("]"))
                    {
                        text = text.substring(1, text.length() - 1).trim();
                        if (text.contains(","))
                        {
                            final String[] parts = text.split(",", 2);
                            if (parts.length > 1 && !parts[1].trim().isBlank())
                            {
                                return stripLeadingAt(parts[1]);
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    private static String stripLeadingAt(final String raw)
    {
        if (raw == null)
        {
            return "";
        }
        String s = raw.trim();
        while (s.startsWith("@"))
        {
            s = s.substring(1).trim();
        }
        return s;
    }
}
