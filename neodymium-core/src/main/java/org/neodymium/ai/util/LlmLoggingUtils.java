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
package org.neodymium.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility methods for formatting and prettifying LLM JSON responses for debug and trace logging.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmLoggingUtils
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private LlmLoggingUtils()
    {
    }

    /**
     * Formats and pretty-prints raw JSON string content (including markdown-fenced json blocks)
     * for human-readable logging. If parsing fails, returns the raw input unchanged.
     *
     * @param rawContent the raw JSON string or markdown-fenced text
     * @return pretty-printed JSON string, or the raw string if parsing fails
     */
    public static String formatJsonForLogging(final String rawContent)
    {
        if (rawContent == null || rawContent.trim().isEmpty())
        {
            return rawContent;
        }
        try
        {
            String jsonContent = rawContent.trim();
            if (jsonContent.startsWith("```json"))
            {
                jsonContent = jsonContent.substring(7);
                if (jsonContent.endsWith("```"))
                {
                    jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
                }
            }
            else if (jsonContent.startsWith("```"))
            {
                jsonContent = jsonContent.substring(3);
                if (jsonContent.endsWith("```"))
                {
                    jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
                }
            }

            final Object json = MAPPER.readValue(jsonContent.trim(), Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        }
        catch (final Exception e)
        {
            return rawContent;
        }
    }
}
