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
import java.util.Locale;
import java.util.Map;

/**
 * Data transfer object representing a single historical point in the cumulative suite evolution timeline.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class BatchSuiteEvolutionPointDto
{
    private final String runId;
    private final int runIndex;
    private final String timestampLabel;
    private final int totalTestsKnown;
    private final int newTestsCount;
    private final int passCleanCount;
    private final int passHealedCount;
    private final int passAiCount;
    private final int fixedCleanCount;
    private final int fixedHealedCount;
    private final int fixedAiCount;
    private final int knownCleanCount;
    private final int knownHealedCount;
    private final int knownAiCount;
    private final int unknownCleanCount;
    private final int unknownHealedCount;
    private final int unknownAiCount;
    private final int ignoredCount;
    private final int playbookCount;
    private final double passRate;
    private final int flakyCount;
    private final Map<String, Map<String, Integer>> tagCountsByType;

    public BatchSuiteEvolutionPointDto(
        final String runId,
        final int runIndex,
        final String timestampLabel,
        final int totalTestsKnown,
        final int newTestsCount,
        final int passCleanCount,
        final int passHealedCount,
        final int passAiCount,
        final int fixedCleanCount,
        final int fixedHealedCount,
        final int fixedAiCount,
        final int knownCleanCount,
        final int knownHealedCount,
        final int knownAiCount,
        final int unknownCleanCount,
        final int unknownHealedCount,
        final int unknownAiCount,
        final int ignoredCount,
        final double passRate)
    {
        this(
            runId, runIndex, timestampLabel, totalTestsKnown, newTestsCount,
            passCleanCount, passHealedCount, passAiCount,
            fixedCleanCount, fixedHealedCount, fixedAiCount,
            knownCleanCount, knownHealedCount, knownAiCount,
            unknownCleanCount, unknownHealedCount, unknownAiCount,
            ignoredCount, 0, passRate, 0, Collections.emptyMap()
        );
    }

    public BatchSuiteEvolutionPointDto(
        final String runId,
        final int runIndex,
        final String timestampLabel,
        final int totalTestsKnown,
        final int newTestsCount,
        final int passCleanCount,
        final int passHealedCount,
        final int passAiCount,
        final int fixedCleanCount,
        final int fixedHealedCount,
        final int fixedAiCount,
        final int knownCleanCount,
        final int knownHealedCount,
        final int knownAiCount,
        final int unknownCleanCount,
        final int unknownHealedCount,
        final int unknownAiCount,
        final int ignoredCount,
        final int playbookCount,
        final double passRate)
    {
        this(
            runId, runIndex, timestampLabel, totalTestsKnown, newTestsCount,
            passCleanCount, passHealedCount, passAiCount,
            fixedCleanCount, fixedHealedCount, fixedAiCount,
            knownCleanCount, knownHealedCount, knownAiCount,
            unknownCleanCount, unknownHealedCount, unknownAiCount,
            ignoredCount, playbookCount, passRate, 0, Collections.emptyMap()
        );
    }

    public BatchSuiteEvolutionPointDto(
        final String runId,
        final int runIndex,
        final String timestampLabel,
        final int totalTestsKnown,
        final int newTestsCount,
        final int passCleanCount,
        final int passHealedCount,
        final int passAiCount,
        final int fixedCleanCount,
        final int fixedHealedCount,
        final int fixedAiCount,
        final int knownCleanCount,
        final int knownHealedCount,
        final int knownAiCount,
        final int unknownCleanCount,
        final int unknownHealedCount,
        final int unknownAiCount,
        final int ignoredCount,
        final int playbookCount,
        final double passRate,
        final int flakyCount)
    {
        this(
            runId, runIndex, timestampLabel, totalTestsKnown, newTestsCount,
            passCleanCount, passHealedCount, passAiCount,
            fixedCleanCount, fixedHealedCount, fixedAiCount,
            knownCleanCount, knownHealedCount, knownAiCount,
            unknownCleanCount, unknownHealedCount, unknownAiCount,
            ignoredCount, playbookCount, passRate, flakyCount, Collections.emptyMap()
        );
    }

    public BatchSuiteEvolutionPointDto(
        final String runId,
        final int runIndex,
        final String timestampLabel,
        final int totalTestsKnown,
        final int newTestsCount,
        final int passCleanCount,
        final int passHealedCount,
        final int passAiCount,
        final int fixedCleanCount,
        final int fixedHealedCount,
        final int fixedAiCount,
        final int knownCleanCount,
        final int knownHealedCount,
        final int knownAiCount,
        final int unknownCleanCount,
        final int unknownHealedCount,
        final int unknownAiCount,
        final int ignoredCount,
        final int playbookCount,
        final double passRate,
        final int flakyCount,
        final Map<String, Map<String, Integer>> tagCountsByType)
    {
        this.runId = (runId != null) ? runId : "";
        this.runIndex = runIndex;
        this.timestampLabel = (timestampLabel != null) ? timestampLabel : "";
        this.totalTestsKnown = Math.max(0, totalTestsKnown);
        this.newTestsCount = Math.max(0, newTestsCount);
        this.passCleanCount = Math.max(0, passCleanCount);
        this.passHealedCount = Math.max(0, passHealedCount);
        this.passAiCount = Math.max(0, passAiCount);
        this.fixedCleanCount = Math.max(0, fixedCleanCount);
        this.fixedHealedCount = Math.max(0, fixedHealedCount);
        this.fixedAiCount = Math.max(0, fixedAiCount);
        this.knownCleanCount = Math.max(0, knownCleanCount);
        this.knownHealedCount = Math.max(0, knownHealedCount);
        this.knownAiCount = Math.max(0, knownAiCount);
        this.unknownCleanCount = Math.max(0, unknownCleanCount);
        this.unknownHealedCount = Math.max(0, unknownHealedCount);
        this.unknownAiCount = Math.max(0, unknownAiCount);
        this.ignoredCount = Math.max(0, ignoredCount);
        this.playbookCount = Math.max(0, playbookCount);
        this.passRate = passRate;
        this.flakyCount = Math.max(0, flakyCount);
        this.tagCountsByType = (tagCountsByType != null) ? Collections.unmodifiableMap(tagCountsByType) : Collections.emptyMap();
    }

    public int getFlakyCount()
    {
        return flakyCount;
    }

    public String getRunId()
    {
        return runId;
    }

    public int getRunIndex()
    {
        return runIndex;
    }

    public String getTimestampLabel()
    {
        return timestampLabel;
    }

    public int getTotalTestsKnown()
    {
        return totalTestsKnown;
    }

    public int getNewTestsCount()
    {
        return newTestsCount;
    }

    public int getPassCleanCount()
    {
        return passCleanCount;
    }

    public int getPassHealedCount()
    {
        return passHealedCount;
    }

    public int getPassAiCount()
    {
        return passAiCount;
    }

    public int getFixedCleanCount()
    {
        return fixedCleanCount;
    }

    public int getFixedHealedCount()
    {
        return fixedHealedCount;
    }

    public int getFixedAiCount()
    {
        return fixedAiCount;
    }

    public int getKnownCleanCount()
    {
        return knownCleanCount;
    }

    public int getKnownHealedCount()
    {
        return knownHealedCount;
    }

    public int getKnownAiCount()
    {
        return knownAiCount;
    }

    public int getUnknownCleanCount()
    {
        return unknownCleanCount;
    }

    public int getUnknownHealedCount()
    {
        return unknownHealedCount;
    }

    public int getUnknownAiCount()
    {
        return unknownAiCount;
    }

    public int getIgnoredCount()
    {
        return ignoredCount;
    }

    public double getPassRate()
    {
        return passRate;
    }

    public int getPassedTotalCount()
    {
        return passCleanCount + passHealedCount + passAiCount;
    }

    public int getFixedTotalCount()
    {
        return fixedCleanCount + fixedHealedCount + fixedAiCount;
    }

    public int getKnownTotalCount()
    {
        return knownCleanCount + knownHealedCount + knownAiCount;
    }

    public int getUnknownTotalCount()
    {
        return unknownCleanCount + unknownHealedCount + unknownAiCount;
    }

    public String getPassRateFormatted()
    {
        return String.format(Locale.ROOT, "%.1f%%", passRate);
    }

    public int getPlaybookCount()
    {
        return playbookCount;
    }

    public int getTotalAiCount()
    {
        return passAiCount + fixedAiCount + knownAiCount + unknownAiCount;
    }

    public int getTotalHealedCount()
    {
        return passHealedCount + fixedHealedCount + knownHealedCount + unknownHealedCount;
    }

    public int getTotalAiUsageCount()
    {
        return getTotalAiCount() + getTotalHealedCount();
    }

    public int getClassicCount()
    {
        return Math.max(0, totalTestsKnown - getTotalAiCount() - getTotalHealedCount() - playbookCount);
    }

    public double getAiUsageRate()
    {
        return totalTestsKnown > 0 ? (getTotalAiUsageCount() * 100.0 / totalTestsKnown) : 0.0;
    }

    public Map<String, Integer> getTagCounts(final String tagKey)
    {
        if (tagCountsByType == null || tagCountsByType.isEmpty() || tagKey == null || tagKey.isBlank())
        {
            return Collections.emptyMap();
        }
        final String normKey = tagKey.trim();
        for (final Map.Entry<String, Map<String, Integer>> entry : tagCountsByType.entrySet())
        {
            if (entry.getKey().equalsIgnoreCase(normKey))
            {
                return entry.getValue() != null ? entry.getValue() : Collections.emptyMap();
            }
        }
        return Collections.emptyMap();
    }

    public Map<String, Map<String, Integer>> getTagCountsByType()
    {
        return tagCountsByType != null ? tagCountsByType : Collections.emptyMap();
    }
}
