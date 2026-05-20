/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.junit5.testclasses.ai;

// AI-generated: Claude Opus 4.6

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.core.AiAgentPrompts;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;

/**
 * Unit tests for {@link AiAgentPrompts#buildStepHistory(Playbook)}.
 * Verifies that step history is correctly built from completed playbook steps
 * and returns empty string when no history is available.
 */
public class AiAgentPromptsHistoryTest
{
    @Test
    public void testNullPlaybook()
    {
        final String result = AiAgentPrompts.buildStepHistory(null);
        Assertions.assertEquals("", result, "Null playbook should return empty string");
    }

    @Test
    public void testEmptyPlaybook()
    {
        final Playbook playbook = new Playbook("test");
        // Cursor defaults to 0, no steps
        final String result = AiAgentPrompts.buildStepHistory(playbook);
        Assertions.assertEquals("", result, "Empty playbook should return empty string");
    }

    @Test
    public void testCursorAtZero()
    {
        final Playbook playbook = new Playbook("test");
        final PlaybookStep step = new PlaybookStep();
        step.setPromptLine("Click the login button");
        playbook.getSteps().add(step);
        // Cursor is still at 0 — no completed steps yet

        final String result = AiAgentPrompts.buildStepHistory(playbook);
        Assertions.assertEquals("", result, "Cursor at 0 means no completed steps");
    }

    @Test
    public void testSingleCompletedStep()
    {
        final Playbook playbook = new Playbook("test");
        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Open homepage");
        playbook.getSteps().add(step1);
        playbook.setCursor(1); // Step 0 is completed

        final String result = AiAgentPrompts.buildStepHistory(playbook);
        Assertions.assertTrue(result.contains("### Completed Steps (for context)"),
                "Should contain the history header");
        Assertions.assertTrue(result.contains("1. Open homepage"),
                "Should contain the step instruction");
        Assertions.assertTrue(result.contains("[CURRENT]"),
                "Should contain the current step marker");
    }

    @Test
    public void testMultipleCompletedSteps()
    {
        final Playbook playbook = new Playbook("test");

        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Open homepage");
        playbook.getSteps().add(step1);

        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Type \"running shoes\" into search field");
        playbook.getSteps().add(step2);

        final PlaybookStep step3 = new PlaybookStep();
        step3.setPromptLine("Click the first product result");
        playbook.getSteps().add(step3);

        playbook.setCursor(3); // All 3 completed

        final String result = AiAgentPrompts.buildStepHistory(playbook);
        Assertions.assertTrue(result.contains("1. Open homepage"), "Should have step 1");
        Assertions.assertTrue(result.contains("2. Type \"running shoes\" into search field"), "Should have step 2");
        Assertions.assertTrue(result.contains("3. Click the first product result"), "Should have step 3");
        Assertions.assertTrue(result.contains("[CURRENT]"), "Should have current marker");
    }

    @Test
    public void testCursorInMiddleOfPlaybook()
    {
        final Playbook playbook = new Playbook("test");

        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Open homepage");
        playbook.getSteps().add(step1);

        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("Search for products");
        playbook.getSteps().add(step2);

        final PlaybookStep step3 = new PlaybookStep();
        step3.setPromptLine("Click Add to Cart");
        playbook.getSteps().add(step3);

        final PlaybookStep step4 = new PlaybookStep();
        step4.setPromptLine("Go to checkout");
        playbook.getSteps().add(step4);

        final PlaybookStep step5 = new PlaybookStep();
        step5.setPromptLine("Fill in payment");
        playbook.getSteps().add(step5);

        playbook.setCursor(3); // Only first 3 completed, 2 remaining

        final String result = AiAgentPrompts.buildStepHistory(playbook);
        Assertions.assertTrue(result.contains("1. Open homepage"), "Should have step 1");
        Assertions.assertTrue(result.contains("2. Search for products"), "Should have step 2");
        Assertions.assertTrue(result.contains("3. Click Add to Cart"), "Should have step 3");
        Assertions.assertFalse(result.contains("Go to checkout"),
                "Future steps should NOT be included");
        Assertions.assertFalse(result.contains("Fill in payment"),
                "Future steps should NOT be included");
    }

    @Test
    public void testNullPromptLinesSkipped()
    {
        final Playbook playbook = new Playbook("test");

        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Open homepage");
        playbook.getSteps().add(step1);

        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine(null); // No prompt line
        playbook.getSteps().add(step2);

        final PlaybookStep step3 = new PlaybookStep();
        step3.setPromptLine("Click button");
        playbook.getSteps().add(step3);

        playbook.setCursor(3);

        final String result = AiAgentPrompts.buildStepHistory(playbook);
        Assertions.assertTrue(result.contains("1. Open homepage"), "Should have step 1");
        Assertions.assertTrue(result.contains("2. Click button"),
                "Step with null promptLine should be skipped, numbering continues");
        Assertions.assertFalse(result.contains("3."),
                "Should only have 2 numbered entries since one was null");
    }

    @Test
    public void testBlankPromptLinesSkipped()
    {
        final Playbook playbook = new Playbook("test");

        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Open homepage");
        playbook.getSteps().add(step1);

        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine("   "); // Blank prompt line
        playbook.getSteps().add(step2);

        playbook.setCursor(2);

        final String result = AiAgentPrompts.buildStepHistory(playbook);
        Assertions.assertTrue(result.contains("1. Open homepage"), "Should have step 1");
        Assertions.assertFalse(result.contains("2."),
                "Blank prompt line should be skipped");
    }

    @Test
    public void testAllNullPromptLinesReturnsEmpty()
    {
        final Playbook playbook = new Playbook("test");

        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine(null);
        playbook.getSteps().add(step1);

        final PlaybookStep step2 = new PlaybookStep();
        step2.setPromptLine(null);
        playbook.getSteps().add(step2);

        playbook.setCursor(2);

        final String result = AiAgentPrompts.buildStepHistory(playbook);
        Assertions.assertEquals("", result,
                "If all completed steps have null promptLines, return empty");
    }

    @Test
    public void testCursorBeyondStepsCount()
    {
        final Playbook playbook = new Playbook("test");

        final PlaybookStep step1 = new PlaybookStep();
        step1.setPromptLine("Open homepage");
        playbook.getSteps().add(step1);

        // Cursor set beyond the actual number of steps
        playbook.setCursor(5);

        final String result = AiAgentPrompts.buildStepHistory(playbook);
        Assertions.assertTrue(result.contains("1. Open homepage"),
                "Should gracefully handle cursor beyond steps count");
        Assertions.assertFalse(result.contains("2."),
                "Should not have phantom steps");
    }
}
