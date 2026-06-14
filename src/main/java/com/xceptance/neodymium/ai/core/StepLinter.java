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
package com.xceptance.neodymium.ai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A semantic linter for AI natural language steps. Checks instructions
 * against common anti-patterns (lacking element targeting, missing input values,
 * or vague actions) and suggests improvements before executing the test.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class StepLinter
{
    private static final Pattern TARGETING_PATTERN = Pattern.compile(
        "(?i)\\b(click|press|hover|tap|select)\\s+(?:on\\s+)?(?:the\\s+)?(button|link|element|input|it|that|dropdown)\\b\\s*[.!]*$"
    );

    private static final Pattern INPUT_PATTERN = Pattern.compile(
        "(?i)\\b(type|enter|input|fill|write)\\s+(?:in(?:to)?\\s+)?(?:the\\s+|a\\s+|your\\s+)?(email|password|username|text|something|value|name|address|field|input)\\b\\s*[.!]*$"
    );

    private static final Pattern VAGUE_PATTERN = Pattern.compile(
        "(?i)^(verify|check|test|do|assert)\\s+(?:the\\s+)?(page|it|something|this|layout|everything)\\b\\s*[.!]*$"
    );

    private static final Pattern VAGUE_SOMETHING_PATTERN = Pattern.compile(
        "(?i)^(?:do\\s+)?something\\b\\s*[.!]*$"
    );

    /**
     * Private constructor to prevent instantiation.
     */
    private StepLinter()
    {
    }

    /**
     * Lints a list of natural language instructions and returns descriptive warnings for any issues found.
     * 
     * @param steps the list of natural language steps to check
     * @return a list of descriptive warning messages
     */
    public static List<String> lint(final List<String> steps)
    {
        return lint(steps, null, null);
    }

    /**
     * Lints a list of natural language instructions with optional file name and line numbers, and returns descriptive warnings for any issues found.
     * 
     * @param steps       the list of natural language steps to check
     * @param lineNumbers the line numbers corresponding to each step
     * @param sourceFile  the path of the source file
     * @return a list of descriptive warning messages
     */
    public static List<String> lint(final List<String> steps, final List<Integer> lineNumbers, final String sourceFile)
    {
        final List<String> warnings = new ArrayList<>();
        if (steps == null)
        {
            return warnings;
        }

        for (int i = 0; i < steps.size(); i++)
        {
            final String step = steps.get(i);
            if (step == null)
            {
                continue;
            }

            final String trimmed = step.trim();
            final int stepNumber = i + 1;

            final String stepLabel;
            final Integer currentLineNumber = (lineNumbers != null && i < lineNumbers.size()) ? lineNumbers.get(i) : null;
            if (currentLineNumber != null)
            {
                stepLabel = "Step " + stepNumber + " (line " + currentLineNumber + ")";
            }
            else
            {
                stepLabel = "Step " + stepNumber;
            }

            // 1. Check Lacking Element Targeting
            if (TARGETING_PATTERN.matcher(trimmed).find())
            {
                // Skip if there is an explicit hint/selector tag
                if (!trimmed.contains("(hint:") && !trimmed.contains("(selector:"))
                {
                    warnings.add(String.format(
                        "%s: \"%s\" - Lacks element targeting. Suggest specifying a label/text (e.g., 'click the \"Login\" button') or adding an inline locator hint `(hint: selector)`.",
                        stepLabel, step
                    ));
                }
            }

            // 2. Check Missing Values for Inputs
            if (INPUT_PATTERN.matcher(trimmed).find())
            {
                // If it has no quotes (single or double) and no numbers, it's likely missing the value to type
                if (!trimmed.contains("\"") && !trimmed.contains("'") && !trimmed.matches(".*\\d+.*"))
                {
                    warnings.add(String.format(
                        "%s: \"%s\" - Missing explicit value to input. Suggest specifying the value in quotes (e.g., 'type \"user@example.com\" into the email field').",
                        stepLabel, step
                    ));
                }
            }

            // 3. Check Vague Actions
            if (VAGUE_PATTERN.matcher(trimmed).find() || VAGUE_SOMETHING_PATTERN.matcher(trimmed).find())
            {
                warnings.add(String.format(
                    "%s: \"%s\" - Vague action description. Suggest using precise assertion text or structural validation descriptions (e.g., 'verify that the page header contains \"Dashboard\"').",
                    stepLabel, step
                ));
            }
        }

        return warnings;
    }
}
