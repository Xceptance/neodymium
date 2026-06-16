/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xceptance.neodymium.common.testdata.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

import com.xceptance.neodymium.util.Neodymium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Utility class to read and parse YAML test scripts with support for dynamic and static inclusions,
 * variables resolution, and location tracing.
 */
public final class YamlFileReader
{
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private static final Set<String> ALLOWED_KEYS = Set.of(
        "_meta", "_properties", "_context", "_data", "_beforeAll", "_beforeEach", "_steps",
        "_onSuccessEach", "_onFailureEach", "_afterEach", "_onSuccessAll", "_onFailureAll",
        "_afterAll", "_include", "_id", "_testId", "_sensitive", "_onSuccess", "_onFailure"
    );

    /**
     * Reads a YAML test script from an InputStream.
     *
     * @param inputStream the input stream containing the YAML content
     * @return the list of parsed datasets
     */
    public static List<Map<String, String>> readFile(final InputStream inputStream)
    {
        return readFile(inputStream, null, null);
    }

    /**
     * Reads a YAML test script from a File.
     *
     * @param file the File to read
     * @return the list of parsed datasets
     */
    public static List<Map<String, String>> readFile(final File file)
    {
        return readFile(file, null);
    }

    /**
     * Reads a YAML test script from a File, providing classpath resource context for relative includes.
     *
     * @param file                  the File to read
     * @param classpathResourcePath the classpath resource path if loaded from classpath
     * @return the list of parsed datasets
     */
    public static List<Map<String, String>> readFile(final File file, final String classpathResourcePath)
    {
        try
        {
            return readFile(new FileInputStream(file), file, classpathResourcePath);
        }
        catch (final FileNotFoundException e)
        {
            throw new RuntimeException("File not found: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Internal implementation of YAML parsing, variable interpolation, and inclusions resolution.
     *
     * @param inputStream           the input stream containing the YAML content
     * @param file                  the file object (optional, for path resolution)
     * @param classpathResourcePath the classpath resource path context (optional)
     * @return the list of parsed datasets
     */
    public static List<Map<String, String>> readFile(final InputStream inputStream, final File file, final String classpathResourcePath)
    {
        final List<Map<String, String>> resultData = new LinkedList<>();
        try
        {
            final byte[] yamlBytes;
            try (final InputStream is = inputStream)
            {
                yamlBytes = is.readAllBytes();
            }

            final String yamlStr = new String(yamlBytes, StandardCharsets.UTF_8);
            final Object data = loadYaml(yamlStr);
            if (data == null)
            {
                return resultData;
            }

            final File baseDir = file != null ? file.getParentFile() : null;

            // Extract root information
            Map<?, ?> rootMap = null;
            List<?> rawDataList = null;

            if (data instanceof Map)
            {
                rootMap = (Map<?, ?>) data;
                validateUnderscoredKeys(rootMap);

                Object dataObj = rootMap.get("_data");
                if (dataObj == null)
                {
                    dataObj = rootMap.get("data");
                }

                if (dataObj instanceof List)
                {
                    rawDataList = (List<?>) dataObj;
                }
                else if (dataObj instanceof Map<?, ?> mapData && mapData.containsKey("_include"))
                {
                    // Full block inclusion
                    final String includePath = String.valueOf(mapData.get("_include"));
                    final Object loaded = loadIncludeData(includePath, baseDir, classpathResourcePath);
                    if (loaded instanceof List)
                    {
                        rawDataList = (List<?>) loaded;
                    }
                }
                else
                {
                    rawDataList = List.of(rootMap);
                }
            }
            else if (data instanceof List)
            {
                rawDataList = (List<?>) data;
            }
            else
            {
                throw new RuntimeException("YAML root must be a List or a Map containing a 'data' List.");
            }

            // Resolve data inclusions (list-level and map-level)
            final List<Map<String, Object>> datasetRows = resolveDataInclusions(rawDataList, baseDir, classpathResourcePath);

            // Extract meta block
            final Map<String, Object> metaMap = new HashMap<>();
            if (rootMap != null)
            {
                final Object metaObj = rootMap.get("_meta");
                if (metaObj instanceof Map<?, ?> map)
                {
                    for (final Map.Entry<?, ?> entry : map.entrySet())
                    {
                        metaMap.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
            }

            // Iterate over dataset rows
            for (final Map<String, Object> row : datasetRows)
            {
                final Map<String, Object> variables = new LinkedHashMap<>(row);

                for (final Map.Entry<String, Object> entry : metaMap.entrySet())
                {
                    if (!variables.containsKey(entry.getKey()))
                    {
                        variables.put(entry.getKey(), entry.getValue());
                    }
                }

                // Process properties block from rootMap or local row
                final Map<String, String> propertiesMap = new HashMap<>();
                if (rootMap != null)
                {
                    parseAndFlattenProperties(rootMap.get("_properties"), propertiesMap);
                    parseAndFlattenProperties(rootMap.get("properties"), propertiesMap);
                }
                parseAndFlattenProperties(variables.get("_properties"), propertiesMap);
                parseAndFlattenProperties(variables.get("properties"), propertiesMap);

                variables.putAll(propertiesMap);

                // Prepare active blocks to resolve in the interleaved loop
                final List<Step> beforeAllSteps = normalizeSteps(getLifecycleBlock(rootMap, variables, "_beforeAll", "beforeAll"), file, yamlStr);
                final List<Step> beforeEachSteps = normalizeSteps(getLifecycleBlock(rootMap, variables, "_beforeEach", "before"), file, yamlStr);
                final List<Step> stepsSteps = normalizeSteps(getLifecycleBlock(rootMap, variables, "_steps", "steps"), file, yamlStr);
                final List<Step> afterEachSteps = normalizeSteps(getLifecycleBlock(rootMap, variables, "_afterEach", "after"), file, yamlStr);
                final List<Step> afterAllSteps = normalizeSteps(getLifecycleBlock(rootMap, variables, "_afterAll", "afterAll"), file, yamlStr);

                final List<Step> onSuccessEachSteps = normalizeSteps(getLifecycleBlock(rootMap, variables, "_onSuccessEach", "_onSuccess"), file, yamlStr);
                final List<Step> onFailureEachSteps = normalizeSteps(getLifecycleBlock(rootMap, variables, "_onFailureEach", "_onFailure"), file, yamlStr);
                final List<Step> onSuccessAllSteps = normalizeSteps(getLifecycleBlock(rootMap, variables, "_onSuccessAll", "onSuccessAll"), file, yamlStr);
                final List<Step> onFailureAllSteps = normalizeSteps(getLifecycleBlock(rootMap, variables, "_onFailureAll", "onFailureAll"), file, yamlStr);

                // Interleaved Loop for resolving variables and expanding inclusions
                int depth = 0;
                boolean changed = true;
                final List<File> inclusionStack = new ArrayList<>();
                if (file != null)
                {
                    inclusionStack.add(file);
                }

                while (changed && depth < 10)
                {
                    changed = false;
                    depth++;

                    // 1. Resolve variables in all blocks
                    if (resolveVariablesInSteps(beforeAllSteps, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (resolveVariablesInSteps(beforeEachSteps, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (resolveVariablesInSteps(stepsSteps, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (resolveVariablesInSteps(afterEachSteps, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (resolveVariablesInSteps(afterAllSteps, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (resolveVariablesInSteps(onSuccessEachSteps, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (resolveVariablesInSteps(onFailureEachSteps, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (resolveVariablesInSteps(onSuccessAllSteps, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (resolveVariablesInSteps(onFailureAllSteps, variables, metaMap))
                    {
                        changed = true;
                    }

                    // 2. Expand includes in all blocks
                    if (expandIncludesInSteps(beforeAllSteps, baseDir, classpathResourcePath, inclusionStack, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (expandIncludesInSteps(beforeEachSteps, baseDir, classpathResourcePath, inclusionStack, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (expandIncludesInSteps(stepsSteps, baseDir, classpathResourcePath, inclusionStack, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (expandIncludesInSteps(afterEachSteps, baseDir, classpathResourcePath, inclusionStack, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (expandIncludesInSteps(afterAllSteps, baseDir, classpathResourcePath, inclusionStack, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (expandIncludesInSteps(onSuccessEachSteps, baseDir, classpathResourcePath, inclusionStack, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (expandIncludesInSteps(onFailureEachSteps, baseDir, classpathResourcePath, inclusionStack, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (expandIncludesInSteps(onSuccessAllSteps, baseDir, classpathResourcePath, inclusionStack, variables, metaMap))
                    {
                        changed = true;
                    }
                    if (expandIncludesInSteps(onFailureAllSteps, baseDir, classpathResourcePath, inclusionStack, variables, metaMap))
                    {
                        changed = true;
                    }
                }

                if (changed)
                {
                    throw new MalformedPlaybookException("Inclusion depth exceeded maximum of 10. Circular dependency likely.");
                }

                // Construct final row dataset map
                final Map<String, String> finalRow = new LinkedHashMap<>();
                for (final Map.Entry<String, Object> entry : variables.entrySet())
                {
                    final String key = entry.getKey();
                    if ("_properties".equals(key) || "properties".equals(key))
                    {
                        continue;
                    }
                    final Object val = entry.getValue();
                    if (val == null)
                    {
                        finalRow.put(key, null);
                    }
                    else if (val instanceof Map || val instanceof List)
                    {
                        finalRow.put(key, GSON.toJson(val));
                    }
                    else
                    {
                        finalRow.put(key, String.valueOf(val));
                    }
                }

                serializeStepsBlock(finalRow, "before", beforeEachSteps);
                serializeStepsBlock(finalRow, "steps", stepsSteps);
                serializeStepsBlock(finalRow, "after", afterEachSteps);
                serializeStepsBlock(finalRow, "beforeAll", beforeAllSteps);
                serializeStepsBlock(finalRow, "afterAll", afterAllSteps);

                serializeStepsBlock(finalRow, "onSuccessEach", onSuccessEachSteps);
                serializeStepsBlock(finalRow, "onFailureEach", onFailureEachSteps);
                serializeStepsBlock(finalRow, "onSuccessAll", onSuccessAllSteps);
                serializeStepsBlock(finalRow, "onFailureAll", onFailureAllSteps);

                // Step Location Tracing JSON serialization
                final List<String> stepLineNumbersList = new ArrayList<>();
                for (final Step step : stepsSteps)
                {
                    stepLineNumbersList.add(step.trace);
                }
                finalRow.put("neodymium.stepLineNumbers", GSON.toJson(stepLineNumbersList));

                if (classpathResourcePath != null)
                {
                    finalRow.put("neodymium.classpathResourcePath", classpathResourcePath);
                }

                resultData.add(finalRow);
            }
        }
        catch (final Exception e)
        {
            if (e instanceof MalformedPlaybookException)
            {
                throw (MalformedPlaybookException) e;
            }
            throw new RuntimeException("Error parsing YAML file", e);
        }

        return resultData;
    }

    private static Object getLifecycleBlock(final Map<?, ?> rootMap, final Map<String, Object> variables, final String preferredKey, final String fallbackKey)
    {
        if (variables.containsKey(preferredKey))
        {
            return variables.get(preferredKey);
        }
        if (variables.containsKey(fallbackKey))
        {
            return variables.get(fallbackKey);
        }
        if (rootMap != null)
        {
            if (rootMap.containsKey(preferredKey))
            {
                return rootMap.get(preferredKey);
            }
            if (rootMap.containsKey(fallbackKey))
            {
                return rootMap.get(fallbackKey);
            }
        }
        return null;
    }

    private static void parseAndFlattenProperties(final Object propertiesObj, final Map<String, String> targetMap)
    {
        if (propertiesObj instanceof Map<?, ?> map)
        {
            for (final Map.Entry<?, ?> entry : map.entrySet())
            {
                final String key = String.valueOf(entry.getKey());
                if ("skipReplay".equals(key))
                {
                    targetMap.put("neodymium.ai.skipReplay", String.valueOf(entry.getValue()));
                }
                else
                {
                    flattenProperties(entry.getValue(), key, targetMap);
                }
            }
            for (final Map.Entry<?, ?> entry : map.entrySet())
            {
                final String key = String.valueOf(entry.getKey());
                if (key.contains("."))
                {
                    targetMap.put(key, String.valueOf(entry.getValue()));
                }
            }
        }
    }

    private static void flattenProperties(final Object value, final String prefix, final Map<String, String> targetMap)
    {
        if (value instanceof Map<?, ?> map)
        {
            for (final Map.Entry<?, ?> entry : map.entrySet())
            {
                final String key = String.valueOf(entry.getKey());
                final String nextPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                flattenProperties(entry.getValue(), nextPrefix, targetMap);
            }
        }
        else if (value != null)
        {
            targetMap.put(prefix, String.valueOf(value));
        }
    }

    private static void validateUnderscoredKeys(final Map<?, ?> map)
    {
        if (map == null)
        {
            return;
        }
        for (final Object keyObj : map.keySet())
        {
            final String key = String.valueOf(keyObj);
            if (key.startsWith("_") && !ALLOWED_KEYS.contains(key))
            {
                throw new MalformedPlaybookException("Unrecognized framework key: " + key);
            }
        }
    }

    private static List<Map<String, Object>> resolveDataInclusions(final List<?> rawDataList, final File baseDir, final String classpathResourcePath)
    {
        final List<Map<String, Object>> resolvedList = new ArrayList<>();
        if (rawDataList == null)
        {
            return resolvedList;
        }

        for (final Object item : rawDataList)
        {
            if (item instanceof Map<?, ?> map)
            {
                @SuppressWarnings("unchecked")
                final Map<String, Object> rowMap = new LinkedHashMap<>((Map<String, Object>) map);
                validateUnderscoredKeys(rowMap);

                if (rowMap.containsKey("_include"))
                {
                    final String rawPath = String.valueOf(rowMap.get("_include"));
                    String resolvedPath = rawPath;
                    if (rawPath.contains("${"))
                    {
                        resolvedPath = resolveVariables(rawPath, rowMap, null);
                    }

                    final Object includedData = loadIncludeData(resolvedPath, baseDir, classpathResourcePath);
                    rowMap.remove("_include");

                    if (includedData instanceof Map<?, ?> includedMap)
                    {
                        for (final Map.Entry<?, ?> entry : includedMap.entrySet())
                        {
                            final String key = String.valueOf(entry.getKey());
                            if (!rowMap.containsKey(key))
                            {
                                rowMap.put(key, entry.getValue());
                            }
                        }
                    }
                    else if (includedData instanceof List<?> includedList)
                    {
                        if (rowMap.isEmpty())
                        {
                            for (final Object subItem : includedList)
                            {
                                if (subItem instanceof Map<?, ?> subMap)
                                {
                                    @SuppressWarnings("unchecked")
                                    final Map<String, Object> resolvedSubMap = new LinkedHashMap<>((Map<String, Object>) subMap);
                                    validateUnderscoredKeys(resolvedSubMap);
                                    resolvedList.add(resolvedSubMap);
                                }
                            }
                            continue;
                        }
                    }
                }
                resolvedList.add(rowMap);
            }
        }
        return resolvedList;
    }

    private static Object loadIncludeData(final String path, final File baseDir, final String classpathResourcePath)
    {
        File includedFile = null;
        if (baseDir != null)
        {
            includedFile = new File(baseDir, path);
        }

        if (includedFile != null && includedFile.exists())
        {
            try
            {
                final String content = Files.readString(includedFile.toPath(), StandardCharsets.UTF_8);
                final Object loaded = loadYaml(content);
                return unwrapData(loaded);
            }
            catch (final IOException e)
            {
                throw new RuntimeException("Failed to read include data file: " + includedFile.getAbsolutePath(), e);
            }
        }

        String resourcePath = path;
        if (classpathResourcePath != null)
        {
            final int lastSlash = classpathResourcePath.lastIndexOf('/');
            if (lastSlash != -1)
            {
                resourcePath = classpathResourcePath.substring(0, lastSlash + 1) + path;
            }
        }

        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (is != null)
        {
            try
            {
                final byte[] bytes = is.readAllBytes();
                final String content = new String(bytes, StandardCharsets.UTF_8);
                final Object loaded = loadYaml(content);
                return unwrapData(loaded);
            }
            catch (final IOException e)
            {
                throw new RuntimeException("Failed to read classpath include data file: " + path, e);
            }
        }

        throw new RuntimeException("Could not resolve include data path: " + path);
    }

    private static Object unwrapData(final Object loaded)
    {
        if (loaded instanceof Map<?, ?> map)
        {
            if (map.containsKey("_data"))
            {
                return map.get("_data");
            }
            else if (map.containsKey("data"))
            {
                return map.get("data");
            }
        }
        return loaded;
    }

    private static List<Step> normalizeSteps(final Object stepsObj, final File file, final String yamlContent)
    {
        final List<Step> stepsList = new ArrayList<>();
        if (stepsObj == null)
        {
            return stepsList;
        }

        final List<String> lines = yamlContent != null ? yamlContent.lines().toList() : new ArrayList<>();
        int searchFromLine = 0;

        if (stepsObj instanceof List<?> list)
        {
            for (final Object item : list)
            {
                if (item instanceof Map<?, ?> map && map.containsKey("_include"))
                {
                    final String path = String.valueOf(map.get("_include"));
                    final String stepText = "_include: " + path;
                    final int lineNum = findLineNumber(path, lines, searchFromLine);
                    searchFromLine = Math.max(searchFromLine, lineNum);
                    final String trace = (file != null ? file.getName() : "inline") + ":" + lineNum;
                    stepsList.add(new Step(stepText, trace));
                }
                else if (item instanceof Map<?, ?> map)
                {
                    final StringBuilder sb = new StringBuilder();
                    for (final Map.Entry<?, ?> entry : map.entrySet())
                    {
                        if (sb.length() > 0)
                        {
                            sb.append(", ");
                        }
                        sb.append(entry.getKey()).append(": ").append(entry.getValue());
                    }
                    final String stepText = sb.toString();
                    final int lineNum = findLineNumber(stepText, lines, searchFromLine);
                    searchFromLine = Math.max(searchFromLine, lineNum);
                    final String trace = (file != null ? file.getName() : "inline") + ":" + lineNum;
                    stepsList.add(new Step(stepText, trace));
                }
                else if (item != null)
                {
                    final String stepText = String.valueOf(item);
                    final int lineNum = findLineNumber(stepText, lines, searchFromLine);
                    searchFromLine = Math.max(searchFromLine, lineNum);
                    final String trace = (file != null ? file.getName() : "inline") + ":" + lineNum;
                    stepsList.add(new Step(stepText, trace));
                }
            }
        }
        else
        {
            final String stepsStr = String.valueOf(stepsObj);
            final String[] linesArr = stepsStr.split("\\r?\\n", -1);
            for (final String line : linesArr)
            {
                final String trimmed = line.trim();
                if (!trimmed.isEmpty())
                {
                    final int lineNum = findLineNumber(trimmed, lines, searchFromLine);
                    searchFromLine = Math.max(searchFromLine, lineNum);
                    final String trace = (file != null ? file.getName() : "inline") + ":" + lineNum;
                    stepsList.add(new Step(line, trace));
                }
            }
        }
        return stepsList;
    }

    private static int findLineNumber(final String stepText, final List<String> lines, final int startFrom)
    {
        final String trimmed = stepText.trim();
        for (int i = startFrom; i < lines.size(); i++)
        {
            final String line = lines.get(i).trim();
            if (line.contains(trimmed))
            {
                return i + 1;
            }
        }
        return startFrom + 1;
    }

    private static List<Step> loadIncludedSteps(final String path, final File baseDir, final String classpathResourcePath, final List<File> inclusionStack)
    {
        File includedFile = null;
        if (baseDir != null)
        {
            includedFile = new File(baseDir, path);
        }

        if (includedFile != null && includedFile.exists())
        {
            if (inclusionStack.contains(includedFile))
            {
                final StringBuilder sb = new StringBuilder();
                for (final File f : inclusionStack)
                {
                    sb.append(f.getName()).append(" -> ");
                }
                sb.append(includedFile.getName());
                throw new MalformedPlaybookException("Circular inclusion detected: " + sb.toString());
            }

            final List<File> newStack = new ArrayList<>(inclusionStack);
            newStack.add(includedFile);

            try
            {
                final String content = Files.readString(includedFile.toPath(), StandardCharsets.UTF_8);
                final Object loaded = loadYaml(content);

                Object stepsObj = null;
                if (loaded instanceof List<?> list)
                {
                    stepsObj = list;
                }
                else if (loaded instanceof Map<?, ?> map)
                {
                    stepsObj = map.get("_steps");
                    if (stepsObj == null)
                    {
                        stepsObj = map.get("steps");
                    }
                }

                return normalizeSteps(stepsObj, includedFile, content);
            }
            catch (final IOException e)
            {
                throw new RuntimeException("Failed to read include file: " + includedFile.getAbsolutePath(), e);
            }
        }

        String resourcePath = path;
        if (classpathResourcePath != null)
        {
            final int lastSlash = classpathResourcePath.lastIndexOf('/');
            if (lastSlash != -1)
            {
                resourcePath = classpathResourcePath.substring(0, lastSlash + 1) + path;
            }
        }

        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (is != null)
        {
            try
            {
                final byte[] bytes = is.readAllBytes();
                final String content = new String(bytes, StandardCharsets.UTF_8);
                final Object loaded = loadYaml(content);

                Object stepsObj = null;
                if (loaded instanceof List<?> list)
                {
                    stepsObj = list;
                }
                else if (loaded instanceof Map<?, ?> map)
                {
                    stepsObj = map.get("_steps");
                    if (stepsObj == null)
                    {
                        stepsObj = map.get("steps");
                    }
                }

                return normalizeSteps(stepsObj, new File(path), content);
            }
            catch (final IOException e)
            {
                throw new RuntimeException("Failed to read classpath include file: " + path, e);
            }
        }

        throw new RuntimeException("Could not resolve include path: " + path);
    }

    private static boolean resolveVariablesInSteps(final List<Step> steps, final Map<String, Object> variables, final Map<String, Object> meta)
    {
        boolean changed = false;
        for (final Step step : steps)
        {
            final String resolved = resolveVariables(step.text, variables, meta);
            if (!resolved.equals(step.text))
            {
                step.text = resolved;
                changed = true;
            }
        }
        return changed;
    }

    private static boolean expandIncludesInSteps(final List<Step> steps, final File baseDir, final String classpathResourcePath, final List<File> inclusionStack, final Map<String, Object> variables, final Map<String, Object> meta)
    {
        boolean changed = false;

        for (int i = 0; i < steps.size(); i++)
        {
            final Step step = steps.get(i);

            if (step.text.trim().startsWith("_include:"))
            {
                final String rawPath = step.text.trim().substring(9).trim();
                final String resolvedPath = resolveVariables(rawPath, variables, meta);
                final List<Step> included = loadIncludedSteps(resolvedPath, baseDir, classpathResourcePath, inclusionStack);

                for (final Step inc : included)
                {
                    inc.trace = inc.trace + " -> " + step.trace;
                }

                steps.remove(i);
                steps.addAll(i, included);
                i += included.size() - 1;
                changed = true;
            }
        }
        return changed;
    }

    private static void serializeStepsBlock(final Map<String, String> finalRow, final String key, final List<Step> steps)
    {
        if (steps.isEmpty())
        {
            return;
        }
        final StringBuilder sb = new StringBuilder();
        for (final Step step : steps)
        {
            sb.append(step.text).append("\n");
        }
        if (sb.length() > 0)
        {
            sb.setLength(sb.length() - 1);
        }
        finalRow.put(key, sb.toString());
    }

    private static String resolveVariables(final String text, final Map<String, ?> variables, final Map<String, ?> meta)
    {
        if (text == null)
        {
            return null;
        }

        final StringBuilder result = new StringBuilder();
        int cursor = 0;
        while (true)
        {
            final int startIdx = text.indexOf("${", cursor);
            if (startIdx == -1)
            {
                result.append(text.substring(cursor));
                break;
            }
            result.append(text.substring(cursor, startIdx));
            final int endIdx = text.indexOf("}", startIdx + 2);
            if (endIdx == -1)
            {
                result.append(text.substring(startIdx));
                break;
            }

            final String placeholder = text.substring(startIdx + 2, endIdx).trim();
            String resolvedValue = null;

            if (placeholder.startsWith("_meta."))
            {
                final String metaKey = placeholder.substring(6);
                resolvedValue = lookupCaseInsensitive(metaKey, meta);
            }
            else
            {
                resolvedValue = lookupCaseInsensitive(placeholder, variables);
                if (resolvedValue == null)
                {
                    resolvedValue = lookupCaseInsensitive(placeholder, meta);
                }
            }

            if (resolvedValue != null)
            {
                result.append(resolvedValue);
            }
            else
            {
                result.append("${").append(placeholder).append("}");
            }
            cursor = endIdx + 1;
        }
        return result.toString();
    }

    private static String lookupCaseInsensitive(final String key, final Map<String, ?> map)
    {
        if (map == null)
        {
            return null;
        }
        if (map.containsKey(key))
        {
            final Object val = map.get(key);
            return val == null ? null : String.valueOf(val);
        }
        for (final Map.Entry<String, ?> entry : map.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(key))
            {
                final Object val = entry.getValue();
                return val == null ? null : String.valueOf(val);
            }
        }
        return null;
    }

    private static Object loadYaml(final String content)
    {
        final Yaml yaml = new Yaml();
        return yaml.load(preprocessYamlString(content));
    }

    private static String preprocessYamlString(final String yamlStr)
    {
        final String[] lines = yamlStr.split("\\r?\\n", -1);
        final Pattern listPattern = Pattern.compile("^(\\s*-\\s+)(If\\s+.*_include:.*)$");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++)
        {
            final String line = lines[i];
            final Matcher m = listPattern.matcher(line);
            if (m.find())
            {
                final String value = m.group(2);
                if (!value.startsWith("\"") && !value.startsWith("'"))
                {
                    final String escaped = value.replace("\"", "\\\"");
                    sb.append(m.group(1)).append("\"").append(escaped).append("\"");
                }
                else
                {
                    sb.append(line);
                }
            }
            else
            {
                sb.append(line);
            }
            if (i < lines.length - 1)
            {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public static List<Step> loadInclude(final String path)
    {
        final String sourceFileVal = Neodymium.getData() != null && Neodymium.getData().exists("neodymium.sourceFile")
                ? Neodymium.getData().asString("neodymium.sourceFile")
                : null;

        String classpathResourcePathVal = Neodymium.getData() != null && Neodymium.getData().exists("neodymium.classpathResourcePath")
                ? Neodymium.getData().asString("neodymium.classpathResourcePath")
                : null;

        if (classpathResourcePathVal == null && sourceFileVal != null)
        {
            final String normalized = sourceFileVal.replace('\\', '/');
            final String[] patterns = {
                "target/test-classes/",
                "target/classes/",
                "build/resources/main/",
                "build/resources/test/",
                "build/classes/java/main/",
                "build/classes/java/test/",
                "out/production/resources/",
                "out/test/resources/",
                "out/production/classes/",
                "out/test/classes/",
                "bin/"
            };
            for (final String pat : patterns)
            {
                final int idx = normalized.indexOf(pat);
                if (idx != -1)
                {
                    classpathResourcePathVal = normalized.substring(idx + pat.length());
                    break;
                }
            }
        }

        File baseDirVal = null;
        if (sourceFileVal != null)
        {
            final File f = new File(sourceFileVal);
            if (f.isAbsolute() || f.exists())
            {
                baseDirVal = f.getParentFile();
            }
            else if (classpathResourcePathVal == null)
            {
                classpathResourcePathVal = sourceFileVal;
            }
        }

        if (baseDirVal == null && classpathResourcePathVal == null)
        {
            baseDirVal = new File(".");
        }

        final File baseDir = baseDirVal;
        final String classpathResourcePath = classpathResourcePathVal;

        return loadIncludedSteps(path, baseDir, classpathResourcePath, new ArrayList<>());
    }

    /**
     * Represents a single step in a test playbook with trace information.
     */
    public static final class Step
    {
        /**
         * The text of the step.
         */
        public String text;

        /**
         * The location trace of the step.
         */
        public String trace;

        /**
         * Constructs a new Step.
         *
         * @param text  the step text
         * @param trace the location trace
         */
        public Step(final String text, final String trace)
        {
            this.text = text;
            this.trace = trace;
        }
    }
}
