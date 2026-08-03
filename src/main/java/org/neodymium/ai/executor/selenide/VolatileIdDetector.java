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
package org.neodymium.ai.executor.selenide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.neodymium.ai.config.AiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects auto-generated, dynamic, or volatile HTML element ID attributes using configurable
 * regex patterns loaded from {@link AiConfiguration}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class VolatileIdDetector
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VolatileIdDetector.class);

    private final List<Pattern> patterns;

    /**
     * Constructs a VolatileIdDetector loading configuration from a new {@link AiConfiguration}.
     */
    public VolatileIdDetector()
    {
        this(AiConfiguration.getInstance());
    }

    /**
     * Constructs a VolatileIdDetector with a given {@link AiConfiguration}.
     *
     * @param config the AI configuration instance
     */
    public VolatileIdDetector(final AiConfiguration config)
    {
        this(config != null ? config.getVolatileIdPatterns() : Collections.emptyList());
    }

    /**
     * Constructs a VolatileIdDetector with explicit pattern strings.
     *
     * @param rawPatterns list of regex pattern strings
     */
    public VolatileIdDetector(final List<String> rawPatterns)
    {
        final List<Pattern> compiled = new ArrayList<>();
        if (rawPatterns != null)
        {
            for (final String raw : rawPatterns)
            {
                if (raw != null && !raw.isBlank())
                {
                    try
                    {
                        compiled.add(Pattern.compile(raw.trim()));
                    }
                    catch (final PatternSyntaxException e)
                    {
                        LOGGER.warn("Invalid volatile ID regex pattern in configuration: '{}'", raw, e);
                    }
                }
            }
        }
        this.patterns = Collections.unmodifiableList(compiled);
    }

    /**
     * Evaluates whether an element ID attribute matches any configured volatile ID pattern.
     *
     * @param idStr the element ID string to test
     * @return true if the ID is auto-generated/volatile and should be stripped from DOM dumps; false otherwise
     */
    public boolean isVolatile(final String idStr)
    {
        if (idStr == null || idStr.isBlank())
        {
            return false;
        }

        final String trimmed = idStr.trim();
        for (final Pattern pattern : this.patterns)
        {
            if (pattern.matcher(trimmed).matches() || pattern.matcher(trimmed).find())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the unmodifiable list of compiled regex patterns.
     *
     * @return compiled pattern list
     */
    public List<Pattern> getPatterns()
    {
        return this.patterns;
    }
}
