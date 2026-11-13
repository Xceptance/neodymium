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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Data transfer object representing an item row inside a list presentation widget.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class WidgetListItemDto
{
    private final String label;
    private final String valueFormatted;
    private final int count;
    private final double percentage;
    private final String formattedPercentage;
    private final String color;
    private final String icon;
    private final int healedCount;
    private final int aiCount;
    private final String badge;
    private final String secondaryLabel;
    private final List<String> tags;
    private final List<WidgetListItemDto> subItems;
    private final String variationKey;
    private final String variationId;
    private final String runId;
    private final String executionId;

    public WidgetListItemDto(
        final String label,
        final String valueFormatted,
        final int count,
        final double percentage,
        final String color,
        final String icon)
    {
        this(label, valueFormatted, count, percentage, color, icon, 0, 0, "");
    }

    public WidgetListItemDto(
        final String label,
        final String valueFormatted,
        final int count,
        final double percentage,
        final String color,
        final String icon,
        final String runId)
    {
        this(label, valueFormatted, count, percentage, color, icon, 0, 0, "", "", Collections.emptyList(), Collections.emptyList(), "", "", runId, "");
    }

    public WidgetListItemDto(
        final String label,
        final String valueFormatted,
        final int count,
        final double percentage,
        final String color,
        final String icon,
        final int healedCount,
        final int aiCount,
        final String badge)
    {
        this(label, valueFormatted, count, percentage, color, icon, healedCount, aiCount, badge, "", Collections.emptyList(), Collections.emptyList());
    }

    public WidgetListItemDto(
        final String label,
        final String valueFormatted,
        final int count,
        final double percentage,
        final String color,
        final String icon,
        final int healedCount,
        final int aiCount,
        final String badge,
        final String secondaryLabel,
        final List<String> tags,
        final List<WidgetListItemDto> subItems)
    {
        this(label, valueFormatted, count, percentage, color, icon, healedCount, aiCount, badge, secondaryLabel, tags, subItems, "", "");
    }

    public WidgetListItemDto(
        final String label,
        final String valueFormatted,
        final int count,
        final double percentage,
        final String color,
        final String icon,
        final int healedCount,
        final int aiCount,
        final String badge,
        final String secondaryLabel,
        final List<String> tags,
        final List<WidgetListItemDto> subItems,
        final String variationKey,
        final String variationId)
    {
        this(label, valueFormatted, count, percentage, color, icon, healedCount, aiCount, badge, secondaryLabel, tags, subItems, variationKey, variationId, "", "");
    }

    public WidgetListItemDto(
        final String label,
        final String valueFormatted,
        final int count,
        final double percentage,
        final String color,
        final String icon,
        final int healedCount,
        final int aiCount,
        final String badge,
        final String secondaryLabel,
        final List<String> tags,
        final List<WidgetListItemDto> subItems,
        final String variationKey,
        final String variationId,
        final String runId,
        final String executionId)
    {
        this.label = (label != null) ? label : "";
        this.valueFormatted = (valueFormatted != null) ? valueFormatted : "";
        this.count = count;
        this.percentage = percentage;
        this.formattedPercentage = String.format(Locale.ROOT, "%.1f%%", percentage);
        this.color = (color != null && !color.isBlank()) ? color : "var(--accent-color)";
        this.icon = (icon != null) ? icon : "";
        this.healedCount = healedCount;
        this.aiCount = aiCount;
        this.badge = (badge != null) ? badge : "";
        this.secondaryLabel = (secondaryLabel != null) ? secondaryLabel : "";
        this.tags = (tags != null && !tags.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(tags))
            : Collections.emptyList();
        this.subItems = (subItems != null && !subItems.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(subItems))
            : Collections.emptyList();
        this.variationKey = (variationKey != null) ? variationKey : "";
        this.variationId = (variationId != null) ? variationId : "";
        this.runId = (runId != null) ? runId.trim() : "";
        this.executionId = (executionId != null) ? executionId.trim() : "";
    }

    public String getLabel()
    {
        return label;
    }

    public String getValueFormatted()
    {
        return valueFormatted;
    }

    public int getCount()
    {
        return count;
    }

    public double getPercentage()
    {
        return percentage;
    }

    public String getFormattedPercentage()
    {
        return formattedPercentage;
    }

    public String getColor()
    {
        return color;
    }

    public String getIcon()
    {
        return icon;
    }

    public int getHealedCount()
    {
        return healedCount;
    }

    public int getAiCount()
    {
        return aiCount;
    }

    public String getBadge()
    {
        return badge;
    }

    public String getSecondaryLabel()
    {
        return secondaryLabel;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public List<WidgetListItemDto> getSubItems()
    {
        return subItems;
    }

    public boolean hasSubItems()
    {
        return !subItems.isEmpty();
    }

    public boolean hasTags()
    {
        return !tags.isEmpty();
    }

    public boolean hasSecondaryLabel()
    {
        return !secondaryLabel.isBlank();
    }

    public boolean hasHealedOrAi()
    {
        return healedCount > 0 || aiCount > 0;
    }

    public String getVariationKey()
    {
        return variationKey;
    }

    public String getVariationId()
    {
        return variationId;
    }

    public String getRunId()
    {
        return runId;
    }

    public String getExecutionId()
    {
        return executionId;
    }

    public boolean hasRunId()
    {
        return runId != null && !runId.isBlank();
    }

    public boolean hasExecutionId()
    {
        return executionId != null && !executionId.isBlank();
    }

    public boolean hasReportUrl()
    {
        return hasRunId();
    }

    public String getReportUrl()
    {
        if (!hasRunId())
        {
            return "";
        }
        if (hasExecutionId())
        {
            return "/run-report?runId=" + runId + "&executionId=" + executionId;
        }
        if (variationId != null && !variationId.isBlank())
        {
            return "/run-report?runId=" + runId + "&variationId=" + URLEncoder.encode(variationId, StandardCharsets.UTF_8);
        }
        return "/run-report?runId=" + runId;
    }
}
