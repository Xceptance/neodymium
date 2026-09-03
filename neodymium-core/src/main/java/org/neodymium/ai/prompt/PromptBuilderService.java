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

/**
 * Service that formats and applies model-specific system/user message templates
 * (e.g. wrapping prompts with instruct tags for specific open source models).
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PromptBuilderService
{
    private PromptBuilderService()
    {
    }

    /**
     * Formats the compiled prompt string according to the target model type.
     *
     * @param modelName the active LLM model identifier
     * @param message the raw compiled message
     * @param isSystemMessage true if formatting system instructions, false for user query
     * @return the formatted prompt string
     */
    public static String format(final String modelName, final String message, final boolean isSystemMessage)
    {
        if (message == null)
        {
            return "";
        }

        final String trimmed = message.trim();
        if (modelName != null && modelName.toLowerCase().contains("mistral"))
        {
            if (isSystemMessage)
            {
                return "<<SYS>>\n" + trimmed + "\n<</SYS>>";
            }
            else
            {
                return "[INST] " + trimmed + " [/INST]";
            }
        }

        // Default: return raw compiled prompt (standard Gemini / OpenAI style)
        return trimmed;
    }
}
