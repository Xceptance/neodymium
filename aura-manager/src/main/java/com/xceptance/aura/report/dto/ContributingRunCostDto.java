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

import java.util.Locale;

/**
 * Data transfer object encapsulating an individual test run's contribution to a historical
 * or latest test status token cost aggregate.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class ContributingRunCostDto
{
    private final String runId;
    private final String timestampLabel;
    private final String environment;
    private final String browser;
    private final String batchName;
    private final int contributedTestCount;
    private final double contributedCost;
    private final long contributedTokens;
    private final double costPercentage;

    public ContributingRunCostDto(
        final String runId,
        final String timestampLabel,
        final String environment,
        final String browser,
        final String batchName,
        final int contributedTestCount,
        final double contributedCost,
        final long contributedTokens,
        final double costPercentage)
    {
        this.runId = (runId != null) ? runId : "";
        this.timestampLabel = (timestampLabel != null) ? timestampLabel : "";
        this.environment = (environment != null && !environment.isBlank()) ? environment : "Default";
        this.browser = (browser != null && !browser.isBlank()) ? browser : "Default";
        this.batchName = (batchName != null && !batchName.isBlank()) ? batchName : "Default";
        this.contributedTestCount = Math.max(0, contributedTestCount);
        this.contributedCost = Math.max(0.0, contributedCost);
        this.contributedTokens = Math.max(0L, contributedTokens);
        this.costPercentage = Math.max(0.0, Math.min(100.0, costPercentage));
    }

    public String getRunId()
    {
        return runId;
    }

    public String getTimestampLabel()
    {
        return timestampLabel;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public String getBrowser()
    {
        return browser;
    }

    public String getBatchName()
    {
        return batchName;
    }

    public int getContributedTestCount()
    {
        return contributedTestCount;
    }

    public double getContributedCost()
    {
        return contributedCost;
    }

    public long getContributedTokens()
    {
        return contributedTokens;
    }

    public double getCostPercentage()
    {
        return costPercentage;
    }

    public String getContributedCostFormatted()
    {
        return formatCurrency(contributedCost);
    }

    public String getContributedTokensFormatted()
    {
        return String.format(Locale.ROOT, "%,d tokens", contributedTokens);
    }

    public String getCostPercentageFormatted()
    {
        return String.format(Locale.ROOT, "%.1f%%", costPercentage);
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
