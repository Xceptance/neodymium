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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final File customResourcesDir;
    private volatile String activeEditingFile = null;

    public AuraFileService()
    {
        this(null);
    }

    public AuraFileService(final File customResourcesDir)
    {
        this.customResourcesDir = customResourcesDir;
    }

    /**
     * Resolves the active test resources base directory.
     *
     * @return the resolved test resources directory
     */
    public File getResourcesDirectory()
    {
        if (customResourcesDir != null && customResourcesDir.exists() && customResourcesDir.isDirectory())
        {
            return customResourcesDir.getAbsoluteFile();
        }
        final File defaultDir = new File("src/test/resources").getAbsoluteFile();
        if (defaultDir.exists() && defaultDir.isDirectory())
        {
            return defaultDir;
        }
        final File coreDir = new File("neodymium-core/src/test/resources").getAbsoluteFile();
        if (coreDir.exists() && coreDir.isDirectory())
        {
            return coreDir;
        }
        final File managerDir = new File("aura-manager/src/test/resources").getAbsoluteFile();
        if (managerDir.exists() && managerDir.isDirectory())
        {
            return managerDir;
        }
        return defaultDir;
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
        final File resourcesDir = getResourcesDirectory();
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
            final String error = (String) details.get("error");
            final boolean hasError = Boolean.TRUE.equals(details.get("hasError")) || (error != null && !error.isBlank());
            responseList.add(new YamlFileDto(file, datasets != null ? datasets : new ArrayList<>(), hasError, error));
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

    public List<String> getStepsFilesList()
    {
        final List<String> stepsFiles = new ArrayList<>();
        final File resourcesDir = getResourcesDirectory();
        if (resourcesDir.exists() && resourcesDir.isDirectory())
        {
            scanDirForStepsStatic(resourcesDir, resourcesDir, stepsFiles);
            stepsFiles.sort(String::compareTo);
        }
        return stepsFiles;
    }

    public List<String> getFilteredStepsFilesList(final String query)
    {
        final List<String> fullList = getStepsFilesList();
        if (query == null || query.trim().isEmpty())
        {
            return fullList;
        }
        final String lowerQuery = query.toLowerCase().trim();
        final List<String> filtered = new ArrayList<>();
        for (final String file : fullList)
        {
            boolean match = file != null && file.toLowerCase().contains(lowerQuery);
            if (!match && file != null)
            {
                try
                {
                    final String content = readYamlFileContent(file);
                    if (content != null && content.toLowerCase().contains(lowerQuery))
                    {
                        match = true;
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.debug("Could not read steps file content for filter matching: {}", file, e);
                }
            }
            if (match)
            {
                filtered.add(file);
            }
        }
        return filtered;
    }

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)(?::[^}]*)?\\}");

    /**
     * Retrieves a map of relative step fragment paths to their formatted string of required variables coming from test/caller.
     *
     * @return map of fragment file path to comma-separated required variable names
     */
    public Map<String, String> getFragmentRequiredVariablesMap()
    {
        final Map<String, String> result = new HashMap<>();
        final List<String> stepsFiles = getStepsFilesList();
        for (final String stepFile : stepsFiles)
        {
            final List<String> reqVars = getRequiredVariablesForFragment(stepFile);
            if (!reqVars.isEmpty())
            {
                result.put(stepFile, String.join(", ", reqVars));
            }
        }
        return result;
    }

    /**
     * Extracts required variables for a given step fragment file that are not defined within it.
     *
     * @param relativePath relative file path of the step fragment
     * @return list of required variable names
     */
    public List<String> getRequiredVariablesForFragment(final String relativePath)
    {
        if (relativePath == null || relativePath.isBlank() || !relativePath.toLowerCase().endsWith(".steps"))
        {
            return List.of();
        }
        try
        {
            final String content = readYamlFileContent(relativePath);
            if (content == null || content.isBlank())
            {
                return List.of();
            }
            final Map<String, Object> sections = parsePlaybookSections(content);
            @SuppressWarnings("unchecked")
            final Map<String, String> fragmentVarScopes = (Map<String, String>) sections.get("fragmentVarScopes");

            final Set<String> referencedVars = new LinkedHashSet<>();
            extractVarsFromObject(sections.get("beforeSteps"), referencedVars);
            extractVarsFromObject(sections.get("mainSteps"), referencedVars);
            extractVarsFromObject(sections.get("afterSteps"), referencedVars);

            final List<String> requiredVars = new ArrayList<>();
            for (final String varName : referencedVars)
            {
                final String scope = fragmentVarScopes != null ? fragmentVarScopes.get(varName) : null;
                if (!"defined".equalsIgnoreCase(scope))
                {
                    requiredVars.add(varName);
                }
            }
            return requiredVars;
        }
        catch (final Exception e)
        {
            LOGGER.debug("Could not parse fragment required variables for {}", relativePath, e);
            return List.of();
        }
    }

    private void extractVarsFromObject(final Object stepsObj, final Set<String> targetSet)
    {
        if (stepsObj instanceof List)
        {
            for (final Object item : (List<?>) stepsObj)
            {
                if (item != null)
                {
                    final String str = String.valueOf(item);
                    final Matcher matcher = VAR_PATTERN.matcher(str);
                    while (matcher.find())
                    {
                        if (matcher.group(1) != null)
                        {
                            targetSet.add(matcher.group(1));
                        }
                    }
                }
            }
        }
    }

    public void scanDirForStepsStatic(final File baseDir, final File currentDir, final List<String> stepsFiles)
    {
        final File[] files = currentDir.listFiles();
        if (files != null)
        {
            for (final File file : files)
            {
                if (file.isDirectory())
                {
                    scanDirForStepsStatic(baseDir, file, stepsFiles);
                }
                else
                {
                    final String name = file.getName().toLowerCase();
                    if (name.endsWith(".steps"))
                    {
                        final String relativePath = baseDir.toURI().relativize(file.toURI()).getPath();
                        stepsFiles.add(relativePath);
                    }
                }
            }
        }
    }

    public boolean isValidIncludeTarget(final String relativePath)
    {
        if (relativePath == null || relativePath.isBlank())
        {
            return false;
        }
        return relativePath.toLowerCase().endsWith(".steps");
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
                                        : itemMap.get("TEST_ID") != null ? itemMap.get("TEST_ID")
                                        : itemMap.get("id") != null ? itemMap.get("id")
                                        : itemMap.get("testid") != null ? itemMap.get("testid")
                                        : itemMap.get("testID");
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
                    final Object tId = root.get("testId") != null ? root.get("testId")
                            : root.get("TEST_ID") != null ? root.get("TEST_ID")
                            : root.get("id") != null ? root.get("id")
                            : root.get("testid") != null ? root.get("testid")
                            : root.get("testID");
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
            final String summary = e.getMessage() != null ? e.getMessage().replace('\n', ' ').replaceAll(" +", " ") : "Unknown syntax error";
            LOGGER.warn("Defective YAML file detected [{}]: {}", file.getName(), summary);
            LOGGER.debug("Stack trace for YAML parse failure:", e);
            details.put("hasError", true);
            details.put("error", e.getMessage());
            details.put("datasets", new ArrayList<DatasetDto>());
        }
        return details;
    }

    public File resolveCanonicalFile(final String file) throws IOException
    {
        final File resourcesDir = getResourcesDirectory().getCanonicalFile();
        File yamlFile = new File(resourcesDir, file).getCanonicalFile();
        if (!yamlFile.exists())
        {
            final File found = findFileRecursively(resourcesDir, file);
            if (found != null)
            {
                yamlFile = found.getCanonicalFile();
            }
        }
        if (!yamlFile.getPath().startsWith(resourcesDir.getPath()))
        {
            throw new SecurityException("Access denied: Directory traversal detected");
        }
        return yamlFile;
    }

    private File findFileRecursively(final File dir, final String targetName)
    {
        final String cleanTarget = targetName.contains("/") ? targetName.substring(targetName.lastIndexOf('/') + 1) : targetName;
        final boolean targetHasExt = cleanTarget.contains(".");
        final String baseTarget = targetHasExt
                ? cleanTarget.substring(0, cleanTarget.lastIndexOf('.'))
                : cleanTarget;

        final File[] files = dir.listFiles();
        if (files != null)
        {
            for (final File f : files)
            {
                if (f.isDirectory())
                {
                    final File found = findFileRecursively(f, targetName);
                    if (found != null)
                    {
                        return found;
                    }
                }
                else
                {
                    final String fName = f.getName();
                    if (fName.equalsIgnoreCase(cleanTarget))
                    {
                        return f;
                    }
                    final String fBase = fName.contains(".")
                            ? fName.substring(0, fName.lastIndexOf('.'))
                            : fName;
                    if (fBase.equalsIgnoreCase(baseTarget))
                    {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public String readYamlFileContent(final String file) throws IOException
    {
        final File yamlFile = resolveCanonicalFile(file);
        if (yamlFile.exists() && yamlFile.isFile())
        {
            return Files.readString(yamlFile.toPath(), StandardCharsets.UTF_8);
        }
        return null;
    }

    public void saveYamlFileContent(final String file, final String content) throws IOException
    {
        final File yamlFile = resolveCanonicalFile(file);
        if (yamlFile.getParentFile() != null && !yamlFile.getParentFile().exists())
        {
            yamlFile.getParentFile().mkdirs();
        }
        Files.writeString(yamlFile.toPath(), content, StandardCharsets.UTF_8);
    }

    public void createYamlFile(final String name) throws IOException
    {
        final String sanitizedName = (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".steps"))
                ? name
                : name + ".yaml";
        final File yamlFile = resolveCanonicalFile(sanitizedName);
        if (!yamlFile.exists())
        {
            if (yamlFile.getParentFile() != null && !yamlFile.getParentFile().exists())
            {
                yamlFile.getParentFile().mkdirs();
            }
            Files.writeString(yamlFile.toPath(), "", StandardCharsets.UTF_8);
        }
    }

    public boolean deleteYamlFile(final String file) throws IOException
    {
        final File yamlFile = resolveCanonicalFile(file);
        if (yamlFile.exists() && yamlFile.isFile())
        {
            Files.delete(yamlFile.toPath());
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parsePlaybookSections(final String content)
    {
        final Map<String, Object> result = new HashMap<>();
        final List<String> beforeSteps = new ArrayList<>();
        final List<String> mainSteps = new ArrayList<>();
        final List<String> afterSteps = new ArrayList<>();
        final List<Map<String, String>> dataMatrix = new ArrayList<>();
        final List<String> varKeys = new ArrayList<>();
        final Map<String, String> fragmentVarScopes = new HashMap<>();

        if (content != null && !content.trim().isEmpty())
        {
            try
            {
                final Yaml yaml = new Yaml();
                final Object loaded = yaml.load(content);
                if (loaded instanceof Map)
                {
                    final Map<String, Object> root = (Map<String, Object>) loaded;

                    if (root.containsKey("before"))
                    {
                        final String beforeStr = String.valueOf(root.get("before"));
                        for (final String line : beforeStr.split("\n"))
                        {
                            if (!line.trim().isEmpty())
                            {
                                beforeSteps.add(line.trim());
                            }
                        }
                    }

                    if (!root.containsKey("steps") && !root.containsKey("before") && !root.containsKey("after")
                            && (root.containsKey("_include") || root.containsKey("include")))
                    {
                        final String incPath = root.containsKey("_include")
                                ? String.valueOf(root.get("_include"))
                                : String.valueOf(root.get("include"));
                        mainSteps.add("_include: " + incPath);
                    }

                    if (root.containsKey("steps"))
                    {
                        final Object stepsObj = root.get("steps");
                        if (stepsObj instanceof List)
                        {
                            for (final Object stepItem : (List<?>) stepsObj)
                            {
                                if (stepItem instanceof Map)
                                {
                                    final Map<?, ?> mapStep = (Map<?, ?>) stepItem;
                                    if (mapStep.containsKey("_include") || mapStep.containsKey("include"))
                                    {
                                        final String incPath = mapStep.containsKey("_include")
                                                ? String.valueOf(mapStep.get("_include"))
                                                : String.valueOf(mapStep.get("include"));
                                        mainSteps.add("_include: " + incPath);
                                    }
                                    else if (mapStep.containsKey("instruction"))
                                    {
                                        final String inst = String.valueOf(mapStep.get("instruction")).trim();
                                        if (!inst.isEmpty())
                                        {
                                            mainSteps.add(inst);
                                        }
                                    }
                                    else
                                    {
                                        final String sMap = String.valueOf(mapStep).trim();
                                        if (!sMap.isEmpty())
                                        {
                                            mainSteps.add(sMap);
                                        }
                                    }
                                }
                                else if (stepItem != null && !String.valueOf(stepItem).trim().isEmpty())
                                {
                                    mainSteps.add(String.valueOf(stepItem).trim());
                                }
                            }
                        }
                        else
                        {
                            final String stepsStr = String.valueOf(stepsObj);
                            for (final String line : stepsStr.split("\n"))
                            {
                                if (!line.trim().isEmpty())
                                {
                                    mainSteps.add(line.trim());
                                }
                            }
                        }
                    }

                    if (root.containsKey("after"))
                    {
                        final String afterStr = String.valueOf(root.get("after"));
                        for (final String line : afterStr.split("\n"))
                        {
                            if (!line.trim().isEmpty())
                            {
                                afterSteps.add(line.trim());
                            }
                        }
                    }

                    if (root.containsKey("variables") && root.get("variables") instanceof Map)
                    {
                        final Map<?, ?> varMap = (Map<?, ?>) root.get("variables");
                        for (final Map.Entry<?, ?> entry : varMap.entrySet())
                        {
                            fragmentVarScopes.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                        }
                    }

                    if (root.containsKey("data") && root.get("data") instanceof List)
                    {
                        final List<?> dataList = (List<?>) root.get("data");
                        for (final Object item : dataList)
                        {
                            if (item instanceof Map)
                            {
                                final Map<?, ?> itemMap = (Map<?, ?>) item;
                                final Map<String, String> row = new HashMap<>();
                                for (final Map.Entry<?, ?> entry : itemMap.entrySet())
                                {
                                    final String k = String.valueOf(entry.getKey());
                                    final String v = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
                                    row.put(k, v);
                                    if (!varKeys.contains(k))
                                    {
                                        varKeys.add(k);
                                    }
                                }
                                dataMatrix.add(row);
                            }
                        }
                    }
                }
                else if (loaded instanceof List)
                {
                    for (final Object stepItem : (List<?>) loaded)
                    {
                        if (stepItem instanceof Map)
                        {
                            final Map<?, ?> mapStep = (Map<?, ?>) stepItem;
                            if (mapStep.containsKey("_include") || mapStep.containsKey("include"))
                            {
                                final String incPath = mapStep.containsKey("_include")
                                        ? String.valueOf(mapStep.get("_include"))
                                        : String.valueOf(mapStep.get("include"));
                                mainSteps.add("_include: " + incPath);
                            }
                            else if (mapStep.containsKey("instruction"))
                            {
                                final String inst = String.valueOf(mapStep.get("instruction")).trim();
                                if (!inst.isEmpty())
                                {
                                    mainSteps.add(inst);
                                }
                            }
                            else
                            {
                                final String sMap = String.valueOf(mapStep).trim();
                                if (!sMap.isEmpty())
                                {
                                    mainSteps.add(sMap);
                                }
                            }
                        }
                        else if (stepItem != null)
                        {
                            String s = String.valueOf(stepItem).trim();
                            if (s.startsWith("-"))
                            {
                                s = s.substring(1).trim();
                            }
                            if (!s.isEmpty())
                            {
                                mainSteps.add(s);
                            }
                        }
                    }
                }

                // Fallback for raw text lines
                if (mainSteps.isEmpty() && beforeSteps.isEmpty() && afterSteps.isEmpty())
                {
                    for (final String line : content.split("\n"))
                    {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#"))
                        {
                            if (trimmed.startsWith("-"))
                            {
                                trimmed = trimmed.substring(1).trim();
                            }
                            if (!trimmed.isEmpty())
                            {
                                mainSteps.add(trimmed);
                            }
                        }
                    }
                }
                result.put("hasError", false);
                result.put("error", null);
            }
            catch (final Exception e)
            {
                boolean parsedAsLines = false;
                final String trimmedContent = content.trim();
                if (!trimmedContent.startsWith("steps:") && !trimmedContent.contains("\nsteps:"))
                {
                    for (final String line : content.split("\n"))
                    {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#"))
                        {
                            if (trimmed.startsWith("-"))
                            {
                                trimmed = trimmed.substring(1).trim();
                            }
                            if (!trimmed.isEmpty())
                            {
                                mainSteps.add(trimmed);
                            }
                        }
                    }
                    if (!mainSteps.isEmpty())
                    {
                        parsedAsLines = true;
                        result.put("hasError", false);
                        result.put("error", null);
                    }
                }

                if (!parsedAsLines)
                {
                    final String summary = e.getMessage() != null ? e.getMessage().replace('\n', ' ').replaceAll(" +", " ") : "Unknown syntax error";
                    LOGGER.warn("Defective YAML content syntax: {}", summary);
                    LOGGER.debug("Stack trace for YAML content parse failure:", e);
                    result.put("hasError", true);
                    result.put("error", e.getMessage());
                }
            }
        }
        else
        {
            result.put("hasError", false);
            result.put("error", null);
        }

        result.put("beforeSteps", beforeSteps);
        result.put("mainSteps", mainSteps);
        result.put("afterSteps", afterSteps);
        result.put("dataMatrix", dataMatrix);
        result.put("varKeys", varKeys);
        result.put("fragmentVarScopes", fragmentVarScopes);
        return result;
    }
}
