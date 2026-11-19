/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.common.testdata.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to preprocess and normalize YAML test steps, block scalars, and reserved syntax
 * characters prior to parsing by SnakeYAML.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class YamlStepPreprocessor
{
    private static final Pattern FOLDED_BLOCK_SCALAR_PATTERN = Pattern.compile(
        "(?m)^([ \\t]*(?:-\\s+|(?:steps|_steps|before|beforeEach|_beforeEach|after|afterEach|_afterEach):\\s*))>(-\\s*(?:#.*)?|[ \\t]*(?:#.*)?)$"
    );

    private static final Pattern STEP_LIST_ITEM_PATTERN = Pattern.compile("^(\\s*-\\s+)(.*)$");

    private static final Pattern CONDITIONAL_INCLUDE_PATTERN = Pattern.compile("(?i)^if\\s+.*(?:_include|include):.*");

    private YamlStepPreprocessor()
    {
    }

    /**
     * Normalizes folded block scalars ({@code >} and {@code >-}) to literal block scalars
     * ({@code |} and {@code |-}) for step lists and playbook lifecycle sections.
     *
     * @param content the raw YAML string content
     * @return the normalized YAML content with preserved line break semantics
     */
    public static String normalizeBlockScalars(final String content)
    {
        if (content == null || content.isEmpty())
        {
            return content;
        }
        return FOLDED_BLOCK_SCALAR_PATTERN.matcher(content).replaceAll("$1|$2");
    }

    /**
     * Preprocesses YAML script content to normalize block scalars and sanitize unquoted
     * step instructions containing YAML special characters (such as square brackets {@code [...]}
     * used in CSS attribute selectors or parameter hints, and inline conditional includes).
     *
     * @param content the raw YAML string content
     * @return the preprocessed YAML content safe for parsing
     */
    public static String preprocessYamlSteps(final String content)
    {
        if (content == null || content.isEmpty())
        {
            return content;
        }

        final String normalized = normalizeBlockScalars(content);
        final String[] lines = normalized.split("\\r?\\n", -1);
        final StringBuilder sb = new StringBuilder(normalized.length() + 64);

        for (int i = 0; i < lines.length; i++)
        {
            final String line = lines[i];
            final Matcher m = STEP_LIST_ITEM_PATTERN.matcher(line);
            if (m.find())
            {
                final String prefix = m.group(1);
                final String rawValue = m.group(2);
                final String trimmed = rawValue.trim();

                if (shouldQuoteStepLine(trimmed))
                {
                    final String escaped = trimmed.replace("\\", "\\\\").replace("\"", "\\\"");
                    sb.append(prefix).append("\"").append(escaped).append("\"");
                }
                else
                {
                    sb.append(line);
                }
            }
            else
            {
                sb.append(line);
            }

            if (i < lines.length - 1)
            {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static boolean shouldQuoteStepLine(final String trimmed)
    {
        if (trimmed.isEmpty() || trimmed.startsWith("\"") || trimmed.startsWith("'")
            || trimmed.startsWith("|") || trimmed.startsWith(">") || trimmed.startsWith("#"))
        {
            return false;
        }

        // Conditional include statements like: If ... _include: ...
        if (CONDITIONAL_INCLUDE_PATTERN.matcher(trimmed).matches())
        {
            return true;
        }

        // Check for structural YAML keys in playbooks/steps/data
        final String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("_include:") || lower.startsWith("include:")
            || lower.startsWith("instruction:") || lower.startsWith("promptline:")
            || lower.startsWith("step:") || lower.startsWith("actions:")
            || lower.startsWith("substeps:") || lower.startsWith("steps:")
            || lower.startsWith("_steps:") || lower.startsWith("data:")
            || lower.startsWith("_data:") || lower.startsWith("tags:")
            || lower.startsWith("_tags:") || lower.startsWith("properties:")
            || lower.startsWith("_properties:") || lower.startsWith("type:")
            || lower.startsWith("target:") || lower.startsWith("value:"))
        {
            return false;
        }

        // Natural language steps containing square brackets (e.g. CSS attribute selectors [data-cse='...'])
        // or starting with a bracketed selector (e.g. [data-test=...] click)
        if (trimmed.contains("[") && (trimmed.contains("]") || trimmed.startsWith("[")))
        {
            return true;
        }

        return false;
    }
}
