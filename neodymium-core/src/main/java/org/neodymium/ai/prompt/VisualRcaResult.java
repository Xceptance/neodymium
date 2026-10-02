/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Class representing the structured outcome of Visual Root Cause Analysis (RCA).
 * Evaluates execution failures across specialized rubrics (target presence, form validation,
 * flow progression, and action obstruction) and synthesizes a concrete root cause diagnosis.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class VisualRcaResult
{
    @JsonProperty("rubrics")
    @SerializedName("rubrics")
    private Rubrics rubrics;

    @JsonProperty("rootCause")
    @SerializedName("rootCause")
    private String rootCause;

    /**
     * Default constructor for serialization.
     */
    public VisualRcaResult()
    {
    }

    /**
     * Constructs a VisualRcaResult with rubrics and root cause.
     *
     * @param rubrics the evaluation rubrics
     * @param rootCause the synthesized root cause explanation
     */
    public VisualRcaResult(final Rubrics rubrics, final String rootCause)
    {
        this.rubrics = rubrics;
        this.rootCause = rootCause;
    }

    /**
     * Retrieves the rubrics container.
     *
     * @return the rubrics container
     */
    public Rubrics getRubrics()
    {
        return this.rubrics;
    }

    /**
     * Sets the rubrics container.
     *
     * @param rubrics the rubrics to set
     */
    public void setRubrics(final Rubrics rubrics)
    {
        this.rubrics = rubrics;
    }

    /**
     * Retrieves the root cause diagnosis.
     *
     * @return the root cause string
     */
    public String getRootCause()
    {
        return this.rootCause;
    }

    /**
     * Sets the root cause diagnosis.
     *
     * @param rootCause the root cause string
     */
    public void setRootCause(final String rootCause)
    {
        this.rootCause = rootCause;
    }

    /**
     * Alias for {@link #getRootCause()}.
     *
     * @return the root cause string
     */
    public String rootCause()
    {
        return this.rootCause;
    }

    /**
     * Formats the visual RCA result into a readable Markdown report block containing
     * the root cause diagnosis followed by rubric breakdowns.
     *
     * @return formatted Markdown diagnosis
     */
    public String toFormattedDiagnosis()
    {
        final StringBuilder sb = new StringBuilder();
        if (this.rootCause != null && !this.rootCause.isBlank())
        {
            sb.append("**Root Cause Analysis:**\n\n").append(this.rootCause.trim()).append("\n\n");
        }
        if (this.rubrics != null)
        {
            sb.append("### Diagnostic Rubrics\n");
            if (this.rubrics.targetPresence() != null)
            {
                sb.append("- **Target Presence Check:** `[").append(this.rubrics.targetPresence().score()).append("]` ")
                  .append(this.rubrics.targetPresence().analysis()).append("\n");
            }
            if (this.rubrics.formValidation() != null)
            {
                sb.append("- **Form & Validation Check:** `[").append(this.rubrics.formValidation().score()).append("]` ")
                  .append(this.rubrics.formValidation().analysis()).append("\n");
            }
            if (this.rubrics.flowState() != null)
            {
                sb.append("- **Navigation & Flow State:** `[").append(this.rubrics.flowState().score()).append("]` ")
                  .append(this.rubrics.flowState().analysis()).append("\n");
            }
            if (this.rubrics.obstruction() != null)
            {
                sb.append("- **Action Obstruction Check:** `[").append(this.rubrics.obstruction().score()).append("]` ")
                  .append(this.rubrics.obstruction().analysis()).append("\n");
            }
        }
        return sb.length() > 0 ? sb.toString().trim() : (this.rootCause != null ? this.rootCause : "");
    }

    /**
     * Record representing an individual rubric evaluation.
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
     * Record containing the four visual RCA rubrics.
     */
    public static record Rubrics(
        @JsonProperty("targetPresence")
        @SerializedName("targetPresence")
        RubricItem targetPresence,

        @JsonProperty("formValidation")
        @SerializedName("formValidation")
        RubricItem formValidation,

        @JsonProperty("flowState")
        @SerializedName("flowState")
        RubricItem flowState,

        @JsonProperty("obstruction")
        @SerializedName("obstruction")
        RubricItem obstruction
    ) {}
}
