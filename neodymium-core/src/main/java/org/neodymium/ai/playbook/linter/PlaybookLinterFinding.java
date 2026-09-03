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
package org.neodymium.ai.playbook.linter;

/**
 * Immutable advisory finding produced by the upfront playbook pre-flight linter.
 *
 * @param stepIndex 1-based step index in the scenario
 * @param lineNumber source line number in the playbook/test file
 * @param sourceFile source file path, or null if unknown
 * @param rawInstruction the original template instruction before variable substitution
 * @param resolvedInstruction the instruction with test data variables resolved
 * @param category semantic quality category
 * @param severity advisory severity level
 * @param message diagnostic explanation
 * @param suggestedRewrite suggested rewrite in the original step's natural language
 * @param scope optional scope qualifier (e.g. VIEWPORT vs FULL_PAGE)
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public record PlaybookLinterFinding(
    int stepIndex,
    int lineNumber,
    String sourceFile,
    String rawInstruction,
    String resolvedInstruction,
    LinterCategory category,
    LinterSeverity severity,
    String message,
    String suggestedRewrite,
    String scope
)
{
    /**
     * Compact constructor with default fallbacks.
     */
    public PlaybookLinterFinding
    {
        severity = severity != null ? severity : LinterSeverity.WARNING;
        rawInstruction = rawInstruction != null ? rawInstruction : "";
        resolvedInstruction = resolvedInstruction != null ? resolvedInstruction : rawInstruction;
        message = message != null ? message : "";
        suggestedRewrite = suggestedRewrite != null ? suggestedRewrite : "";
    }

    /**
     * Convenience constructor without explicit source file and line number.
     *
     * @param stepIndex 1-based step index
     * @param rawInstruction original raw instruction
     * @param resolvedInstruction resolved instruction with data substituted
     * @param category quality category
     * @param severity severity level
     * @param message explanation message
     * @param suggestedRewrite suggested rewrite
     * @param scope optional scope
     */
    public PlaybookLinterFinding(
        final int stepIndex,
        final String rawInstruction,
        final String resolvedInstruction,
        final LinterCategory category,
        final LinterSeverity severity,
        final String message,
        final String suggestedRewrite,
        final String scope)
    {
        this(stepIndex, -1, null, rawInstruction, resolvedInstruction, category, severity, message, suggestedRewrite, scope);
    }
}
