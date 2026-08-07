/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import org.neodymium.util.Neodymium;

/**
 * Contains prompt loaders for the AI agent.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AiAgentPrompts
{
    private static final ConcurrentHashMap<String, String> PROMPT_CACHE = new ConcurrentHashMap<>();

    private AiAgentPrompts()
    {
        // Prevent instantiation
    }

    /**
     * Clears the cached prompt contents, forcing them to be reloaded on the next access.
     */
    public static void clearCache()
    {
        PROMPT_CACHE.clear();
    }

    private static String getPrompt(final String filename)
    {
        return PROMPT_CACHE.computeIfAbsent(filename, AiAgentPrompts::loadPrompt);
    }

    private static String loadPrompt(final String filename)
    {
        final String resourcePath = "ai-prompts/" + filename;
        try (final InputStream is = AiAgentPrompts.class.getClassLoader().getResourceAsStream(resourcePath))
        {
            if (is != null)
            {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            else
            {
                throw new RuntimeException("Could not find prompt file on classpath: " + resourcePath);
            }
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Failed to load prompt file: " + resourcePath, e);
        }
    }

    /**
     * Loads the JIT pre-step PESAP prompt template.
     *
     * @return the fully prepared pre-step PESAP system prompt
     */
    public static String getPesapPreStepPrompt()
    {
        return getPrompt("pesap-pre-step-prompt.md");
    }

    /**
     * Loads the action extraction system prompt template based on global configuration.
     *
     * @return the action extraction system prompt
     */
    public static String getActionExtractionPrompt()
    {
        return getActionExtractionPrompt(Neodymium.aiConfiguration().isEmbeddedJudgingEnabled());
    }

    /**
     * Loads the action extraction system prompt template.
     *
     * @param embeddedJudgingEnabled true to load judging prompt, false to load non-judging prompt
     * @return the action extraction system prompt
     */
    public static String getActionExtractionPrompt(final boolean embeddedJudgingEnabled)
    {
        return embeddedJudgingEnabled
            ? getPrompt("action-extraction-prompt-judging.md")
            : getPrompt("action-extraction-prompt-non-judging.md");
    }
}
