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
 * Universal, domain-neutral quality categories evaluated by the upfront playbook pre-flight linter.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public enum LinterCategory
{
    /**
     * Compound multi-action steps or mixed interactive action + post-condition verification.
     */
    STEP_SPLITTING_CANDIDATE,

    /**
     * Visual/layout descriptions lacking viewport {@code (visual)} or full-page {@code (visual: full)} tags.
     */
    MISSING_VISUAL_TAG,

    /**
     * Passive element capability phrasing ("allows to...", "ermöglicht...") lacking explicit imperative action or verification.
     */
    AMBIGUOUS_AFFORDANCE,

    /**
     * Under-specified element targets lacking container, label, or section context.
     */
    VAGUE_TARGET,

    /**
     * Subjective or non-verifiable test oracles ("Make sure everything looks good").
     */
    VAGUE_VERIFICATION,

    /**
     * Ambiguous relative pronouns or lost antecedents across steps ("it", "that one").
     */
    DANGLING_ANAPHORA,

    /**
     * Sequence inversion or operating on entities prior to opening/creating them.
     */
    TEMPORAL_FLOW_ANOMALY,

    /**
     * Hardcoded execution-time dynamic timestamps or IDs instead of parameterized variables (${...}).
     */
    HARDCODED_VOLATILE_DATA;

    /**
     * Resolves an enum constant from a string code safely, case-insensitively.
     *
     * @param code the category code
     * @return matching LinterCategory or null if not found
     */
    public static LinterCategory fromCode(final String code)
    {
        if (code == null || code.isBlank())
        {
            return null;
        }

        final String normalized = code.trim().toUpperCase();
        for (final LinterCategory category : values())
        {
            if (category.name().equals(normalized))
            {
                return category;
            }
        }
        return null;
    }
}
