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
package com.xceptance.neodymium.aura.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object representing a dataset of a YAML test file.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class DatasetDto
{
    public final String id;
    public final String label;
    public final boolean hasTestId;
    public final boolean hasPlaybook;
    public final String playbookPath;
    public final Map<String, List<String>> tags;

    public DatasetDto(final String id, final String label, final boolean hasTestId)
    {
        this(id, label, hasTestId, false, null, Collections.emptyMap());
    }

    public DatasetDto(final String id, final String label, final boolean hasTestId, final boolean hasPlaybook, final String playbookPath)
    {
        this(id, label, hasTestId, hasPlaybook, playbookPath, Collections.emptyMap());
    }

    public DatasetDto(final String id, final String label, final boolean hasTestId, final boolean hasPlaybook, final String playbookPath, final Map<String, List<String>> tags)
    {
        this.id = id;
        this.label = label;
        this.hasTestId = hasTestId;
        this.hasPlaybook = hasPlaybook;
        this.playbookPath = playbookPath;
        this.tags = tags != null ? Collections.unmodifiableMap(new LinkedHashMap<>(tags)) : Collections.emptyMap();
    }

    public Map<String, List<String>> getTags()
    {
        return tags;
    }

    public boolean isHasPlaybook()
    {
        return hasPlaybook;
    }

    public String getPlaybookPath()
    {
        return playbookPath;
    }
}
