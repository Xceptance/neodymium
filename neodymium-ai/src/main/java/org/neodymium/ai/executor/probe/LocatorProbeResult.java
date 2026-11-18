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
package org.neodymium.ai.executor.probe;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of probing a candidate locator against the live SUT.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class LocatorProbeResult
{
    private final String candidateLocator;
    private final int matchCount;
    private final List<ProbeElementSummary> matches;
    private final String errorMessage;
    private final boolean isSupported;

    /**
     * Constructs a complete locator probe result.
     *
     * @param candidateLocator probed locator string
     * @param matchCount total matched elements in live DOM
     * @param matches summaries for probed matches up to max depth
     * @param errorMessage error message if probing failed or syntax was invalid
     * @param isSupported whether target executor supports probing
     */
    @JsonCreator
    public LocatorProbeResult(
            @JsonProperty("candidateLocator") final String candidateLocator,
            @JsonProperty("matchCount") final int matchCount,
            @JsonProperty("matches") final List<ProbeElementSummary> matches,
            @JsonProperty("errorMessage") final String errorMessage,
            @JsonProperty("isSupported") final boolean isSupported)
    {
        this.candidateLocator = candidateLocator != null ? candidateLocator : "";
        this.matchCount = matchCount;
        this.matches = matches != null ? Collections.unmodifiableList(matches) : Collections.emptyList();
        this.errorMessage = errorMessage;
        this.isSupported = isSupported;
    }

    /**
     * Creates a successful probe result for a supported executor.
     *
     * @param candidateLocator probed locator string
     * @param matchCount total count of matched elements
     * @param matches summaries of matched elements
     * @return supported probe result
     */
    public static LocatorProbeResult supported(
            final String candidateLocator,
            final int matchCount,
            final List<ProbeElementSummary> matches)
    {
        return new LocatorProbeResult(candidateLocator, matchCount, matches, null, true);
    }

    /**
     * Creates an error probe result (e.g. invalid syntax).
     *
     * @param candidateLocator probed locator string
     * @param errorMessage description of error
     * @return error probe result
     */
    public static LocatorProbeResult error(final String candidateLocator, final String errorMessage)
    {
        return new LocatorProbeResult(candidateLocator, 0, Collections.emptyList(), errorMessage, true);
    }

    /**
     * Creates an unsupported probe result.
     *
     * @param candidateLocator candidate locator
     * @return unsupported probe result
     */
    public static LocatorProbeResult unsupported(final String candidateLocator)
    {
        return new LocatorProbeResult(candidateLocator, 0, Collections.emptyList(), "Probing not supported by active target executor", false);
    }

    public String getCandidateLocator()
    {
        return this.candidateLocator;
    }

    public int getMatchCount()
    {
        return this.matchCount;
    }

    public List<ProbeElementSummary> getMatches()
    {
        return this.matches;
    }

    public String getErrorMessage()
    {
        return this.errorMessage;
    }

    public boolean isSupported()
    {
        return this.isSupported;
    }

    public boolean isUnique()
    {
        return this.isSupported && this.matchCount == 1 && this.errorMessage == null;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof final LocatorProbeResult other))
        {
            return false;
        }
        return this.matchCount == other.matchCount
                && this.isSupported == other.isSupported
                && Objects.equals(this.candidateLocator, other.candidateLocator)
                && Objects.equals(this.matches, other.matches)
                && Objects.equals(this.errorMessage, other.errorMessage);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.candidateLocator, this.matchCount, this.matches, this.errorMessage, this.isSupported);
    }

    @Override
    public String toString()
    {
        if (!this.isSupported)
        {
            return String.format("Probe['%s']: UNSUPPORTED", this.candidateLocator);
        }
        if (this.errorMessage != null)
        {
            return String.format("Probe['%s']: ERROR (%s)", this.candidateLocator, this.errorMessage);
        }
        return String.format("Probe['%s']: %d match(es)", this.candidateLocator, this.matchCount);
    }
}
