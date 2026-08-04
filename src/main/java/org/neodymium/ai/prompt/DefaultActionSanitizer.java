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
        final Map<String, String> varMap = data.getAllVariables();
        if (varMap.isEmpty())
        {
            return rawAction;
        }

        // Sort entries by value length descending to prevent substring collision (e.g. nested URLs)
        final List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(varMap.entrySet());
        sortedEntries.sort((e1, e2) -> Integer.compare(e2.getValue().length(), e1.getValue().length()));

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
                for (final Map.Entry<String, String> entry : sortedEntries)
                {
                    final String varKey = entry.getKey();
                    final String rawVal = entry.getValue();
                    if (rawVal != null && !rawVal.isEmpty())
                    {
                        final boolean isSensitive = sensitiveMap.containsKey(varKey);
                        if (isSensitive || rawVal.length() >= 4)
                        {
                            cleanVal = cleanVal.replace(rawVal, "${" + varKey + "}");
                        }
                    }
                }
                sanitizedValues.add(cleanVal);
            }
        }

        // 2. Sanitize target selector/URL
        String rawTarget = rawAction.getTarget();
        if (("NAVIGATE".equalsIgnoreCase(rawAction.getType()) || "GOTO".equalsIgnoreCase(rawAction.getType()))
            && (rawTarget == null || rawTarget.isEmpty() || "url".equalsIgnoreCase(rawTarget)))
        {
            if (rawAction.getValue() != null && !rawAction.getValue().isEmpty())
            {
                rawTarget = rawAction.getValue();
            }
        }
        String sanitizedTarget = rawTarget;
        if (sanitizedTarget != null)
        {
            for (final Map.Entry<String, String> entry : sortedEntries)
            {
                final String varKey = entry.getKey();
                final String rawVal = entry.getValue();
                if (rawVal != null && !rawVal.isEmpty())
                {
                    final boolean isSensitive = sensitiveMap.containsKey(varKey);
                    if (isSensitive || rawVal.length() >= 4)
                    {
                        sanitizedTarget = sanitizedTarget.replace(rawVal, "${" + varKey + "}");
                    }
                }
            }
        }
        if (("NAVIGATE".equalsIgnoreCase(rawAction.getType()) || "GOTO".equalsIgnoreCase(rawAction.getType()))
            && (sanitizedTarget == null || sanitizedTarget.isEmpty() || "url".equalsIgnoreCase(sanitizedTarget))
            && !sanitizedValues.isEmpty() && sanitizedValues.get(0) != null)
        {
            sanitizedTarget = sanitizedValues.get(0);
        }

        // 3. Sanitize description
        String sanitizedDesc = rawAction.getDescription();
        if (sanitizedDesc != null)
        {
            for (final Map.Entry<String, String> entry : sortedEntries)
            {
                final String varKey = entry.getKey();
                final String rawVal = entry.getValue();
                if (rawVal != null && !rawVal.isEmpty())
                {
                    final boolean isSensitive = sensitiveMap.containsKey(varKey);
                    if (isSensitive || rawVal.length() >= 4)
                    {
                        sanitizedDesc = sanitizedDesc.replace(rawVal, "${" + varKey + "}");
                    }
                }
            }
        }

        // 4. Sanitize reasoning
        String sanitizedReasoning = rawAction.getReasoning();
        if (sanitizedReasoning != null)
        {
            for (final Map.Entry<String, String> entry : sortedEntries)
            {
                final String varKey = entry.getKey();
                final String rawVal = entry.getValue();
                if (rawVal != null && !rawVal.isEmpty())
                {
                    final boolean isSensitive = sensitiveMap.containsKey(varKey);
                    if (isSensitive || rawVal.length() >= 4)
                    {
                        sanitizedReasoning = sanitizedReasoning.replace(rawVal, "${" + varKey + "}");
                    }
                }
            }
        }

        // Construct the new sanitized action
        final Action sanitizedAction = new Action(
            rawAction.getType(),
            sanitizedTarget,
            sanitizedValues,
            sanitizedDesc,
            sanitizedReasoning
        );

        // Copy dynamic parameters map
        sanitizedAction.getParameters().putAll(rawAction.getParameters());

        // Recursively sanitize nested branch/conditional action lists
        if (rawAction.getCondition() != null)
        {
            final List<Action> sanitizedCondition = new ArrayList<>();
            for (final Action condAct : rawAction.getCondition())
            {
                sanitizedCondition.add(sanitize(condAct, data));
            }
            sanitizedAction.setCondition(sanitizedCondition);
        }
        
        if (rawAction.getThen() != null)
        {
            final List<Action> sanitizedThen = new ArrayList<>();
            for (final Action thenAct : rawAction.getThen())
            {
                sanitizedThen.add(sanitize(thenAct, data));
            }
            sanitizedAction.setThen(sanitizedThen);
        }
        
        if (rawAction.getElseActions() != null)
        {
            final List<Action> sanitizedElse = new ArrayList<>();
            for (final Action elseAct : rawAction.getElseActions())
            {
                sanitizedElse.add(sanitize(elseAct, data));
            }
            sanitizedAction.setElseActions(sanitizedElse);
        }

        return sanitizedAction;
    }

    /**
     * Sanitizes raw text content against SessionData variables.
     *
     * @param input the raw input string
     * @param data the SessionData variable container
     * @return the sanitized string with variables replaced
     */
    public String sanitizeText(final String input, final SessionData data)
    {
        if (input == null || input.isEmpty() || data == null)
        {
            return input;
        }

        final Map<String, String> sensitiveMap = data.getRawSensitiveData();
        final Map<String, String> varMap = data.getAllVariables();
        if (varMap.isEmpty())
        {
            return input;
        }

        final List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(varMap.entrySet());
        sortedEntries.sort((e1, e2) -> Integer.compare(e2.getValue().length(), e1.getValue().length()));

        String clean = input;
        for (final Map.Entry<String, String> entry : sortedEntries)
        {
            final String varKey = entry.getKey();
            final String rawVal = entry.getValue();
            if (rawVal != null && !rawVal.isEmpty())
            {
                final boolean isSensitive = sensitiveMap.containsKey(varKey);
                if (isSensitive || rawVal.length() >= 4)
                {
                    clean = clean.replace(rawVal, "${" + varKey + "}");
                }
            }
        }
        return clean;
    }
}
