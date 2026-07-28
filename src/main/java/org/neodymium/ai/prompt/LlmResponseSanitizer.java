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
package org.neodymium.ai.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility helper to clean and extract raw JSON structures from LLM response text outputs.
 * Handles markdown code block stripping (```json ... ```) and extracts bounded JSON
 * objects ({ ... }) or arrays ([ ... ]), filtering out any conversational preambles,
 * markdown headers, or malformed leading labels (e.g. {\label} : country_selector_click).
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmResponseSanitizer
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LlmResponseSanitizer()
    {
        // utility class
    }

    /**
     * Extracts and sanitizes the raw JSON content (object or array) from an LLM response text.
     *
     * @param rawContent the raw string returned by the LLM
     * @return the sanitized JSON string, or empty string if input is null/empty
     */
    public static String extractJson(final String rawContent)
    {
        if (rawContent == null || rawContent.isBlank())
        {
            return "";
        }

        String content = rawContent.trim();

        // 1. Strip markdown code block wrappers
        if (content.contains("```json"))
        {
            content = content.substring(content.indexOf("```json") + 7);
            if (content.contains("```"))
            {
                content = content.substring(0, content.indexOf("```"));
            }
        }
        else if (content.contains("```"))
        {
            content = content.substring(content.indexOf("```") + 3);
            if (content.contains("```"))
            {
                content = content.substring(0, content.indexOf("```"));
            }
        }

        content = content.trim();

        // Quick check if full string is already valid JSON
        try
        {
            MAPPER.readTree(content);
            return content;
        }
        catch (final Exception ignored)
        {
            // Fall through to sub-string extraction
        }

        // 2. Extract valid JSON Object { ... }
        final int lastBrace = content.lastIndexOf('}');
        if (lastBrace > 0)
        {
            int searchPos = 0;
            while (searchPos < lastBrace)
            {
                final int candidateBrace = content.indexOf('{', searchPos);
                if (candidateBrace < 0)
                {
                    break;
                }
                final String candidateJson = content.substring(candidateBrace, lastBrace + 1).trim();
                try
                {
                    MAPPER.readTree(candidateJson);
                    return candidateJson;
                }
                catch (final Exception ignored)
                {
                    searchPos = candidateBrace + 1;
                }
            }
        }

        // 3. Extract valid JSON Array [ ... ]
        final int lastBracket = content.lastIndexOf(']');
        if (lastBracket > 0)
        {
            int searchPos = 0;
            while (searchPos < lastBracket)
            {
                final int candidateBracket = content.indexOf('[', searchPos);
                if (candidateBracket < 0)
                {
                    break;
                }
                final String candidateJson = content.substring(candidateBracket, lastBracket + 1).trim();
                try
                {
                    MAPPER.readTree(candidateJson);
                    return candidateJson;
                }
                catch (final Exception ignored)
                {
                    searchPos = candidateBracket + 1;
                }
            }
        }

        return content;
    }
}
