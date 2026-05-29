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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link StepLinter} verifying semantic warning generation.
 * 
 * // AI-generated: Gemini 3.5 Flash
 */
class StepLinterTest
{
    @Test
    void lint_validSteps_returnsNoWarnings()
    {
        final List<String> steps = new ArrayList<>();
        steps.add("Click the \"Login\" button.");
        steps.add("Type \"user@example.com\" into the email address field.");
        steps.add("Verify that the page header contains \"Dashboard\".");
        steps.add("Click the button (hint: #login-btn).");
        steps.add("Enter password 123456.");

        final List<String> warnings = StepLinter.lint(steps);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void lint_lackingElementTargeting_returnsWarnings()
    {
        final List<String> steps = new ArrayList<>();
        steps.add("Click the button.");
        steps.add("Hover the link");
        steps.add("Click it");

        final List<String> warnings = StepLinter.lint(steps);
        assertEquals(3, warnings.size());
        assertTrue(warnings.get(0).contains("Lacks element targeting"));
        assertTrue(warnings.get(1).contains("Lacks element targeting"));
        assertTrue(warnings.get(2).contains("Lacks element targeting"));
    }

    @Test
    void lint_lackingElementTargetingWithHint_returnsNoWarnings()
    {
        final List<String> steps = new ArrayList<>();
        steps.add("Click the button (hint: .submit)");
        steps.add("Click the link (selector: #nav)");

        final List<String> warnings = StepLinter.lint(steps);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void lint_missingInputValues_returnsWarnings()
    {
        final List<String> steps = new ArrayList<>();
        steps.add("Type the email");
        steps.add("Enter password");
        steps.add("Input username.");

        final List<String> warnings = StepLinter.lint(steps);
        assertEquals(3, warnings.size());
        assertTrue(warnings.get(0).contains("Missing explicit value to input"));
        assertTrue(warnings.get(1).contains("Missing explicit value to input"));
        assertTrue(warnings.get(2).contains("Missing explicit value to input"));
    }

    @Test
    void lint_vagueActions_returnsWarnings()
    {
        final List<String> steps = new ArrayList<>();
        steps.add("Verify page.");
        steps.add("Do something");
        steps.add("Test this.");

        final List<String> warnings = StepLinter.lint(steps);
        assertEquals(3, warnings.size());
        assertTrue(warnings.get(0).contains("Vague action description"));
        assertTrue(warnings.get(1).contains("Vague action description"));
        assertTrue(warnings.get(2).contains("Vague action description"));
    }

    @Test
    void lint_withContext_returnsWarningsWithLineNumbers()
    {
        final List<String> steps = new ArrayList<>();
        steps.add("Click the button.");
        steps.add("Hover the link");
        steps.add("Click it");

        final List<Integer> lineNumbers = new ArrayList<>();
        lineNumbers.add(10);
        lineNumbers.add(20);
        lineNumbers.add(30);

        final List<String> warnings = StepLinter.lint(steps, lineNumbers, "TC_TEST_001.yaml");
        assertEquals(3, warnings.size());
        assertTrue(warnings.get(0).contains("Step 1 (line 10)"));
        assertTrue(warnings.get(1).contains("Step 2 (line 20)"));
        assertTrue(warnings.get(2).contains("Step 3 (line 30)"));
    }
}
