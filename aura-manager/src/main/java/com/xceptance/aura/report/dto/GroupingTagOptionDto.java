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

/**
 * Data transfer object representing a dynamic tag option in the grouping toolbar.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class GroupingTagOptionDto
{
    private final String tagKey;
    private final String displayName;
    private final String toggleUrl;
    private final boolean active;

    public GroupingTagOptionDto(
        final String tagKey,
        final String displayName,
        final String toggleUrl,
        final boolean active)
    {
        this.tagKey = tagKey;
        this.displayName = displayName;
        this.toggleUrl = toggleUrl;
        this.active = active;
    }

    public String getTagKey()
    {
        return tagKey;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getToggleUrl()
    {
        return toggleUrl;
    }

    public boolean isActive()
    {
        return active;
    }
}
