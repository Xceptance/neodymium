/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.action.LocatorCandidate;
import org.neodymium.ai.model.SessionData;
import org.neodymium.util.Neodymium;

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
        return sanitizeAction(rawAction, data, false);
    }

    /**
     * Scrubs only sensitive session values (from {@link SessionData#getRawSensitiveData()})
     * in the given action while preserving already-resolved non-sensitive values and action metadata.
     *
     * @param rawAction the action to scrub
     * @param data the active session data container identifying sensitive credentials
     * @return the action with sensitive credential literals replaced by placeholders
     */
    public Action sanitizeSensitiveOnly(final Action rawAction, final SessionData data)
    {
        return sanitizeAction(rawAction, data, true);
    }

    private Action sanitizeAction(final Action rawAction, final SessionData data, final boolean sensitiveOnly)
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
        if (sensitiveOnly && (sensitiveMap == null || sensitiveMap.isEmpty()))
        {
            return rawAction;
        }

        final Map<String, String> varMap = sensitiveOnly ? sensitiveMap : buildSanitizationVariables(rawAction, data);
        if (varMap == null || varMap.isEmpty())
        {
            return rawAction;
        }

        // Sort entries by value length descending to prevent substring collision (e.g. nested URLs)
        final List<Map.Entry<String, String>> sortedEntries = sortEntriesDescending(varMap);
        if (sortedEntries.isEmpty())
        {
            return rawAction;
        }

        // 1. Sanitize values list
        final List<String> sanitizedValues = new ArrayList<>();
        for (final String val : rawAction.getValues())
        {
            sanitizedValues.add(replaceVariablesInText(val, sortedEntries, sensitiveMap, data, sensitiveOnly));
        }

        // 2. Sanitize target selector/URL
        String rawTarget = rawAction.getTarget();
        final boolean isNavigate = "NAVIGATE".equalsIgnoreCase(rawAction.getType()) || "GOTO".equalsIgnoreCase(rawAction.getType());
        if (!sensitiveOnly && isNavigate && (rawTarget == null || rawTarget.isEmpty() || "url".equalsIgnoreCase(rawTarget)))
        {
            if (rawAction.getValue() != null && !rawAction.getValue().isEmpty())
            {
                rawTarget = rawAction.getValue();
            }
        }
        String sanitizedTarget = replaceVariablesInText(rawTarget, sortedEntries, sensitiveMap, data, sensitiveOnly);
        if (!sensitiveOnly && isNavigate
            && (sanitizedTarget == null || sanitizedTarget.isEmpty() || "url".equalsIgnoreCase(sanitizedTarget))
            && !sanitizedValues.isEmpty() && sanitizedValues.get(0) != null)
        {
            sanitizedTarget = sanitizedValues.get(0);
        }

        // 3. Sanitize description
        final String sanitizedDesc = replaceVariablesInText(rawAction.getDescription(), sortedEntries, sensitiveMap, data, sensitiveOnly);

        // 4. Sanitize reasoning
        final String sanitizedReasoning = replaceVariablesInText(rawAction.getReasoning(), sortedEntries, sensitiveMap, data, sensitiveOnly);

        // Construct the new sanitized action while preserving flags and metadata
        final Action sanitizedAction = new Action(
            rawAction.getType(),
            sanitizedTarget,
            sanitizedValues,
            sanitizedDesc,
            sanitizedReasoning,
            rawAction.isRegex(),
            rawAction.isNegated()
        );

        sanitizedAction.setStepInstruction(sanitizeFieldText(rawAction.getStepInstruction(), data, sortedEntries, sensitiveMap, sensitiveOnly));
        sanitizedAction.setStepLine(rawAction.getStepLine());
        sanitizedAction.setStepFile(rawAction.getStepFile());
        sanitizedAction.setStepScreenshotHash(rawAction.getStepScreenshotHash());
        sanitizedAction.setAdjust(rawAction.getAdjust());
        sanitizedAction.setSelfCritique(rawAction.getSelfCritique());
        final List<LocatorCandidate> sanitizedCandidates = new ArrayList<>();
        if (rawAction.getCandidateLocators() != null)
        {
            for (final LocatorCandidate candidate : rawAction.getCandidateLocators())
            {
                if (candidate != null)
                {
                    final String sanitizedCandidateTarget = sanitizeFieldText(candidate.getLocator(), data, sortedEntries, sensitiveMap, sensitiveOnly);
                    sanitizedCandidates.add(new LocatorCandidate(sanitizedCandidateTarget, candidate.getStrategy(), candidate.getScore(), candidate.getReasoning()));
                }
            }
        }
        sanitizedAction.setCandidateLocators(sanitizedCandidates);
        sanitizedAction.setDomFeatureVector(rawAction.getDomFeatureVector());
        sanitizedAction.setDurationMs(rawAction.getDurationMs());
        sanitizedAction.setDelayMs(rawAction.getDelayMs());
        sanitizedAction.setHasElse(rawAction.getHasElse());
        if (rawAction.getFrame() != null)
        {
            sanitizedAction.setFrame(sanitizeFieldText(rawAction.getFrame(), data, sortedEntries, sensitiveMap, sensitiveOnly));
        }
        sanitizedAction.setSubStepIndex(rawAction.getSubStepIndex());
        if (rawAction.getSubStepInstruction() != null)
        {
            sanitizedAction.setSubStepInstruction(sanitizeFieldText(rawAction.getSubStepInstruction(), data, sortedEntries, sensitiveMap, sensitiveOnly));
        }
        sanitizedAction.setHealed(rawAction.isHealed());
        if (rawAction.getExpectedTarget() != null)
        {
            sanitizedAction.setExpectedTarget(sanitizeFieldText(rawAction.getExpectedTarget(), data, sortedEntries, sensitiveMap, sensitiveOnly));
        }

        // Copy dynamic parameters map
        sanitizedAction.getParameters().putAll(rawAction.getParameters());

        // Recursively sanitize nested branch/conditional action lists
        if (rawAction.getCondition() != null)
        {
            final List<Action> sanitizedCondition = new ArrayList<>();
            for (final Action condAct : rawAction.getCondition())
            {
                sanitizedCondition.add(sanitizeAction(condAct, data, sensitiveOnly));
            }
            sanitizedAction.setCondition(sanitizedCondition);
        }

        if (rawAction.getThen() != null)
        {
            final List<Action> sanitizedThen = new ArrayList<>();
            for (final Action thenAct : rawAction.getThen())
            {
                sanitizedThen.add(sanitizeAction(thenAct, data, sensitiveOnly));
            }
            sanitizedAction.setThen(sanitizedThen);
        }

        if (rawAction.getElseActions() != null)
        {
            final List<Action> sanitizedElse = new ArrayList<>();
            for (final Action elseAct : rawAction.getElseActions())
            {
                sanitizedElse.add(sanitizeAction(elseAct, data, sensitiveOnly));
            }
            sanitizedAction.setElseActions(sanitizedElse);
        }

        return sanitizedAction;
    }

    private String sanitizeFieldText(
            final String input,
            final SessionData data,
            final List<Map.Entry<String, String>> sortedEntries,
            final Map<String, String> sensitiveMap,
            final boolean sensitiveOnly)
    {
        if (input == null || input.isEmpty())
        {
            return input;
        }
        if (sensitiveOnly)
        {
            return replaceVariablesInText(input, sortedEntries, sensitiveMap, data, true);
        }
        return sanitizeText(input, data);
    }

    private static List<Map.Entry<String, String>> sortEntriesDescending(final Map<String, String> varMap)
    {
        final List<Map.Entry<String, String>> sortedEntries = new ArrayList<>();
        for (final Map.Entry<String, String> entry : varMap.entrySet())
        {
            if (entry.getValue() != null && !entry.getValue().isEmpty())
            {
                sortedEntries.add(entry);
            }
        }
        sortedEntries.sort((e1, e2) -> Integer.compare(e2.getValue().length(), e1.getValue().length()));
        return sortedEntries;
    }

    private String replaceVariablesInText(
            final String input,
            final List<Map.Entry<String, String>> sortedEntries,
            final Map<String, String> sensitiveMap,
            final SessionData data,
            final boolean sensitiveOnly)
    {
        if (input == null || input.isEmpty() || sortedEntries.isEmpty())
        {
            return input;
        }

        String clean = input;
        for (final Map.Entry<String, String> entry : sortedEntries)
        {
            final String varKey = entry.getKey();
            final String rawVal = entry.getValue();
            if (rawVal != null && !rawVal.isEmpty())
            {
                if (!sensitiveOnly)
                {
                    if (!isEligibleVariable(varKey, data))
                    {
                        continue;
                    }
                    final boolean isSensitive = sensitiveMap != null && sensitiveMap.containsKey(varKey);
                    if (!isSensitive && rawVal.length() < 4)
                    {
                        continue;
                    }
                }
                clean = replaceOutsidePlaceholders(clean, rawVal, "${" + varKey + "}");
            }
        }
        return clean;
    }

    /**
     * Builds the prioritized variable map for sanitizing an action.
     * Prioritizes explicit placeholders declared in the step template, active dataset variables,
     * sensitive keys, and Neodymium.getData() before falling back to all variables.
     *
     * @param rawAction the action to sanitize
     * @param data the session data container
     * @return map of variable names to raw values
     */
    private Map<String, String> buildSanitizationVariables(final Action rawAction, final SessionData data)
    {
        final Map<String, String> allVars = data.getAllVariables();
        final Map<String, String> varMap = new LinkedHashMap<>();

        // 1. Explicit placeholders in the action's step instruction template
        if (rawAction != null && rawAction.getStepInstruction() != null)
        {
            final Set<String> keys = data.extractVariableNames(rawAction.getStepInstruction());
            for (final String key : keys)
            {
                final String val = allVars.get(key);
                if (val != null && !val.isEmpty())
                {
                    varMap.put(key, val);
                }
            }
        }

        // 2. Sensitive variables (always parameterize credentials)
        if (data.getRawSensitiveData() != null)
        {
            for (final String key : data.getRawSensitiveData().keySet())
            {
                final String val = allVars.get(key);
                if (val != null && !val.isEmpty())
                {
                    varMap.put(key, val);
                }
            }
        }

        // 3. Explicit dataset row variables
        if (data.getAllRawDataMap() != null)
        {
            for (final String key : data.getAllRawDataMap().keySet())
            {
                final String val = allVars.get(key);
                if (val != null && !val.isEmpty())
                {
                    varMap.put(key, val);
                }
            }
        }

        // 4. Neodymium test data
        try
        {
            if (Neodymium.getData() != null)
            {
                for (final String key : Neodymium.getData().keySet())
                {
                    final String val = allVars.get(key);
                    if (val != null && !val.isEmpty())
                    {
                        varMap.put(key, val);
                    }
                }
            }
        }
        catch (final Throwable ignored)
        {
        }

        // 5. Fallback to all variables filtered by eligibility
        for (final Map.Entry<String, String> entry : allVars.entrySet())
        {
            if (entry.getValue() != null && !entry.getValue().isEmpty() && !varMap.containsKey(entry.getKey()) && isEligibleVariable(entry.getKey(), data))
            {
                varMap.put(entry.getKey(), entry.getValue());
            }
        }

        // 6. If action is STORE, exclude the target variable being defined to avoid circular self-references
        if (rawAction != null && "STORE".equalsIgnoreCase(rawAction.getType()))
        {
            final String targetVar = rawAction.getValue();
            if (targetVar != null && !targetVar.isBlank())
            {
                varMap.remove(targetVar.trim());
            }
        }

        return varMap;
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
        return sanitizeText(input, data, Collections.emptySet());
    }

    /**
     * Sanitizes raw text content against SessionData variables, excluding specified target variable keys.
     *
     * @param input the raw input string
     * @param data the SessionData variable container
     * @param excludedVars set of variable names that must not be used for replacement
     * @return the sanitized string with variables replaced
     */
    public String sanitizeText(final String input, final SessionData data, final Set<String> excludedVars)
    {
        if (input == null || input.isEmpty() || data == null)
        {
            return input;
        }

        final Map<String, String> sensitiveMap = data.getRawSensitiveData();
        final Map<String, String> allVars = data.getAllVariables();
        final Map<String, String> varMap = new LinkedHashMap<>();

        // 1. Explicit placeholders inside the input string itself
        final Set<String> keys = data.extractVariableNames(input);
        for (final String key : keys)
        {
            if (excludedVars != null && excludedVars.contains(key))
            {
                continue;
            }
            final String val = allVars.get(key);
            if (val != null && !val.isEmpty())
            {
                varMap.put(key, val);
            }
        }

        // 2. Sensitive data
        if (data.getRawSensitiveData() != null)
        {
            for (final String key : data.getRawSensitiveData().keySet())
            {
                if (excludedVars != null && excludedVars.contains(key))
                {
                    continue;
                }
                final String val = allVars.get(key);
                if (val != null && !val.isEmpty())
                {
                    varMap.put(key, val);
                }
            }
        }

        // 3. Dataset variables
        if (data.getAllRawDataMap() != null)
        {
            for (final String key : data.getAllRawDataMap().keySet())
            {
                if (excludedVars != null && excludedVars.contains(key))
                {
                    continue;
                }
                final String val = allVars.get(key);
                if (val != null && !val.isEmpty())
                {
                    varMap.put(key, val);
                }
            }
        }

        // 4. Neodymium data
        try
        {
            if (Neodymium.getData() != null)
            {
                for (final String key : Neodymium.getData().keySet())
                {
                    if (excludedVars != null && excludedVars.contains(key))
                    {
                        continue;
                    }
                    final String val = allVars.get(key);
                    if (val != null && !val.isEmpty())
                    {
                        varMap.put(key, val);
                    }
                }
            }
        }
        catch (final Throwable ignored)
        {
        }

        // 5. Fallback
        for (final Map.Entry<String, String> entry : allVars.entrySet())
        {
            if (excludedVars != null && excludedVars.contains(entry.getKey()))
            {
                continue;
            }
            if (entry.getValue() != null && !entry.getValue().isEmpty() && !varMap.containsKey(entry.getKey()) && isEligibleVariable(entry.getKey(), data))
            {
                varMap.put(entry.getKey(), entry.getValue());
            }
        }

        if (excludedVars != null && !excludedVars.isEmpty())
        {
            varMap.keySet().removeAll(excludedVars);
        }

        if (varMap.isEmpty())
        {
            return input;
        }

        final List<Map.Entry<String, String>> sortedEntries = sortEntriesDescending(varMap);
        return replaceVariablesInText(input, sortedEntries, sensitiveMap, data, false);
    }

    /**
     * Determines whether a variable key is eligible for action and text sanitization.
     */
    private boolean isEligibleVariable(final String varKey, final SessionData data)
    {
        if (varKey == null || varKey.isEmpty())
        {
            return false;
        }

        // Always allow variables that exist in dynamicData, staticData, sensitiveData, or Neodymium.getData()
        if (data != null)
        {
            if (data.getAllRawDataMap().containsKey(varKey)
                || data.getRawSensitiveData().containsKey(varKey))
            {
                return true;
            }
            try
            {
                if (Neodymium.getData() != null && Neodymium.getData().containsKey(varKey))
                {
                    return true;
                }
            }
            catch (final Throwable ignored)
            {
            }
        }

        // Skip internal system, Java, sun, and OS environment variable properties
        if (varKey.startsWith("neodymium.junit.") || varKey.startsWith("neodymium.report.")
            || varKey.startsWith("java.") || varKey.startsWith("sun.") || varKey.startsWith("user.")
            || varKey.startsWith("os.") || varKey.startsWith("path.") || varKey.startsWith("file.")
            || varKey.startsWith("line.") || varKey.startsWith("XDG_") || varKey.startsWith("GNOME_")
            || varKey.startsWith("DESKTOP_") || varKey.startsWith("SESSION_") || varKey.startsWith("DBUS_")
            || varKey.startsWith("VTE_") || varKey.startsWith("SSH_") || varKey.startsWith("GIO_")
            || varKey.startsWith("GTK_") || varKey.startsWith("QT_")
            || "SHELL".equals(varKey) || "USER".equals(varKey) || "LOGNAME".equals(varKey)
            || "LANG".equals(varKey) || "PWD".equals(varKey) || "HOME".equals(varKey)
            || "SHLVL".equals(varKey) || "TERM".equals(varKey) || "COLORTERM".equals(varKey)
            || "DISPLAY".equals(varKey) || "OLDPWD".equals(varKey))
        {
            return false;
        }

        return true;
    }

    /**
     * Safely replaces target string with replacement without modifying text inside existing ${...} placeholders.
     */
    private static String replaceOutsidePlaceholders(final String text, final String target, final String replacement)
    {
        if (text == null || target == null || target.isEmpty())
        {
            return text;
        }

        final StringBuilder result = new StringBuilder();
        int i = 0;
        final int len = text.length();
        final int targetLen = target.length();

        while (i < len)
        {
            if (text.startsWith("${", i))
            {
                final int closeIdx = text.indexOf('}', i + 2);
                if (closeIdx != -1)
                {
                    result.append(text, i, closeIdx + 1);
                    i = closeIdx + 1;
                    continue;
                }
            }

            if (text.startsWith(target, i))
            {
                result.append(replacement);
                i += targetLen;
            }
            else
            {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
}
