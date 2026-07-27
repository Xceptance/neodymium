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
package com.xceptance.neodymium.aura;

import com.xceptance.neodymium.aura.dto.DatasetDto;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Service handling test YAML file management, scanning, file CRUD operations,
 * directory expansions, and metadata extraction.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraFileService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraFileService.class);

    private final Set<String> expandedFiles = ConcurrentHashMap.newKeySet();
    private volatile String activeEditingFile = null;

    public AuraFileService()
    {
    }

    public String getActiveEditingFile()
    {
        return activeEditingFile;
    }

    public void setActiveEditingFile(final String file)
    {
        this.activeEditingFile = file;
    }

    public Set<String> getExpandedFiles()
    {
        return expandedFiles;
    }

    public boolean isExpanded(final String file)
    {
        return expandedFiles.contains(file);
    }

    public void toggleFileExpansion(final String file)
    {
        if (expandedFiles.contains(file))
        {
            expandedFiles.remove(file);
        }
        else
        {
            expandedFiles.add(file);
        }
    }

    public List<YamlFileDto> getYamlFilesList()
    {
        final List<String> yamlFiles = new ArrayList<>();
        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        if (resourcesDir.exists() && resourcesDir.isDirectory())
        {
            scanDirStatic(resourcesDir, resourcesDir, yamlFiles);
            yamlFiles.sort(String::compareTo);
        }
        final List<YamlFileDto> responseList = new ArrayList<>();
        for (final String file : yamlFiles)
        {
            final File yamlFile = new File(resourcesDir, file);
            final Map<String, Object> details = getFileDetails(yamlFile);
            @SuppressWarnings("unchecked")
            final List<DatasetDto> datasets = (List<DatasetDto>) details.get("datasets");
            responseList.add(new YamlFileDto(file, datasets != null ? datasets : new ArrayList<>()));
        }
        return responseList;
    }

    public List<YamlFileDto> getFilteredYamlFilesList(final String query)
    {
        final List<YamlFileDto> fullList = getYamlFilesList();
        if (query == null || query.trim().isEmpty())
        {
            return fullList;
        }
        final String lowerQuery = query.toLowerCase().trim();
        final List<YamlFileDto> filtered = new ArrayList<>();
        for (final YamlFileDto file : fullList)
        {
            boolean match = file.file != null && file.file.toLowerCase().contains(lowerQuery);
            if (!match && file.datasets != null)
            {
                for (final DatasetDto dataset : file.datasets)
                {
                    if ((dataset.label != null && dataset.label.toLowerCase().contains(lowerQuery))
                            || (dataset.id != null && dataset.id.toLowerCase().contains(lowerQuery)))
                    {
                        match = true;
                        break;
                    }
                }
            }
            if (match)
            {
                filtered.add(file);
            }
        }
        return filtered;
    }

    public void scanDirStatic(final File baseDir, final File currentDir, final List<String> yamlFiles)
    {
        final File[] files = currentDir.listFiles();
        if (files != null)
        {
            for (final File file : files)
            {
                if (file.isDirectory())
                {
                    scanDirStatic(baseDir, file, yamlFiles);
                }
                else
                {
                    final String name = file.getName().toLowerCase();
                    if (name.endsWith(".yaml") || name.endsWith(".yml"))
                    {
                        final String relativePath = baseDir.toURI().relativize(file.toURI()).getPath();
                        yamlFiles.add(relativePath);
                    }
                }
            }
        }
    }

    public Map<String, Object> getFileDetails(final File file)
    {
        final Map<String, Object> details = new HashMap<>();
        details.put("path", file.getName());
        try
        {
            final Yaml yaml = new Yaml();
            final Object data;
            try (final InputStream is = new FileInputStream(file))
            {
                data = yaml.load(is);
            }
            if (data instanceof Map)
            {
                @SuppressWarnings("unchecked")
                final Map<String, Object> root = (Map<String, Object>) data;

                // Extract steps info
                if (root.containsKey("steps"))
                {
                    final String stepsStr = String.valueOf(root.get("steps"));
                    final String[] steps = stepsStr.split("\n");
                    final List<String> cleanSteps = new ArrayList<>();
                    for (final String step : steps)
                    {
                        if (!step.trim().isEmpty())
                        {
                            cleanSteps.add(step.trim());
                        }
                    }
                    details.put("totalSteps", cleanSteps.size());
                    final List<String> summary = cleanSteps.subList(0, Math.min(cleanSteps.size(), 3));
                    details.put("stepsSummary", summary);
                }
                else
                {
                    details.put("totalSteps", 0);
                    details.put("stepsSummary", new ArrayList<>());
                }

                // Extract data / test IDs / dataset keys
                final List<DatasetDto> datasetsList = new ArrayList<>();
                final List<String> datasetKeys = new ArrayList<>();
                if (root.containsKey("data"))
                {
                    final Object dataObj = root.get("data");
                    if (dataObj instanceof List)
                    {
                        final List<?> dataList = (List<?>) dataObj;
                        for (int i = 0; i < dataList.size(); i++)
                        {
                            final Object item = dataList.get(i);
                            if (item instanceof Map)
                            {
                                final Map<?, ?> itemMap = (Map<?, ?>) item;
                                final Object tId = itemMap.get("testId") != null ? itemMap.get("testId")
                                        : itemMap.get("TEST_ID");
                                if (tId != null && !String.valueOf(tId).trim().isEmpty())
                                {
                                    datasetsList.add(new DatasetDto(String.valueOf(tId), String.valueOf(tId), true));
                                }
                                else
                                {
                                    datasetsList.add(new DatasetDto(String.valueOf(i + 1), "Dataset " + (i + 1), false));
                                }
                                for (final Object key : itemMap.keySet())
                                {
                                    final String keyStr = String.valueOf(key);
                                    if (!datasetKeys.contains(keyStr) && !keyStr.startsWith("neodymium."))
                                    {
                                        datasetKeys.add(keyStr);
                                    }
                                }
                            }
                        }
                    }
                }
                else
                {
                    final Object tId = root.get("testId") != null ? root.get("testId") : root.get("TEST_ID");
                    if (tId != null && !String.valueOf(tId).trim().isEmpty())
                    {
                        datasetsList.add(new DatasetDto(String.valueOf(tId), String.valueOf(tId), true));
                    }
                    else
                    {
                        datasetsList.add(new DatasetDto("1", "Dataset 1", false));
                    }
                    for (final Object key : root.keySet())
                    {
                        final String keyStr = String.valueOf(key);
                        if (!"steps".equals(keyStr) && !"data".equals(keyStr) && !keyStr.startsWith("neodymium."))
                        {
                            datasetKeys.add(keyStr);
                        }
                    }
                }
                details.put("datasets", datasetsList);
                final List<String> testIds = new ArrayList<>();
                for (final DatasetDto d : datasetsList)
                {
                    testIds.add(d.label);
                }
                details.put("testIds", testIds);
                details.put("datasetKeys", datasetKeys);
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to parse YAML file details for: " + file.getAbsolutePath(), e);
            details.put("error", e.getMessage());
        }
        return details;
    }

    public File resolveCanonicalFile(final String file) throws IOException
    {
        final File resourcesDir = new File("src/test/resources").getCanonicalFile();
        final File yamlFile = new File(resourcesDir, file).getCanonicalFile();
        if (!yamlFile.getPath().startsWith(resourcesDir.getPath()))
        {
            throw new SecurityException("Access denied: Directory traversal detected");
        }
        return yamlFile;
    }

    public String readYamlFileContent(final String file) throws IOException
    {
        final File yamlFile = resolveCanonicalFile(file);
        if (yamlFile.exists() && yamlFile.isFile())
        {
            return Files.readString(yamlFile.toPath(), StandardCharsets.UTF_8);
        }
        return "";
    }

    public void saveYamlFileContent(final String file, final String content) throws IOException
    {
        final File yamlFile = resolveCanonicalFile(file);
        Files.writeString(yamlFile.toPath(), content, StandardCharsets.UTF_8);
    }

    public void createYamlFile(final String name) throws IOException
    {
        final String sanitizedName = name.endsWith(".yaml") ? name : name + ".yaml";
        final File yamlFile = resolveCanonicalFile(sanitizedName);
        if (!yamlFile.exists())
        {
            Files.writeString(yamlFile.toPath(), "steps: |\n  # Add your steps here\n", StandardCharsets.UTF_8);
        }
    }

    public void deleteYamlFile(final String file) throws IOException
    {
        final File yamlFile = resolveCanonicalFile(file);
        if (yamlFile.exists() && yamlFile.isFile())
        {
            Files.delete(yamlFile.toPath());
        }
    }
}
