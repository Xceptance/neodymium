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
package org.neodymium.ai.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Layered data holder providing clean static/dynamic variables separation,
 * snapshot mapping, and sensitive value guarding.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SessionData
{
    /**
     * Represents a single variable entry holding a value and sensitivity flag.
     *
     * @param value the raw object value of the variable
     * @param sensitive true if the value is sensitive (e.g. credentials) and must be masked, false otherwise
     */
    public static record DataEntry(Object value, boolean sensitive) {}

    /**
     * The immutable static dataset injected at session startup (defensively copied).
     */
    private final Map<String, DataEntry> staticData;

    /**
     * The mutable dynamic data layer for storing extracted variables and runtime parameters.
     */
    private final Map<String, DataEntry> dynamicData = new ConcurrentHashMap<>();
    
    /**
     * History mapping step index to a snapshot copy of the dynamic data at the start of that step.
     */
    private final Map<Integer, Map<String, DataEntry>> dynamicHistory = new ConcurrentHashMap<>();

    /**
     * Constructs a default empty SessionData instance.
     */
    public SessionData()
    {
        this(Collections.emptyMap());
    }

    /**
     * Constructs a SessionData instance with a static dataset.
     * The input map is defensively copied and wrapped as unmodifiable.
     *
     * @param staticData the static dataset parameter map
     */
    public SessionData(final Map<String, DataEntry> staticData)
    {
        this.staticData = staticData != null ? Collections.unmodifiableMap(new HashMap<>(staticData)) : Collections.emptyMap();
    }

    /**
     * Sets a non-sensitive variable in the dynamic dataset.
     *
     * @param key the variable key name
     * @param value the variable object value
     */
    public void set(final String key, final Object value)
    {
        putDynamic(key, value, false);
    }

    /**
     * Puts a variable into the mutable dynamic data layer.
     *
     * @param key the variable key name
     * @param value the variable object value
     * @param sensitive true if the value is sensitive (e.g. password) and must be masked, false otherwise
     */
    public void putDynamic(final String key, final Object value, final boolean sensitive)
    {
        this.dynamicData.put(key, new DataEntry(value, sensitive));
    }

    /**
     * Returns the variable entry for a key. Checks the dynamic layer first,
     * falling back to the static layer.
     *
     * @param key the variable key name
     * @return the DataEntry associated with the key, or null
     */
    public DataEntry getEntry(final String key)
    {
        // 1. Resolve from dynamic data first
        if (this.dynamicData.containsKey(key))
        {
            return this.dynamicData.get(key);
        }
        final DataEntry dynamicNested = resolveNestedPath(this.dynamicData, key);
        if (dynamicNested != null)
        {
            return dynamicNested;
        }

        // 2. Check Neodymium test data properties (runtime code overrides via Neodymium.getData().put)
        try
        {
            if (org.neodymium.util.Neodymium.getData() != null && org.neodymium.util.Neodymium.getData().exists(key))
            {
                final String neoProp = org.neodymium.util.Neodymium.getData().asString(key);
                if (neoProp != null)
                {
                    final DataEntry staticEntry = this.staticData.get(key);
                    if (staticEntry != null && !(staticEntry.value() instanceof String)
                        && neoProp.equals(String.valueOf(staticEntry.value())))
                    {
                        // Fall through to rich object in staticData below
                    }
                    else
                    {
                        return new DataEntry(neoProp, false);
                    }
                }
            }
        }
        catch (final Throwable ignored)
        {
        }
        try
        {
            if (com.xceptance.neodymium.util.Neodymium.getData() != null && com.xceptance.neodymium.util.Neodymium.getData().exists(key))
            {
                final String neoProp = com.xceptance.neodymium.util.Neodymium.getData().asString(key);
                if (neoProp != null)
                {
                    final DataEntry staticEntry = this.staticData.get(key);
                    if (staticEntry != null && !(staticEntry.value() instanceof String)
                        && neoProp.equals(String.valueOf(staticEntry.value())))
                    {
                        // Fall through to rich object in staticData below
                    }
                    else
                    {
                        return new DataEntry(neoProp, false);
                    }
                }
            }
        }
        catch (final Throwable ignored)
        {
        }

        // 3. Fallback to static data (from external data file)
        if (this.staticData.containsKey(key))
        {
            return this.staticData.get(key);
        }
        final DataEntry staticNested = resolveNestedPath(this.staticData, key);
        if (staticNested != null)
        {
            return staticNested;
        }

        // 4. Fallback to System properties
        final String sysProp = System.getProperty(key);
        if (sysProp != null)
        {
            return new DataEntry(sysProp, false);
        }
        // 5. Fallback to Neodymium configuration properties
        try
        {
            final String configProp = org.neodymium.util.Neodymium.configuration().getProperty(key);
            if (configProp != null)
            {
                return new DataEntry(configProp, false);
            }
        }
        catch (final Throwable t)
        {
            // ignore
        }
        return null;
    }

    /**
     * Returns the raw value for a variable key. Checks the dynamic layer first,
     * falling back to the static layer.
     *
     * @param key the variable key name
     * @return the raw object value, or null
     */
    public Object get(final String key)
    {
        final DataEntry entry = getEntry(key);
        return entry != null ? entry.value() : null;
    }

    /**
     * Captures a snapshot of the dynamic data at the start of a step.
     *
     * @param stepIndex the index of the playbook step
     */
    public void captureSnapshot(final int stepIndex)
    {
        this.dynamicHistory.put(stepIndex, new HashMap<>(this.dynamicData));
    }

    /**
     * Rolls back the dynamic data state to a previous step,
     * discarding any dynamic changes made after that step started.
     *
     * @param stepIndex the target playbook step index to rollback to
     */
    public void rollbackToStep(final int stepIndex)
    {
        final Map<String, DataEntry> snapshot = this.dynamicHistory.get(stepIndex);
        if (snapshot != null)
        {
            this.dynamicData.clear();
            this.dynamicData.putAll(snapshot);
        }
        
        // Remove history for steps after the rollback target
        this.dynamicHistory.keySet().removeIf(idx -> idx > stepIndex);
    }

    /**
     * Returns a merged map of static and dynamic entries with raw unmasked values.
     *
     * @return the raw data map
     */
    public Map<String, Object> getAllRawDataMap()
    {
        final Map<String, Object> merged = new HashMap<>();
        this.staticData.forEach((k, v) -> merged.put(k, v.value()));
        this.dynamicData.forEach((k, v) -> merged.put(k, v.value()));
        return merged;
    }

    /**
     * Returns a copy of the merged static and dynamic data map,
     * masking any sensitive values before they are sent to the LLM.
     *
     * @return the guarded data map containing masked values
     */
    public Map<String, Object> getGuardedDataMap()
    {
        final Map<String, Object> merged = new HashMap<>();
        
        // Merge static and dynamic data maps
        final Map<String, DataEntry> allData = new HashMap<>(this.staticData);
        allData.putAll(this.dynamicData);
        
        for (final Map.Entry<String, DataEntry> entry : allData.entrySet())
        {
            if (entry.getValue().sensitive())
            {
                merged.put(entry.getKey(), "[SENSITIVE_VALUE]");
            }
            else
            {
                merged.put(entry.getKey(), entry.getValue().value());
            }
        }
        return merged;
    }

    /**
     * Retrieves a map containing all raw sensitive variables and their actual secret values.
     * Used strictly by sanitizers to identify and mask secrets in outbound payloads.
     *
     * @return the map of raw secret values mapped to their variable keys
     */
    public Map<String, String> getRawSensitiveData()
    {
        final Map<String, String> sensitiveMap = new HashMap<>();
        
        for (final Map.Entry<String, DataEntry> entry : this.staticData.entrySet())
        {
            if (entry.getValue().sensitive() && entry.getValue().value() != null)
            {
                sensitiveMap.put(entry.getKey(), String.valueOf(entry.getValue().value()));
            }
        }
        
        for (final Map.Entry<String, DataEntry> entry : this.dynamicData.entrySet())
        {
            if (entry.getValue().sensitive() && entry.getValue().value() != null)
            {
                sensitiveMap.put(entry.getKey(), String.valueOf(entry.getValue().value()));
            }
        }
        
        return sensitiveMap;
    }

    private static final java.util.regex.Pattern VARIABLE_PATTERN = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Resolves variable placeholders in the format "${variableName}" in a template string
     * using the values stored in this SessionData container. Supports nested resolution.
     *
     * @param template the template string containing placeholders
     * @return the resolved string with placeholders replaced by actual values
     */
    public String resolveVariables(final String template)
    {
        return resolveVariables(template, 0);
    }

    private String resolveVariables(final String template, final int depth)
    {
        if (depth > 10 || template == null || template.isEmpty())
        {
            return template;
        }

        final java.util.regex.Matcher matcher = VARIABLE_PATTERN.matcher(template);
        final StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        boolean replaced = false;

        while (matcher.find())
        {
            sb.append(template, lastEnd, matcher.start());
            final String placeholderKey = matcher.group(1);
            final Object value = this.get(placeholderKey);
            if (value == null)
            {
                throw new IllegalArgumentException(
                    "Unresolvable variable placeholder '${" + placeholderKey + "}' in template: \"" + template + "\""
                );
            }
            else
            {
                sb.append(value);
                replaced = true;
            }
            lastEnd = matcher.end();
        }
        sb.append(template.substring(lastEnd));

        final String result = sb.toString();
        if (replaced && result.contains("${"))
        {
            return resolveVariables(result, depth + 1);
        }
        return result;
    }

    /**
     * Extracts all variable placeholder keys present in the given template string.
     *
     * @param template the template string possibly containing ${var} placeholders
     * @return the set of variable placeholder names found in the template
     */
    public Set<String> extractVariableNames(final String template)
    {
        if (template == null || template.isEmpty())
        {
            return Collections.emptySet();
        }
        final Set<String> names = new LinkedHashSet<>();
        final Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find())
        {
            names.add(matcher.group(1));
        }
        return names;
    }

    /**
     * Resolves and extracts the key-value mappings for all placeholders present in the given template string.
     *
     * @param template the template string
     * @return map of placeholder key to resolved string value for all placeholders in the template
     */
    public Map<String, String> getVariablesUsedIn(final String template)
    {
        if (template == null || template.isEmpty())
        {
            return Collections.emptyMap();
        }
        final Set<String> keys = extractVariableNames(template);
        if (keys.isEmpty())
        {
            return Collections.emptyMap();
        }
        final Map<String, String> used = new LinkedHashMap<>();
        for (final String key : keys)
        {
            final Object val = this.get(key);
            if (val != null)
            {
                used.put(key, String.valueOf(val));
            }
        }
        return used;
    }

    /**
     * Returns a map containing all raw variables (static and dynamic) and their current values.
     * Used for sanitizing and parameterizing executed actions.
     *
     * @return the map of variable values mapped to their keys
     */
    public Map<String, String> getAllVariables()
    {
        final Map<String, String> varMap = new java.util.HashMap<>();
        
        // 1. Neodymium configuration properties
        try
        {
            if (org.neodymium.util.Neodymium.configuration() instanceof org.aeonbits.owner.Accessible acc)
            {
                for (final String propName : acc.propertyNames())
                {
                    final String val = acc.getProperty(propName, null);
                    if (val != null && !val.isEmpty())
                    {
                        varMap.put(propName, val);
                    }
                }
            }
        }
        catch (final Throwable ignored)
        {
        }

        // 2. Neodymium test data properties
        try
        {
            if (org.neodymium.util.Neodymium.getData() != null)
            {
                for (final Map.Entry<String, String> entry : org.neodymium.util.Neodymium.getData().entrySet())
                {
                    if (entry.getValue() != null)
                    {
                        varMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        catch (final Throwable ignored)
        {
        }

        // 3. System properties
        try
        {
            final java.util.Properties sysProps = System.getProperties();
            if (sysProps != null)
            {
                for (final String key : sysProps.stringPropertyNames())
                {
                    final String val = sysProps.getProperty(key);
                    if (val != null && !key.startsWith("java.") && !key.startsWith("sun.") && !key.startsWith("user.") && !key.startsWith("path.") && !key.startsWith("file.") && !key.startsWith("line."))
                    {
                        varMap.put(key, val);
                    }
                }
            }
        }
        catch (final Throwable ignored)
        {
        }

        // 4. Static dataset layer
        for (final Map.Entry<String, DataEntry> entry : this.staticData.entrySet())
        {
            if (entry.getValue() != null && entry.getValue().value() != null)
            {
                flattenObject(entry.getKey(), entry.getValue().value(), varMap);
            }
        }
        
        // 5. Dynamic data layer
        for (final Map.Entry<String, DataEntry> entry : this.dynamicData.entrySet())
        {
            if (entry.getValue() != null && entry.getValue().value() != null)
            {
                flattenObject(entry.getKey(), entry.getValue().value(), varMap);
            }
        }

        // Recursively resolve nested variable placeholders in varMap values
        for (int pass = 0; pass < 5; pass++)
        {
            boolean changed = false;
            for (final Map.Entry<String, String> entry : new java.util.HashMap<>(varMap).entrySet())
            {
                final String val = entry.getValue();
                if (val != null && val.contains("${"))
                {
                    final String resolved = resolveVariables(val);
                    if (!val.equals(resolved))
                    {
                        varMap.put(entry.getKey(), resolved);
                        changed = true;
                    }
                }
            }
            if (!changed)
            {
                break;
            }
        }
        
        return varMap;
    }

    /**
     * Returns a map containing only the local static dataset variables and Neodymium test data.
     *
     * @return map of local dataset variable keys and string values
     */
    public Map<String, String> getStaticDataVariables()
    {
        final Map<String, String> map = new HashMap<>();
        try
        {
            if (org.neodymium.util.Neodymium.getData() != null)
            {
                for (final Map.Entry<String, String> entry : org.neodymium.util.Neodymium.getData().entrySet())
                {
                    if (entry.getValue() != null)
                    {
                        map.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        catch (final Throwable ignored)
        {
        }

        for (final Map.Entry<String, DataEntry> entry : this.staticData.entrySet())
        {
            if (entry.getValue() != null && entry.getValue().value() != null)
            {
                flattenObject(entry.getKey(), entry.getValue().value(), map);
            }
        }
        return map;
    }

    /**
     * Resolves a dot-delimited property path against a dataset layer.
     *
     * @param layer the data layer map
     * @param key the dot-delimited property key
     * @return the resolved DataEntry, or null if unresolvable
     */
    private DataEntry resolveNestedPath(final Map<String, DataEntry> layer, final String key)
    {
        if (layer == null || key == null || !key.contains("."))
        {
            return null;
        }

        final int firstDot = key.indexOf('.');
        final String rootKey = key.substring(0, firstDot);
        final String remainingPath = key.substring(firstDot + 1);

        final DataEntry rootEntry = layer.get(rootKey);
        if (rootEntry == null || rootEntry.value() == null)
        {
            return null;
        }

        final Object leafValue = navigatePath(rootEntry.value(), remainingPath);
        if (leafValue != null)
        {
            return new DataEntry(leafValue, rootEntry.sensitive());
        }
        return null;
    }

    /**
     * Traverses a nested object or Map structure following a dot-delimited path.
     *
     * @param current the current object in the navigation hierarchy
     * @param path the remaining dot-delimited path
     * @return the leaf object value, or null if path segment is not found
     */
    private Object navigatePath(final Object current, final String path)
    {
        if (current == null || path == null || path.isEmpty())
        {
            return current;
        }

        final int dotIdx = path.indexOf('.');
        final String currentSegment = dotIdx == -1 ? path : path.substring(0, dotIdx);
        final String nextPath = dotIdx == -1 ? null : path.substring(dotIdx + 1);

        if (current instanceof Map<?, ?> map)
        {
            Object nextVal = map.get(currentSegment);
            if (nextVal == null)
            {
                for (final Map.Entry<?, ?> entry : map.entrySet())
                {
                    if (String.valueOf(entry.getKey()).equalsIgnoreCase(currentSegment))
                    {
                        nextVal = entry.getValue();
                        break;
                    }
                }
            }
            if (nextVal != null)
            {
                return nextPath == null ? nextVal : navigatePath(nextVal, nextPath);
            }
        }
        return null;
    }

    /**
     * Recursively flattens nested maps into dot-separated string key-value pairs.
     *
     * @param prefix current property path prefix
     * @param value object value to flatten
     * @param outMap target map receiving flattened key-value entries
     */
    private void flattenObject(final String prefix, final Object value, final Map<String, String> outMap)
    {
        if (value == null)
        {
            return;
        }
        if (value instanceof Map<?, ?> map)
        {
            for (final Map.Entry<?, ?> entry : map.entrySet())
            {
                final String childKey = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
                flattenObject(childKey, entry.getValue(), outMap);
            }
        }
        else
        {
            outMap.put(prefix, String.valueOf(value));
        }
    }
}
