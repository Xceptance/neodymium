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
package com.xceptance.neodymium.ai.core;

/**
 * Details of a variable lookup resolved during template processing.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class LookupDetails
{
    private final String key;
    private final String resolvedValue;
    private final boolean localized;
    private final String source;

    public LookupDetails(
            final String key,
            final String resolvedValue,
            final boolean localized,
            final String source)
    {
        this.key = key;
        this.resolvedValue = resolvedValue;
        this.localized = localized;
        this.source = source;
    }

    public final String getKey()
    {
        return this.key;
    }

    public final String getResolvedValue()
    {
        return this.resolvedValue;
    }

    public final boolean isLocalized()
    {
        return this.localized;
    }

    public final String getSource()
    {
        return this.source;
    }
}
