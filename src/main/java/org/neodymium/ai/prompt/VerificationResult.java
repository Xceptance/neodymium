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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Class representing the outcome of semantic validation checks.
 * Under the AI Judge approach, it uses a multi-criteria rubric structure.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerificationResult
{
    @JsonProperty("rubrics")
    @SerializedName("rubrics")
    private Rubrics rubrics;

    @JsonProperty("overallVerdict")
    @SerializedName("overallVerdict")
    private OverallVerdict overallVerdict;

    /**
     * Default constructor for serialization.
     */
    public VerificationResult()
    {
    }

    /**
     * Constructs a verification result with the same message for all rubrics and overall verdict.
     *
     * @param passed true if the verification criteria were met, false otherwise
     * @param message the explanation message
     */
    public VerificationResult(final boolean passed, final String message)
    {
        final RubricItem item = new RubricItem(message, passed ? "PASS" : "FAIL");
        this.rubrics = new Rubrics(item, item, item);
        this.overallVerdict = new OverallVerdict(passed, message);
    }

    /**
     * Constructs a verification result with rubrics and overall verdict.
     *
     * @param rubrics the rubrics details
     * @param overallVerdict the overall verdict details
     */
    public VerificationResult(final Rubrics rubrics, final OverallVerdict overallVerdict)
    {
        this.rubrics = rubrics;
        this.overallVerdict = overallVerdict;
    }

    /**
     * Checks if the verification passed.
     *
     * @return true if passed, false otherwise
     */
    public boolean passed()
    {
        return overallVerdict != null && overallVerdict.passed();
    }

    /**
     * Gets the action reasoning detail.
     *
     * @return the action reasoning string
     */
    public String actionReasoning()
    {
        if (rubrics != null && rubrics.intentMatch() != null)
        {
            return rubrics.intentMatch().analysis() + " [Score: " + rubrics.intentMatch().score() + "]";
        }
        return overallVerdict != null ? overallVerdict.summary() : "";
    }

    /**
     * Gets the visual reasoning detail.
     *
     * @return the visual reasoning string
     */
    public String visualReasoning()
    {
        if (rubrics != null && rubrics.visualDelta() != null)
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(rubrics.visualDelta().analysis()).append(" [Score: ").append(rubrics.visualDelta().score()).append("]");
            if (rubrics.absenceOfErrors() != null)
            {
                sb.append(" | Error Check: ").append(rubrics.absenceOfErrors().analysis()).append(" [Score: ").append(rubrics.absenceOfErrors().score()).append("]");
            }
            return sb.toString();
        }
        return overallVerdict != null ? overallVerdict.summary() : "";
    }

    /**
     * Gets the rubrics.
     *
     * @return the rubrics
     */
    public Rubrics getRubrics()
    {
        return rubrics;
    }

    /**
     * Gets the overall verdict.
     *
     * @return the overall verdict
     */
    public OverallVerdict getOverallVerdict()
    {
        return overallVerdict;
    }

    /**
     * Nested class representing a single rubric evaluation item.
     */
    public static record RubricItem(
        @JsonProperty("analysis")
        @SerializedName("analysis")
        String analysis,

        @JsonProperty("score")
        @SerializedName("score")
        String score
    ) {}

    /**
     * Nested class representing the full set of evaluation rubrics.
     */
    public static record Rubrics(
        @JsonProperty("intentMatch")
        @SerializedName("intentMatch")
        RubricItem intentMatch,

        @JsonProperty("visualDelta")
        @SerializedName("visualDelta")
        RubricItem visualDelta,

        @JsonProperty("absenceOfErrors")
        @SerializedName("absenceOfErrors")
        RubricItem absenceOfErrors
    ) {}

    /**
     * Nested class representing the overall verdict.
     */
    public static record OverallVerdict(
        @JsonProperty("passed")
        @SerializedName("passed")
        boolean passed,

        @JsonProperty("summary")
        @SerializedName("summary")
        String summary
    ) {}
}
