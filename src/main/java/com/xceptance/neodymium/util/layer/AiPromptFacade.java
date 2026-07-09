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
package com.xceptance.neodymium.util.layer;

import com.xceptance.neodymium.ai.core.ContextLevel;

/**
 * Facade abstracting all AI prompt-related instructions and representations.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public interface AiPromptFacade
{
    /**
     * Returns the role prompt snippet for the AI agent.
     *
     * @return AI role prompt string
     */
    default String getAiRolePrompt()
    {
        return "You are an AI browser test automation agent. Your job is to translate natural language test instructions into concrete browser actions.";
    }

    /**
     * Returns any targeting/selection rules specific to this interaction layer.
     *
     * @return AI targeting rules string, or empty/null
     */
    default String getAiTargetingRules()
    {
        return "";
    }

    /**
     * Returns instructions on how the LLM should format/use the CLICK action.
     *
     * @return CLICK instructions string
     */
    default String getClickPromptInstructions()
    {
        return "CLICK: Click on a target element (requires 'tg').";
    }

    /**
     * Returns instructions on how the LLM should format/use the TYPE action.
     *
     * @return TYPE instructions string
     */
    default String getTypePromptInstructions()
    {
        return "TYPE: Type text into a target input field (requires 'tg' and 'v'). Automatically clears the field first.";
    }

    /**
     * Returns instructions on how the LLM should format/use the HOVER action.
     *
     * @return HOVER instructions string
     */
    default String getHoverPromptInstructions()
    {
        return "HOVER: Hover the mouse pointer over a target element (requires 'tg').";
    }

    /**
     * Returns instructions on how the LLM should format/use the NAVIGATE action.
     *
     * @return NAVIGATE instructions string
     */
    default String getNavigatePromptInstructions()
    {
        return "NAVIGATE: Navigate to a URL. Set 'v' to the URL. For Basic Authentication, set 'tg' to the URL and 'v' to a JSON array containing the username and password (e.g. [\"user\", \"pass\"]).";
    }

    /**
     * Returns instructions on how the LLM should format/use the ASSERT action.
     *
     * @return ASSERT instructions string
     */
    default String getAssertPromptInstructions()
    {
        return "ASSERT: Verify element state (set 'tg' to the locator, and 'v' to 'visible', 'focused', 'hidden'/'absent', or expected text content) or verify the current page URL (set 'tg' to 'url', and 'v' to the expected URL substring).";
    }

    /**
     * Returns the structured source representation of the page/screen under the given context level.
     *
     * @param level context level controlling how much detail to capture
     * @return the structured representation, or {@code null} if standard HTML DOM extraction should be used
     */
    default String getPageSourceRepresentation(final ContextLevel level)
    {
        return null;
    }
}
