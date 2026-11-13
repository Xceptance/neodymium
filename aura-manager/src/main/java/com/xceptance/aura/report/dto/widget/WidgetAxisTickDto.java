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
 * Data transfer object encapsulating a single tick mark, label, and gridline on a chart axis.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class WidgetAxisTickDto
{
    private final double coordinate;
    private final String label;
    private final boolean showGridLine;
    private final String tooltip;

    public WidgetAxisTickDto(final double coordinate, final String label, final boolean showGridLine)
    {
        this(coordinate, label, showGridLine, label);
    }

    public WidgetAxisTickDto(final double coordinate, final String label, final boolean showGridLine, final String tooltip)
    {
        this.coordinate = coordinate;
        this.label = (label != null) ? label : "";
        this.showGridLine = showGridLine;
        this.tooltip = (tooltip != null && !tooltip.isBlank()) ? tooltip : this.label;
    }

    public double getCoordinate()
    {
        return coordinate;
    }

    public String getLabel()
    {
        return label;
    }

    public boolean isShowGridLine()
    {
        return showGridLine;
    }

    public String getTooltip()
    {
        return tooltip;
    }
}
