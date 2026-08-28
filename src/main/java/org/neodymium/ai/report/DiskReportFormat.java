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
package org.neodymium.ai.report;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration of supported preliminary disk report output formats.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public enum DiskReportFormat
{
    /** Self-contained single-file HTML report with embedded styles, SVG icons, and screenshots */
    HTML("html"),

    /** GitHub-flavored Markdown summary report */
    MARKDOWN("md"),

    /** Structured machine-readable JSON data export */
    JSON("json"),

    /** Wildcard format encompassing all supported report formats */
    ALL("all");

    private final String extension;

    DiskReportFormat(final String extension)
    {
        this.extension = extension;
    }

    /**
     * Gets the file extension associated with this report format.
     *
     * @return file extension string (e.g. "html", "md", "json")
     */
    public String getExtension()
    {
        return this.extension;
    }

    /**
     * Parses a comma-delimited configuration string into a set of active report formats.
     *
     * @param formatConfig comma-delimited or single format string (e.g. "HTML,JSON", "ALL", "md")
     * @return unmodifiable set of resolved report formats
     */
    public static Set<DiskReportFormat> parseFormats(final String formatConfig)
    {
        if (formatConfig == null || formatConfig.trim().isEmpty())
        {
            return Collections.unmodifiableSet(EnumSet.of(HTML, MARKDOWN, JSON));
        }

        final String[] tokens = formatConfig.split("[,;\\s]+");
        final Set<DiskReportFormat> formats = new HashSet<>();

        for (final String token : tokens)
        {
            final String normalized = token.trim().toUpperCase();
            if (normalized.isEmpty())
            {
                continue;
            }

            if ("ALL".equals(normalized) || "*".equals(normalized))
            {
                return Collections.unmodifiableSet(EnumSet.of(HTML, MARKDOWN, JSON));
            }
            else if ("MD".equals(normalized) || "MARKDOWN".equals(normalized))
            {
                formats.add(MARKDOWN);
            }
            else if ("HTML".equals(normalized) || "HTM".equals(normalized))
            {
                formats.add(HTML);
            }
            else if ("JSON".equals(normalized))
            {
                formats.add(JSON);
            }
        }

        if (formats.isEmpty())
        {
            return Collections.unmodifiableSet(EnumSet.of(HTML, MARKDOWN, JSON));
        }

        return Collections.unmodifiableSet(formats);
    }
}
