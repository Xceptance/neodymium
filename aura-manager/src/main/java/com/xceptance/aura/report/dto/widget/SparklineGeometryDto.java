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
 * Data transfer object encapsulating pre-calculated SVG paths and endpoint coordinates
 * for rendering high-performance sparkline micro-charts on dashboard KPI widgets.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class SparklineGeometryDto
{
    private final String strokePath;

    private final String areaPath;

    private final double lastX;

    private final double lastY;

    private final boolean hasEndpoint;

    public SparklineGeometryDto(
        final String strokePath,
        final String areaPath,
        final double lastX,
        final double lastY,
        final boolean hasEndpoint)
    {
        this.strokePath = (strokePath != null) ? strokePath : "";
        this.areaPath = (areaPath != null) ? areaPath : "";
        this.lastX = lastX;
        this.lastY = lastY;
        this.hasEndpoint = hasEndpoint;
    }

    public String getStrokePath()
    {
        return strokePath;
    }

    public String getAreaPath()
    {
        return areaPath;
    }

    public double getLastX()
    {
        return lastX;
    }

    public double getLastY()
    {
        return lastY;
    }

    public boolean isHasEndpoint()
    {
        return hasEndpoint;
    }
}
