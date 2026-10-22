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

import java.util.Locale;

/**
 * Data transfer object representing a proportional ratio item (such as browser or locale distribution).
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class DistributionRatioDto
{
    private final String name;
    private final int count;
    private final double percentage;
    private final String formattedPercentage;
    private final String color;

    public DistributionRatioDto(
        final String name,
        final int count,
        final double percentage,
        final String color)
    {
        this.name = (name != null && !name.isBlank()) ? name : "Default";
        this.count = count;
        this.percentage = percentage;
        this.formattedPercentage = String.format(Locale.ROOT, "%.0f%%", percentage);
        this.color = (color != null && !color.isBlank()) ? color : "var(--status-pass)";
    }

    public String getName()
    {
        return name;
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
}
