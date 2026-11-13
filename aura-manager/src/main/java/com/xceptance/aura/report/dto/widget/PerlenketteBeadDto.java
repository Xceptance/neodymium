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

import java.util.Collections;
import java.util.Map;

/**
 * Data transfer object encapsulating a single milestone bead in a Perlenkette diagram.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class PerlenketteBeadDto
{
    private final int index;
    private final String id;
    private final String label;
    private final String timestampLabel;
    private final String valueFormatted;
    private final double percentValue;
    private final String statusClass;
    private final String color;
    private final String linkUrl;
    private final Map<String, String> details;
    private final String innerValue;
    private final String darkColor;
    private final double cx;
    private final double cy;
    private final double radius;

    public PerlenketteBeadDto(
        final int index,
        final String id,
        final String label,
        final String timestampLabel,
        final String valueFormatted,
        final double percentValue,
        final String statusClass,
        final String color,
        final String linkUrl,
        final Map<String, String> details)
    {
        this(index, id, label, timestampLabel, valueFormatted, percentValue, statusClass, color, linkUrl, details,
            extractDefaultInnerValue(valueFormatted, percentValue), computeDefaultDarkColor(color), 0.0, 0.0, 36.0);
    }

    public PerlenketteBeadDto(
        final int index,
        final String id,
        final String label,
        final String timestampLabel,
        final String valueFormatted,
        final double percentValue,
        final String statusClass,
        final String color,
        final String linkUrl,
        final Map<String, String> details,
        final String innerValue,
        final String darkColor,
        final double cx,
        final double cy,
        final double radius)
    {
        this.index = index;
        this.id = (id != null) ? id : "";
        this.label = (label != null) ? label : "";
        this.timestampLabel = (timestampLabel != null) ? timestampLabel : "";
        this.valueFormatted = (valueFormatted != null) ? valueFormatted : "";
        this.percentValue = percentValue;
        this.statusClass = (statusClass != null) ? statusClass : "bead-neutral";
        this.color = (color != null) ? color : "#10b981";
        this.linkUrl = (linkUrl != null) ? linkUrl : "";
        this.details = (details != null) ? Map.copyOf(details) : Collections.emptyMap();
        this.innerValue = (innerValue != null && !innerValue.isBlank()) ? innerValue : extractDefaultInnerValue(valueFormatted, percentValue);
        this.darkColor = (darkColor != null && !darkColor.isBlank()) ? darkColor : computeDefaultDarkColor(this.color);
        this.cx = cx;
        this.cy = cy;
        this.radius = radius > 0.0 ? radius : 36.0;
    }

    private static String extractDefaultInnerValue(final String valueFormatted, final double percentValue)
    {
        if (valueFormatted != null && !valueFormatted.isBlank())
        {
            final String trimmed = valueFormatted.trim();
            if (trimmed.endsWith("%"))
            {
                return trimmed.substring(0, trimmed.length() - 1).trim();
            }
            return trimmed;
        }
        return String.valueOf(Math.round(percentValue));
    }

    private static String computeDefaultDarkColor(final String baseColor)
    {
        if (baseColor == null || !baseColor.startsWith("#") || baseColor.length() < 7)
        {
            return "#15803d";
        }
        try
        {
            final int r = Integer.parseInt(baseColor.substring(1, 3), 16);
            final int g = Integer.parseInt(baseColor.substring(3, 5), 16);
            final int b = Integer.parseInt(baseColor.substring(5, 7), 16);
            final int dr = Math.max(0, (int) (r * 0.72));
            final int dg = Math.max(0, (int) (g * 0.72));
            final int db = Math.max(0, (int) (b * 0.72));
            return String.format("#%02x%02x%02x", dr, dg, db);
        }
        catch (final NumberFormatException ex)
        {
            return "#15803d";
        }
    }

    public int getIndex()
    {
        return index;
    }

    public String getId()
    {
        return id;
    }

    public String getLabel()
    {
        return label;
    }

    public String getTimestampLabel()
    {
        return timestampLabel;
    }

    public String getValueFormatted()
    {
        return valueFormatted;
    }

    public double getPercentValue()
    {
        return percentValue;
    }

    public String getStatusClass()
    {
        return statusClass;
    }

    public String getColor()
    {
        return color;
    }

    public String getLinkUrl()
    {
        return linkUrl;
    }

    public Map<String, String> getDetails()
    {
        return details;
    }

    public String getInnerValue()
    {
        return innerValue;
    }

    public String getDarkColor()
    {
        return darkColor;
    }

    public double getCx()
    {
        return cx;
    }

    public double getCy()
    {
        return cy;
    }

    public double getRadius()
    {
        return radius;
    }

    public double getPillWidth()
    {
        final int len = (label != null) ? label.length() : 0;
        final double textW = len * 7.2;
        return Math.max(radius * 1.9, textW + 16.0);
    }
}

