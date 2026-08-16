/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.playbook;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Concrete implementation of {@link PlaybookParser} that parses playbooks
 * recursively from YAML files, resolving nested inclusions and checking for cycles.
 * <p>
 * Supported YAML formats:
 * <ul>
 *   <li><b>Consumer Format (Multiline Block):</b> {@code steps: |} multiline string block (primary natural language format).</li>
 *   <li><b>Consumer Format (String List):</b> {@code steps:} list of string instructions.</li>
 *   <li><b>Internal/Test Format (Structured Action Maps):</b> {@code steps:} list of maps containing {@code instruction} and {@code actions} list for single-file deterministic execution or test fixtures without a JSON companion.</li>
 *   <li><b>Inclusion Maps:</b> {@code steps:} list of maps containing {@code include: relative/path.yaml}.</li>
 * </ul>
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class YamlPlaybookParser implements PlaybookParser
{
    private static final Logger LOG = LoggerFactory.getLogger(YamlPlaybookParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructs a default YamlPlaybookParser.
     */
    public YamlPlaybookParser()
    {
    }

    /**
     * Parses a playbook from a YAML resource identifier using the provided manager.
     * Starts the parsing recursion with an empty active inclusion stack.
     *
     * @param identifier the resource identifier of the playbook file
     * @param manager the resource manager to retrieve file input streams and resolve relative paths
     * @return the parsed Playbook instance
     * @throws IOException if an I/O error or recursive parsing error occurs
     */
    @Override
    public Playbook parse(final String identifier, final PlaybookResourceManager manager) throws IOException
    {
        final Playbook playbook = parseInternal(identifier, manager);
        if (playbook != null)
        {
            setParentReferences(playbook.getSteps(), null);
        }
        return playbook;
    }

    private void setParentReferences(final List<PlaybookStep> steps, final PlaybookStep parent)
    {
        if (steps != null)
        {
            for (final PlaybookStep step : steps)
            {
                step.setParent(parent);
                setParentReferences(step.getSubSteps(), step);
            }
        }
    }

    private Playbook parseInternal(final String identifier, final PlaybookResourceManager manager) throws IOException
    {
        final LinkedHashSet<String> activeStack = new LinkedHashSet<>();
        final List<PlaybookStep> steps = new ArrayList<>();
        final List<Map<String, SessionData.DataEntry>> dataSets = new ArrayList<>();

        if (identifier.endsWith(".json"))
        {
            // Try to find the companion YAML file
            String yamlPath = identifier.substring(0, identifier.length() - 5) + ".yaml";
            boolean yamlExists = false;
            try (final InputStream in = manager.read(yamlPath))
            {
                if (in != null)
                {
                    yamlExists = true;
                }
            }
            catch (final Exception e)
            {
                // ignore
            }

            if (!yamlExists)
            {
                yamlPath = identifier.substring(0, identifier.length() - 5) + ".yml";
                try (final InputStream in = manager.read(yamlPath))
                {
                    if (in != null)
                    {
                        yamlExists = true;
                    }
                }
                catch (final Exception e)
                {
                    // ignore
                }
            }

            if (yamlExists)
            {
                try
                {
                    final Playbook merged = parseAndMerge(identifier, yamlPath, manager);
                    if (merged != null && merged.getSteps() != null && !merged.getSteps().isEmpty())
                    {
                        return merged;
                    }
                }
                catch (final Exception e)
                {
                    // Fall back to reading JSON recording directly
                }
            }
        }

        Map<String, String> promptAddons = new HashMap<>();
        if (!identifier.endsWith(".json"))
        {
            try (final InputStream in = manager.read(identifier))
            {
                if (in != null)
                {
                    final byte[] bytes = in.readAllBytes();
                    final String fileContent = new String(bytes, StandardCharsets.UTF_8);
                    final Yaml yaml = new Yaml();
                    final Map<String, Object> loadedMap = yaml.load(fileContent);
                    promptAddons = parsePromptAddons(loadedMap);
                }
            }
            catch (final Exception e)
            {
                // ignore
            }
        }

        // Check if it is a JSON recording
        try (final InputStream in = manager.read(identifier))
        {
            if (in == null)
            {
                throw new IOException("Failed to load playbook: resource stream is null for " + identifier);
            }
            final byte[] bytes = in.readAllBytes();
            final String content = new String(bytes, StandardCharsets.UTF_8).trim();
            if (content.startsWith("["))
            {
                try
                {
                    final List<PlaybookStep> parsedSteps = MAPPER.readValue(content, new TypeReference<List<PlaybookStep>>(){});
                    if (parsedSteps != null)
                    {
                        if (parsedSteps.isEmpty())
                        {
                            throw new IllegalArgumentException("Playbook cannot be empty: " + identifier + " parsed to 0 executable steps.");
                        }
                        return new Playbook(parsedSteps, dataSets, promptAddons);
                    }
                }
                catch (final IllegalArgumentException e)
                {
                    throw e;
                }
                catch (final Exception e)
                {
                    LOG.error("Jackson exception parsing JSON step list for {}: ", identifier, e);
                    // Fall back to legacy Action list parsing
                }

                final List<Action> actions = MAPPER.readValue(content, new TypeReference<List<Action>>(){});
                final String fileName = new File(identifier).getName();
                PlaybookStep currentStep = null;
                if (actions != null)
                {
                    for (final Action action : actions)
                    {
                        final String stepDesc = (action.getStepInstruction() != null && !action.getStepInstruction().trim().isEmpty())
                            ? action.getStepInstruction()
                            : action.getDescription();

                        final String stepFile = (action.getStepFile() != null && !action.getStepFile().trim().isEmpty())
                            ? action.getStepFile()
                            : fileName;

                        final int stepLine = action.getStepLine();

                        if (currentStep != null 
                            && Objects.equals(currentStep.getInstruction(), stepDesc)
                            && Objects.equals(currentStep.getSourceFile(), stepFile)
                            && currentStep.getLineNumber() == stepLine)
                        {
                            currentStep.getActions().add(action);
                            if (action.getStepScreenshotHash() != null && !action.getStepScreenshotHash().isEmpty())
                            {
                                currentStep.setScreenshotHash(action.getStepScreenshotHash());
                            }
                        }
                        else
                        {
                            currentStep = new PlaybookStep(stepDesc);
                            currentStep.getActions().add(action);
                            currentStep.setSourceFile(stepFile);
                            if (stepLine != -1)
                            {
                                currentStep.setLineNumber(stepLine);
                            }
                            if (action.getStepScreenshotHash() != null && !action.getStepScreenshotHash().isEmpty())
                            {
                                currentStep.setScreenshotHash(action.getStepScreenshotHash());
                            }
                            steps.add(currentStep);
                        }
                    }
                }
                return new Playbook(steps, dataSets, promptAddons);
            }
        }

        parseRecursive(identifier, manager, activeStack, steps, dataSets);

        if (steps.isEmpty())
        {
            throw new IllegalArgumentException("Playbook cannot be empty: " + identifier + " parsed to 0 executable steps.");
        }

        return new Playbook(steps, dataSets, promptAddons);
    }

    private Map<String, String> parsePromptAddons(final Map<String, Object> loadedMap)
    {
        final Map<String, String> addons = new HashMap<>();
        if (loadedMap == null)
        {
            return addons;
        }

        for (final Map.Entry<String, Object> entry : loadedMap.entrySet())
        {
            final String key = entry.getKey();
            if (key.startsWith("promptAddon.") && entry.getValue() instanceof String)
            {
                final String type = key.substring("promptAddon.".length());
                addons.put(type, (String) entry.getValue());
            }
            else if (key.equals("promptAddon") && entry.getValue() instanceof String)
            {
                addons.put("default", (String) entry.getValue());
            }
        }

        final Object promptAddonObj = loadedMap.get("promptAddon");
        if (promptAddonObj instanceof Map)
        {
            parseAddonMap((Map<?, ?>) promptAddonObj, addons);
        }

        return addons;
    }

    private void parseAddonMap(final Map<?, ?> sourceMap, final Map<String, String> targetMap)
    {
        for (final Map.Entry<?, ?> entry : sourceMap.entrySet())
        {
            final String subKey = String.valueOf(entry.getKey());
            if (entry.getValue() instanceof String)
            {
                targetMap.put(subKey, (String) entry.getValue());
            }
        }
    }

    /**
     * Helper method to parse a playbook YAML file recursively.
     * Manages cycle detection by tracking resolved paths in the active stack.
     *
     * @param identifier the current resource identifier to parse
     * @param manager the resource manager
     * @param activeStack the set of identifiers currently in the active recursive resolution path
     * @param outSteps the output steps list to add parsed steps to
     * @param outDataSets the output datasets list to add parsed data parameters to
     * @throws IOException if circular dependencies or I/O failures are encountered
     */
    @SuppressWarnings("unchecked")
    private void parseRecursive(
        final String identifier,
        final PlaybookResourceManager manager,
        final LinkedHashSet<String> activeStack,
        final List<PlaybookStep> outSteps,
        final List<Map<String, SessionData.DataEntry>> outDataSets) throws IOException
    {
        // 1. Cycle detection: if current path is already in the active stack, fail immediately
        if (!activeStack.add(identifier))
        {
            throw new IOException("Circular inclusion loop detected: " 
                + String.join(" -> ", activeStack) + " -> " + identifier);
        }

        try (final InputStream in = manager.read(identifier))
        {
            if (in == null)
            {
                throw new IOException("Failed to load playbook: resource stream is null for " + identifier);
            }

            final byte[] bytes = in.readAllBytes();
            final String fileContent = new String(bytes, StandardCharsets.UTF_8);
            final String fileName = new File(identifier).getName();

            final Yaml yaml = new Yaml();
            final Map<String, Object> loadedMap = yaml.load(fileContent);

            if (loadedMap == null)
            {
                return;
            }

            // 2. Parse datasets ('data')
            final Object rawData = loadedMap.get("data");
            if (rawData instanceof List)
            {
                for (final Object entry : (List<?>) rawData)
                {
                    if (entry instanceof Map)
                    {
                        final Map<String, SessionData.DataEntry> datasetMap = new HashMap<>();
                        for (final Map.Entry<?, ?> mapEntry : ((Map<?, ?>) entry).entrySet())
                        {
                            final String key = String.valueOf(mapEntry.getKey());
                            final Object val = mapEntry.getValue();
                            final boolean sensitive = isSensitiveKey(key);
                            datasetMap.put(key, new SessionData.DataEntry(val, sensitive));
                        }
                        outDataSets.add(datasetMap);
                    }
                }
            }

            // 3. Parse before, steps, and after blocks
            final String[] beforeKeys = {"before", "beforeEach", "_beforeEach", "_beforeAll"};
            for (final String key : beforeKeys)
            {
                parseStepBlock(loadedMap.get(key), identifier, fileName, fileContent, manager, activeStack, outSteps, outDataSets);
            }

            final String[] stepKeys = {"steps", "_steps"};
            for (final String key : stepKeys)
            {
                parseStepBlock(loadedMap.get(key), identifier, fileName, fileContent, manager, activeStack, outSteps, outDataSets);
            }

            final String[] afterKeys = {"after", "afterEach", "_afterEach", "_afterAll"};
            for (final String key : afterKeys)
            {
                parseStepBlock(loadedMap.get(key), identifier, fileName, fileContent, manager, activeStack, outSteps, outDataSets);
            }

            if (outSteps.isEmpty() && (loadedMap.containsKey("_include") || loadedMap.containsKey("include")))
            {
                final String includeRelativePath = loadedMap.containsKey("_include")
                    ? String.valueOf(loadedMap.get("_include"))
                    : String.valueOf(loadedMap.get("include"));
                final String resolvedIdentifier = manager.resolveInclude(identifier, includeRelativePath);

                final PlaybookStep includeStep = new PlaybookStep("_include: " + includeRelativePath);
                initStepLocation(includeStep, fileName, fileContent, "_include: " + includeRelativePath);
                parseRecursive(resolvedIdentifier, manager, activeStack, includeStep.getSubSteps(), outDataSets);
                outSteps.add(includeStep);
            }
        }
        finally
        {
            // Pop the identifier off the stack once its children are fully parsed
            activeStack.remove(identifier);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseStepBlock(
        final Object rawSteps,
        final String identifier,
        final String fileName,
        final String fileContent,
        final PlaybookResourceManager manager,
        final LinkedHashSet<String> activeStack,
        final List<PlaybookStep> outSteps,
        final List<Map<String, SessionData.DataEntry>> outDataSets) throws IOException
    {
        if (rawSteps instanceof List)
        {
            for (final Object stepItem : (List<?>) rawSteps)
            {
                if (stepItem instanceof String)
                {
                    final PlaybookStep step = new PlaybookStep((String) stepItem);
                    initStepLocation(step, fileName, fileContent, (String) stepItem);
                    outSteps.add(step);
                }
                else if (stepItem instanceof Map)
                {
                    final Map<?, ?> mapStep = (Map<?, ?>) stepItem;
                    if (mapStep.containsKey("_include") || mapStep.containsKey("include"))
                    {
                        final String includeRelativePath = mapStep.containsKey("_include") 
                            ? String.valueOf(mapStep.get("_include")) 
                            : String.valueOf(mapStep.get("include"));
                        final String resolvedIdentifier = manager.resolveInclude(identifier, includeRelativePath);

                        final PlaybookStep includeStep = new PlaybookStep("_include: " + includeRelativePath);
                        initStepLocation(includeStep, fileName, fileContent, "_include: " + includeRelativePath);
                        
                        parseRecursive(resolvedIdentifier, manager, activeStack, includeStep.getSubSteps(), outDataSets);

                        outSteps.add(includeStep);
                    }
                    else if (mapStep.containsKey("instruction"))
                    {
                        final String instruction = String.valueOf(mapStep.get("instruction"));
                        final PlaybookStep step = new PlaybookStep(instruction);
                        initStepLocation(step, fileName, fileContent, instruction);

                        final Object rawActions = mapStep.get("actions");
                        if (rawActions instanceof List)
                        {
                            for (final Object actObj : (List<?>) rawActions)
                            {
                                if (actObj instanceof Map)
                                {
                                    try
                                    {
                                        final Action action = MAPPER.convertValue(actObj, Action.class);
                                        step.getActions().add(action);
                                    }
                                    catch (final Exception e)
                                    {
                                        throw new IllegalArgumentException("Failed to parse action in step: " + instruction, e);
                                    }
                                }
                            }
                        }
                        outSteps.add(step);
                    }
                    else
                    {
                        throw new IllegalArgumentException("Invalid playbook step format in file: " + fileName 
                            + ". Expected string step, 'include' map, or 'instruction' map, but found map keys: " + mapStep.keySet());
                    }
                }
                else
                {
                    throw new IllegalArgumentException("Invalid playbook step item type in file: " + fileName 
                        + ". Expected string or map, but found: " + (stepItem != null ? stepItem.getClass().getName() : "null"));
                }
            }
        }
        else if (rawSteps instanceof String)
        {
            final String[] lines = ((String) rawSteps).split("\\r?\\n");
            for (final String line : lines)
            {
                final String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#"))
                {
                    continue;
                }
                if (trimmed.startsWith("_include:") || trimmed.startsWith("include:"))
                {
                    final int colonIdx = trimmed.indexOf(':');
                    final String includeRelativePath = trimmed.substring(colonIdx + 1).trim();
                    final String resolvedIdentifier = manager.resolveInclude(identifier, includeRelativePath);
                    
                    final PlaybookStep includeStep = new PlaybookStep("_include: " + includeRelativePath);
                    initStepLocation(includeStep, fileName, fileContent, line);
                    parseRecursive(resolvedIdentifier, manager, activeStack, includeStep.getSubSteps(), outDataSets);
                    outSteps.add(includeStep);
                }
                else
                {
                    final PlaybookStep step = new PlaybookStep(trimmed);
                    initStepLocation(step, fileName, fileContent, line);
                    outSteps.add(step);
                }
            }
        }
    }

    /**
     * Helper to initialize the step's source file and approximate line number.
     */
    private void initStepLocation(final PlaybookStep step, final String fileName, final String fileContent, final String searchStr)
    {
        step.setSourceFile(fileName);
        if (fileContent != null && searchStr != null && !searchStr.isEmpty())
        {
            final int idx = fileContent.indexOf(searchStr);
            if (idx != -1)
            {
                int lineCount = 1;
                for (int i = 0; i < idx; i++)
                {
                    if (fileContent.charAt(i) == '\n')
                    {
                        lineCount++;
                    }
                }
                step.setLineNumber(lineCount);
            }
        }
    }

    /**
     * Checks if a key represents a sensitive parameter based on case-insensitive matches.
     *
     * @param key the variable parameter key
     * @return true if the key contains sensitive terms, false otherwise
     */
    private boolean isSensitiveKey(final String key)
    {
        if (key == null)
        {
            return false;
        }
        final String lower = key.toLowerCase();
        return lower.contains("password") 
            || lower.contains("token") 
            || lower.contains("secret") 
            || lower.contains("private") 
            || lower.contains("sensitive");
    }

    private Playbook parseAndMerge(
        final String jsonIdentifier,
        final String yamlIdentifier,
        final PlaybookResourceManager manager) throws IOException
    {
        // 1. Parse the YAML playbook
        final Playbook yamlPlaybook = parse(yamlIdentifier, manager);
        final List<PlaybookStep> yamlSteps = yamlPlaybook.getSteps();

        // 2. Parse the JSON actions
        final List<Action> actions;
        try (final InputStream in = manager.read(jsonIdentifier))
        {
            if (in == null)
            {
                return yamlPlaybook;
            }
            final byte[] bytes = in.readAllBytes();
            final String content = new String(bytes, StandardCharsets.UTF_8).trim();
            final ObjectMapper mapper = new ObjectMapper();
            if (content.startsWith("["))
            {
                try
                {
                    final List<PlaybookStep> jsonSteps = mapper.readValue(content, new TypeReference<List<PlaybookStep>>(){});
                    return new Playbook(jsonSteps, yamlPlaybook.getDataSets(), yamlPlaybook.getPromptAddons());
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                    // Fall back to legacy merge logic
                }
            }
            actions = mapper.readValue(content, new TypeReference<List<Action>>(){});
        }

        // 3. Merge actions into YAML steps sequentially
        int yamlIndex = 0;
        for (final Action action : actions)
        {
            final String stepDesc = (action.getStepInstruction() != null && !action.getStepInstruction().trim().isEmpty())
                ? action.getStepInstruction()
                : action.getDescription();

            // Find the matching YAML step sequentially starting from the current index
            int foundIndex = -1;
            for (int i = yamlIndex; i < yamlSteps.size(); i++)
            {
                final PlaybookStep yamlStep = yamlSteps.get(i);
                if (yamlStep.getInstruction().trim().equalsIgnoreCase(stepDesc.trim()))
                {
                    foundIndex = i;
                    break;
                }
            }

            // If not found from current pointer, scan from the beginning as fallback
            if (foundIndex == -1)
            {
                for (int i = 0; i < yamlSteps.size(); i++)
                {
                    final PlaybookStep yamlStep = yamlSteps.get(i);
                    if (yamlStep.getInstruction().trim().equalsIgnoreCase(stepDesc.trim()))
                    {
                        foundIndex = i;
                        break;
                    }
                }
            }

            if (foundIndex != -1)
            {
                final PlaybookStep yamlStep = yamlSteps.get(foundIndex);
                yamlStep.getActions().add(action);
                if (action.getStepScreenshotHash() != null && !action.getStepScreenshotHash().isEmpty())
                {
                    yamlStep.setScreenshotHash(action.getStepScreenshotHash());
                }
                // Advance the pointer to the next step
                yamlIndex = foundIndex;
            }
            else
            {
                LOG.warn("No matching step found in YAML for JSON action instruction: {}", stepDesc);
            }
        }

        return yamlPlaybook;
    }
}
