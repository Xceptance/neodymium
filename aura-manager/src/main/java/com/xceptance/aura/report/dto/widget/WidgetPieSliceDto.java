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

import java.util.Locale;

/**
 * Data transfer object encapsulating a slice segment for a dashboard widget pie/donut chart.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class WidgetPieSliceDto
{
    private final String label;
    private final int count;
    private final double percentage;
    private final String formattedPercentage;
    private final String color;
    private final String healedColor;
    private final String dashArray;
    private final String dashOffset;
    private final int healedCount;
    private final int aiCount;
    private final String healedDashArray;
    private final String healedDashOffset;
    private final String aiDashArray;
    private final String aiDashOffset;

    public WidgetPieSliceDto(
        final String label,
        final int count,
        final double percentage,
        final String color,
        final double dashOffsetVal,
        final int healedCount,
        final int aiCount)
    {
        this(label, count, percentage, color, resolveHealedColor(label, color), dashOffsetVal, healedCount, aiCount, 0.0, 0.0);
    }

    public WidgetPieSliceDto(
        final String label,
        final int count,
        final double percentage,
        final String color,
        final double dashOffsetVal,
        final int healedCount,
        final int aiCount,
        final double healedPct,
        final double aiPct)
    {
        this(label, count, percentage, color, resolveHealedColor(label, color), dashOffsetVal, healedCount, aiCount, healedPct, aiPct);
    }

    public WidgetPieSliceDto(
        final String label,
        final int count,
        final double percentage,
        final String color,
        final String healedColor,
        final double dashOffsetVal,
        final int healedCount,
        final int aiCount,
        final double healedPct,
        final double aiPct)
    {
        this.label = (label != null) ? label : "";
        this.count = count;
        this.percentage = percentage;
        this.formattedPercentage = String.format(Locale.ROOT, "%.1f%%", percentage);
        this.color = (color != null) ? color : "#64748b";
        this.healedColor = (healedColor != null && !healedColor.isBlank())
            ? healedColor
            : resolveHealedColor(label, this.color);
        this.dashArray = String.format(Locale.ROOT, "%.2f %.2f", percentage, 100.0 - percentage);
        this.dashOffset = String.format(Locale.ROOT, "%.2f", dashOffsetVal);
        this.healedCount = healedCount;
        this.aiCount = aiCount;

        if (healedPct > 0.0)
        {
            this.healedDashArray = String.format(Locale.ROOT, "%.2f %.2f", healedPct, 100.0 - healedPct);
            final double offset = dashOffsetVal - (percentage - healedPct - aiPct);
            this.healedDashOffset = String.format(Locale.ROOT, "%.2f", offset);
        }
        else
        {
            this.healedDashArray = "0 100";
            this.healedDashOffset = "25";
        }

        if (aiPct > 0.0)
        {
            this.aiDashArray = String.format(Locale.ROOT, "%.2f %.2f", aiPct, 100.0 - aiPct);
            final double offset = dashOffsetVal - (percentage - aiPct);
            this.aiDashOffset = String.format(Locale.ROOT, "%.2f", offset);
        }
        else
        {
            this.aiDashArray = "0 100";
            this.aiDashOffset = "25";
        }
    }

    private static String resolveHealedColor(final String label, final String fallbackColor)
    {
        final String l = (label != null) ? label.toLowerCase(Locale.ROOT) : "";
        if (l.contains("pass") || l.contains("success"))
        {
            return "var(--status-pass-healed, #34d399)";
        }
        if (l.contains("fixed"))
        {
            return "var(--status-fixed-healed, #60a5fa)";
        }
        if (l.contains("unknown"))
        {
            return "var(--status-unknown-healed, #f87171)";
        }
        if (l.contains("known"))
        {
            return "var(--status-known-healed, #fb923c)";
        }
        if (l.contains("fail"))
        {
            return "var(--status-unknown-healed, #f87171)";
        }
        return (fallbackColor != null && !fallbackColor.isBlank()) ? fallbackColor : "#34d399";
    }

    public String getLabel()
    {
        return label;
    }

    public int getCount()
    {
        return count;
    }

    public String getFormattedCount()
    {
        return String.format(Locale.ROOT, "%,d", count);
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

    public String getHealedColor()
    {
        return healedColor;
    }

    public String getDashArray()
    {
        return dashArray;
    }

    public String getDashOffset()
    {
        return dashOffset;
    }

    public int getHealedCount()
    {
        return healedCount;
    }

    public int getAiCount()
    {
        return aiCount;
    }

    public boolean hasHealed()
    {
        return healedCount > 0;
    }

    public boolean hasAi()
    {
        return aiCount > 0;
    }

    public boolean hasHealedOrAi()
    {
        return healedCount > 0 || aiCount > 0;
    }

    public String getHealedDashArray()
    {
        return healedDashArray;
    }

    public String getHealedDashOffset()
    {
        return healedDashOffset;
    }

    public String getAiDashArray()
    {
        return aiDashArray;
    }

    public String getAiDashOffset()
    {
        return aiDashOffset;
    }
}
