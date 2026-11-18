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
 * Thrown when the upfront Playbook Pre-Flight Linter detects findings and strict quality gating
 * is enabled via {@code @AiLinter(failOnFindings = true)} or {@code neodymium.ai.linter.failOnFindings=true}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookLinterException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final List<PlaybookLinterFinding> findings;

    /**
     * Constructs a PlaybookLinterException with the given findings.
     *
     * @param message failure explanation message
     * @param findings the list of detected linter findings
     */
    public PlaybookLinterException(final String message, final List<PlaybookLinterFinding> findings)
    {
        super(message);
        this.findings = findings != null ? Collections.unmodifiableList(findings) : Collections.emptyList();
    }

    /**
     * Returns the detected linter findings that triggered this failure.
     *
     * @return immutable list of linter findings
     */
    public List<PlaybookLinterFinding> getFindings()
    {
        return this.findings;
    }
}
