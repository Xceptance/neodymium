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

import java.util.Map;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Utility helper to resolve and append custom system prompt add-ons.
 * Supports resolution from test datasets (prioritized) and YAML playbook files,
 * with capability-specific targeting (e.g. pesap, general, verification).
 * Enforces safety controls (max character limits and strict reminder suffixes).
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SystemPromptAddonHelper
{
    private static final int MAX_ADDON_LENGTH = 2000;

    private static final String ENFORCEMENT_SUFFIX = "\n\nCRITICAL REMINDER: The above rules are custom extensions for this test step. You MUST still strictly follow all JSON schema formatting rules, action capabilities, and output guidelines specified in the main system prompt above.";

    private SystemPromptAddonHelper()
    {
        // utility class
    }

    /**
     * Resolves the custom system prompt add-on value based on precedence rules.
     * Precedence:
     * 1. Test Dataset Layer (via SessionData):
     *    - Specific key: "systemPromptAddon.<type>"
     *    - General key: "systemPromptAddon"
     * 2. YAML Playbook Layer:
     *    - Specific key: "<type>"
     *    - General key: "default"
     *
     * @param type the type of prompt/LLM query (e.g., pesap, general, verification)
     * @param context the active execution context
     * @return the resolved add-on string, or null if none
     * @throws IllegalArgumentException if the resolved add-on exceeds the 2000-character length limit
     */
    @SuppressWarnings("unchecked")
    public static String getAddon(final String type, final ExecutionContext context)
    {
        if (context == null)
        {
            return null;
        }

        // 1. Resolve from dataset/SessionData
        final SessionData sessionData = context.getSessionData();
        if (sessionData != null)
        {
            // Check specific key first: systemPromptAddon.<type>
            final SessionData.DataEntry specificEntry = sessionData.getEntry("systemPromptAddon." + type);
            if (specificEntry != null && specificEntry.value() != null)
            {
                final String val = String.valueOf(specificEntry.value());
                validateLength(val);
                return val;
            }
            // Fallback to general key: systemPromptAddon
            final SessionData.DataEntry generalEntry = sessionData.getEntry("systemPromptAddon");
            if (generalEntry != null && generalEntry.value() != null)
            {
                final String val = String.valueOf(generalEntry.value());
                validateLength(val);
                return val;
            }
        }

        // 2. Resolve from YAML file/Playbook
        final Map<String, String> yamlAddons = (Map<String, String>) context.getTransientData().get("playbook.systemPromptAddons");
        if (yamlAddons != null)
        {
            // Check specific key first
            final String specific = yamlAddons.get(type);
            if (specific != null && !specific.trim().isEmpty())
            {
                final String val = specific.trim();
                validateLength(val);
                return val;
            }
            // Fallback to general key
            final String general = yamlAddons.get("default");
            if (general != null && !general.trim().isEmpty())
            {
                final String val = general.trim();
                validateLength(val);
                return val;
            }
        }

        // 3. Resolve model-family default add-on
        final String activeModel = resolveActiveModel(context);
        if (activeModel != null && activeModel.toLowerCase().contains("lite") && "general".equalsIgnoreCase(type))
        {
            final String val = "CRITICAL FOR LITE MODEL LOCATORS: DOM dump element tags represent real HTML tags (<p>, <div>, <span>, <h1>, <button>, <input>, <link>). Never invent synthetic tag names or pseudotags (such as 'text' or 'text:nth-of-type(N)') in locators. If a target element lacks a direct class or id attribute, select its parent element (e.g., 'div:has(...)') or set 'status' to 'ESCALATE' to request visual context.";
            validateLength(val);
            return val;
        }

        return null;
    }

    private static String resolveActiveModel(final ExecutionContext context)
    {
        if (context != null)
        {
            final Object modelObj = context.getTransientData().get(ExecutionContext.KEY_ACTIVE_MODEL);
            if (modelObj != null && !modelObj.toString().trim().isEmpty())
            {
                return modelObj.toString().trim();
            }
        }
        try
        {
            return com.xceptance.neodymium.util.Neodymium.aiConfiguration().aiModel();
        }
        catch (final Exception e)
        {
            return null;
        }
    }

    /**
     * Appends the resolved custom system prompt add-on to the system prompt.
     * Automatically enforces safety controls by adding the critical adherence reminder.
     *
     * @param systemPrompt the base system prompt
     * @param type the type of prompt/LLM query (e.g., pesap, general, verification)
     * @param context the active execution context
     * @return the combined system prompt, or the original system prompt if no add-on is resolved
     */
    public static String appendAddon(final String systemPrompt, final String type, final ExecutionContext context)
    {
        final String addon = getAddon(type, context);
        if (addon == null || addon.trim().isEmpty())
        {
            return systemPrompt;
        }

        final String base = systemPrompt != null ? systemPrompt.trim() : "";
        return base + "\n\n### Custom System Add-on Prompt\n\n" + addon.trim() + ENFORCEMENT_SUFFIX;
    }

    private static void validateLength(final String value)
    {
        if (value != null && value.length() > MAX_ADDON_LENGTH)
        {
            throw new IllegalArgumentException("Custom system prompt add-on exceeds the maximum limit of " + MAX_ADDON_LENGTH + " characters (length: " + value.length() + ").");
        }
    }
}
