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
// AI-generated: Gemini 3.5 Flash
package com.xceptance.neodymium.junit5.testclasses.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.core.AiAgentPrompts;
import com.xceptance.neodymium.ai.core.ContextLevel;

/**
 * Unit tests verifying that the dynamic prompt composition in {@link AiAgentPrompts}
 * correctly includes or excludes snippets based on the requested {@link ContextLevel}.
 */
public final class AiAgentPromptsTest
{
    /**
     * Verifies that the HINT prompt is minimal and contains only role, capabilities,
     * response format, and HINT guidance, while omitting visual/DOM rules.
     */
    @Test
    public final void testHintPromptComposition()
    {
        final String prompt = AiAgentPrompts.getSystemPrompt(ContextLevel.HINT);

        Assertions.assertTrue(prompt.contains("Context Level: HINT"));
        Assertions.assertTrue(prompt.contains("You are an AI browser test automation agent"));
        Assertions.assertTrue(prompt.contains("## Your Capabilities"));
        Assertions.assertTrue(prompt.contains("## Response Format"));
        Assertions.assertTrue(prompt.contains("## Rules"));
        Assertions.assertTrue(prompt.contains("Set \"d\" to true"));

        // Should not contain verbose plugin descriptions like JavaMethod or KeyPress instructions
        Assertions.assertFalse(prompt.contains("Invoke a Java method"));
        Assertions.assertFalse(prompt.contains("send key presses to an element"));

        // Should contain the list of supported action types
        Assertions.assertTrue(prompt.contains("CLICK"));
        Assertions.assertTrue(prompt.contains("TYPE"));
        Assertions.assertTrue(prompt.contains("ASSERT"));

        // Should not contain DOM rules or visual screenshot rules
        Assertions.assertFalse(prompt.contains("## Rules for Visual Analysis"));
        Assertions.assertFalse(prompt.contains("Important Guidelines for Element Selection"));
    }

    /**
     * Verifies that the AXTREE prompt contains success/failure and DOM rules, but
     * omits visual/screenshot rules and element selection guidelines.
     */
    @Test
    public final void testAxTreePromptComposition()
    {
        final String prompt = AiAgentPrompts.getSystemPrompt(ContextLevel.AXTREE);

        Assertions.assertTrue(prompt.contains("Context Level: AXTREE"));
        Assertions.assertTrue(prompt.contains("## Critical Rules for success/failure"));
        Assertions.assertTrue(prompt.contains("## Rules"));
        Assertions.assertTrue(prompt.contains("Important Guidelines for Element Selection"));

        // Should not contain visual screenshot rules
        Assertions.assertFalse(prompt.contains("## Rules for Visual Analysis"));
    }

    /**
     * Verifies that the LEAN prompt contains success/failure, DOM rules, and element
     * selection guidelines, but omits visual rules.
     */
    @Test
    public final void testLeanPromptComposition()
    {
        final String prompt = AiAgentPrompts.getSystemPrompt(ContextLevel.LEAN);

        Assertions.assertTrue(prompt.contains("Context Level: LEAN"));
        Assertions.assertTrue(prompt.contains("## Critical Rules for success/failure"));
        Assertions.assertTrue(prompt.contains("## Rules"));
        Assertions.assertTrue(prompt.contains("Important Guidelines for Element Selection"));

        // Should not contain visual rules
        Assertions.assertFalse(prompt.contains("## Rules for Visual Analysis"));
    }

    /**
     * Verifies that the VISUAL prompt contains all snippets, including visual rules.
     */
    @Test
    public final void testVisualPromptComposition()
    {
        final String prompt = AiAgentPrompts.getSystemPrompt(ContextLevel.VISUAL);

        Assertions.assertTrue(prompt.contains("Context Level: VISUAL"));
        Assertions.assertTrue(prompt.contains("## Critical Rules for success/failure"));
        Assertions.assertTrue(prompt.contains("## Rules"));
        Assertions.assertTrue(prompt.contains("Important Guidelines for Element Selection"));
        Assertions.assertTrue(prompt.contains("## Rules for Visual Analysis"));
    }
}
