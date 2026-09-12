/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.executor.probe;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Normalized, driver-agnostic bounding rectangle representing an on-screen element's geometry.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ProbeBoundingRect
{
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    /**
     * Constructs a normalized bounding rectangle.
     *
     * @param x top-left horizontal coordinate
     * @param y top-left vertical coordinate
     * @param width rectangle width
     * @param height rectangle height
     */
    @JsonCreator
    public ProbeBoundingRect(
            @JsonProperty("x") final double x,
            @JsonProperty("y") final double y,
            @JsonProperty("width") final double width,
            @JsonProperty("height") final double height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX()
    {
        return this.x;
    }

    public double getY()
    {
        return this.y;
    }

    public double getWidth()
    {
        return this.width;
    }

    public double getHeight()
    {
        return this.height;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof final ProbeBoundingRect other))
        {
            return false;
        }
        return Double.compare(this.x, other.x) == 0
                && Double.compare(this.y, other.y) == 0
                && Double.compare(this.width, other.width) == 0
                && Double.compare(this.height, other.height) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.x, this.y, this.width, this.height);
    }

    @Override
    public String toString()
    {
        return String.format("[%.0f, %.0f, %.0fx%.0f]", this.x, this.y, this.width, this.height);
    }
}
