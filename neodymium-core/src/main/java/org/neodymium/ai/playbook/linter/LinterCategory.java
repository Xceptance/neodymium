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
    HARDCODED_VOLATILE_DATA,

    /**
     * Dangling or incomplete conditional clause ("If...", "When...", "Falls...") lacking a consequence or action.
     */
    INCOMPLETE_BRANCH_CLAUSE,

    /**
     * Mid-scenario direct URL navigation or mutation skipping interactive user journeys.
     */
    JOURNEY_FIDELITY_VIOLATION,

    /**
     * Unsupported, misspelled, or non-standard parenthetical modality tags (e.g. {@code (screenshot)}, {@code (fullpage)}).
     */
    UNRECOGNIZED_MODALITY_TAG,

    /**
     * Instructions explicitly commanding raw script or code execution (e.g. "Run JS to click...") instead of standard user interactions.
     */
    EXPLICIT_SCRIPT_INTERACTION,

    /**
     * Empirical runtime detection of a flat step executing multiple interactive mutating actions.
     */
    EMPIRICAL_MULTI_ACTION,

    /**
     * Empirical runtime divergence where instruction text did not match the clicked element's accessible name or DOM text.
     */
    LABEL_DIVERGENCE,

    /**
     * Empirical agent execution thrashing requiring excessive turns or retries.
     */
    HIGH_AGENT_FRICTION,

    /**
     * Empirical step resolution that required visual perception at runtime but lacks the (visual) tag.
     */
    UNTAGGED_VISUAL_DEPENDENCY,

    /**
     * Empirical step resolution that was tagged (visual) but was fully resolved via standard DOM elements without visual comparison.
     */
    REDUNDANT_VISUAL_TAG;

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
