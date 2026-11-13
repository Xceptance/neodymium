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
package com.xceptance.aura.report.dto;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Data transfer object encapsulating a single chronological total point in the token cost trend,
 * representing a group of contributing runs that formed the test status aggregate at that time.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class TokenCostPointGroupDto
{
    private final int pointIndex;
    private final String pointLabel;
    private final String runId;
    private final String timestampLabel;
    private final double totalCost;
    private final long totalTokens;
    private final double deltaCost;
    private final double deltaPercentage;
    private final List<ContributingRunCostDto> contributingRuns;

    public TokenCostPointGroupDto(
        final int pointIndex,
        final String pointLabel,
        final String runId,
        final String timestampLabel,
        final double totalCost,
        final long totalTokens,
        final double deltaCost,
        final double deltaPercentage,
        final List<ContributingRunCostDto> contributingRuns)
    {
        this.pointIndex = pointIndex;
        this.pointLabel = (pointLabel != null) ? pointLabel : "";
        this.runId = (runId != null) ? runId : "";
        this.timestampLabel = (timestampLabel != null) ? timestampLabel : "";
        this.totalCost = Math.max(0.0, totalCost);
        this.totalTokens = Math.max(0L, totalTokens);
        this.deltaCost = deltaCost;
        this.deltaPercentage = deltaPercentage;
        this.contributingRuns = (contributingRuns != null) ? List.copyOf(contributingRuns) : Collections.emptyList();
    }

    public int getPointIndex()
    {
        return pointIndex;
    }

    public String getPointLabel()
    {
        return pointLabel;
    }

    public String getRunId()
    {
        return runId;
    }

    public String getTimestampLabel()
    {
        return timestampLabel;
    }

    public double getTotalCost()
    {
        return totalCost;
    }

    public long getTotalTokens()
    {
        return totalTokens;
    }

    public double getDeltaCost()
    {
        return deltaCost;
    }

    public double getDeltaPercentage()
    {
        return deltaPercentage;
    }

    public List<ContributingRunCostDto> getContributingRuns()
    {
        return contributingRuns;
    }

    public String getTotalCostFormatted()
    {
        return formatCurrency(totalCost);
    }

    public String getTotalTokensFormatted()
    {
        return String.format(Locale.ROOT, "%,d tokens", totalTokens);
    }

    public String getDeltaLabel()
    {
        if (Math.abs(deltaCost) < 0.00005)
        {
            return "$0.0000 vs prev day";
        }
        final String sign = deltaCost > 0.0 ? "+" : "-";
        final String formattedAbs = formatCurrency(Math.abs(deltaCost));
        return String.format(Locale.ROOT, "%s%s (%.0f%%) vs prev day", sign, formattedAbs, deltaPercentage);
    }

    private static String formatCurrency(final double amount)
    {
        if (amount >= 1_000_000.0)
        {
            return String.format(Locale.ROOT, "$%.2fM", amount / 1_000_000.0);
        }
        if (amount >= 10_000.0)
        {
            return String.format(Locale.ROOT, "$%.1fK", amount / 1_000.0);
        }
        if (amount >= 1.0)
        {
            return String.format(Locale.ROOT, "$%.2f", amount);
        }
        if (amount > 0.0)
        {
            return String.format(Locale.ROOT, "$%.4f", amount);
        }
        return "$0.0000";
    }
}
