/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.aura.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object representing a scanned YAML test file and its associated datasets.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class YamlFileDto
{
    public final String file;
    public final List<DatasetDto> datasets;
    public final boolean hasError;
    public final String errorMessage;
    public final boolean hasPlaybook;
    /** Aggregated tags combining file-level tags and all dataset-level tags. */
    public final Map<String, List<String>> tags;
    /** File-level (root) tags declared directly on the YAML test file, excluding dataset-only tags. */
    public final Map<String, List<String>> rootTags;

    public YamlFileDto(final String file, final List<DatasetDto> datasets)
    {
        this(file, datasets, false, null, false, Collections.emptyMap(), Collections.emptyMap());
    }

    public YamlFileDto(final String file, final List<DatasetDto> datasets, final boolean hasError, final String errorMessage)
    {
        this(file, datasets, hasError, errorMessage, false, Collections.emptyMap(), Collections.emptyMap());
    }

    public YamlFileDto(final String file, final List<DatasetDto> datasets, final boolean hasError, final String errorMessage, final boolean hasPlaybook)
    {
        this(file, datasets, hasError, errorMessage, hasPlaybook, Collections.emptyMap(), Collections.emptyMap());
    }

    public YamlFileDto(final String file, final List<DatasetDto> datasets, final boolean hasError, final String errorMessage, final boolean hasPlaybook, final Map<String, List<String>> tags)
    {
        this(file, datasets, hasError, errorMessage, hasPlaybook, tags, tags);
    }

    public YamlFileDto(final String file, final List<DatasetDto> datasets, final boolean hasError, final String errorMessage, final boolean hasPlaybook, final Map<String, List<String>> tags, final Map<String, List<String>> rootTags)
    {
        this.file = file;
        this.datasets = datasets;
        this.hasError = hasError;
        this.errorMessage = errorMessage;
        this.hasPlaybook = hasPlaybook;
        this.tags = tags != null ? Collections.unmodifiableMap(new LinkedHashMap<>(tags)) : Collections.emptyMap();
        this.rootTags = rootTags != null ? Collections.unmodifiableMap(new LinkedHashMap<>(rootTags)) : Collections.emptyMap();
    }

    public Map<String, List<String>> getTags()
    {
        return tags;
    }

    /**
     * Returns the file-level (root) tags declared directly on the YAML file.
     *
     * @return unmodifiable map of root tags
     */
    public Map<String, List<String>> getRootTags()
    {
        return rootTags;
    }

    /**
     * Returns tags present on this file's datasets that are not already declared in {@link #rootTags}.
     *
     * @return unmodifiable map of dataset-only tag keys to their unique values
     */
    public Map<String, List<String>> getDatasetOnlyTags()
    {
        if (tags.isEmpty())
        {
            return Collections.emptyMap();
        }
        final Map<String, List<String>> result = new LinkedHashMap<>();
        for (final Map.Entry<String, List<String>> entry : tags.entrySet())
        {
            final String key = entry.getKey();
            final List<String> allValues = entry.getValue();
            if (allValues == null || allValues.isEmpty())
            {
                continue;
            }
            final List<String> rootValues = rootTags.get(key);
            final List<String> extraValues = new ArrayList<>();
            for (final String val : allValues)
            {
                if (rootValues == null || !rootValues.contains(val))
                {
                    extraValues.add(val);
                }
            }
            if (!extraValues.isEmpty())
            {
                result.put(key, Collections.unmodifiableList(extraValues));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns the total number of unique tag key-value entries across file and datasets.
     *
     * @return total tag value count
     */
    public int getTotalTagCount()
    {
        return countTagValues(tags);
    }

    /**
     * Returns the number of unique dataset-only tag key-value entries not already present in {@link #rootTags}.
     *
     * @return dataset-only tag value count
     */
    public int getDatasetOnlyTagCount()
    {
        return countTagValues(getDatasetOnlyTags());
    }

    private static int countTagValues(final Map<String, List<String>> tagMap)
    {
        int count = 0;
        for (final List<String> values : tagMap.values())
        {
            if (values != null)
            {
                count += values.size();
            }
        }
        return count;
    }

    public boolean isHasPlaybook()
    {
        return hasPlaybook;
    }

    public boolean isHasError()
    {
        return hasError;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public String getFileName()
    {
        if (file == null)
        {
            return "";
        }
        final int lastSlash = file.lastIndexOf('/');
        return lastSlash >= 0 ? file.substring(lastSlash + 1) : file;
    }

    public String getFileNameWithoutExtension()
    {
        final String name = getFileName();
        final int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }
}
