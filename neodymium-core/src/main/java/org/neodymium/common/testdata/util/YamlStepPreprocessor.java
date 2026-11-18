/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
