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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LlmResponseSanitizer}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmResponseSanitizerTest
{
    @Test
    public void testCleanJsonObject()
    {
        final String input = "{\"status\": \"SUCCESS\"}";
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertEquals("{\"status\": \"SUCCESS\"}", actual);
    }

    @Test
    public void testMarkdownWrappedJson()
    {
        final String input = "```json\n{\"status\": \"SUCCESS\"}\n```";
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertEquals("{\"status\": \"SUCCESS\"}", actual);
    }

    @Test
    public void testMalformedLeadingLabelHeader()
    {
        final String input = "{\\label} : country_selector_click\n{\n  \"status\": \"SUCCESS\"\n}";
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertEquals("{\n  \"status\": \"SUCCESS\"\n}", actual);
    }

    @Test
    public void testJsonArray()
    {
        final String input = "Here is the result:\n[1, 2, 3]";
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertEquals("[1, 2, 3]", actual);
    }

    @Test
    public void testNullOrBlank()
    {
        Assertions.assertEquals("", LlmResponseSanitizer.extractJson(null));
        Assertions.assertEquals("", LlmResponseSanitizer.extractJson("   "));
    }

    @Test
    public void testMultipleCodeBlocksPicksLastValidBlock()
    {
        final String input = """
            Here is my initial thought draft:
            ```json
            {
              "action": "ASSERT",
              "locator": "#country-trigger-btn"
            }
            ```
            Now let me format the final response:
            ```json
            {
              "reasoning": "German flag is visible",
              "status": "SUCCESS",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#country-trigger-btn",
                  "value": "🇩🇪"
                }
              ]
            }
            ```
            """;
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertTrue(actual.contains("\"reasoning\": \"German flag is visible\""));
        Assertions.assertTrue(actual.contains("\"status\": \"SUCCESS\""));
    }

    @Test
    public void testUnfencedTrailingJsonPicksLast()
    {
        final String input = """
            Draft in thinking:
            ```json
            {"draft": true}
            ```
            The final verdict:
            {
              "finalResult": "PASSED"
            }
            """;
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertTrue(actual.contains("\"finalResult\": \"PASSED\""));
    }

    @Test
    public void testThinkingMonologueWithDraftsAndFinalEnvelope() 
    {
        final String input = """
            **Flag Verification - Asserting the Presence of 🇩🇪**

            Okay, so the instruction is crystal clear: verify that the German flag, 🇩🇪, is displayed. This is a straightforward assertion. I need to generate an `ASSERT` action. Let's look at the DOM, specifically this element: `<button id="country-trigger-btn" ...>🇩🇪</button>`. Perfect, the flag is right there within the button's text content.

            Now, according to my rules for assertions, I need to:
            1. Emit an `ASSERT` action. Check.
            2. Target the specific UI component mentioned. That's `#country-trigger-btn`. Got it.

            Time to construct the action:
            ```json
            {
              "action": "ASSERT",
              "locator": "#country-trigger-btn",
              "value": "🇩🇪",
              "isRegex": false,
              "reasoning": "Verify that the German flag '🇩🇪' is displayed on the country trigger button."
            }
            ```

            Now, let's verify that the structure is good, and confirm the expected JSON:

            ```json
            {
              "reasoning": "The country flag '🇩🇪' is present inside the button with id 'country-trigger-btn'.",
              "assertionSatisfied": true,
              "status": "SUCCESS",
              "targetContextLevel": "STANDARD",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#country-trigger-btn",
                  "value": "🇩🇪",
                  "isRegex": false,
                  "reasoning": "Verify that the country flag '🇩🇪' is displayed."
                }
              ]
            }
            ```

            The assertion is now fully formed, ready to be executed and verified. No problems. I'm all set.
            """;
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertTrue(actual.contains("\"actions\""));
        Assertions.assertTrue(actual.contains("\"assertionSatisfied\": true"));
        Assertions.assertTrue(actual.contains("\"status\": \"SUCCESS\""));
    }

    @Test
    public void testActionDraftWithTrailingUnfencedJson()
    {
        final String input = """
            **My Thought Process: Selecting Germany**

            Okay, the goal is to select 'Germany'. First, I see a country trigger button in the DOM: `<button id="country-trigger-btn" class="utility-btn" aria-label="Select Country" data-ai="xchiu8od">🇺🇸</button>`. My initial instinct is to click that. But, before I leap, let's confirm. Does clicking this button actually open a selector (dropdown or modal) with the country options?

            So, let's craft the JSON response:

            ```json
            {
              "reasoning": "Click the country trigger button to open the country selection modal or dropdown.",
              "assertionSatisfied": false,
              "status": "SUCCESS",
              "targetContextLevel": "LEAN",
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#country-trigger-btn",
                  "value": "",
                  "isRegex": false,
                  "reasoning": "Click the country selector button in the header to open country choices."
                }
              ]
            }
            ```

            Okay, I've confirmed that this is correct. I have considered everything and it is now ready to be output.




            {
              "reasoning": "Click the country trigger button in the header to open the country selection menu/modal.",
              "assertionSatisfied": false,
              "status": "SUCCESS2",
              "targetContextLevel": "LEAN",
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#country-trigger-btn",
                  "value": "",
                  "isRegex": false,
                  "reasoning": "Click the country trigger button to display country options."
                }
              ]
            }
            """;
        final String actual = LlmResponseSanitizer.extractJson(input);
        Assertions.assertTrue(actual.contains("\"actions\""));
        Assertions.assertTrue(actual.contains("\"status\": \"SUCCESS2\""));
        Assertions.assertTrue(actual.contains("Click the country trigger button in the header to open the country selection menu/modal."));
    }
}
