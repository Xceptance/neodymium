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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.yaml.snakeyaml.Yaml;

/**
 * Concrete implementation of {@link PlaybookParser} that parses playbooks
 * recursively from YAML files, resolving nested inclusions and checking for cycles.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class YamlPlaybookParser implements PlaybookParser
{
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
        final LinkedHashSet<String> activeStack = new LinkedHashSet<>();
        final List<PlaybookStep> steps = new ArrayList<>();
        final List<Map<String, SessionData.DataEntry>> dataSets = new ArrayList<>();

        parseRecursive(identifier, manager, activeStack, steps, dataSets);

        return new Playbook(steps, dataSets);
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

            final Yaml yaml = new Yaml();
            final Map<String, Object> loadedMap = yaml.load(in);

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

            // 3. Parse steps ('steps')
            final Object rawSteps = loadedMap.get("steps");
            if (rawSteps instanceof List)
            {
                for (final Object stepItem : (List<?>) rawSteps)
                {
                    if (stepItem instanceof String)
                    {
                        outSteps.add(new PlaybookStep((String) stepItem));
                    }
                    else if (stepItem instanceof Map)
                    {
                        final Map<?, ?> mapStep = (Map<?, ?>) stepItem;
                        if (mapStep.containsKey("include"))
                        {
                            final String includeRelativePath = String.valueOf(mapStep.get("include"));
                            final String resolvedIdentifier = manager.resolveInclude(identifier, includeRelativePath);

                            // Construct the composite parent inclusion step
                            final PlaybookStep includeStep = new PlaybookStep("include: " + includeRelativePath);
                            
                            // Recursively parse the included file, writing output sub-steps into this composite parent
                            parseRecursive(resolvedIdentifier, manager, activeStack, includeStep.getSubSteps(), outDataSets);

                            outSteps.add(includeStep);
                        }
                    }
                }
            }
        }
        finally
        {
            // Pop the identifier off the stack once its children are fully parsed
            activeStack.remove(identifier);
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
}
