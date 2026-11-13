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
package com.xceptance.aura.report.dto.widget;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Immutable representation of an individual active filter chip displayed on a dashboard widget card.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class WidgetFilterChipDto
{
    private final String category;
    private final String icon;
    private final String label;
    private final boolean excluded;

    @JsonCreator
    public WidgetFilterChipDto(
        @JsonProperty("category") final String category,
        @JsonProperty("icon") final String icon,
        @JsonProperty("label") final String label,
        @JsonProperty("excluded") final boolean excluded)
    {
        this.category = (category != null) ? category : "tag";
        this.icon = (icon != null) ? icon : "sell";
        this.label = (label != null) ? label : "";
        this.excluded = excluded;
    }

    public String getCategory()
    {
        return category;
    }

    public String getIcon()
    {
        return icon;
    }

    public String getLabel()
    {
        return label;
    }

    public boolean isExcluded()
    {
        return excluded;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final WidgetFilterChipDto that = (WidgetFilterChipDto) o;
        return excluded == that.excluded
            && Objects.equals(category, that.category)
            && Objects.equals(icon, that.icon)
            && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(category, icon, label, excluded);
    }

    @Override
    public String toString()
    {
        return "WidgetFilterChipDto{" +
            "category='" + category + '\'' +
            ", icon='" + icon + '\'' +
            ", label='" + label + '\'' +
            ", excluded=" + excluded +
            '}';
    }
}
