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
 * Data transfer object representing a small decorative accent bead along the undulating thread
 * of a String of Pearls (Perlenkette) diagram.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class PerlenketteMinorDotDto
{
    private final double cx;
    private final double cy;
    private final double radius;
    private final String color;

    public PerlenketteMinorDotDto(final double cx, final double cy, final double radius, final String color)
    {
        this.cx = cx;
        this.cy = cy;
        this.radius = radius;
        this.color = (color != null) ? color : "#10b981";
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

    public String getColor()
    {
        return color;
    }
}
