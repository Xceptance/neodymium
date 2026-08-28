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
import java.util.ArrayList;
import java.util.List;

/**
 * Generic utility helper to clean and extract the authoritative JSON structure (object or array)
 * from LLM response text outputs.
 * <p>
 * Supports responses containing multiple intermediate reasoning scratchpads or markdown code blocks
 * by scanning code blocks and JSON structures in reverse order (from last to first), guaranteeing
 * that the model's final conclusion is extracted rather than intermediate brainstormed drafts.
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
     * Extracts and sanitizes the final, authoritative JSON content (object or array) from an LLM response text.
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

        final String trimmed = rawContent.trim();

        // 1. Check if the entire rawContent is directly a valid JSON structure
        if (isValidJson(trimmed))
        {
            return trimmed;
        }

        // 2. Search for the last valid JSON object or array in the text (scanned from end of text)
        final String extracted = findLastValidJson(trimmed);
        if (extracted != null)
        {
            return extracted;
        }

        return trimmed;
    }

    private static boolean isValidJson(final String text)
    {
        if (text == null || text.isEmpty())
        {
            return false;
        }
        final char firstChar = text.charAt(0);
        final char lastChar = text.charAt(text.length() - 1);
        if ((firstChar != '{' || lastChar != '}') && (firstChar != '[' || lastChar != ']'))
        {
            return false;
        }
        try (com.fasterxml.jackson.core.JsonParser parser = MAPPER.createParser(text))
        {
            MAPPER.readTree(parser);
            return parser.nextToken() == null;
        }
        catch (final Exception ignored)
        {
            return false;
        }
    }

    /**
     * Finds the last valid JSON Object or Array in text by inspecting balanced close/open pairs from the end.
     */
    private static String findLastValidJson(final String text)
    {
        if (text == null || text.isEmpty())
        {
            return null;
        }

        // Collect all candidate closing brace and bracket positions in reverse order
        final List<Integer> closePositions = new ArrayList<>();
        for (int i = text.length() - 1; i >= 0; i--)
        {
            final char c = text.charAt(i);
            if (c == '}' || c == ']')
            {
                closePositions.add(i);
            }
        }

        for (final int closePos : closePositions)
        {
            final char closeChar = text.charAt(closePos);
            final char openChar = (closeChar == '}') ? '{' : '[';

            // Collect all matching open characters before closePos
            final List<Integer> openPositions = new ArrayList<>();
            for (int i = 0; i < closePos; i++)
            {
                if (text.charAt(i) == openChar)
                {
                    openPositions.add(i);
                }
            }

            // Test candidate openings from closest to furthest
            for (int j = openPositions.size() - 1; j >= 0; j--)
            {
                final int openPos = openPositions.get(j);
                final String candidate = text.substring(openPos, closePos + 1).trim();
                if (isValidJson(candidate))
                {
                    return candidate;
                }
            }
        }

        return null;
    }
}
