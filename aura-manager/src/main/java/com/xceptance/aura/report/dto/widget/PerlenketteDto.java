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
import java.util.List;

/**
 * Data transfer object encapsulating a complete Perlenkette diagram and its bead sequence.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class PerlenketteDto
{
    private final List<PerlenketteBeadDto> beads;
    private final WidgetValueFormat valueFormat;
    private final double averagePercent;
    private final String trendSummary;
    private final String svgWavePath;
    private final double svgWidth;
    private final double svgHeight;
    private final List<PerlenketteMinorDotDto> minorDots;

    public PerlenketteDto(
        final List<PerlenketteBeadDto> beads,
        final WidgetValueFormat valueFormat,
        final double averagePercent,
        final String trendSummary)
    {
        this(beads, valueFormat, averagePercent, trendSummary, "", 800.0, 180.0, Collections.emptyList());
    }

    public PerlenketteDto(
        final List<PerlenketteBeadDto> beads,
        final WidgetValueFormat valueFormat,
        final double averagePercent,
        final String trendSummary,
        final String svgWavePath,
        final double svgWidth,
        final double svgHeight,
        final List<PerlenketteMinorDotDto> minorDots)
    {
        this.beads = (beads != null) ? List.copyOf(beads) : Collections.emptyList();
        this.valueFormat = (valueFormat != null) ? valueFormat : WidgetValueFormat.PERCENT;
        this.averagePercent = averagePercent;
        this.trendSummary = (trendSummary != null) ? trendSummary : "";
        this.svgWavePath = (svgWavePath != null) ? svgWavePath : "";
        this.svgWidth = svgWidth > 0.0 ? svgWidth : 800.0;
        this.svgHeight = svgHeight > 0.0 ? svgHeight : 180.0;
        this.minorDots = (minorDots != null) ? List.copyOf(minorDots) : Collections.emptyList();
    }

    public List<PerlenketteBeadDto> getBeads()
    {
        return beads;
    }

    public WidgetValueFormat getValueFormat()
    {
        return valueFormat;
    }

    public double getAveragePercent()
    {
        return averagePercent;
    }

    public String getTrendSummary()
    {
        return trendSummary;
    }

    public int getTotalBeads()
    {
        return beads.size();
    }

    public boolean hasBeads()
    {
        return !beads.isEmpty();
    }

    public String getSvgWavePath()
    {
        return svgWavePath;
    }

    public double getSvgWidth()
    {
        return svgWidth;
    }

    public double getSvgHeight()
    {
        return svgHeight;
    }

    public List<PerlenketteMinorDotDto> getMinorDots()
    {
        return minorDots;
    }
}

