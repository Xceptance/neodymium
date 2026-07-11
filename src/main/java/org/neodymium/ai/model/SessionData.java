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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
     * Constructs a SessionData instance with a static dataset.
     * The input map is defensively copied and wrapped as unmodifiable.
     *
     * @param staticData the static dataset parameter map
     */
    public SessionData(final Map<String, DataEntry> staticData)
    {
        this.staticData = Collections.unmodifiableMap(new HashMap<>(staticData));
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
        // 2. Fallback to static data
        return this.staticData.get(key);
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
}
