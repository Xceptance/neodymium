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

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the complete result of a playbook pre-flight linting pass,
 * including advisory findings, execution success status, and error details.
 *
 * @param findings the list of advisory findings detected (never null)
 * @param successful whether the linter successfully invoked the LLM provider
 * @param errorMessage descriptive error message if execution failed, or null if successful
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public record PlaybookLinterResult(
    List<PlaybookLinterFinding> findings,
    boolean successful,
    String errorMessage
)
{
    /**
     * Compact constructor validating and ensuring unmodifiable findings list.
     */
    public PlaybookLinterResult
    {
        findings = findings != null ? List.copyOf(findings) : Collections.emptyList();
    }

    /**
     * Factory method creating a successful linter result.
     *
     * @param findings the detected findings
     * @return a successful PlaybookLinterResult
     */
    public static PlaybookLinterResult success(final List<PlaybookLinterFinding> findings)
    {
        return new PlaybookLinterResult(findings, true, null);
    }

    /**
     * Factory method creating a failed linter result.
     *
     * @param errorMessage description of the failure reason
     * @return a failed PlaybookLinterResult
     */
    public static PlaybookLinterResult failure(final String errorMessage)
    {
        return new PlaybookLinterResult(Collections.emptyList(), false, errorMessage);
    }
}
