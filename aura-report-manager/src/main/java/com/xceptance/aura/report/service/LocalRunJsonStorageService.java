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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        Files.createDirectories(path.getParent());
        Files.writeString(path, jsonContent, StandardCharsets.UTF_8);
        LOG.info("Saved run JSON to disk: {}", path.toAbsolutePath());
    }

    public Optional<String> readRunJson(final String runId)
    {
        final Path nestedRunJson = Paths.get(baseDir, runId, "run.json");
        final Path runDir = Paths.get(baseDir, runId);

        if (!Files.exists(nestedRunJson) && Files.exists(runDir) && Files.isDirectory(runDir))
        {
            generateRunJsonFromTestExecutions(runDir.toFile(), runId);
        }

        if (Files.exists(nestedRunJson))
        {
            try
            {
                return Optional.of(buildFullRunJsonFromNestedDir(runId, nestedRunJson));
            }
            catch (final Exception e)
            {
                LOG.error("Failed to assemble nested run JSON for run {}: {}", runId, e.getMessage());
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

        return Optional.empty();
    }

    public void generateRunJsonFromTestExecutions(final File runDir, final String runId)
    {
        try
        {
            final List<File> testExecJsonFiles = new ArrayList<>();
            scanForTestExecJsonFiles(runDir, testExecJsonFiles);

            if (testExecJsonFiles.isEmpty())
            {
                return;
            }

            final Map<String, Map<String, List<String>>> areasMap = new LinkedHashMap<>();
            int totalExecsCount = 0;
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
                            String folderArea = "";
                            String folderClass = "";
                            if (relPath.getNameCount() >= 3)
                            {
                                folderArea = relPath.getName(0).toString();
                                folderClass = relPath.getName(1).toString();
                            }
                            else if (relPath.getNameCount() == 2)
                            {
                                folderArea = "";
                                folderClass = relPath.getName(0).toString();
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
                            if (rawArea.isEmpty() || "General".equalsIgnoreCase(rawArea))
                            {
                                if (!folderArea.isEmpty() && !"General".equalsIgnoreCase(folderArea))
                                {
                                    rawArea = folderArea;
                                }
                                else
                                {
                                    rawArea = "Browsing (default)";
                                }
                            }
                            final String areaName = rawArea.trim();

                            objNode.put("areaName", areaName);
                            objNode.put("testClass", testClass);

                            final String title = objNode.path("title").asText("");
                            final String location = objNode.path("location").asText("US");
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

                            // Relocate file on disk if not currently in runDir / areaName / testClass / f.getName()
                            final Path targetDir = runDir.toPath().resolve(areaName).resolve(testClass);
                            final Path targetFilePath = targetDir.resolve(f.getName());
                            File targetFile = f;

                            if (!f.toPath().toAbsolutePath().equals(targetFilePath.toAbsolutePath()))
                            {
                                Files.createDirectories(targetDir);
                                final Path oldParent = f.toPath().getParent();
                                targetFile = targetFilePath.toFile();
                                objectMapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, objNode);
                                try
                                {
                                    Files.deleteIfExists(f.toPath());
                                    if (oldParent != null && Files.exists(oldParent) && !oldParent.equals(runDir.toPath()))
                                    {
                                        try (final var entries = Files.list(oldParent))
                                        {
                                            if (entries.findFirst().isEmpty())
                                            {
                                                Files.deleteIfExists(oldParent);
                                                final Path oldGrandParent = oldParent.getParent();
                                                if (oldGrandParent != null && Files.exists(oldGrandParent) && !oldGrandParent.equals(runDir.toPath()))
                                                {
                                                    try (final var grandEntries = Files.list(oldGrandParent))
                                                    {
                                                        if (grandEntries.findFirst().isEmpty())
                                                        {
                                                            Files.deleteIfExists(oldGrandParent);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                catch (final Exception ignored)
                                {
                                }
                            }
                            else
                            {
                                try
                                {
                                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, objNode);
                                }
                                catch (final Exception ignored)
                                {
                                }
                            }
                            areasMap.computeIfAbsent(areaName, k -> new LinkedHashMap<>()).computeIfAbsent(testClass, k -> new ArrayList<>()).add(targetFile.getName());
                        }

                        totalExecsCount++;
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

            final int total = totalExecsCount;
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

            rootNode.set("summary", summaryNode);
            rootNode.set("areas", areasArray);

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

    private String buildFullRunJsonFromNestedDir(final String runId, final Path runJsonPath) throws IOException
    {
        final ObjectNode root = (ObjectNode) objectMapper.readTree(runJsonPath.toFile());
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
                                        final String areaToUse = (!areaFolder.isEmpty() && !"General".equalsIgnoreCase(areaFolder)) ? areaFolder : "Browsing (default)";
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
                                            else if (execObj.has("testName") && !execObj.path("testName").asText().isEmpty())
                                            {
                                                execObj.put("title", execObj.path("testName").asText());
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

        if (!root.has("executions") || root.get("executions") == null || root.get("executions").isNull() || root.get("executions").isEmpty())
        {
            root.set("executions", mergedExecutions);
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

    public boolean updateExecutionInRun(final String runId, final String rowId, final java.util.function.Consumer<ObjectNode> updater)
    {
        final Path runDir = getRunDir(runId);
        if (Files.exists(runDir))
        {
            try (final var stream = Files.walk(runDir))
            {
                final java.util.List<Path> jsonFiles = stream.filter(p -> p.toString().endsWith(".json") && !p.getFileName().toString().equals("run.json") && !p.getFileName().toString().equals("batch.json"))
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

                        final boolean matches = (rowId != null && !rowId.isEmpty() && (
                            rowId.equalsIgnoreCase(nodeId)
                            || rowId.equalsIgnoreCase(nodeTestId)
                            || rowId.equalsIgnoreCase(nodeDatasetId)
                            || rowId.equalsIgnoreCase(fileNameNoExt)
                        )) || (jsonFiles.size() == 1);

                        if (matches)
                        {
                            if (!objNode.has("id") || objNode.path("id").asText().isEmpty())
                            {
                                objNode.put("id", rowId != null && !rowId.isEmpty() ? rowId : fileNameNoExt);
                            }
                            updater.accept(objNode);

                            final String areaName = objNode.has("areaName") && !objNode.path("areaName").asText().trim().isEmpty() ? objNode.path("areaName").asText().trim() : "Browsing (default)";
                            final String testClass = objNode.has("testClass") && !objNode.path("testClass").asText().trim().isEmpty() ? objNode.path("testClass").asText().trim() : "DefaultClass";
                            final Path targetDir = runDir.resolve(areaName).resolve(testClass);
                            final Path targetFilePath = targetDir.resolve(jsonPath.getFileName().toString());

                            if (!jsonPath.toAbsolutePath().equals(targetFilePath.toAbsolutePath()))
                            {
                                Files.createDirectories(targetDir);
                                final Path oldParent = jsonPath.getParent();
                                objectMapper.writerWithDefaultPrettyPrinter().writeValue(targetFilePath.toFile(), objNode);
                                Files.deleteIfExists(jsonPath);
                                cleanEmptyParentDirectories(oldParent, runDir);
                                LOG.info("Updated and relocated execution JSON on disk: {} -> {}", jsonPath.toAbsolutePath(), targetFilePath.toAbsolutePath());
                            }
                            else
                            {
                                objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), objNode);
                                LOG.info("Updated execution JSON on disk: {}", jsonPath.toAbsolutePath());
                            }
                            return true;
                        }
                    }
                }
            }
            catch (final IOException e)
            {
                LOG.error("Failed updating execution {} in run dir {}: {}", rowId, runId, e.getMessage());
            }
        }

        final Path flatRunJson = Paths.get(baseDir, "run-" + runId + ".json");
        if (Files.exists(flatRunJson))
        {
            try
            {
                final ObjectNode root = (ObjectNode) objectMapper.readTree(flatRunJson.toFile());
                final JsonNode execArray = root.path("executions");
                if (execArray.isArray())
                {
                    for (final JsonNode execNode : execArray)
                    {
                        if (execNode instanceof ObjectNode execObj && rowId.equalsIgnoreCase(execObj.path("id").asText()))
                        {
                            updater.accept(execObj);
                            objectMapper.writerWithDefaultPrettyPrinter().writeValue(flatRunJson.toFile(), root);
                            LOG.info("Updated flat run JSON on disk: {}", flatRunJson.toAbsolutePath());
                            return true;
                        }
                    }
                }
            }
            catch (final IOException e)
            {
                LOG.error("Failed updating execution {} in flat run JSON {}: {}", rowId, runId, e.getMessage());
            }
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

    private void cleanEmptyParentDirectories(final Path dir, final Path stopDir)
    {
        Path current = dir;
        while (current != null && Files.exists(current) && !current.equals(stopDir))
        {
            try (final var entries = Files.list(current))
            {
                if (entries.findFirst().isEmpty())
                {
                    Files.deleteIfExists(current);
                    current = current.getParent();
                }
                else
                {
                    break;
                }
            }
            catch (final Exception e)
            {
                break;
            }
        }
    }
}
