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

import java.util.Collections;
import java.util.Map;

/**
 * Immutable record representing the sanitized prompt and SUT state,
 * containing a mapping from the masked placeholder values back to their variable references.
 *
 * @param sanitizedPrompt the sanitized prompt string
 * @param sanitizedStateText the sanitized state text content (e.g. DOM HTML)
 * @param maskToVariableMap the mapping of masked placeholders to variable references (e.g. "[MASKED_PASSWORD]" to "${password}")
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public record SanitizedPayload(
    String sanitizedPrompt,
    String sanitizedStateText,
    Map<String, String> maskToVariableMap
)
{
    /**
     * Canonical constructor performing defensive copying of the mapping map.
     */
    public SanitizedPayload(
        final String sanitizedPrompt,
        final String sanitizedStateText,
        final Map<String, String> maskToVariableMap
    )
    {
        this.sanitizedPrompt = sanitizedPrompt;
        this.sanitizedStateText = sanitizedStateText;
        this.maskToVariableMap = maskToVariableMap == null ? Collections.emptyMap() : Map.copyOf(maskToVariableMap);
    }
}
