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

/**
 * Data transfer object encapsulating a single visual layer or legend chip in a trend graph,
 * such as a status stacked area slice (passed, fixed, known fail, unknown fail, ignored)
 * or a metric category legend indicator.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class WidgetTrendLayerDto
{
    private final String name;
    private final String color;
    private final String svgAreaPath;
    private final String svgLinePath;
    private final int count;
    private final String formattedValue;
    private final String patternUrl;
    private final String patternClass;
    private final int peakCount;
    private final String rawValuesCsv;

    public WidgetTrendLayerDto(
        final String name,
        final String color,
        final String svgAreaPath,
        final int count,
        final String formattedValue)
    {
        this(name, color, svgAreaPath, "", count, formattedValue, null, null, count, "");
    }

    public WidgetTrendLayerDto(
        final String name,
        final String color,
        final String svgAreaPath,
        final String svgLinePath,
        final int count,
        final String formattedValue)
    {
        this(name, color, svgAreaPath, svgLinePath, count, formattedValue, null, null, count, "");
    }

    public WidgetTrendLayerDto(
        final String name,
        final String color,
        final String svgAreaPath,
        final int count,
        final String formattedValue,
        final String patternUrl,
        final String patternClass)
    {
        this(name, color, svgAreaPath, "", count, formattedValue, patternUrl, patternClass, count, "");
    }

    public WidgetTrendLayerDto(
        final String name,
        final String color,
        final String svgAreaPath,
        final String svgLinePath,
        final int count,
        final String formattedValue,
        final String patternUrl,
        final String patternClass)
    {
        this(name, color, svgAreaPath, svgLinePath, count, formattedValue, patternUrl, patternClass, count, "");
    }

    public WidgetTrendLayerDto(
        final String name,
        final String color,
        final String svgAreaPath,
        final String svgLinePath,
        final int count,
        final String formattedValue,
        final String patternUrl,
        final String patternClass,
        final int peakCount,
        final String rawValuesCsv)
    {
        this.name = (name != null) ? name : "";
        this.color = (color != null) ? color : "#3b82f6";
        this.svgAreaPath = (svgAreaPath != null) ? svgAreaPath : "";
        this.svgLinePath = (svgLinePath != null) ? svgLinePath : "";
        this.count = Math.max(0, count);
        this.formattedValue = (formattedValue != null) ? formattedValue : "";
        this.patternUrl = (patternUrl != null) ? patternUrl : "";
        this.patternClass = (patternClass != null) ? patternClass : "";
        this.peakCount = Math.max(0, peakCount);
        this.rawValuesCsv = (rawValuesCsv != null) ? rawValuesCsv : "";
    }

    public String getName()
    {
        return name;
    }

    public String getColor()
    {
        return color;
    }

    public String getSvgAreaPath()
    {
        return svgAreaPath;
    }

    public String getSvgLinePath()
    {
        return svgLinePath;
    }

    public int getCount()
    {
        return count;
    }

    public String getFormattedValue()
    {
        return formattedValue;
    }

    public String getPatternUrl()
    {
        return patternUrl;
    }

    public String getPatternClass()
    {
        return patternClass;
    }

    public boolean hasPattern()
    {
        return !patternUrl.isBlank();
    }

    public boolean hasPath()
    {
        return !svgAreaPath.isBlank();
    }

    public boolean hasLinePath()
    {
        return !svgLinePath.isBlank();
    }

    public int getPeakCount()
    {
        return peakCount;
    }

    public String getRawValuesCsv()
    {
        return rawValuesCsv;
    }
}
