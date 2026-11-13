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

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/**
 * Enumeration representing how values on Perlenkette beads should be formatted.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public enum WidgetValueFormat
{
    /**
     * Value formatted as percentage (e.g. "95%").
     */
    PERCENT("Percents (%)", "percent"),

    /**
     * Value formatted as absolute number (e.g. "42", "$0.15").
     */
    ABSOLUTE("Absolute Number", "tag");

    private final String displayName;
    private final String icon;

    WidgetValueFormat(final String displayName, final String icon)
    {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getIcon()
    {
        return icon;
    }

    @JsonCreator
    public static WidgetValueFormat fromString(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return PERCENT;
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (final WidgetValueFormat f : values())
        {
            if (f.name().equals(normalized))
            {
                return f;
            }
        }
        return PERCENT;
    }
}
