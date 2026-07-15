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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;

/**
 * Service providing multi-stage response parsing and repairing capabilities.
 * Strips markdown code blocks, balances missing JSON structural brackets,
 * and deserializes json string payloads into target typed classes.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ResponseRepairService
{
    /**
     * Shared Gson instance for deserialization.
     */
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private ResponseRepairService()
    {
    }

    /**
     * Extracts raw JSON content from markdown code fences and balances trailing braces.
     *
     * @param rawContent the raw text output returned by the LLM
     * @return the repaired and cleaned JSON string
     */
    public static String repairJson(final String rawContent)
    {
        if (rawContent == null)
        {
            return "";
        }

        // 1. Strip markdown json code fences (e.g. ```json ... ``` or ``` ... ```)
        String content = rawContent.trim();
        if (content.startsWith("```"))
        {
            final int firstNewLine = content.indexOf('\n');
            if (firstNewLine != -1)
            {
                content = content.substring(firstNewLine + 1);
            }
            else
            {
                content = content.substring(3);
            }

            if (content.endsWith("```"))
            {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();
        }

        // 2. Balance missing JSON closing braces (curly braces)
        int openCurlyBraces = 0;
        int closeCurlyBraces = 0;
        for (int i = 0; i < content.length(); i++)
        {
            final char c = content.charAt(i);
            if (c == '{')
            {
                openCurlyBraces++;
            }
            else if (c == '}')
            {
                closeCurlyBraces++;
            }
        }

        final StringBuilder sb = new StringBuilder(content);
        while (openCurlyBraces > closeCurlyBraces)
        {
            sb.append('}');
            closeCurlyBraces++;
        }

        return sb.toString();
    }

    /**
     * Repairs and deserializes a JSON response payload into the target class type.
     *
     * @param <T> the target type
     * @param rawContent the raw string response
     * @param type the target class type
     * @return the deserialized object instance
     * @throws IOException if GSON deserialization fails after repairing
     */
    public static <T> T deserialize(final String rawContent, final Class<T> type) throws IOException
    {
        final String repaired = repairJson(rawContent);
        try
        {
            return GSON.fromJson(repaired, type);
        }
        catch (final Exception e)
        {
            throw new IOException("Failed to deserialize repaired JSON to type: " + type.getSimpleName(), e);
        }
    }
}
