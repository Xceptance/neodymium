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

import com.xceptance.aura.report.entity.TestRunEntity;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object encapsulating the aggregate results of the latest execution of all batch tests,
 * including contributing runs and variation distributions for browsers and locales.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class BatchLatestExecutionsDto
{
    private final BatchSummaryDto batchSummary;
    private final List<TestRunEntity> contributingRuns;
    private final List<DistributionRatioDto> browserRatios;
    private final List<DistributionRatioDto> localeRatios;
    private final List<DistributionRatioDto> environmentRatios;
    private final List<DistributionRatioDto> areaRatios;
    private final List<DistributionRatioDto> tagRatios;
    private final Map<String, List<DistributionRatioDto>> tagRatiosByType;
    private final List<BatchLatestVariationDto> latestVariations;
    private final int totalVariationsCount;
    private final int eligibleRunsCount;
    private final List<TestRunEntity> eligibleRuns;
    private final TokenCostTrendDto tokenCostTrend;
    private final BatchSuiteEvolutionDto suiteEvolution;
    private final String asOfRunId;
    private final String asOfTimestampLabel;
    private final boolean isHistoricalSnapshot;

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final int totalVariationsCount)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), totalVariationsCount, (contributingRuns != null) ? contributingRuns.size() : 0, contributingRuns);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), latestVariations, totalVariationsCount, (contributingRuns != null) ? contributingRuns.size() : 0, contributingRuns);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<DistributionRatioDto> environmentRatios,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, environmentRatios, Collections.emptyList(), Collections.emptyList(), latestVariations, totalVariationsCount, (contributingRuns != null) ? contributingRuns.size() : 0, contributingRuns);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<DistributionRatioDto> environmentRatios,
        final List<DistributionRatioDto> areaRatios,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, environmentRatios, areaRatios, Collections.emptyList(), latestVariations, totalVariationsCount, (contributingRuns != null) ? contributingRuns.size() : 0, contributingRuns);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<DistributionRatioDto> environmentRatios,
        final List<DistributionRatioDto> areaRatios,
        final List<DistributionRatioDto> tagRatios,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, environmentRatios, areaRatios, tagRatios, latestVariations, totalVariationsCount, (contributingRuns != null) ? contributingRuns.size() : 0, contributingRuns);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<DistributionRatioDto> environmentRatios,
        final List<DistributionRatioDto> areaRatios,
        final List<DistributionRatioDto> tagRatios,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount,
        final int eligibleRunsCount)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, environmentRatios, areaRatios, tagRatios, latestVariations, totalVariationsCount, eligibleRunsCount, contributingRuns);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<DistributionRatioDto> environmentRatios,
        final List<DistributionRatioDto> areaRatios,
        final List<DistributionRatioDto> tagRatios,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount,
        final int eligibleRunsCount,
        final List<TestRunEntity> eligibleRuns)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, environmentRatios, areaRatios, tagRatios, (tagRatios != null && !tagRatios.isEmpty()) ? Map.of("batch", tagRatios) : Collections.emptyMap(), latestVariations, totalVariationsCount, eligibleRunsCount, eligibleRuns);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<DistributionRatioDto> environmentRatios,
        final List<DistributionRatioDto> areaRatios,
        final List<DistributionRatioDto> tagRatios,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount,
        final int eligibleRunsCount,
        final List<TestRunEntity> eligibleRuns,
        final TokenCostTrendDto tokenCostTrend)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, environmentRatios, areaRatios, tagRatios, (tagRatios != null && !tagRatios.isEmpty()) ? Map.of("batch", tagRatios) : Collections.emptyMap(), latestVariations, totalVariationsCount, eligibleRunsCount, eligibleRuns, tokenCostTrend, null);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<DistributionRatioDto> environmentRatios,
        final List<DistributionRatioDto> areaRatios,
        final List<DistributionRatioDto> tagRatios,
        final Map<String, List<DistributionRatioDto>> tagRatiosByType,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount,
        final int eligibleRunsCount,
        final List<TestRunEntity> eligibleRuns)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, environmentRatios, areaRatios, tagRatios, tagRatiosByType, latestVariations, totalVariationsCount, eligibleRunsCount, eligibleRuns, null, null);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<DistributionRatioDto> environmentRatios,
        final List<DistributionRatioDto> areaRatios,
        final List<DistributionRatioDto> tagRatios,
        final Map<String, List<DistributionRatioDto>> tagRatiosByType,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount,
        final int eligibleRunsCount,
        final List<TestRunEntity> eligibleRuns,
        final TokenCostTrendDto tokenCostTrend)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, environmentRatios, areaRatios, tagRatios, tagRatiosByType, latestVariations, totalVariationsCount, eligibleRunsCount, eligibleRuns, tokenCostTrend, null);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<DistributionRatioDto> environmentRatios,
        final List<DistributionRatioDto> areaRatios,
        final List<DistributionRatioDto> tagRatios,
        final Map<String, List<DistributionRatioDto>> tagRatiosByType,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount,
        final int eligibleRunsCount,
        final List<TestRunEntity> eligibleRuns,
        final TokenCostTrendDto tokenCostTrend,
        final BatchSuiteEvolutionDto suiteEvolution)
    {
        this(batchSummary, contributingRuns, browserRatios, localeRatios, environmentRatios, areaRatios, tagRatios, tagRatiosByType, latestVariations, totalVariationsCount, eligibleRunsCount, eligibleRuns, tokenCostTrend, suiteEvolution, null, null, false);
    }

    public BatchLatestExecutionsDto(
        final BatchSummaryDto batchSummary,
        final List<TestRunEntity> contributingRuns,
        final List<DistributionRatioDto> browserRatios,
        final List<DistributionRatioDto> localeRatios,
        final List<DistributionRatioDto> environmentRatios,
        final List<DistributionRatioDto> areaRatios,
        final List<DistributionRatioDto> tagRatios,
        final Map<String, List<DistributionRatioDto>> tagRatiosByType,
        final List<BatchLatestVariationDto> latestVariations,
        final int totalVariationsCount,
        final int eligibleRunsCount,
        final List<TestRunEntity> eligibleRuns,
        final TokenCostTrendDto tokenCostTrend,
        final BatchSuiteEvolutionDto suiteEvolution,
        final String asOfRunId,
        final String asOfTimestampLabel,
        final boolean isHistoricalSnapshot)
    {
        this.batchSummary = batchSummary;
        this.contributingRuns = (contributingRuns != null) ? List.copyOf(contributingRuns) : Collections.emptyList();
        this.browserRatios = (browserRatios != null) ? List.copyOf(browserRatios) : Collections.emptyList();
        this.localeRatios = (localeRatios != null) ? List.copyOf(localeRatios) : Collections.emptyList();
        this.environmentRatios = (environmentRatios != null) ? List.copyOf(environmentRatios) : Collections.emptyList();
        this.areaRatios = (areaRatios != null) ? List.copyOf(areaRatios) : Collections.emptyList();
        this.tagRatios = (tagRatios != null) ? List.copyOf(tagRatios) : Collections.emptyList();
        this.tagRatiosByType = (tagRatiosByType != null) ? Collections.unmodifiableMap(new LinkedHashMap<>(tagRatiosByType)) : Collections.emptyMap();
        this.latestVariations = (latestVariations != null) ? List.copyOf(latestVariations) : Collections.emptyList();
        this.totalVariationsCount = totalVariationsCount;
        this.eligibleRunsCount = eligibleRunsCount;
        this.eligibleRuns = (eligibleRuns != null) ? List.copyOf(eligibleRuns) : (contributingRuns != null ? List.copyOf(contributingRuns) : Collections.emptyList());
        this.tokenCostTrend = tokenCostTrend;
        this.suiteEvolution = suiteEvolution;
        this.asOfRunId = asOfRunId;
        this.asOfTimestampLabel = asOfTimestampLabel;
        this.isHistoricalSnapshot = isHistoricalSnapshot;
    }

    public String getAsOfRunId()
    {
        return asOfRunId;
    }

    public String getAsOfTimestampLabel()
    {
        return asOfTimestampLabel;
    }

    public boolean isHistoricalSnapshot()
    {
        return isHistoricalSnapshot;
    }

    public BatchSuiteEvolutionDto getSuiteEvolution()
    {
        return suiteEvolution;
    }

    public TokenCostTrendDto getTokenCostTrend()
    {
        return tokenCostTrend;
    }

    public double getLatestTotalLlmCost()
    {
        return (tokenCostTrend != null) ? tokenCostTrend.getCurrentCost() : 0.0;
    }

    public String getLatestTotalLlmCostFormatted()
    {
        return (tokenCostTrend != null) ? tokenCostTrend.getCurrentCostFormatted() : "$0.0000";
    }

    public long getLatestTotalLlmTokens()
    {
        return (tokenCostTrend != null) ? tokenCostTrend.getTotalTokens() : 0L;
    }

    public String getLatestTotalLlmTokensFormatted()
    {
        return (tokenCostTrend != null) ? tokenCostTrend.getTotalTokensFormatted() : "0 tokens";
    }

    public BatchSummaryDto getBatchSummary()
    {
        return batchSummary;
    }

    public List<TestRunEntity> getContributingRuns()
    {
        return contributingRuns;
    }

    public List<DistributionRatioDto> getBrowserRatios()
    {
        return browserRatios;
    }

    public List<DistributionRatioDto> getLocaleRatios()
    {
        return localeRatios;
    }

    public List<DistributionRatioDto> getEnvironmentRatios()
    {
        return environmentRatios;
    }

    public List<DistributionRatioDto> getAreaRatios()
    {
        return areaRatios;
    }

    public List<DistributionRatioDto> getTagRatios()
    {
        return tagRatios;
    }

    public Map<String, List<DistributionRatioDto>> getTagRatiosByType()
    {
        return tagRatiosByType;
    }

    public List<DistributionRatioDto> getTagRatios(final String tagKey)
    {
        if (tagRatiosByType == null || tagKey == null)
        {
            return Collections.emptyList();
        }
        final List<DistributionRatioDto> exact = tagRatiosByType.get(tagKey);
        if (exact != null)
        {
            return exact;
        }
        for (final Map.Entry<String, List<DistributionRatioDto>> entry : tagRatiosByType.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(tagKey))
            {
                return entry.getValue();
            }
        }
        return Collections.emptyList();
    }

    public List<BatchLatestVariationDto> getLatestVariations()
    {
        return latestVariations;
    }

    public int getTotalVariationsCount()
    {
        return totalVariationsCount;
    }

    public int getEligibleRunsCount()
    {
        return eligibleRunsCount;
    }

    public List<TestRunEntity> getEligibleRuns()
    {
        return eligibleRuns;
    }
}
