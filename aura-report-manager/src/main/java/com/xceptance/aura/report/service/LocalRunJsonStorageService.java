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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service managing local disk storage for structured run report JSON files (storage/runs/{runId}/run.json and storage/runs/run-{runId}.json).
 * Compatible with Neodymium Aura runs directory organization.
 *
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
                                final Path execFilePath = runDir.resolve(areaFolder).resolve(classFolder).resolve(execFileName);

                                if (Files.exists(execFilePath))
                                {
                                    final JsonNode execNode = objectMapper.readTree(execFilePath.toFile());
                                    if (execNode instanceof ObjectNode execObj)
                                    {
                                        if (!areaFolder.isEmpty())
                                        {
                                            execObj.put("areaName", areaFolder);
                                        }
                                        if (!classFolder.isEmpty() && (!execObj.has("testClass") || execObj.path("testClass").asText().isEmpty()))
                                        {
                                            execObj.put("testClass", classFolder);
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

        if (!root.has("executions") || root.get("executions").isEmpty())
        {
            root.set("executions", mergedExecutions);
        }

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
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
                    if (node instanceof ObjectNode objNode && rowId.equalsIgnoreCase(objNode.path("id").asText()))
                    {
                        updater.accept(objNode);
                        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), objNode);
                        LOG.info("Updated execution JSON on disk: {}", jsonPath.toAbsolutePath());
                        return true;
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
}
