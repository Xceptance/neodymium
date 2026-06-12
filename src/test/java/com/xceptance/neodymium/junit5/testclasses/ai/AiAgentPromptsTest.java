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
        Assertions.assertTrue(prompt.contains("## Critical Rules"));
        Assertions.assertTrue(prompt.contains("## Rules"));
        Assertions.assertTrue(prompt.contains("Element Selection & Targeting Rules"));

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
        Assertions.assertTrue(prompt.contains("## Critical Rules"));
        Assertions.assertTrue(prompt.contains("## Rules"));
        Assertions.assertTrue(prompt.contains("Element Selection & Targeting Rules"));

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
        Assertions.assertTrue(prompt.contains("## Critical Rules"));
        Assertions.assertTrue(prompt.contains("## Rules"));
        Assertions.assertTrue(prompt.contains("Element Selection & Targeting Rules"));
        Assertions.assertTrue(prompt.contains("## Rules for Visual Analysis"));
    }

    /**
     * Checks that the system prompts generated for each ContextLevel do not exceed
     * the maximum length we expect. Keep the prompts lean.
     */
    @Test
    public final void testSystemPromptMaxLength()
    {
        final int maxHintLength = 1800;
        final int maxAxTreeLength = 11500;
        final int maxLeanLength = 11500;
        final int maxStandardLength = 11500;
        final int maxVisualLeanLength = 13000;
        final int maxVisualLength = 13000;

        final String hintPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.HINT);
        final String axTreePrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.AXTREE);
        final String leanPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.LEAN);
        final String standardPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.STANDARD);
        final String visualLeanPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.VISUAL_LEAN);
        final String visualPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.VISUAL);

        System.out.println("HINT prompt length: " + hintPrompt.length());
        System.out.println("AXTREE prompt length: " + axTreePrompt.length());
        System.out.println("LEAN prompt length: " + leanPrompt.length());
        System.out.println("STANDARD prompt length: " + standardPrompt.length());
        System.out.println("VISUAL_LEAN prompt length: " + visualLeanPrompt.length());
        System.out.println("VISUAL prompt length: " + visualPrompt.length());

        Assertions.assertTrue(hintPrompt.length() < maxHintLength, "HINT prompt exceeds limit: " + hintPrompt.length());
        Assertions.assertTrue(axTreePrompt.length() < maxAxTreeLength, "AXTREE prompt exceeds limit: " + axTreePrompt.length());
        Assertions.assertTrue(leanPrompt.length() < maxLeanLength, "LEAN prompt exceeds limit: " + leanPrompt.length());
        Assertions.assertTrue(standardPrompt.length() < maxStandardLength, "STANDARD prompt exceeds limit: " + standardPrompt.length());
        Assertions.assertTrue(visualLeanPrompt.length() < maxVisualLeanLength, "VISUAL_LEAN prompt exceeds limit: " + visualLeanPrompt.length());
        Assertions.assertTrue(visualPrompt.length() < maxVisualLength, "VISUAL prompt exceeds limit: " + visualPrompt.length());
    }

    /**
     * Verifies that clearCache() works correctly.
     */
    @Test
    public final void testClearCache()
    {
        final String firstPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.HINT);
        Assertions.assertNotNull(firstPrompt);

        AiAgentPrompts.clearCache();

        final String secondPrompt = AiAgentPrompts.getSystemPrompt(ContextLevel.HINT);
        Assertions.assertEquals(firstPrompt, secondPrompt);
    }
}
