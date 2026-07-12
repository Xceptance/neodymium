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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.SessionData;

/**
 * Default implementation of {@link ActionSanitizer}.
 * Sanitizes executed actions on the fly by replacing actual secret credential values
 * with their corresponding variable reference placeholders (e.g. "${password}").
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class DefaultActionSanitizer implements ActionSanitizer
{
    /**
     * Constructs a default DefaultActionSanitizer.
     */
    public DefaultActionSanitizer()
    {
    }

    /**
     * Sanitizes an executed action on the fly. Replaces actual sensitive values in
     * the target selector, description, and input values list with variable reference syntax.
     *
     * @param rawAction the raw executed action
     * @param data the active session variables map identifying which values are sensitive
     * @return the sanitized and parameterized Action instance
     */
    @Override
    public Action sanitize(final Action rawAction, final SessionData data)
    {
        if (rawAction == null)
        {
            return null;
        }

        if (data == null)
        {
            return rawAction;
        }

        final Map<String, String> sensitiveMap = data.getRawSensitiveData();
        if (sensitiveMap.isEmpty())
        {
            return rawAction;
        }

        // 1. Sanitize values list
        final List<String> sanitizedValues = new ArrayList<>();
        for (final String val : rawAction.getValues())
        {
            if (val == null)
            {
                sanitizedValues.add(null);
            }
            else
            {
                String cleanVal = val;
                for (final Map.Entry<String, String> entry : sensitiveMap.entrySet())
                {
                    final String varKey = entry.getKey();
                    final String secretValue = entry.getValue();
                    if (secretValue != null && !secretValue.isEmpty())
                    {
                        cleanVal = cleanVal.replace(secretValue, "${" + varKey + "}");
                    }
                }
                sanitizedValues.add(cleanVal);
            }
        }

        // 2. Sanitize target selector/URL
        String sanitizedTarget = rawAction.getTarget();
        if (sanitizedTarget != null)
        {
            for (final Map.Entry<String, String> entry : sensitiveMap.entrySet())
            {
                final String varKey = entry.getKey();
                final String secretValue = entry.getValue();
                if (secretValue != null && !secretValue.isEmpty())
                {
                    sanitizedTarget = sanitizedTarget.replace(secretValue, "${" + varKey + "}");
                }
            }
        }

        // 3. Sanitize description
        String sanitizedDesc = rawAction.getDescription();
        if (sanitizedDesc != null)
        {
            for (final Map.Entry<String, String> entry : sensitiveMap.entrySet())
            {
                final String varKey = entry.getKey();
                final String secretValue = entry.getValue();
                if (secretValue != null && !secretValue.isEmpty())
                {
                    sanitizedDesc = sanitizedDesc.replace(secretValue, "${" + varKey + "}");
                }
            }
        }

        // Construct the new sanitized action
        final Action sanitizedAction = new Action(
            rawAction.getType(),
            sanitizedTarget,
            sanitizedValues,
            sanitizedDesc,
            rawAction.getReasoning()
        );

        // Copy dynamic parameters map
        sanitizedAction.getParameters().putAll(rawAction.getParameters());

        return sanitizedAction;
    }
}
