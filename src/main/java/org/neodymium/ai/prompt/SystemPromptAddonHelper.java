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

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.neodymium.ai.config.AiConfiguration;
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

    private static final String MULTILINGUAL_PESAP_ADDON =
        "Language Universality: Instructions may be written in any natural language. Apply all semantic splitting, context level, and non-splitting rules to equivalent phrasing in the given language. Always preserve the original language in generated sub-steps.";

    private static final String MULTILINGUAL_GENERAL_ADDON =
        "Language Universality: The System Under Test and test step instructions may be localized in any language. When evaluating button texts, labels, links, and assertions, apply all action and assertion rules to the localized equivalents in the page DOM.";

    private static final String MULTILINGUAL_VERIFICATION_ADDON =
        "Language Universality: The System Under Test and test step instructions may be localized in any language. Verify outcomes against localized content and labels accordingly.";

    private static final String MULTILINGUAL_VISUAL_ADDON =
        "Language Universality: The System Under Test and test step instructions may be localized in any language. When evaluating visual appearance, text, labels, and assertions on the screenshot, apply all rules to the localized equivalents.";

    /**
     * Resolves and accumulates custom prompt add-ons across all active layers:
     * 1. Multilingual layer (if enabled via neodymium.ai.multilingual)
     * 2. Model / Disk layer (model-specific general + capability-targeted)
     * 3. YAML Playbook layer (general + capability-targeted)
     * 4. Test Dataset layer (general + capability-targeted)
     *
     * Resolves dynamic variable placeholders (${variableName}) against active session data.
     *
     * @param type the type of prompt/LLM query (e.g., pesap, general, verification)
     * @param context the active execution context
     * @return the combined resolved add-on string, or null if none
     * @throws IllegalArgumentException if the combined resolved add-on exceeds the 2000-character length limit
     */
    @SuppressWarnings("unchecked")
    public static String getAddon(final String type, final ExecutionContext context)
    {
        if (context == null)
        {
            return null;
        }

        final List<String> segments = new ArrayList<>();
        final SessionData sessionData = context.getSessionData();

        // 0. Multilingual layer
        if (isMultilingualEnabled(context, sessionData))
        {
            final String multiAddon = getMultilingualAddon(type);
            if (multiAddon != null && !multiAddon.isBlank())
            {
                segments.add(multiAddon);
            }
        }

        // 1. Model / Disk layer
        collectModelAddons(type, context, sessionData, segments);

        // 2. YAML Playbook layer
        final Map<String, String> yamlAddons = (Map<String, String>) context.getTransientData().get("playbook.promptAddons");
        collectPlaybookAddons(type, yamlAddons, sessionData, segments);

        // 3. Dataset layer
        collectDatasetAddons(type, sessionData, segments);

        if (segments.isEmpty())
        {
            return null;
        }

        final String combined = String.join("\n\n", segments);
        validateLength(combined);
        return combined;
    }

    private static boolean isMultilingualEnabled(final ExecutionContext context, final SessionData sessionData)
    {
        if (sessionData != null)
        {
            final Object sessionProp = sessionData.get("neodymium.ai.multilingual");
            if (sessionProp != null)
            {
                return Boolean.parseBoolean(sessionProp.toString().trim());
            }
        }
        return AiConfiguration.getInstance().isMultilingual();
    }

    private static String getMultilingualAddon(final String type)
    {
        if (type == null || "general".equalsIgnoreCase(type) || "default".equalsIgnoreCase(type))
        {
            return MULTILINGUAL_GENERAL_ADDON;
        }
        if ("pesap".equalsIgnoreCase(type))
        {
            return MULTILINGUAL_PESAP_ADDON;
        }
        if ("verification".equalsIgnoreCase(type))
        {
            return MULTILINGUAL_VERIFICATION_ADDON;
        }
        if ("visual".equalsIgnoreCase(type))
        {
            return MULTILINGUAL_VISUAL_ADDON;
        }
        return null;
    }

    private static void collectModelAddons(
        final String type,
        final ExecutionContext context,
        final SessionData sessionData,
        final List<String> segments)
    {
        final String activeModel = resolveActiveModel(context);
        if (activeModel == null || activeModel.isBlank())
        {
            return;
        }

        final String cleanModel = activeModel.trim().toLowerCase().replaceAll("[^a-z0-9_\\-]", "-");

        final boolean isGeneral = type == null || "general".equalsIgnoreCase(type)
            || "action".equalsIgnoreCase(type) || "default".equalsIgnoreCase(type);

        if (isGeneral)
        {
            // General model add-on (for action extraction / execution prompts)
            final String modelGeneral = loadModelAddon(cleanModel, null);
            if (modelGeneral != null && !modelGeneral.isBlank())
            {
                addSegment(modelGeneral, sessionData, segments);
            }
        }
        else
        {
            // Specific capability model add-on (e.g. addon-pesap.md, addon-verification.md)
            final String modelSpecific = loadModelAddon(cleanModel, type);
            if (modelSpecific != null && !modelSpecific.isBlank())
            {
                addSegment(modelSpecific, sessionData, segments);
            }
        }
    }

    private static void collectPlaybookAddons(
        final String type,
        final Map<String, String> yamlAddons,
        final SessionData sessionData,
        final List<String> segments)
    {
        if (yamlAddons == null || yamlAddons.isEmpty())
        {
            return;
        }

        // 1. Playbook general
        String general = yamlAddons.get("default");
        if (general == null || general.isBlank())
        {
            general = yamlAddons.get("general");
        }
        if (general != null && !general.isBlank())
        {
            addSegment(general, sessionData, segments);
        }

        // 2. Playbook targeted (if type is not general/default)
        if (type != null && !type.equalsIgnoreCase("general") && !type.equalsIgnoreCase("default"))
        {
            final String specific = yamlAddons.get(type);
            if (specific != null && !specific.isBlank())
            {
                addSegment(specific, sessionData, segments);
            }
        }
    }

    private static void collectDatasetAddons(
        final String type,
        final SessionData sessionData,
        final List<String> segments)
    {
        if (sessionData == null)
        {
            return;
        }

        // 1. Dataset general
        final String generalVal = resolveDatasetEntry(
            sessionData,
            "promptAddon",
            "promptAddon.general");
        if (generalVal != null && !generalVal.isBlank())
        {
            addSegment(generalVal, sessionData, segments);
        }

        // 2. Dataset targeted (if type is not general/default)
        if (type != null && !type.equalsIgnoreCase("general") && !type.equalsIgnoreCase("default"))
        {
            final String specificVal = resolveDatasetEntry(
                sessionData,
                "promptAddon." + type);
            if (specificVal != null && !specificVal.isBlank())
            {
                addSegment(specificVal, sessionData, segments);
            }
        }
    }

    private static void addSegment(
        final String rawText,
        final SessionData sessionData,
        final List<String> segments)
    {
        if (rawText == null || rawText.isBlank())
        {
            return;
        }

        String resolved = rawText.trim();
        if (sessionData != null && resolved.contains("${"))
        {
            resolved = sessionData.resolveVariables(resolved).trim();
        }

        if (!resolved.isBlank() && !segments.contains(resolved))
        {
            segments.add(resolved);
        }
    }

    private static String resolveDatasetEntry(final SessionData sessionData, final String... keys)
    {
        for (final String key : keys)
        {
            final SessionData.DataEntry entry = sessionData.getEntry(key);
            if (entry != null && entry.value() != null)
            {
                final String val = String.valueOf(entry.value()).trim();
                if (!val.isEmpty())
                {
                    return val;
                }
            }
        }
        return null;
    }

    private static String loadModelAddon(final String cleanModel, final String type)
    {
        final List<String> candidatePaths = new ArrayList<>();
        if (type != null && !type.isBlank())
        {
            candidatePaths.add("config/ai-prompts/models/" + cleanModel + "/addon-" + type + ".md");
            candidatePaths.add("ai-prompts/models/" + cleanModel + "/addon-" + type + ".md");
            if ("general".equalsIgnoreCase(type) || "default".equalsIgnoreCase(type))
            {
                candidatePaths.add("config/ai-prompts/models/" + cleanModel + "/addon.md");
                candidatePaths.add("ai-prompts/models/" + cleanModel + "/addon.md");
            }
        }
        else
        {
            candidatePaths.add("config/ai-prompts/models/" + cleanModel + "/addon-general.md");
            candidatePaths.add("ai-prompts/models/" + cleanModel + "/addon-general.md");
            candidatePaths.add("config/ai-prompts/models/" + cleanModel + "/addon.md");
            candidatePaths.add("ai-prompts/models/" + cleanModel + "/addon.md");
        }

        for (final String path : candidatePaths)
        {
            if (path.startsWith("config/"))
            {
                final File file = new File(path);
                if (file.exists() && file.isFile())
                {
                    try
                    {
                        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    }
                    catch (final Exception e)
                    {
                        // ignore and continue
                    }
                }
            }
            else
            {
                try (final InputStream is = SystemPromptAddonHelper.class.getClassLoader().getResourceAsStream(path))
                {
                    if (is != null)
                    {
                        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                catch (final Exception e)
                {
                    // ignore and continue
                }
            }
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
        return null;
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
        if (base.contains(ENFORCEMENT_SUFFIX.trim()))
        {
            return base;
        }
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
