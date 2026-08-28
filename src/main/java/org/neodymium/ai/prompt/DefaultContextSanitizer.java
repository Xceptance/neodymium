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

import java.util.HashMap;
import java.util.Map;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.model.SessionData;

/**
 * Default implementation of {@link ContextSanitizer}.
 * Scans prompts and SUT state texts for raw secret values and replaces them
 * with format-preserving stand-in placeholders (e.g. "[MASKED_VAR_key]").
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class DefaultContextSanitizer implements ContextSanitizer
{
    /**
     * Constructs a default DefaultContextSanitizer.
     */
    public DefaultContextSanitizer()
    {
    }

    /**
     * Sanitizes raw prompt text and SUT state DOM content.
     * Replaces actual sensitive values with standardized "[MASKED_VAR_key]" placeholders
     * and maps each placeholder to its key reference.
     *
     * @param rawPrompt the raw compiled user or system prompt string
     * @param rawState the raw captured SUT state containing textual elements (e.g. DOM source)
     * @param data the active session variables map identifying which values are sensitive
     * @return the sanitized payload containing cleaned text and reverse value mappings
     */
    @Override
    public SanitizedPayload sanitize(final String rawPrompt, final SutState rawState, final SessionData data)
    {
        String cleanPrompt = rawPrompt == null ? "" : rawPrompt;
        String cleanStateText = (rawState == null || rawState.getTextContent() == null) ? "" : rawState.getTextContent();
        final Map<String, String> maskToVariableMap = new HashMap<>();

        if (data != null)
        {
            final Map<String, String> sensitiveMap = data.getRawSensitiveData();
            for (final Map.Entry<String, String> entry : sensitiveMap.entrySet())
            {
                final String varKey = entry.getKey();
                final String secretValue = entry.getValue();

                if (secretValue != null && !secretValue.isEmpty())
                {
                    final String maskPlaceholder = "[MASKED_VAR_" + varKey + "]";
                    maskToVariableMap.put(maskPlaceholder, "${" + varKey + "}");

                    // Replace all occurrences in the prompt and state text
                    cleanPrompt = cleanPrompt.replace(secretValue, maskPlaceholder);
                    cleanStateText = cleanStateText.replace(secretValue, maskPlaceholder);
                }
            }
        }

        return new SanitizedPayload(cleanPrompt, cleanStateText, maskToVariableMap);
    }
}
