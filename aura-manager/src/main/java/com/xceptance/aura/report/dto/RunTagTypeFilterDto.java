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
package com.xceptance.aura.report.dto;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Data transfer object representing a dynamic tag type filter dropdown and its selection state.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class RunTagTypeFilterDto
{
    private final String tagKey;
    private final String displayName;
    private final String paramName;
    private final String excludeParamName;
    private final String dropdownId;
    private final String menuId;
    private final List<String> candidateValues;
    private final Set<String> selectedValues;
    private final Set<String> excludedValues;
    private final boolean active;
    private final boolean noneSelected;
    private final boolean allSelected;

    public RunTagTypeFilterDto(
        final String tagKey,
        final List<String> candidateValues,
        final Set<String> selectedValues)
    {
        this(tagKey, candidateValues, selectedValues, Collections.emptySet());
    }

    public RunTagTypeFilterDto(
        final String tagKey,
        final List<String> candidateValues,
        final Set<String> selectedValues,
        final Set<String> excludedValues)
    {
        final String safeTagKey = (tagKey != null && !tagKey.isBlank()) ? tagKey.trim() : "tag";
        this.tagKey = safeTagKey;

        final String cleanId = safeTagKey.replaceAll("[^a-zA-Z0-9_]", "_");
        final String capKey = safeTagKey.substring(0, 1).toUpperCase(Locale.ROOT) + safeTagKey.substring(1);
        this.displayName = "Tag: " + capKey;
        this.paramName = "runTag_" + cleanId;
        this.excludeParamName = "excludeRunTag_" + cleanId;
        this.dropdownId = "scopeTagDropdown_" + cleanId;
        this.menuId = "scopeTagMenu_" + cleanId;

        this.candidateValues = (candidateValues != null) ? List.copyOf(candidateValues) : Collections.emptyList();
        this.selectedValues = (selectedValues != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(selectedValues)) : Collections.emptySet();
        this.excludedValues = (excludedValues != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(excludedValues)) : Collections.emptySet();

        final boolean hasSelection = !this.selectedValues.isEmpty();
        final boolean hasExclusions = !this.excludedValues.isEmpty();
        this.noneSelected = this.selectedValues.contains("_none_");
        this.active = hasSelection || hasExclusions;
        this.allSelected = (!hasSelection && !hasExclusions)
            || (!this.noneSelected && !this.candidateValues.isEmpty() && this.selectedValues.size() == this.candidateValues.size());
    }

    public String getTagKey()
    {
        return tagKey;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getParamName()
    {
        return paramName;
    }

    public String getExcludeParamName()
    {
        return excludeParamName;
    }

    public String getDropdownId()
    {
        return dropdownId;
    }

    public String getMenuId()
    {
        return menuId;
    }

    public List<String> getCandidateValues()
    {
        return candidateValues;
    }

    public Set<String> getSelectedValues()
    {
        return selectedValues;
    }

    public Set<String> getExcludedValues()
    {
        return excludedValues;
    }

    public boolean isExclusionMode()
    {
        return !excludedValues.isEmpty();
    }

    public int getActiveCount()
    {
        if (isExclusionMode())
        {
            return Math.max(0, candidateValues.size() - excludedValues.size());
        }
        return noneSelected ? 0 : selectedValues.size();
    }

    public boolean isActive()
    {
        return active;
    }

    public boolean isNoneSelected()
    {
        return noneSelected;
    }

    public boolean isAllSelected()
    {
        return allSelected;
    }
}
